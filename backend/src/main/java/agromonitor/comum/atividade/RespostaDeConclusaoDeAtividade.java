// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado; // classe-mae: faz da resposta uma mensagem do protocolo

// Servidor -> Cliente: resultado da conclusao de atividade.
public class RespostaDeConclusaoDeAtividade extends Comunicado
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // sucesso diz se a atividade foi concluida; mensagem explica o motivo (ex.: atividade inexistente).
    private boolean sucesso;
    private String  mensagem;

    public RespostaDeConclusaoDeAtividade(boolean sucesso, String mensagem)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
    }

    // So getters: a resposta nao muda depois de criada.
    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }

    // Usado para exibir a resposta de forma legivel em logs.
    @Override
    public String toString() { return "RespostaDeConclusaoDeAtividade[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}