package agromonitor.servidor.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import agromonitor.comum.atividade.AtividadeResumo;
import agromonitor.comum.plantio.PlantioResumo;
import agromonitor.servidor.BancoDeTeste;

class PlantioDAOTest
{
    private static final LocalDate D = LocalDate.of(2035, 3, 1); // sempre no futuro: nada nasce atrasado

    private BancoDeTeste bd;
    private PlantioDAO   dao;

    @BeforeEach
    void preparar()
    {
        bd  = new BancoDeTeste();
        dao = new PlantioDAO(bd.banco());
        dao.garantirIndices();
    }

    @AfterEach
    void limpar() { bd.close(); }

    private int plantio(int usuario, LocalDate dataPlantio)
    {
        return dao.inserir(usuario, "Soja", "Soja T1", "25/26", null, "IRRIGADO", "T1",
                           12.5, "Campinas", 650, dataPlantio, "PLANTIO_DIRETO", new LocalDate[4]);
    }

    // ---------- inserir ----------
    @Test
    void codigosDePlantioSaoSequenciais()
    {
        assertEquals(1, plantio(1, D));
        assertEquals(2, plantio(1, D));
    }

    @Test
    void inserirCriaQuatroAtividadesComCronogramaPadrao()
    {
        plantio(1, D);
        List<AtividadeResumo> a = dao.listarAtividadesPorUsuario(1);

        assertEquals(4, a.size());
        assertEquals("PLANTIO",    a.get(0).getTipoAtividade());
        assertEquals("IRRIGACAO",  a.get(1).getTipoAtividade());
        assertEquals("ADUBACAO",   a.get(2).getTipoAtividade());
        assertEquals("TRATAMENTO", a.get(3).getTipoAtividade());
        assertEquals(D.toString(),               a.get(0).getData());
        assertEquals(D.plusDays(7).toString(),   a.get(1).getData());
        assertEquals(D.plusDays(20).toString(),  a.get(2).getData());
        assertEquals(D.plusDays(35).toString(),  a.get(3).getData());
        for (AtividadeResumo x : a)
            assertEquals("PENDENTE", x.getStatus());
    }

    @Test
    void ajusteSubstituiAPrevisaoAutomatica()
    {
        LocalDate[] ajustes = new LocalDate[4];
        ajustes[1] = D.plusDays(3);
        dao.inserir(1, "Soja", "Soja T1", "25/26", null, "IRRIGADO", "T1", 12.5, null, null, D, null, ajustes);

        List<AtividadeResumo> a = dao.listarAtividadesPorUsuario(1);
        // ordenadas por data: PLANTIO (D), IRRIGACAO ajustada (D+3), ADUBACAO (D+20), TRATAMENTO (D+35)
        assertEquals("IRRIGACAO", a.get(1).getTipoAtividade());
        assertEquals(D.plusDays(3).toString(), a.get(1).getData());
    }

    @Test
    void codigosDeAtividadeNaoSeRepetemEntrePlantios()
    {
        plantio(1, D);
        plantio(1, D);
        List<AtividadeResumo> a = dao.listarAtividadesPorUsuario(1);
        assertEquals(8, a.stream().map(AtividadeResumo::getCodigo).distinct().count());
    }

    // ---------- listar plantios ----------
    @Test
    void listarPlantiosDevolveResumoCompleto()
    {
        int c = plantio(7, D);
        ArrayList<PlantioResumo> lista = dao.listarPorUsuario(7);

        assertEquals(1, lista.size());
        PlantioResumo p = lista.get(0);
        assertEquals(c, p.getCodigo());
        assertEquals("Soja T1", p.getNome());
        assertEquals("Soja", p.getCultura());
        assertEquals("T1", p.getTalhao());
        assertEquals(12.5, p.getAreaPlantada());
        assertEquals(D.toString(), p.getDataPlantio());
        assertEquals(4, p.getQuantidadeAtividades());
        assertEquals("PLANTIO", p.getProximaAtividadeTipo());
        assertEquals(D.toString(), p.getProximaAtividadeData());
        assertEquals(0, p.getProgresso());
        assertEquals("FASE_INICIAL", p.getStatus());
        assertEquals("Campinas", p.getLocalizacao());
    }

