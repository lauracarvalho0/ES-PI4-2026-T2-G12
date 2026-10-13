package agromonitor.comum;

// Servidor -> Cliente: resultado do cadastro.
public class RespostaDeCadastro extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean sucesso;
    private String  mensagem;
    private int     codigo; // codigo do usuario criado (0 se falhou)

    public RespostaDeCadastro(boolean sucesso, String mensagem, int codigo)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        this.codigo   = codigo;
    }

    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }
    public int     getCodigo()   { return this.codigo; }

    @Override
    public String toString() { return "RespostaDeCadastro[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}
