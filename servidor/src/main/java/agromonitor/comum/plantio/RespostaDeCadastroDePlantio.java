package agromonitor.comum.plantio;

import agromonitor.comum.Comunicado;

// Servidor -> Cliente: resultado do cadastro de plantio.
public class RespostaDeCadastroDePlantio extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean sucesso;
    private String  mensagem;
    private int     codigo; // codigo do plantio criado (0 se falhou)

    public RespostaDeCadastroDePlantio(boolean sucesso, String mensagem, int codigo)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        this.codigo   = codigo;
    }

    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }
    public int     getCodigo()   { return this.codigo; }

    @Override
    public String toString() { return "RespostaDeCadastroDePlantio[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}
