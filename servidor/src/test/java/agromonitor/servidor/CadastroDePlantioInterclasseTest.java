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

class CadastroDePlantioInterclasseTest
{
    private MongoServer   servidorMongo;
    private MongoClient   clienteMongo;
    private MongoDatabase banco;

    private UsuarioDAO   usuarioDao;
    private PlantioDAO   plantioDao;
    private AlertaDAO    alertaDao;
    private EmailService email;

    private ServerSocket       ss;
    private Socket             cliente;
    private ObjectOutputStream saida;
    private ObjectInputStream  entrada;

    private int usuarioCodigo;

    @BeforeEach
    void preparar() throws Exception
    {
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
        email = mock(EmailService.class);

        // usuario dono dos plantios (precisa existir e estar ativo)
        usuarioCodigo = usuarioDao.inserir("Maria Silva", "maria@x.com", null, null, "hash-qualquer");

        ss = new ServerSocket(0);
        Thread aceitadora = new Thread(() -> {
            try
            {
                Socket s = ss.accept();
                new SupervisoraDeConexao(s, new ArrayList<Parceiro>(), usuarioDao, plantioDao, alertaDao, email).start();
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
    void limpar()
    {
        try { saida.writeObject(new PedidoParaSair()); saida.flush(); } catch (Exception ignorada) {}
        try { cliente.close(); } catch (Exception ignorada) {}
        try { ss.close(); } catch (Exception ignorada) {}
        clienteMongo.close();
        servidorMongo.shutdownNow();
    }

    // ---------- utilitarios ----------
    private <T extends Comunicado> T enviar(Comunicado pedido, Class<T> tipo) throws Exception
    {
        saida.writeObject(pedido);
        saida.flush();
        return tipo.cast(entrada.readObject());
    }

    private RespostaDeCadastroDePlantio cadastrarPlantio(int usuario, String cultura, LocalDate dataPlantio) throws Exception
    {
        return enviar(new PedidoDeCadastroDePlantio(usuario, cultura, "  Soja T1  ", "25/26", "descricao",
                "IRRIGADO", "T1", 12.5, "Campinas", 650, dataPlantio.toString(), "PLANTIO_DIRETO",
                null, null, null, null), RespostaDeCadastroDePlantio.class);
    }

    private static LocalDate paraData(Document atividade, String campo)
    {
        return atividade.getDate(campo).toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    // o alerta e criado em outra thread (verificarEmSegundoPlano): espera ate 5 s
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
        assertEquals(1, banco.getCollection("plantios").countDocuments());
        Document plantio = banco.getCollection("plantios").find().first();
        assertEquals(r.getCodigo(), plantio.getInteger("codigo"));
        assertEquals(usuarioCodigo, ((Document) plantio.get("usuario")).getInteger("codigo"));
        assertEquals("Soja T1", plantio.getString("nome"));                        // espacos das pontas removidos
        assertEquals("Soja", ((Document) plantio.get("cultura")).getString("nome"));
        assertEquals("FASE_INICIAL", plantio.getString("status"));
        assertEquals(0, plantio.getInteger("progresso"));

        // cronograma padrao: plantio +0, irrigacao +7, adubacao +20, tratamento +35 dias
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
        assertEquals(0, banco.getCollection("plantios").countDocuments());
        assertEquals(0, banco.getCollection("alertas").countDocuments());
    }

    // CENARIO ALTERNATIVO / DE ERRO (b): usuario que nao existe (consulta ao UsuarioDAO)
    @Test
    void cenarioDeErro_usuarioInexistenteEhRecusado() throws Exception
    {
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
        RespostaDeCadastroDePlantio primeira = cadastrarPlantio(usuarioCodigo, "Soja", LocalDate.now().plusDays(100));
        RespostaDeCadastroDePlantio segunda  = cadastrarPlantio(usuarioCodigo, "Soja", LocalDate.now().plusDays(100));

        assertTrue(primeira.isSucesso());
        assertTrue(segunda.isSucesso());
        assertNotEquals(primeira.getCodigo(), segunda.getCodigo());
        assertEquals(2, banco.getCollection("plantios").countDocuments());
    }
}
