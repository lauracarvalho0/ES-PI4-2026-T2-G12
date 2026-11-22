// Laura Carvalho - 25014543

package agromonitor.servidor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import agromonitor.comum.Comunicado;
import agromonitor.comum.Parceiro;
import agromonitor.comum.PedidoParaSair;
import agromonitor.comum.alerta.AlertaResumo;
import agromonitor.comum.alerta.PedidoDeListagemDeAlertas;
import agromonitor.comum.alerta.RespostaDeListagemDeAlertas;
import agromonitor.comum.plantio.PedidoDeCadastroDePlantio;
import agromonitor.comum.plantio.PedidoDeListagemDePlantios;
import agromonitor.comum.plantio.RespostaDeCadastroDePlantio;
import agromonitor.comum.plantio.RespostaDeListagemDePlantios;
import agromonitor.servidor.dao.AlertaDAO;
import agromonitor.servidor.dao.PlantioDAO;
import agromonitor.servidor.dao.UsuarioDAO;
import agromonitor.servidor.email.EmailService;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

// Teste de INTERCLASSE: em vez de testar uma classe sozinha, sobe o fluxo inteiro do cadastro
// (cliente -> socket -> SupervisoraDeConexao -> Validador -> DAO -> banco) e confere o resultado.
// O banco e um Mongo falso em memoria (MongoServer + MemoryBackend), entao nao toca no Atlas de verdade.
class CadastroDePlantioInterclasseTest
{
    // banco falso
    private MongoServer   servidorMongo;
    private MongoClient   clienteMongo;
    private MongoDatabase banco;

    // as classes reais do servidor, ligadas nesse banco falso
    private UsuarioDAO   usuarioDao;
    private PlantioDAO   plantioDao;
    private AlertaDAO    alertaDao;
    private EmailService email;

    // lado "cliente" da conexao (faz o papel do front, falando pelo socket)
    private ServerSocket       ss;
    private Socket             cliente;
    private ObjectOutputStream saida;
    private ObjectInputStream  entrada;

    private int usuarioCodigo;

    // roda ANTES de cada teste: monta um ambiente novinho e limpo pra cada um
    @BeforeEach
    void preparar() throws Exception
    {
        // sobe o Mongo em memoria e conecta nele (bind() escolhe uma porta livre)
        servidorMongo = new MongoServer(new MemoryBackend());
        InetSocketAddress endereco = servidorMongo.bind();
        clienteMongo = MongoClients.create("mongodb://" + endereco.getHostString() + ":" + endereco.getPort());
        banco = clienteMongo.getDatabase("agromonitor_teste");

        usuarioDao = new UsuarioDAO(banco);
        plantioDao = spy(new PlantioDAO(banco));   // spy: da para simular falha do banco em um teste
        alertaDao  = new AlertaDAO(banco);
        usuarioDao.garantirIndices();
        plantioDao.garantirIndices();
        alertaDao.garantirIndices();
        // mock: e-mail de mentira, nao envia nada de verdade
        email = mock(EmailService.class);

        // usuario dono dos plantios (precisa existir e estar ativo)
        usuarioCodigo = usuarioDao.inserir("Maria Silva", "maria@x.com", null, null, "hash-qualquer");

        // sobe um servidor de socket numa porta livre (porta 0 = o sistema escolhe)
        // e uma thread que aceita UMA conexao e entrega pra SupervisoraDeConexao (a classe real)
        ss = new ServerSocket(0);
        Thread aceitadora = new Thread(() -> {
            try
            {
                Socket s = ss.accept();
                new SupervisoraDeConexao(s, new ArrayList<Parceiro>(), usuarioDao, plantioDao, alertaDao, email).start();
            }
            catch (Exception fim) {}
        });
        aceitadora.setDaemon(true); // daemon: nao impede o teste de terminar
        aceitadora.start();

        // conecta como cliente; setSoTimeout evita o teste travar pra sempre se o servidor nao responder
        cliente = new Socket("localhost", ss.getLocalPort());
        cliente.setSoTimeout(15000);
        saida = new ObjectOutputStream(cliente.getOutputStream());
        saida.flush(); // o flush aqui evita travar o "aperto de mao" dos streams de objeto
        entrada = new ObjectInputStream(cliente.getInputStream());
    }

    // roda DEPOIS de cada teste: avisa o servidor que vai sair e fecha tudo
    // (cada try/catch separado: se um fechamento falhar, os outros ainda acontecem)
    @AfterEach
    void limpar()
    {
        try { saida.writeObject(new PedidoParaSair()); saida.flush(); } catch (Exception ignorada) {}
        try { cliente.close(); } catch (Exception ignorada) {}
        try { ss.close(); } catch (Exception ignorada) {}
        clienteMongo.close();
        servidorMongo.shutdownNow();
    }

