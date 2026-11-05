package agromonitor.servidor.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TokenRecuperacaoTest
{
    @Test
    void tokenTem43CaracteresUrlSafe()
    {
        // 32 bytes em Base64 URL sem padding = 43 caracteres
        String t = TokenRecuperacao.gerar();
        assertEquals(43, t.length());
        assertTrue(t.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void tokensSaoUnicos()
    {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 1000; i++)
            assertTrue(vistos.add(TokenRecuperacao.gerar()));
    }

    @Test
    void hashEDeterministicoEHexadecimalDe64Posicoes()
    {
        String h1 = TokenRecuperacao.hash("abc");
        String h2 = TokenRecuperacao.hash("abc");
        assertEquals(h1, h2);
        assertEquals(64, h1.length());
        assertTrue(h1.matches("[0-9a-f]{64}"));
    }

    @Test
    void hashConhecidoSha256()
    {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                     TokenRecuperacao.hash("abc"));
    }

    @Test
    void hashNaoDevolveOProprioToken()
    {
        String t = TokenRecuperacao.gerar();
        assertNotEquals(t, TokenRecuperacao.hash(t));
    }
}