    @Test
    void listarPlantiosSoMostraOsDoUsuario()
    {
        plantio(1, D);
        plantio(2, D);
        assertEquals(1, dao.listarPorUsuario(1).size());
        assertEquals(1, dao.listarPorUsuario(2).size());
        assertTrue(dao.listarPorUsuario(3).isEmpty());
    }

    @Test
    void listarPlantiosMaisRecentesPrimeiro() throws Exception
    {
        int primeiro = plantio(1, D);
        Thread.sleep(20);
        int segundo = plantio(1, D);
        ArrayList<PlantioResumo> lista = dao.listarPorUsuario(1);
        assertEquals(segundo,  lista.get(0).getCodigo());
        assertEquals(primeiro, lista.get(1).getCodigo());
    }

    @Test
    void nomeAtivoDoUsuarioSoParaODono()
    {
        int c = plantio(1, D);
        assertEquals("Soja T1", dao.nomeAtivoDoUsuario(1, c));
        assertNull(dao.nomeAtivoDoUsuario(2, c));
        assertNull(dao.nomeAtivoDoUsuario(1, 999));
    }

    // ---------- registrar atividade ----------
    @Test
    void registrarAtividadePendenteNaoMudaProgresso()
    {
        int p = plantio(1, D);
        int a = dao.registrarAtividade(1, p, "MONITORAMENTO", "PENDENTE", D.plusDays(2), "x", "Joao", "obs");

        assertTrue(a > 0);
        PlantioResumo r = dao.listarPorUsuario(1).get(0);
        assertEquals(5, r.getQuantidadeAtividades());
        assertEquals(0, r.getProgresso());
    }

    @Test
    void registrarAtividadeConcluidaAtualizaProgresso()
    {
        int p = plantio(1, D);
        dao.registrarAtividade(1, p, "MONITORAMENTO", "CONCLUIDA", LocalDate.now(), null, null, null);

        PlantioResumo r = dao.listarPorUsuario(1).get(0);
        assertEquals(5, r.getQuantidadeAtividades());
        assertEquals(20, r.getProgresso()); // 1 de 5
        assertEquals("FASE_INICIAL", r.getStatus());
    }

    @Test
    void registrarAtividadeGravaOsCamposOpcionais()
    {
        int p = plantio(1, D);
        int a = dao.registrarAtividade(1, p, "IRRIGACAO", "PENDENTE", D.plusDays(1), "22 mm", "Joao", "molhar cedo");

        AtividadeResumo r = dao.listarAtividadesPorUsuario(1).stream()
                               .filter(x -> x.getCodigo() == a).findFirst().orElseThrow();
        assertEquals("22 mm", r.getQuantidade());
        assertEquals("Joao", r.getResponsavel());
        assertEquals("molhar cedo", r.getObservacoes());
        assertEquals(p, r.getPlantioCodigo());
        assertEquals("Soja T1", r.getPlantioNome());
        assertEquals("T1", r.getTalhao());
    }

    @Test
    void registrarAtividadeEmPlantioDeOutroUsuarioOuInexistenteDevolveZero()
    {
        int p = plantio(1, D);
        assertEquals(0, dao.registrarAtividade(2, p, "COLHEITA", "PENDENTE", D, null, null, null));
        assertEquals(0, dao.registrarAtividade(1, 999, "COLHEITA", "PENDENTE", D, null, null, null));
        assertEquals(4, dao.listarPorUsuario(1).get(0).getQuantidadeAtividades()); // nada foi gravado
    }

    // ---------- concluir atividade e fases do plantio ----------
    @Test
    void progressoEFaseAcompanhamAsConclusoes()
    {
        int p = plantio(1, D);
        List<AtividadeResumo> atividades = dao.listarAtividadesPorUsuario(1);

        int[]    progresso = { 25, 50, 75, 100 };
        String[] fase      = { "FASE_INICIAL", "DESENVOLVIMENTO", "FASE_FINAL", "CONCLUIDO" };

        for (int i = 0; i < 4; i++)
        {
            assertTrue(dao.concluirAtividade(1, p, atividades.get(i).getCodigo()));
            PlantioResumo r = dao.listarPorUsuario(1).get(0);
            assertEquals(progresso[i], r.getProgresso(), "progresso apos " + (i + 1) + " conclusao(oes)");
            assertEquals(fase[i], r.getStatus());
        }
    }

