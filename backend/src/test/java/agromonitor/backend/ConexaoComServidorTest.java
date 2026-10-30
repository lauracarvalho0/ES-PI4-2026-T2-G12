package agromonitor.backend;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import agromonitor.comum.Comunicado;
import agromonitor.comum.ComunicadoDeDesligamento;
import agromonitor.comum.PedidoParaSair;
import agromonitor.comum.auth.PedidoDeLogin;
import agromonitor.comum.auth.RespostaDeLogin;

// Teste de integracao leve: sobe um "servidor" falso em uma porta livre e confere
// o protocolo (pedido -> resposta -> PedidoParaSair) sem depender do Mongo.
class ConexaoComServidorTest
{
    private ServerSocket falso;
    private ConexaoComServidor conexao;

    @BeforeEach
    void preparar() throws Exception
    {
        falso = new ServerSocket(0);
        conexao = new ConexaoComServidor();
        ReflectionTestUtils.setField(conexao, "host", "localhost");
        ReflectionTestUtils.setField(conexao, "porta", falso.getLocalPort());
    }

    @AfterEach
    void limpar() throws Exception
    {
        falso.close();
    }

    // Servidor falso: le 1 pedido, responde com "resposta" e devolve o que mais chegou depois.
    private CompletableFuture<Comunicado[]> servidorFalso(Comunicado resposta)
    {
        return CompletableFuture.supplyAsync(() -> {
            try (Socket s = falso.accept())
            {
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());

                Comunicado pedido = (Comunicado) in.readObject();
                out.writeObject(resposta);
                out.flush();

                Comunicado seguinte = null;
                try { seguinte = (Comunicado) in.readObject(); } catch (Exception fimDaConexao) {}
                return new Comunicado[] { pedido, seguinte };
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void enviaPedidoRecebeRespostaEAvisaQueVaiSair() throws Exception
    {
        var futuro = servidorFalso(new RespostaDeLogin(true, "ok", 3, "Maria", "maria@x.com"));

        Comunicado resposta = conexao.enviarEReceber(new PedidoDeLogin("maria@x.com", "123456"));

        RespostaDeLogin r = assertInstanceOf(RespostaDeLogin.class, resposta);
        assertTrue(r.isSucesso());
        assertEquals(3, r.getCodigo());

        Comunicado[] visto = futuro.get(5, TimeUnit.SECONDS);
        PedidoDeLogin recebido = assertInstanceOf(PedidoDeLogin.class, visto[0]);
        assertEquals("maria@x.com", recebido.getEmail());
        assertInstanceOf(PedidoParaSair.class, visto[1]);
    }

    @Test
    void servidorDesligandoGeraExcecao() throws Exception
    {
        servidorFalso(new ComunicadoDeDesligamento());

        Exception e = assertThrows(Exception.class,
            () -> conexao.enviarEReceber(new PedidoDeLogin("a@b.com", "123456")));
        assertEquals("Servidor sendo desligado", e.getMessage());
    }

    @Test
    void servidorInacessivelGeraExcecao() throws Exception
    {
        int portaFechada = falso.getLocalPort();
        falso.close();
        ReflectionTestUtils.setField(conexao, "porta", portaFechada);

        assertThrows(Exception.class, () -> conexao.enviarEReceber(new PedidoDeLogin("a@b.com", "123456")));
    }
}
