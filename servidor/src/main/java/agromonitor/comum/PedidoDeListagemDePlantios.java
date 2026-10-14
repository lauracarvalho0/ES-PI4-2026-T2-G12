package agromonitor.comum;

// Cliente -> Servidor: pede a lista de plantios de um usuario (tela Meus Plantios).
public class PedidoDeListagemDePlantios extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;

    public PedidoDeListagemDePlantios(int usuarioCodigo)
    {
        this.usuarioCodigo = usuarioCodigo;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }

    @Override
    public String toString() { return "PedidoDeListagemDePlantios[usuario=" + usuarioCodigo + "]"; }
}
