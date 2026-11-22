// Giovana Uchelli - 25008818

package agromonitor.comum.conta;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: altera a preferencia de alertas por e-mail da conta.
public class PedidoDeAtualizacaoDePreferencias extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;
    private boolean receberAlertasPorEmail;

    public PedidoDeAtualizacaoDePreferencias(int usuarioCodigo, boolean receberAlertasPorEmail)
    {
        this.usuarioCodigo = usuarioCodigo;
        this.receberAlertasPorEmail = receberAlertasPorEmail;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }
    public boolean isReceberAlertasPorEmail() { return this.receberAlertasPorEmail; }

    @Override
    public String toString() { return "PedidoDeAtualizacaoDePreferencias[usuarioCodigo=" + usuarioCodigo + "]"; }
}
