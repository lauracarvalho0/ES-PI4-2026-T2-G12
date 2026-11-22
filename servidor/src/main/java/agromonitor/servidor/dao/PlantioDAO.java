// Laura Carvalho - 25014543
package agromonitor.servidor.dao;

import java.time.LocalDate;  // Datas sem hora (plantio, atividades, "hoje")
import java.time.ZoneOffset; // Fuso UTC usado ao converter datas para o Mongo
import java.util.ArrayList;  // Listas de documentos e de resumos
import java.util.Collections;  // Collections.sort: ordena as atividades
import java.util.Comparator;  // Define o critério de ordenação (data e código)
import java.util.Date;  // Tipo de data que o MongoDB armazena
import java.util.List;  // Lista genérica de atividades

import agromonitor.comum.atividade.AtividadeResumo;  // Resumo da atividade enviado ao cliente
import agromonitor.comum.plantio.PlantioResumo; // Resumo do plantio enviado ao cliente

import org.bson.Document;  // Documento BSON: formato dos dados no Mongo

import com.mongodb.client.MongoCollection; // Representa uma coleção do banco
import com.mongodb.client.MongoDatabase;  // Representa o banco "agromonitor"
import com.mongodb.client.model.*; // Filters, Updates, Sorts, Indexes e opções de operação
import com.mongodb.client.result.UpdateResult; // Resultado de updateOne (ex.: getMatchedCount)

// Unica classe que fala com o MongoDB para plantios.
// Segue o dicionario de dados: a colecao plantios embute a lista de atividades.
public class PlantioDAO
{
    // cronograma padrao (dias apos a data de plantio), igual ao mostrado na tela
    // os dois vetores andam juntos: TIPOS[i] acontece OFFSETS[i] dias depois do plantio
    private static final String[] TIPOS   = { "PLANTIO", "IRRIGACAO", "ADUBACAO", "TRATAMENTO" };
    private static final int[]    OFFSETS = { 0, 7, 20, 35 };

    private final MongoDatabase banco;

    public PlantioDAO(MongoDatabase banco)
    {
        this.banco = banco;
    }

    // atalho pra pegar a colecao "plantios" sem repetir o nome em todo lugar
    private MongoCollection<Document> plantios() { return banco.getCollection("plantios"); }

    // Chamado uma vez na inicializacao do servidor (pode rodar varias vezes sem problema).
    public void garantirIndices()
    {
        // unique = nao deixa dois plantios com o mesmo codigo
        plantios().createIndex(Indexes.ascending("codigo"), new IndexOptions().unique(true));
        // indices nos campos mais usados nas buscas, pra elas ficarem rapidas
        plantios().createIndex(Indexes.ascending("usuario.codigo"));
        plantios().createIndex(Indexes.ascending("status"));
    }

