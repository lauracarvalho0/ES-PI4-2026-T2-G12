// Giovana Uchelli - 25008818

package agromonitor.comum.conta;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: altera nome, e-mail e telefone.
public class PedidoDeAtualizacaoDePerfil extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;
    private String nome;
    private String email;
    private String telefone;

    public PedidoDeAtualizacaoDePerfil(int usuarioCodigo, String nome, String email, String telefone)
    {
        this.usuarioCodigo = usuarioCodigo;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }
    public String getNome() { return this.nome; }
    public String getEmail() { return this.email; }
    public String getTelefone() { return this.telefone; }

    @Override
    public String toString() { return "PedidoDeAtualizacaoDePerfil[usuarioCodigo=" + usuarioCodigo + "]"; }
}
