// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado; // classe-mae: faz do pedido uma mensagem do protocolo

// Cliente -> Servidor: registra uma atividade dentro de um plantio (tela "Registrar nova atividade").
public class PedidoDeRegistroDeAtividade extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;

    private int    usuarioCodigo;
    private int    plantioCodigo;
    private String tipoAtividade;  
    private String status;        
    private String dataExecucao;   // se conluida é a data realizada; senao é a data prevista
    private String quantidade;     
    private String responsavel;    
    private String observacoes;    

    // o pedido ja nasce completo; so tem getters, entao nao muda depois de criado
    public PedidoDeRegistroDeAtividade(int usuarioCodigo, int plantioCodigo, String tipoAtividade,
                                       String status, String dataExecucao, String quantidade,
                                       String responsavel, String observacoes)
    {
        this.usuarioCodigo = usuarioCodigo;
        this.plantioCodigo = plantioCodigo;
        this.tipoAtividade = tipoAtividade;
        this.status        = status;
        this.dataExecucao  = dataExecucao;
        this.quantidade    = quantidade;
        this.responsavel   = responsavel;
        this.observacoes   = observacoes;
    }

    public int    getUsuarioCodigo() { return this.usuarioCodigo; }
    public int    getPlantioCodigo() { return this.plantioCodigo; }
    public String getTipoAtividade() { return this.tipoAtividade; }
    public String getStatus()        { return this.status; }
    public String getDataExecucao()  { return this.dataExecucao; }
    public String getQuantidade()    { return this.quantidade; }
    public String getResponsavel()   { return this.responsavel; }
    public String getObservacoes()   { return this.observacoes; }

    // texto de depuracao: mostra so os campos principais (sem observacoes, que pode ser longa)
    @Override
    public String toString()
    {
        return "PedidoDeRegistroDeAtividade[usuario=" + usuarioCodigo + ", plantio=" + plantioCodigo + ", tipo=" + tipoAtividade + "]";
    }
}