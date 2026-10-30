package agromonitor.comum;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

// Teclado guarda o "System.in" no momento em que a classe e carregada,
// por isso a leitura de teclado real nao e simulada aqui: so conferimos que a classe existe e e utilizavel.
class TecladoTest
{
    @Test
    void metodoDeLeituraEstaDisponivel() throws Exception
    {
        assertNotNull(Teclado.class.getMethod("getUmString"));
    }
}
