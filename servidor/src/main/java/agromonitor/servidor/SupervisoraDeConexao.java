// Autoria: Beatriz Leme — RA 25015554.
package agromonitor.servidor;

import java.io.*;   // fornece os streams (ObjectInputStrem ...)
import java.net.*; // fornece o socket
import java.time.LocalDate; 
import java.util.*; // fornece ArrayList, set, HashSet, Date

import at.favre.lib.crypto.bcrypt.BCrypt; // Biblioteca de hash de senha

//Traz os pedidos e respostas, que são a "conversa" entre cliente e servidor
import agromonitor.comum.*;
import agromonitor.comum.alerta.*;
import agromonitor.comum.atividade.*;
import agromonitor.comum.auth.*;
import agromonitor.comum.conta.*;
import agromonitor.comum.plantio.*;

// Traz o banco, as classes de dados, e as checagens de campos
import agromonitor.servidor.dao.*;
import agromonitor.servidor.email.EmailService;
import agromonitor.servidor.modelo.*;
import agromonitor.servidor.validacao.*;

// Uma thread por cliente conectado. Recebe os comunicados e atende cada um.
// No exemplo do professor o "if instanceof" fazia a conta; aqui ele chama
// validacao + MongoDB (cadastro e login).
public class SupervisoraDeConexao extends Thread
{
    private Parceiro            usuario;
    private Socket              conexao;
    private ArrayList<Parceiro> usuarios;
    private UsuarioDAO          dao;
    private PlantioDAO          plantioDao;
    private AlertaDAO           alertaDao;
    private ProcessadorDeAlertas processador;
    private EmailService        email;

    private static final int VALIDADE_TOKEN_MINUTOS     = 30;
    private static final int INTERVALO_REENVIO_SEGUNDOS = 60;

    // Enderecos do site que podem aparecer no link do e-mail (separe por virgula em APP_URLS_PERMITIDAS)
    private static final Set<String> BASES_PERMITIDAS = carregarBasesPermitidas();

    private static Set<String> carregarBasesPermitidas()
    {
        String env   = System.getenv("APP_URLS_PERMITIDAS");
        String lista = (env == null || env.isBlank())
            ? "http://localhost:8080,http://127.0.0.1:8080,http://localhost:5500,http://127.0.0.1:5500"
            : env;

        Set<String> bases = new HashSet<String>();
        for (String b : lista.split(","))
        {
            b = b.trim();
            while (b.endsWith("/"))
                b = b.substring(0, b.length() - 1);
            if (!b.isEmpty())
                bases.add(b.toLowerCase());
        }
        return bases;
    }

    public SupervisoraDeConexao(Socket conexao, ArrayList<Parceiro> usuarios, UsuarioDAO dao,
                                PlantioDAO plantioDao, AlertaDAO alertaDao, EmailService email) throws Exception
    {
        if (conexao == null)
            throw new Exception("Conexao ausente");
        if (usuarios == null)
            throw new Exception("Usuarios ausentes");
        if (dao == null)
            throw new Exception("DAO ausente");
       if (plantioDao == null)
            throw new Exception("DAO de plantio ausente");
        if (alertaDao == null)
            throw new Exception("DAO de alerta ausente");
        if (email == null)
            throw new Exception("Servico de e-mail ausente");

        this.conexao    = conexao;
        this.usuarios   = usuarios;
        this.dao        = dao;
        this.plantioDao = plantioDao;
        this.alertaDao  = alertaDao;
        this.email      = email;
        this.processador = new ProcessadorDeAlertas(alertaDao, dao, email);
    }

