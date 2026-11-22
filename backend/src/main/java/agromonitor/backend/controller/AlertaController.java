// Autoria: Beatriz Leme — RA 25015554.
package agromonitor.backend.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import agromonitor.backend.ConexaoComServidor;
import agromonitor.comum.Comunicado;
import agromonitor.comum.alerta.*;

// Recebe JSON do front (fetch) e repassa ao servidor como objetos Comunicado.
// Nao valida regra de negocio: quem valida e grava no Mongo e o SERVIDOR.
@RestController
@RequestMapping("/api")
@CrossOrigin
public class AlertaController
{
    public record MarcarLidoRequest(Integer usuarioCodigo, Integer alertaCodigo) {}
    public record AlertaClimaticoRequest(Integer usuarioCodigo, Integer plantioCodigo, String chave,
            String titulo, String descricao, String prioridade, String data) {}

    @Autowired
    private ConexaoComServidor servidor;

    @GetMapping("/alertas")
    // Traduz a resposta do protocolo interno para o formato JSON consumido pelo front.
    public ResponseEntity<Map<String, Object>> listar(@RequestParam(required = false) Integer usuarioCodigo)
    {
        try
        {
            // Sem código informado, usa zero como valor inválido tratado pelo servidor.
            int usuario = usuarioCodigo == null ? 0 : usuarioCodigo;

            Comunicado resposta = servidor.enviarEReceber(new PedidoDeListagemDeAlertas(usuario));

            RespostaDeListagemDeAlertas r = (RespostaDeListagemDeAlertas) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            if (r.isSucesso())
                corpo.put("alertas", r.getAlertas());
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            return indisponivel();
        }
    }

    @PostMapping("/alertas/lido")
    // O código do usuário acompanha o alerta para impedir que outro usuário o altere.
    public ResponseEntity<Map<String, Object>> marcarComoLido(@RequestBody MarcarLidoRequest req)
    {
        try
        {
            int usuario = req.usuarioCodigo() == null ? 0 : req.usuarioCodigo();
            int alerta  = req.alertaCodigo()  == null ? 0 : req.alertaCodigo();

            Comunicado resposta = servidor.enviarEReceber(new PedidoDeMarcacaoDeAlertaLido(usuario, alerta));

            RespostaDeMarcacaoDeAlertaLido r = (RespostaDeMarcacaoDeAlertaLido) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            return indisponivel();
        }
    }

    @PostMapping("/alertas/climaticos/email")
    // Encaminha ao servidor o alerta climático produzido pela previsão no navegador.
    public ResponseEntity<Map<String, Object>> registrarAlertaClimatico(@RequestBody AlertaClimaticoRequest req)
    {
        try
        {
            Comunicado resposta = servidor.enviarEReceber(new PedidoDeEnvioDeAlertaClimatico(
                req.usuarioCodigo() == null ? 0 : req.usuarioCodigo(),
                req.plantioCodigo() == null ? 0 : req.plantioCodigo(), req.chave(), req.titulo(),
                req.descricao(), req.prioridade(), req.data()));
            RespostaDeEnvioDeAlertaClimatico r = (RespostaDeEnvioDeAlertaClimatico) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            return indisponivel();
        }
    }

    private ResponseEntity<Map<String, Object>> indisponivel()
    {
        // Padroniza a resposta quando a comunicação com o servidor interno falha.
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sucesso", false);
        corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
    }
}
