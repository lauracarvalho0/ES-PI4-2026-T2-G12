// Laura Carvalho - 25014543

package agromonitor.comum.plantio;

import agromonitor.comum.Comunicado; // classe-mae: faz do pedido uma mensagem do protocolo

// extends Comunicado: e assim que o cliente reconhece que isso e uma mensagem do protocolo
public class RespostaDeCadastroDePlantio extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;

    private boolean sucesso;   // true = cadastrou; false = deu algum problema
    private String  mensagem; 
    private int     codigo;    // codigo do plantio criado (0 se falhou)

    // a resposta ja nasce completa; so tem getters, entao nao muda depois de criada
    public RespostaDeCadastroDePlantio(boolean sucesso, String mensagem, int codigo)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        this.codigo   = codigo;
    }

    // para boolean, o padrao do getter e "is" em vez de "get"
    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }
    public int     getCodigo()   { return this.codigo; }

    // texto de depuracao: mostra so sucesso e mensagem (o codigo fica de fora)
    @Override
    public String toString() { return "RespostaDeCadastroDePlantio[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}