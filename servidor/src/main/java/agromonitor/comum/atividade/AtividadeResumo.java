// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import java.io.Serializable; //permite transformar o objeto em bytes pra mandar pela rede (socket)

// Uma linha da tela Atividades.
public class AtividadeResumo implements Serializable
{
    // versao da classe; se o servidor e o cliente tiverem versoes diferentes, a leitura avisa em vez de dar lixo
    private static final long serialVersionUID = 1L;

    private int    codigo;
    private int    plantioCodigo;
    private String plantioNome;
    private String talhao;
    private String tipoAtividade;
    private String data;        // data realizada (se concluida) ou data prevista
    private String status;      
    private String quantidade;  
    private String responsavel;
    private String observacoes; 

    // recebe todos os campos de uma vez; o objeto nasce completo e so tem getters (nao muda depois)
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

    // getters: so leitura
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