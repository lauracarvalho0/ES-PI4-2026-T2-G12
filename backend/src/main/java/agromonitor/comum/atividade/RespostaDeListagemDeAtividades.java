// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import java.util.ArrayList; // lista de AtividadeResumo que viaja na resposta
import agromonitor.comum.Comunicado; // classe-mae: faz da resposta uma mensagem do protocolo

// Servidor -> Cliente: atividades do usuario, da mais antiga para a mais recente.
public class RespostaDeListagemDeAtividades extends Comunicado
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // sucesso diz se a listagem funcionou; mensagem explica o motivo quando falha.
    private boolean                  sucesso;
    private String                   mensagem;
    private ArrayList<AtividadeResumo> atividades;

    public RespostaDeListagemDeAtividades(boolean sucesso, String mensagem, ArrayList<AtividadeResumo> atividades)
    {
        this.sucesso    = sucesso;
        this.mensagem   = mensagem;
        // Nunca guarda null: sem atividades, usa uma lista vazia.
        this.atividades = atividades == null ? new ArrayList<AtividadeResumo>() : atividades;
    }

    // So getters: a resposta nao muda depois de criada.
    public boolean                  isSucesso()      { return this.sucesso; }
    public String                   getMensagem()    { return this.mensagem; }
    public ArrayList<AtividadeResumo> getAtividades() { return this.atividades; }

    // Usado para exibir a resposta de forma legivel em logs (mostra so o total de atividades).
    @Override
    public String toString() { return "RespostaDeListagemDeAtividades[sucesso=" + sucesso + ", total=" + atividades.size() + "]"; }
}