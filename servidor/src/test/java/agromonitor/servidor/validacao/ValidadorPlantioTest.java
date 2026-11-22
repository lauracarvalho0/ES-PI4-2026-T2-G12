// Laura Carvalho - 25014543

package agromonitor.servidor.validacao;

import java.time.LocalDate; // LocalDate: "hoje" e datas relativas (plusDays/minusDays) nos testes de data x status

import static org.junit.jupiter.api.Assertions.assertEquals;  // Confere se a mensagem de erro é exatamente a esperada
import static org.junit.jupiter.api.Assertions.assertNotNull;  // Confere que o validador rejeitou (devolveu alguma mensagem)
import static org.junit.jupiter.api.Assertions.assertNull; // Confere que o validador aceitou (null = ok)
import org.junit.jupiter.api.Test;  // @Test: marca um método como teste
import org.junit.jupiter.params.ParameterizedTest; // @ParameterizedTest: roda o mesmo teste com vários valores
import org.junit.jupiter.params.provider.NullAndEmptySource; // Acrescenta as execuções com null e ""
import org.junit.jupiter.params.provider.ValueSource;  // Lista os valores usados em cada execução do teste

// Testa o ValidadorPlantio: cada metodo dele devolve null (ok) ou a mensagem de erro,
// entao os testes usam assertNull (passou) e assertEquals/assertNotNull (rejeitou)
class ValidadorPlantioTest
{
    // fronteira do codigo: 1 passa, 0 e negativo nao
    @Test
    void usuario()
    {
        assertNull(ValidadorPlantio.validarUsuario(1));
        assertNotNull(ValidadorPlantio.validarUsuario(0));
        assertNotNull(ValidadorPlantio.validarUsuario(-1));
    }

    // @ParameterizedTest roda o mesmo teste uma vez por valor: null, "" (do @NullAndEmptySource) e "  "
    // (3 execucoes); o assertNull no fim confere que uma cultura preenchida passa
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void culturaObrigatoria(String cultura)
    {
        assertEquals("Selecione a cultura.", ValidadorPlantio.validarCultura(cultura));
        assertNull(ValidadorPlantio.validarCultura("Soja"));
    }

    // testa os dois lados do limite: exatamente 100 passa, 101 nao
    // "a".repeat(100) gera um texto de 100 letras sem precisar digitar
    @Test
    void nomeObrigatorioEAte100Caracteres()
    {
        assertNotNull(ValidadorPlantio.validarNome(null));
        assertNotNull(ValidadorPlantio.validarNome("   "));
        assertNull(ValidadorPlantio.validarNome("Soja Talhao 1"));
        assertNull(ValidadorPlantio.validarNome("a".repeat(100)));
        assertNotNull(ValidadorPlantio.validarNome("a".repeat(101)));
        // espacos nas pontas nao contam
        assertNull(ValidadorPlantio.validarNome("  " + "a".repeat(100) + "  "));
    }

    // formatos aceitos pela regex ^\d{2}/\d{2}$ (o " 24/25 " passa porque o validador usa trim)
    @ParameterizedTest
    @ValueSource(strings = {"25/26", "00/01", " 24/25 "})
    void safraValida(String safra)
    {
        assertNull(ValidadorPlantio.validarSafra(safra));
    }

    // cada valor quebra a regex de um jeito: sem barra, separador errado, digitos a menos/a mais,
    // letras, ou texto extra depois (8 execucoes com o null e o "")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2526", "25-26", "25/2", "2025/26", "ab/cd", "25/26/27"})
    void safraInvalida(String safra)
    {
        assertEquals("Informe a safra no formato 25/26.", ValidadorPlantio.validarSafra(safra));
    }

    // campo opcional: null e "" passam; so o tamanho e conferido (200 ok, 201 nao)
    @Test
    void descricaoOpcionalAte200()
    {
        assertNull(ValidadorPlantio.validarDescricao(null));
        assertNull(ValidadorPlantio.validarDescricao(""));
        assertNull(ValidadorPlantio.validarDescricao("a".repeat(200)));
        assertNotNull(ValidadorPlantio.validarDescricao("a".repeat(201)));
    }

    @Test
    void sistemaCultivo()
    {
        assertNull(ValidadorPlantio.validarSistemaCultivo("SEQUEIRO"));
        assertNull(ValidadorPlantio.validarSistemaCultivo("IRRIGADO"));
        assertNotNull(ValidadorPlantio.validarSistemaCultivo("sequeiro")); // diferencia maiuscula
        assertNotNull(ValidadorPlantio.validarSistemaCultivo("OUTRO"));
        assertNotNull(ValidadorPlantio.validarSistemaCultivo(null));
    }

