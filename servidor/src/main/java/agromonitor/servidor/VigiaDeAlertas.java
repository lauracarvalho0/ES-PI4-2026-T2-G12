// Autoria: Beatriz Leme — RA 25015554.
package agromonitor.servidor;

import agromonitor.servidor.dao.AlertaDAO;
import agromonitor.servidor.dao.UsuarioDAO;
import agromonitor.servidor.email.EmailService;

// Thread que fica de olho nas atividades: confere assim que o servidor liga e depois de hora em hora,
// criando os alertas que faltam e enviando os e-mails pendentes. Roda em paralelo com a aceitadora
// de conexoes. O trabalho em si fica no ProcessadorDeAlertas.
public class VigiaDeAlertas extends Thread
{
    private static final long INTERVALO_MS = 60L * 60L * 1000L; // 1 hora

    private ProcessadorDeAlertas processador;

    public VigiaDeAlertas(AlertaDAO dao, UsuarioDAO usuarioDao, EmailService email) throws Exception
    {
        this.processador = new ProcessadorDeAlertas(dao, usuarioDao, email);
        this.setName("VigiaDeAlertas");
        this.setDaemon(true); // nao impede o servidor de encerrar
    }

    public void run()
    {
        // Executa uma verificação ao iniciar e repete o processamento no intervalo configurado.
        for (;;)
        {
            // O processador verifica atividades e trata os e-mails que ainda estão pendentes.
            this.processador.verificar();

            try
            {
                // Pausa a thread entre as verificações para respeitar o intervalo de uma hora.
                Thread.sleep(INTERVALO_MS);
            }
            catch (InterruptedException erro)
            {
                return;
            }
        }
    }
}
