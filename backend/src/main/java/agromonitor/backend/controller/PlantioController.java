// Laura Carvalho - 25014543

package agromonitor.backend.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import agromonitor.backend.ConexaoComServidor;
import agromonitor.comum.Comunicado;
import agromonitor.comum.plantio.*;

// Recebe JSON do front (fetch) e repassa ao servidor como objetos Comunicado.
// Nao valida regra de negocio: quem valida e grava no Mongo e o SERVIDOR.
@RestController
@RequestMapping("/api")   // todas as rotas daqui comecam com /api
@CrossOrigin              // libera o front (outra origem) para chamar esta API
public class PlantioController
{
    // Corpo JSON esperado no cadastro de um plantio.
    // Integer e Double (e nao int e double) permitem receber null quando o campo nao vem no JSON.
    public record PlantioRequest(Integer usuarioCodigo, String cultura, String nome, String safra,
                                 String descricao, String sistemaCultivo, String talhao,
                                 Double areaPlantada, String localizacao, Integer altitude,
                                 String dataPlantio, String metodoPlantio,
                                 String ajustePlantio, String ajusteIrrigacao,
                                 String ajusteAdubacao, String ajusteTratos) {}

    // Conexao com o servidor, injetada pelo Spring.
    @Autowired
    private ConexaoComServidor servidor;

    // POST /api/plantios -> cadastra um novo plantio para o usuario.
    @PostMapping("/plantios")
    public ResponseEntity<Map<String, Object>> cadastrar(@RequestBody PlantioRequest req)
    {
        try
        {
            // Codigo de usuario ausente vira 0; o servidor decide se isso e invalido.
            int usuario = req.usuarioCodigo() == null ? 0 : req.usuarioCodigo();

            // Monta o pedido, envia ao servidor e espera a resposta.
            Comunicado resposta = servidor.enviarEReceber(new PedidoDeCadastroDePlantio(
                usuario, req.cultura(), req.nome(), req.safra(), req.descricao(),
                req.sistemaCultivo(), req.talhao(), req.areaPlantada(), req.localizacao(),
                req.altitude(), req.dataPlantio(), req.metodoPlantio(),
                req.ajustePlantio(), req.ajusteIrrigacao(), req.ajusteAdubacao(), req.ajusteTratos()));

            // Converte o Comunicado generico para o tipo de resposta especifico.
            RespostaDeCadastroDePlantio r = (RespostaDeCadastroDePlantio) resposta;

            // Monta o JSON de retorno (LinkedHashMap mantem a ordem dos campos).
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            if (r.isSucesso())
                corpo.put("codigo", r.getCodigo());   // codigo do plantio criado

            // 201 se criou, 400 se o servidor recusou (ex.: dados invalidos).
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            // Falha de conexao ou resposta inesperada: devolve 503 com mensagem amigavel.
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", false);
            corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
        }
    }

    // GET /api/plantios?usuarioCodigo=X -> lista os plantios do usuario.
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
                corpo.put("plantios", r.getPlantios());   // lista so vai se deu certo

            // 200 se listou, 400 se o servidor recusou o pedido.
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            // Mesmo tratamento de erro do metodo cadastrar.
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", false);
            corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
        }
    }
}