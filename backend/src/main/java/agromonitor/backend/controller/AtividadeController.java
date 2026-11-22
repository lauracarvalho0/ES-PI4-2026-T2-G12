// Laura Carvalho - 25014543

package agromonitor.backend.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import agromonitor.backend.ConexaoComServidor;
import agromonitor.comum.atividade.*;
import agromonitor.comum.Comunicado;

// Recebe JSON do front (fetch) e repassa ao servidor como objetos Comunicado.
// Nao valida regra de negocio: quem valida e grava no Mongo e o SERVIDOR.
@RestController
@RequestMapping("/api")   // todas as rotas daqui comecam com /api
@CrossOrigin              // libera o front (outra origem) para chamar esta API
public class AtividadeController
{
    // Corpo JSON esperado no cadastro de uma atividade.
    // Integer (e nao int) permite receber null quando o campo nao vem no JSON.
    public record AtividadeRequest(Integer usuarioCodigo, Integer plantioCodigo, String tipoAtividade,
                                   String status, String dataExecucao, String quantidade,
                                   String responsavel, String observacoes) {}

    // Corpo JSON esperado ao concluir uma atividade ja existente.
    public record ConclusaoRequest(Integer usuarioCodigo, Integer plantioCodigo, Integer atividadeCodigo) {}

    // Conexao com o servidor, injetada pelo Spring.
    @Autowired
    private ConexaoComServidor servidor;

    // POST /api/atividades -> registra uma nova atividade em um plantio.
    @PostMapping("/atividades")
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody AtividadeRequest req)
    {
        try
        {
            // Campos numericos ausentes viram 0; o servidor decide se isso e invalido.
            int usuario = req.usuarioCodigo() == null ? 0 : req.usuarioCodigo();
            int plantio = req.plantioCodigo() == null ? 0 : req.plantioCodigo();

            // Monta o pedido, envia ao servidor e espera a resposta.
            Comunicado resposta = servidor.enviarEReceber(new PedidoDeRegistroDeAtividade(
                usuario, plantio, req.tipoAtividade(), req.status(), req.dataExecucao(),
                req.quantidade(), req.responsavel(), req.observacoes()));

            // Converte o Comunicado generico para o tipo de resposta especifico.
            RespostaDeRegistroDeAtividade r = (RespostaDeRegistroDeAtividade) resposta;

            // Monta o JSON de retorno (LinkedHashMap mantem a ordem dos campos).
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            if (r.isSucesso())
                corpo.put("codigo", r.getCodigo());   // codigo da atividade criada

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

    // GET /api/atividades?usuarioCodigo=X -> lista as atividades do usuario.
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
                corpo.put("atividades", r.getAtividades());   // lista so vai se deu certo

            // 200 se listou, 400 se o servidor recusou o pedido.
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            // Mesmo tratamento de erro do metodo registrar.
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", false);
            corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
        }
    }

    // POST /api/atividades/concluir -> marca uma atividade existente como concluida.
    @PostMapping("/atividades/concluir")
    public ResponseEntity<Map<String, Object>> concluir(@RequestBody ConclusaoRequest req)
    {
        try
        {
            // Identificam de quem e o plantio e qual atividade sera concluida.
            int usuario   = req.usuarioCodigo()   == null ? 0 : req.usuarioCodigo();
            int plantio   = req.plantioCodigo()   == null ? 0 : req.plantioCodigo();
            int atividade = req.atividadeCodigo() == null ? 0 : req.atividadeCodigo();

            Comunicado resposta = servidor.enviarEReceber(new PedidoDeConclusaoDeAtividade(usuario, plantio, atividade));

            RespostaDeConclusaoDeAtividade r = (RespostaDeConclusaoDeAtividade) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());

            // 200 se concluiu, 400 se o servidor recusou (ex.: atividade inexistente).
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            // Mesmo tratamento de erro dos outros metodos.
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", false);
            corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
        }
    }
}