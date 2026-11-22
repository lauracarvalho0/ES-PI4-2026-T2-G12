// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: registra uma atividade dentro de um plantio (tela "Registrar nova atividade").
// A data vai como texto "yyyy-MM-dd"; o servidor valida tudo.
public class PedidoDeRegistroDeAtividade extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int    usuarioCodigo;
    private int    plantioCodigo;
    private String tipoAtividade;  // PLANTIO, IRRIGACAO, ADUBACAO, TRATAMENTO, MONITORAMENTO, COLHEITA
    private String status;         // PENDENTE, CONCLUIDA ou ATRASADA
    private String dataExecucao;   // se CONCLUIDA e a data realizada; senao e a data prevista
    private String quantidade;     // opcional, texto livre (ex.: "22 mm")
    private String responsavel;    // opcional
    private String observacoes;    // opcional

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

    @Override
    public String toString()
    {
        return "PedidoDeRegistroDeAtividade[usuario=" + usuarioCodigo + ", plantio=" + plantioCodigo + ", tipo=" + tipoAtividade + "]";
    }
}
