// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado; // classe-mae: faz do pedido uma mensagem do protocolo

// Cliente -> Servidor: registra uma atividade dentro de um plantio (tela "Registrar nova atividade").
// A data vai como texto "yyyy-MM-dd"; o servidor valida tudo.
public class PedidoDeRegistroDeAtividade extends Comunicado
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // Os dois codigos dizem de quem e o plantio e em qual plantio a atividade sera registrada.
    private int    usuarioCodigo;
    private int    plantioCodigo;
    private String tipoAtividade;  // PLANTIO, IRRIGACAO, ADUBACAO, TRATAMENTO, MONITORAMENTO, COLHEITA
    private String status;         // PENDENTE, CONCLUIDA ou ATRASADA
    private String dataExecucao;   // se CONCLUIDA e a data realizada; senao e a data prevista
    private String quantidade;     // opcional, texto livre (ex.: "22 mm")
    private String responsavel;    // opcional
    private String observacoes;    // opcional

    // Recebe todos os campos de uma vez; os opcionais podem vir null.
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

    // So getters: o pedido nao muda depois de criado.
    public int    getUsuarioCodigo() { return this.usuarioCodigo; }
    public int    getPlantioCodigo() { return this.plantioCodigo; }
    public String getTipoAtividade() { return this.tipoAtividade; }
    public String getStatus()        { return this.status; }
    public String getDataExecucao()  { return this.dataExecucao; }
    public String getQuantidade()    { return this.quantidade; }
    public String getResponsavel()   { return this.responsavel; }
    public String getObservacoes()   { return this.observacoes; }

    // Usado para exibir o pedido de forma legivel em logs (so os campos principais).
    @Override
    public String toString()
    {
        return "PedidoDeRegistroDeAtividade[usuario=" + usuarioCodigo + ", plantio=" + plantioCodigo + ", tipo=" + tipoAtividade + "]";
    }
}