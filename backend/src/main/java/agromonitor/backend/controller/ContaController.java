// Giovana Uchelli - 25008818

package agromonitor.backend.controller; // Pasta (pacote) onde a classe fica no projeto

import java.util.*; // List, Map, LinkedHashMap

import org.springframework.beans.factory.annotation.Autowired; // Permite o Spring injetar objetos prontos automaticamente
import org.springframework.http.*; // Respostas http (status 400, 200, 503 ...)
import org.springframework.web.bind.annotation.*; // Anotações para criar a API (@RestController, @PostMapping...)

import agromonitor.backend.ConexaoComServidor; // Conexão com o servidor

// Base de tudo que é enviado + pedidos e respostas da conta
import agromonitor.comum.Comunicado; 
import agromonitor.comum.conta.*;


@RestController // Responde a aquisições da API
@RequestMapping("/api/conta") // Todas as rotas começam com /api/conta
@CrossOrigin //Permite o front acessar de outra porta
public class ContaController
{
    // Modelos dos dados que chegam do front (json)
    public record PerfilRequest(Integer usuarioCodigo, String nome, String email, String telefone) {}
    public record PreferenciasRequest(Integer usuarioCodigo, Boolean receberAlertasPorEmail) {}
    public record SenhaRequest(Integer usuarioCodigo, String senhaAtual, String novaSenha) {}
    public record ExclusaoRequest(Integer usuarioCodigo, String senha) {}

    // Spring entrega a conexão com o servidor pronta para uso
    @Autowired
    private ConexaoComServidor servidor;

    // Rota para buscar os dados da conta: GET /api/conta
    @GetMapping
    public ResponseEntity<Map<String, Object>> buscar(@RequestParam(required = false) Integer usuarioCodigo)
    {
        try
        {
            // pede ao servidor
            Comunicado resposta = servidor.enviarEReceber(new PedidoDeBuscaDePerfil(codigo(usuarioCodigo)));

            RespostaDeBuscaDePerfil r = (RespostaDeBuscaDePerfil) resposta;

            // Monta o json que volta ao front
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());

            // se deu certo, devolve os dados da conta
            if (r.isSucesso())
            {
                Map<String, Object> conta = new LinkedHashMap<>();
                conta.put("nome", r.getNome());
                conta.put("email", r.getEmail());
                conta.put("telefone", r.getTelefone());
                conta.put("receberAlertasPorEmail", r.isReceberAlertasPorEmail());
                corpo.put("conta", conta);
            }

            // 200 se deu certo, 400 se deu erro
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            return indisponivel();
        }
    }

    // Rota para atualizar os dados do usuario: PUT /api/conta
    @PutMapping
    public ResponseEntity<Map<String, Object>> atualizarPerfil(@RequestBody PerfilRequest req)
    {
        // Envia o pedido de atualização ao servidor
        return enviar(new PedidoDeAtualizacaoDePerfil(
            codigo(req.usuarioCodigo()), req.nome(), req.email(), req.telefone()));
    }

    // Rota para atualizar as preferencias (alertas por email): PUT /api/conta/preferencias
    @PutMapping("/preferencias")
    public ResponseEntity<Map<String, Object>> atualizarPreferencias(@RequestBody PreferenciasRequest req)
    {
        // se nao vier valor, considera que quer receber alertas
        return enviar(new PedidoDeAtualizacaoDePreferencias(
            codigo(req.usuarioCodigo()),
            req.receberAlertasPorEmail() == null || req.receberAlertasPorEmail()));
    }

    // Rota para trocar a senha
    @PutMapping("/senha")
    public ResponseEntity<Map<String, Object>> alterarSenha(@RequestBody SenhaRequest req)
    {
        // Envia a senha atual e a nova ao servidor
        return enviar(new PedidoDeAlteracaoDeSenha(
            codigo(req.usuarioCodigo()), req.senhaAtual(), req.novaSenha()));
    }


    // Rota para excluir conta: POST /api/conta/excluir
    @PostMapping("/excluir")
    public ResponseEntity<Map<String, Object>> excluir(@RequestBody ExclusaoRequest req)
    {
        // Pede a senha para confirmar a exlusao
        return enviar(new PedidoDeExclusaoDeConta(codigo(req.usuarioCodigo()), req.senha()));
    }

    // Método auxiliar: envia o pedido ao servidor e monta a resposta
    private ResponseEntity<Map<String, Object>> enviar(Comunicado pedido)
    {
        try
        {
            RespostaDeConta r = (RespostaDeConta) servidor.enviarEReceber(pedido);

            // Monta a resposta para o front
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());

            // 200 se deu certo, 400 se deu erro.
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            return indisponivel();
        }
    }

    // Pega o codigo do usurio
    private static int codigo(Integer usuarioCodigo)
    {
        return usuarioCodigo == null ? 0 : usuarioCodigo;
    }

    // Resposta padrão (erro 503) quando o servidor está fora do ar.
    private ResponseEntity<Map<String, Object>> indisponivel()
    {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sucesso", false);
        corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
    }
}
