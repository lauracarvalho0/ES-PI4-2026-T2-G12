package agromonitor.comum.conta;

import agromonitor.comum.Comunicado;

// Servidor -> Cliente: dados da conta (so valem se sucesso).
public class RespostaDeBuscaDePerfil extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean sucesso;
    private String mensagem;
    private String nome;
    private String email;
    private String telefone;
    private boolean receberAlertasPorEmail;

    public RespostaDeBuscaDePerfil(boolean sucesso, String mensagem, String nome, String email, String telefone,
            boolean receberAlertasPorEmail)
    {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.receberAlertasPorEmail = receberAlertasPorEmail;
    }

    public boolean isSucesso() { return this.sucesso; }
    public String getMensagem() { return this.mensagem; }
    public String getNome() { return this.nome; }
    public String getEmail() { return this.email; }
    public String getTelefone() { return this.telefone; }
    public boolean isReceberAlertasPorEmail() { return this.receberAlertasPorEmail; }

    @Override
    public String toString() { return "RespostaDeBuscaDePerfil[sucesso=" + sucesso + "]"; }
}
