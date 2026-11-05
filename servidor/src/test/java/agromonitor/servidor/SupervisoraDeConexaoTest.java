package agromonitor.servidor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import agromonitor.comum.Comunicado;
import agromonitor.comum.Parceiro;
import agromonitor.comum.PedidoParaSair;
import agromonitor.comum.alerta.*;
import agromonitor.comum.atividade.*;
import agromonitor.comum.auth.*;
import agromonitor.comum.conta.*;
import agromonitor.comum.plantio.*;
import agromonitor.servidor.dao.AlertaDAO;
import agromonitor.servidor.dao.PlantioDAO;
import agromonitor.servidor.dao.UsuarioDAO;
import agromonitor.servidor.email.EmailService;

// Teste de integracao do servidor: uma conexao de verdade (socket) + Mongo em memoria.
// So o envio de e-mail e falso. Passa por validacao -> DAO -> resposta, como no uso real.
class SupervisoraDeConexaoTest
{
    private BancoDeTeste bd;
    private UsuarioDAO   usuarioDao;
    private PlantioDAO   plantioDao;
    private AlertaDAO    alertaDao;
    private EmailService email;

    private ServerSocket         ss;
    private Socket               cliente;
    private ObjectOutputStream   saida;
    private ObjectInputStream    entrada;
    private ArrayList<Parceiro>  conectados;

    @BeforeEach
    void preparar() throws Exception
    {
        bd         = new BancoDeTeste();
        usuarioDao = new UsuarioDAO(bd.banco());
        plantioDao = new PlantioDAO(bd.banco());
        alertaDao  = new AlertaDAO(bd.banco());
        usuarioDao.garantirIndices();
        plantioDao.garantirIndices();
        alertaDao.garantirIndices();
        email      = mock(EmailService.class);
        conectados = new ArrayList<Parceiro>();

        ss = new ServerSocket(0);
        Thread aceitadora = new Thread(() -> {
            try
            {
                Socket s = ss.accept();
                new SupervisoraDeConexao(s, conectados, usuarioDao, plantioDao, alertaDao, email).start();
            }
            catch (Exception fim) {}
        });
        aceitadora.setDaemon(true);
        aceitadora.start();

        cliente = new Socket("localhost", ss.getLocalPort());
        cliente.setSoTimeout(15000);
        saida = new ObjectOutputStream(cliente.getOutputStream());
        saida.flush();
        entrada = new ObjectInputStream(cliente.getInputStream());
    }

    @AfterEach
    void limpar() throws Exception
    {
        try { saida.writeObject(new PedidoParaSair()); saida.flush(); } catch (Exception ignorada) {}
        try { cliente.close(); } catch (Exception ignorada) {}
        try { ss.close(); } catch (Exception ignorada) {}
        bd.close();
    }

    // ---------- utilitarios ----------
    private <T extends Comunicado> T enviar(Comunicado pedido, Class<T> tipo) throws Exception
    {
        saida.writeObject(pedido);
        saida.flush();
        return tipo.cast(entrada.readObject());
    }

    private RespostaDeCadastro cadastrar(String email, String senha) throws Exception
    {
        return enviar(new PedidoDeCadastro("Maria Silva", email, "52998224725", "19999990000", senha), RespostaDeCadastro.class);
    }

    private int cadastrarELogar(String email) throws Exception
    {
        assertTrue(cadastrar(email, "123456").isSucesso());
        RespostaDeLogin r = enviar(new PedidoDeLogin(email, "123456"), RespostaDeLogin.class);
        assertTrue(r.isSucesso());
        return r.getCodigo();
    }

    private RespostaDeCadastroDePlantio cadastrarPlantio(int usuario, LocalDate dataPlantio) throws Exception
    {
        return enviar(new PedidoDeCadastroDePlantio(usuario, "Soja", "Soja T1", "25/26", "descricao",
                "IRRIGADO", "T1", 12.5, "Campinas", 650, dataPlantio.toString(), "PLANTIO_DIRETO",
                null, null, null, null), RespostaDeCadastroDePlantio.class);
    }

