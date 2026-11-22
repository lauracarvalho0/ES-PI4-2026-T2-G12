// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: pede as atividades de todos os plantios de um usuario (tela Atividades).
public class PedidoDeListagemDeAtividades extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;

    public PedidoDeListagemDeAtividades(int usuarioCodigo)
    {
        this.usuarioCodigo = usuarioCodigo;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }

    @Override
    public String toString() { return "PedidoDeListagemDeAtividades[usuario=" + usuarioCodigo + "]"; }
}
