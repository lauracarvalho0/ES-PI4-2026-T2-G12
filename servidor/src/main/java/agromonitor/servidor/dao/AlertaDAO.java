package agromonitor.servidor.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import agromonitor.comum.alerta.AlertaResumo;
import agromonitor.servidor.RegrasDeAlerta;

import org.bson.Document;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;

// Unica classe que fala com o MongoDB para a colecao alertas (dicionario de dados, entidade Alerta).
// As recomendacoes ficam embutidas no proprio documento do alerta.
// O front gera os riscos climaticos e esta classe evita e-mails duplicados por evento.
public class AlertaDAO
{
    public record AlertaEmail(int codigo, int usuarioCodigo, String titulo, String descricao,
                              String plantio, String data, String prioridade) {}

    private final MongoDatabase banco;

    public AlertaDAO(MongoDatabase banco)
    {
        this.banco = banco;
    }

    private MongoCollection<Document> alertas() { return banco.getCollection("alertas"); }

    // Chamado uma vez na inicializacao do servidor (pode rodar varias vezes sem problema).
    public void garantirIndices()
    {
        alertas().createIndex(Indexes.ascending("codigo"), new IndexOptions().unique(true));
        alertas().createIndex(Indexes.ascending("usuario.codigo", "situacao"));
        // garante que nunca saem dois alertas iguais (mesma atividade + mesmo motivo)
        alertas().createIndex(Indexes.ascending("atividade.codigo", "motivo"), new IndexOptions().unique(true));
        banco.getCollection("alertas_climaticos_email").createIndex(
            Indexes.ascending("chave"), new IndexOptions().unique(true));
    }

