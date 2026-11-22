// Giovana Uchelli - 25008818

package agromonitor.comum.auth;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: "esqueci minha senha". O servidor gera o link e envia por e-mail.
// urlBase = endereco do site (ex.: http://localhost:8080); o link do e-mail volta para la.
public class PedidoDeRecuperacaoDeSenha extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private String email;
    private String urlBase;

    public PedidoDeRecuperacaoDeSenha(String email, String urlBase)
    {
        this.email   = email;
        this.urlBase = urlBase;
    }

    public String getEmail()   { return this.email; }
    public String getUrlBase() { return this.urlBase; }

    @Override
    public String toString() { return "PedidoDeRecuperacaoDeSenha[email=" + email + "]"; }
}
