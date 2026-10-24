package agromonitor.servidor;

import java.time.LocalDate;

import agromonitor.servidor.dao.AlertaDAO;
import agromonitor.servidor.dao.UsuarioDAO;
import agromonitor.servidor.email.EmailService;

// Thread que fica de olho nas atividades: confere assim que o servidor liga e depois de hora em hora,
// criando os alertas que faltam. Roda em paralelo com a aceitadora de conexoes.
public class VigiaDeAlertas extends Thread
{
    private static final long INTERVALO_MS = 60L * 60L * 1000L; // 1 hora

    private AlertaDAO dao;
    private UsuarioDAO usuarioDao;
    private EmailService email;

    public VigiaDeAlertas(AlertaDAO dao, UsuarioDAO usuarioDao, EmailService email) throws Exception
    {
        if (dao == null)
            throw new Exception("DAO de alerta ausente");

        this.dao = dao;
        if (usuarioDao == null || email == null)
            throw new Exception("Dependencias de e-mail ausentes");
        this.usuarioDao = usuarioDao;
        this.email = email;
        this.setName("VigiaDeAlertas");
        this.setDaemon(true); // nao impede o servidor de encerrar
    }

    public void run()
    {
        for (;;)
        {
            try
            {
                int criados = this.dao.gerarAlertasDeAtividades(LocalDate.now());
                if (criados > 0)
                    System.out.println("Vigia de alertas: " + criados + " alerta(s) novo(s).");
                enviarAlertasPendentes();
            }
            catch (Exception erro)
            {
                // erro de banco nao pode derrubar o vigia: tenta de novo na proxima rodada
                System.err.println("Vigia de alertas: falha na verificacao: " + erro);
            }

            try
            {
                Thread.sleep(INTERVALO_MS);
            }
            catch (InterruptedException erro)
            {
                return;
            }
        }
    }

    private void enviarAlertasPendentes()
    {
        for (AlertaDAO.AlertaEmail alerta : this.dao.listarPendentesDeEmail())
        {
            try
            {
                String destino = this.usuarioDao.emailParaAlerta(alerta.usuarioCodigo());
                if (destino == null)
                {
                    this.dao.marcarEmailProcessado(alerta.codigo());
                    continue;
                }
                if (this.email.enviarAlerta(destino, "Alerta de atividade", alerta.titulo(),
                        alerta.descricao(), alerta.plantio(), alerta.data(), alerta.prioridade()))
                    this.dao.marcarEmailProcessado(alerta.codigo());
            }
            catch (Exception erro)
            {
                System.err.println("Falha ao enviar alerta por e-mail: " + erro);
            }
        }
    }
}
