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
import agromonitor.comum.plantio.*;

@WebMvcTest(PlantioController.class)
class PlantioControllerTest
{
    private static final String JSON_VALIDO = """
        {"usuarioCodigo":5,"cultura":"Soja","nome":"Soja T1","safra":"25/26","descricao":"teste",
         "sistemaCultivo":"IRRIGADO","talhao":"T1","areaPlantada":12.5,"localizacao":"Campinas",
         "altitude":650,"dataPlantio":"2026-10-21","metodoPlantio":"PLANTIO_DIRETO"}
        """;

    @Autowired MockMvc mvc;
    @MockitoBean ConexaoComServidor servidor;

    @Test
    void cadastrarComSucessoRetorna201ECodigo() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeCadastroDePlantio(true, "Plantio cadastrado", 11));

        mvc.perform(post("/api/plantios").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.sucesso").value(true))
           .andExpect(jsonPath("$.codigo").value(11));

        ArgumentCaptor<Comunicado> cap = ArgumentCaptor.forClass(Comunicado.class);
        verify(servidor).enviarEReceber(cap.capture());
        assertEquals(PedidoDeCadastroDePlantio.class, cap.getValue().getClass());
    }

    @Test
    void cadastrarRecusadoRetorna400SemCodigo() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeCadastroDePlantio(false, "Selecione a cultura.", 0));

        mvc.perform(post("/api/plantios").contentType(MediaType.APPLICATION_JSON).content("{\"usuarioCodigo\":5}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.mensagem").value("Selecione a cultura."))
           .andExpect(jsonPath("$.codigo").doesNotExist());
    }

    @Test
    void cadastrarSemUsuarioEnviaCodigoZero() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeCadastroDePlantio(false, "Usuario nao identificado. Entre novamente.", 0));

        mvc.perform(post("/api/plantios").contentType(MediaType.APPLICATION_JSON).content("{\"cultura\":\"Soja\"}"))
           .andExpect(status().isBadRequest());

        ArgumentCaptor<Comunicado> cap = ArgumentCaptor.forClass(Comunicado.class);
        verify(servidor).enviarEReceber(cap.capture());
        assertEquals(0, ((PedidoDeCadastroDePlantio) cap.getValue()).getUsuarioCodigo());
    }

    @Test
    void cadastrarComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("refused"));

        mvc.perform(post("/api/plantios").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
           .andExpect(status().isServiceUnavailable())
           .andExpect(jsonPath("$.sucesso").value(false));
    }

    @Test
    void listarRetorna200ComArrayDePlantios() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeListagemDePlantios(true, "ok", new ArrayList<>()));

        mvc.perform(get("/api/plantios").param("usuarioCodigo", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.sucesso").value(true))
           .andExpect(jsonPath("$.plantios").isArray());

        ArgumentCaptor<Comunicado> cap = ArgumentCaptor.forClass(Comunicado.class);
        verify(servidor).enviarEReceber(cap.capture());
        assertEquals(5, ((PedidoDeListagemDePlantios) cap.getValue()).getUsuarioCodigo());
    }

    @Test
    void listarSemUsuarioEnviaZeroERetorna400QuandoServidorRecusa() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenReturn(new RespostaDeListagemDePlantios(false, "Usuario nao identificado. Entre novamente.", null));

        mvc.perform(get("/api/plantios"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.plantios").doesNotExist());

        ArgumentCaptor<Comunicado> cap = ArgumentCaptor.forClass(Comunicado.class);
        verify(servidor).enviarEReceber(cap.capture());
        assertEquals(0, ((PedidoDeListagemDePlantios) cap.getValue()).getUsuarioCodigo());
    }

    @Test
    void listarComServidorForaDoArRetorna503() throws Exception
    {
        when(servidor.enviarEReceber(any())).thenThrow(new Exception("x"));

        mvc.perform(get("/api/plantios").param("usuarioCodigo", "5"))
           .andExpect(status().isServiceUnavailable());
    }
}
