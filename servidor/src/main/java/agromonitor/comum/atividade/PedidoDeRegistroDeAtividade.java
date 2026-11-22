// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: registra uma atividade dentro de um plantio (tela "Registrar nova atividade").
// A data vai como texto "yyyy-MM-dd"; o servidor valida tudo.
// extends Comunicado: e assim que o servidor reconhece que isso e uma mensagem do protocolo
public class PedidoDeRegistroDeAtividade extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;

    private int    usuarioCodigo;
    private int    plantioCodigo;
    private String tipoAtividade;  // PLANTIO, IRRIGACAO, ADUBACAO, TRATAMENTO, MONITORAMENTO, COLHEITA
    private String status;         // PENDENTE, CONCLUIDA ou ATRASADA
    private String dataExecucao;   // se CONCLUIDA e a data realizada; senao e a data prevista
    private String quantidade;     // opcional, texto livre (ex.: "22 mm")
    private String responsavel;    // opcional
    private String observacoes;    // opcional

    // o pedido ja nasce completo; so tem getters, entao nao muda depois de criado
    // (aqui nao valida nada: quem valida e o ValidadorAtividade, no servidor)
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