    private RespostaDeRegistroDeAtividade registrar(int usuario, int plantio, String tipo, String status, LocalDate data) throws Exception
    {
        return enviar(new PedidoDeRegistroDeAtividade(usuario, plantio, tipo, status, data.toString(), null, null, null),
                      RespostaDeRegistroDeAtividade.class);
    }

    // ================= CADASTRO / LOGIN =================
    @Test
    void cadastroELoginComSucesso() throws Exception
    {
        RespostaDeCadastro c = cadastrar("MARIA@X.COM", "123456");
        assertTrue(c.isSucesso());
        assertEquals("Cadastro realizado com sucesso!", c.getMensagem());
        assertTrue(c.getCodigo() > 0);

        // o e-mail e guardado em minusculas: o login funciona de qualquer jeito
        RespostaDeLogin l = enviar(new PedidoDeLogin("maria@x.com", "123456"), RespostaDeLogin.class);
        assertTrue(l.isSucesso());
        assertEquals("Maria Silva", l.getNome());
        assertEquals("maria@x.com", l.getEmail());
        assertEquals(c.getCodigo(), l.getCodigo());
    }

    @Test
    void senhaEGravadaComoHashBcryptNuncaEmTextoPuro() throws Exception
    {
        cadastrar("maria@x.com", "123456");
        Document d = bd.banco().getCollection("usuarios").find().first();
        assertNotEquals("123456", d.getString("senhaHash"));
        assertTrue(d.getString("senhaHash").startsWith("$2"));
    }

    @Test
    void cadastroComEmailDuplicadoERecusado() throws Exception
    {
        cadastrar("maria@x.com", "123456");
        RespostaDeCadastro r = cadastrar("maria@x.com", "654321");
        assertFalse(r.isSucesso());
        assertEquals("Este e-mail ja esta cadastrado.", r.getMensagem());
    }

    @Test
    void cadastroInvalidoNaoGravaNada() throws Exception
    {
        RespostaDeCadastro r = cadastrar("maria@x.com", "123");
        assertFalse(r.isSucesso());
        assertEquals("A senha deve ter pelo menos 6 caracteres.", r.getMensagem());
        assertEquals(0, bd.banco().getCollection("usuarios").countDocuments());

        assertEquals("E-mail invalido.", cadastrar("invalido", "123456").getMensagem());
    }

    @Test
    void loginComSenhaErradaOuEmailInexistenteDaMesmaMensagem() throws Exception
    {
        cadastrar("maria@x.com", "123456");

        RespostaDeLogin errada = enviar(new PedidoDeLogin("maria@x.com", "errada1"), RespostaDeLogin.class);
        RespostaDeLogin inexistente = enviar(new PedidoDeLogin("naoexiste@x.com", "123456"), RespostaDeLogin.class);

        assertFalse(errada.isSucesso());
        assertFalse(inexistente.isSucesso());
        assertEquals("E-mail ou senha incorretos.", errada.getMensagem());
        assertEquals(errada.getMensagem(), inexistente.getMensagem()); // nao revela quem esta cadastrado
    }

    @Test
    void loginComDadosVaziosEhRecusado() throws Exception
    {
        assertFalse(enviar(new PedidoDeLogin("", ""), RespostaDeLogin.class).isSucesso());
        assertFalse(enviar(new PedidoDeLogin("maria@x.com", ""), RespostaDeLogin.class).isSucesso());
    }

    // ================= PLANTIOS =================
    @Test
    void cadastrarEListarPlantio() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");
        RespostaDeCadastroDePlantio r = cadastrarPlantio(u, LocalDate.now().plusDays(100));
        assertTrue(r.isSucesso());
        assertEquals("Plantio cadastrado com sucesso!", r.getMensagem());
        assertTrue(r.getCodigo() > 0);

