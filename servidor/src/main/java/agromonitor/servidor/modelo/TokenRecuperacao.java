package agromonitor.servidor.modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// Token de recuperacao de senha.
// - gerar(): 32 bytes aleatorios, em texto seguro para URL. Vai SO no link do e-mail.
// - hash(): SHA-256 do token. E isso que fica no banco; se o banco vazar, o link nao pode ser refeito.
public class TokenRecuperacao
{
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public static String gerar()
    {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token)
    {
        try
        {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        }
        catch (java.security.NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e); // SHA-256 existe em toda JVM
        }
    }
}
