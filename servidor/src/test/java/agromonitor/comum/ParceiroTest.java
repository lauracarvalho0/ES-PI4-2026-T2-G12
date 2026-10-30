package agromonitor.comum;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import agromonitor.comum.auth.PedidoDeLogin;

// Dois sockets de verdade ligados entre si (loopback): "a" e o lado do servidor, "b" e o do cliente.
class ParceiroTest
{
    private ServerSocket ss;
    private Socket       ladoServidor;
    private Socket       ladoCliente;
    private Parceiro     servidor;   // Parceiro que o servidor usa para falar com o cliente
    private ObjectOutputStream saidaCliente;
    private ObjectInputStream  entradaCliente;
    private ObjectOutputStream saidaServ;
    private ObjectInputStream  entradaServ;

    @BeforeEach
    void preparar() throws Exception
    {
        ss = new ServerSocket(0);
        ladoCliente = new Socket("localhost", ss.getLocalPort());
        ladoServidor = ss.accept();

        saidaServ = new ObjectOutputStream(ladoServidor.getOutputStream());
        saidaServ.flush();
        saidaCliente = new ObjectOutputStream(ladoCliente.getOutputStream());
        saidaCliente.flush();
        entradaServ = new ObjectInputStream(ladoServidor.getInputStream());
        entradaCliente = new ObjectInputStream(ladoCliente.getInputStream());

        servidor = new Parceiro(ladoServidor, entradaServ, saidaServ);
    }

    @AfterEach
    void limpar() throws Exception
    {
        try { ladoCliente.close(); } catch (Exception e) {}
        try { ladoServidor.close(); } catch (Exception e) {}
        ss.close();
    }

    @Test
    void construtorExigeTodosOsParametros() throws Exception
    {
        assertEquals("Conexao ausente",
            assertThrows(Exception.class, () -> new Parceiro(null, null, null)).getMessage());
        assertEquals("Receptor ausente",
            assertThrows(Exception.class, () -> new Parceiro(ladoServidor, null, null)).getMessage());
        assertEquals("Transmissor ausente",
            assertThrows(Exception.class, () -> new Parceiro(ladoServidor, entradaServ, null)).getMessage());
    }

    @Test
    void recebaEnviaUmComunicadoParaOOutroLado() throws Exception
    {
        servidor.receba(new PedidoParaSair());
        assertInstanceOf(PedidoParaSair.class, entradaCliente.readObject());
    }

    @Test
    void envieRecebeUmComunicadoDoOutroLado() throws Exception
    {
        saidaCliente.writeObject(new PedidoDeLogin("a@x.com", "123456"));
        saidaCliente.flush();

        PedidoDeLogin p = assertInstanceOf(PedidoDeLogin.class, servidor.envie());
        assertEquals("a@x.com", p.getEmail());
    }

    @Test
    void espieNaoConsomeOComunicado() throws Exception
    {
        saidaCliente.writeObject(new PedidoDeLogin("a@x.com", "123456"));
        saidaCliente.flush();

        Comunicado visto1 = servidor.espie();
        Comunicado visto2 = servidor.espie();
        assertSame(visto1, visto2);                 // continua o mesmo
        assertSame(visto1, servidor.envie());       // envie entrega o que foi espiado
    }

    @Test
    void comunicadosChegamNaOrdemEnviada() throws Exception
    {
        saidaCliente.writeObject(new PedidoDeLogin("1@x.com", "123456"));
        saidaCliente.writeObject(new PedidoDeLogin("2@x.com", "123456"));
        saidaCliente.writeObject(new PedidoParaSair());
        saidaCliente.flush();

        assertEquals("1@x.com", ((PedidoDeLogin) servidor.envie()).getEmail());
        assertEquals("2@x.com", ((PedidoDeLogin) servidor.envie()).getEmail());
        assertInstanceOf(PedidoParaSair.class, servidor.envie());
    }

    @Test
    void envieComConexaoFechadaLancaErroDeRecepcao() throws Exception
    {
        ladoCliente.close();
        assertEquals("Erro de recepcao", assertThrows(Exception.class, () -> servidor.envie()).getMessage());
        assertEquals("Erro de recepcao", assertThrows(Exception.class, () -> servidor.espie()).getMessage());
    }

    @Test
    void adeusFechaAConexao() throws Exception
    {
        servidor.adeus();
        assertTrue(ladoServidor.isClosed());
    }
}
