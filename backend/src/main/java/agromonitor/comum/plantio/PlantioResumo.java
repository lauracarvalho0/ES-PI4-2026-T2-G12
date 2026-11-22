// Laura Carvalho - 25014543

package agromonitor.comum.plantio;

import java.io.Serializable;

// Uma linha da tela Meus Plantios. Nao e um Comunicado: viaja dentro da
// RespostaDeListagemDePlantios. As datas vao como texto "yyyy-MM-dd".
public class PlantioResumo implements Serializable
{
    // DTO serializável usado para enviar à interface apenas os dados resumidos do plantio.
    // Versao da classe para a serializacao; precisa ser a mesma no servidor e no backend.
    private static final long serialVersionUID = 1L;

    private int    codigo;
    private String nome;
    private String cultura;
    private String talhao;
    private double areaPlantada;          // hectares
    private String dataPlantio;
    private int    quantidadeAtividades;  // total de atividades registradas neste plantio
    private String proximaAtividadeTipo;  // null se nao ha atividade pendente
    private String proximaAtividadeData;  // null se nao ha atividade pendente
    private int    progresso;
    // A fase é derivada do progresso e usada pela interface para indicar o estágio do cultivo.
    private String status;                // FASE_INICIAL, DESENVOLVIMENTO, FASE_FINAL, CONCLUIDO
    private String localizacao;
    
    // Recebe todos os campos de uma vez; o objeto e montado pelo servidor.
    public PlantioResumo(int codigo, String nome, String cultura, String talhao, double areaPlantada,
                         String dataPlantio, int quantidadeAtividades,
                         String proximaAtividadeTipo, String proximaAtividadeData,
                         int progresso, String status, String localizacao)
    {
        // O construtor reúne em um único objeto os campos exibidos no resumo do plantio.
        this.codigo               = codigo;
        this.nome                 = nome;
        this.cultura              = cultura;
        this.talhao               = talhao;
        this.areaPlantada         = areaPlantada;
        this.dataPlantio          = dataPlantio;
        this.quantidadeAtividades = quantidadeAtividades;
        this.proximaAtividadeTipo = proximaAtividadeTipo;
        this.proximaAtividadeData = proximaAtividadeData;
        this.progresso            = progresso;
        this.status               = status;
        this.localizacao          = localizacao;
    }

    // So getters: o resumo e somente leitura depois de criado.
    public int    getCodigo()               { return this.codigo; }
    public String getNome()                 { return this.nome; }
    public String getCultura()              { return this.cultura; }
    public String getTalhao()               { return this.talhao; }
    public double getAreaPlantada()         { return this.areaPlantada; }
    public String getDataPlantio()          { return this.dataPlantio; }
    public int    getQuantidadeAtividades() { return this.quantidadeAtividades; }
    public String getProximaAtividadeTipo() { return this.proximaAtividadeTipo; }
    public String getProximaAtividadeData() { return this.proximaAtividadeData; }
    public int    getProgresso()            { return this.progresso; }
    public String getStatus()               { return this.status; }
    public String getLocalizacao()          { return this.localizacao; }
}