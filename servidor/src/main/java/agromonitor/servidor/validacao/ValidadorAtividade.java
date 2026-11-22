// Laura Carvalho - 25014543

package agromonitor.servidor.validacao;

import java.time.LocalDate;  // LocalDate: data de hoje e comparação com a data de execução.

// Regras do dicionario de dados para a atividade.
// Cada metodo devolve null se estiver OK, ou a mensagem de erro.
// Todos sao static: da pra chamar sem criar objeto (ValidadorAtividade.validarTipo(...)).
public class ValidadorAtividade
{
    // os codigos tem que ser positivos (0 ou negativo = nao veio nada valido do cliente)
    public static String validarIds(int usuarioCodigo, int plantioCodigo)
    {
        if (usuarioCodigo <= 0)
            return "Usuario nao identificado. Entre novamente.";
        if (plantioCodigo <= 0)
            return "Selecione o plantio.";
        return null;
    }

    // so aceita os 6 tipos do dicionario de dados (o null e checado primeiro pra nao dar erro no equals)
    public static String validarTipo(String tipo)
    {
        if (tipo == null || !(tipo.equals("PLANTIO") || tipo.equals("IRRIGACAO") || tipo.equals("ADUBACAO")
                || tipo.equals("TRATAMENTO") || tipo.equals("MONITORAMENTO") || tipo.equals("COLHEITA")))
            return "Tipo de atividade invalido.";
        return null;
    }

    // so aceita os 3 status do dicionario de dados
    public static String validarStatus(String status)
    {
        if (status == null || !(status.equals("PENDENTE") || status.equals("CONCLUIDA") || status.equals("ATRASADA")))
            return "Status da atividade invalido.";
        return null;
    }

    // Alem de ser uma data real, ela precisa combinar com o status
    // (o dicionario diz que o status e calculado pela data):
    //   CONCLUIDA -> hoje ou antes   |   ATRASADA -> antes de hoje   |   PENDENTE -> hoje ou depois
    public static String validarData(String data, String status)
    {
        if (data == null || data.trim().isEmpty())
            return "Informe a data de execucao.";

        // lerData (do ValidadorPlantio) devolve null se o texto nao for uma data real (ex.: 31/02)
        LocalDate d = ValidadorPlantio.lerData(data);
        if (d == null)
            return "Data de execucao invalida.";

        LocalDate hoje = LocalDate.now();
        // "CONCLUIDA".equals(status) e a ordem segura: nao da erro se status for null
        if ("CONCLUIDA".equals(status) && d.isAfter(hoje))
            return "Uma atividade concluida nao pode ter data futura.";
        if ("ATRASADA".equals(status) && !d.isBefore(hoje)) // !isBefore = hoje ou depois
            return "Uma atividade atrasada precisa ter data anterior a hoje.";
        if ("PENDENTE".equals(status) && d.isBefore(hoje))
            return "Uma atividade pendente precisa ter data de hoje ou futura. Use Atrasada ou Concluida.";
        return null;
    }

    // campo opcional: so valida o tamanho se vier preenchido (null passa direto)
    public static String validarQuantidade(String quantidade)
    {
        if (quantidade != null && quantidade.trim().length() > 30)
            return "A quantidade pode ter no maximo 30 caracteres.";
        return null;
    }

    // campo opcional, maximo de 100 caracteres (sem contar espacos nas pontas)
    public static String validarResponsavel(String responsavel)
    {
        if (responsavel != null && responsavel.trim().length() > 100)
            return "O responsavel pode ter no maximo 100 caracteres.";
        return null;
    }

    // campo opcional, maximo de 200 caracteres (aqui nao usa trim, conta o texto como veio)
    public static String validarObservacoes(String observacoes)
    {
        if (observacoes != null && observacoes.length() > 200)
            return "As observacoes podem ter no maximo 200 caracteres.";
        return null;
    }
}