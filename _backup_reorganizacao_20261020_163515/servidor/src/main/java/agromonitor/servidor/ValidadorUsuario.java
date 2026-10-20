package agromonitor.servidor;

import java.util.regex.Pattern;

// Regras do dicionario de dados para o usuario.
// Cada metodo devolve null se estiver OK, ou a mensagem de erro.
public class ValidadorUsuario
{
    private static final Pattern EMAIL =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static String validarNome(String nome)
    {
        if (nome == null || nome.trim().length() < 2)
            return "Informe o nome completo.";
        return null;
    }

    public static String validarEmail(String email)
    {
        if (email == null || !EMAIL.matcher(email.trim()).matches())
            return "E-mail invalido.";
        return null;
    }

    // telefone e opcional; se vier, precisa ter pelo menos um digito
    public static String validarTelefone(String telefone)
    {
        if (telefone == null || telefone.trim().isEmpty())
            return null;
        if (!telefone.matches(".*\\d.*"))
            return "Telefone invalido.";
        return null;
    }

    public static String validarSenha(String senha)
    {
        if (senha == null || senha.length() < 6)
            return "A senha deve ter pelo menos 6 caracteres.";
        return null;
    }

    // CPF e opcional; se vier, precisa ser valido
    public static String validarCpf(String cpf)
    {
        if (cpf == null || cpf.trim().isEmpty())
            return null;
        if (!cpfValido(somenteDigitos(cpf)))
            return "CPF invalido.";
        return null;
    }

    public static String somenteDigitos(String s)
    {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static boolean cpfValido(String c)
    {
        if (c.length() != 11 || c.matches("(\\d)\\1{10}"))
            return false;
        return c.charAt(9)  - '0' == digito(c, 9)
            && c.charAt(10) - '0' == digito(c, 10);
    }

    private static int digito(String c, int tamanho)
    {
        int soma = 0;
        for (int i = 0; i < tamanho; i++)
            soma += (c.charAt(i) - '0') * (tamanho + 1 - i);
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
