// Giovana Uchelli - 25008818

package agromonitor.servidor.validacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidadorUsuarioTest
{
    // ---------- nome ----------
    @ParameterizedTest
    @ValueSource(strings = {"Jo", "Maria Silva", "  Ana  "})
    void nomeValidoRetornaNull(String nome)
    {
        assertNull(ValidadorUsuario.validarNome(nome));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A", "  B  "})
    void nomeInvalidoRetornaMensagem(String nome)
    {
        assertEquals("Informe o nome completo.", ValidadorUsuario.validarNome(nome));
    }

    // ---------- e-mail ----------
    @ParameterizedTest
    @ValueSource(strings = {"a@b.co", "maria.silva@fazenda.com.br", "  x_y+z@dominio.org  "})
    void emailValido(String email)
    {
        assertNull(ValidadorUsuario.validarEmail(email));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"semarroba.com", "a@b", "a@b.c", "@dominio.com", "a b@dominio.com", "a@@b.com"})
    void emailInvalido(String email)
    {
        assertEquals("E-mail invalido.", ValidadorUsuario.validarEmail(email));
    }

    // ---------- telefone (opcional) ----------
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "(19) 99999-0000", "19999990000"})
    void telefoneValidoOuOpcional(String tel)
    {
        assertNull(ValidadorUsuario.validarTelefone(tel));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "(--) ----"})
    void telefoneSemDigitoEInvalido(String tel)
    {
        assertEquals("Telefone invalido.", ValidadorUsuario.validarTelefone(tel));
    }

    // ---------- senha ----------
    @Test
    void senhaComSeisCaracteresEValida()
    {
        assertNull(ValidadorUsuario.validarSenha("123456"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345", "abcde"})
    void senhaCurtaEInvalida(String senha)
    {
        assertEquals("A senha deve ter pelo menos 6 caracteres.", ValidadorUsuario.validarSenha(senha));
    }

    // ---------- CPF (opcional) ----------
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "529.982.247-25", "52998224725", "111.444.777-35"})
    void cpfValidoOuOpcional(String cpf)
    {
        assertNull(ValidadorUsuario.validarCpf(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "529.982.247-24",   // digito verificador errado
        "111.111.111-11",   // todos iguais
        "00000000000",
        "123",              // curto
        "529982247255"      // longo (12 digitos)
    })
    void cpfInvalido(String cpf)
    {
        assertEquals("CPF invalido.", ValidadorUsuario.validarCpf(cpf));
    }

    @Test
    void somenteDigitosRemoveMascara()
    {
        assertEquals("52998224725", ValidadorUsuario.somenteDigitos("529.982.247-25"));
        assertEquals("", ValidadorUsuario.somenteDigitos(null));
        assertEquals("", ValidadorUsuario.somenteDigitos("abc"));
    }

    // ---------- codigo ----------
    @Test
    void codigoDeUsuario()
    {
        assertNull(ValidadorUsuario.validarCodigo(1));
        assertNotNull(ValidadorUsuario.validarCodigo(0));
        assertNotNull(ValidadorUsuario.validarCodigo(-5));
    }
}
