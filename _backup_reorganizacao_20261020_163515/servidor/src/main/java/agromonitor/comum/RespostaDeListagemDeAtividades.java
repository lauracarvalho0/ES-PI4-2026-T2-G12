package agromonitor.comum;

import java.util.ArrayList;

// Servidor -> Cliente: atividades do usuario, da mais antiga para a mais recente.
public class RespostaDeListagemDeAtividades extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean                  sucesso;
    private String                   mensagem;
    private ArrayList<AtividadeResumo> atividades;

    public RespostaDeListagemDeAtividades(boolean sucesso, String mensagem, ArrayList<AtividadeResumo> atividades)
    {
        this.sucesso    = sucesso;
        this.mensagem   = mensagem;
        this.atividades = atividades == null ? new ArrayList<AtividadeResumo>() : atividades;
    }

    public boolean                  isSucesso()      { return this.sucesso; }
    public String                   getMensagem()    { return this.mensagem; }
    public ArrayList<AtividadeResumo> getAtividades() { return this.atividades; }

    @Override
    public String toString() { return "RespostaDeListagemDeAtividades[sucesso=" + sucesso + ", total=" + atividades.size() + "]"; }
}
