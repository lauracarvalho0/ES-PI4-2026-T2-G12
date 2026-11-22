// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado; // classe-mae: faz da resposta uma mensagem do protocolo

// Servidor -> Cliente: resultado do registro de atividade.
public class RespostaDeRegistroDeAtividade extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;

    private boolean sucesso;   
    private String  mensagem;  
    private int     codigo; // codigo da atividade criada (0 se falhou)

    // a resposta ja nasce completa; so tem getters, entao nao muda depois de criada
    public RespostaDeRegistroDeAtividade(boolean sucesso, String mensagem, int codigo)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        this.codigo   = codigo;
    }

    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }
    public int     getCodigo()   { return this.codigo; }

    // texto de depuracao: mostra so sucesso e mensagem (o codigo fica de fora)
    @Override
    public String toString() { return "RespostaDeRegistroDeAtividade[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}