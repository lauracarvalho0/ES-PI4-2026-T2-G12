// Laura Carvalho - 25014543

package agromonitor.comum;

import java.io.Serializable; // permite converter o objeto em bytes para enviar pelo Socket

// Raiz de tudo que trafega pela rede (classe "oca")
// Precisa implementar Serializable para poder ser enviada por Socket.
public class Comunicado implements Serializable
{
    // as filhas (Pedido..., Resposta...) herdam o Serializable daqui
    private static final long serialVersionUID = 1L;
}