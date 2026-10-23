package agromonitor.comum.conta;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: exclui a conta (exclusao logica; exige a senha).
public class PedidoDeExclusaoDeConta extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;
    private String senha;

    public PedidoDeExclusaoDeConta(int usuarioCodigo, String senha)
    {
        this.usuarioCodigo = usuarioCodigo;
        this.senha = senha;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }
    public String getSenha() { return this.senha; }

    @Override
    public String toString() { return "PedidoDeExclusaoDeConta[usuarioCodigo=" + usuarioCodigo + "]"; }
}
