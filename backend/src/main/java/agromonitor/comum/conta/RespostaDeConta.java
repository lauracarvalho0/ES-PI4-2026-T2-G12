package agromonitor.comum.conta;

import agromonitor.comum.Comunicado;

// Servidor -> Cliente: resultado de alterar perfil, preferencias, senha ou excluir a conta.
public class RespostaDeConta extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean sucesso;
    private String mensagem;

    public RespostaDeConta(boolean sucesso, String mensagem)
    {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
    }

    public boolean isSucesso() { return this.sucesso; }
    public String getMensagem() { return this.mensagem; }

    @Override
    public String toString() { return "RespostaDeConta[sucesso=" + sucesso + "]"; }
}
