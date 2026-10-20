package agromonitor.comum.auth;

import agromonitor.comum.Comunicado;

// Servidor -> Cliente: resultado da troca de senha.
public class RespostaDeRedefinicaoDeSenha extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean sucesso;
    private String  mensagem;

    public RespostaDeRedefinicaoDeSenha(boolean sucesso, String mensagem)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
    }

    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }

    @Override
    public String toString() { return "RespostaDeRedefinicaoDeSenha[sucesso=" + sucesso + "]"; }
}
