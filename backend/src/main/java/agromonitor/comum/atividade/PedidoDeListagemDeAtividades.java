// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado; // classe-mae: faz do pedido uma mensagem do protocolo

// Cliente -> Servidor: pede as atividades de todos os plantios de um usuario (tela Atividades).
public class PedidoDeListagemDeAtividades extends Comunicado
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // So o usuario e necessario: o servidor busca os plantios e as atividades dele.
    private int usuarioCodigo;

    public PedidoDeListagemDeAtividades(int usuarioCodigo)
    {
        this.usuarioCodigo = usuarioCodigo;
    }

    // So getter: o pedido nao muda depois de criado.
    public int getUsuarioCodigo() { return this.usuarioCodigo; }

    // Usado para exibir o pedido de forma legivel em logs.
    @Override
    public String toString() { return "PedidoDeListagemDeAtividades[usuario=" + usuarioCodigo + "]"; }
}