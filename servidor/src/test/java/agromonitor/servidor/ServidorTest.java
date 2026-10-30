package agromonitor.servidor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class ServidorTest
{
    private static String capturarErro(Runnable acao) throws Exception
    {
        PrintStream original = System.err;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        System.setErr(new PrintStream(bytes, true, "UTF-8"));
        try { acao.run(); } finally { System.setErr(original); }
        return bytes.toString("UTF-8");
    }

    @Test
    void portaPadraoEA3000()
    {
        assertEquals("3000", Servidor.PORTA_PADRAO);
    }

    @Test
    void argumentosDemaisMostramOUsoEEncerram() throws Exception
    {
        String erro = capturarErro(() -> Servidor.main(new String[] { "3000", "extra" }));
        assertTrue(erro.contains("Uso esperado"));
    }

    // so roda se voce NAO tiver MONGO_URI definida (senao o servidor de verdade tentaria subir)
    @Test
    void semMongoUriAvisaEEncerra() throws Exception
    {
        Assumptions.assumeTrue(System.getenv("MONGO_URI") == null || System.getenv("MONGO_URI").isBlank());
        String erro = capturarErro(() -> Servidor.main(new String[0]));
        assertTrue(erro.contains("MONGO_URI"));
    }
}
