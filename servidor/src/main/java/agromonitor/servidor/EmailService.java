package agromonitor.servidor;

import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

// Envia e-mails por SMTP. Configurado por variaveis de ambiente:
//   SMTP_USER  (obrigatoria)  ex.: agromonitor.sistema@gmail.com
//   SMTP_PASS  (obrigatoria)  senha de app do Gmail (16 letras, sem espacos)
//   SMTP_HOST  (opcional)     padrao: smtp.gmail.com
//   SMTP_PORT  (opcional)     padrao: 587
//   SMTP_FROM  (opcional)     padrao: o proprio SMTP_USER
// Sem SMTP_USER/SMTP_PASS o servidor entra em "modo desenvolvimento":
// em vez de enviar, imprime o link no console do servidor.
public class EmailService
{
    private final String usuario;
    private final String senha;
    private final String host;
    private final String porta;
    private final String remetente;

    private EmailService(String usuario, String senha, String host, String porta, String remetente)
    {
        this.usuario   = usuario;
        this.senha     = senha;
        this.host      = host;
        this.porta     = porta;
        this.remetente = remetente;
    }

    public static EmailService doAmbiente()
    {
        String u = System.getenv("SMTP_USER");
        String s = System.getenv("SMTP_PASS");
        String h = valorOu(System.getenv("SMTP_HOST"), "smtp.gmail.com");
        String p = valorOu(System.getenv("SMTP_PORT"), "587");

        if (u == null || u.isBlank() || s == null || s.isBlank())
            return new EmailService(null, null, h, p, null);

        // o Google mostra a senha de app em grupos de 4 letras; tira os espacos
        return new EmailService(u.trim(), s.replace(" ", "").trim(), h, p,
                                valorOu(System.getenv("SMTP_FROM"), u.trim()));
    }

    private static String valorOu(String v, String padrao)
    {
        return (v == null || v.isBlank()) ? padrao : v.trim();
    }

    public boolean isAtivo()
    {
        return this.usuario != null;
    }

    public void enviarRecuperacao(String nome, String destino, String link) throws Exception
    {
        if (!isAtivo())
        {
            System.out.println("\n[MODO DESENVOLVIMENTO] SMTP nao configurado, e-mail NAO enviado.");
            System.out.println("Link de recuperacao para " + destino + ":\n" + link + "\n");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", this.host);
        props.put("mail.smtp.port", this.porta);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        final String u = this.usuario;
        final String s = this.senha;
        Session sessao = Session.getInstance(props, new Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(u, s);
            }
        });

        String texto =
            "Ola, " + nome + "!\n\n" +
            "Recebemos um pedido para redefinir a senha da sua conta no AgroMonitor.\n" +
            "Para criar uma senha nova, abra o link abaixo (valido por 30 minutos):\n\n" +
            link + "\n\n" +
            "Se voce nao fez esse pedido, ignore este e-mail: sua senha continua a mesma.";

        String html =
            "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto;color:#1a1a1a\">" +
            "<h2 style=\"color:#163a2b\">AgroMonitor</h2>" +
            "<p>Ol&aacute;, " + escapar(nome) + "!</p>" +
            "<p>Recebemos um pedido para redefinir a senha da sua conta.</p>" +
            "<p><a href=\"" + escapar(link) + "\" style=\"display:inline-block;background:#163a2b;color:#fff;" +
            "padding:12px 24px;border-radius:999px;text-decoration:none;font-weight:bold\">Criar nova senha</a></p>" +
            "<p style=\"font-size:13px;color:#5b5b5b\">O link vale por 30 minutos e s&oacute; pode ser usado uma vez.<br>" +
            "Se o bot&atilde;o n&atilde;o funcionar, copie e cole no navegador:<br>" + escapar(link) + "</p>" +
            "<p style=\"font-size:13px;color:#5b5b5b\">Se voc&ecirc; n&atilde;o fez esse pedido, ignore este e-mail: " +
            "sua senha continua a mesma.</p></div>";

        MimeBodyPart parteTexto = new MimeBodyPart();
        parteTexto.setText(texto, "UTF-8");
        MimeBodyPart parteHtml = new MimeBodyPart();
        parteHtml.setContent(html, "text/html; charset=UTF-8");

        MimeMultipart corpo = new MimeMultipart("alternative");
        corpo.addBodyPart(parteTexto);
        corpo.addBodyPart(parteHtml);

        MimeMessage mensagem = new MimeMessage(sessao);
        mensagem.setFrom(new InternetAddress(this.remetente, "AgroMonitor", "UTF-8"));
        mensagem.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino, false));
        mensagem.setSubject("Recuperação de senha - AgroMonitor", "UTF-8");
        mensagem.setContent(corpo);

        Transport.send(mensagem);
    }

    private static String escapar(String s)
    {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;")
                                 .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
