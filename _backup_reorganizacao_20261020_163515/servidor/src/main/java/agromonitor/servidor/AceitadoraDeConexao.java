package agromonitor.servidor;

import java.net.*;
import java.util.*;

import agromonitor.comum.*;

// Thread que so aceita conexoes e, para cada uma, dispara uma SupervisoraDeConexao.
// Fica curta e rapida de proposito, para voltar logo a aceitar novos clientes.
public class AceitadoraDeConexao extends Thread
{
    private ServerSocket        pedido;
    private ArrayList<Parceiro> usuarios;
    private UsuarioDAO          dao;
    private PlantioDAO          plantioDao;
    private EmailService        email;

    public AceitadoraDeConexao(String porta, ArrayList<Parceiro> usuarios, UsuarioDAO dao,
                               PlantioDAO plantioDao, EmailService email) throws Exception
    {
        if (porta == null)
            throw new Exception("Porta ausente");

        try
        {
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
        if (email == null)
            throw new Exception("Servico de e-mail ausente");

        this.usuarios   = usuarios;
        this.dao        = dao;
        this.plantioDao = plantioDao;
        this.email      = email;
    }

    public void run()
    {
        for (;;)
        {
            Socket conexao = null;
            try
            {
                conexao = this.pedido.accept();
            }
            catch (Exception erro)
            {
                continue;
            }

            SupervisoraDeConexao supervisora = null;
            try
            {
                supervisora = new SupervisoraDeConexao(conexao, this.usuarios, this.dao, this.plantioDao, this.email);
            }
            catch (Exception erro)
            {} // sei que passei parametros corretos

            supervisora.start();
        }
    }
}