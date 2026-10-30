package agromonitor.comum;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import agromonitor.comum.alerta.*;
import agromonitor.comum.atividade.*;
import agromonitor.comum.auth.*;
import agromonitor.comum.conta.*;
import agromonitor.comum.plantio.*;

// Todas as mensagens trocadas entre backend e servidor: getters, serializacao e vazamento de segredos.
class ComunicadosTest
{
    @SuppressWarnings("unchecked")
    private static <T> T viajar(T objeto) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(objeto); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())))
        { return (T) in.readObject(); }
    }

    // ---------- auth ----------
    @Test
    void pedidosDeAuth() throws Exception
    {
        PedidoDeCadastro c = viajar(new PedidoDeCadastro("Maria", "m@x.com", "52998224725", "1999", "senha1"));
        assertEquals("Maria", c.getNome());
        assertEquals("m@x.com", c.getEmail());
        assertEquals("52998224725", c.getCpf());
        assertEquals("1999", c.getTelefone());
        assertEquals("senha1", c.getSenha());

        PedidoDeLogin l = viajar(new PedidoDeLogin("m@x.com", "senha1"));
        assertEquals("m@x.com", l.getEmail());
        assertEquals("senha1", l.getSenha());

        PedidoDeRecuperacaoDeSenha r = viajar(new PedidoDeRecuperacaoDeSenha("m@x.com", "http://localhost:8080"));
        assertEquals("m@x.com", r.getEmail());
        assertEquals("http://localhost:8080", r.getUrlBase());

        PedidoDeRedefinicaoDeSenha d = viajar(new PedidoDeRedefinicaoDeSenha("tok", "nova123"));
        assertEquals("tok", d.getToken());
        assertEquals("nova123", d.getNovaSenha());
    }

    @Test
    void respostasDeAuth() throws Exception
    {
        RespostaDeCadastro c = viajar(new RespostaDeCadastro(true, "ok", 4));
        assertTrue(c.isSucesso()); assertEquals("ok", c.getMensagem()); assertEquals(4, c.getCodigo());

        RespostaDeLogin l = viajar(new RespostaDeLogin(true, "ok", 4, "Maria", "m@x.com"));
        assertTrue(l.isSucesso()); assertEquals(4, l.getCodigo());
        assertEquals("Maria", l.getNome()); assertEquals("m@x.com", l.getEmail());

        RespostaDeRecuperacaoDeSenha r = viajar(new RespostaDeRecuperacaoDeSenha(false, "x"));
        assertFalse(r.isSucesso()); assertEquals("x", r.getMensagem());

        RespostaDeRedefinicaoDeSenha d = viajar(new RespostaDeRedefinicaoDeSenha(true, "y"));
        assertTrue(d.isSucesso()); assertEquals("y", d.getMensagem());
    }

    // ---------- plantio ----------
    @Test
    void pedidoDeCadastroDePlantio() throws Exception
    {
        PedidoDeCadastroDePlantio p = viajar(new PedidoDeCadastroDePlantio(3, "Soja", "Soja T1", "25/26", "desc",
            "IRRIGADO", "T1", 12.5, "Campinas", 650, "2026-10-21", "PLANTIO_DIRETO",
            "2026-10-21", "2026-10-28", "2026-11-10", "2026-11-25"));
        assertEquals(3, p.getUsuarioCodigo());
        assertEquals("Soja", p.getCultura());
        assertEquals("Soja T1", p.getNome());
        assertEquals("25/26", p.getSafra());
        assertEquals("desc", p.getDescricao());
        assertEquals("IRRIGADO", p.getSistemaCultivo());
        assertEquals("T1", p.getTalhao());
        assertEquals(12.5, p.getAreaPlantada());
        assertEquals("Campinas", p.getLocalizacao());
        assertEquals(650, p.getAltitude());
        assertEquals("2026-10-21", p.getDataPlantio());
        assertEquals("PLANTIO_DIRETO", p.getMetodoPlantio());
        assertEquals("2026-10-21", p.getAjustePlantio());
        assertEquals("2026-10-28", p.getAjusteIrrigacao());
        assertEquals("2026-11-10", p.getAjusteAdubacao());
        assertEquals("2026-11-25", p.getAjusteTratos());
        assertNotNull(p.toString());
    }

    @Test
    void camposOpcionaisDoPlantioPodemSerNulos() throws Exception
    {
        PedidoDeCadastroDePlantio p = viajar(new PedidoDeCadastroDePlantio(3, "Soja", "n", "25/26", null,
            "SEQUEIRO", "T1", null, null, null, "2026-10-21", null, null, null, null, null));
        assertNull(p.getAreaPlantada());
        assertNull(p.getAltitude());
        assertNull(p.getAjusteTratos());
    }

    @Test
    void resumoEListagemDePlantios() throws Exception
    {
        PlantioResumo r = new PlantioResumo(1, "Soja T1", "Soja", "T1", 12.5, "2026-10-21", 4,
            "PLANTIO", "2026-10-21", 25, "FASE_INICIAL", "Campinas");
        RespostaDeListagemDePlantios l = viajar(new RespostaDeListagemDePlantios(true, "OK",
            new ArrayList<>(List.of(r))));

        assertEquals(1, l.getPlantios().size());
        PlantioResumo x = l.getPlantios().get(0);
        assertEquals(1, x.getCodigo()); assertEquals("Soja T1", x.getNome()); assertEquals("Soja", x.getCultura());
        assertEquals("T1", x.getTalhao()); assertEquals(12.5, x.getAreaPlantada());
        assertEquals("2026-10-21", x.getDataPlantio()); assertEquals(4, x.getQuantidadeAtividades());
        assertEquals("PLANTIO", x.getProximaAtividadeTipo()); assertEquals("2026-10-21", x.getProximaAtividadeData());
        assertEquals(25, x.getProgresso()); assertEquals("FASE_INICIAL", x.getStatus());
        assertEquals("Campinas", x.getLocalizacao());
        assertTrue(l.toString().contains("total=1"));

        assertEquals(3, viajar(new PedidoDeListagemDePlantios(3)).getUsuarioCodigo());
        RespostaDeCadastroDePlantio c = viajar(new RespostaDeCadastroDePlantio(true, "ok", 9));
        assertTrue(c.isSucesso()); assertEquals(9, c.getCodigo()); assertEquals("ok", c.getMensagem());
    }

    // ---------- atividade ----------
    @Test
    void pedidosDeAtividade() throws Exception
    {
        PedidoDeRegistroDeAtividade r = viajar(new PedidoDeRegistroDeAtividade(3, 9, "IRRIGACAO", "PENDENTE",
            "2030-01-10", "22 mm", "Joao", "obs"));
        assertEquals(3, r.getUsuarioCodigo()); assertEquals(9, r.getPlantioCodigo());
        assertEquals("IRRIGACAO", r.getTipoAtividade()); assertEquals("PENDENTE", r.getStatus());
        assertEquals("2030-01-10", r.getDataExecucao()); assertEquals("22 mm", r.getQuantidade());
        assertEquals("Joao", r.getResponsavel()); assertEquals("obs", r.getObservacoes());

        PedidoDeConclusaoDeAtividade c = viajar(new PedidoDeConclusaoDeAtividade(3, 9, 42));
        assertEquals(3, c.getUsuarioCodigo()); assertEquals(9, c.getPlantioCodigo()); assertEquals(42, c.getAtividadeCodigo());

        assertEquals(3, viajar(new PedidoDeListagemDeAtividades(3)).getUsuarioCodigo());
    }

    @Test
    void respostasDeAtividade() throws Exception
    {
        RespostaDeRegistroDeAtividade r = viajar(new RespostaDeRegistroDeAtividade(true, "ok", 42));
        assertTrue(r.isSucesso()); assertEquals(42, r.getCodigo());

        RespostaDeConclusaoDeAtividade c = viajar(new RespostaDeConclusaoDeAtividade(false, "nao"));
        assertFalse(c.isSucesso()); assertEquals("nao", c.getMensagem());

        AtividadeResumo a = new AtividadeResumo(42, 9, "Soja T1", "T1", "IRRIGACAO", "2030-01-10", "PENDENTE",
            "22 mm", "Joao", "obs");
        RespostaDeListagemDeAtividades l = viajar(new RespostaDeListagemDeAtividades(true, "OK", new ArrayList<>(List.of(a))));
        AtividadeResumo x = l.getAtividades().get(0);
        assertEquals(42, x.getCodigo()); assertEquals(9, x.getPlantioCodigo()); assertEquals("Soja T1", x.getPlantioNome());
        assertEquals("T1", x.getTalhao()); assertEquals("IRRIGACAO", x.getTipoAtividade());
        assertEquals("2030-01-10", x.getData()); assertEquals("PENDENTE", x.getStatus());
        assertEquals("22 mm", x.getQuantidade()); assertEquals("Joao", x.getResponsavel()); assertEquals("obs", x.getObservacoes());
        assertTrue(l.toString().contains("total=1"));
    }

    // ---------- alerta ----------
    @Test
    void mensagensDeAlerta() throws Exception
    {
        AlertaResumo a = new AlertaResumo(5, 9, "Soja T1", "Soja", "T1", "ATIVIDADE", "Atividade atrasada",
            "desc", "ALTA", "NAO_LIDO", "2026-10-28 10:00", "SISTEMA_MONITORAMENTO", "Colheita",
            "Prevista para 27/10/2026", "2026-10-27", new ArrayList<>(List.of("r1", "r2")));
        RespostaDeListagemDeAlertas l = viajar(new RespostaDeListagemDeAlertas(true, "OK", new ArrayList<>(List.of(a))));
        AlertaResumo x = l.getAlertas().get(0);

        assertEquals(5, x.getCodigo()); assertEquals(9, x.getPlantioCodigo()); assertEquals("Soja T1", x.getPlantioNome());
        assertEquals("Soja", x.getCultura()); assertEquals("T1", x.getTalhao()); assertEquals("ATIVIDADE", x.getTipoAlerta());
        assertEquals("Atividade atrasada", x.getTitulo()); assertEquals("desc", x.getDescricao());
        assertEquals("ALTA", x.getPrioridade()); assertEquals("NAO_LIDO", x.getSituacao());
        assertEquals("2026-10-28 10:00", x.getDataAlerta()); assertEquals("SISTEMA_MONITORAMENTO", x.getOrigem());
        assertEquals("Colheita", x.getImpactoAtividade()); assertEquals("Prevista para 27/10/2026", x.getImpactoDataLabel());
        assertEquals("2026-10-27", x.getDataAtividade()); assertEquals(List.of("r1", "r2"), x.getRecomendacoes());

        assertEquals(3, viajar(new PedidoDeListagemDeAlertas(3)).getUsuarioCodigo());
        PedidoDeMarcacaoDeAlertaLido m = viajar(new PedidoDeMarcacaoDeAlertaLido(3, 5));
        assertEquals(3, m.getUsuarioCodigo()); assertEquals(5, m.getAlertaCodigo());
        assertTrue(viajar(new RespostaDeMarcacaoDeAlertaLido(true, "ok")).isSucesso());

        PedidoDeEnvioDeAlertaClimatico e = viajar(new PedidoDeEnvioDeAlertaClimatico(3, 9, "chave", "Geada",
            "Risco de geada", "ALTA", "2026-10-28"));
        assertEquals(3, e.getUsuarioCodigo()); assertEquals(9, e.getPlantioCodigo()); assertEquals("chave", e.getChave());
        assertEquals("Geada", e.getTitulo()); assertEquals("Risco de geada", e.getDescricao());
        assertEquals("ALTA", e.getPrioridade()); assertEquals("2026-10-28", e.getData());
        assertEquals("ok", viajar(new RespostaDeEnvioDeAlertaClimatico(true, "ok")).getMensagem());
    }

    // ---------- conta ----------
    @Test
    void mensagensDeConta() throws Exception
    {
        PedidoDeBuscaDePerfil b = viajar(new PedidoDeBuscaDePerfil(3));
        assertEquals(3, b.getUsuarioCodigo());

        PedidoDeAtualizacaoDePerfil p = viajar(new PedidoDeAtualizacaoDePerfil(3, "Maria", "m@x.com", "1999"));
        assertEquals("Maria", p.getNome()); assertEquals("m@x.com", p.getEmail()); assertEquals("1999", p.getTelefone());

        PedidoDeAtualizacaoDePreferencias f = viajar(new PedidoDeAtualizacaoDePreferencias(3, "(GMT-03:00) Brasília", "Português", false));
        assertEquals("(GMT-03:00) Brasília", f.getFusoHorario()); assertEquals("Português", f.getIdioma());
        assertFalse(f.isReceberAlertasPorEmail());

        PedidoDeAlteracaoDeSenha s = viajar(new PedidoDeAlteracaoDeSenha(3, "velha1", "nova12"));
        assertEquals("velha1", s.getSenhaAtual()); assertEquals("nova12", s.getNovaSenha());

        PedidoDeExclusaoDeConta x = viajar(new PedidoDeExclusaoDeConta(3, "senha1"));
        assertEquals(3, x.getUsuarioCodigo()); assertEquals("senha1", x.getSenha());

        RespostaDeBuscaDePerfil r = viajar(new RespostaDeBuscaDePerfil(true, "OK", "Maria", "m@x.com", "1999",
            "(GMT-03:00) Brasília", "Português", true));
        assertEquals("Maria", r.getNome()); assertEquals("m@x.com", r.getEmail()); assertEquals("1999", r.getTelefone());
        assertEquals("(GMT-03:00) Brasília", r.getFusoHorario()); assertEquals("Português", r.getIdioma());
        assertTrue(r.isReceberAlertasPorEmail());

        RespostaDeConta c = viajar(new RespostaDeConta(true, "feito"));
        assertTrue(c.isSucesso()); assertEquals("feito", c.getMensagem());
    }

    // ---------- controle ----------
    @Test
    void comunicadosDeControle() throws Exception
    {
        assertInstanceOf(PedidoParaSair.class, viajar(new PedidoParaSair()));
        assertInstanceOf(ComunicadoDeDesligamento.class, viajar(new ComunicadoDeDesligamento()));
        assertInstanceOf(Comunicado.class, new PedidoParaSair());
    }

    // ---------- seguranca: toString nunca mostra senha/token (vai parar em log) ----------
    @Test
    void toStringNaoVazaSenhasNemToken()
    {
        assertFalse(new PedidoDeCadastro("Maria", "m@x.com", "52998224725", "1999", "SEGREDO123").toString().contains("SEGREDO123"));
        assertFalse(new PedidoDeLogin("m@x.com", "SEGREDO123").toString().contains("SEGREDO123"));
        String redef = new PedidoDeRedefinicaoDeSenha("TOKEN-SECRETO", "SEGREDO123").toString();
        assertFalse(redef.contains("SEGREDO123"));
        assertFalse(redef.contains("TOKEN-SECRETO"));
        assertFalse(new PedidoDeAlteracaoDeSenha(1, "ATUAL123", "NOVA1234").toString().contains("ATUAL123"));
        assertFalse(new PedidoDeAlteracaoDeSenha(1, "ATUAL123", "NOVA1234").toString().contains("NOVA1234"));
        assertFalse(new PedidoDeExclusaoDeConta(1, "SEGREDO123").toString().contains("SEGREDO123"));
    }
}
