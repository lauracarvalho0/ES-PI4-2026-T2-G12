package agromonitor.servidor.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.client.model.Filters;

import agromonitor.servidor.BancoDeTeste;
import agromonitor.servidor.modelo.Perfil;
import agromonitor.servidor.modelo.Usuario;

class UsuarioDAOTest
{
    private BancoDeTeste bd;
    private UsuarioDAO   dao;

    @BeforeEach
    void preparar()
    {
        bd  = new BancoDeTeste();
        dao = new UsuarioDAO(bd.banco());
        dao.garantirIndices();
    }

    @AfterEach
    void limpar() { bd.close(); }

    private int novo(String email) throws Exception
    {
        return dao.inserir("Maria Silva", email, "19999990000", "52998224725", "hash123");
    }

    private Document doc(int codigo)
    {
        return bd.banco().getCollection("usuarios").find(Filters.eq("codigo", codigo)).first();
    }

    // ---------- inserir ----------
    @Test
    void codigosSaoSequenciais() throws Exception
    {
        assertEquals(1, novo("a@x.com"));
        assertEquals(2, novo("b@x.com"));
        assertEquals(3, novo("c@x.com"));
    }

    @Test
    void inserirGravaValoresPadraoDoDicionario() throws Exception
    {
        int c = novo("a@x.com");
        Document d = doc(c);

        assertEquals("Maria Silva", d.getString("nome"));
        assertEquals("a@x.com", d.getString("email"));
        assertEquals("hash123", d.getString("senhaHash"));
        assertEquals("(GMT-03:00) Brasília", d.getString("fusoHorario"));
        assertEquals("Português", d.getString("idioma"));
        assertTrue(d.getBoolean("receberAlertasPorEmail"));
        assertTrue(d.getBoolean("ativo"));
        assertEquals("CPF", ((Document) d.get("documento")).getString("tipo"));
        assertEquals("52998224725", ((Document) d.get("documento")).getString("valor"));
        assertEquals("Gratuito", ((Document) d.get("assinaturaAtiva")).getString("plano"));
    }

    @Test
    void inserirSemCpfGravaDocumentoNulo() throws Exception
    {
        int c = dao.inserir("Joao", "j@x.com", null, null, "h");
        assertNull(doc(c).get("documento"));
    }

    @Test
    void inserirCriaHistoricoDeAssinatura() throws Exception
    {
        int c = novo("a@x.com");
        Document h = bd.banco().getCollection("assinaturas_historico")
                       .find(Filters.eq("usuarioCodigo", c)).first();
        assertNotNull(h);
        assertEquals("ATIVA", h.getString("status"));
        assertEquals("Gratuito", h.getString("plano"));
    }

    @Test
    void emailDuplicadoLancaExcecao() throws Exception
    {
        novo("a@x.com");
        assertThrows(UsuarioDAO.EmailJaCadastradoException.class, () -> novo("a@x.com"));
    }

    // ---------- buscas ----------
    @Test
    void buscarAtivoPorEmail() throws Exception
    {
        int c = novo("a@x.com");
        Usuario u = dao.buscarAtivoPorEmail("a@x.com");
        assertNotNull(u);
        assertEquals(c, u.getCodigo());
        assertEquals("hash123", u.getSenhaHash());
        assertNull(dao.buscarAtivoPorEmail("naoexiste@x.com"));
    }

    @Test
    void existeAtivoPorCodigo() throws Exception
    {
        int c = novo("a@x.com");
        assertTrue(dao.existeAtivoPorCodigo(c));
        assertFalse(dao.existeAtivoPorCodigo(999));
    }

    // ---------- recuperacao de senha ----------
    @Test
    void tokenDeRecuperacaoEGuardadoEDevolveUsuario() throws Exception
    {
        int c = novo("a@x.com");
        Date agora = new Date();
        Usuario u = dao.salvarTokenDeRecuperacao("a@x.com", "hashDoToken",
                new Date(agora.getTime() + 60_000), new Date(agora.getTime() - 60_000));

        assertNotNull(u);
        assertEquals(c, u.getCodigo());
        assertEquals("hashDoToken", ((Document) doc(c).get("recuperacaoSenha")).getString("tokenHash"));
    }

    @Test
    void tokenParaEmailInexistenteDevolveNull()
    {
        Date agora = new Date();
        assertNull(dao.salvarTokenDeRecuperacao("x@x.com", "h",
                new Date(agora.getTime() + 60_000), new Date(agora.getTime() - 60_000)));
    }

    @Test
    void cooldownImpedePedidosSeguidos() throws Exception
    {
        novo("a@x.com");
        Date agora = new Date();
        Date expira = new Date(agora.getTime() + 60_000);
        Date limite = new Date(agora.getTime() - 60_000); // so aceita novo pedido se o anterior for mais velho que isso

        assertNotNull(dao.salvarTokenDeRecuperacao("a@x.com", "h1", expira, limite));
        assertNull(dao.salvarTokenDeRecuperacao("a@x.com", "h2", expira, limite));

        // passado o cooldown (limite no futuro), o novo pedido e aceito
        assertNotNull(dao.salvarTokenDeRecuperacao("a@x.com", "h3", expira, new Date(agora.getTime() + 60_000)));
    }

