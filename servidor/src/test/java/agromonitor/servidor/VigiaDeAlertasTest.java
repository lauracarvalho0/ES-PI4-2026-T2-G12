package agromonitor.servidor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import agromonitor.servidor.dao.AlertaDAO;
import agromonitor.servidor.dao.UsuarioDAO;
import agromonitor.servidor.email.EmailService;

class VigiaDeAlertasTest
{
    private final AlertaDAO    alertaDao  = mock(AlertaDAO.class);
    private final UsuarioDAO   usuarioDao = mock(UsuarioDAO.class);
    private final EmailService email      = mock(EmailService.class);

    @Test
    void construtorValidaDependencias()
    {
        assertThrows(Exception.class, () -> new VigiaDeAlertas(null, usuarioDao, email));
        assertThrows(Exception.class, () -> new VigiaDeAlertas(alertaDao, null, email));
        assertThrows(Exception.class, () -> new VigiaDeAlertas(alertaDao, usuarioDao, null));
    }

    @Test
    void eThreadDaemonComNome() throws Exception
    {
        VigiaDeAlertas v = new VigiaDeAlertas(alertaDao, usuarioDao, email);
        assertTrue(v.isDaemon());            // nao impede o servidor de encerrar
        assertEquals("VigiaDeAlertas", v.getName());
    }

    @Test
    void confereAssimQueLigaEParaQuandoInterrompida() throws Exception
    {
        VigiaDeAlertas v = new VigiaDeAlertas(alertaDao, usuarioDao, email);
        v.start();

        verify(alertaDao, timeout(3000).atLeastOnce()).gerarAlertasDeAtividades(any());
        Thread.sleep(300);
        verify(alertaDao, times(1)).gerarAlertasDeAtividades(any()); // so uma vez: o proximo ciclo e daqui a 1 hora

        v.interrupt();
        v.join(3000);
        assertFalse(v.isAlive());
    }
}