    // ---------- utilitarios ----------
    // manda um pedido pelo socket e le a resposta, ja convertida pro tipo esperado
    private <T extends Comunicado> T enviar(Comunicado pedido, Class<T> tipo) throws Exception
    {
        saida.writeObject(pedido);
        saida.flush();
        return tipo.cast(entrada.readObject());
    }

    // atalho pra cadastrar um plantio valido (nome com espacos nas pontas de proposito, pra testar o trim)
    // os 4 null do fim sao os ajustes de data: sem ajuste, vale o cronograma padrao
    private RespostaDeCadastroDePlantio cadastrarPlantio(int usuario, String cultura, LocalDate dataPlantio) throws Exception
    {
        return enviar(new PedidoDeCadastroDePlantio(usuario, cultura, "  Soja T1  ", "25/26", "descricao",
                "IRRIGADO", "T1", 12.5, "Campinas", 650, dataPlantio.toString(), "PLANTIO_DIRETO",
                null, null, null, null), RespostaDeCadastroDePlantio.class);
    }

    // le um campo de data de um documento do Mongo (guardado como meia-noite UTC) e devolve LocalDate
    private static LocalDate paraData(Document atividade, String campo)
    {
        return atividade.getDate(campo).toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    // o alerta e criado em outra thread (verificarEmSegundoPlano): espera ate 5 s
    // repete a consulta a cada 50 ms ate aparecer o minimo de alertas ou acabar o tempo
    private List<AlertaResumo> esperarAlertas(int usuario, int minimo) throws Exception
    {
        List<AlertaResumo> alertas = new ArrayList<AlertaResumo>();
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite)
        {
            alertas = enviar(new PedidoDeListagemDeAlertas(usuario), RespostaDeListagemDeAlertas.class).getAlertas();
            if (alertas.size() >= minimo)
                break;
            Thread.sleep(50);
        }
        return alertas;
    }

    // CENARIO NORMAL: plantio valido, usuario existente, data de plantio = hoje
    @Test
    void cenarioNormal_cadastraPlantioComQuatroAtividadesEGeraAlertaDeHoje() throws Exception
    {
        LocalDate hoje = LocalDate.now();

        // 1) resposta devolvida ao cliente
        RespostaDeCadastroDePlantio r = cadastrarPlantio(usuarioCodigo, "Soja", hoje);
        assertTrue(r.isSucesso());
        assertEquals("Plantio cadastrado com sucesso!", r.getMensagem());
        assertTrue(r.getCodigo() > 0);

        // 2) o que ficou gravado no banco (PlantioDAO.inserir)
        // aqui o teste olha direto o banco, sem passar pelo servidor, pra ter certeza do que foi gravado
        assertEquals(1, banco.getCollection("plantios").countDocuments());
        Document plantio = banco.getCollection("plantios").find().first();
        assertEquals(r.getCodigo(), plantio.getInteger("codigo"));
        assertEquals(usuarioCodigo, ((Document) plantio.get("usuario")).getInteger("codigo"));
        assertEquals("Soja T1", plantio.getString("nome"));                        // espacos das pontas removidos
        assertEquals("Soja", ((Document) plantio.get("cultura")).getString("nome"));
        assertEquals("FASE_INICIAL", plantio.getString("status"));
        assertEquals(0, plantio.getInteger("progresso"));

        // cronograma padrao: plantio +0, irrigacao +7, adubacao +20, tratamento +35 dias
        // (mesmos TIPOS e OFFSETS do PlantioDAO)
        List<Document> atividades = plantio.getList("atividades", Document.class);
        assertEquals(4, atividades.size());
        String[]  tipos   = { "PLANTIO", "IRRIGACAO", "ADUBACAO", "TRATAMENTO" };
        int[]     offsets = { 0, 7, 20, 35 };
        for (int i = 0; i < 4; i++)
        {
            assertEquals(tipos[i], atividades.get(i).getString("tipoAtividade"));
            assertEquals("PENDENTE", atividades.get(i).getString("status"));
            assertEquals(hoje.plusDays(offsets[i]), paraData(atividades.get(i), "dataPrevista"));
        }

        // 3) leitura pelo proprio servico de listagem (PlantioDAO.listarPorUsuario)
        RespostaDeListagemDePlantios l = enviar(new PedidoDeListagemDePlantios(usuarioCodigo), RespostaDeListagemDePlantios.class);
        assertTrue(l.isSucesso());
        assertEquals(1, l.getPlantios().size());
        assertEquals(4, l.getPlantios().get(0).getQuantidadeAtividades());
        assertEquals("PLANTIO", l.getPlantios().get(0).getProximaAtividadeTipo());

        // 4) alerta gerado em segundo plano: atividade de plantio e para hoje (RegrasDeAlerta.classificar)
        List<AlertaResumo> alertas = esperarAlertas(usuarioCodigo, 1);
        assertEquals(1, alertas.size());                                // as outras 3 estao longe: sem alerta
        assertEquals("Atividade para hoje", alertas.get(0).getTitulo());
        assertEquals("ALTA", alertas.get(0).getPrioridade());
        assertEquals("NAO_LIDO", alertas.get(0).getSituacao());
        assertEquals("Soja T1", alertas.get(0).getPlantioNome());
        assertEquals(hoje.toString(), alertas.get(0).getDataAtividade());
    }

    
    // CENARIO ALTERNATIVO / DE ERRO (a): dados invalidos
    // O ValidadorPlantio barra o pedido: nada chega ao banco e vem so o PRIMEIRO erro.
    @Test
    void cenarioDeErro_dadosInvalidosDevolvemPrimeiroErroENaoGravamNada() throws Exception
    {
        // cultura vazia E data invalida: a cultura e validada antes, entao e ela que volta
        RespostaDeCadastroDePlantio r = enviar(new PedidoDeCadastroDePlantio(usuarioCodigo, "", "Soja T1", "25/26", null,
                "IRRIGADO", "T1", 12.5, null, null, "data-ruim", null, null, null, null, null),
                RespostaDeCadastroDePlantio.class);

        assertFalse(r.isSucesso());
        assertEquals("Selecione a cultura.", r.getMensagem());
        assertEquals(0, r.getCodigo());
        // nada foi gravado: nem plantio, nem alerta
        assertEquals(0, banco.getCollection("plantios").countDocuments());
        assertEquals(0, banco.getCollection("alertas").countDocuments());
    }

