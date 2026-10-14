package agromonitor.servidor;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import agromonitor.comum.PlantioResumo;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;

// Unica classe que fala com o MongoDB para plantios.
// Segue o dicionario de dados: a colecao plantios embute a lista de atividades.
public class PlantioDAO
{
    // cronograma padrao (dias apos a data de plantio), igual ao mostrado na tela
    private static final String[] TIPOS   = { "PLANTIO", "IRRIGACAO", "ADUBACAO", "TRATAMENTO" };
    private static final int[]    OFFSETS = { 0, 7, 20, 35 };

    private final MongoDatabase banco;

    public PlantioDAO(MongoDatabase banco)
    {
        this.banco = banco;
    }

    private MongoCollection<Document> plantios() { return banco.getCollection("plantios"); }

    // Chamado uma vez na inicializacao do servidor (pode rodar varias vezes sem problema).
    public void garantirIndices()
    {
        plantios().createIndex(Indexes.ascending("codigo"), new IndexOptions().unique(true));
        plantios().createIndex(Indexes.ascending("usuario.codigo"));
        plantios().createIndex(Indexes.ascending("status"));
    }

    // Reserva "quantidade" codigos seguidos na colecao de contadores
    // e devolve o primeiro deles (ex.: reservou 4 e voltou 9 -> usa 9, 10, 11, 12).
    private int reservarCodigos(String colecao, int quantidade)
    {
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
        Date agora  = new Date();
        int  codigo = reservarCodigos("plantios", 1);
        int  primeiroCodigoAtividade = reservarCodigos("atividades", TIPOS.length);

        List<Document> atividades = new ArrayList<Document>();
        for (int i = 0; i < TIPOS.length; i++)
        {
            LocalDate prevista = ajustes[i] != null ? ajustes[i] : dataPlantio.plusDays(OFFSETS[i]);

            atividades.add(new Document("codigo", primeiroCodigoAtividade + i)
                .append("tipoAtividade", TIPOS[i])
                .append("dataPrevista", paraData(prevista))
                .append("dataRealizada", null)
                .append("status", "PENDENTE")
                .append("observacoes", null)
                .append("ordem", i + 1));
        }

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
            .append("ativo", true)
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

        for (Document d : plantios()
                .find(Filters.and(Filters.eq("usuario.codigo", usuarioCodigo), Filters.eq("ativo", true)))
                .sort(Sorts.descending("dataCriacao")))
        {
            List<Document> atividades = d.getList("atividades", Document.class, new ArrayList<Document>());

            String tipoProxima = null;
            Date   dataProxima = null;
            for (Document a : atividades)
            {
                if (!"PENDENTE".equals(a.getString("status")))
                    continue;
                Date prevista = a.getDate("dataPrevista");
                if (dataProxima == null || prevista.before(dataProxima))
                {
                    dataProxima = prevista;
                    tipoProxima = a.getString("tipoAtividade");
                }
            }

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
                d.getInteger("progresso"),
                d.getString("status")));
        }
        return lista;
    }
}
