package agromonitor.servidor;

import java.util.Date;

import org.bson.Document;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;

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
}
