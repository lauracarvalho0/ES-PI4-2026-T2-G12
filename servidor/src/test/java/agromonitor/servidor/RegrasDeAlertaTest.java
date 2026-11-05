package agromonitor.servidor;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import agromonitor.servidor.RegrasDeAlerta.Classificacao;

class RegrasDeAlertaTest
{
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 28);

    // dias de diferenca (data prevista - hoje) -> motivo, prioridade, titulo
    @ParameterizedTest(name = "{0} dia(s) de diferenca -> {1}/{2}")
    @CsvSource({
        "-10, ATRASADA, CRITICO, Atividade atrasada",
        "-3,  ATRASADA, CRITICO, Atividade atrasada",
        "-2,  ATRASADA, ALTA,    Atividade atrasada",
        "-1,  ATRASADA, ALTA,    Atividade atrasada",
        " 0,  PROXIMA,  ALTA,    Atividade para hoje",
        " 1,  PROXIMA,  MEDIA,   Atividade se aproxima",
        " 2,  PROXIMA,  MEDIA,   Atividade se aproxima"
    })
    void classifica(long dias, String motivo, String prioridade, String titulo)
    {
        Classificacao c = RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(dias));
        assertNotNull(c);
        assertEquals(motivo, c.motivo());
        assertEquals(prioridade, c.prioridade());
        assertEquals(titulo, c.titulo());
        assertEquals(3, c.recomendacoes().size());
    }

    @ParameterizedTest
    @CsvSource({"3", "4", "30", "365"})
    void atividadeLongeNaoGeraAlerta(long dias)
    {
        assertNull(RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(dias)));
    }

    @Test
    void recomendacoesDependemDoTipoDeAlerta()
    {
        var atrasada = RegrasDeAlerta.classificar(HOJE, HOJE.minusDays(1));
        var hoje     = RegrasDeAlerta.classificar(HOJE, HOJE);
        var proxima  = RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(1));

        assertNotEquals(atrasada.recomendacoes(), hoje.recomendacoes());
        assertNotEquals(hoje.recomendacoes(), proxima.recomendacoes());
        assertNotEquals(atrasada.recomendacoes(), proxima.recomendacoes());
    }

    @Test
    void listaDeRecomendacoesNaoPodeSerAlterada()
    {
        var c = RegrasDeAlerta.classificar(HOJE, HOJE);
        assertThrows(UnsupportedOperationException.class, () -> c.recomendacoes().add("x"));
    }

    @ParameterizedTest
    @CsvSource({
        "PLANTIO,       Plantio",
        "IRRIGACAO,     Irrigação",
        "ADUBACAO,      Adubação de cobertura",
        "TRATAMENTO,    Tratos culturais",
        "MONITORAMENTO, Monitoramento",
        "COLHEITA,      Colheita",
        "OUTRO,         OUTRO"
    })
    void nomeDoTipo(String tipo, String esperado)
    {
        assertEquals(esperado, RegrasDeAlerta.nomeDoTipo(tipo));
    }

    @Test
    void nomeDoTipoNulo()
    {
        assertEquals("Atividade", RegrasDeAlerta.nomeDoTipo(null));
    }

    @Test
    void descricaoDeAtrasada()
    {
        var c = RegrasDeAlerta.classificar(HOJE, HOJE.minusDays(2));
        String d = RegrasDeAlerta.descricao(c, "Colheita", "Soja T1", HOJE.minusDays(2));
        assertEquals("A atividade \"Colheita\" do plantio Soja T1 estava prevista para 26/10 e ainda não foi concluída.", d);
    }

    @Test
    void descricaoDeHoje()
    {
        var c = RegrasDeAlerta.classificar(HOJE, HOJE);
        String d = RegrasDeAlerta.descricao(c, "Irrigação", "Milho", HOJE);
        assertEquals("A atividade \"Irrigação\" do plantio Milho está prevista para hoje (28/10).", d);
    }

    @Test
    void descricaoDeProxima()
    {
        var c = RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(2));
        String d = RegrasDeAlerta.descricao(c, "Plantio", "Feijão", HOJE.plusDays(2));
        assertEquals("A atividade \"Plantio\" do plantio Feijão está prevista para 30/10.", d);
    }

    @Test
    void rotuloDaData()
    {
        assertEquals("Prevista para 05/01/2027", RegrasDeAlerta.rotuloDaData(LocalDate.of(2027, 1, 5)));
    }
}
