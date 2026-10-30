package agromonitor.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import agromonitor.backend.ConexaoComServidor;
import agromonitor.comum.Comunicado;
import agromonitor.comum.auth.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest
{
    @Autowired MockMvc mvc;
    @MockitoBean ConexaoComServidor servidor;

    // ---------- POST /api/cadastro ----------
    @Test
    void cadastroComSucessoRetorna201() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeCadastro(true, "Cadastro realizado", 7));

        mvc.perform(post("/api/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria Silva\",\"email\":\"maria@x.com\",\"cpf\":\"52998224725\",\"telefone\":\"19999990000\",\"senha\":\"123456\"}"))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.sucesso").value(true))
           .andExpect(jsonPath("$.mensagem").value("Cadastro realizado"));

        ArgumentCaptor<Comunicado> cap = ArgumentCaptor.forClass(Comunicado.class);
        verify(servidor).enviarEReceber(cap.capture());
        PedidoDeCadastro p = (PedidoDeCadastro) cap.getValue();
        assertEquals("Maria Silva", p.getNome());
        assertEquals("maria@x.com", p.getEmail());
        assertEquals("52998224725", p.getCpf());
        assertEquals("123456", p.getSenha());
    }

    @Test
    void cadastroRecusadoPeloServidorRetorna400() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeCadastro(false, "E-mail ja cadastrado", 0));

        mvc.perform(post("/api/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"email\":\"maria@x.com\",\"senha\":\"123456\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.sucesso").value(false))
           .andExpect(jsonPath("$.mensagem").value("E-mail ja cadastrado"));
    }

    @Test
    void cadastroComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("Connection refused"));

        mvc.perform(post("/api/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"email\":\"maria@x.com\",\"senha\":\"123456\"}"))
           .andExpect(status().isServiceUnavailable())
           .andExpect(jsonPath("$.sucesso").value(false))
           .andExpect(jsonPath("$.mensagem").value("Servidor indisponivel. Tente novamente mais tarde."));
    }

    @Test
    void corpoAusenteRetorna400() throws Exception
    {
        mvc.perform(post("/api/cadastro").contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isBadRequest());
    }

    // ---------- POST /api/login ----------
    @Test
    void loginComSucessoRetorna200EDadosDoUsuario() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeLogin(true, "ok", 3, "Maria", "maria@x.com"));

        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@x.com\",\"senha\":\"123456\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.sucesso").value(true))
           .andExpect(jsonPath("$.usuario.codigo").value(3))
           .andExpect(jsonPath("$.usuario.nome").value("Maria"))
           .andExpect(jsonPath("$.usuario.email").value("maria@x.com"));
    }

    @Test
    void loginInvalidoRetorna401SemDadosDeUsuario() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeLogin(false, "Credenciais invalidas", 0, null, null));

        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@x.com\",\"senha\":\"errada\"}"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.sucesso").value(false))
           .andExpect(jsonPath("$.usuario").doesNotExist());
    }

    @Test
    void loginComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("timeout"));

        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.com\",\"senha\":\"123456\"}"))
           .andExpect(status().isServiceUnavailable());
    }

    // ---------- POST /api/recuperar-senha ----------
    @Test
    void recuperarSenhaOk() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeRecuperacaoDeSenha(true, "E-mail enviado"));

        mvc.perform(post("/api/recuperar-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@x.com\",\"urlBase\":\"http://localhost:8080\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.mensagem").value("E-mail enviado"));
    }

    @Test
    void recuperarSenhaRecusada() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeRecuperacaoDeSenha(false, "Aguarde para tentar de novo"));

        mvc.perform(post("/api/recuperar-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria@x.com\",\"urlBase\":\"http://localhost:8080\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.sucesso").value(false));
    }

    // ---------- POST /api/redefinir-senha ----------
    @Test
    void redefinirSenhaOk() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeRedefinicaoDeSenha(true, "Senha alterada"));

        mvc.perform(post("/api/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\",\"senha\":\"novaSenha1\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.sucesso").value(true));
    }

    @Test
    void redefinirSenhaComTokenInvalidoRetorna400() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeRedefinicaoDeSenha(false, "Token invalido ou expirado"));

        mvc.perform(post("/api/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"ruim\",\"senha\":\"novaSenha1\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.mensagem").value("Token invalido ou expirado"));
    }

    @Test
    void redefinirSenhaComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("x"));

        mvc.perform(post("/api/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\",\"senha\":\"novaSenha1\"}"))
           .andExpect(status().isServiceUnavailable());
    }
}
