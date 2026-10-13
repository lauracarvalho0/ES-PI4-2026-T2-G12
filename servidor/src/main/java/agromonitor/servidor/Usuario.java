package agromonitor.servidor;

// Dados do usuario que o servidor precisa para autenticar.
public class Usuario
{
    private int    codigo;
    private String nome;
    private String email;
    private String senhaHash;

    public Usuario(int codigo, String nome, String email, String senhaHash)
    {
        this.codigo    = codigo;
        this.nome      = nome;
        this.email     = email;
        this.senhaHash = senhaHash;
    }

    public int    getCodigo()    { return this.codigo; }
    public String getNome()      { return this.nome; }
    public String getEmail()     { return this.email; }
    public String getSenhaHash() { return this.senhaHash; }
}
