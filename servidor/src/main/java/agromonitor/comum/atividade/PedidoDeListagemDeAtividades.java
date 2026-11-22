// Laura Carvalho - 25014543

package agromonitor.comum.atividade;

import agromonitor.comum.Comunicado; // classe-mae: faz do pedido uma mensagem do protocolo

// Cliente -> Servidor: pede as atividades de todos os plantios de um usuario (tela Atividades).
public class PedidoDeListagemDeAtividades extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;

    // so precisa do usuario: o servidor busca todos os plantios ativos dele
    private int usuarioCodigo;

    // o pedido ja nasce completo; so tem getter, entao nao muda depois de criado
    public PedidoDeListagemDeAtividades(int usuarioCodigo)
    {
        this.usuarioCodigo = usuarioCodigo;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }

    // texto de depuracao: aparece quando o pedido e impresso (ex.: em logs)
    @Override
    public String toString() { return "PedidoDeListagemDeAtividades[usuario=" + usuarioCodigo + "]"; }
}