// Laura Carvalho - 25014543

package agromonitor.servidor;

import java.time.LocalDate;  // Data de hoje, usada para gerar os alertas das atividades.

import agromonitor.servidor.dao.AlertaDAO; // Acesso à coleção de alertas.
import agromonitor.servidor.dao.UsuarioDAO; // Acesso à coleção de usuários (e-mail para alerta).
import agromonitor.servidor.email.EmailService; // Envio de e-mail dos alertas.

// Faz o trabalho do vigia de alertas: cria os alertas de atividade que faltam e envia por e-mail
// os que estao pendentes. Quem chama e o VigiaDeAlertas (de hora em hora) e a SupervisoraDeConexao
// (logo depois de cadastrar um plantio ou registrar uma atividade, para o alerta nao esperar o vigia).
public class ProcessadorDeAlertas
{
    // vigia e pedidos de usuarios nao rodam ao mesmo tempo: evita e-mail duplicado
    // static = uma unica trava compartilhada por todas as instancias da classe
    private static final Object TRAVA = new Object();

    private AlertaDAO   alertaDao;
    private UsuarioDAO  usuarioDao;
    private EmailService email;

    // recebe tudo que precisa de fora e recusa dependencia nula (falha logo, nao la na frente)
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
        // so uma thread por vez entra aqui; as outras esperam a vez
        synchronized (TRAVA)
        {
            try
            {
                // cria no banco os alertas das atividades de hoje que ainda nao tem alerta
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
        // classe anonima: cria uma Runnable na hora, so pra chamar verificar() dentro da nova thread
        Thread t = new Thread(new Runnable()
        {
            public void run() { verificar(); }
        }, "VerificacaoDeAlertas"); // nome da thread (ajuda a identificar em logs)
        t.setDaemon(true); // daemon: nao impede o programa de fechar se ela ainda estiver rodando
        t.start();
    }

    // manda por e-mail cada alerta que ainda nao foi enviado
    private void enviarAlertasPendentes()
    {
        for (AlertaDAO.AlertaEmail alerta : this.alertaDao.listarPendentesDeEmail())
        {
            // try dentro do for: se um alerta falhar, os outros ainda sao enviados
            try
            {
                String destino = this.usuarioDao.emailParaAlerta(alerta.usuarioCodigo());
                // usuario sem e-mail para alerta: marca como processado pra nao tentar de novo toda hora
                if (destino == null)
                {
                    this.alertaDao.marcarEmailProcessado(alerta.codigo());
                    continue;
                }
                // so marca como processado se o envio deu certo; se falhou, tenta na proxima rodada
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