package agromonitor.comum.alerta;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: pede os alertas de atividades de um usuario (tela Alertas).
public class PedidoDeListagemDeAlertas extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;

    public PedidoDeListagemDeAlertas(int usuarioCodigo)
    {
        this.usuarioCodigo = usuarioCodigo;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }

    @Override
    public String toString() { return "PedidoDeListagemDeAlertas[usuario=" + usuarioCodigo + "]"; }
}
