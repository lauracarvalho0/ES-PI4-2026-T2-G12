// Laura Carvalho - 25014543

package agromonitor.comum.plantio;

import java.util.ArrayList;
import agromonitor.comum.Comunicado;

// Servidor -> Cliente: lista de plantios do usuario.
public class RespostaDeListagemDePlantios extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean                sucesso;
    private String                 mensagem;
    private ArrayList<PlantioResumo> plantios;

    public RespostaDeListagemDePlantios(boolean sucesso, String mensagem, ArrayList<PlantioResumo> plantios)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        this.plantios = plantios == null ? new ArrayList<PlantioResumo>() : plantios;
    }

    public boolean                isSucesso()   { return this.sucesso; }
    public String                 getMensagem() { return this.mensagem; }
    public ArrayList<PlantioResumo> getPlantios() { return this.plantios; }

    @Override
    public String toString() { return "RespostaDeListagemDePlantios[sucesso=" + sucesso + ", total=" + plantios.size() + "]"; }
}
