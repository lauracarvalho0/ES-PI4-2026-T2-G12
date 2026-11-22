// Laura Carvalho - 25014543

package agromonitor.comum;

// Enviado pelo cliente quando quer se desconectar do servidor.
// extends Comunicado: e assim que o servidor reconhece que isso e uma mensagem do protocolo
public class PedidoParaSair extends Comunicado
{
    // versao da classe pra serializacao (o Comunicado ja e Serializable)
    private static final long serialVersionUID = 1L;
}