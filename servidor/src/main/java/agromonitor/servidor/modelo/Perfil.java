// Giovana Uchelli - 25008818

package agromonitor.servidor.modelo;

// Dados da conta que a tela Configuracoes mostra e edita.
public class Perfil
{
    private String  nome;
    private String  email;
    private String  telefone;
    private boolean receberAlertasPorEmail;

    public Perfil(String nome, String email, String telefone,
                  boolean receberAlertasPorEmail)
    {
        this.nome                   = nome;
        this.email                  = email;
        this.telefone               = telefone;
        this.receberAlertasPorEmail = receberAlertasPorEmail;
    }

    public String  getNome()                  { return this.nome; }
    public String  getEmail()                 { return this.email; }
    public String  getTelefone()              { return this.telefone; }
    public boolean isReceberAlertasPorEmail() { return this.receberAlertasPorEmail; }
}