    public void run()
    {
        // Cada instância atende um cliente: lê comunicados do socket e devolve a resposta correspondente.
        ObjectOutputStream transmissor;
        try
        {
            transmissor = new ObjectOutputStream(this.conexao.getOutputStream());
            transmissor.flush();
        }
        catch (Exception erro)
        {
            return;
        }

        ObjectInputStream receptor = null;
        try
        {
            receptor = new ObjectInputStream(this.conexao.getInputStream());
            // O filtro limita os tipos que podem ser reconstruídos a partir da rede.
            // seguranca: so aceita desserializar classes do nosso pacote
            receptor.setObjectInputFilter(
                ObjectInputFilter.Config.createFilter("agromonitor.comum.**;java.lang.*;!*"));
        }
        catch (Exception erro)
        {
            try { transmissor.close(); } catch (Exception falha) {}
            return;
        }

        try
        {
            this.usuario = new Parceiro(this.conexao, receptor, transmissor);
        }
        catch (Exception erro)
        {} // sei que passei parametros corretos

        synchronized (this.usuarios)
        {
            this.usuarios.add(this.usuario);
        }

        try
        {
            for (;;)
            {
                Comunicado comunicado = this.usuario.envie();

                // O tipo do comunicado determina qual regra de negócio atende a solicitação.
                if (comunicado == null)
                    return;
                else if (comunicado instanceof PedidoDeCadastro)
                    this.usuario.receba(tratarCadastro((PedidoDeCadastro) comunicado));
                else if (comunicado instanceof PedidoDeLogin)
                    this.usuario.receba(tratarLogin((PedidoDeLogin) comunicado));
                else if (comunicado instanceof PedidoDeCadastroDePlantio)
                    this.usuario.receba(tratarCadastroDePlantio((PedidoDeCadastroDePlantio) comunicado));
                else if (comunicado instanceof PedidoDeListagemDePlantios)
                    this.usuario.receba(tratarListagemDePlantios((PedidoDeListagemDePlantios) comunicado));
                else if (comunicado instanceof PedidoDeRegistroDeAtividade)
                    this.usuario.receba(tratarRegistroDeAtividade((PedidoDeRegistroDeAtividade) comunicado));
                else if (comunicado instanceof PedidoDeConclusaoDeAtividade)
                    this.usuario.receba(tratarConclusaoDeAtividade((PedidoDeConclusaoDeAtividade) comunicado));
                else if (comunicado instanceof PedidoDeListagemDeAtividades)
                    this.usuario.receba(tratarListagemDeAtividades((PedidoDeListagemDeAtividades) comunicado));
                else if (comunicado instanceof PedidoDeListagemDeAlertas)
                    this.usuario.receba(tratarListagemDeAlertas((PedidoDeListagemDeAlertas) comunicado));
                else if (comunicado instanceof PedidoDeMarcacaoDeAlertaLido)
                    this.usuario.receba(tratarMarcacaoDeAlertaLido((PedidoDeMarcacaoDeAlertaLido) comunicado));
                else if (comunicado instanceof PedidoDeEnvioDeAlertaClimatico)
                    this.usuario.receba(tratarEnvioDeAlertaClimatico((PedidoDeEnvioDeAlertaClimatico) comunicado));
                else if (comunicado instanceof PedidoDeRecuperacaoDeSenha)
                    this.usuario.receba(tratarRecuperacao((PedidoDeRecuperacaoDeSenha) comunicado));
                else if (comunicado instanceof PedidoDeRedefinicaoDeSenha)
                    this.usuario.receba(tratarRedefinicao((PedidoDeRedefinicaoDeSenha) comunicado));
                else if (comunicado instanceof PedidoDeBuscaDePerfil)
                    this.usuario.receba(tratarBuscaDePerfil((PedidoDeBuscaDePerfil) comunicado));
                else if (comunicado instanceof PedidoDeAtualizacaoDePerfil)
                    this.usuario.receba(tratarAtualizacaoDePerfil((PedidoDeAtualizacaoDePerfil) comunicado));
                else if (comunicado instanceof PedidoDeAtualizacaoDePreferencias)
                    this.usuario.receba(tratarAtualizacaoDePreferencias((PedidoDeAtualizacaoDePreferencias) comunicado));
                else if (comunicado instanceof PedidoDeAlteracaoDeSenha)
                    this.usuario.receba(tratarAlteracaoDeSenha((PedidoDeAlteracaoDeSenha) comunicado));
                else if (comunicado instanceof PedidoDeExclusaoDeConta)
                    this.usuario.receba(tratarExclusaoDeConta((PedidoDeExclusaoDeConta) comunicado));
                else if (comunicado instanceof PedidoParaSair)
                {
                    synchronized (this.usuarios)
                    {
                        this.usuarios.remove(this.usuario);
                    }
                    this.usuario.adeus();
                    return;
                }
            }
        }
        catch (Exception erro)
        {
            // cliente caiu ou erro de comunicacao: tira da lista e encerra
            try
            {
                synchronized (this.usuarios)
                {
                    this.usuarios.remove(this.usuario);
                }
                this.usuario.adeus();
            }
            catch (Exception falha)
            {}
        }
    }

