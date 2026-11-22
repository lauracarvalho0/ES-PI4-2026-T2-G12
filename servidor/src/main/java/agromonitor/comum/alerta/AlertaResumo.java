// Autoria: Beatriz Leme — RA 25015554.

package agromonitor.comum.alerta;

import java.io.Serializable;
import java.util.ArrayList;

// Um alerta da tela Alertas. Nao e um Comunicado: viaja dentro da RespostaDeListagemDeAlertas.
// Datas como texto: dataAlerta = "yyyy-MM-dd HH:mm", dataAtividade = "yyyy-MM-dd".
public class AlertaResumo implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int               codigo;
    private int               plantioCodigo;
    private String            plantioNome;
    private String            cultura;
    private String            talhao;
    private String            tipoAlerta;        // ATIVIDADE (CLIMATICO ainda e gerado no front)
    private String            titulo;
    private String            descricao;
    private String            prioridade;        // CRITICO, ALTA, MEDIA
    private String            situacao;          // LIDO, NAO_LIDO
    private String            dataAlerta;
    private String            origem;            // SISTEMA_MONITORAMENTO
    private String            impactoAtividade;
    private String            impactoDataLabel;
    private String            dataAtividade;     // data prevista da atividade que originou o alerta
    private ArrayList<String> recomendacoes;

    public AlertaResumo(int codigo, int plantioCodigo, String plantioNome, String cultura, String talhao,
                        String tipoAlerta, String titulo, String descricao, String prioridade, String situacao,
                        String dataAlerta, String origem, String impactoAtividade, String impactoDataLabel,
                        String dataAtividade, ArrayList<String> recomendacoes)
    {
        this.codigo           = codigo;
        this.plantioCodigo    = plantioCodigo;
        this.plantioNome      = plantioNome;
        this.cultura          = cultura;
        this.talhao           = talhao;
        this.tipoAlerta       = tipoAlerta;
        this.titulo           = titulo;
        this.descricao        = descricao;
        this.prioridade       = prioridade;
        this.situacao         = situacao;
        this.dataAlerta       = dataAlerta;
        this.origem           = origem;
        this.impactoAtividade = impactoAtividade;
        this.impactoDataLabel = impactoDataLabel;
        this.dataAtividade    = dataAtividade;
        this.recomendacoes    = recomendacoes == null ? new ArrayList<String>() : recomendacoes;
    }

    public int               getCodigo()           { return this.codigo; }
    public int               getPlantioCodigo()    { return this.plantioCodigo; }
    public String            getPlantioNome()      { return this.plantioNome; }
    public String            getCultura()          { return this.cultura; }
    public String            getTalhao()           { return this.talhao; }
    public String            getTipoAlerta()       { return this.tipoAlerta; }
    public String            getTitulo()           { return this.titulo; }
    public String            getDescricao()        { return this.descricao; }
    public String            getPrioridade()       { return this.prioridade; }
    public String            getSituacao()         { return this.situacao; }
    public String            getDataAlerta()       { return this.dataAlerta; }
    public String            getOrigem()           { return this.origem; }
    public String            getImpactoAtividade() { return this.impactoAtividade; }
    public String            getImpactoDataLabel() { return this.impactoDataLabel; }
    public String            getDataAtividade()    { return this.dataAtividade; }
    public ArrayList<String> getRecomendacoes()    { return this.recomendacoes; }
}
