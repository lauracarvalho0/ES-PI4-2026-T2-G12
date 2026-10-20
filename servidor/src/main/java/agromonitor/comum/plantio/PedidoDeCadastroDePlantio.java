package agromonitor.comum.plantio;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: pede para cadastrar um plantio (assistente "Novo Plantio").
// As datas viajam como texto "yyyy-MM-dd" e o servidor e quem valida tudo.
public class PedidoDeCadastroDePlantio extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int     usuarioCodigo;   // dono do plantio
    private String  cultura;
    private String  nome;
    private String  safra;
    private String  descricao;      // opcional
    private String  sistemaCultivo; // SEQUEIRO ou IRRIGADO
    private String  talhao;
    private Double  areaPlantada;   // hectares
    private String  localizacao;    // opcional
    private Integer altitude;       // opcional, metros
    private String  dataPlantio;
    private String  metodoPlantio;  // opcional: PLANTIO_DIRETO, CONVENCIONAL, MINIMO

    // ajustes manuais (opcionais) das datas previstas; vazio = usa a previsao automatica
    private String ajustePlantio;
    private String ajusteIrrigacao;
    private String ajusteAdubacao;
    private String ajusteTratos;

    public PedidoDeCadastroDePlantio(int usuarioCodigo, String cultura, String nome, String safra,
                                     String descricao, String sistemaCultivo, String talhao,
                                     Double areaPlantada, String localizacao, Integer altitude,
                                     String dataPlantio, String metodoPlantio,
                                     String ajustePlantio, String ajusteIrrigacao,
                                     String ajusteAdubacao, String ajusteTratos)
    {
        this.usuarioCodigo   = usuarioCodigo;
        this.cultura         = cultura;
        this.nome            = nome;
        this.safra           = safra;
        this.descricao       = descricao;
        this.sistemaCultivo  = sistemaCultivo;
        this.talhao          = talhao;
        this.areaPlantada    = areaPlantada;
        this.localizacao     = localizacao;
        this.altitude        = altitude;
        this.dataPlantio     = dataPlantio;
        this.metodoPlantio   = metodoPlantio;
        this.ajustePlantio   = ajustePlantio;
        this.ajusteIrrigacao = ajusteIrrigacao;
        this.ajusteAdubacao  = ajusteAdubacao;
        this.ajusteTratos    = ajusteTratos;
    }

    public int     getUsuarioCodigo()   { return this.usuarioCodigo; }
    public String  getCultura()         { return this.cultura; }
    public String  getNome()            { return this.nome; }
    public String  getSafra()           { return this.safra; }
    public String  getDescricao()       { return this.descricao; }
    public String  getSistemaCultivo()  { return this.sistemaCultivo; }
    public String  getTalhao()          { return this.talhao; }
    public Double  getAreaPlantada()    { return this.areaPlantada; }
    public String  getLocalizacao()     { return this.localizacao; }
    public Integer getAltitude()        { return this.altitude; }
    public String  getDataPlantio()     { return this.dataPlantio; }
    public String  getMetodoPlantio()   { return this.metodoPlantio; }
    public String  getAjustePlantio()   { return this.ajustePlantio; }
    public String  getAjusteIrrigacao() { return this.ajusteIrrigacao; }
    public String  getAjusteAdubacao()  { return this.ajusteAdubacao; }
    public String  getAjusteTratos()    { return this.ajusteTratos; }

    @Override
    public String toString()
    {
        return "PedidoDeCadastroDePlantio[usuario=" + usuarioCodigo + ", nome=" + nome + ", cultura=" + cultura + "]";
    }
}
