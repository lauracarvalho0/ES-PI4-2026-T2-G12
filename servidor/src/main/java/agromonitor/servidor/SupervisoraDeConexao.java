package agromonitor.servidor;

import java.io.*;
import java.net.*;
import java.time.LocalDate;
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
    private PlantioDAO          plantioDao;

    public SupervisoraDeConexao(Socket conexao, ArrayList<Parceiro> usuarios, UsuarioDAO dao, PlantioDAO plantioDao) throws Exception
    {
        if (conexao == null)
            throw new Exception("Conexao ausente");
        if (usuarios == null)
            throw new Exception("Usuarios ausentes");
        if (dao == null)
            throw new Exception("DAO ausente");
        if (plantioDao == null)
            throw new Exception("DAO de plantio ausente");

        this.conexao    = conexao;
        this.usuarios   = usuarios;
        this.dao        = dao;
        this.plantioDao = plantioDao;
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
                else if (comunicado instanceof PedidoDeCadastroDePlantio)
                    this.usuario.receba(tratarCadastroDePlantio((PedidoDeCadastroDePlantio) comunicado));
                else if (comunicado instanceof PedidoDeListagemDePlantios)
                    this.usuario.receba(tratarListagemDePlantios((PedidoDeListagemDePlantios) comunicado));
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

    private RespostaDeCadastroDePlantio tratarCadastroDePlantio(PedidoDeCadastroDePlantio p)
    {
        String erro = primeiroErro(
            ValidadorPlantio.validarUsuario(p.getUsuarioCodigo()),
            ValidadorPlantio.validarCultura(p.getCultura()),
            ValidadorPlantio.validarNome(p.getNome()),
            ValidadorPlantio.validarSafra(p.getSafra()),
            ValidadorPlantio.validarDescricao(p.getDescricao()),
            ValidadorPlantio.validarTalhao(p.getTalhao()),
            ValidadorPlantio.validarAreaPlantada(p.getAreaPlantada()),
            ValidadorPlantio.validarSistemaCultivo(p.getSistemaCultivo()),
            ValidadorPlantio.validarAltitude(p.getAltitude()),
            ValidadorPlantio.validarDataPlantio(p.getDataPlantio()),
            ValidadorPlantio.validarMetodoPlantio(p.getMetodoPlantio()),
            ValidadorPlantio.validarAjuste(p.getAjustePlantio(),   "plantio"),
            ValidadorPlantio.validarAjuste(p.getAjusteIrrigacao(), "irrigacao"),
            ValidadorPlantio.validarAjuste(p.getAjusteAdubacao(),  "adubacao"),
            ValidadorPlantio.validarAjuste(p.getAjusteTratos(),    "tratos culturais"));
        if (erro != null)
            return new RespostaDeCadastroDePlantio(false, erro, 0);

        try
        {
            // o dono do plantio precisa existir e estar ativo
            if (!this.dao.existeAtivoPorCodigo(p.getUsuarioCodigo()))
                return new RespostaDeCadastroDePlantio(false, "Usuario nao encontrado. Entre novamente.", 0);

            String descricao = p.getDescricao() == null ? "" : p.getDescricao().trim();
            String local     = p.getLocalizacao() == null ? "" : p.getLocalizacao().trim();
            String metodo    = p.getMetodoPlantio() == null ? "" : p.getMetodoPlantio().trim();

            // ajustes opcionais: data valida ou null (null = previsao automatica)
            LocalDate[] ajustes = new LocalDate[4];
            ajustes[0] = ValidadorPlantio.lerData(vazioParaNull(p.getAjustePlantio()));
            ajustes[1] = ValidadorPlantio.lerData(vazioParaNull(p.getAjusteIrrigacao()));
            ajustes[2] = ValidadorPlantio.lerData(vazioParaNull(p.getAjusteAdubacao()));
            ajustes[3] = ValidadorPlantio.lerData(vazioParaNull(p.getAjusteTratos()));

            int codigo = this.plantioDao.inserir(
                p.getUsuarioCodigo(),
                p.getCultura().trim(),
                p.getNome().trim(),
                p.getSafra().trim(),
                descricao.isEmpty() ? null : descricao,
                p.getSistemaCultivo(),
                p.getTalhao().trim(),
                p.getAreaPlantada(),
                local.isEmpty() ? null : local,
                p.getAltitude(),
                ValidadorPlantio.lerData(p.getDataPlantio()),
                metodo.isEmpty() ? null : metodo,
                ajustes);
            return new RespostaDeCadastroDePlantio(true, "Plantio cadastrado com sucesso!", codigo);
        }
        catch (Exception e)
        {
            System.err.println("Erro ao cadastrar plantio: " + e);
            return new RespostaDeCadastroDePlantio(false, "Erro interno ao cadastrar o plantio. Tente novamente.", 0);
        }
    }

    private RespostaDeListagemDePlantios tratarListagemDePlantios(PedidoDeListagemDePlantios p)
    {
        String erro = ValidadorPlantio.validarUsuario(p.getUsuarioCodigo());
        if (erro != null)
            return new RespostaDeListagemDePlantios(false, erro, null);

        try
        {
            return new RespostaDeListagemDePlantios(true, "OK", this.plantioDao.listarPorUsuario(p.getUsuarioCodigo()));
        }
        catch (Exception e)
        {
            System.err.println("Erro ao listar plantios: " + e);
            return new RespostaDeListagemDePlantios(false, "Erro interno ao buscar os plantios. Tente novamente.", null);
        }
    }

    // lerData nao aceita null; este auxiliar devolve "" quando nao veio nada
    private static String vazioParaNull(String texto)
    {
        return texto == null ? "" : texto;
    }

    private static String primeiroErro(String... erros)
    {
        for (String e : erros)
            if (e != null)
                return e;
        return null;
    }
}
