package agromonitor.comum.plantio;

import java.io.Serializable;

// Uma linha da tela Meus Plantios. Nao e um Comunicado: viaja dentro da
// RespostaDeListagemDePlantios. As datas vao como texto "yyyy-MM-dd".
public class PlantioResumo implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int    codigo;
    private String nome;
    private String cultura;
    private String talhao;
    private double areaPlantada;
    private String dataPlantio;
    private int    quantidadeAtividades;
    private String proximaAtividadeTipo;  // null se nao ha atividade pendente
    private String proximaAtividadeData;  // null se nao ha atividade pendente
    private int    progresso;
    private String status;                // FASE_INICIAL, DESENVOLVIMENTO, FASE_FINAL, CONCLUIDO

    public PlantioResumo(int codigo, String nome, String cultura, String talhao, double areaPlantada,
                         String dataPlantio, int quantidadeAtividades,
                         String proximaAtividadeTipo, String proximaAtividadeData,
                         int progresso, String status)
    {
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
    }

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
}