    @Test
    void proximaAtividadePulaAsConcluidas()
    {
        int p = plantio(1, D);
        int primeira = dao.listarAtividadesPorUsuario(1).get(0).getCodigo();
        dao.concluirAtividade(1, p, primeira);

        PlantioResumo r = dao.listarPorUsuario(1).get(0);
        assertEquals("IRRIGACAO", r.getProximaAtividadeTipo());
        assertEquals(D.plusDays(7).toString(), r.getProximaAtividadeData());
    }

    @Test
    void todasConcluidasNaoTemProximaAtividade()
    {
        int p = plantio(1, D);
        for (AtividadeResumo a : dao.listarAtividadesPorUsuario(1))
            dao.concluirAtividade(1, p, a.getCodigo());

        PlantioResumo r = dao.listarPorUsuario(1).get(0);
        assertNull(r.getProximaAtividadeTipo());
        assertNull(r.getProximaAtividadeData());
    }

    @Test
    void concluirDuasVezesOuDeOutroUsuarioFalha()
    {
        int p = plantio(1, D);
        int a = dao.listarAtividadesPorUsuario(1).get(0).getCodigo();

        assertFalse(dao.concluirAtividade(2, p, a));      // outro usuario
        assertFalse(dao.concluirAtividade(1, 999, a));    // plantio inexistente
        assertFalse(dao.concluirAtividade(1, p, 99999));  // atividade inexistente

        assertTrue(dao.concluirAtividade(1, p, a));
        assertFalse(dao.concluirAtividade(1, p, a));      // ja concluida
        assertEquals(25, dao.listarPorUsuario(1).get(0).getProgresso()); // nao contou duas vezes
    }

    @Test
    void concluirGravaStatusConcluidaNaListagem()
    {
        int p = plantio(1, D);
        int a = dao.listarAtividadesPorUsuario(1).get(0).getCodigo();
        dao.concluirAtividade(1, p, a);

        AtividadeResumo r = dao.listarAtividadesPorUsuario(1).stream()
                               .filter(x -> x.getCodigo() == a).findFirst().orElseThrow();
        assertEquals("CONCLUIDA", r.getStatus());
        assertEquals(LocalDate.now().toString(), r.getData()); // mostra a data realizada
    }

    // ---------- listar atividades ----------
    @Test
    void pendenteComDataPassadaApareceComoAtrasada()
    {
        LocalDate hoje = LocalDate.now();
        plantio(1, hoje.minusDays(10)); // atividades em hoje-10, hoje-3, hoje+10, hoje+25

        List<AtividadeResumo> a = dao.listarAtividadesPorUsuario(1);
        assertEquals("ATRASADA", a.get(0).getStatus());
        assertEquals("ATRASADA", a.get(1).getStatus());
        assertEquals("PENDENTE", a.get(2).getStatus());
        assertEquals("PENDENTE", a.get(3).getStatus());
    }

    @Test
    void atividadePendenteParaHojeNaoEAtrasada()
    {
        plantio(1, LocalDate.now());
        assertEquals("PENDENTE", dao.listarAtividadesPorUsuario(1).get(0).getStatus());
    }

    @Test
    void atividadesVemOrdenadasPorDataDeTodosOsPlantiosDoUsuario()
    {
        plantio(1, D.plusDays(5));
        plantio(1, D);
        plantio(2, D); // de outro usuario: nao entra

        List<AtividadeResumo> a = dao.listarAtividadesPorUsuario(1);
        assertEquals(8, a.size());
        for (int i = 1; i < a.size(); i++)
            assertTrue(a.get(i - 1).getData().compareTo(a.get(i).getData()) <= 0);
    }
}
