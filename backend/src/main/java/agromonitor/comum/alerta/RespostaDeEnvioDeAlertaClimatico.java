// Autoria: Beatriz Leme — RA 25015554.
package agromonitor.comum.alerta;

import agromonitor.comum.Comunicado;

public class RespostaDeEnvioDeAlertaClimatico extends Comunicado
{
    // Informa ao backend se o servidor aceitou registrar o alerta e a mensagem do resultado.
    private static final long serialVersionUID = 1L;
    private boolean sucesso;
    private String mensagem;

    public RespostaDeEnvioDeAlertaClimatico(boolean sucesso, String mensagem)
    {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
    }

    public boolean isSucesso() { return sucesso; }
    public String getMensagem() { return mensagem; }
}