        RespostaDeListagemDePlantios l = enviar(new PedidoDeListagemDePlantios(u), RespostaDeListagemDePlantios.class);
        assertTrue(l.isSucesso());
        assertEquals(1, l.getPlantios().size());
        assertEquals("Soja T1", l.getPlantios().get(0).getNome());
        assertEquals(4, l.getPlantios().get(0).getQuantidadeAtividades());
    }

    @Test
    void plantioDeUsuarioInexistenteERecusado() throws Exception
    {
        RespostaDeCadastroDePlantio r = cadastrarPlantio(999, LocalDate.now());
        assertFalse(r.isSucesso());
        assertEquals("Usuario nao encontrado. Entre novamente.", r.getMensagem());
    }

    @Test
    void plantioInvalidoMostraOPrimeiroErro() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");
        RespostaDeCadastroDePlantio r = enviar(new PedidoDeCadastroDePlantio(u, "", "Soja T1", "25/26", null,
                "IRRIGADO", "T1", 12.5, null, null, "2030-01-01", null, null, null, null, null),
                RespostaDeCadastroDePlantio.class);
        assertFalse(r.isSucesso());
        assertEquals("Selecione a cultura.", r.getMensagem());
        assertEquals(0, bd.banco().getCollection("plantios").countDocuments());
    }

    @Test
    void listarPlantiosSemUsuarioERecusado() throws Exception
    {
        RespostaDeListagemDePlantios r = enviar(new PedidoDeListagemDePlantios(0), RespostaDeListagemDePlantios.class);
        assertFalse(r.isSucesso());
        assertEquals("Usuario nao identificado. Entre novamente.", r.getMensagem());
    }

    // ================= ATIVIDADES =================
    @Test
    void registrarAtividadeEListar() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");
        int p = cadastrarPlantio(u, LocalDate.now().plusDays(100)).getCodigo();

        RespostaDeRegistroDeAtividade r = registrar(u, p, "IRRIGACAO", "PENDENTE", LocalDate.now().plusDays(5));
        assertTrue(r.isSucesso());
        assertEquals("Atividade registrada com sucesso!", r.getMensagem());

        RespostaDeListagemDeAtividades l = enviar(new PedidoDeListagemDeAtividades(u), RespostaDeListagemDeAtividades.class);
        assertTrue(l.isSucesso());
        assertEquals(5, l.getAtividades().size()); // 4 do cronograma + 1 registrada
    }

    @Test
    void registrarAtividadeInvalida() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");
        int p = cadastrarPlantio(u, LocalDate.now().plusDays(100)).getCodigo();

        assertEquals("Tipo de atividade invalido.",
            registrar(u, p, "PODA", "PENDENTE", LocalDate.now()).getMensagem());
        assertEquals("Uma atividade pendente precisa ter data de hoje ou futura. Use Atrasada ou Concluida.",
            registrar(u, p, "IRRIGACAO", "PENDENTE", LocalDate.now().minusDays(1)).getMensagem());
        assertEquals("Uma atividade concluida nao pode ter data futura.",
            registrar(u, p, "IRRIGACAO", "CONCLUIDA", LocalDate.now().plusDays(1)).getMensagem());
    }

    @Test
    void registrarAtividadeEmPlantioDeOutraPessoaERecusado() throws Exception
    {
        int u1 = cadastrarELogar("maria@x.com");
        int p  = cadastrarPlantio(u1, LocalDate.now().plusDays(100)).getCodigo();
        assertTrue(cadastrar("joao@x.com", "123456").isSucesso());
        int u2 = enviar(new PedidoDeLogin("joao@x.com", "123456"), RespostaDeLogin.class).getCodigo();

        RespostaDeRegistroDeAtividade r = registrar(u2, p, "IRRIGACAO", "PENDENTE", LocalDate.now());
        assertFalse(r.isSucesso());
        assertEquals("Plantio nao encontrado.", r.getMensagem());
    }

    @Test
    void concluirAtividadeAtualizaProgressoDoPlantio() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");
        int p = cadastrarPlantio(u, LocalDate.now().plusDays(100)).getCodigo();
        int a = enviar(new PedidoDeListagemDeAtividades(u), RespostaDeListagemDeAtividades.class)
                    .getAtividades().get(0).getCodigo();

        RespostaDeConclusaoDeAtividade r = enviar(new PedidoDeConclusaoDeAtividade(u, p, a), RespostaDeConclusaoDeAtividade.class);
        assertTrue(r.isSucesso());
        assertEquals("Atividade concluida com sucesso!", r.getMensagem());

        assertEquals(25, enviar(new PedidoDeListagemDePlantios(u), RespostaDeListagemDePlantios.class)
                            .getPlantios().get(0).getProgresso());

        RespostaDeConclusaoDeAtividade de_novo = enviar(new PedidoDeConclusaoDeAtividade(u, p, a), RespostaDeConclusaoDeAtividade.class);
        assertFalse(de_novo.isSucesso());
        assertEquals("Atividade nao encontrada ou ja concluida.", de_novo.getMensagem());
    }

    @Test
    void concluirSemIdentificarAtividadeERecusado() throws Exception
    {
        RespostaDeConclusaoDeAtividade r = enviar(new PedidoDeConclusaoDeAtividade(1, 1, 0), RespostaDeConclusaoDeAtividade.class);
        assertFalse(r.isSucesso());
        assertEquals("Atividade nao identificada.", r.getMensagem());
    }

    // ================= ALERTAS =================
    @Test
    void concluirAtividadeMarcaSeusAlertasComoLidos() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");
        int p = cadastrarPlantio(u, LocalDate.now().plusDays(100)).getCodigo(); // cronograma longe: sem alertas
        int a = registrar(u, p, "COLHEITA", "PENDENTE", LocalDate.now()).getCodigo();

        alertaDao.gerarAlertasDeAtividades(LocalDate.now()); // idempotente: o vigia em segundo plano nao duplica

        RespostaDeListagemDeAlertas antes = enviar(new PedidoDeListagemDeAlertas(u), RespostaDeListagemDeAlertas.class);
        assertEquals(1, antes.getAlertas().size());
        assertEquals("NAO_LIDO", antes.getAlertas().get(0).getSituacao());

        assertTrue(enviar(new PedidoDeConclusaoDeAtividade(u, p, a), RespostaDeConclusaoDeAtividade.class).isSucesso());

        RespostaDeListagemDeAlertas depois = enviar(new PedidoDeListagemDeAlertas(u), RespostaDeListagemDeAlertas.class);
        assertEquals("LIDO", depois.getAlertas().get(0).getSituacao());
    }

    @Test
    void marcarAlertaComoLido() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");
        int p = cadastrarPlantio(u, LocalDate.now().plusDays(100)).getCodigo();
        registrar(u, p, "COLHEITA", "PENDENTE", LocalDate.now());
        alertaDao.gerarAlertasDeAtividades(LocalDate.now());
        int alerta = enviar(new PedidoDeListagemDeAlertas(u), RespostaDeListagemDeAlertas.class).getAlertas().get(0).getCodigo();

        assertTrue(enviar(new PedidoDeMarcacaoDeAlertaLido(u, alerta), RespostaDeMarcacaoDeAlertaLido.class).isSucesso());

        RespostaDeMarcacaoDeAlertaLido inexistente = enviar(new PedidoDeMarcacaoDeAlertaLido(u, 9999), RespostaDeMarcacaoDeAlertaLido.class);
        assertFalse(inexistente.isSucesso());
        assertEquals("Alerta nao encontrado.", inexistente.getMensagem());

        assertEquals("Alerta nao identificado.",
            enviar(new PedidoDeMarcacaoDeAlertaLido(u, 0), RespostaDeMarcacaoDeAlertaLido.class).getMensagem());
    }

    // ================= ALERTA CLIMATICO =================
    private RespostaDeEnvioDeAlertaClimatico climatico(int u, int p, String chave, String data) throws Exception
    {
        return enviar(new PedidoDeEnvioDeAlertaClimatico(u, p, chave, "Geada", "Risco de geada", "ALTA", data),
                      RespostaDeEnvioDeAlertaClimatico.class);
    }

    @Test
    void alertaClimaticoInvalido() throws Exception
    {
        assertEquals("Alerta climatico invalido.", climatico(0, 1, "k", "2026-10-28").getMensagem());

        int u = cadastrarELogar("maria@x.com");
        int p = cadastrarPlantio(u, LocalDate.now().plusDays(100)).getCodigo();
        assertEquals("Data do alerta invalida.", climatico(u, p, "k", "amanha").getMensagem());
        assertEquals("Plantio nao encontrado.", climatico(u, 999, "k", "2026-10-28").getMensagem());
    }

    @Test
    void alertaClimaticoEnviaEmailUmaUnicaVez() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");
        int p = cadastrarPlantio(u, LocalDate.now().plusDays(100)).getCodigo();
        when(email.enviarAlerta(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(true);

        assertEquals("Alerta climatico registrado.", climatico(u, p, "geada-1", "2026-10-28").getMensagem());
        assertEquals("Alerta ja processado.",        climatico(u, p, "geada-1", "2026-10-28").getMensagem());

        verify(email, times(1)).enviarAlerta(eq("maria@x.com"), eq("Alerta climatico"), eq("Geada"),
            eq("Risco de geada"), eq("Soja T1"), eq("2026-10-28"), eq("ALTA"));
    }

    @Test
    void alertaClimaticoRespeitaPreferenciaDeNaoReceberEmail() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");
        int p = cadastrarPlantio(u, LocalDate.now().plusDays(100)).getCodigo();
        enviar(new PedidoDeAtualizacaoDePreferencias(u, false), RespostaDeConta.class);

        assertEquals("Alertas climaticos por e-mail desativados.", climatico(u, p, "k1", "2026-10-28").getMensagem());
        verify(email, never()).enviarAlerta(any(), any(), any(), any(), any(), any(), any());
    }

    // ================= RECUPERACAO DE SENHA =================
    @Test
    void fluxoCompletoDeRecuperacaoDeSenha() throws Exception
    {
        cadastrar("maria@x.com", "123456");

        RespostaDeRecuperacaoDeSenha r = enviar(
            new PedidoDeRecuperacaoDeSenha("maria@x.com", "http://localhost:8080/"), RespostaDeRecuperacaoDeSenha.class);
        assertTrue(r.isSucesso());

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(email, timeout(3000)).enviarRecuperacao(eq("Maria Silva"), eq("maria@x.com"), link.capture());
        String prefixo = "http://localhost:8080/pages/auth/redefinirSenha.html?token=";
        assertTrue(link.getValue().startsWith(prefixo));
        String token = link.getValue().substring(prefixo.length());

        RespostaDeRedefinicaoDeSenha ok = enviar(new PedidoDeRedefinicaoDeSenha(token, "novaSenha1"), RespostaDeRedefinicaoDeSenha.class);
        assertTrue(ok.isSucesso());
        assertEquals("Senha alterada com sucesso!", ok.getMensagem());

        assertTrue(enviar(new PedidoDeLogin("maria@x.com", "novaSenha1"), RespostaDeLogin.class).isSucesso());
        assertFalse(enviar(new PedidoDeLogin("maria@x.com", "123456"), RespostaDeLogin.class).isSucesso());

        // o link vale uma vez so
        RespostaDeRedefinicaoDeSenha repetido = enviar(new PedidoDeRedefinicaoDeSenha(token, "outraSenha2"), RespostaDeRedefinicaoDeSenha.class);
        assertFalse(repetido.isSucesso());
        assertEquals("Link invalido ou expirado. Solicite um novo.", repetido.getMensagem());
    }

    @Test
    void recuperacaoComEmailInexistenteDaMesmaRespostaEnaoEnviaEmail() throws Exception
    {
        cadastrar("maria@x.com", "123456");
        RespostaDeRecuperacaoDeSenha existe = enviar(new PedidoDeRecuperacaoDeSenha("maria@x.com", "http://localhost:8080"), RespostaDeRecuperacaoDeSenha.class);
        RespostaDeRecuperacaoDeSenha nao    = enviar(new PedidoDeRecuperacaoDeSenha("outro@x.com", "http://localhost:8080"), RespostaDeRecuperacaoDeSenha.class);

        assertTrue(nao.isSucesso());
        assertEquals(existe.getMensagem(), nao.getMensagem());
        verify(email, timeout(3000).times(1)).enviarRecuperacao(any(), any(), any()); // so para quem existe
    }

    @Test
    void recuperacaoComSiteNaoPermitidoERecusada() throws Exception
    {
        cadastrar("maria@x.com", "123456");
        RespostaDeRecuperacaoDeSenha r = enviar(new PedidoDeRecuperacaoDeSenha("maria@x.com", "http://site-falso.com"), RespostaDeRecuperacaoDeSenha.class);
        assertFalse(r.isSucesso());
        verify(email, never()).enviarRecuperacao(any(), any(), any());
    }

    @Test
    void recuperacaoComEmailInvalido() throws Exception
    {
        RespostaDeRecuperacaoDeSenha r = enviar(new PedidoDeRecuperacaoDeSenha("invalido", "http://localhost:8080"), RespostaDeRecuperacaoDeSenha.class);
        assertFalse(r.isSucesso());
        assertEquals("Informe um e-mail valido.", r.getMensagem());
    }

    @Test
    void redefinirComSenhaCurtaOuTokenInvalido() throws Exception
    {
        assertEquals("A senha deve ter pelo menos 6 caracteres.",
            enviar(new PedidoDeRedefinicaoDeSenha("tk", "123"), RespostaDeRedefinicaoDeSenha.class).getMensagem());
        assertEquals("Link invalido ou expirado. Solicite um novo.",
            enviar(new PedidoDeRedefinicaoDeSenha("", "123456"), RespostaDeRedefinicaoDeSenha.class).getMensagem());
        assertEquals("Link invalido ou expirado. Solicite um novo.",
            enviar(new PedidoDeRedefinicaoDeSenha("token-que-nao-existe", "123456"), RespostaDeRedefinicaoDeSenha.class).getMensagem());
    }

    // ================= CONFIGURACOES DA CONTA =================
    @Test
    void buscarEAtualizarPerfil() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");

        RespostaDeBuscaDePerfil p = enviar(new PedidoDeBuscaDePerfil(u), RespostaDeBuscaDePerfil.class);
        assertTrue(p.isSucesso());
        assertEquals("Maria Silva", p.getNome());
        assertTrue(p.isReceberAlertasPorEmail());

        RespostaDeConta a = enviar(new PedidoDeAtualizacaoDePerfil(u, "Maria Souza", "NOVO@x.com", ""), RespostaDeConta.class);
        assertTrue(a.isSucesso());

        RespostaDeBuscaDePerfil depois = enviar(new PedidoDeBuscaDePerfil(u), RespostaDeBuscaDePerfil.class);
        assertEquals("Maria Souza", depois.getNome());
        assertEquals("novo@x.com", depois.getEmail());
        assertNull(depois.getTelefone()); // telefone vazio vira "sem telefone"
    }

    @Test
    void atualizarPerfilComEmailDeOutraContaERecusado() throws Exception
    {
        cadastrar("joao@x.com", "123456");
        int u = cadastrarELogar("maria@x.com");

        RespostaDeConta r = enviar(new PedidoDeAtualizacaoDePerfil(u, "Maria", "joao@x.com", null), RespostaDeConta.class);
        assertFalse(r.isSucesso());
        assertEquals("Este e-mail ja esta cadastrado.", r.getMensagem());
    }

    @Test
    void buscarPerfilDeUsuarioInexistente() throws Exception
    {
        RespostaDeBuscaDePerfil r = enviar(new PedidoDeBuscaDePerfil(999), RespostaDeBuscaDePerfil.class);
        assertFalse(r.isSucesso());
        assertEquals("Usuario nao encontrado. Entre novamente.", r.getMensagem());
    }

    @Test
    void preferenciaDeAlertasPorEmailEhSalva() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");
        assertEquals("Usuario nao identificado. Entre novamente.",
            enviar(new PedidoDeAtualizacaoDePreferencias(0, true), RespostaDeConta.class).getMensagem());

        assertTrue(enviar(new PedidoDeAtualizacaoDePreferencias(u, false), RespostaDeConta.class).isSucesso());
        assertFalse(enviar(new PedidoDeBuscaDePerfil(u), RespostaDeBuscaDePerfil.class).isReceberAlertasPorEmail());
    }

    @Test
    void alterarSenha() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");

        assertEquals("Informe a senha atual.",
            enviar(new PedidoDeAlteracaoDeSenha(u, "", "654321"), RespostaDeConta.class).getMensagem());
        assertEquals("Senha atual incorreta.",
            enviar(new PedidoDeAlteracaoDeSenha(u, "errada1", "654321"), RespostaDeConta.class).getMensagem());
        assertEquals("A nova senha deve ser diferente da atual.",
            enviar(new PedidoDeAlteracaoDeSenha(u, "123456", "123456"), RespostaDeConta.class).getMensagem());
        assertEquals("A senha deve ter pelo menos 6 caracteres.",
            enviar(new PedidoDeAlteracaoDeSenha(u, "123456", "123"), RespostaDeConta.class).getMensagem());

        assertTrue(enviar(new PedidoDeAlteracaoDeSenha(u, "123456", "654321"), RespostaDeConta.class).isSucesso());
        assertTrue(enviar(new PedidoDeLogin("maria@x.com", "654321"), RespostaDeLogin.class).isSucesso());
        assertFalse(enviar(new PedidoDeLogin("maria@x.com", "123456"), RespostaDeLogin.class).isSucesso());
    }

    @Test
    void excluirContaExigeSenhaCorretaEImpedeNovoLogin() throws Exception
    {
        int u = cadastrarELogar("maria@x.com");

        assertEquals("Informe sua senha para confirmar a exclusao.",
            enviar(new PedidoDeExclusaoDeConta(u, ""), RespostaDeConta.class).getMensagem());
        assertEquals("Senha incorreta.",
            enviar(new PedidoDeExclusaoDeConta(u, "errada1"), RespostaDeConta.class).getMensagem());
        assertTrue(enviar(new PedidoDeLogin("maria@x.com", "123456"), RespostaDeLogin.class).isSucesso());

        RespostaDeConta ok = enviar(new PedidoDeExclusaoDeConta(u, "123456"), RespostaDeConta.class);
        assertTrue(ok.isSucesso());
        assertEquals("Conta excluida.", ok.getMensagem());

        assertFalse(enviar(new PedidoDeLogin("maria@x.com", "123456"), RespostaDeLogin.class).isSucesso());
        // exclusao logica: o registro continua no banco, so fica inativo
        assertFalse(bd.banco().getCollection("usuarios").find().first().getBoolean("ativo"));
    }

    // ================= CONEXAO =================
    @Test
    void pedidoParaSairEncerraAConexaoERemoveDaLista() throws Exception
    {
        // faz um pedido para garantir que a supervisora ja registrou o cliente
        cadastrar("maria@x.com", "123456");
        synchronized (conectados) { assertEquals(1, conectados.size()); }

        saida.writeObject(new PedidoParaSair());
        saida.flush();

        long limite = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < limite)
        {
            synchronized (conectados) { if (conectados.isEmpty()) break; }
            Thread.sleep(50);
        }
        synchronized (conectados) { assertTrue(conectados.isEmpty()); }
    }

    @Test
    void construtorExigeTodasAsDependencias() throws Exception
    {
        Socket s = new Socket();
        assertThrows(Exception.class, () -> new SupervisoraDeConexao(null, conectados, usuarioDao, plantioDao, alertaDao, email));
        assertThrows(Exception.class, () -> new SupervisoraDeConexao(s, null, usuarioDao, plantioDao, alertaDao, email));
        assertThrows(Exception.class, () -> new SupervisoraDeConexao(s, conectados, null, plantioDao, alertaDao, email));
        assertThrows(Exception.class, () -> new SupervisoraDeConexao(s, conectados, usuarioDao, null, alertaDao, email));
        assertThrows(Exception.class, () -> new SupervisoraDeConexao(s, conectados, usuarioDao, plantioDao, null, email));
        assertThrows(Exception.class, () -> new SupervisoraDeConexao(s, conectados, usuarioDao, plantioDao, alertaDao, null));
    }
}
