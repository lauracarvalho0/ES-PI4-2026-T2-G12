// Giovana Uchelli - 25008818
package agromonitor.servidor.validacao;

import java.util.regex.Pattern; // Permite criar e usar expressões regulares

// Regras do dicionario de dados para o usuario.
// Cada metodo devolve null se estiver OK, ou a mensagem de erro.
public class ValidadorUsuario
{
    // Formato de email valido: texto + @ + dominio + ponto + final
    private static final Pattern EMAIL =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");


    // Nome deve ter pelo menos 2 letras
    public static String validarNome(String nome)
    {
        if (nome == null || nome.trim().length() < 2) 
            return "Informe o nome completo.";
        return null;
    }

    // Email precisa seguir o formato definido
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
        if (!telefone.matches(".*\\d.*")) // Um digito
            return "Telefone invalido.";
        return null;
    }


    // Senha precisa ter no minimo 6 caracteres
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

    // Remove tudo que nao é numero
    public static String somenteDigitos(String s)
    {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    // Confere se o cpf é valido
    private static boolean cpfValido(String c)
    {
        if (c.length() != 11 || c.matches("(\\d)\\1{10}"))
            return false;
        return c.charAt(9)  - '0' == digito(c, 9)
            && c.charAt(10) - '0' == digito(c, 10);
    }

    // Calcula um digito verificador do CPF 
    private static int digito(String c, int tamanho)
    {
        int soma = 0;
        for (int i = 0; i < tamanho; i++)
            soma += (c.charAt(i) - '0') * (tamanho + 1 - i);
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    // Codigo precisa ser maior que 0
    public static String validarCodigo(int codigo)
    {
        if (codigo <= 0)
            return "Usuario nao identificado. Entre novamente.";
        return null;
    }
}
