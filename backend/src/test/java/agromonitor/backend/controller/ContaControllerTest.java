package agromonitor.backend.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import agromonitor.backend.ConexaoComServidor;
import agromonitor.comum.Comunicado;
import agromonitor.comum.conta.*;

@WebMvcTest(ContaController.class)
class ContaControllerTest
{
    @Autowired MockMvc mvc;
    @MockitoBean ConexaoComServidor servidor;

    private Comunicado capturar() throws Exception
    {
        ArgumentCaptor<Comunicado> cap = ArgumentCaptor.forClass(Comunicado.class);
        verify(servidor).enviarEReceber(cap.capture());
        return cap.getValue();
    }

    // ---------- GET /api/conta ----------
    @Test
    void buscarPerfilRetornaObjetoConta() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeBuscaDePerfil(true, "ok", "Maria", "maria@x.com",
                "19999990000", true));

        mvc.perform(get("/api/conta").param("usuarioCodigo", "3"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.conta.nome").value("Maria"))
           .andExpect(jsonPath("$.conta.email").value("maria@x.com"))
           .andExpect(jsonPath("$.conta.telefone").value("19999990000"))
           .andExpect(jsonPath("$.conta.receberAlertasPorEmail").value(true));

        assertEquals(3, ((PedidoDeBuscaDePerfil) capturar()).getUsuarioCodigo());
    }

    @Test
    void buscarPerfilRecusadoRetorna400SemConta() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(
            new RespostaDeBuscaDePerfil(false, "Usuario nao identificado. Entre novamente.", null, null, null, false));

        mvc.perform(get("/api/conta"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.conta").doesNotExist());

        assertEquals(0, ((PedidoDeBuscaDePerfil) capturar()).getUsuarioCodigo());
    }

    @Test
    void buscarPerfilComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("x"));
        mvc.perform(get("/api/conta").param("usuarioCodigo", "3")).andExpect(status().isServiceUnavailable());
    }

    // ---------- PUT /api/conta ----------
    @Test
    void atualizarPerfilOk() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeConta(true, "Perfil atualizado"));

        mvc.perform(put("/api/conta").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioCodigo\":3,\"nome\":\"Maria\",\"email\":\"m@x.com\",\"telefone\":\"1\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.mensagem").value("Perfil atualizado"));

        assertInstanceOf(PedidoDeAtualizacaoDePerfil.class, capturar());
    }

    @Test
    void atualizarPerfilRecusado() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeConta(false, "E-mail invalido."));

        mvc.perform(put("/api/conta").contentType(MediaType.APPLICATION_JSON).content("{\"usuarioCodigo\":3}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.sucesso").value(false));
    }

    // ---------- PUT /api/conta/preferencias ----------
    @Test
    void preferenciasRepassaDados() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeConta(true, "ok"));

        mvc.perform(put("/api/conta/preferencias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioCodigo\":3,\"receberAlertasPorEmail\":false}"))
           .andExpect(status().isOk());

        PedidoDeAtualizacaoDePreferencias p = (PedidoDeAtualizacaoDePreferencias) capturar();
        assertEquals(3, p.getUsuarioCodigo());
        assertFalse(p.isReceberAlertasPorEmail());
    }

    @Test
    void preferenciasSemFlagDeEmailAssumeTrue() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeConta(true, "ok"));

        mvc.perform(put("/api/conta/preferencias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioCodigo\":3}"))
           .andExpect(status().isOk());

        assertTrue(((PedidoDeAtualizacaoDePreferencias) capturar()).isReceberAlertasPorEmail());
    }

    // ---------- PUT /api/conta/senha ----------
    @Test
    void alterarSenhaOk() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeConta(true, "Senha alterada"));

        mvc.perform(put("/api/conta/senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioCodigo\":3,\"senhaAtual\":\"123456\",\"novaSenha\":\"654321\"}"))
           .andExpect(status().isOk());

        PedidoDeAlteracaoDeSenha p = (PedidoDeAlteracaoDeSenha) capturar();
        assertEquals("123456", p.getSenhaAtual());
        assertEquals("654321", p.getNovaSenha());
    }

    @Test
    void alterarSenhaComSenhaAtualErradaRetorna400() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeConta(false, "Senha atual incorreta"));

        mvc.perform(put("/api/conta/senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioCodigo\":3,\"senhaAtual\":\"x\",\"novaSenha\":\"654321\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.mensagem").value("Senha atual incorreta"));
    }

    // ---------- POST /api/conta/excluir ----------
    @Test
    void excluirContaOk() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeConta(true, "Conta excluida"));

        mvc.perform(post("/api/conta/excluir").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioCodigo\":3,\"senha\":\"123456\"}"))
           .andExpect(status().isOk());

        PedidoDeExclusaoDeConta p = (PedidoDeExclusaoDeConta) capturar();
        assertEquals(3, p.getUsuarioCodigo());
        assertEquals("123456", p.getSenha());
    }

    @Test
    void excluirContaComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("x"));

        mvc.perform(post("/api/conta/excluir").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioCodigo\":3,\"senha\":\"123456\"}"))
           .andExpect(status().isServiceUnavailable());
    }
}
