package agromonitor.backend.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import agromonitor.backend.ConexaoComServidor;
import agromonitor.comum.Comunicado;
import agromonitor.comum.conta.*;

// Tela Configuracoes. Recebe JSON do front e repassa ao servidor como Comunicado.
// Nao valida regra de negocio: quem valida e grava no Mongo e o SERVIDOR.
@RestController
@RequestMapping("/api/conta")
@CrossOrigin
public class ContaController
{
    public record PerfilRequest(Integer usuarioCodigo, String nome, String email, String telefone) {}
    public record PreferenciasRequest(Integer usuarioCodigo, Boolean receberAlertasPorEmail) {}
    public record SenhaRequest(Integer usuarioCodigo, String senhaAtual, String novaSenha) {}
    public record ExclusaoRequest(Integer usuarioCodigo, String senha) {}

    @Autowired
    private ConexaoComServidor servidor;

    @GetMapping
    public ResponseEntity<Map<String, Object>> buscar(@RequestParam(required = false) Integer usuarioCodigo)
    {
        try
        {
            Comunicado resposta = servidor.enviarEReceber(new PedidoDeBuscaDePerfil(codigo(usuarioCodigo)));

            RespostaDeBuscaDePerfil r = (RespostaDeBuscaDePerfil) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            if (r.isSucesso())
            {
                Map<String, Object> conta = new LinkedHashMap<>();
                conta.put("nome", r.getNome());
                conta.put("email", r.getEmail());
                conta.put("telefone", r.getTelefone());
                conta.put("receberAlertasPorEmail", r.isReceberAlertasPorEmail());
                corpo.put("conta", conta);
            }
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            return indisponivel();
        }
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> atualizarPerfil(@RequestBody PerfilRequest req)
    {
        return enviar(new PedidoDeAtualizacaoDePerfil(
            codigo(req.usuarioCodigo()), req.nome(), req.email(), req.telefone()));
    }

    @PutMapping("/preferencias")
    public ResponseEntity<Map<String, Object>> atualizarPreferencias(@RequestBody PreferenciasRequest req)
    {
        return enviar(new PedidoDeAtualizacaoDePreferencias(
            codigo(req.usuarioCodigo()),
            req.receberAlertasPorEmail() == null || req.receberAlertasPorEmail()));
    }

    @PutMapping("/senha")
    public ResponseEntity<Map<String, Object>> alterarSenha(@RequestBody SenhaRequest req)
    {
        return enviar(new PedidoDeAlteracaoDeSenha(
            codigo(req.usuarioCodigo()), req.senhaAtual(), req.novaSenha()));
    }

    @PostMapping("/excluir")
    public ResponseEntity<Map<String, Object>> excluir(@RequestBody ExclusaoRequest req)
    {
        return enviar(new PedidoDeExclusaoDeConta(codigo(req.usuarioCodigo()), req.senha()));
    }

    private ResponseEntity<Map<String, Object>> enviar(Comunicado pedido)
    {
        try
        {
            RespostaDeConta r = (RespostaDeConta) servidor.enviarEReceber(pedido);
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

    private static int codigo(Integer usuarioCodigo)
    {
        return usuarioCodigo == null ? 0 : usuarioCodigo;
    }

    private ResponseEntity<Map<String, Object>> indisponivel()
    {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sucesso", false);
        corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
    }
}
