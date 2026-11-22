// Autoria: Beatriz Leme — RA 25015554.

package agromonitor.comum.alerta;

import agromonitor.comum.Comunicado;

// Servidor -> Cliente: resultado da marcacao como lido.
public class RespostaDeMarcacaoDeAlertaLido extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean sucesso;
    private String  mensagem;

    public RespostaDeMarcacaoDeAlertaLido(boolean sucesso, String mensagem)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
    }

    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }

    @Override
    public String toString() { return "RespostaDeMarcacaoDeAlertaLido[sucesso=" + sucesso + "]"; }
}
