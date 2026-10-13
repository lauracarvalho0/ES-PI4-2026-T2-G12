package agromonitor.comum;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Semaphore;

// Mesmo desenho da aula: representa "quem esta do outro lado" da conexao.
// receba = eu TRANSMITO e o parceiro recebe; envie = o parceiro transmite e eu RECEBO;
// espie = olho o que vem sem consumir.
public class Parceiro
{
    private Socket             conexao;
    private ObjectInputStream  receptor;
    private ObjectOutputStream transmissor;

    private Comunicado proximoComunicado = null;

    private Semaphore mutex = new Semaphore(1, true);

    public Parceiro(Socket conexao, ObjectInputStream receptor, ObjectOutputStream transmissor) throws Exception
    {
        if (conexao == null)     throw new Exception("Conexao ausente");
        if (receptor == null)    throw new Exception("Receptor ausente");
        if (transmissor == null) throw new Exception("Transmissor ausente");

        this.conexao     = conexao;
        this.receptor    = receptor;
        this.transmissor = transmissor;
    }

    public void receba(Comunicado x) throws Exception
    {
        try
        {
            this.transmissor.writeObject(x);
            this.transmissor.flush();
        }
        catch (IOException erro)
        {
            throw new Exception("Erro de transmissao");
        }
    }

    public Comunicado espie() throws Exception
    {
        try
        {
            this.mutex.acquireUninterruptibly();
            if (this.proximoComunicado == null)
                this.proximoComunicado = (Comunicado) this.receptor.readObject();
            return this.proximoComunicado;
        }
        catch (Exception erro)
        {
            throw new Exception("Erro de recepcao");
        }
        finally
        {
            this.mutex.release();
        }
    }

    public Comunicado envie() throws Exception
    {
        try
        {
            this.mutex.acquireUninterruptibly();
            if (this.proximoComunicado == null)
                this.proximoComunicado = (Comunicado) this.receptor.readObject();
            Comunicado ret = this.proximoComunicado;
            this.proximoComunicado = null;
            return ret;
        }
        catch (Exception erro)
        {
            throw new Exception("Erro de recepcao");
        }
        finally
        {
            this.mutex.release();
        }
    }

    public void adeus() throws Exception
    {
        try
        {
            this.transmissor.close();
            this.receptor.close();
            this.conexao.close();
        }
        catch (Exception erro)
        {
            throw new Exception("Erro de desconexao");
        }
    }
}
