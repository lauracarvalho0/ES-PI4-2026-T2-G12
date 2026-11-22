// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import java.util.ArrayList; // lista de AtividadeResumo que viaja na resposta
import agromonitor.comum.Comunicado; // classe-mae: faz da resposta uma mensagem do protocolo

// Servidor -> Cliente: atividades do usuario, da mais antiga para a mais recente.
public class RespostaDeListagemDeAtividades extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;

    private boolean                  sucesso;     // true = a busca deu certo
    private String                   mensagem;    // texto de erro quando sucesso = false
    private ArrayList<AtividadeResumo> atividades; // as linhas da tela Atividades

    // a resposta ja nasce completa; so tem getters, entao nao muda depois de criada
    public RespostaDeListagemDeAtividades(boolean sucesso, String mensagem, ArrayList<AtividadeResumo> atividades)
    {
        this.sucesso    = sucesso;
        this.mensagem   = mensagem;

        // se vier null (ex.: em caso de erro), usa lista vazia pra quem le nao tomar NullPointerException
        this.atividades = atividades == null ? new ArrayList<AtividadeResumo>() : atividades;
    }

    public boolean                  isSucesso()      { return this.sucesso; }
    public String                   getMensagem()    { return this.mensagem; }
    public ArrayList<AtividadeResumo> getAtividades() { return this.atividades; }

    // texto de depuracao: mostra so o total, nao a lista inteira
    @Override
    public String toString() { return "RespostaDeListagemDeAtividades[sucesso=" + sucesso + ", total=" + atividades.size() + "]"; }
}