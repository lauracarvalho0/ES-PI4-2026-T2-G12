package agromonitor.backend.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import agromonitor.backend.ConexaoComServidor;
import agromonitor.comum.atividade.PedidoDeConclusaoDeAtividade;
import agromonitor.comum.atividade.PedidoDeListagemDeAtividades;
import agromonitor.comum.atividade.PedidoDeRegistroDeAtividade;
import agromonitor.comum.atividade.RespostaDeConclusaoDeAtividade;
import agromonitor.comum.atividade.RespostaDeListagemDeAtividades;
import agromonitor.comum.atividade.RespostaDeRegistroDeAtividade;
import agromonitor.comum.Comunicado;

// Recebe JSON do front (fetch) e repassa ao servidor como objetos Comunicado.
// Nao valida regra de negocio: quem valida e grava no Mongo e o SERVIDOR.
@RestController
@RequestMapping("/api")
@CrossOrigin
public class AtividadeController
{
    public record AtividadeRequest(Integer usuarioCodigo, Integer plantioCodigo, String tipoAtividade,
                                   String status, String dataExecucao, String quantidade,
                                   String responsavel, String observacoes) {}

    public record ConclusaoRequest(Integer usuarioCodigo, Integer plantioCodigo, Integer atividadeCodigo) {}

    @Autowired
    private ConexaoComServidor servidor;

    @PostMapping("/atividades")
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody AtividadeRequest req)
    {
        try
        {
            int usuario = req.usuarioCodigo() == null ? 0 : req.usuarioCodigo();
            int plantio = req.plantioCodigo() == null ? 0 : req.plantioCodigo();

            Comunicado resposta = servidor.enviarEReceber(new PedidoDeRegistroDeAtividade(
                usuario, plantio, req.tipoAtividade(), req.status(), req.dataExecucao(),
                req.quantidade(), req.responsavel(), req.observacoes()));

            RespostaDeRegistroDeAtividade r = (RespostaDeRegistroDeAtividade) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            if (r.isSucesso())
                corpo.put("codigo", r.getCodigo());
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", false);
            corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
        }
    }

    @GetMapping("/atividades")
    public ResponseEntity<Map<String, Object>> listar(@RequestParam(required = false) Integer usuarioCodigo)
    {
        try
        {
            int usuario = usuarioCodigo == null ? 0 : usuarioCodigo;

            Comunicado resposta = servidor.enviarEReceber(new PedidoDeListagemDeAtividades(usuario));

            RespostaDeListagemDeAtividades r = (RespostaDeListagemDeAtividades) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            if (r.isSucesso())
                corpo.put("atividades", r.getAtividades());
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", false);
            corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
        }
    }

    @PostMapping("/atividades/concluir")
    public ResponseEntity<Map<String, Object>> concluir(@RequestBody ConclusaoRequest req)
    {
        try
        {
            int usuario   = req.usuarioCodigo()   == null ? 0 : req.usuarioCodigo();
            int plantio   = req.plantioCodigo()   == null ? 0 : req.plantioCodigo();
            int atividade = req.atividadeCodigo() == null ? 0 : req.atividadeCodigo();

            Comunicado resposta = servidor.enviarEReceber(new PedidoDeConclusaoDeAtividade(usuario, plantio, atividade));

            RespostaDeConclusaoDeAtividade r = (RespostaDeConclusaoDeAtividade) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", false);
            corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
        }
    }
}
