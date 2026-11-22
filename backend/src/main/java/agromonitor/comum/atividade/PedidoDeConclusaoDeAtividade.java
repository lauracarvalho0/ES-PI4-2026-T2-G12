// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: marca uma atividade que ja existe como concluida (data realizada = hoje).
public class PedidoDeConclusaoDeAtividade extends Comunicado
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // Os tres codigos identificam a atividade: de quem e, em qual plantio e qual delas.
    private int usuarioCodigo;
    private int plantioCodigo;
    private int atividadeCodigo;

    public PedidoDeConclusaoDeAtividade(int usuarioCodigo, int plantioCodigo, int atividadeCodigo)
    {
        this.usuarioCodigo   = usuarioCodigo;
        this.plantioCodigo   = plantioCodigo;
        this.atividadeCodigo = atividadeCodigo;
    }

    // So getters: o pedido nao muda depois de criado.
    public int getUsuarioCodigo()   { return this.usuarioCodigo; }
    public int getPlantioCodigo()   { return this.plantioCodigo; }
    public int getAtividadeCodigo() { return this.atividadeCodigo; }

    // Usado para exibir o pedido de forma legivel em logs.
    @Override
    public String toString()
    {
        return "PedidoDeConclusaoDeAtividade[usuario=" + usuarioCodigo + ", plantio=" + plantioCodigo + ", atividade=" + atividadeCodigo + "]";
    }
}