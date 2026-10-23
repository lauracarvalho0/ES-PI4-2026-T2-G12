package agromonitor.comum.conta;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: troca a senha (exige a senha atual).
public class PedidoDeAlteracaoDeSenha extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;
    private String senhaAtual;
    private String novaSenha;

    public PedidoDeAlteracaoDeSenha(int usuarioCodigo, String senhaAtual, String novaSenha)
    {
        this.usuarioCodigo = usuarioCodigo;
        this.senhaAtual = senhaAtual;
        this.novaSenha = novaSenha;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }
    public String getSenhaAtual() { return this.senhaAtual; }
    public String getNovaSenha() { return this.novaSenha; }

    @Override
    public String toString() { return "PedidoDeAlteracaoDeSenha[usuarioCodigo=" + usuarioCodigo + "]"; }
}