    private RespostaDeCadastro tratarCadastro(PedidoDeCadastro p)
    {
        String erro = primeiroErro(
            ValidadorUsuario.validarNome(p.getNome()),
            ValidadorUsuario.validarEmail(p.getEmail()),
            ValidadorUsuario.validarCpf(p.getCpf()),
            ValidadorUsuario.validarTelefone(p.getTelefone()),
            ValidadorUsuario.validarSenha(p.getSenha()));
        if (erro != null)
            return new RespostaDeCadastro(false, erro, 0);

        String email    = p.getEmail().trim().toLowerCase();
        String cpf      = ValidadorUsuario.somenteDigitos(p.getCpf());
        String telefone = p.getTelefone() == null ? "" : p.getTelefone().trim();

        try
        {
            String hash = BCrypt.withDefaults().hashToString(12, p.getSenha().toCharArray());
            int codigo = this.dao.inserir(
                p.getNome().trim(), email,
                telefone.isEmpty() ? null : telefone,
                cpf.isEmpty() ? null : cpf,
                hash);
            return new RespostaDeCadastro(true, "Cadastro realizado com sucesso!", codigo);
        }
        catch (UsuarioDAO.EmailJaCadastradoException e)
        {
            return new RespostaDeCadastro(false, "Este e-mail ja esta cadastrado.", 0);
        }
        catch (Exception e)
        {
            System.err.println("Erro ao cadastrar: " + e);
            return new RespostaDeCadastro(false, "Erro interno ao cadastrar. Tente novamente.", 0);
        }
    }

    private RespostaDeLogin tratarLogin(PedidoDeLogin p)
    {
        final String falha = "E-mail ou senha incorretos.";

        if (ValidadorUsuario.validarEmail(p.getEmail()) != null || p.getSenha() == null || p.getSenha().isEmpty())
            return new RespostaDeLogin(false, falha, 0, null, null);

        try
        {
            Usuario u = this.dao.buscarAtivoPorEmail(p.getEmail().trim().toLowerCase());
            if (u == null || !BCrypt.verifyer().verify(p.getSenha().toCharArray(), u.getSenhaHash()).verified)
                return new RespostaDeLogin(false, falha, 0, null, null);

            return new RespostaDeLogin(true, "Login realizado com sucesso!", u.getCodigo(), u.getNome(), u.getEmail());
        }
        catch (Exception e)
        {
            System.err.println("Erro no login: " + e);
            return new RespostaDeLogin(false, "Erro interno ao entrar. Tente novamente.", 0, null, null);
        }
    }

    private RespostaDeCadastroDePlantio tratarCadastroDePlantio(PedidoDeCadastroDePlantio p)
    {
        String erro = primeiroErro(
            ValidadorPlantio.validarUsuario(p.getUsuarioCodigo()),
            ValidadorPlantio.validarCultura(p.getCultura()),
            ValidadorPlantio.validarNome(p.getNome()),
            ValidadorPlantio.validarSafra(p.getSafra()),
            ValidadorPlantio.validarDescricao(p.getDescricao()),
            ValidadorPlantio.validarTalhao(p.getTalhao()),
            ValidadorPlantio.validarAreaPlantada(p.getAreaPlantada()),
            ValidadorPlantio.validarSistemaCultivo(p.getSistemaCultivo()),
            ValidadorPlantio.validarAltitude(p.getAltitude()),
            ValidadorPlantio.validarDataPlantio(p.getDataPlantio()),
            ValidadorPlantio.validarMetodoPlantio(p.getMetodoPlantio()),
            ValidadorPlantio.validarAjuste(p.getAjustePlantio(),   "plantio"),
            ValidadorPlantio.validarAjuste(p.getAjusteIrrigacao(), "irrigacao"),
            ValidadorPlantio.validarAjuste(p.getAjusteAdubacao(),  "adubacao"),
            ValidadorPlantio.validarAjuste(p.getAjusteTratos(),    "tratos culturais"));
        if (erro != null)
            return new RespostaDeCadastroDePlantio(false, erro, 0);

        try
        {
            // o dono do plantio precisa existir e estar ativo
            if (!this.dao.existeAtivoPorCodigo(p.getUsuarioCodigo()))
                return new RespostaDeCadastroDePlantio(false, "Usuario nao encontrado. Entre novamente.", 0);

            String descricao = p.getDescricao() == null ? "" : p.getDescricao().trim();
            String local     = p.getLocalizacao() == null ? "" : p.getLocalizacao().trim();
            String metodo    = p.getMetodoPlantio() == null ? "" : p.getMetodoPlantio().trim();

            // ajustes opcionais: data valida ou null (null = previsao automatica)
            LocalDate[] ajustes = new LocalDate[4];
            ajustes[0] = ValidadorPlantio.lerData(vazioParaNull(p.getAjustePlantio()));
            ajustes[1] = ValidadorPlantio.lerData(vazioParaNull(p.getAjusteIrrigacao()));
            ajustes[2] = ValidadorPlantio.lerData(vazioParaNull(p.getAjusteAdubacao()));
            ajustes[3] = ValidadorPlantio.lerData(vazioParaNull(p.getAjusteTratos()));

            int codigo = this.plantioDao.inserir(
                p.getUsuarioCodigo(),
                p.getCultura().trim(),
                p.getNome().trim(),
                p.getSafra().trim(),
                descricao.isEmpty() ? null : descricao,
                p.getSistemaCultivo(),
                p.getTalhao().trim(),
                p.getAreaPlantada(),
                local.isEmpty() ? null : local,
                p.getAltitude(),
                ValidadorPlantio.lerData(p.getDataPlantio()),
                metodo.isEmpty() ? null : metodo,
                ajustes);

            // o plantio ja nasce com atividades (algumas podem estar para hoje ou atrasadas):
            // gera os alertas agora, sem esperar o vigia
            this.processador.verificarEmSegundoPlano();

            return new RespostaDeCadastroDePlantio(true, "Plantio cadastrado com sucesso!", codigo);
        }
        catch (Exception e)
        {
            System.err.println("Erro ao cadastrar plantio: " + e);
            return new RespostaDeCadastroDePlantio(false, "Erro interno ao cadastrar o plantio. Tente novamente.", 0);
        }
    }

