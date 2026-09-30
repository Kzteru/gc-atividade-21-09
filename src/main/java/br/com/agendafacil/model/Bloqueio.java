package br.com.agendafacil.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Período em que o profissional NÃO atende, mesmo estando dentro do expediente:
 * limpeza, pausa, consulta médica, compromisso pessoal etc.
 */
@Entity
@Table(name = "bloqueios")
public class Bloqueio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private Usuario profissional;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime inicio;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fim;

    private String motivo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getProfissional() { return profissional; }
    public void setProfissional(Usuario profissional) { this.profissional = profissional; }

    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }

    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
