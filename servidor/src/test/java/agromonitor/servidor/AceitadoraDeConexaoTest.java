package agromonitor.servidor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import agromonitor.comum.Parceiro;
import agromonitor.comum.PedidoParaSair;
import agromonitor.comum.auth.PedidoDeCadastro;
import agromonitor.comum.auth.PedidoDeLogin;
import agromonitor.comum.auth.RespostaDeCadastro;
import agromonitor.comum.auth.RespostaDeLogin;
import agromonitor.servidor.dao.AlertaDAO;
import agromonitor.servidor.dao.PlantioDAO;
import agromonitor.servidor.dao.UsuarioDAO;
import agromonitor.servidor.email.EmailService;

class AceitadoraDeConexaoTest
{
    private BancoDeTeste bd;
    private UsuarioDAO   usuarioDao;
    private PlantioDAO   plantioDao;
    private AlertaDAO    alertaDao;
    private EmailService email;

    @BeforeEach
    void preparar()
    {
        bd         = new BancoDeTeste();
        usuarioDao = new UsuarioDAO(bd.banco());
        plantioDao = new PlantioDAO(bd.banco());
        alertaDao  = new AlertaDAO(bd.banco());
        usuarioDao.garantirIndices();
        email      = mock(EmailService.class);
    }

    @AfterEach
    void limpar() { bd.close(); }

    private static int portaLivre() throws Exception
    {
        try (ServerSocket s = new ServerSocket(0)) { return s.getLocalPort(); }
    }

    private AceitadoraDeConexao criar(String porta, ArrayList<Parceiro> lista) throws Exception
    {
        return new AceitadoraDeConexao(porta, lista, usuarioDao, plantioDao, alertaDao, email);
    }

    // ---------- validacoes ----------
    @Test
    void portaAusente()
    {
        assertEquals("Porta ausente", assertThrows(Exception.class, () -> criar(null, new ArrayList<>())).getMessage());
    }

    @Test
    void portaInvalida()
    {
        assertEquals("Porta invalida", assertThrows(Exception.class, () -> criar("abc", new ArrayList<>())).getMessage());
        assertEquals("Porta invalida", assertThrows(Exception.class, () -> criar("99999", new ArrayList<>())).getMessage());
    }

    @Test
    void dependenciasAusentes() throws Exception
    {
        assertEquals("Usuarios ausentes",
            assertThrows(Exception.class, () -> criar(String.valueOf(portaLivre()), null)).getMessage());
        assertEquals("DAO ausente",
            assertThrows(Exception.class, () -> new AceitadoraDeConexao(String.valueOf(portaLivre()), new ArrayList<>(), null, plantioDao, alertaDao, email)).getMessage());
        assertEquals("DAO de plantio ausente",
            assertThrows(Exception.class, () -> new AceitadoraDeConexao(String.valueOf(portaLivre()), new ArrayList<>(), usuarioDao, null, alertaDao, email)).getMessage());
        assertEquals("DAO de alerta ausente",
            assertThrows(Exception.class, () -> new AceitadoraDeConexao(String.valueOf(portaLivre()), new ArrayList<>(), usuarioDao, plantioDao, null, email)).getMessage());
        assertEquals("Servico de e-mail ausente",
            assertThrows(Exception.class, () -> new AceitadoraDeConexao(String.valueOf(portaLivre()), new ArrayList<>(), usuarioDao, plantioDao, alertaDao, null)).getMessage());
    }

    @Test
    void portaEmUsoEPortaInvalida() throws Exception
    {
        try (ServerSocket ocupada = new ServerSocket(0))
        {
            assertEquals("Porta invalida",
                assertThrows(Exception.class, () -> criar(String.valueOf(ocupada.getLocalPort()), new ArrayList<>())).getMessage());
        }
    }

    // ---------- funcionamento: atende varios clientes ao mesmo tempo ----------
    private static class Cliente
    {
        final Socket s; final ObjectOutputStream out; final ObjectInputStream in;
        Cliente(int porta) throws Exception
        {
            s = new Socket("localhost", porta);
            s.setSoTimeout(15000);
            out = new ObjectOutputStream(s.getOutputStream()); out.flush();
            in  = new ObjectInputStream(s.getInputStream());
        }
        Object pedir(Object p) throws Exception { out.writeObject(p); out.flush(); return in.readObject(); }
    }

    @Test
    void aceitaVariosClientesEAtendeCadaUm() throws Exception
    {
        int porta = portaLivre();
        ArrayList<Parceiro> lista = new ArrayList<>();
        AceitadoraDeConexao aceitadora = criar(String.valueOf(porta), lista);
        aceitadora.setDaemon(true);
        aceitadora.start();

        Cliente a = new Cliente(porta);
        Cliente b = new Cliente(porta);

        RespostaDeCadastro ca = (RespostaDeCadastro) a.pedir(new PedidoDeCadastro("Maria", "m@x.com", null, null, "123456"));
        assertTrue(ca.isSucesso());
        RespostaDeLogin lb = (RespostaDeLogin) b.pedir(new PedidoDeLogin("m@x.com", "123456")); // outro cliente ve o cadastro
        assertTrue(lb.isSucesso());

        synchronized (lista) { assertEquals(2, lista.size()); }

        a.out.writeObject(new PedidoParaSair()); a.out.flush();
        b.out.writeObject(new PedidoParaSair()); b.out.flush();
        a.s.close(); b.s.close();
    }
}
