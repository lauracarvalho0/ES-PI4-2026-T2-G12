// Laura Carvalho - 25014543

package agromonitor.comum.plantio;

import java.util.ArrayList;
import agromonitor.comum.Comunicado;

// Servidor -> Cliente: lista de plantios do usuario.
public class RespostaDeListagemDePlantios extends Comunicado
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // sucesso diz se a listagem funcionou; mensagem explica o motivo quando falha.
    private boolean                sucesso;
    private String                 mensagem;
    private ArrayList<PlantioResumo> plantios;

    public RespostaDeListagemDePlantios(boolean sucesso, String mensagem, ArrayList<PlantioResumo> plantios)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        // Nunca guarda null: sem plantios, usa uma lista vazia.
        this.plantios = plantios == null ? new ArrayList<PlantioResumo>() : plantios;
    }

    // So getters: a resposta nao muda depois de criada.
    public boolean                isSucesso()   { return this.sucesso; }
    public String                 getMensagem() { return this.mensagem; }
    public ArrayList<PlantioResumo> getPlantios() { return this.plantios; }

    // Usado para exibir a resposta de forma legivel em logs (mostra so o total de plantios).
    @Override
    public String toString() { return "RespostaDeListagemDePlantios[sucesso=" + sucesso + ", total=" + plantios.size() + "]"; }
}