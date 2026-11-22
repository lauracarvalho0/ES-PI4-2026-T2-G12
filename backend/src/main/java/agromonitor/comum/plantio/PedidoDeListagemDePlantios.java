// Laura Carvalho - 25014543

package agromonitor.comum.plantio;

import agromonitor.comum.Comunicado; // classe-mae: faz do pedido uma mensagem do protocolo

// Cliente -> Servidor: pede a lista de plantios de um usuario (tela Meus Plantios).
public class PedidoDeListagemDePlantios extends Comunicado
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // So o usuario e necessario: o servidor busca todos os plantios dele.
    private int usuarioCodigo;

    public PedidoDeListagemDePlantios(int usuarioCodigo)
    {
        this.usuarioCodigo = usuarioCodigo;
    }

    // So getter: o pedido nao muda depois de criado.
    public int getUsuarioCodigo() { return this.usuarioCodigo; }

    // Usado para exibir o pedido de forma legivel em logs.
    @Override
    public String toString() { return "PedidoDeListagemDePlantios[usuario=" + usuarioCodigo + "]"; }
}