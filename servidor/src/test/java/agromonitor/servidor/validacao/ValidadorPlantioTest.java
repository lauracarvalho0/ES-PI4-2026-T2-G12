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

class ValidadorPlantioTest
{
    @Test
    void usuario()
    {
        assertNull(ValidadorPlantio.validarUsuario(1));
        assertNotNull(ValidadorPlantio.validarUsuario(0));
        assertNotNull(ValidadorPlantio.validarUsuario(-1));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void culturaObrigatoria(String cultura)
    {
        assertEquals("Selecione a cultura.", ValidadorPlantio.validarCultura(cultura));
        assertNull(ValidadorPlantio.validarCultura("Soja"));
    }

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

    @ParameterizedTest
    @ValueSource(strings = {"25/26", "00/01", " 24/25 "})
    void safraValida(String safra)
    {
        assertNull(ValidadorPlantio.validarSafra(safra));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2526", "25-26", "25/2", "2025/26", "ab/cd", "25/26/27"})
    void safraInvalida(String safra)
    {
        assertEquals("Informe a safra no formato 25/26.", ValidadorPlantio.validarSafra(safra));
    }

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

    @Test
    void altitudeOpcionalNaoNegativa()
    {
        assertNull(ValidadorPlantio.validarAltitude(null));
        assertNull(ValidadorPlantio.validarAltitude(0));
        assertNull(ValidadorPlantio.validarAltitude(850));
        assertNotNull(ValidadorPlantio.validarAltitude(-1));
    }

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

    @Test
    void dataDePlantio()
    {
        assertNull(ValidadorPlantio.validarDataPlantio("2026-10-21"));
        assertEquals("Informe a data de plantio.", ValidadorPlantio.validarDataPlantio(null));
        assertEquals("Informe a data de plantio.", ValidadorPlantio.validarDataPlantio("  "));
        assertEquals("Data de plantio invalida.", ValidadorPlantio.validarDataPlantio("21/10/2026"));
        assertEquals("Data de plantio invalida.", ValidadorPlantio.validarDataPlantio("2026-02-31"));
    }

    @Test
    void ajusteOpcionalMasPrecisaSerData()
    {
        assertNull(ValidadorPlantio.validarAjuste(null, "Irrigacao"));
        assertNull(ValidadorPlantio.validarAjuste("", "Irrigacao"));
        assertNull(ValidadorPlantio.validarAjuste("2026-11-01", "Irrigacao"));
        assertEquals("Data ajustada de Irrigacao invalida.", ValidadorPlantio.validarAjuste("amanha", "Irrigacao"));
    }

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