    // CENARIO ALTERNATIVO / DE ERRO (b): usuario que nao existe (consulta ao UsuarioDAO)
    @Test
    void cenarioDeErro_usuarioInexistenteEhRecusado() throws Exception
    {
        // o codigo 999 nao existe no banco (so existe o usuario criado no preparar())
        RespostaDeCadastroDePlantio r = cadastrarPlantio(999, "Soja", LocalDate.now());

        assertFalse(r.isSucesso());
        assertEquals("Usuario nao encontrado. Entre novamente.", r.getMensagem());
        assertEquals(0, r.getCodigo());
        assertEquals(0, banco.getCollection("plantios").countDocuments());
    }

    // CENARIO DE ERRO (c): o banco lanca excecao. A SupervisoraDeConexao captura (throw tratado),
    // devolve mensagem generica ao cliente e a conexao continua funcionando.
    @Test
    void cenarioDeErro_falhaNoBancoViraMensagemDeErroInterno() throws Exception
    {
        // diz ao spy: "quando chamarem inserir(...) com qualquer argumento, lance esta excecao"
        // (e por isso que aparece "falha simulada do banco" no console quando o teste roda: e de proposito)
        doThrow(new RuntimeException("falha simulada do banco"))
            .when(plantioDao).inserir(anyInt(), any(), any(), any(), any(), any(), any(),
                                      anyDouble(), any(), any(), any(), any(), any());

        RespostaDeCadastroDePlantio r = cadastrarPlantio(usuarioCodigo, "Soja", LocalDate.now());

        assertFalse(r.isSucesso());
        assertEquals("Erro interno ao cadastrar o plantio. Tente novamente.", r.getMensagem());
        assertEquals(0, r.getCodigo());
        assertEquals(0, banco.getCollection("plantios").countDocuments());

        // a conexao nao caiu: um pedido seguinte ainda e atendido
        assertTrue(enviar(new PedidoDeListagemDePlantios(usuarioCodigo), RespostaDeListagemDePlantios.class).isSucesso());
    }

    // ANOMALIA 1 (documentada): o servico aceita o mesmo plantio duas vezes. Nao ha regra nem
    // indice unico para nome + talhao + safra do mesmo usuario; so o "codigo" e unico.
    @Test
    void anomalia_mesmoPlantioPodeSerCadastradoDuasVezes() throws Exception
    {
        // data daqui a 100 dias: longe o bastante pra nao gerar alerta (nao interfere no teste)
        RespostaDeCadastroDePlantio primeira = cadastrarPlantio(usuarioCodigo, "Soja", LocalDate.now().plusDays(100));
        RespostaDeCadastroDePlantio segunda  = cadastrarPlantio(usuarioCodigo, "Soja", LocalDate.now().plusDays(100));

        assertTrue(primeira.isSucesso());
        assertTrue(segunda.isSucesso());
        assertNotEquals(primeira.getCodigo(), segunda.getCodigo());
        assertEquals(2, banco.getCollection("plantios").countDocuments());
    }
}