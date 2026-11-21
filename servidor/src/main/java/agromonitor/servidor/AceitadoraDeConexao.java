// Giovana Uchelli - 25008818

package agromonitor.servidor;

import java.net.*; // Socket, serverSocket
import java.util.*; // ArrayList

// Classes compartilhadas entre servidor e cliente
import agromonitor.comum.*;

// Classes que falam com o mongo
import agromonitor.servidor.dao.AlertaDAO;
import agromonitor.servidor.dao.PlantioDAO;
import agromonitor.servidor.dao.UsuarioDAO;

// Serviço que envia emails
import agromonitor.servidor.email.EmailService;

// Thread que so aceita conexoes e, para cada uma, dispara uma SupervisoraDeConexao.
// Fica curta e rapida de proposito, para voltar logo a aceitar novos clientes.
public class AceitadoraDeConexao extends Thread
{
    private ServerSocket        pedido; // Fica "ouvindo" a porta e esperando clientes
    private ArrayList<Parceiro> usuarios;
    private UsuarioDAO          dao;
    private PlantioDAO          plantioDao;
    private AlertaDAO           alertaDao;
    private EmailService        email;

    public AceitadoraDeConexao(String porta, ArrayList<Parceiro> usuarios, UsuarioDAO dao,
                               PlantioDAO plantioDao, AlertaDAO alertaDao, EmailService email) throws Exception
    {
        if (porta == null)
            throw new Exception("Porta ausente");

        try
        {
            // Abre o servidor nessa porta
            this.pedido = new ServerSocket(Integer.parseInt(porta));
        }
        catch (Exception erro)
        {
            throw new Exception("Porta invalida");
        }

        if (usuarios == null)
            throw new Exception("Usuarios ausentes");
        if (dao == null)
            throw new Exception("DAO ausente");
        if (plantioDao == null)
            throw new Exception("DAO de plantio ausente");
        if (alertaDao == null)
            throw new Exception("DAO de alerta ausente");
        if (email == null)
            throw new Exception("Servico de e-mail ausente");

        this.usuarios   = usuarios;
        this.dao        = dao;
        this.plantioDao = plantioDao;
        this.alertaDao  = alertaDao;
        this.email      = email;
    }

    public void run()
    {
        for (;;)
        {
            Socket conexao = null;
            try
            {
                conexao = this.pedido.accept(); // Espera um cliente se conectar
            }
            catch (Exception erro)
            {
                continue; 
            }

            SupervisoraDeConexao supervisora = null;
            try
            {
                supervisora = new SupervisoraDeConexao(conexao, this.usuarios, this.dao, this.plantioDao, this.alertaDao, this.email);
            }
            catch (Exception erro)
            {} // sei que passei parametros corretos

            supervisora.start();
        }
    }
}