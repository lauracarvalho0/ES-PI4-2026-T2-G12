// Laura Carvalho - 25014543

package agromonitor.comum;

// Enviado pelo cliente quando quer se desconectar do servidor.
public class PedidoParaSair extends Comunicado
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // Sem campos de proposito: o proprio tipo da mensagem ja diz o que o cliente quer.
    // E enviado pelo ConexaoComServidor ao fim de cada requisicao, antes de fechar o socket.
}