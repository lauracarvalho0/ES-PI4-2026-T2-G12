// Laura Carvalho - 25014543

package agromonitor.comum;

import java.io.Serializable;

// Raiz de tudo que trafega pela rede (classe "oca", como no exemplo do professor).
// Precisa implementar Serializable para poder ser enviada por Socket.
public class Comunicado implements Serializable
{
    // versao da classe pra serializacao; as filhas (Pedido..., Resposta...) herdam o Serializable daqui
    private static final long serialVersionUID = 1L;
}