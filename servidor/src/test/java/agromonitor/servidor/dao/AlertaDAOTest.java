package agromonitor.servidor.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import agromonitor.comum.alerta.AlertaResumo;
import agromonitor.servidor.BancoDeTeste;

class AlertaDAOTest
{
    private final LocalDate hoje = LocalDate.now();

    private BancoDeTeste bd;
    private UsuarioDAO   usuarioDao;
    private PlantioDAO   plantioDao;
    private AlertaDAO    dao;
    private int          usuario;
    private int          plantio;

    @BeforeEach
    void preparar() throws Exception
    {
        bd         = new BancoDeTeste();
        usuarioDao = new UsuarioDAO(bd.banco());
        plantioDao = new PlantioDAO(bd.banco());
        dao        = new AlertaDAO(bd.banco());
        usuarioDao.garantirIndices();
        plantioDao.garantirIndices();
        dao.garantirIndices();

        usuario = usuarioDao.inserir("Maria", "m@x.com", null, null, "hash");
        // as 4 atividades automaticas ficam a mais de 200 dias: nao geram alerta
        plantio = plantioDao.inserir(usuario, "Soja", "Soja T1", "25/26", null, "IRRIGADO", "T1",
                                     10.0, null, null, hoje.plusDays(200), null, new LocalDate[4]);
    }

    @AfterEach
    void limpar() { bd.close(); }

    private int atividade(String status, LocalDate data)
    {
        return plantioDao.registrarAtividade(usuario, plantio, "COLHEITA", status, data, null, null, null);
    }

    // ---------- gerar alertas ----------
    @Test
    void atividadeAtrasadaGeraAlertaAltaComTodosOsCampos()
    {
        atividade("PENDENTE", hoje.minusDays(1));
        assertEquals(1, dao.gerarAlertasDeAtividades(hoje));

        List<AlertaResumo> lista = dao.listarPorUsuario(usuario);
        assertEquals(1, lista.size());
        AlertaResumo a = lista.get(0);
        assertEquals("Atividade atrasada", a.getTitulo());
        assertEquals("ALTA", a.getPrioridade());
        assertEquals("NAO_LIDO", a.getSituacao());
        assertEquals("ATIVIDADE", a.getTipoAlerta());
        assertEquals("SISTEMA_MONITORAMENTO", a.getOrigem());
        assertEquals("Colheita", a.getImpactoAtividade());
        assertEquals("Soja T1", a.getPlantioNome());
        assertEquals("Soja", a.getCultura());
        assertEquals("T1", a.getTalhao());
        assertEquals(plantio, a.getPlantioCodigo());
        assertEquals(hoje.minusDays(1).toString(), a.getDataAtividade());
        assertEquals(3, a.getRecomendacoes().size());
    }

    @Test
    void atividadeParaHojeGeraAlertaAlta()
    {
        atividade("PENDENTE", hoje);
        assertEquals(1, dao.gerarAlertasDeAtividades(hoje));
        AlertaResumo a = dao.listarPorUsuario(usuario).get(0);
        assertEquals("Atividade para hoje", a.getTitulo());
        assertEquals("ALTA", a.getPrioridade());
    }

    @Test
    void atividadeEmAteDoisDiasGeraAlertaMedia()
    {
        atividade("PENDENTE", hoje.plusDays(1));
        atividade("PENDENTE", hoje.plusDays(2));
        assertEquals(2, dao.gerarAlertasDeAtividades(hoje));
        for (AlertaResumo a : dao.listarPorUsuario(usuario))
        {
            assertEquals("Atividade se aproxima", a.getTitulo());
            assertEquals("MEDIA", a.getPrioridade());
        }
    }

    @Test
    void atividadeLongeNaoGeraAlerta()
    {
        atividade("PENDENTE", hoje.plusDays(3));
        assertEquals(0, dao.gerarAlertasDeAtividades(hoje));
        assertTrue(dao.listarPorUsuario(usuario).isEmpty());
    }

    @Test
    void atividadeConcluidaNaoGeraAlerta()
    {
        atividade("CONCLUIDA", hoje.minusDays(5));
        assertEquals(0, dao.gerarAlertasDeAtividades(hoje));
    }

    @Test
    void gerarDuasVezesNaoDuplicaAlertas()
    {
        atividade("PENDENTE", hoje.minusDays(1));
        assertEquals(1, dao.gerarAlertasDeAtividades(hoje));
        assertEquals(0, dao.gerarAlertasDeAtividades(hoje));
        assertEquals(1, dao.listarPorUsuario(usuario).size());
    }

    @Test
    void cadaAtividadeTemNoMaximoDoisAlertasProximaEDepoisAtrasada()
    {
        atividade("PENDENTE", hoje.plusDays(1));
        assertEquals(1, dao.gerarAlertasDeAtividades(hoje));                 // PROXIMA
        assertEquals(1, dao.gerarAlertasDeAtividades(hoje.plusDays(2)));     // agora esta atrasada: ATRASADA
        assertEquals(0, dao.gerarAlertasDeAtividades(hoje.plusDays(3)));     // nao cria um terceiro
        assertEquals(2, dao.listarPorUsuario(usuario).size());
    }

