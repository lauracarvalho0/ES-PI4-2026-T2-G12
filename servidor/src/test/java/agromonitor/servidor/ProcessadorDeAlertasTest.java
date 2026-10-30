package agromonitor.servidor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import agromonitor.servidor.dao.AlertaDAO;
import agromonitor.servidor.dao.AlertaDAO.AlertaEmail;
import agromonitor.servidor.dao.UsuarioDAO;
import agromonitor.servidor.email.EmailService;

// Sem banco: os DAOs e o e-mail sao "dublês" (mocks), so a logica do processador e testada.
class ProcessadorDeAlertasTest
{
    private AlertaDAO    alertaDao;
    private UsuarioDAO   usuarioDao;
    private EmailService email;
    private ProcessadorDeAlertas processador;

    private static AlertaEmail alerta(int codigo, int usuario)
    {
        return new AlertaEmail(codigo, usuario, "Atividade atrasada", "descricao", "Soja T1", "2026-10-28", "ALTA");
    }

    @BeforeEach
    void preparar() throws Exception
    {
        alertaDao  = mock(AlertaDAO.class);
        usuarioDao = mock(UsuarioDAO.class);
        email      = mock(EmailService.class);
        processador = new ProcessadorDeAlertas(alertaDao, usuarioDao, email);
    }

    // ---------- construtor ----------
    @Test
    void construtorExigeTodasAsDependencias()
    {
        assertEquals("DAO de alerta ausente",
            assertThrows(Exception.class, () -> new ProcessadorDeAlertas(null, usuarioDao, email)).getMessage());
        assertThrows(Exception.class, () -> new ProcessadorDeAlertas(alertaDao, null, email));
        assertThrows(Exception.class, () -> new ProcessadorDeAlertas(alertaDao, usuarioDao, null));
    }

    // ---------- verificar ----------
    @Test
    void verificarGeraOsAlertasDeHoje()
    {
        processador.verificar();
        verify(alertaDao).gerarAlertasDeAtividades(LocalDate.now());
    }

    @Test
    void alertaPendenteEEnviadoEMarcadoComoProcessado() throws Exception
    {
        when(alertaDao.listarPendentesDeEmail()).thenReturn(List.of(alerta(10, 5)));
        when(usuarioDao.emailParaAlerta(5)).thenReturn("maria@x.com");
        when(email.enviarAlerta("maria@x.com", "Alerta de atividade", "Atividade atrasada",
                "descricao", "Soja T1", "2026-10-28", "ALTA")).thenReturn(true);

        processador.verificar();

        verify(alertaDao).marcarEmailProcessado(10);
    }

    @Test
    void semSmtpOAlertaContinuaPendente() throws Exception
    {
        when(alertaDao.listarPendentesDeEmail()).thenReturn(List.of(alerta(10, 5)));
        when(usuarioDao.emailParaAlerta(5)).thenReturn("maria@x.com");
        when(email.enviarAlerta(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);

        processador.verificar();

        verify(alertaDao, never()).marcarEmailProcessado(anyInt());
    }

    @Test
    void usuarioQueNaoQuerEmailTemOAlertaMarcadoSemEnviar() throws Exception
    {
        when(alertaDao.listarPendentesDeEmail()).thenReturn(List.of(alerta(10, 5)));
        when(usuarioDao.emailParaAlerta(5)).thenReturn(null);

        processador.verificar();

        verify(alertaDao).marcarEmailProcessado(10);
        verify(email, never()).enviarAlerta(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void falhaNoEnvioDeUmAlertaNaoImpedeOsDemais() throws Exception
    {
        when(alertaDao.listarPendentesDeEmail()).thenReturn(List.of(alerta(10, 5), alerta(11, 6)));
        when(usuarioDao.emailParaAlerta(5)).thenReturn("a@x.com");
        when(usuarioDao.emailParaAlerta(6)).thenReturn("b@x.com");
        when(email.enviarAlerta(eq("a@x.com"), any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException("SMTP caiu"));
        when(email.enviarAlerta(eq("b@x.com"), any(), any(), any(), any(), any(), any())).thenReturn(true);

        assertDoesNotThrow(() -> processador.verificar());

        verify(alertaDao, never()).marcarEmailProcessado(10);
        verify(alertaDao).marcarEmailProcessado(11);
    }

    @Test
    void erroDeBancoNaoPropagaParaQuemChamou()
    {
        when(alertaDao.gerarAlertasDeAtividades(any())).thenThrow(new RuntimeException("mongo fora do ar"));
        assertDoesNotThrow(() -> processador.verificar());
    }

    // ---------- segundo plano ----------
    @Test
    void verificarEmSegundoPlanoRodaEmOutraThread()
    {
        processador.verificarEmSegundoPlano();
        verify(alertaDao, timeout(3000)).gerarAlertasDeAtividades(any());
    }
}