    private RespostaDeListagemDePlantios tratarListagemDePlantios(PedidoDeListagemDePlantios p)
    {
        String erro = ValidadorPlantio.validarUsuario(p.getUsuarioCodigo());
        if (erro != null)
            return new RespostaDeListagemDePlantios(false, erro, null);

        try
        {
            return new RespostaDeListagemDePlantios(true, "OK", this.plantioDao.listarPorUsuario(p.getUsuarioCodigo()));
        }
        catch (Exception e)
        {
            System.err.println("Erro ao listar plantios: " + e);
            return new RespostaDeListagemDePlantios(false, "Erro interno ao buscar os plantios. Tente novamente.", null);
        }
    }

    private RespostaDeRegistroDeAtividade tratarRegistroDeAtividade(PedidoDeRegistroDeAtividade p)
    {
        String erro = primeiroErro(
            ValidadorAtividade.validarIds(p.getUsuarioCodigo(), p.getPlantioCodigo()),
            ValidadorAtividade.validarTipo(p.getTipoAtividade()),
            ValidadorAtividade.validarStatus(p.getStatus()),
            ValidadorAtividade.validarData(p.getDataExecucao(), p.getStatus()),
            ValidadorAtividade.validarQuantidade(p.getQuantidade()),
            ValidadorAtividade.validarResponsavel(p.getResponsavel()),
            ValidadorAtividade.validarObservacoes(p.getObservacoes()));
        if (erro != null)
            return new RespostaDeRegistroDeAtividade(false, erro, 0);

        try
        {
            String quantidade  = p.getQuantidade()  == null ? "" : p.getQuantidade().trim();
            String responsavel = p.getResponsavel() == null ? "" : p.getResponsavel().trim();
            String observacoes = p.getObservacoes() == null ? "" : p.getObservacoes().trim();

            int codigo = this.plantioDao.registrarAtividade(
                p.getUsuarioCodigo(),
                p.getPlantioCodigo(),
                p.getTipoAtividade(),
                p.getStatus(),
                ValidadorPlantio.lerData(p.getDataExecucao()),
                quantidade.isEmpty()  ? null : quantidade,
                responsavel.isEmpty() ? null : responsavel,
                observacoes.isEmpty() ? null : observacoes);

            if (codigo == 0)
                return new RespostaDeRegistroDeAtividade(false, "Plantio nao encontrado.", 0);

            // gera o alerta (e o e-mail) agora, sem esperar o vigia
            this.processador.verificarEmSegundoPlano();

            return new RespostaDeRegistroDeAtividade(true, "Atividade registrada com sucesso!", codigo);
        }
        catch (Exception e)
        {
            System.err.println("Erro ao registrar atividade: " + e);
            return new RespostaDeRegistroDeAtividade(false, "Erro interno ao registrar a atividade. Tente novamente.", 0);
        }
    }

