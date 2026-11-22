// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado;

// Servidor -> Cliente: resultado do registro de atividade.
public class RespostaDeRegistroDeAtividade extends Comunicado
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // sucesso diz se a atividade foi registrada; mensagem explica o motivo quando falha.
    private boolean sucesso;
    private String  mensagem;
    private int     codigo; // codigo da atividade criada (0 se falhou)

    public RespostaDeRegistroDeAtividade(boolean sucesso, String mensagem, int codigo)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        this.codigo   = codigo;
    }

    // So getters: a resposta nao muda depois de criada.
    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }
    public int     getCodigo()   { return this.codigo; }

    // Usado para exibir a resposta de forma legivel em logs (o codigo nao aparece aqui).
    @Override
    public String toString() { return "RespostaDeRegistroDeAtividade[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}