    @Test
    void atrasoDeTresDiasSobeParaCriticoEnquantoNaoLido()
    {
        atividade("PENDENTE", hoje.minusDays(2));
        dao.gerarAlertasDeAtividades(hoje);
        assertEquals("ALTA", dao.listarPorUsuario(usuario).get(0).getPrioridade());

        assertEquals(0, dao.gerarAlertasDeAtividades(hoje.plusDays(1))); // nao cria novo, so sobe
        assertEquals("CRITICO", dao.listarPorUsuario(usuario).get(0).getPrioridade());
    }

    @Test
    void alertaJaLidoNaoSobeParaCritico()
    {
        atividade("PENDENTE", hoje.minusDays(2));
        dao.gerarAlertasDeAtividades(hoje);
        int codigo = dao.listarPorUsuario(usuario).get(0).getCodigo();
        dao.marcarComoLido(usuario, codigo);

        dao.gerarAlertasDeAtividades(hoje.plusDays(1));
        assertEquals("ALTA", dao.listarPorUsuario(usuario).get(0).getPrioridade());
    }

    @Test
    void usuarioDesativadoNaoRecebeAlertas() throws Exception
    {
        atividade("PENDENTE", hoje.minusDays(1));
        usuarioDao.desativar(usuario);
        assertEquals(0, dao.gerarAlertasDeAtividades(hoje));
    }

    // ---------- listar / marcar como lido ----------
    @Test
    void listarSoMostraAlertasDoProprioUsuario() throws Exception
    {
        atividade("PENDENTE", hoje);
        dao.gerarAlertasDeAtividades(hoje);
        int outro = usuarioDao.inserir("Joao", "j@x.com", null, null, "h");

        assertEquals(1, dao.listarPorUsuario(usuario).size());
        assertTrue(dao.listarPorUsuario(outro).isEmpty());
    }

    @Test
    void marcarComoLido() throws Exception
    {
        atividade("PENDENTE", hoje);
        dao.gerarAlertasDeAtividades(hoje);
        int codigo = dao.listarPorUsuario(usuario).get(0).getCodigo();
        int outro  = usuarioDao.inserir("Joao", "j@x.com", null, null, "h");

        assertFalse(dao.marcarComoLido(outro, codigo));   // alerta de outra pessoa
        assertFalse(dao.marcarComoLido(usuario, 999));    // inexistente
        assertEquals("NAO_LIDO", dao.listarPorUsuario(usuario).get(0).getSituacao());

        assertTrue(dao.marcarComoLido(usuario, codigo));
        assertEquals("LIDO", dao.listarPorUsuario(usuario).get(0).getSituacao());
    }

    @Test
    void marcarLidosDaAtividadeMarcaTodosOsAlertasDela()
    {
        int a = atividade("PENDENTE", hoje.plusDays(1));
        dao.gerarAlertasDeAtividades(hoje);                    // PROXIMA
        dao.gerarAlertasDeAtividades(hoje.plusDays(2));        // ATRASADA

        dao.marcarLidosDaAtividade(a);
        for (AlertaResumo r : dao.listarPorUsuario(usuario))
            assertEquals("LIDO", r.getSituacao());
    }

    // ---------- e-mail de alertas de atividade ----------
    @Test
    void pendentesDeEmailSaoOsNaoLidosAindaNaoEnviados()
    {
        atividade("PENDENTE", hoje);
        dao.gerarAlertasDeAtividades(hoje);

        List<AlertaDAO.AlertaEmail> pendentes = dao.listarPendentesDeEmail();
        assertEquals(1, pendentes.size());
        AlertaDAO.AlertaEmail e = pendentes.get(0);
        assertEquals(usuario, e.usuarioCodigo());
        assertEquals("Atividade para hoje", e.titulo());
        assertEquals("Soja T1", e.plantio());
        assertEquals("ALTA", e.prioridade());
        assertEquals(hoje.toString(), e.data());

        dao.marcarEmailProcessado(e.codigo());
        assertTrue(dao.listarPendentesDeEmail().isEmpty());
    }

    @Test
    void alertaLidoNaoFicaPendenteDeEmail()
    {
        atividade("PENDENTE", hoje);
        dao.gerarAlertasDeAtividades(hoje);
        dao.marcarComoLido(usuario, dao.listarPorUsuario(usuario).get(0).getCodigo());
        assertTrue(dao.listarPendentesDeEmail().isEmpty());
    }

    // ---------- alertas climaticos (sem duplicar e-mail) ----------
    @Test
    void alertaClimaticoSoERegistradoUmaVez()
    {
        assertTrue(dao.registrarAlertaClimaticoEmail("geada-1", usuario, plantio, "Soja T1",
                "Geada", "Risco de geada", "ALTA", "2026-10-28"));
        assertFalse(dao.registrarAlertaClimaticoEmail("geada-1", usuario, plantio, "Soja T1",
                "Geada", "Risco de geada", "ALTA", "2026-10-28"));

        // outra chave (outro evento) e aceita
        assertTrue(dao.registrarAlertaClimaticoEmail("geada-2", usuario, plantio, "Soja T1",
                "Geada", "Risco de geada", "ALTA", "2026-10-29"));
    }

    @Test
    void alertaClimaticoProcessadoNaoVoltaAFicarPendente()
    {
        dao.registrarAlertaClimaticoEmail("chuva-1", usuario, plantio, "Soja T1", "Chuva", "Forte", "MEDIA", "2026-10-28");
        dao.marcarEmailClimaticoProcessado("chuva-1");
        assertFalse(dao.registrarAlertaClimaticoEmail("chuva-1", usuario, plantio, "Soja T1", "Chuva", "Forte", "MEDIA", "2026-10-28"));
    }
}