    private RespostaDeConclusaoDeAtividade tratarConclusaoDeAtividade(PedidoDeConclusaoDeAtividade p)
    {
        String erro = ValidadorAtividade.validarIds(p.getUsuarioCodigo(), p.getPlantioCodigo());
        if (erro == null && p.getAtividadeCodigo() <= 0)
            erro = "Atividade nao identificada.";
        if (erro != null)
            return new RespostaDeConclusaoDeAtividade(false, erro);

        try
        {
            if (!this.plantioDao.concluirAtividade(p.getUsuarioCodigo(), p.getPlantioCodigo(), p.getAtividadeCodigo()))
                return new RespostaDeConclusaoDeAtividade(false, "Atividade nao encontrada ou ja concluida.");

            // os alertas de uma atividade ja feita nao fazem mais sentido: marca como lidos
            try
            {
                this.alertaDao.marcarLidosDaAtividade(p.getAtividadeCodigo());
            }
            catch (Exception e)
            {
                System.err.println("Erro ao marcar os alertas da atividade como lidos: " + e);
            }

            return new RespostaDeConclusaoDeAtividade(true, "Atividade concluida com sucesso!");
        }
        catch (Exception e)
        {
            System.err.println("Erro ao concluir atividade: " + e);
            return new RespostaDeConclusaoDeAtividade(false, "Erro interno ao concluir a atividade. Tente novamente.");
        }
    }

    private RespostaDeListagemDeAtividades tratarListagemDeAtividades(PedidoDeListagemDeAtividades p)
    {
        String erro = ValidadorPlantio.validarUsuario(p.getUsuarioCodigo());
        if (erro != null)
            return new RespostaDeListagemDeAtividades(false, erro, null);

        try
        {
            return new RespostaDeListagemDeAtividades(true, "OK",
                this.plantioDao.listarAtividadesPorUsuario(p.getUsuarioCodigo()));
        }
        catch (Exception e)
        {
            System.err.println("Erro ao listar atividades: " + e);
            return new RespostaDeListagemDeAtividades(false, "Erro interno ao buscar as atividades. Tente novamente.", null);
        }
    }

    private RespostaDeListagemDeAlertas tratarListagemDeAlertas(PedidoDeListagemDeAlertas p)
    {
        String erro = ValidadorPlantio.validarUsuario(p.getUsuarioCodigo());
        if (erro != null)
            return new RespostaDeListagemDeAlertas(false, erro, null);

        try
        {
            return new RespostaDeListagemDeAlertas(true, "OK", this.alertaDao.listarPorUsuario(p.getUsuarioCodigo()));
        }
        catch (Exception e)
        {
            System.err.println("Erro ao listar alertas: " + e);
            return new RespostaDeListagemDeAlertas(false, "Erro interno ao buscar os alertas. Tente novamente.", null);
        }
    }

    private RespostaDeMarcacaoDeAlertaLido tratarMarcacaoDeAlertaLido(PedidoDeMarcacaoDeAlertaLido p)
    {
        String erro = ValidadorPlantio.validarUsuario(p.getUsuarioCodigo());
        if (erro == null && p.getAlertaCodigo() <= 0)
            erro = "Alerta nao identificado.";
        if (erro != null)
            return new RespostaDeMarcacaoDeAlertaLido(false, erro);

        try
        {
            if (!this.alertaDao.marcarComoLido(p.getUsuarioCodigo(), p.getAlertaCodigo()))
                return new RespostaDeMarcacaoDeAlertaLido(false, "Alerta nao encontrado.");
            return new RespostaDeMarcacaoDeAlertaLido(true, "Alerta marcado como lido.");
        }
        catch (Exception e)
        {
            System.err.println("Erro ao marcar alerta como lido: " + e);
            return new RespostaDeMarcacaoDeAlertaLido(false, "Erro interno ao marcar o alerta como lido. Tente novamente.");
        }
    }

