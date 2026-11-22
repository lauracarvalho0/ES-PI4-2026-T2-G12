// Laura Carvalho - 25014543

package agromonitor.comum;

import java.io.Serializable;

// Raiz de tudo que trafega pela rede (classe "oca", como no exemplo do professor).
// Precisa implementar Serializable para poder ser enviada por Socket.
public class Comunicado implements Serializable
{
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    // Sem campos nem metodos de proposito: serve so como tipo comum.
    // Por isso o backend pode receber qualquer Pedido/Resposta como Comunicado
    // e depois fazer o cast para o tipo especifico.
}