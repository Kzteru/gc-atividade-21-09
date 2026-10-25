package br.com.agendafacil.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado da tela de faturamento (dupla 4).
 * "porProfissional" só vem preenchido para o admin; para o profissional fica vazio,
 * porque ele não pode ver quanto os colegas faturaram.
 */
public class ResumoFaturamento {

    private final BigDecimal total;
    private final long quantidade;
    private final BigDecimal ticketMedio;
    private final List<LinhaFaturamento> porServico;
    private final List<LinhaFaturamento> porProfissional;

    public ResumoFaturamento(BigDecimal total, long quantidade, BigDecimal ticketMedio,
                             List<LinhaFaturamento> porServico, List<LinhaFaturamento> porProfissional) {
        this.total = total;
        this.quantidade = quantidade;
        this.ticketMedio = ticketMedio;
        this.porServico = porServico;
        this.porProfissional = porProfissional;
    }

    public BigDecimal getTotal() { return total; }
    public long getQuantidade() { return quantidade; }
    public BigDecimal getTicketMedio() { return ticketMedio; }
    public List<LinhaFaturamento> getPorServico() { return porServico; }
    public List<LinhaFaturamento> getPorProfissional() { return porProfissional; }
}
