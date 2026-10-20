package agromonitor.backend;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import agromonitor.comum.*;

// Recebe JSON do front (fetch) e repassa ao servidor como objetos Comunicado.
// Nao valida regra de negocio: quem valida e grava no Mongo e o SERVIDOR.
@RestController
@RequestMapping("/api")
@CrossOrigin
public class PlantioController
{
    public record PlantioRequest(Integer usuarioCodigo, String cultura, String nome, String safra,
                                 String descricao, String sistemaCultivo, String talhao,
                                 Double areaPlantada, String localizacao, Integer altitude,
                                 String dataPlantio, String metodoPlantio,
                                 String ajustePlantio, String ajusteIrrigacao,
                                 String ajusteAdubacao, String ajusteTratos) {}

    @Autowired
    private ConexaoComServidor servidor;

    @PostMapping("/plantios")
    public ResponseEntity<Map<String, Object>> cadastrar(@RequestBody PlantioRequest req)
    {
        try
        {
            int usuario = req.usuarioCodigo() == null ? 0 : req.usuarioCodigo();

            Comunicado resposta = servidor.enviarEReceber(new PedidoDeCadastroDePlantio(
                usuario, req.cultura(), req.nome(), req.safra(), req.descricao(),
                req.sistemaCultivo(), req.talhao(), req.areaPlantada(), req.localizacao(),
                req.altitude(), req.dataPlantio(), req.metodoPlantio(),
                req.ajustePlantio(), req.ajusteIrrigacao(), req.ajusteAdubacao(), req.ajusteTratos()));

            RespostaDeCadastroDePlantio r = (RespostaDeCadastroDePlantio) resposta;
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

    @GetMapping("/plantios")
    public ResponseEntity<Map<String, Object>> listar(@RequestParam(required = false) Integer usuarioCodigo)
    {
        try
        {
            int usuario = usuarioCodigo == null ? 0 : usuarioCodigo;

            Comunicado resposta = servidor.enviarEReceber(new PedidoDeListagemDePlantios(usuario));

            RespostaDeListagemDePlantios r = (RespostaDeListagemDePlantios) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            if (r.isSucesso())
                corpo.put("plantios", r.getPlantios());
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
