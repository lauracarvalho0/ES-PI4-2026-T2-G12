// Giovana Uchelli - 25008818

package agromonitor.comum.auth;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: troca a senha usando o token recebido por e-mail.
public class PedidoDeRedefinicaoDeSenha extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private String token;
    private String novaSenha;

    public PedidoDeRedefinicaoDeSenha(String token, String novaSenha)
    {
        this.token     = token;
        this.novaSenha = novaSenha;
    }

    public String getToken()     { return this.token; }
    public String getNovaSenha() { return this.novaSenha; }

    @Override
    public String toString() { return "PedidoDeRedefinicaoDeSenha[...]"; } // nunca imprime token nem senha
}
