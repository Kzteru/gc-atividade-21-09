package br.com.agendafacil.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario cliente;

    @ManyToOne(optional = false)
    private Usuario profissional;

    @ManyToOne(optional = false)
    private Servico servico;

    @Column(nullable = false)
    private LocalDateTime inicio;

    /** Calculado automaticamente: inicio + duração do serviço. */
    @Column(nullable = false)
    private LocalDateTime fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    /** Valor copiado do serviço no momento da marcação (o preço pode mudar depois). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    /** Observação escrita pelo cliente ao agendar. */
    @Column(length = 500)
    private String observacoesCliente;

    /** Anotações do profissional sobre o atendimento (formam o histórico do cliente). */
    @Column(length = 1000)
    private String anotacoesProfissional;

    private boolean lembreteEnviado = false;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }

    public Usuario getProfissional() { return profissional; }
    public void setProfissional(Usuario profissional) { this.profissional = profissional; }

    public Servico getServico() { return servico; }
    public void setServico(Servico servico) { this.servico = servico; }

    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }

    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }

    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getObservacoesCliente() { return observacoesCliente; }
    public void setObservacoesCliente(String observacoesCliente) { this.observacoesCliente = observacoesCliente; }

    public String getAnotacoesProfissional() { return anotacoesProfissional; }
    public void setAnotacoesProfissional(String anotacoesProfissional) { this.anotacoesProfissional = anotacoesProfissional; }

    public boolean isLembreteEnviado() { return lembreteEnviado; }
    public void setLembreteEnviado(boolean lembreteEnviado) { this.lembreteEnviado = lembreteEnviado; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