    // Reserva "quantidade" codigos seguidos na colecao de contadores
    // e devolve o primeiro deles (ex.: reservou 4 e voltou 9 -> usa 9, 10, 11, 12).
    private int reservarCodigos(String colecao, int quantidade)
    {
        // findOneAndUpdate faz "soma e le" numa operacao so, entao dois usuarios nunca pegam o mesmo codigo
        // upsert = cria o contador se ainda nao existir; AFTER = devolve o valor ja somado
        Document d = banco.getCollection("contadores").findOneAndUpdate(
            Filters.eq("_id", colecao),
            Updates.inc("seq", quantidade),
            new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        return d.getInteger("seq") - quantidade + 1;
    }

    // Data sem hora vai para o Mongo como meia-noite UTC
    private static Date paraData(LocalDate data)
    {
        return Date.from(data.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    // Insere o plantio com as 4 atividades previstas e devolve o codigo gerado.
    // Cada ajuste e a data escolhida pelo usuario (ou null para usar a previsao automatica).
    public int inserir(int usuarioCodigo, String cultura, String nome, String safra, String descricao,
                       String sistemaCultivo, String talhao, double areaPlantada, String localizacao,
                       Integer altitude, LocalDate dataPlantio, String metodoPlantio,
                       LocalDate[] ajustes)
    {
        // Cria o plantio e deixa as atividades previstas embutidas no mesmo documento MongoDB.
        Date agora  = new Date();
        int  codigo = reservarCodigos("plantios", 1);
        // reserva os 4 codigos das atividades de uma vez
        int  primeiroCodigoAtividade = reservarCodigos("atividades", TIPOS.length);

        List<Document> atividades = new ArrayList<Document>();
        // Para cada etapa, usa a data personalizada ou calcula a previsão padrão em dias.
        for (int i = 0; i < TIPOS.length; i++)
        {
            LocalDate prevista = ajustes[i] != null ? ajustes[i] : dataPlantio.plusDays(OFFSETS[i]);

            // toda atividade nasce PENDENTE e sem data realizada
            atividades.add(new Document("codigo", primeiroCodigoAtividade + i)
                .append("tipoAtividade", TIPOS[i])
                .append("dataPrevista", paraData(prevista))
                .append("dataRealizada", null)
                .append("status", "PENDENTE")
                .append("observacoes", null)
                .append("ordem", i + 1));
        }

        // cultura, talhao e usuario sao guardados como sub-documentos (ex.: usuario.codigo)
        Document doc = new Document("codigo", codigo)
            .append("usuario", new Document("codigo", usuarioCodigo))
            .append("cultura", new Document("nome", cultura))
            .append("talhao", new Document("nome", talhao))
            .append("nome", nome)
            .append("safra", safra)
            .append("descricao", descricao)
            .append("sistemaCultivo", sistemaCultivo)
            .append("areaPlantada", areaPlantada)
            .append("localizacao", localizacao)
            .append("altitude", altitude)
            .append("dataPlantio", paraData(dataPlantio))
            .append("metodoPlantio", metodoPlantio)
            .append("status", "FASE_INICIAL")
            .append("progresso", 0)
            .append("ativo", true) // "ativo" = false seria uma exclusao logica (nao apaga do banco)
            .append("dataCriacao", agora)
            .append("atividades", atividades);

        plantios().insertOne(doc);
        return codigo;
    }

    // Data do Mongo (meia-noite UTC) de volta para texto "yyyy-MM-dd"
    private static String paraTexto(Date data)
    {
        return data.toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString();
    }

    // Plantios ativos do usuario, os mais recentes primeiro.
    // A "proxima atividade" e a pendente com a menor data prevista.
    public ArrayList<PlantioResumo> listarPorUsuario(int usuarioCodigo)
    {
        ArrayList<PlantioResumo> lista = new ArrayList<PlantioResumo>();

        // busca so os plantios desse usuario que estao ativos, do mais novo pro mais antigo
        for (Document d : plantios()
                .find(Filters.and(Filters.eq("usuario.codigo", usuarioCodigo), Filters.eq("ativo", true)))
                .sort(Sorts.descending("dataCriacao")))
        {
            List<Document> atividades = d.getList("atividades", Document.class, new ArrayList<Document>());

            // procura a atividade nao concluida com a menor data prevista
            String tipoProxima = null;
            Date   dataProxima = null;
            for (Document a : atividades)
            {
                if ("CONCLUIDA".equals(a.getString("status")))
                    continue;
                Date prevista = a.getDate("dataPrevista");
                if (dataProxima == null || prevista.before(dataProxima))
                {
                    dataProxima = prevista;
                    tipoProxima = a.getString("tipoAtividade");
                }
            }

            // monta o resumo que vai pro cliente (cultura e talhao vem de dentro dos sub-documentos)
            lista.add(new PlantioResumo(
                d.getInteger("codigo"),
                d.getString("nome"),
                ((Document) d.get("cultura")).getString("nome"),
                ((Document) d.get("talhao")).getString("nome"),
                d.getDouble("areaPlantada"),
                paraTexto(d.getDate("dataPlantio")),
                atividades.size(),
                tipoProxima,
                dataProxima == null ? null : paraTexto(dataProxima),
                calcularProgresso(atividades),
                calcularFase(calcularProgresso(atividades)),
                d.getString("localizacao")));
        }
        return lista;
    }

    // devolve o nome do plantio se ele for desse usuario e estiver ativo; senao, null
    public String nomeAtivoDoUsuario(int usuarioCodigo, int plantioCodigo)
    {
        Document d = plantios().find(Filters.and(
            Filters.eq("codigo", plantioCodigo),
            Filters.eq("usuario.codigo", usuarioCodigo),
            Filters.eq("ativo", true))).first();
        return d == null ? null : d.getString("nome");
    }

    // Registra uma atividade dentro do plantio. So funciona se o plantio for desse usuario
    // e estiver ativo. Devolve o codigo da atividade, ou 0 se o plantio nao foi encontrado.
    // Se concluida, a data e a "realizada" (e tambem a prevista); senao e so a prevista.
    public int registrarAtividade(int usuarioCodigo, int plantioCodigo, String tipo, String status,
                                  LocalDate data, String quantidade, String responsavel, String observacoes)
    {
        int codigo = reservarCodigos("atividades", 1);

        Document atividade = new Document("codigo", codigo)
            .append("tipoAtividade", tipo)
            .append("dataPrevista", paraData(data))
            .append("dataRealizada", status.equals("CONCLUIDA") ? paraData(data) : null)
            .append("status", status)
            .append("observacoes", observacoes)
            .append("quantidade", quantidade)
            .append("responsavel", responsavel);

        // push adiciona a atividade no fim da lista; o filtro garante que o plantio e desse usuario
        UpdateResult resultado = plantios().updateOne(
            Filters.and(
                Filters.eq("codigo", plantioCodigo),
                Filters.eq("usuario.codigo", usuarioCodigo),
                Filters.eq("ativo", true)),
            Updates.push("atividades", atividade));

        // nenhum documento encontrado: plantio inexistente, de outro usuario ou inativo
        if (resultado.getMatchedCount() == 0)
            return 0;

        atualizarProgresso(plantioCodigo); // a lista mudou, entao o progresso muda
        return codigo;
    }

    // ---------- progresso e fase do plantio (saem das atividades) ----------

    // progresso = atividades concluidas / total de atividades, em %
    private static int calcularProgresso(List<Document> atividades)
    {
        // A porcentagem é calculada com base nas atividades marcadas como concluídas.
        if (atividades.isEmpty())
            return 0; // evita dividir por zero

        int concluidas = 0;
        for (Document a : atividades)
            if ("CONCLUIDA".equals(a.getString("status")))
                concluidas++;

        return concluidas * 100 / atividades.size(); // divisao de int: ja arredonda pra baixo
    }

    // 0-33% inicial, 34-66% desenvolvimento, 67-99% fase final, 100% concluido
    private static String calcularFase(int progresso)
    {
        if (progresso >= 100) return "CONCLUIDO";
        if (progresso >= 67)  return "FASE_FINAL";
        if (progresso >= 34)  return "DESENVOLVIMENTO";
        return "FASE_INICIAL";
    }

    // Recalcula e grava o progresso e a fase no plantio (chamado sempre que as atividades mudam)
    private void atualizarProgresso(int plantioCodigo)
    {
        Document d = plantios().find(Filters.eq("codigo", plantioCodigo)).first();
        if (d == null)
            return;

        int progresso = calcularProgresso(d.getList("atividades", Document.class, new ArrayList<Document>()));

        // grava progresso e fase juntos, numa unica atualizacao
        plantios().updateOne(Filters.eq("codigo", plantioCodigo),
            Updates.combine(
                Updates.set("progresso", progresso),
                Updates.set("status", calcularFase(progresso))));
    }

    // Marca uma atividade existente como concluida (hoje). So funciona se o plantio for do usuario
    // e a atividade ainda nao estiver concluida. Devolve false se nao encontrou.
    public boolean concluirAtividade(int usuarioCodigo, int plantioCodigo, int atividadeCodigo)
    {
        // elemMatch acha a atividade certa dentro da lista (codigo igual e status diferente de CONCLUIDA)
        // o "$" em "atividades.$.status" significa "a atividade que bateu no filtro"
        UpdateResult resultado = plantios().updateOne(
            Filters.and(
                Filters.eq("codigo", plantioCodigo),
                Filters.eq("usuario.codigo", usuarioCodigo),
                Filters.eq("ativo", true),
                Filters.elemMatch("atividades", Filters.and(
                    Filters.eq("codigo", atividadeCodigo),
                    Filters.ne("status", "CONCLUIDA")))),
            Updates.combine(
                Updates.set("atividades.$.status", "CONCLUIDA"),
                Updates.set("atividades.$.dataRealizada", paraData(LocalDate.now()))));

        if (resultado.getMatchedCount() == 0)
            return false;

        atualizarProgresso(plantioCodigo);
        return true;
    }

    // Todas as atividades de todos os plantios ativos do usuario, da mais antiga para a mais recente.
    // O status e calculado pela data (como diz o dicionario): pendente com data ja passada vira ATRASADA.
    public ArrayList<AtividadeResumo> listarAtividadesPorUsuario(int usuarioCodigo)
    {
        ArrayList<AtividadeResumo> lista = new ArrayList<AtividadeResumo>();
        LocalDate hoje = LocalDate.now();

        for (Document d : plantios().find(Filters.and(
                Filters.eq("usuario.codigo", usuarioCodigo), Filters.eq("ativo", true))))
        {
            String nome   = d.getString("nome");
            String talhao = ((Document) d.get("talhao")).getString("nome");

            for (Document a : d.getList("atividades", Document.class, new ArrayList<Document>()))
            {
                String status = a.getString("status");
                // concluida mostra a data realizada; as demais mostram a data prevista
                Date   data   = "CONCLUIDA".equals(status) && a.getDate("dataRealizada") != null
                                ? a.getDate("dataRealizada") : a.getDate("dataPrevista");
                String texto  = paraTexto(data);

                // ATRASADA nao e gravada no banco: e calculada aqui, na hora de listar
                if ("PENDENTE".equals(status) && LocalDate.parse(texto).isBefore(hoje))
                    status = "ATRASADA";

                lista.add(new AtividadeResumo(
                    a.getInteger("codigo"), d.getInteger("codigo"), nome, talhao,
                    a.getString("tipoAtividade"), texto, status,
                    a.getString("quantidade"), a.getString("responsavel"), a.getString("observacoes")));
            }
        }

        // ordena por data; se a data for igual, desempata pelo codigo da atividade
        Collections.sort(lista, new Comparator<AtividadeResumo>()
        {
            public int compare(AtividadeResumo x, AtividadeResumo y)
            {
                int c = x.getData().compareTo(y.getData()); // "yyyy-MM-dd" ordena como texto
                return c != 0 ? c : Integer.compare(x.getCodigo(), y.getCodigo());
            }
        });
        return lista;
    }
}