    private RespostaDeEnvioDeAlertaClimatico tratarEnvioDeAlertaClimatico(PedidoDeEnvioDeAlertaClimatico p)
    {
        if (p.getUsuarioCodigo() <= 0 || p.getPlantioCodigo() <= 0
                || p.getChave() == null || p.getChave().length() > 160
                || p.getTitulo() == null || p.getTitulo().isBlank() || p.getTitulo().length() > 160
                || p.getDescricao() == null || p.getDescricao().isBlank() || p.getDescricao().length() > 1000
                || p.getPrioridade() == null || p.getData() == null)
            return new RespostaDeEnvioDeAlertaClimatico(false, "Alerta climatico invalido.");

        final LocalDate data;
        try { data = LocalDate.parse(p.getData()); }
        catch (Exception erro) { return new RespostaDeEnvioDeAlertaClimatico(false, "Data do alerta invalida."); }

        try
        {
            String nomePlantio = this.plantioDao.nomeAtivoDoUsuario(p.getUsuarioCodigo(), p.getPlantioCodigo());
            if (nomePlantio == null)
                return new RespostaDeEnvioDeAlertaClimatico(false, "Plantio nao encontrado.");
            if (!this.dao.existeAtivoPorCodigo(p.getUsuarioCodigo()))
                return new RespostaDeEnvioDeAlertaClimatico(false, "Usuario nao encontrado.");

            boolean pendente = this.alertaDao.registrarAlertaClimaticoEmail(
                p.getChave(), p.getUsuarioCodigo(), p.getPlantioCodigo(), nomePlantio, p.getTitulo(),
                p.getDescricao(), p.getPrioridade(), data.toString());
            if (!pendente)
                return new RespostaDeEnvioDeAlertaClimatico(true, "Alerta ja processado.");

            String destino = this.dao.emailParaAlerta(p.getUsuarioCodigo());
            if (destino == null)
            {
                this.alertaDao.marcarEmailClimaticoProcessado(p.getChave());
                return new RespostaDeEnvioDeAlertaClimatico(true, "Alertas climaticos por e-mail desativados.");
            }

            boolean enviado = this.email.enviarAlerta(destino, "Alerta climatico", p.getTitulo(),
                p.getDescricao(), nomePlantio, data.toString(), p.getPrioridade());
            if (enviado)
                this.alertaDao.marcarEmailClimaticoProcessado(p.getChave());
            else
                System.err.println("Alerta climatico pendente: SMTP nao configurado.");
            return new RespostaDeEnvioDeAlertaClimatico(true, "Alerta climatico registrado.");
        }
        catch (Exception erro)
        {
            System.err.println("Erro ao processar alerta climatico por e-mail: " + erro);
            return new RespostaDeEnvioDeAlertaClimatico(false, "Nao foi possivel processar o alerta climatico.");
        }
    }

    // lerData nao aceita null; este auxiliar devolve "" quando nao veio nada
    private static String vazioParaNull(String texto)
    {
        return texto == null ? "" : texto;
    }

    // "Esqueci minha senha": gera o token, guarda o hash no banco e envia o link por e-mail.
    private RespostaDeRecuperacaoDeSenha tratarRecuperacao(PedidoDeRecuperacaoDeSenha p)
    {
        // mesma mensagem exista o e-mail ou nao: ninguem descobre quem esta cadastrado
        final String generica = "Se o e-mail estiver cadastrado, enviamos um link para redefinir a senha. "
                              + "Confira tambem a caixa de spam.";

        if (ValidadorUsuario.validarEmail(p.getEmail()) != null)
            return new RespostaDeRecuperacaoDeSenha(false, "Informe um e-mail valido.");

        String base = p.getUrlBase() == null ? "" : p.getUrlBase().trim();
        while (base.endsWith("/"))
            base = base.substring(0, base.length() - 1);
        // seguranca: o link do e-mail so pode apontar para um site nosso. Se aceitasse qualquer
        // endereco, alguem poderia pedir a recuperacao da conta de outra pessoa com um site falso
        // e a vitima receberia um e-mail verdadeiro com o link do site falso.
        if (!BASES_PERMITIDAS.contains(base.toLowerCase()))
        {
            System.err.println("urlBase NAO permitida no pedido de recuperacao: " + base
                + "  (permitidas: " + BASES_PERMITIDAS + "; para mudar, defina APP_URLS_PERMITIDAS)");
            return new RespostaDeRecuperacaoDeSenha(false, "Erro interno ao enviar o e-mail. Tente novamente.");
        }

        try
        {
            String token = TokenRecuperacao.gerar();
            long   agora = System.currentTimeMillis();

            Usuario u = this.dao.salvarTokenDeRecuperacao(
                p.getEmail().trim().toLowerCase(),
                TokenRecuperacao.hash(token),
                new Date(agora + VALIDADE_TOKEN_MINUTOS * 60_000L),
                new Date(agora - INTERVALO_REENVIO_SEGUNDOS * 1000L));

            if (u != null)
            {
                final String link    = base + "/pages/auth/redefinirSenha.html?token=" + token;
                final String nome    = u.getNome();
                final String destino = u.getEmail();

                // o envio roda em outra thread: a resposta ao cliente sai na hora, existindo o e-mail
                // ou nao (se demorasse so para quem existe, daria para descobrir quem esta cadastrado)
                new Thread(() ->
                {
                    try
                    {
                        this.email.enviarRecuperacao(nome, destino, link);
                    }
                    catch (Exception e)
                    {
                        System.err.println("Falha ao enviar e-mail de recuperacao para " + destino + ": " + e);
                    }
                }).start();
            }

            return new RespostaDeRecuperacaoDeSenha(true, generica);
        }
        catch (Exception e)
        {
            System.err.println("Erro na recuperacao de senha: " + e);
            return new RespostaDeRecuperacaoDeSenha(false, "Erro interno. Tente novamente.");
        }
    }

