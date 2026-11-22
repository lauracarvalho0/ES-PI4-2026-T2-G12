// Giovana Uchelli - 25008818

package agromonitor.comum.auth;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: pede para criar uma conta de usuario.
public class PedidoDeCadastro extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private String nome;
    private String email;
    private String cpf;       // opcional (documento do tipo CPF)
    private String telefone;  // opcional
    private String senha;     // texto puro so ate o servidor; la vira hash

    public PedidoDeCadastro(String nome, String email, String cpf, String telefone, String senha)
    {
        this.nome     = nome;
        this.email    = email;
        this.cpf      = cpf;
        this.telefone = telefone;
        this.senha    = senha;
    }

    public String getNome()     { return this.nome; }
    public String getEmail()    { return this.email; }
    public String getCpf()      { return this.cpf; }
    public String getTelefone() { return this.telefone; }
    public String getSenha()    { return this.senha; }

    @Override
    public String toString()
    {
        // nunca imprime a senha
        return "PedidoDeCadastro[nome=" + nome + ", email=" + email + "]";
    }
}
