package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado;

// Servidor -> Cliente: resultado do registro de atividade.
public class RespostaDeRegistroDeAtividade extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean sucesso;
    private String  mensagem;
    private int     codigo; // codigo da atividade criada (0 se falhou)

    public RespostaDeRegistroDeAtividade(boolean sucesso, String mensagem, int codigo)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        this.codigo   = codigo;
    }

    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }
    public int     getCodigo()   { return this.codigo; }

    @Override
    public String toString() { return "RespostaDeRegistroDeAtividade[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}
