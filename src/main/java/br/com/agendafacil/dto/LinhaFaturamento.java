package br.com.agendafacil.dto;

import java.math.BigDecimal;

/** Uma linha das tabelas de faturamento: um serviço ou um profissional, com quantos atendimentos e quanto rendeu. */
public class LinhaFaturamento {

    private final String nome;
    private final long quantidade;
    private final BigDecimal total;

    public LinhaFaturamento(String nome, long quantidade, BigDecimal total) {
        this.nome = nome;
        this.quantidade = quantidade;
        this.total = total;
    }

    public String getNome() { return nome; }
    public long getQuantidade() { return quantidade; }
    public BigDecimal getTotal() { return total; }
}
