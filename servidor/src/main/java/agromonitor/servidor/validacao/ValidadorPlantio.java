// Laura Carvalho - 25014543

package agromonitor.servidor.validacao;

import java.time.LocalDate;
import java.util.regex.Pattern;

// Regras do dicionario de dados para o plantio.
// Cada metodo devolve null se estiver OK, ou a mensagem de erro.
public class ValidadorPlantio
{
    private static final Pattern SAFRA = Pattern.compile("^\\d{2}/\\d{2}$");

    public static String validarUsuario(int codigo)
    {
        if (codigo <= 0)
            return "Usuario nao identificado. Entre novamente.";
        return null;
    }

    public static String validarCultura(String cultura)
    {
        if (cultura == null || cultura.trim().isEmpty())
            return "Selecione a cultura.";
        return null;
    }

    public static String validarNome(String nome)
    {
        if (nome == null || nome.trim().isEmpty())
            return "Informe o nome do plantio.";
        if (nome.trim().length() > 100)
            return "O nome do plantio pode ter no maximo 100 caracteres.";
        return null;
    }

    public static String validarSafra(String safra)
    {
        if (safra == null || !SAFRA.matcher(safra.trim()).matches())
            return "Informe a safra no formato 25/26.";
        return null;
    }

    // descricao e opcional, mas a tela limita a 200 caracteres
    public static String validarDescricao(String descricao)
    {
        if (descricao != null && descricao.length() > 200)
            return "A descricao pode ter no maximo 200 caracteres.";
        return null;
    }

    public static String validarSistemaCultivo(String sistema)
    {
        if (sistema == null || !(sistema.equals("SEQUEIRO") || sistema.equals("IRRIGADO")))
            return "Sistema de cultivo invalido.";
        return null;
    }

    public static String validarTalhao(String talhao)
    {
        if (talhao == null || talhao.trim().isEmpty())
            return "Selecione o talhao.";
        return null;
    }

    public static String validarAreaPlantada(Double area)
    {
        if (area == null || area.isNaN() || area.isInfinite() || area <= 0)
            return "Informe uma area plantada maior que zero.";
        return null;
    }

    // altitude e opcional; se vier, nao pode ser negativa
    public static String validarAltitude(Integer altitude)
    {
        if (altitude != null && altitude < 0)
            return "A altitude nao pode ser negativa.";
        return null;
    }

    // metodo e opcional; se vier, precisa estar na lista
    public static String validarMetodoPlantio(String metodo)
    {
        if (metodo == null || metodo.trim().isEmpty())
            return null;
        if (!(metodo.equals("PLANTIO_DIRETO") || metodo.equals("CONVENCIONAL") || metodo.equals("MINIMO")))
            return "Metodo de plantio invalido.";
        return null;
    }

    public static String validarDataPlantio(String data)
    {
        if (data == null || data.trim().isEmpty())
            return "Informe a data de plantio.";
        if (lerData(data) == null)
            return "Data de plantio invalida.";
        return null;
    }

    // ajuste de data previsto e opcional; se vier, precisa ser uma data valida
    public static String validarAjuste(String data, String nomeAtividade)
    {
        if (data == null || data.trim().isEmpty())
            return null;
        if (lerData(data) == null)
            return "Data ajustada de " + nomeAtividade + " invalida.";
        return null;
    }

    // Devolve a data ou null se o texto nao for uma data real (ex.: 2026-02-31)
    public static LocalDate lerData(String texto)
    {
        try
        {
            return LocalDate.parse(texto.trim());
        }
        catch (Exception erro)
        {
            return null;
        }
    }
}
