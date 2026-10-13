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
@CrossOrigin // permite abrir o front por outra porta (ex.: Live Server) durante o desenvolvimento
public class AuthController
{
    public record CadastroRequest(String nome, String email, String cpf, String telefone, String senha) {}
    public record LoginRequest(String email, String senha) {}

    @Autowired
    private ConexaoComServidor servidor;

    @PostMapping("/cadastro")
    public ResponseEntity<Map<String, Object>> cadastrar(@RequestBody CadastroRequest req)
    {
        try
        {
            Comunicado resposta = servidor.enviarEReceber(
                new PedidoDeCadastro(req.nome(), req.email(), req.cpf(), req.telefone(), req.senha()));

            RespostaDeCadastro r = (RespostaDeCadastro) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST).body(corpo);
        }
        catch (Exception e)
        {
            return indisponivel();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> entrar(@RequestBody LoginRequest req)
    {
        try
        {
            Comunicado resposta = servidor.enviarEReceber(new PedidoDeLogin(req.email(), req.senha()));

            RespostaDeLogin r = (RespostaDeLogin) resposta;
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sucesso", r.isSucesso());
            corpo.put("mensagem", r.getMensagem());
            if (r.isSucesso())
            {
                Map<String, Object> usuario = new LinkedHashMap<>();
                usuario.put("codigo", r.getCodigo());
                usuario.put("nome", r.getNome());
                usuario.put("email", r.getEmail());
                corpo.put("usuario", usuario);
            }
            return ResponseEntity.status(r.isSucesso() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED).body(corpo);
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
