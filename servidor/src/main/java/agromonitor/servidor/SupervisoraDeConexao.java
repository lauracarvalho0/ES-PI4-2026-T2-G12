package agromonitor.servidor;

import java.io.*;
import java.net.*;
import java.util.*;

import agromonitor.comum.*;
import at.favre.lib.crypto.bcrypt.BCrypt;

// Uma thread por cliente conectado. Recebe os comunicados e atende cada um.
// No exemplo do professor o "if instanceof" fazia a conta; aqui ele chama
// validacao + MongoDB (cadastro e login).
public class SupervisoraDeConexao extends Thread
{
    private Parceiro            usuario;
    private Socket              conexao;
    private ArrayList<Parceiro> usuarios;
    private UsuarioDAO          dao;

    public SupervisoraDeConexao(Socket conexao, ArrayList<Parceiro> usuarios, UsuarioDAO dao) throws Exception
    {
        if (conexao == null)
            throw new Exception("Conexao ausente");
        if (usuarios == null)
            throw new Exception("Usuarios ausentes");
        if (dao == null)
            throw new Exception("DAO ausente");

        this.conexao  = conexao;
        this.usuarios = usuarios;
        this.dao      = dao;
    }

    public void run()
    {
        ObjectOutputStream transmissor;
        try
        {
            transmissor = new ObjectOutputStream(this.conexao.getOutputStream());
            transmissor.flush();
        }
        catch (Exception erro)
        {
            return;
        }

        ObjectInputStream receptor = null;
        try
        {
            receptor = new ObjectInputStream(this.conexao.getInputStream());
            // seguranca: so aceita desserializar classes do nosso pacote
            receptor.setObjectInputFilter(
                ObjectInputFilter.Config.createFilter("agromonitor.comum.*;java.lang.*;!*"));
        }
        catch (Exception erro)
        {
            try { transmissor.close(); } catch (Exception falha) {}
            return;
        }

        try
        {
            this.usuario = new Parceiro(this.conexao, receptor, transmissor);
        }
        catch (Exception erro)
        {} // sei que passei parametros corretos

        synchronized (this.usuarios)
        {
            this.usuarios.add(this.usuario);
        }

        try
        {
            for (;;)
            {
                Comunicado comunicado = this.usuario.envie();

                if (comunicado == null)
                    return;
                else if (comunicado instanceof PedidoDeCadastro)
                    this.usuario.receba(tratarCadastro((PedidoDeCadastro) comunicado));
                else if (comunicado instanceof PedidoDeLogin)
                    this.usuario.receba(tratarLogin((PedidoDeLogin) comunicado));
                else if (comunicado instanceof PedidoParaSair)
                {
                    synchronized (this.usuarios)
                    {
                        this.usuarios.remove(this.usuario);
                    }
                    this.usuario.adeus();
                    return;
                }
            }
        }
        catch (Exception erro)
        {
            // cliente caiu ou erro de comunicacao: tira da lista e encerra
            try
            {
                synchronized (this.usuarios)
                {
                    this.usuarios.remove(this.usuario);
                }
                this.usuario.adeus();
            }
            catch (Exception falha)
            {}
        }
    }

    private RespostaDeCadastro tratarCadastro(PedidoDeCadastro p)
    {
        String erro = primeiroErro(
            ValidadorUsuario.validarNome(p.getNome()),
            ValidadorUsuario.validarEmail(p.getEmail()),
            ValidadorUsuario.validarCpf(p.getCpf()),
            ValidadorUsuario.validarTelefone(p.getTelefone()),
            ValidadorUsuario.validarSenha(p.getSenha()));
        if (erro != null)
            return new RespostaDeCadastro(false, erro, 0);

        String email    = p.getEmail().trim().toLowerCase();
        String cpf      = ValidadorUsuario.somenteDigitos(p.getCpf());
        String telefone = p.getTelefone() == null ? "" : p.getTelefone().trim();

        try
        {
            String hash = BCrypt.withDefaults().hashToString(12, p.getSenha().toCharArray());
            int codigo = this.dao.inserir(
                p.getNome().trim(), email,
                telefone.isEmpty() ? null : telefone,
                cpf.isEmpty() ? null : cpf,
                hash);
            return new RespostaDeCadastro(true, "Cadastro realizado com sucesso!", codigo);
        }
        catch (UsuarioDAO.EmailJaCadastradoException e)
        {
            return new RespostaDeCadastro(false, "Este e-mail ja esta cadastrado.", 0);
        }
        catch (Exception e)
        {
            System.err.println("Erro ao cadastrar: " + e);
            return new RespostaDeCadastro(false, "Erro interno ao cadastrar. Tente novamente.", 0);
        }
    }

    private RespostaDeLogin tratarLogin(PedidoDeLogin p)
    {
        final String falha = "E-mail ou senha incorretos.";

        if (ValidadorUsuario.validarEmail(p.getEmail()) != null || p.getSenha() == null || p.getSenha().isEmpty())
            return new RespostaDeLogin(false, falha, 0, null, null);

        try
        {
            Usuario u = this.dao.buscarAtivoPorEmail(p.getEmail().trim().toLowerCase());
            if (u == null || !BCrypt.verifyer().verify(p.getSenha().toCharArray(), u.getSenhaHash()).verified)
                return new RespostaDeLogin(false, falha, 0, null, null);

            return new RespostaDeLogin(true, "Login realizado com sucesso!", u.getCodigo(), u.getNome(), u.getEmail());
        }
        catch (Exception e)
        {
            System.err.println("Erro no login: " + e);
            return new RespostaDeLogin(false, "Erro interno ao entrar. Tente novamente.", 0, null, null);
        }
    }

    private static String primeiroErro(String... erros)
    {
        for (String e : erros)
            if (e != null)
                return e;
        return null;
    }
}
