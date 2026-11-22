// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: marca uma atividade que ja existe como concluida (data realizada = hoje).
public class PedidoDeConclusaoDeAtividade extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;
    private int plantioCodigo;
    private int atividadeCodigo;

    public PedidoDeConclusaoDeAtividade(int usuarioCodigo, int plantioCodigo, int atividadeCodigo)
    {
        this.usuarioCodigo   = usuarioCodigo;
        this.plantioCodigo   = plantioCodigo;
        this.atividadeCodigo = atividadeCodigo;
    }

    public int getUsuarioCodigo()   { return this.usuarioCodigo; }
    public int getPlantioCodigo()   { return this.plantioCodigo; }
    public int getAtividadeCodigo() { return this.atividadeCodigo; }

    @Override
    public String toString()
    {
        return "PedidoDeConclusaoDeAtividade[usuario=" + usuarioCodigo + ", plantio=" + plantioCodigo + ", atividade=" + atividadeCodigo + "]";
    }
}
