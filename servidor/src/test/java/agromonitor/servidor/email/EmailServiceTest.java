package agromonitor.servidor.email;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

// Nenhum e-mail real e enviado aqui.
class EmailServiceTest
{
    // cria um EmailService "ativo" apontando para host/porta de teste (construtor e privado)
    private static EmailService ativo(String host, int porta) throws Exception
    {
        Constructor<EmailService> c = EmailService.class.getDeclaredConstructor(
            String.class, String.class, String.class, String.class, String.class);
        c.setAccessible(true);
        return c.newInstance("usuario@teste.com", "senha", host, String.valueOf(porta), "usuario@teste.com");
    }

    // cria um EmailService "inativo" (sem SMTP), igual ao que doAmbiente() devolve sem variaveis
    private static EmailService inativo() throws Exception
    {
        Constructor<EmailService> c = EmailService.class.getDeclaredConstructor(
            String.class, String.class, String.class, String.class, String.class);
        c.setAccessible(true);
        return c.newInstance(null, null, "smtp.gmail.com", "587", null);
    }

    // ---------- sem SMTP (modo desenvolvimento) ----------
    @Test
    void semSmtpFicaInativoEAlertaNaoEEnviado() throws Exception
    {
        EmailService e = inativo();
        assertFalse(e.isAtivo());
        assertFalse(e.enviarAlerta("a@x.com", "Alerta de atividade", "t", "d", "Soja", "2026-10-28", "ALTA"));
    }

    @Test
    void semSmtpARecuperacaoImprimeOLinkNoConsole() throws Exception
    {
        PrintStream original = System.out;
        ByteArrayOutputStream capturado = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturado, true, "UTF-8"));
        try
        {
            inativo().enviarRecuperacao("Maria", "m@x.com", "http://localhost:8080/pages/auth/redefinirSenha.html?token=abc");
        }
        finally { System.setOut(original); }

        String saida = capturado.toString("UTF-8");
        assertTrue(saida.contains("m@x.com"));
        assertTrue(saida.contains("redefinirSenha.html?token=abc"));
    }

    // ---------- com SMTP configurado ----------
    @Test
    void comCredenciaisFicaAtivo() throws Exception
    {
        assertTrue(ativo("localhost", 1).isAtivo());
    }

    @Test
    void servidorInacessivelGeraExcecaoNoEnvio() throws Exception
    {
        int portaFechada;
        try (ServerSocket s = new ServerSocket(0)) { portaFechada = s.getLocalPort(); }
        EmailService e = ativo("localhost", portaFechada);

        assertThrows(Exception.class, () -> e.enviarAlerta("a@x.com", "Alerta", "t", "d", "Soja", "2026-10-28", "ALTA"));
        assertThrows(Exception.class, () -> e.enviarRecuperacao("Maria", "a@x.com", "http://localhost:8080/x"));
    }

    // SMTP falso SEM suporte a STARTTLS: o envio precisa ser recusado (nao pode mandar a senha sem criptografia)
    @Test
    void recusaEnviarSemCriptografia() throws Exception
    {
        try (ServerSocket falso = new ServerSocket(0))
        {
            Thread t = new Thread(() -> {
                try (Socket s = falso.accept())
                {
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    Writer out = new OutputStreamWriter(s.getOutputStream());
                    out.write("220 localhost ESMTP\r\n"); out.flush();
                    String linha;
                    while ((linha = in.readLine()) != null)
                    {
                        if (linha.toUpperCase().startsWith("EHLO") || linha.toUpperCase().startsWith("HELO"))
                            out.write("250-localhost\r\n250 AUTH PLAIN LOGIN\r\n");      // sem STARTTLS
                        else if (linha.toUpperCase().startsWith("QUIT"))
                        { out.write("221 bye\r\n"); out.flush(); break; }
                        else
                            out.write("250 OK\r\n");
                        out.flush();
                    }
                }
                catch (Exception ignorada) {}
            });
            t.setDaemon(true);
            t.start();

            EmailService e = ativo("localhost", falso.getLocalPort());
            Exception erro = assertThrows(Exception.class,
                () -> e.enviarAlerta("a@x.com", "Alerta", "t", "d", "Soja", "2026-10-28", "ALTA"));
            assertTrue(erro.getMessage().toUpperCase().contains("STARTTLS"), "mensagem: " + erro.getMessage());
        }
    }

    // ---------- escape de HTML do corpo do e-mail ----------
    @Test
    void escaparTrocaCaracteresPerigososDoHtml() throws Exception
    {
        Method m = EmailService.class.getDeclaredMethod("escapar", String.class);
        m.setAccessible(true);
        assertEquals("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt; &amp; fim", m.invoke(null, "<script>alert(\"x\")</script> & fim"));
        assertEquals("", m.invoke(null, (Object) null));
        assertEquals("texto normal", m.invoke(null, "texto normal"));
    }
}