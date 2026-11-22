// Autoria: Beatriz Leme — RA 25015554.

package agromonitor.servidor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Regras dos alertas de atividade (so calculo, sem banco). Uma atividade NAO concluida gera:
//   data ja passou   -> "Atividade atrasada"      ALTA (CRITICO depois de 3 dias de atraso)
//   e hoje           -> "Atividade para hoje"     ALTA
//   em 1 ou 2 dias   -> "Atividade se aproxima"   MEDIA
// Cada atividade tem no maximo 2 alertas: um do motivo PROXIMA e um do motivo ATRASADA.
public class RegrasDeAlerta
{
    public static final String PROXIMA  = "PROXIMA";   // hoje, amanha ou depois de amanha
    public static final String ATRASADA = "ATRASADA";

    public static final int DIAS_AVISO   = 2;  // avisa com ate 2 dias de antecedencia
    public static final int DIAS_CRITICO = 3;  // atraso de 3 dias ou mais vira CRITICO

    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private static final List<String> RECOMENDACOES_HOJE = List.of(
        "Realize a atividade hoje, nos melhores horários do dia.",
        "Confira a previsão do tempo antes de ir a campo.",
        "Registre a atividade assim que concluir.");

    private static final List<String> RECOMENDACOES_PROXIMA = List.of(
        "Organize insumos, equipamentos e mão de obra antes da data prevista.",
        "Confira a previsão do tempo para o dia da atividade.",
        "Registre a atividade assim que concluir.");

    private static final List<String> RECOMENDACOES_ATRASADA = List.of(
        "Realize a atividade assim que possível ou ajuste a data prevista.",
        "Avalie se o atraso afeta as próximas atividades do plantio.",
        "Registre a atividade assim que concluir.");

    // O que o alerta deve ser (motivo, prioridade, titulo e recomendacoes fixas do tipo de alerta)
    public record Classificacao(String motivo, String prioridade, String titulo, List<String> recomendacoes) {}

    // Devolve null quando a atividade ainda esta longe (nao precisa de alerta).
    public static Classificacao classificar(LocalDate hoje, LocalDate dataPrevista)
    {
        long dias = ChronoUnit.DAYS.between(hoje, dataPrevista);

        if (dias < 0)
            return new Classificacao(ATRASADA, -dias >= DIAS_CRITICO ? "CRITICO" : "ALTA",
                                     "Atividade atrasada", RECOMENDACOES_ATRASADA);
        if (dias == 0)
            return new Classificacao(PROXIMA, "ALTA", "Atividade para hoje", RECOMENDACOES_HOJE);
        if (dias <= DIAS_AVISO)
            return new Classificacao(PROXIMA, "MEDIA", "Atividade se aproxima", RECOMENDACOES_PROXIMA);
        return null;
    }

    // Mesmo nome que a tela Atividades usa para cada tipo
    public static String nomeDoTipo(String tipo)
    {
        if (tipo == null)
            return "Atividade";
        switch (tipo)
        {
            case "PLANTIO":       return "Plantio";
            case "IRRIGACAO":     return "Irrigação";
            case "ADUBACAO":      return "Adubação de cobertura";
            case "TRATAMENTO":    return "Tratos culturais";
            case "MONITORAMENTO": return "Monitoramento";
            case "COLHEITA":      return "Colheita";
            default:              return tipo;
        }
    }

    public static String descricao(Classificacao c, String nomeDoTipo, String nomeDoPlantio, LocalDate dataPrevista)
    {
        String atividade = "A atividade \"" + nomeDoTipo + "\" do plantio " + nomeDoPlantio;
        String dia = DIA_MES.format(dataPrevista);

        if (c.motivo().equals(ATRASADA))
            return atividade + " estava prevista para " + dia + " e ainda não foi concluída.";
        if (c.titulo().equals("Atividade para hoje"))
            return atividade + " está prevista para hoje (" + dia + ").";
        return atividade + " está prevista para " + dia + ".";
    }

    // Texto do painel "Impacto nos plantios" (nao usa "hoje"/"amanha" para nao ficar velho)
    public static String rotuloDaData(LocalDate dataPrevista)
    {
        return "Prevista para " + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(dataPrevista);
    }
}
