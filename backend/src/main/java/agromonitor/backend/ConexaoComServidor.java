// Laura Carvalho - 25014543

package agromonitor.backend;

import java.io.*; // Entrada e saída: ObjectInputStream e ObjectOutputStream para trocar objetos pelo socket.
import java.net.*; // Rede: Socket e InetSocketAddress para conectar no servidor.

import org.springframework.beans.factory.annotation.Value; // Lê host e porta do application.properties (@Value).
import org.springframework.stereotype.Component; // Marca a classe como bean do Spring, para ser injetada nos controllers (@Component).

import agromonitor.comum.*;

// Faz o papel do "Cliente": conecta no servidor por Socket, cria o Parceiro, manda um pedido e recebe a resposta.
// Cada requisicao HTTP abre uma conexao curta e a fecha com PedidoParaSair.
@Component   
public class ConexaoComServidor
{
    // Endereco e porta do servidor, lidos do application.properties.
    // Se nao estiverem definidos, usa os valores padrao (localhost e 3000).
    @Value("${servidor.host:localhost}")
    private String host;

    @Value("${servidor.porta:3000}")
    private int porta;

    // Envia um pedido ao servidor e devolve a resposta.
    // Lanca Exception em qualquer falha; os controllers transformam isso em HTTP 503.
    public Comunicado enviarEReceber(Comunicado pedido) throws Exception
    {
        // Abre a conexao com timeout de 5s para conectar e 20s para esperar dados.
        Socket conexao = new Socket();
        conexao.connect(new InetSocketAddress(this.host, this.porta), 5000);
        conexao.setSoTimeout(20000);

        // O flush logo apos criar o ObjectOutputStream envia o cabecalho do stream,
        // evitando que os dois lados fiquem esperando um ao outro ao criar o ObjectInputStream.
        ObjectOutputStream transmissor = new ObjectOutputStream(conexao.getOutputStream());
        transmissor.flush();
        ObjectInputStream receptor = new ObjectInputStream(conexao.getInputStream());

        // Parceiro encapsula a conversa com o servidor (enviar e receber objetos).
        Parceiro servidor = new Parceiro(conexao, receptor, transmissor);
        try
        {
            // Manda o pedido e bloqueia ate chegar a resposta.
            servidor.receba(pedido);
            Comunicado resposta = servidor.envie();

            // Se o servidor avisou que esta desligando, trata como falha.
            if (resposta instanceof ComunicadoDeDesligamento)
                throw new Exception("Servidor sendo desligado");

            // Avisa o servidor que a conversa acabou; se falhar, a resposta ja esta em maos.
            try { servidor.receba(new PedidoParaSair()); } catch (Exception ignorada) {}
            return resposta;
        }
        finally
        {
            // Sempre encerra a conexao, mesmo se der erro acima.
            try { servidor.adeus(); } catch (Exception ignorada) {}
        }
    }
}