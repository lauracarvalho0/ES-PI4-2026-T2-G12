// Laura Carvalho - 25014543

package agromonitor.comum.plantio;

import agromonitor.comum.Comunicado; // classe-mae: faz da resposta uma mensagem do protocolo

// Servidor -> Cliente: resultado do cadastro de plantio.
public class RespostaDeCadastroDePlantio extends Comunicado
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // sucesso diz se o plantio foi cadastrado; mensagem explica o motivo quando falha.
    private boolean sucesso;
    private String  mensagem;
    private int     codigo; // codigo do plantio criado (0 se falhou)

    public RespostaDeCadastroDePlantio(boolean sucesso, String mensagem, int codigo)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        this.codigo   = codigo;
    }

    // So getters: a resposta nao muda depois de criada.
    public boolean isSucesso()   { return this.sucesso; }
    public String  getMensagem() { return this.mensagem; }
    public int     getCodigo()   { return this.codigo; }

    // Usado para exibir a resposta de forma legivel em logs (o codigo nao aparece aqui).
    @Override
    public String toString() { return "RespostaDeCadastroDePlantio[sucesso=" + sucesso + ", mensagem=" + mensagem + "]"; }
}