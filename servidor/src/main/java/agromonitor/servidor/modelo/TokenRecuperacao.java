// Giovana Uchelli - 25008818

package agromonitor.servidor.modelo;

import java.nio.charset.StandardCharsets; // Define o conjunto de caracteres (UTF-8) para converter textos em bytes
import java.security.MessageDigest; // Usado para calcular o hash
import java.security.SecureRandom; // Gerador de numeros aleatórios
import java.util.Base64; // Converte bytes em texto (Base 64)
import java.util.HexFormat; // Converte bytes em texto hexadecimal

// Token de recuperacao de senha.
// - gerar(): 32 bytes aleatorios, em texto seguro para URL. Vai SO no link do e-mail.
// - hash(): SHA-256 do token. E isso que fica no banco; se o banco vazar, o link nao pode ser refeito.
public class TokenRecuperacao
{
    private static final SecureRandom ALEATORIO = new SecureRandom(); // Gerador aleatorio

    public static String gerar()
    {
        byte[] bytes = new byte[32]; // Cria 32 bytes aleatorios
        ALEATORIO.nextBytes(bytes);

        // Transforma os bytes em texto que pode ser usado na URL
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    // Transforma o token em um hash (o que vai ao banco)
    public static String hash(String token)
    {
        try
        {
            // Aplica o SHA-256 sobre o texto do token
            byte[] h = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            // Converte o resultado para texto haxadecimal
            return HexFormat.of().formatHex(h);
        }
        catch (java.security.NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e); // SHA-256 existe em toda JVM
        }
    }
}
