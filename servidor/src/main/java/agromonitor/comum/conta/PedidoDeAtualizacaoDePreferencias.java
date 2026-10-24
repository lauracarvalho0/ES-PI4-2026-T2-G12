package agromonitor.comum.conta;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: altera preferencias da conta, incluindo alertas por e-mail.
public class PedidoDeAtualizacaoDePreferencias extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;
    private String fusoHorario;
    private String idioma;
    private boolean receberAlertasPorEmail;

    public PedidoDeAtualizacaoDePreferencias(int usuarioCodigo, String fusoHorario, String idioma,
            boolean receberAlertasPorEmail)
    {
        this.usuarioCodigo = usuarioCodigo;
        this.fusoHorario = fusoHorario;
        this.idioma = idioma;
        this.receberAlertasPorEmail = receberAlertasPorEmail;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }
    public String getFusoHorario() { return this.fusoHorario; }
    public String getIdioma() { return this.idioma; }
    public boolean isReceberAlertasPorEmail() { return this.receberAlertasPorEmail; }

    @Override
    public String toString() { return "PedidoDeAtualizacaoDePreferencias[usuarioCodigo=" + usuarioCodigo + "]"; }
}
