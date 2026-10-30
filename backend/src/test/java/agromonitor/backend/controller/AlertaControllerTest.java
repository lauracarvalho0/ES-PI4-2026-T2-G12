package agromonitor.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import agromonitor.backend.ConexaoComServidor;
import agromonitor.comum.Comunicado;
import agromonitor.comum.alerta.*;

@WebMvcTest(AlertaController.class)
class AlertaControllerTest
{
    @Autowired MockMvc mvc;
    @MockitoBean ConexaoComServidor servidor;

    private Comunicado capturar() throws Exception
    {
        ArgumentCaptor<Comunicado> cap = ArgumentCaptor.forClass(Comunicado.class);
        verify(servidor).enviarEReceber(cap.capture());
        return cap.getValue();
    }

    @Test
    void listarAlertasRetorna200ComArray() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeListagemDeAlertas(true, "ok", new ArrayList<>()));

        mvc.perform(get("/api/alertas").param("usuarioCodigo", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.alertas").isArray());

        assertEquals(5, ((PedidoDeListagemDeAlertas) capturar()).getUsuarioCodigo());
    }

    @Test
    void listarAlertasRecusadoRetorna400() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeListagemDeAlertas(false, "Usuario nao identificado. Entre novamente.", null));

        mvc.perform(get("/api/alertas"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.alertas").doesNotExist());

        assertEquals(0, ((PedidoDeListagemDeAlertas) capturar()).getUsuarioCodigo());
    }

    @Test
    void listarAlertasComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("x"));
        mvc.perform(get("/api/alertas").param("usuarioCodigo", "5")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void marcarComoLidoOk() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeMarcacaoDeAlertaLido(true, "Alerta lido"));

        mvc.perform(post("/api/alertas/lido").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioCodigo\":5,\"alertaCodigo\":77}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.sucesso").value(true));

        PedidoDeMarcacaoDeAlertaLido p = (PedidoDeMarcacaoDeAlertaLido) capturar();
        assertEquals(5, p.getUsuarioCodigo());
        assertEquals(77, p.getAlertaCodigo());
    }

    @Test
    void marcarComoLidoRecusadoRetorna400() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeMarcacaoDeAlertaLido(false, "Alerta nao encontrado"));

        mvc.perform(post("/api/alertas/lido").contentType(MediaType.APPLICATION_JSON).content("{}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.mensagem").value("Alerta nao encontrado"));
    }

    @Test
    void alertaClimaticoRepassaTodosOsCampos() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeEnvioDeAlertaClimatico(true, "registrado"));

        mvc.perform(post("/api/alertas/climaticos/email").contentType(MediaType.APPLICATION_JSON).content("""
                {"usuarioCodigo":5,"plantioCodigo":9,"chave":"geada-2026-10-28","titulo":"Geada",
                 "descricao":"Risco de geada","prioridade":"ALTA","data":"2026-10-28"}"""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.sucesso").value(true));

        PedidoDeEnvioDeAlertaClimatico p = (PedidoDeEnvioDeAlertaClimatico) capturar();
        assertEquals(5, p.getUsuarioCodigo());
        assertEquals(9, p.getPlantioCodigo());
        assertEquals("geada-2026-10-28", p.getChave());
        assertEquals("Geada", p.getTitulo());
        assertEquals("ALTA", p.getPrioridade());
        assertEquals("2026-10-28", p.getData());
    }

    @Test
    void alertaClimaticoComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("x"));

        mvc.perform(post("/api/alertas/climaticos/email").contentType(MediaType.APPLICATION_JSON).content("{}"))
           .andExpect(status().isServiceUnavailable());
    }
}
