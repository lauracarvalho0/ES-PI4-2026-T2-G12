// Autoria: Beatriz Leme — RA 25015554.

package agromonitor.comum.alerta;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: botao "Marcar como lido" do detalhe do alerta.
public class PedidoDeMarcacaoDeAlertaLido extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;
    private int alertaCodigo;

    public PedidoDeMarcacaoDeAlertaLido(int usuarioCodigo, int alertaCodigo)
    {
        this.usuarioCodigo = usuarioCodigo;
        this.alertaCodigo  = alertaCodigo;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }
    public int getAlertaCodigo()  { return this.alertaCodigo; }

    @Override
    public String toString() { return "PedidoDeMarcacaoDeAlertaLido[usuario=" + usuarioCodigo + ", alerta=" + alertaCodigo + "]"; }
}
