// Giovana Uchelli - 25008818

package agromonitor.comum.conta;

import agromonitor.comum.Comunicado;

// Cliente -> Servidor: pede os dados da conta (tela Configuracoes).
public class PedidoDeBuscaDePerfil extends Comunicado
{
    private static final long serialVersionUID = 1L;

    private int usuarioCodigo;

    public PedidoDeBuscaDePerfil(int usuarioCodigo)
    {
        this.usuarioCodigo = usuarioCodigo;
    }

    public int getUsuarioCodigo() { return this.usuarioCodigo; }

    @Override
    public String toString() { return "PedidoDeBuscaDePerfil[usuarioCodigo=" + usuarioCodigo + "]"; }
}
