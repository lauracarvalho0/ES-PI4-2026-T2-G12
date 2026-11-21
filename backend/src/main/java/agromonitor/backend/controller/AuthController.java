// Giovana Uchelli - 25008818

package agromonitor.backend.controller; // Pasta (pacote) onde a classe fica no projeto

import java.util.*; //List, map, linkedHashMap

import org.springframework.beans.factory.annotation.Autowired; // Permite o spring injetar objetos prontos automaticamente
import org.springframework.http.*; // Montar a resposta http (status 200, 400, 401)
import org.springframework.web.bind.annotation.*; // Anotações para criar a API (@RestController, @PostMapping...)

import agromonitor.backend.ConexaoComServidor; // Conexão do backend com o servidor

// Pedidos e respostas de auth + tipo que é enviado
import agromonitor.comum.auth.*; 
import agromonitor.comum.Comunicado;


@RestController // Indica que a classe responde a requisições da  API
@RequestMapping("/api") // Todas as rotas começam assim
@CrossOrigin // permite abrir o front por outra porta (ex.: Live Server) durante o desenvolvimento

public class AuthController
{
    // Classes que apenas guardam dados 
    public record CadastroRequest(String nome, String email, String cpf, String telefone, String senha) {}
    public record LoginRequest(String email, String senha) {}
    public record RecuperarRequest(String email, String urlBase) {}
    public record RedefinirRequest(String token, String senha) {}

    @Autowired
    private ConexaoComServidor servidor;

    // Rota de cadastro: POST /api/cadastro
    @PostMapping("/cadastro")
    public ResponseEntity<Map<String, Object>> cadastrar(@RequestBody CadastroRequest req)
    {
        try
        {
            Comunicado resposta = servidor.enviarEReceber(
                new PedidoDeCadastro(req.nome(), req.email(), req.cpf(), req.telefone(), req.senha()));

            RespostaDeCadastro r = (RespostaDeCadastro) resposta;

            // Monta o JSON que volta para o front
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());

            // 201 se deu certo, 400 se deu erro
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            return indisponivel();
        }
    }

    // Rota de login: POST /api/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> entrar(@RequestBody LoginRequest req)
    {
        try
        {
            Comunicado resposta = servidor.enviarEReceber(new PedidoDeLogin(req.email(), req.senha()));

            RespostaDeLogin r = (RespostaDeLogin) resposta;

            //Monta o JSON que volta para o front
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());

            // Se o login deu certo, devolve os dados do usuario.
            if (r.isSucesso())
            {
                Map<String, Object> usuario = new LinkedHashMap<>();
                usuario.put("codigo", r.getCodigo());
                usuario.put("nome", r.getNome());
                usuario.put("email", r.getEmail());
                corpo.put("usuario", usuario);
            }

            // 200 se deu certo, 401 se nao autorizado
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED).body(corpo);
        }
        catch (Exception e)
        {
            return indisponivel();
        }
    }

    // "Esqueci minha senha": o servidor gera o token e envia o e-mail com o link.
    @PostMapping("/recuperar-senha")
    public ResponseEntity<Map<String, Object>> recuperarSenha(@RequestBody RecuperarRequest req)
    {
        try
        {
            // Pede ao servidor para enviar o email de recuperação.
            Comunicado resposta = servidor.enviarEReceber(
                new PedidoDeRecuperacaoDeSenha(req.email(), req.urlBase()));

            RespostaDeRecuperacaoDeSenha r = (RespostaDeRecuperacaoDeSenha) resposta;

            // Monta o json que volta para o front
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());

            // 200 se deu certo, 400 se deu erro
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            return indisponivel();
        }
    }

    // Tela "nova senha": o servidor confere o token e grava a senha nova.
    @PostMapping("/redefinir-senha")
    public ResponseEntity<Map<String, Object>> redefinirSenha(@RequestBody RedefinirRequest req)
    {
        try
        {
            Comunicado resposta = servidor.enviarEReceber(
                new PedidoDeRedefinicaoDeSenha(req.token(), req.senha()));

            RespostaDeRedefinicaoDeSenha r = (RespostaDeRedefinicaoDeSenha) resposta;

            // Monta o json que volta para o front
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

    // Resposta padrão (503) quando o servidor está fora do ar.
    private ResponseEntity<Map<String, Object>> indisponivel()
    {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sucesso", false);
        corpo.put("mensagem", "Servidor indisponivel. Tente novamente mais tarde.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo);
    }
}
