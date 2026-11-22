// Giovana Uchelli - 25008818

package agromonitor.comum.auth;

import agromonitor.comum.Comunicado;

// Servidor -> Cliente: resultado do login.
public class RespostaDeLogin extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean sucesso;
    private String  mensagem;
    private int     codigo;  // dados do usuario (so valem se sucesso)
    private String  nome;
    private String  email;

    public RespostaDeLogin(boolean sucesso, String mensagem, int codigo, String nome, String email)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        this.codigo   = codigo;
        this.nome     = nome;
        this.email    = email;
    }

    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }
    public int     getCodigo()   { return this.codigo; }
    public String  getNome()     { return this.nome; }
    public String  getEmail()    { return this.email; }

    @Override
    public String toString() { return "RespostaDeLogin[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}
