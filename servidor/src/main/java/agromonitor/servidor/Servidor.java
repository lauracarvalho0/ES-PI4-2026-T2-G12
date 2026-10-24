package agromonitor.servidor;

import java.util.*;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import agromonitor.comum.*;
import agromonitor.servidor.dao.AlertaDAO;
import agromonitor.servidor.dao.PlantioDAO;
import agromonitor.servidor.dao.UsuarioDAO;
import agromonitor.servidor.email.EmailService;

public class Servidor
{
    public static final String PORTA_PADRAO = "3000";

    public static void main(String[] args)
    {
        // 1) porta (opcional) vinda de args
        if (args.length > 1)
        {
            System.err.println("Uso esperado: java Servidor [PORTA]\n");
            return;
        }

        String porta = Servidor.PORTA_PADRAO;
        if (args.length == 1)
            porta = args[0];

        // conexao com o MongoDB (string de conexao vem da variavel de ambiente)
        String uri = System.getenv("MONGO_URI");
        if (uri == null || uri.isBlank())
        {
            System.err.println("Defina a variavel de ambiente MONGO_URI com a string de conexao do Atlas.\n");
            return;
        }

        UsuarioDAO dao;
        PlantioDAO plantioDao;
        AlertaDAO alertaDao;
        try
        {
            MongoClient cliente = MongoClients.create(uri);
            dao = new UsuarioDAO(cliente.getDatabase("agromonitor"));
            plantioDao = new PlantioDAO(cliente.getDatabase("agromonitor"));
            alertaDao = new AlertaDAO(cliente.getDatabase("agromonitor"));
            dao.garantirIndices(); // tambem testa a conexao
            plantioDao.garantirIndices();
            alertaDao.garantirIndices();
        }
        catch (Exception erro)
        {
            System.err.println("Nao foi possivel conectar ao MongoDB. Confira MONGO_URI, usuario/senha e o IP liberado no Atlas.\n" + erro.getMessage());
            return;
        }
        System.out.println("Conectado ao MongoDB.");

        // servico de e-mail (recuperacao de senha e alertas); usa SMTP_USER/SMTP_PASS
        EmailService email = EmailService.doAmbiente();
        if (email.isAtivo())
            System.out.println("E-mail: SMTP configurado.");
        else
            System.out.println("E-mail: SMTP_USER/SMTP_PASS nao definidas. Alertas nao serao enviados ate configurar o SMTP.");

        // 2) estrutura de dados compartilhada com os clientes conectados
        ArrayList<Parceiro> usuarios = new ArrayList<Parceiro>();

        // 3) aceitadora de conexoes
        AceitadoraDeConexao aceitadora = null;
        try
        {
            aceitadora = new AceitadoraDeConexao(porta, usuarios, dao, plantioDao, alertaDao, email);
            aceitadora.start();
        }
        catch (Exception erro)
        {
            System.err.println("Escolha uma porta liberada para uso!\n");
            return;
        }

        // vigia de alertas: confere as atividades agora e depois de hora em hora
        try
        {
            new VigiaDeAlertas(alertaDao, dao, email).start();
        }
        catch (Exception erro)
        {
            System.err.println("Nao foi possivel iniciar o vigia de alertas: " + erro.getMessage());
        }

        // tarefa simultanea da main: interagir com o administrador
        for (;;)
        {
            System.out.println("O servidor esta ativo na porta " + porta + "! Para desativa-lo,");
            System.out.println("use o comando \"desativar\"\n");
            System.out.print("> ");

            String comando = null;
            try
            {
                comando = Teclado.getUmString();
            }
            catch (Exception erro)
            {
                // entrada do console indisponivel: continua rodando sem comandos
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
                continue;
            }

            if (comando.toLowerCase().equals("desativar"))
            {
                synchronized (usuarios)
                {
                    ComunicadoDeDesligamento comunicado = new ComunicadoDeDesligamento();

                    for (Parceiro usuario : usuarios)
                    {
                        try
                        {
                            usuario.receba(comunicado);
                            usuario.adeus();
                        }
                        catch (Exception erro)
                        {}
                    }
                }

                System.out.println("O servidor foi desativado!\n");
                System.exit(0);
            }
            else
                System.err.println("Comando invalido!\n");
        }
    }
}