    // Tela "nova senha": confere o token (hash, validade, uso unico) e grava o novo hash.
    private RespostaDeRedefinicaoDeSenha tratarRedefinicao(PedidoDeRedefinicaoDeSenha p)
    {
        String erro = ValidadorUsuario.validarSenha(p.getNovaSenha());
        if (erro != null)
            return new RespostaDeRedefinicaoDeSenha(false, erro);

        String token = p.getToken() == null ? "" : p.getToken().trim();
        if (token.isEmpty() || token.length() > 100)
            return new RespostaDeRedefinicaoDeSenha(false, "Link invalido ou expirado. Solicite um novo.");

        try
        {
            String novoHash = BCrypt.withDefaults().hashToString(12, p.getNovaSenha().toCharArray());
            boolean trocou  = this.dao.redefinirSenha(TokenRecuperacao.hash(token), novoHash);

            if (!trocou)
                return new RespostaDeRedefinicaoDeSenha(false, "Link invalido ou expirado. Solicite um novo.");
            return new RespostaDeRedefinicaoDeSenha(true, "Senha alterada com sucesso!");
        }
        catch (Exception e)
        {
            System.err.println("Erro ao redefinir senha: " + e);
            return new RespostaDeRedefinicaoDeSenha(false, "Erro interno. Tente novamente.");
        }
    }

    // ---------- tela Configuracoes ----------

    private RespostaDeBuscaDePerfil tratarBuscaDePerfil(PedidoDeBuscaDePerfil p)
    {
        String erro = ValidadorUsuario.validarCodigo(p.getUsuarioCodigo());
        if (erro != null)
            return new RespostaDeBuscaDePerfil(false, erro, null, null, null, true);

        try
        {
            Perfil perfil = this.dao.buscarPerfil(p.getUsuarioCodigo());
            if (perfil == null)
                return new RespostaDeBuscaDePerfil(false, "Usuario nao encontrado. Entre novamente.",
                    null, null, null, true);
            return new RespostaDeBuscaDePerfil(true, "OK", perfil.getNome(), perfil.getEmail(),
                perfil.getTelefone(), perfil.isReceberAlertasPorEmail());
        }
        catch (Exception e)
        {
            System.err.println("Erro ao buscar perfil: " + e);
            return new RespostaDeBuscaDePerfil(false, "Erro interno ao buscar seus dados. Tente novamente.",
                    null, null, null, true);
        }
    }

