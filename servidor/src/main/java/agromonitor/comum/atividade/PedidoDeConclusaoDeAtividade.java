// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado; // classe-mae: faz do pedido uma mensagem do protocolo

// Cliente -> Servidor: marca uma atividade que ja existe como concluida (data realizada = hoje).
public class PedidoDeConclusaoDeAtividade extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;

    // os 3 codigos identificam exatamente qual atividade concluir e de quem é
    private int usuarioCodigo;
    private int plantioCodigo;
    private int atividadeCodigo;

    // o pedido ja nasce completo; so tem getters, entao nao muda depois de criado
    public PedidoDeConclusaoDeAtividade(int usuarioCodigo, int plantioCodigo, int atividadeCodigo)
    {
        this.usuarioCodigo   = usuarioCodigo;
        this.plantioCodigo   = plantioCodigo;
        this.atividadeCodigo = atividadeCodigo;
    }

    public int getUsuarioCodigo()   { return this.usuarioCodigo; }
    public int getPlantioCodigo()   { return this.plantioCodigo; }
    public int getAtividadeCodigo() { return this.atividadeCodigo; }

    // texto de depuracao: aparece quando o pedido e impresso (ex.: em logs)
    @Override
    public String toString()
    {
        return "PedidoDeConclusaoDeAtividade[usuario=" + usuarioCodigo + ", plantio=" + plantioCodigo + ", atividade=" + atividadeCodigo + "]";
    }
}