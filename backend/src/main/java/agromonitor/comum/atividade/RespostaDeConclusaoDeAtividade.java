// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado;

// Servidor -> Cliente: resultado da conclusao de atividade.
public class RespostaDeConclusaoDeAtividade extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean sucesso;
    private String  mensagem;

    public RespostaDeConclusaoDeAtividade(boolean sucesso, String mensagem)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
    }

    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }

    @Override
    public String toString() { return "RespostaDeConclusaoDeAtividade[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}
