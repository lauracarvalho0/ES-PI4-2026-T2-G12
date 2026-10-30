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
import agromonitor.comum.atividade.*;

@WebMvcTest(AtividadeController.class)
class AtividadeControllerTest
{
    @Autowired MockMvc mvc;
    @MockitoBean ConexaoComServidor servidor;

    private PedidoDeRegistroDeAtividade capturarRegistro() throws Exception
    {
        ArgumentCaptor<Comunicado> cap = ArgumentCaptor.forClass(Comunicado.class);
        verify(servidor).enviarEReceber(cap.capture());
        return (PedidoDeRegistroDeAtividade) cap.getValue();
    }

    // ---------- POST /api/atividades ----------
    @Test
    void registrarComSucessoRetorna201ComCodigo() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeRegistroDeAtividade(true, "Atividade registrada", 42));

        mvc.perform(post("/api/atividades").contentType(MediaType.APPLICATION_JSON).content("""
                {"usuarioCodigo":5,"plantioCodigo":9,"tipoAtividade":"IRRIGACAO","status":"PENDENTE",
                 "dataExecucao":"2030-01-10","quantidade":"22 mm","responsavel":"Joao","observacoes":"obs"}"""))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.sucesso").value(true))
           .andExpect(jsonPath("$.codigo").value(42));

        PedidoDeRegistroDeAtividade p = capturarRegistro();
        assertEquals(5, p.getUsuarioCodigo());
        assertEquals(9, p.getPlantioCodigo());
        assertEquals("IRRIGACAO", p.getTipoAtividade());
        assertEquals("PENDENTE", p.getStatus());
        assertEquals("2030-01-10", p.getDataExecucao());
        assertEquals("22 mm", p.getQuantidade());
        assertEquals("Joao", p.getResponsavel());
        assertEquals("obs", p.getObservacoes());
    }

    @Test
    void registrarRecusadoRetorna400SemCodigo() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeRegistroDeAtividade(false, "Tipo de atividade invalido.", 0));

        mvc.perform(post("/api/atividades").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioCodigo\":5,\"plantioCodigo\":9,\"tipoAtividade\":\"PODA\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.mensagem").value("Tipo de atividade invalido."))
           .andExpect(jsonPath("$.codigo").doesNotExist());
    }

    @Test
    void registrarSemIdsEnviaZeros() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeRegistroDeAtividade(false, "x", 0));

        mvc.perform(post("/api/atividades").contentType(MediaType.APPLICATION_JSON).content("{}"))
           .andExpect(status().isBadRequest());

        PedidoDeRegistroDeAtividade p = capturarRegistro();
        assertEquals(0, p.getUsuarioCodigo());
        assertEquals(0, p.getPlantioCodigo());
    }

    @Test
    void registrarComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("refused"));

        mvc.perform(post("/api/atividades").contentType(MediaType.APPLICATION_JSON).content("{}"))
           .andExpect(status().isServiceUnavailable())
           .andExpect(jsonPath("$.mensagem").value("Servidor indisponivel. Tente novamente mais tarde."));
    }

    // ---------- GET /api/atividades ----------
    @Test
    void listarRetorna200ComArray() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeListagemDeAtividades(true, "ok", new ArrayList<>()));

        mvc.perform(get("/api/atividades").param("usuarioCodigo", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.atividades").isArray());

        ArgumentCaptor<Comunicado> cap = ArgumentCaptor.forClass(Comunicado.class);
        verify(servidor).enviarEReceber(cap.capture());
        assertEquals(5, ((PedidoDeListagemDeAtividades) cap.getValue()).getUsuarioCodigo());
    }

    @Test
    void listarRecusadoRetorna400() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeListagemDeAtividades(false, "Usuario nao identificado. Entre novamente.", null));

        mvc.perform(get("/api/atividades"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.atividades").doesNotExist());
    }

    @Test
    void listarComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("x"));

        mvc.perform(get("/api/atividades").param("usuarioCodigo", "5"))
           .andExpect(status().isServiceUnavailable());
    }

    // ---------- POST /api/atividades/concluir ----------
    @Test
    void concluirRetorna200ERepassaOsTresCodigos() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeConclusaoDeAtividade(true, "Atividade concluida"));

        mvc.perform(post("/api/atividades/concluir").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioCodigo\":5,\"plantioCodigo\":9,\"atividadeCodigo\":42}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.sucesso").value(true));

        ArgumentCaptor<Comunicado> cap = ArgumentCaptor.forClass(Comunicado.class);
        verify(servidor).enviarEReceber(cap.capture());
        PedidoDeConclusaoDeAtividade p = (PedidoDeConclusaoDeAtividade) cap.getValue();
        assertEquals(5, p.getUsuarioCodigo());
        assertEquals(9, p.getPlantioCodigo());
        assertEquals(42, p.getAtividadeCodigo());
    }

    @Test
    void concluirRecusadoRetorna400() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeConclusaoDeAtividade(false, "Atividade nao encontrada"));

        mvc.perform(post("/api/atividades/concluir").contentType(MediaType.APPLICATION_JSON).content("{}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.mensagem").value("Atividade nao encontrada"));
    }

    @Test
    void concluirComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("x"));

        mvc.perform(post("/api/atividades/concluir").contentType(MediaType.APPLICATION_JSON).content("{}"))
           .andExpect(status().isServiceUnavailable());
    }
}
