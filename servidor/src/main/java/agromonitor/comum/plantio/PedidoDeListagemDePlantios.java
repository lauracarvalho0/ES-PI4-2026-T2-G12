// Laura Carvalho - 25014543

package agromonitor.comum.plantio;

import agromonitor.comum.Comunicado; // classe-mae: faz do pedido uma mensagem do protocolo

// extends Comunicado: e assim que o servidor reconhece que isso e uma mensagem do protocolo
public class PedidoDeListagemDePlantios extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;

    // so precisa do usuario: o servidor busca todos os plantios ativos dele
    private int usuarioCodigo;

    // o pedido ja nasce completo; so tem getter, entao nao muda depois de criado
    public PedidoDeListagemDePlantios(int usuarioCodigo)
    {
        this.usuarioCodigo = usuarioCodigo;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }

    // texto de depuracao: aparece quando o pedido e impresso (ex.: em logs)
    @Override
    public String toString() { return "PedidoDeListagemDePlantios[usuario=" + usuarioCodigo + "]"; }
}