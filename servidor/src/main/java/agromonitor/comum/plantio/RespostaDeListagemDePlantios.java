// Laura Carvalho - 25014543

package agromonitor.comum.plantio;

import java.util.ArrayList; // lista de PlantioResumo que viaja na resposta
import agromonitor.comum.Comunicado; // classe-mae: faz da resposta uma mensagem do protocolo

// extends Comunicado: e assim que o cliente reconhece que isso e uma mensagem do protocolo
public class RespostaDeListagemDePlantios extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;

    private boolean                sucesso;   // true = a busca deu certo
    private String                 mensagem;  // texto de erro quando sucesso = false
    private ArrayList<PlantioResumo> plantios; // as linhas da tela Meus Plantios

    // a resposta ja nasce completa; so tem getters, entao nao muda depois de criada
    public RespostaDeListagemDePlantios(boolean sucesso, String mensagem, ArrayList<PlantioResumo> plantios)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;

        // se vier null usa lista vazia pra quem le nao tomar NullPointerException
        this.plantios = plantios == null ? new ArrayList<PlantioResumo>() : plantios;
    }

    // para boolean, o padrao do getter e "is" em vez de "get"
    public boolean                isSucesso()   { return this.sucesso; }
    public String                 getMensagem() { return this.mensagem; }
    public ArrayList<PlantioResumo> getPlantios() { return this.plantios; }

    // texto de depuracao: mostra so o total, nao a lista inteira
    @Override
    public String toString() { return "RespostaDeListagemDePlantios[sucesso=" + sucesso + ", total=" + plantios.size() + "]"; }
}