// Autoria: Beatriz Leme — RA 25015554.

package agromonitor.servidor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import agromonitor.servidor.RegrasDeAlerta.Classificacao;


class RegrasDeAlertaTest
{
    // data fixa: o teste nao depende do dia em que roda
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 13);

    @Nested
    @DisplayName("Categoria A - calculo: classificar()")
    class Calculo
    {
        @ParameterizedTest(name = "atrasada ha {0} dia(s) -> CRITICO")
        @ValueSource(ints = {3, 4, 30, 365})
        void atrasoDeTresDiasOuMaisEhCritico(int diasDeAtraso)
        {
            Classificacao c = RegrasDeAlerta.classificar(HOJE, HOJE.minusDays(diasDeAtraso));

            assertEquals(RegrasDeAlerta.ATRASADA, c.motivo());
            assertEquals("CRITICO", c.prioridade());
            assertEquals("Atividade atrasada", c.titulo());
        }

        @ParameterizedTest(name = "atrasada ha {0} dia(s) -> ALTA")
        @ValueSource(ints = {1, 2})
        void atrasoDeUmOuDoisDiasEhAlta(int diasDeAtraso)
        {
            Classificacao c = RegrasDeAlerta.classificar(HOJE, HOJE.minusDays(diasDeAtraso));

            assertEquals(RegrasDeAlerta.ATRASADA, c.motivo());
            assertEquals("ALTA", c.prioridade());
            assertEquals("Atividade atrasada", c.titulo());
        }

        @Test
        void atividadeDeHojeEhAltaEProxima()
        {
            Classificacao c = RegrasDeAlerta.classificar(HOJE, HOJE);

            assertEquals(RegrasDeAlerta.PROXIMA, c.motivo());
            assertEquals("ALTA", c.prioridade());
            assertEquals("Atividade para hoje", c.titulo());
        }

        @ParameterizedTest(name = "faltam {0} dia(s) -> MEDIA")
        @ValueSource(ints = {1, 2})
        void faltandoUmOuDoisDiasEhMedia(int diasQueFaltam)
        {
            Classificacao c = RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(diasQueFaltam));

            assertEquals(RegrasDeAlerta.PROXIMA, c.motivo());
            assertEquals("MEDIA", c.prioridade());
            assertEquals("Atividade se aproxima", c.titulo());
        }

        @ParameterizedTest(name = "faltam {0} dia(s) -> sem alerta")
        @ValueSource(ints = {3, 4, 30, 365})
        void faltandoTresDiasOuMaisNaoGeraAlerta(int diasQueFaltam)
        {
            assertNull(RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(diasQueFaltam)));
        }

        @Test
        void fronteirasUsamAsConstantesDaClasse()
        {
            assertEquals(3, RegrasDeAlerta.DIAS_CRITICO);
            assertEquals(2, RegrasDeAlerta.DIAS_AVISO);
            // a ultima data que ainda avisa e a primeira que nao avisa
            assertNotNull(RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(RegrasDeAlerta.DIAS_AVISO)));
            assertNull(RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(RegrasDeAlerta.DIAS_AVISO + 1)));
        }

        @Test
        void viradaDeMesEDeAnoContaDiasCorretamente()
        {
            LocalDate fimDoAno = LocalDate.of(2026, 12, 31);
            assertEquals("MEDIA", RegrasDeAlerta.classificar(fimDoAno, LocalDate.of(2027, 1, 2)).prioridade());
            assertNull(RegrasDeAlerta.classificar(fimDoAno, LocalDate.of(2027, 1, 3)));
            // 29/02 de ano bissexto
            assertEquals("ALTA", RegrasDeAlerta.classificar(LocalDate.of(2028, 3, 1), LocalDate.of(2028, 2, 29)).prioridade());
        }

        @Test
        void cadaTipoDeAlertaTemTresRecomendacoesDiferentesEntreSi()
        {
            Classificacao atrasada = RegrasDeAlerta.classificar(HOJE, HOJE.minusDays(1));
            Classificacao hoje     = RegrasDeAlerta.classificar(HOJE, HOJE);
            Classificacao proxima  = RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(1));

            for (Classificacao c : List.of(atrasada, hoje, proxima))
                assertEquals(3, c.recomendacoes().size());

            assertNotEquals(atrasada.recomendacoes(), hoje.recomendacoes());
            assertNotEquals(hoje.recomendacoes(), proxima.recomendacoes());
            // ALTA atrasada e CRITICO usam a mesma lista de recomendacoes
            assertEquals(atrasada.recomendacoes(), RegrasDeAlerta.classificar(HOJE, HOJE.minusDays(5)).recomendacoes());
        }

        // ANOMALIA 1: datas nulas nao sao tratadas, estoura NullPointerException
        @Test
        void anomalia_dataNulaEstouraNullPointerException()
        {
            assertThrows(NullPointerException.class, () -> RegrasDeAlerta.classificar(HOJE, null));
            assertThrows(NullPointerException.class, () -> RegrasDeAlerta.classificar(null, HOJE));
        }
    }

    @Nested
    @DisplayName("Categoria B - consulta: nomeDoTipo()")
    class Consulta
    {
        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "PLANTIO,       Plantio",
            "IRRIGACAO,     Irrigação",
            "ADUBACAO,      Adubação de cobertura",
            "TRATAMENTO,    Tratos culturais",
            "MONITORAMENTO, Monitoramento",
            "COLHEITA,      Colheita"
        })
        void tiposConhecidosTemNomeDeTela(String tipo, String nomeEsperado)
        {
            assertEquals(nomeEsperado, RegrasDeAlerta.nomeDoTipo(tipo));
        }

        @ParameterizedTest
        @NullSource
        void tipoNuloViraAtividadeGenerica(String tipo)
        {
            assertEquals("Atividade", RegrasDeAlerta.nomeDoTipo(tipo));
        }

        @Test
        void tipoDesconhecidoEhDevolvidoSemAlteracao()
        {
            assertEquals("PODA", RegrasDeAlerta.nomeDoTipo("PODA"));
            assertEquals("", RegrasDeAlerta.nomeDoTipo(""));
        }

        // ANOMALIA 2: a comparacao diferencia maiusculas de minusculas e nao ignora espacos
        @ParameterizedTest
        @ValueSource(strings = {"irrigacao", "Irrigacao", " IRRIGACAO"})
        void anomalia_grafiaDiferenteNaoEReconhecida(String tipo)
        {
            assertEquals(tipo, RegrasDeAlerta.nomeDoTipo(tipo));
        }
    }

    @Nested
    @DisplayName("Categoria C - formatacao: descricao() e rotuloDaData()")
    class Formatacao
    {
        private final LocalDate data = LocalDate.of(2026, 3, 5);

        @Test
        void descricaoDeAtividadeAtrasada()
        {
            Classificacao c = RegrasDeAlerta.classificar(HOJE, HOJE.minusDays(2));

            assertEquals("A atividade \"Irrigação\" do plantio Soja Norte estava prevista para 05/03 e ainda não foi concluída.",
                         RegrasDeAlerta.descricao(c, "Irrigação", "Soja Norte", data));
        }

        @Test
        void descricaoDeAtividadeDeHoje()
        {
            Classificacao c = RegrasDeAlerta.classificar(HOJE, HOJE);

            assertEquals("A atividade \"Colheita\" do plantio Milho 1 está prevista para hoje (05/03).",
                         RegrasDeAlerta.descricao(c, "Colheita", "Milho 1", data));
        }

        @Test
        void descricaoDeAtividadeProxima()
        {
            Classificacao c = RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(2));

            assertEquals("A atividade \"Plantio\" do plantio Feijao está prevista para 05/03.",
                         RegrasDeAlerta.descricao(c, "Plantio", "Feijao", data));
        }

        @Test
        void diaEMesSempreComDoisDigitos()
        {
            Classificacao c = RegrasDeAlerta.classificar(HOJE, HOJE.plusDays(1));

            assertTrue(RegrasDeAlerta.descricao(c, "Plantio", "X", LocalDate.of(2026, 1, 9)).endsWith("09/01."));
        }

        @Test
        void rotuloDaDataUsaDiaMesAno()
        {
            assertEquals("Prevista para 05/03/2026", RegrasDeAlerta.rotuloDaData(data));
            assertEquals("Prevista para 31/12/2026", RegrasDeAlerta.rotuloDaData(LocalDate.of(2026, 12, 31)));
        }

        // ANOMALIA 3: descricao() descobre se e "hoje" comparando o TEXTO do titulo
        // (c.titulo().equals("Atividade para hoje")). Se alguem mudar o titulo em classificar(),
        // a descricao de hoje passa a sair como a de "proxima", sem nenhum erro.
        @Test
        void anomalia_descricaoDependeDoTextoDoTitulo()
        {
            Classificacao hojeComOutroTitulo = new Classificacao(RegrasDeAlerta.PROXIMA, "ALTA", "Para hoje!", List.of());

            assertEquals("A atividade \"Plantio\" do plantio X está prevista para 05/03.",
                         RegrasDeAlerta.descricao(hojeComOutroTitulo, "Plantio", "X", data));
        }

        // ANOMALIA 4: data nula tambem nao e tratada nos metodos de formatacao
        @Test
        void anomalia_dataNulaEstouraExcecao()
        {
            Classificacao c = RegrasDeAlerta.classificar(HOJE, HOJE);

            assertThrows(NullPointerException.class, () -> RegrasDeAlerta.descricao(c, "Plantio", "X", null));
            assertThrows(NullPointerException.class, () -> RegrasDeAlerta.rotuloDaData(null));
        }
    }
}
