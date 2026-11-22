// Laura Carvalho - 25014543

package agromonitor.servidor.validacao;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;


// Testa o ValidadorAtividade: cada metodo dele devolve null (ok) ou a mensagem de erro,
// entao os testes usam assertNull (passou) e assertEquals/assertNotNull (rejeitou)
class ValidadorAtividadeTest
{
    private final LocalDate hoje = LocalDate.now();

    @Test
    void ids()
    {
        assertNull(ValidadorAtividade.validarIds(1, 1));
        assertEquals("Usuario nao identificado. Entre novamente.", ValidadorAtividade.validarIds(0, 1));
        assertEquals("Selecione o plantio.", ValidadorAtividade.validarIds(1, 0));
        // quando os dois estao errados, o usuario vem primeiro
        assertEquals("Usuario nao identificado. Entre novamente.", ValidadorAtividade.validarIds(-1, -1));
    }

    // @ParameterizedTest roda o mesmo teste uma vez pra cada valor do @ValueSource
    // (aqui sao 6 execucoes, uma por tipo valido)
    @ParameterizedTest
    @ValueSource(strings = {"PLANTIO", "IRRIGACAO", "ADUBACAO", "TRATAMENTO", "MONITORAMENTO", "COLHEITA"})
    void tiposValidos(String tipo)
    {
        assertNull(ValidadorAtividade.validarTipo(tipo));
    }

    // @NullAndEmptySource acrescenta mais duas execucoes: uma com null e outra com ""
    // os outros valores mostram que o tipo diferencia maiuscula de minuscula ("Plantio" nao vale)
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"irrigacao", "PODA", "Plantio"})
    void tiposInvalidos(String tipo)
    {
        assertEquals("Tipo de atividade invalido.", ValidadorAtividade.validarTipo(tipo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDENTE", "CONCLUIDA", "ATRASADA"})
    void statusValidos(String s)
    {
        assertNull(ValidadorAtividade.validarStatus(s));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"pendente", "CANCELADA"})
    void statusInvalidos(String s)
    {
        assertEquals("Status da atividade invalido.", ValidadorAtividade.validarStatus(s));
    }

    // ---------- data x status ----------
    // primeiro a data em si: vazia ou impossivel (31/02) e rejeitada, independente do status
    @Test
    void dataObrigatoriaEReal()
    {
        assertEquals("Informe a data de execucao.", ValidadorAtividade.validarData(null, "PENDENTE"));
        assertEquals("Informe a data de execucao.", ValidadorAtividade.validarData("  ", "PENDENTE"));
        assertEquals("Data de execucao invalida.", ValidadorAtividade.validarData("2026-02-31", "PENDENTE"));
        assertEquals("Data de execucao invalida.", ValidadorAtividade.validarData("hoje", "PENDENTE"));
    }

    // os testes de status usam "hoje" como referencia (e plusDays/minusDays pra ir pro futuro/passado)
    // assim nao dependem do dia em que o teste roda
    @Test
    void pendente_hojeOuFuturoOk_passadoNao()
    {
        assertNull(ValidadorAtividade.validarData(hoje.toString(), "PENDENTE"));
        assertNull(ValidadorAtividade.validarData(hoje.plusDays(10).toString(), "PENDENTE"));
        assertNotNull(ValidadorAtividade.validarData(hoje.minusDays(1).toString(), "PENDENTE"));
    }

    // testa o limite: ontem passa, hoje ja nao (atrasada precisa ser ANTES de hoje)
    @Test
    void atrasada_somenteAntesDeHoje()
    {
        assertNull(ValidadorAtividade.validarData(hoje.minusDays(1).toString(), "ATRASADA"));
        assertNotNull(ValidadorAtividade.validarData(hoje.toString(), "ATRASADA"));
        assertNotNull(ValidadorAtividade.validarData(hoje.plusDays(1).toString(), "ATRASADA"));
    }

    @Test
    void concluida_hojeOuPassadoOk_futuroNao()
    {
        assertNull(ValidadorAtividade.validarData(hoje.toString(), "CONCLUIDA"));
        assertNull(ValidadorAtividade.validarData(hoje.minusDays(30).toString(), "CONCLUIDA"));
        assertEquals("Uma atividade concluida nao pode ter data futura.",
                     ValidadorAtividade.validarData(hoje.plusDays(1).toString(), "CONCLUIDA"));
    }

    // ---------- campos opcionais ----------
    // testa os dois lados do limite: exatamente 30 passa, 31 nao
    // "a".repeat(30) gera um texto de 30 letras sem precisar digitar
    @Test
    void quantidadeAte30()
    {
        assertNull(ValidadorAtividade.validarQuantidade(null));
        assertNull(ValidadorAtividade.validarQuantidade("22 mm"));
        assertNull(ValidadorAtividade.validarQuantidade("a".repeat(30)));
        assertNotNull(ValidadorAtividade.validarQuantidade("a".repeat(31)));
    }

    @Test
    void responsavelAte100()
    {
        assertNull(ValidadorAtividade.validarResponsavel(null));
        assertNull(ValidadorAtividade.validarResponsavel("a".repeat(100)));
        assertNotNull(ValidadorAtividade.validarResponsavel("a".repeat(101)));
    }

    @Test
    void observacoesAte200()
    {
        assertNull(ValidadorAtividade.validarObservacoes(null));
        assertNull(ValidadorAtividade.validarObservacoes("a".repeat(200)));
        assertNotNull(ValidadorAtividade.validarObservacoes("a".repeat(201)));
    }


    // ANOMALIA 5: o tipo nao e aparado (trim); quantidade e responsavel sao
    // (os testes de "anomalia" documentam o comportamento ATUAL do validador, mesmo quando e estranho:
    //  eles passam porque o codigo faz exatamente isso hoje)
    @ParameterizedTest
    @ValueSource(strings = {" PLANTIO", "PLANTIO ", " COLHEITA "})
    void anomalia_espacosNasPontasInvalidamOTipo(String tipo)
    {
        assertEquals("Tipo de atividade invalido.", ValidadorAtividade.validarTipo(tipo));
    }

    // ANOMALIA 6: com status nulo ou invalido nenhuma regra de estado e aplicada e qualquer data real passa
    // (no validarData, os ifs comparam com "CONCLUIDA", "ATRASADA" e "PENDENTE"; status estranho nao cai em nenhum)
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"CANCELADA", "pendente"})
    void anomalia_statusInvalidoDesligaAsRegrasDeData(String status)
    {
        assertNull(ValidadorAtividade.validarData(hoje.minusDays(100).toString(), status));
        assertNull(ValidadorAtividade.validarData(hoje.plusDays(100).toString(), status));
    }

    // ANOMALIA 7: nao existe limite de data, datas absurdas sao aceitas
    @Test
    void anomalia_dataMuitoAntigaOuMuitoFuturaEhAceita()
    {
        assertNull(ValidadorAtividade.validarData("1900-01-01", "CONCLUIDA"));
        assertNull(ValidadorAtividade.validarData("9999-12-31", "PENDENTE"));
    }

    // ANOMALIA 8: observacoes nao apara espacos, responsavel apara
    // (200 letras + 1 espaco = 201 e rejeita; 100 letras + 1 espaco passa porque o trim tira o espaco)
    @Test
    void anomalia_observacoesNaoApararEspacos()
    {
        assertNotNull(ValidadorAtividade.validarObservacoes("a".repeat(200) + " "));
        assertNull(ValidadorAtividade.validarResponsavel("a".repeat(100) + " "));
    }
}