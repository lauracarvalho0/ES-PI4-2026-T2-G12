package agromonitor.comum;

// Servidor -> Cliente. A mensagem e sempre a mesma exista ou nao o e-mail,
// para nao revelar quais e-mails estao cadastrados.
public class RespostaDeRecuperacaoDeSenha extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean sucesso;
    private String  mensagem;

    public RespostaDeRecuperacaoDeSenha(boolean sucesso, String mensagem)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
    }

    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }

    @Override
    public String toString() { return "RespostaDeRecuperacaoDeSenha[sucesso=" + sucesso + "]"; }
}
