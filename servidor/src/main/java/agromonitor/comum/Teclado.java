package agromonitor.comum;

import java.io.*;

// Leitura do que o administrador digita no console do servidor.
public class Teclado
{
    private static final BufferedReader entrada =
        new BufferedReader(new InputStreamReader(System.in));

    public static String getUmString() throws Exception
    {
        String linha = entrada.readLine();
        if (linha == null)
            throw new Exception("Entrada encerrada");
        return linha;
    }
}
