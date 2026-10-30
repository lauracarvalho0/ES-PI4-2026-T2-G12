package agromonitor.servidor;

import java.net.InetSocketAddress;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

// Mongo em memoria (so para teste): nao precisa instalar nada nem ter Docker.
// Cada teste cria o seu proprio banco, entao um teste nunca atrapalha o outro.
public class BancoDeTeste implements AutoCloseable
{
    private final MongoServer   servidor;
    private final MongoClient   cliente;
    private final MongoDatabase banco;

    public BancoDeTeste()
    {
        this.servidor = new MongoServer(new MemoryBackend());
        InetSocketAddress endereco = this.servidor.bind();
        this.cliente = MongoClients.create("mongodb://" + endereco.getHostString() + ":" + endereco.getPort());
        this.banco   = this.cliente.getDatabase("agromonitor_teste");
    }

    public MongoDatabase banco() { return this.banco; }

    @Override
    public void close()
    {
        this.cliente.close();
        this.servidor.shutdownNow();
    }
}