    // Mesma ideia do PlantioDAO: reserva codigos na colecao de contadores
    private int reservarCodigo(String colecao)
    {
        Document d = banco.getCollection("contadores").findOneAndUpdate(
            Filters.eq("_id", colecao),
            Updates.inc("seq", 1),
            new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        return d.getInteger("seq");
    }

    // Data do Mongo (meia-noite UTC) <-> LocalDate, como no PlantioDAO
    private static LocalDate paraLocalDate(Date data)
    {
        return data.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    // ---------- "vigia": cria os alertas que faltam ----------

    // Confere as atividades NAO concluidas dos plantios ativos (de usuarios ativos) e cria os alertas
    // que ainda nao existem. Devolve quantos alertas novos foram criados.
    public int gerarAlertasDeAtividades(LocalDate hoje)
    {
        Set<Integer> usuariosAtivos = new HashSet<Integer>();
        for (Document u : banco.getCollection("usuarios").find(Filters.eq("ativo", true))
                               .projection(Projections.include("codigo")))
            usuariosAtivos.add(u.getInteger("codigo"));

        int criados = 0;

        for (Document plantio : banco.getCollection("plantios").find(Filters.eq("ativo", true)))
        {
            int usuarioCodigo = ((Document) plantio.get("usuario")).getInteger("codigo");
            if (!usuariosAtivos.contains(usuarioCodigo))
                continue;

            for (Document atividade : plantio.getList("atividades", Document.class, new ArrayList<Document>()))
            {
                if ("CONCLUIDA".equals(atividade.getString("status")))
                    continue;

                Date dataPrevista = atividade.getDate("dataPrevista");
                RegrasDeAlerta.Classificacao c = RegrasDeAlerta.classificar(hoje, paraLocalDate(dataPrevista));
                if (c == null)
                    continue;

                int atividadeCodigo = atividade.getInteger("codigo");

                // ja existe? (so confere antes de reservar codigo, para nao gastar numeracao toda hora)
                if (alertas().countDocuments(Filters.and(
                        Filters.eq("atividade.codigo", atividadeCodigo),
                        Filters.eq("motivo", c.motivo()))) > 0)
                    continue;

                if (inserir(usuarioCodigo, plantio, atividade, dataPrevista, c))
                    criados++;
            }
        }

        // atraso de 3 dias ou mais sobe de ALTA para CRITICO enquanto o alerta nao foi lido
        alertas().updateMany(
            Filters.and(
                Filters.eq("motivo", RegrasDeAlerta.ATRASADA),
                Filters.eq("prioridade", "ALTA"),
                Filters.eq("situacao", "NAO_LIDO"),
                Filters.lte("dataAtividade",
                    Date.from(hoje.minusDays(RegrasDeAlerta.DIAS_CRITICO).atStartOfDay(ZoneOffset.UTC).toInstant()))),
            Updates.set("prioridade", "CRITICO"));

        return criados;
    }

    private boolean inserir(int usuarioCodigo, Document plantio, Document atividade, Date dataPrevista,
                            RegrasDeAlerta.Classificacao c)
    {
        LocalDate data      = paraLocalDate(dataPrevista);
        String    nomeTipo  = RegrasDeAlerta.nomeDoTipo(atividade.getString("tipoAtividade"));
        String    nomePlant = plantio.getString("nome");

        Document alerta = new Document("codigo", reservarCodigo("alertas"))
            .append("usuario", new Document("codigo", usuarioCodigo))
            .append("plantio", new Document("codigo", plantio.getInteger("codigo"))
                                   .append("nome", nomePlant)
                                   .append("cultura", ((Document) plantio.get("cultura")).getString("nome")))
            .append("talhao", new Document("nome", ((Document) plantio.get("talhao")).getString("nome")))
            .append("atividade", new Document("codigo", atividade.getInteger("codigo"))
                                     .append("tipoAtividade", atividade.getString("tipoAtividade")))
            .append("motivo", c.motivo())
            .append("tipoAlerta", "ATIVIDADE")
            .append("titulo", c.titulo())
            .append("descricao", RegrasDeAlerta.descricao(c, nomeTipo, nomePlant, data))
            .append("prioridade", c.prioridade())
            .append("situacao", "NAO_LIDO")
            .append("dataAlerta", new Date())
            .append("dataAtividade", dataPrevista)
            .append("origem", "SISTEMA_MONITORAMENTO")
            .append("impactoAtividade", nomeTipo)
            .append("impactoDataLabel", RegrasDeAlerta.rotuloDaData(data))
            .append("recomendacoes", new ArrayList<String>(c.recomendacoes()))
            .append("emailEnviado", false);   // reservado para o envio de e-mail (ainda nao implementado)

        try
        {
            alertas().insertOne(alerta);
            return true;
        }
        catch (MongoWriteException erro)
        {
            // o indice unico recusou: outro alerta igual acabou de ser criado. Tudo bem, so ignora.
            if (erro.getError().getCategory() == ErrorCategory.DUPLICATE_KEY)
                return false;
            throw erro;
        }
    }

    // ---------- consultas e atualizacoes feitas pelo usuario ----------

    // Alertas do usuario, os mais recentes primeiro.
    public ArrayList<AlertaResumo> listarPorUsuario(int usuarioCodigo)
    {
        ArrayList<AlertaResumo> lista = new ArrayList<AlertaResumo>();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Document d : alertas().find(Filters.eq("usuario.codigo", usuarioCodigo))
                                   .sort(Sorts.descending("dataAlerta")))
        {
            Document plantio = (Document) d.get("plantio");

            lista.add(new AlertaResumo(
                d.getInteger("codigo"),
                plantio.getInteger("codigo"),
                plantio.getString("nome"),
                plantio.getString("cultura"),
                ((Document) d.get("talhao")).getString("nome"),
                d.getString("tipoAlerta"),
                d.getString("titulo"),
                d.getString("descricao"),
                d.getString("prioridade"),
                d.getString("situacao"),
                LocalDateTime.ofInstant(d.getDate("dataAlerta").toInstant(), ZoneId.systemDefault()).format(formato),
                d.getString("origem"),
                d.getString("impactoAtividade"),
                d.getString("impactoDataLabel"),
                paraLocalDate(d.getDate("dataAtividade")).toString(),
                new ArrayList<String>(d.getList("recomendacoes", String.class, new ArrayList<String>()))));
        }
        return lista;
    }

    // Botao "Marcar como lido". So mexe em alerta do proprio usuario. Devolve false se nao achou.
    public boolean marcarComoLido(int usuarioCodigo, int alertaCodigo)
    {
        return alertas().updateOne(
            Filters.and(Filters.eq("codigo", alertaCodigo), Filters.eq("usuario.codigo", usuarioCodigo)),
            Updates.set("situacao", "LIDO")).getMatchedCount() > 0;
    }

    // Quando a atividade e concluida, os alertas dela (se aproxima / atrasada) deixam de fazer sentido.
    public void marcarLidosDaAtividade(int atividadeCodigo)
    {
        alertas().updateMany(
            Filters.and(Filters.eq("atividade.codigo", atividadeCodigo), Filters.eq("situacao", "NAO_LIDO")),
            Updates.set("situacao", "LIDO"));
    }

    // Alertas de atividade novos aguardando processamento de e-mail.
    // So os ainda nao lidos e das ultimas 24h: assim, ao configurar o SMTP, alertas antigos ou
    // de atividades ja concluidas nao saem todos de uma vez por e-mail.
    public List<AlertaEmail> listarPendentesDeEmail()
    {
        List<AlertaEmail> lista = new ArrayList<>();
        Date limite = new Date(System.currentTimeMillis() - 24L * 60L * 60L * 1000L);
        for (Document d : alertas().find(Filters.and(
                Filters.eq("tipoAlerta", "ATIVIDADE"),
                Filters.eq("situacao", "NAO_LIDO"),
                Filters.gte("dataAlerta", limite),
                Filters.ne("emailEnviado", true))))
        {
            Document plantio = (Document) d.get("plantio");
            lista.add(new AlertaEmail(d.getInteger("codigo"),
                ((Document) d.get("usuario")).getInteger("codigo"), d.getString("titulo"),
                d.getString("descricao"), plantio.getString("nome"),
                paraLocalDate(d.getDate("dataAtividade")).toString(), d.getString("prioridade")));
        }
        return lista;
    }

    public void marcarEmailProcessado(int alertaCodigo)
    {
        alertas().updateOne(Filters.eq("codigo", alertaCodigo), Updates.set("emailEnviado", true));
    }

    // Registra cada evento climatico uma unica vez para evitar e-mails duplicados a cada visita.
    public boolean registrarAlertaClimaticoEmail(String chave, int usuarioCodigo, int plantioCodigo,
            String plantio, String titulo, String descricao, String prioridade, String data)
    {
        MongoCollection<Document> colecao = banco.getCollection("alertas_climaticos_email");
        if (colecao.countDocuments(Filters.eq("chave", chave)) > 0)
            return false;
        try
        {
            colecao.insertOne(new Document("chave", chave)
                .append("usuarioCodigo", usuarioCodigo).append("plantioCodigo", plantioCodigo)
                .append("plantio", plantio).append("titulo", titulo).append("descricao", descricao)
                .append("prioridade", prioridade).append("data", data).append("criadoEm", new Date())
                .append("emailProcessado", false));
            return true;
        }
        catch (MongoWriteException erro)
        {
            if (erro.getError().getCategory() == ErrorCategory.DUPLICATE_KEY)
            {
                Document existente = colecao.find(Filters.eq("chave", chave)).first();
                return existente != null && !existente.getBoolean("emailProcessado", false);
            }
            throw erro;
        }
    }

    public void marcarEmailClimaticoProcessado(String chave)
    {
        banco.getCollection("alertas_climaticos_email").updateOne(
            Filters.eq("chave", chave), Updates.set("emailProcessado", true));
    }
}
