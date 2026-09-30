package br.com.agendafacil.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Um período de trabalho do profissional em um dia da semana.
 * Para ter pausa de almoço, cadastre DOIS períodos no mesmo dia
 * (ex.: segunda 09:00-12:00 e segunda 13:00-18:00).
 */
@Entity
@Table(name = "horarios_trabalho")
public class HorarioTrabalho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private Usuario profissional;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek diaSemana;

    @NotNull
    @Column(nullable = false)
    private LocalTime horaInicio;

    @NotNull
    @Column(nullable = false)
    private LocalTime horaFim;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getProfissional() { return profissional; }
    public void setProfissional(Usuario profissional) { this.profissional = profissional; }

    public DayOfWeek getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DayOfWeek diaSemana) { this.diaSemana = diaSemana; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
}
