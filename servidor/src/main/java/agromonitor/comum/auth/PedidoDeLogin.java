// Giovana Uchelli - 25008818

package agromonitor.comum.auth;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: pede para autenticar um usuario.
public class PedidoDeLogin extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private String email;
    private String senha;

    public PedidoDeLogin(String email, String senha)
    {
        this.email = email;
        this.senha = senha;
    }

    public String getEmail() { return this.email; }
    public String getSenha() { return this.senha; }

    @Override
    public String toString() { return "PedidoDeLogin[email=" + email + "]"; }
}