    @Test
    void talhaoObrigatorio()
    {
        assertNull(ValidadorPlantio.validarTalhao("T1"));
        assertNotNull(ValidadorPlantio.validarTalhao(null));
        assertNotNull(ValidadorPlantio.validarTalhao("  "));
    }

    // NaN e infinito sao numeros "quebrados" do double que tambem precisam ser recusados
    @Test
    void areaPlantadaPrecisaSerPositiva()
    {
        assertNull(ValidadorPlantio.validarAreaPlantada(0.01));
        assertNull(ValidadorPlantio.validarAreaPlantada(120.5));
        assertNotNull(ValidadorPlantio.validarAreaPlantada(null));
        assertNotNull(ValidadorPlantio.validarAreaPlantada(0.0));
        assertNotNull(ValidadorPlantio.validarAreaPlantada(-3.0));
        assertNotNull(ValidadorPlantio.validarAreaPlantada(Double.NaN));
        assertNotNull(ValidadorPlantio.validarAreaPlantada(Double.POSITIVE_INFINITY));
    }

    // 0 e valido (so negativo e rejeitado)
    @Test
    void altitudeOpcionalNaoNegativa()
    {
        assertNull(ValidadorPlantio.validarAltitude(null));
        assertNull(ValidadorPlantio.validarAltitude(0));
        assertNull(ValidadorPlantio.validarAltitude(850));
        assertNotNull(ValidadorPlantio.validarAltitude(-1));
    }

    // vazio passa; se vier preenchido, tem que ser um dos 3 metodos da lista
    @Test
    void metodoPlantioOpcionalMasRestrito()
    {
        assertNull(ValidadorPlantio.validarMetodoPlantio(null));
        assertNull(ValidadorPlantio.validarMetodoPlantio(""));
        assertNull(ValidadorPlantio.validarMetodoPlantio("PLANTIO_DIRETO"));
        assertNull(ValidadorPlantio.validarMetodoPlantio("CONVENCIONAL"));
        assertNull(ValidadorPlantio.validarMetodoPlantio("MINIMO"));
        assertEquals("Metodo de plantio invalido.", ValidadorPlantio.validarMetodoPlantio("AEREO"));
    }

    // o formato aceito e yyyy-MM-dd; "21/10/2026" e rejeitado, assim como data impossivel (31/02)
    @Test
    void dataDePlantio()
    {
        assertNull(ValidadorPlantio.validarDataPlantio("2026-10-21"));
        assertEquals("Informe a data de plantio.", ValidadorPlantio.validarDataPlantio(null));
        assertEquals("Informe a data de plantio.", ValidadorPlantio.validarDataPlantio("  "));
        assertEquals("Data de plantio invalida.", ValidadorPlantio.validarDataPlantio("21/10/2026"));
        assertEquals("Data de plantio invalida.", ValidadorPlantio.validarDataPlantio("2026-02-31"));
    }

    // o nome da atividade ("Irrigacao") so entra na mensagem de erro
    @Test
    void ajusteOpcionalMasPrecisaSerData()
    {
        assertNull(ValidadorPlantio.validarAjuste(null, "Irrigacao"));
        assertNull(ValidadorPlantio.validarAjuste("", "Irrigacao"));
        assertNull(ValidadorPlantio.validarAjuste("2026-11-01", "Irrigacao"));
        assertEquals("Data ajustada de Irrigacao invalida.", ValidadorPlantio.validarAjuste("amanha", "Irrigacao"));
    }

    // aqui o retorno e uma data (ou null), entao compara com LocalDate.of(...) em vez de mensagem
    // casos de borda: espacos nas pontas, ano bissexto (29/02 so existe em 2028), mes 13, null e vazio
    @Test
    void lerData()
    {
        assertEquals(LocalDate.of(2026, 2, 28), ValidadorPlantio.lerData("2026-02-28"));
        assertEquals(LocalDate.of(2026, 2, 28), ValidadorPlantio.lerData("  2026-02-28 "));
        assertEquals(LocalDate.of(2028, 2, 29), ValidadorPlantio.lerData("2028-02-29")); // bissexto
        assertNull(ValidadorPlantio.lerData("2026-02-29"));
        assertNull(ValidadorPlantio.lerData("2026-13-01"));
        assertNull(ValidadorPlantio.lerData(null));
        assertNull(ValidadorPlantio.lerData(""));
    }
}