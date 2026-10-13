package agromonitor.backend;

import java.io.*;
import java.net.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import agromonitor.comum.*;

// Faz o papel do "Cliente" do professor: conecta no servidor por Socket,
// cria o Parceiro, manda um pedido e recebe a resposta.
// Cada requisicao HTTP abre uma conexao curta e a fecha com PedidoParaSair.
@Component
public class ConexaoComServidor
{
    @Value("${servidor.host:localhost}")
    private String host;

    @Value("${servidor.porta:3000}")
    private int porta;

    public Comunicado enviarEReceber(Comunicado pedido) throws Exception
    {
        Socket conexao = new Socket();
        conexao.connect(new InetSocketAddress(this.host, this.porta), 5000);
        conexao.setSoTimeout(20000);

        ObjectOutputStream transmissor = new ObjectOutputStream(conexao.getOutputStream());
        transmissor.flush();
        ObjectInputStream receptor = new ObjectInputStream(conexao.getInputStream());

        Parceiro servidor = new Parceiro(conexao, receptor, transmissor);
        try
        {
            servidor.receba(pedido);
            Comunicado resposta = servidor.envie();

            if (resposta instanceof ComunicadoDeDesligamento)
                throw new Exception("Servidor sendo desligado");

            try { servidor.receba(new PedidoParaSair()); } catch (Exception ignorada) {}
            return resposta;
        }
        finally
        {
            try { servidor.adeus(); } catch (Exception ignorada) {}
        }
    }
}
