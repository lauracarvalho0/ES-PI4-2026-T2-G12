package agromonitor.servidor.dao;

import java.util.Date;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import agromonitor.servidor.modelo.Perfil;
import agromonitor.servidor.modelo.Usuario;

// Unica classe que fala com o MongoDB para usuarios.
// As colecoes sao criadas automaticamente no primeiro insert.
public class UsuarioDAO
{
    public static class EmailJaCadastradoException extends Exception
    {
        public EmailJaCadastradoException() { super("E-mail ja cadastrado"); }
    }

    private final MongoDatabase banco;

    public UsuarioDAO(MongoDatabase banco)
    {
        this.banco = banco;
    }

    private MongoCollection<Document> usuarios() { return banco.getCollection("usuarios"); }

    // Chamado uma vez na inicializacao do servidor (pode rodar varias vezes sem problema).
    public void garantirIndices()
    {
        usuarios().createIndex(Indexes.ascending("email"),  new IndexOptions().unique(true));
        usuarios().createIndex(Indexes.ascending("codigo"), new IndexOptions().unique(true));
        usuarios().createIndex(Indexes.ascending("recuperacaoSenha.tokenHash"));
    }

    // Gera codigos sequenciais (1, 2, 3...) usando uma colecao de contadores.
    private int proximoCodigo()
    {
        Document d = banco.getCollection("contadores").findOneAndUpdate(
            Filters.eq("_id", "usuarios"),
            Updates.inc("seq", 1),
            new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        return d.getInteger("seq");
    }

    // Insere o usuario no formato do dicionario de dados e devolve o codigo gerado.
    public int inserir(String nome, String email, String telefone, String cpfSomenteDigitos, String senhaHash)
        throws EmailJaCadastradoException
    {
        Date agora   = new Date();
        int  codigo  = proximoCodigo();

        Document doc = new Document("codigo", codigo)
            .append("nome", nome)
            .append("email", email)
            .append("telefone", telefone)
            .append("documento", cpfSomenteDigitos == null ? null
                    : new Document("tipo", "CPF").append("valor", cpfSomenteDigitos))
            .append("senhaHash", senhaHash)
            .append("fusoHorario", "(GMT-03:00) Brasília")
            .append("idioma", "Português")
            .append("receberAlertasPorEmail", true)
            .append("ativo", true)
            .append("dataCriacao", agora)
            // dicionario: a colecao usuarios embute a assinatura ativa
            .append("assinaturaAtiva", new Document("plano", "Gratuito")
                    .append("dataInicio", agora)
                    .append("status", "ATIVA"));

        try
        {
            usuarios().insertOne(doc);
        }
        catch (MongoWriteException erro)
        {
            if (erro.getError().getCategory() == ErrorCategory.DUPLICATE_KEY)
                throw new EmailJaCadastradoException();
            throw erro;
        }

        // historico de assinaturas fica em colecao propria
        banco.getCollection("assinaturas_historico").insertOne(
            new Document("usuarioCodigo", codigo)
                .append("plano", "Gratuito")
                .append("dataInicio", agora)
                .append("dataFim", null)
                .append("status", "ATIVA")
                .append("formaPagamento", null));

        return codigo;
    }

    // Diz se existe usuario ativo com esse codigo (usado ao cadastrar plantio).
    public boolean existeAtivoPorCodigo(int codigo)
    {
        return usuarios().find(Filters.and(
                Filters.eq("codigo", codigo),
                Filters.eq("ativo", true))).first() != null;
    }

    // Guarda (o hash do) token de recuperacao no usuario ativo com esse e-mail.
    // Devolve o usuario, ou null se o e-mail nao existe OU se ja foi pedido um link ha menos
    // de "cooldownLimite" (evita encher a caixa de e-mail de alguem). Quem chama trata os dois
    // casos do mesmo jeito, para nao revelar se o e-mail esta cadastrado.
    public Usuario salvarTokenDeRecuperacao(String email, String tokenHash, Date expiraEm, Date cooldownLimite)
    {
        Document d = usuarios().findOneAndUpdate(
            Filters.and(
                Filters.eq("email", email),
                Filters.eq("ativo", true),
                Filters.or(
                    Filters.exists("recuperacaoSenha.criadoEm", false),
                    Filters.lt("recuperacaoSenha.criadoEm", cooldownLimite))),
            Updates.set("recuperacaoSenha", new Document("tokenHash", tokenHash)
                    .append("expiraEm", expiraEm)
                    .append("criadoEm", new Date())),
            new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

        if (d == null)
            return null;
        return new Usuario(d.getInteger("codigo"), d.getString("nome"),
                           d.getString("email"), d.getString("senhaHash"));
    }

    // Troca a senha se o token existe, nao expirou e o usuario esta ativo.
    // Uma unica operacao atomica: troca a senha E apaga o token, entao o link so vale uma vez.
    public boolean redefinirSenha(String tokenHash, String novoSenhaHash)
    {
        Document d = usuarios().findOneAndUpdate(
            Filters.and(
                Filters.eq("recuperacaoSenha.tokenHash", tokenHash),
                Filters.gt("recuperacaoSenha.expiraEm", new Date()),
                Filters.eq("ativo", true)),
            Updates.combine(
                Updates.set("senhaHash", novoSenhaHash),
                Updates.unset("recuperacaoSenha")));
        return d != null;
    }

    // Devolve o usuario ativo com esse e-mail, ou null.
    public Usuario buscarAtivoPorEmail(String email)
    {
        Document d = usuarios().find(Filters.and(
                Filters.eq("email", email),
                Filters.eq("ativo", true))).first();
        if (d == null)
            return null;
        return new Usuario(d.getInteger("codigo"), d.getString("nome"),
                           d.getString("email"), d.getString("senhaHash"));
    }

    // ---------- tela Configuracoes ----------

    private Bson ativoComCodigo(int codigo)
    {
        return Filters.and(Filters.eq("codigo", codigo), Filters.eq("ativo", true));
    }

    // Dados da conta do usuario ativo, ou null.
    public Perfil buscarPerfil(int codigo)
    {
        Document d = usuarios().find(ativoComCodigo(codigo)).first();
        if (d == null)
            return null;
        return new Perfil(d.getString("nome"), d.getString("email"), d.getString("telefone"),
                          d.getString("fusoHorario"), d.getString("idioma"),
                          d.getBoolean("receberAlertasPorEmail", true));
    }

    // Altera nome, e-mail e telefone. Devolve false se o usuario nao existe/esta inativo.
    public boolean atualizarPerfil(int codigo, String nome, String email, String telefone)
        throws EmailJaCadastradoException
    {
        try
        {
            return usuarios().updateOne(ativoComCodigo(codigo), Updates.combine(
                    Updates.set("nome", nome),
                    Updates.set("email", email),
                    Updates.set("telefone", telefone))).getMatchedCount() > 0;
        }
        catch (MongoWriteException erro)
        {
            if (erro.getError().getCategory() == ErrorCategory.DUPLICATE_KEY)
                throw new EmailJaCadastradoException();
            throw erro;
        }
    }

    public boolean atualizarPreferencias(int codigo, String fusoHorario, String idioma, boolean alertasPorEmail)
    {
        return usuarios().updateOne(ativoComCodigo(codigo), Updates.combine(
                Updates.set("fusoHorario", fusoHorario),
                Updates.set("idioma", idioma),
                Updates.set("receberAlertasPorEmail", alertasPorEmail)))
            .getMatchedCount() > 0;
    }

    // E-mail so e devolvido quando a conta esta ativa e autorizou alertas por e-mail.
    public String emailParaAlerta(int codigo)
    {
        Document d = usuarios().find(ativoComCodigo(codigo))
            .projection(Projections.include("email", "receberAlertasPorEmail"))
            .first();
        if (d == null)
            return null;

        if (!d.getBoolean("receberAlertasPorEmail", true))
            return null;
        return d.getString("email");
    }

    // Hash da senha do usuario ativo, ou null.
    public String buscarSenhaHash(int codigo)
    {
        Document d = usuarios().find(ativoComCodigo(codigo)).first();
        return d == null ? null : d.getString("senhaHash");
    }

    // Troca a senha e invalida qualquer link de recuperacao pendente.
    public boolean alterarSenha(int codigo, String novoSenhaHash)
    {
        return usuarios().updateOne(ativoComCodigo(codigo), Updates.combine(
                Updates.set("senhaHash", novoSenhaHash),
                Updates.unset("recuperacaoSenha"))).getMatchedCount() > 0;
    }

    // Exclusao logica (dicionario: "ativo" fica falso) e encerra a assinatura vigente.
    public boolean desativar(int codigo)
    {
        Date agora = new Date();
        boolean ok = usuarios().updateOne(ativoComCodigo(codigo), Updates.combine(
                Updates.set("ativo", false),
                Updates.unset("recuperacaoSenha"),
                Updates.set("assinaturaAtiva.status", "CANCELADA"))).getMatchedCount() > 0;
        if (ok)
            banco.getCollection("assinaturas_historico").updateMany(
                Filters.and(Filters.eq("usuarioCodigo", codigo), Filters.eq("status", "ATIVA")),
                Updates.combine(Updates.set("status", "CANCELADA"), Updates.set("dataFim", agora)));
        return ok;
    }
}
