package agromonitor.servidor;

import java.time.LocalDate;

import agromonitor.servidor.dao.AlertaDAO;

// Thread que fica de olho nas atividades: confere assim que o servidor liga e depois de hora em hora,
// criando os alertas que faltam. Roda em paralelo com a aceitadora de conexoes.
public class VigiaDeAlertas extends Thread
{
    private static final long INTERVALO_MS = 60L * 60L * 1000L; // 1 hora

    private AlertaDAO dao;

    public VigiaDeAlertas(AlertaDAO dao) throws Exception
    {
        if (dao == null)
            throw new Exception("DAO de alerta ausente");

        this.dao = dao;
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
}
