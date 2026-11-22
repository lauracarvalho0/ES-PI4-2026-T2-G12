// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado; // classe-mae: faz da resposta uma mensagem do protocolo

// Servidor -> Cliente: resultado da conclusao de atividade.
public class RespostaDeConclusaoDeAtividade extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;

    private boolean sucesso;   // true = concluiu; false = deu algum problema
    private String  mensagem;  // texto pra mostrar ao usuario (sucesso ou motivo do erro)

    // a resposta ja nasce completa; so tem getters, entao nao muda depois de criada
    public RespostaDeConclusaoDeAtividade(boolean sucesso, String mensagem)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
    }

    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }

    // texto de depuracao: aparece quando a resposta e impressa (ex.: em logs)
    @Override
    public String toString() { return "RespostaDeConclusaoDeAtividade[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}