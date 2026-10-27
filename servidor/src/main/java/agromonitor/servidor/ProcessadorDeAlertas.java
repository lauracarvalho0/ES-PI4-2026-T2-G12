package agromonitor.servidor;

import java.time.LocalDate;

import agromonitor.servidor.dao.AlertaDAO;
import agromonitor.servidor.dao.UsuarioDAO;
import agromonitor.servidor.email.EmailService;

// Faz o trabalho do vigia de alertas: cria os alertas de atividade que faltam e envia por e-mail
// os que estao pendentes. Quem chama e o VigiaDeAlertas (de hora em hora) e a SupervisoraDeConexao
// (logo depois de cadastrar um plantio ou registrar uma atividade, para o alerta nao esperar o vigia).
public class ProcessadorDeAlertas
{
    // vigia e pedidos de usuarios nao rodam ao mesmo tempo: evita e-mail duplicado
    private static final Object TRAVA = new Object();

    private AlertaDAO   alertaDao;
    private UsuarioDAO  usuarioDao;
    private EmailService email;

    public ProcessadorDeAlertas(AlertaDAO alertaDao, UsuarioDAO usuarioDao, EmailService email) throws Exception
    {
        if (alertaDao == null)
            throw new Exception("DAO de alerta ausente");
        if (usuarioDao == null || email == null)
            throw new Exception("Dependencias de e-mail ausentes");

        this.alertaDao  = alertaDao;
        this.usuarioDao = usuarioDao;
        this.email      = email;
    }

    // Confere agora, na thread de quem chamou. Nunca lanca excecao.
    public void verificar()
    {
        synchronized (TRAVA)
        {
            try
            {
                int criados = this.alertaDao.gerarAlertasDeAtividades(LocalDate.now());
                if (criados > 0)
                    System.out.println("Vigia de alertas: " + criados + " alerta(s) novo(s).");
                enviarAlertasPendentes();
            }
            catch (Exception erro)
            {
                // erro de banco nao pode derrubar quem chamou: o vigia tenta de novo na proxima rodada
                System.err.println("Vigia de alertas: falha na verificacao: " + erro);
            }
        }
    }

    // Confere em outra thread, para quem chamou nao esperar (enviar e-mail pode demorar alguns segundos).
    public void verificarEmSegundoPlano()
    {
        Thread t = new Thread(new Runnable()
        {
            public void run() { verificar(); }
        }, "VerificacaoDeAlertas");
        t.setDaemon(true);
        t.start();
    }

    private void enviarAlertasPendentes()
    {
        for (AlertaDAO.AlertaEmail alerta : this.alertaDao.listarPendentesDeEmail())
        {
            try
            {
                String destino = this.usuarioDao.emailParaAlerta(alerta.usuarioCodigo());
                if (destino == null)
                {
                    this.alertaDao.marcarEmailProcessado(alerta.codigo());
                    continue;
                }
                if (this.email.enviarAlerta(destino, "Alerta de atividade", alerta.titulo(),
                        alerta.descricao(), alerta.plantio(), alerta.data(), alerta.prioridade()))
                    this.alertaDao.marcarEmailProcessado(alerta.codigo());
            }
            catch (Exception erro)
            {
                System.err.println("Falha ao enviar alerta por e-mail: " + erro);
            }
        }
    }
}
