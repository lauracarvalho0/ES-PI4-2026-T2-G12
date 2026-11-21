// Giovana Uchelli - 25008818

package agromonitor.comum;

import java.io.*; // ObjectInputStream, ObjectOutpuStream ...
import java.net.*; // Socket
import java.util.concurrent.Semaphore; // Semaphore que controla o acesso das threads


// Representa o "parceiro" do outro lado da conexão 
// Serve para enviar e receber objetos Comunicado pela rede
public class Parceiro
{
    private Socket             conexao;
    private ObjectInputStream  receptor;
    private ObjectOutputStream transmissor;
    
    private Comunicado proximoComunicado=null;

    // Garante que só um thread leia por vez
    private Semaphore mutEx = new Semaphore (1,true);

    public Parceiro (Socket             conexao,
                     ObjectInputStream  receptor,
                     ObjectOutputStream transmissor)
                     throws Exception 
    {
        if (conexao==null)
            throw new Exception ("Conexao ausente");

        if (receptor==null)
            throw new Exception ("Receptor ausente");

        if (transmissor==null)
            throw new Exception ("Transmissor ausente");

        this.conexao     = conexao;
        this.receptor    = receptor;
        this.transmissor = transmissor;
    }

    // Envia uma mensagem para o outro lado
    public void receba (Comunicado x) throws Exception
    {
        try
        {
            this.transmissor.writeObject (x); // Escreve o objeto na conexao
            this.transmissor.flush       (); // Força o envio imediato
        }
        catch (IOException erro)
        {
            throw new Exception ("Erro de transmissao");
        }
    }

    // Espia a proxima mensagem sem retirá-la 
    public Comunicado espie () throws Exception
    {
        try
        {
            // Trava para só uma thread ler por vez
            this.mutEx.acquireUninterruptibly(); 
            if (this.proximoComunicado==null) this.proximoComunicado = (Comunicado)this.receptor.readObject();
            this.mutEx.release();

            return this.proximoComunicado;
        }
        catch (Exception erro)
        {
            throw new Exception ("Erro de recepcao");
        }
    }

    // Receba a proxima mensagem e a retira
    public Comunicado envie () throws Exception
    {
        try
        {
            if (this.proximoComunicado==null) this.proximoComunicado = (Comunicado)this.receptor.readObject();
            Comunicado ret         = this.proximoComunicado;
            this.proximoComunicado = null;
            return ret;
        }
        catch (Exception erro)
        {
            throw new Exception ("Erro de recepcao");
        }
    }

    // Encerra a conexão
    public void adeus () throws Exception
    {
        try
        {
            this.transmissor.close();
            this.receptor   .close();
            this.conexao    .close();
        }
        catch (Exception erro)
        {
            throw new Exception ("Erro de desconexao");
        }
    }
}