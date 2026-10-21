package agromonitor.comum.atividade;

import java.io.Serializable;

// Uma linha da tela Atividades. Nao e um Comunicado: viaja dentro da
// RespostaDeListagemDeAtividades. A data vai como texto "yyyy-MM-dd".
public class AtividadeResumo implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int    codigo;
    private int    plantioCodigo;
    private String plantioNome;
    private String talhao;
    private String tipoAtividade;
    private String data;        // data realizada (se concluida) ou data prevista
    private String status;      // PENDENTE, CONCLUIDA ou ATRASADA (ja calculado pela data)
    private String quantidade;  // pode ser null
    private String responsavel; // pode ser null
    private String observacoes; // pode ser null

    public AtividadeResumo(int codigo, int plantioCodigo, String plantioNome, String talhao,
                           String tipoAtividade, String data, String status,
                           String quantidade, String responsavel, String observacoes)
    {
        this.codigo        = codigo;
        this.plantioCodigo = plantioCodigo;
        this.plantioNome   = plantioNome;
        this.talhao        = talhao;
        this.tipoAtividade = tipoAtividade;
        this.data          = data;
        this.status        = status;
        this.quantidade    = quantidade;
        this.responsavel   = responsavel;
        this.observacoes   = observacoes;
    }

    public int    getCodigo()        { return this.codigo; }
    public int    getPlantioCodigo() { return this.plantioCodigo; }
    public String getPlantioNome()   { return this.plantioNome; }
    public String getTalhao()        { return this.talhao; }
    public String getTipoAtividade() { return this.tipoAtividade; }
    public String getData()          { return this.data; }
    public String getStatus()        { return this.status; }
    public String getQuantidade()    { return this.quantidade; }
    public String getResponsavel()   { return this.responsavel; }
    public String getObservacoes()   { return this.observacoes; }
}
