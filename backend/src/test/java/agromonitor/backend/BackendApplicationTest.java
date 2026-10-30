package agromonitor.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import agromonitor.backend.controller.*;
import agromonitor.comum.Comunicado;
import agromonitor.comum.PedidoParaSair;
import agromonitor.comum.auth.PedidoDeLogin;
import agromonitor.comum.auth.RespostaDeLogin;
import agromonitor.comum.plantio.PedidoDeListagemDePlantios;
import agromonitor.comum.plantio.RespostaDeListagemDePlantios;

// Aplicacao inteira no ar (sem mocks): HTTP -> controller -> ConexaoComServidor (real) -> servidor falso por socket.
@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTest
{
    private static final ServerSocket servidorFalso;

    static
    {
        try { servidorFalso = new ServerSocket(0); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }

        Thread aceitadora = new Thread(() -> {
            for (;;)
            {
                try
                {
                    Socket s = servidorFalso.accept();
                    Thread atendimento = new Thread(() -> atender(s));
                    atendimento.setDaemon(true);
                    atendimento.start();
                }
                catch (Exception fim) { return; }
            }
        });
        aceitadora.setDaemon(true);
        aceitadora.start();
    }

    // responde como o servidor de verdade responderia, so para os pedidos usados neste teste
    private static void atender(Socket s)
    {
        try (Socket conexao = s)
        {
            ObjectOutputStream out = new ObjectOutputStream(conexao.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(conexao.getInputStream());

            Comunicado pedido = (Comunicado) in.readObject();
            Comunicado resposta;
            if (pedido instanceof PedidoDeLogin)
            {
                PedidoDeLogin l = (PedidoDeLogin) pedido;
                resposta = "123456".equals(l.getSenha())
                    ? new RespostaDeLogin(true, "ok", 7, "Maria", l.getEmail())
                    : new RespostaDeLogin(false, "E-mail ou senha incorretos.", 0, null, null);
            }
            else if (pedido instanceof PedidoDeListagemDePlantios)
                resposta = new RespostaDeListagemDePlantios(true, "OK", new java.util.ArrayList<>());
            else
                resposta = new agromonitor.comum.auth.RespostaDeCadastro(false, "pedido nao tratado no teste", 0);

            out.writeObject(resposta);
            out.flush();
            try { in.readObject(); } catch (Exception fim) {}   // PedidoParaSair
        }
        catch (Exception ignorada) {}
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro)
    {
        registro.add("servidor.host", () -> "localhost");
        registro.add("servidor.porta", () -> servidorFalso.getLocalPort());
    }

    @AfterAll
    static void fechar() throws Exception { servidorFalso.close(); }

    @Autowired MockMvc mvc;
    @Autowired ApplicationContext contexto;

    @Test
    void aAplicacaoSobeComTodosOsControllers()
    {
        assertNotNull(contexto.getBean(ConexaoComServidor.class));
        assertNotNull(contexto.getBean(AuthController.class));
        assertNotNull(contexto.getBean(PlantioController.class));
        assertNotNull(contexto.getBean(AtividadeController.class));
        assertNotNull(contexto.getBean(ContaController.class));
        assertNotNull(contexto.getBean(AlertaController.class));
    }

    @Test
    void loginPassaPeloControllerEPelaConexaoReal() throws Exception
    {
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@x.com\",\"senha\":\"123456\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.sucesso").value(true))
           .andExpect(jsonPath("$.usuario.codigo").value(7))
           .andExpect(jsonPath("$.usuario.email").value("maria@x.com"));
    }

    @Test
    void loginInvalidoRetorna401() throws Exception
    {
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@x.com\",\"senha\":\"errada\"}"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.mensagem").value("E-mail ou senha incorretos."));
    }

    @Test
    void listarPlantiosPassaPelaConexaoReal() throws Exception
    {
        mvc.perform(get("/api/plantios").param("usuarioCodigo", "7"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.sucesso").value(true))
           .andExpect(jsonPath("$.plantios").isArray());
    }

    // ---------- o backend tambem serve o front-end (pasta ../front) ----------
    @Test
    void servePaginasEScriptsDoFront() throws Exception
    {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
        mvc.perform(get("/pages/auth/login.html")).andExpect(status().isOk());
        mvc.perform(get("/js/auth.js")).andExpect(status().isOk());
    }

    @Test
    void paginaInexistenteRetorna404() throws Exception
    {
        mvc.perform(get("/pages/nao-existe.html")).andExpect(status().isNotFound());
    }
}
