package agromonitor.comum.alerta;

import agromonitor.comum.Comunicado;

public class PedidoDeEnvioDeAlertaClimatico extends Comunicado
{
    private static final long serialVersionUID = 1L;
    private int usuarioCodigo;
    private int plantioCodigo;
    private String chave;
    private String titulo;
    private String descricao;
    private String prioridade;
    private String data;

    public PedidoDeEnvioDeAlertaClimatico(int usuarioCodigo, int plantioCodigo, String chave,
            String titulo, String descricao, String prioridade, String data)
    {
        this.usuarioCodigo = usuarioCodigo;
        this.plantioCodigo = plantioCodigo;
        this.chave = chave;
        this.titulo = titulo;
        this.descricao = descricao;
        this.prioridade = prioridade;
        this.data = data;
    }

    public int getUsuarioCodigo() { return usuarioCodigo; }
    public int getPlantioCodigo() { return plantioCodigo; }
    public String getChave() { return chave; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getPrioridade() { return prioridade; }
    public String getData() { return data; }
}