    @Test
    void redefinirSenhaComTokenValidoTrocaESoVaiUmaVez() throws Exception
    {
        int c = novo("a@x.com");
        Date agora = new Date();
        dao.salvarTokenDeRecuperacao("a@x.com", "tk", new Date(agora.getTime() + 60_000), new Date(agora.getTime() - 60_000));

        assertTrue(dao.redefinirSenha("tk", "novoHash"));
        assertEquals("novoHash", dao.buscarSenhaHash(c));
        assertNull(doc(c).get("recuperacaoSenha"));

        assertFalse(dao.redefinirSenha("tk", "outroHash")); // uso unico
        assertEquals("novoHash", dao.buscarSenhaHash(c));
    }

    @Test
    void redefinirSenhaComTokenExpiradoOuErradoFalha() throws Exception
    {
        int c = novo("a@x.com");
        Date agora = new Date();
        dao.salvarTokenDeRecuperacao("a@x.com", "tk", new Date(agora.getTime() - 1000), new Date(agora.getTime() - 60_000));

        assertFalse(dao.redefinirSenha("tk", "novoHash"));      // expirado
        assertFalse(dao.redefinirSenha("errado", "novoHash"));  // inexistente
        assertEquals("hash123", dao.buscarSenhaHash(c));
    }

    // ---------- tela Configuracoes ----------
    @Test
    void buscarPerfil() throws Exception
    {
        int c = novo("a@x.com");
        Perfil p = dao.buscarPerfil(c);
        assertEquals("Maria Silva", p.getNome());
        assertEquals("a@x.com", p.getEmail());
        assertEquals("19999990000", p.getTelefone());
        assertEquals("Português", p.getIdioma());
        assertTrue(p.isReceberAlertasPorEmail());
        assertNull(dao.buscarPerfil(999));
    }

    @Test
    void atualizarPerfil() throws Exception
    {
        int c = novo("a@x.com");
        assertTrue(dao.atualizarPerfil(c, "Maria Souza", "novo@x.com", null));

        Perfil p = dao.buscarPerfil(c);
        assertEquals("Maria Souza", p.getNome());
        assertEquals("novo@x.com", p.getEmail());
        assertNull(p.getTelefone());
        assertFalse(dao.atualizarPerfil(999, "x", "y@x.com", null));
    }

    @Test
    void atualizarPerfilComEmailDeOutroUsuarioLancaExcecao() throws Exception
    {
        novo("a@x.com");
        int c2 = novo("b@x.com");
        assertThrows(UsuarioDAO.EmailJaCadastradoException.class,
                () -> dao.atualizarPerfil(c2, "Maria", "a@x.com", null));
    }

    @Test
    void atualizarPreferencias() throws Exception
    {
        int c = novo("a@x.com");
        assertTrue(dao.atualizarPreferencias(c, "(GMT-04:00) Manaus", "English", false));

        Perfil p = dao.buscarPerfil(c);
        assertEquals("(GMT-04:00) Manaus", p.getFusoHorario());
        assertEquals("English", p.getIdioma());
        assertFalse(p.isReceberAlertasPorEmail());
        assertFalse(dao.atualizarPreferencias(999, "a", "b", true));
    }

    @Test
    void emailParaAlertaRespeitaPreferencia() throws Exception
    {
        int c = novo("a@x.com");
        assertEquals("a@x.com", dao.emailParaAlerta(c));

        dao.atualizarPreferencias(c, "(GMT-03:00) Brasília", "Português", false);
        assertNull(dao.emailParaAlerta(c));

        assertNull(dao.emailParaAlerta(999));
    }

    @Test
    void alterarSenhaTambemInvalidaLinkPendente() throws Exception
    {
        int c = novo("a@x.com");
        Date agora = new Date();
        dao.salvarTokenDeRecuperacao("a@x.com", "tk", new Date(agora.getTime() + 60_000), new Date(agora.getTime() - 60_000));

        assertTrue(dao.alterarSenha(c, "hashNovo"));
        assertEquals("hashNovo", dao.buscarSenhaHash(c));
        assertFalse(dao.redefinirSenha("tk", "invasor"));
        assertFalse(dao.alterarSenha(999, "x"));
    }

    // ---------- exclusao logica ----------
    @Test
    void desativarFazExclusaoLogicaECancelaAssinatura() throws Exception
    {
        int c = novo("a@x.com");
        assertTrue(dao.desativar(c));

        assertFalse(doc(c).getBoolean("ativo"));                 // o registro continua no banco
        assertEquals("CANCELADA", ((Document) doc(c).get("assinaturaAtiva")).getString("status"));
        assertNull(dao.buscarAtivoPorEmail("a@x.com"));
        assertFalse(dao.existeAtivoPorCodigo(c));
        assertNull(dao.buscarPerfil(c));

        Document h = bd.banco().getCollection("assinaturas_historico").find(Filters.eq("usuarioCodigo", c)).first();
        assertEquals("CANCELADA", h.getString("status"));
        assertNotNull(h.getDate("dataFim"));

        assertFalse(dao.desativar(c)); // ja estava inativo
    }
}
