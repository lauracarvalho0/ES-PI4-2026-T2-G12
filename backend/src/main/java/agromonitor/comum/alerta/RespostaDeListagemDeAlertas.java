package agromonitor.comum.alerta;

import java.util.ArrayList;

import agromonitor.comum.Comunicado;

// Servidor -> Cliente: lista de alertas do usuario.
public class RespostaDeListagemDeAlertas extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private boolean                sucesso;
    private String                 mensagem;
    private ArrayList<AlertaResumo> alertas;

    public RespostaDeListagemDeAlertas(boolean sucesso, String mensagem, ArrayList<AlertaResumo> alertas)
    {
        this.sucesso  = sucesso;
        this.mensagem = mensagem;
        this.alertas  = alertas == null ? new ArrayList<AlertaResumo>() : alertas;
    }

    public boolean                 isSucesso()   { return this.sucesso; }
    public String                  getMensagem() { return this.mensagem; }
    public ArrayList<AlertaResumo> getAlertas()  { return this.alertas; }

    @Override
    public String toString() { return "RespostaDeListagemDeAlertas[sucesso=" + sucesso + ", total=" + alertas.size() + "]"; }
}