    private RespostaDeConta tratarAtualizacaoDePerfil(PedidoDeAtualizacaoDePerfil p)
    {
        String erro = primeiroErro(
            ValidadorUsuario.validarCodigo(p.getUsuarioCodigo()),
            ValidadorUsuario.validarNome(p.getNome()),
            ValidadorUsuario.validarEmail(p.getEmail()),
            ValidadorUsuario.validarTelefone(p.getTelefone()));
        if (erro != null)
            return new RespostaDeConta(false, erro);

        try
        {
            String telefone = p.getTelefone() == null ? "" : p.getTelefone().trim();
            boolean ok = this.dao.atualizarPerfil(p.getUsuarioCodigo(), p.getNome().trim(),
                p.getEmail().trim().toLowerCase(), telefone.isEmpty() ? null : telefone);
            if (!ok)
                return new RespostaDeConta(false, "Usuario nao encontrado. Entre novamente.");
            return new RespostaDeConta(true, "Informacoes atualizadas com sucesso.");
        }
        catch (UsuarioDAO.EmailJaCadastradoException e)
        {
            return new RespostaDeConta(false, "Este e-mail ja esta cadastrado.");
        }
        catch (Exception e)
        {
            System.err.println("Erro ao atualizar perfil: " + e);
            return new RespostaDeConta(false, "Erro interno ao salvar. Tente novamente.");
        }
    }

    private RespostaDeConta tratarAtualizacaoDePreferencias(PedidoDeAtualizacaoDePreferencias p)
    {
        String erro = ValidadorUsuario.validarCodigo(p.getUsuarioCodigo());
        if (erro != null)
            return new RespostaDeConta(false, erro);

        try
        {
            if (!this.dao.atualizarPreferencias(p.getUsuarioCodigo(), p.isReceberAlertasPorEmail()))
                return new RespostaDeConta(false, "Usuario nao encontrado. Entre novamente.");
            return new RespostaDeConta(true, "Preferencias salvas.");
        }
        catch (Exception e)
        {
            System.err.println("Erro ao salvar preferencias: " + e);
            return new RespostaDeConta(false, "Erro interno ao salvar. Tente novamente.");
        }
    }

    private RespostaDeConta tratarAlteracaoDeSenha(PedidoDeAlteracaoDeSenha p)
    {
        String erro = primeiroErro(
            ValidadorUsuario.validarCodigo(p.getUsuarioCodigo()),
            ValidadorUsuario.validarSenha(p.getNovaSenha()));
        if (erro != null)
            return new RespostaDeConta(false, erro);
        if (p.getSenhaAtual() == null || p.getSenhaAtual().isEmpty())
            return new RespostaDeConta(false, "Informe a senha atual.");

        try
        {
            String hashAtual = this.dao.buscarSenhaHash(p.getUsuarioCodigo());
            if (hashAtual == null)
                return new RespostaDeConta(false, "Usuario nao encontrado. Entre novamente.");
            if (!BCrypt.verifyer().verify(p.getSenhaAtual().toCharArray(), hashAtual).verified)
                return new RespostaDeConta(false, "Senha atual incorreta.");
            if (p.getSenhaAtual().equals(p.getNovaSenha()))
                return new RespostaDeConta(false, "A nova senha deve ser diferente da atual.");

            String novoHash = BCrypt.withDefaults().hashToString(12, p.getNovaSenha().toCharArray());
            this.dao.alterarSenha(p.getUsuarioCodigo(), novoHash);
            return new RespostaDeConta(true, "Senha alterada com sucesso.");
        }
        catch (Exception e)
        {
            System.err.println("Erro ao alterar senha: " + e);
            return new RespostaDeConta(false, "Erro interno ao alterar a senha. Tente novamente.");
        }
    }

    private RespostaDeConta tratarExclusaoDeConta(PedidoDeExclusaoDeConta p)
    {
        String erro = ValidadorUsuario.validarCodigo(p.getUsuarioCodigo());
        if (erro != null)
            return new RespostaDeConta(false, erro);
        if (p.getSenha() == null || p.getSenha().isEmpty())
            return new RespostaDeConta(false, "Informe sua senha para confirmar a exclusao.");

        try
        {
            String hash = this.dao.buscarSenhaHash(p.getUsuarioCodigo());
            if (hash == null)
                return new RespostaDeConta(false, "Usuario nao encontrado. Entre novamente.");
            if (!BCrypt.verifyer().verify(p.getSenha().toCharArray(), hash).verified)
                return new RespostaDeConta(false, "Senha incorreta.");

            this.dao.desativar(p.getUsuarioCodigo());
            return new RespostaDeConta(true, "Conta excluida.");
        }
        catch (Exception e)
        {
            System.err.println("Erro ao excluir conta: " + e);
            return new RespostaDeConta(false, "Erro interno ao excluir a conta. Tente novamente.");
        }
    }

    private static String primeiroErro(String... erros)
    {
        for (String e : erros)
            if (e != null)
                return e;
        return null;
    }
}