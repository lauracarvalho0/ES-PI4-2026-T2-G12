package agromonitor.backend.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import agromonitor.backend.ConexaoComServidor;
import agromonitor.comum.Comunicado;
import agromonitor.comum.alerta.PedidoDeListagemDeAlertas;
import agromonitor.comum.alerta.PedidoDeMarcacaoDeAlertaLido;
import agromonitor.comum.alerta.RespostaDeListagemDeAlertas;
import agromonitor.comum.alerta.RespostaDeMarcacaoDeAlertaLido;

// Recebe JSON do front (fetch) e repassa ao servidor como objetos Comunicado.
// Nao valida regra de negocio: quem valida e grava no Mongo e o SERVIDOR.
@RestController
@RequestMapping("/api")
@CrossOrigin
public class AlertaController
{
    public record MarcarLidoRequest(Integer usuarioCodigo, Integer alertaCodigo) {}

    @Autowired
    private ConexaoComServidor servidor;

    @GetMapping("/alertas")
    public ResponseEntity<Map<String, Object>> listar(@RequestParam(required = false) Integer usuarioCodigo)
    {
        try
        {
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

    private ResponseEntity<Map<String, Object>> indisponivel()
    {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sucesso", false);
        corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
    }
}
