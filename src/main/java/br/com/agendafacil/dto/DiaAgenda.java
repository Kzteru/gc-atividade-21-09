package br.com.agendafacil.dto;

import br.com.agendafacil.model.Agendamento;

import java.time.LocalDate;
import java.util.List;

/** Um dia da tela "Minha agenda": a data, o texto do cabeçalho e os atendimentos daquele dia. */
public class DiaAgenda {

    private final LocalDate data;
    private final String rotulo;
    private final List<Agendamento> agendamentos;

    public DiaAgenda(LocalDate data, String rotulo, List<Agendamento> agendamentos) {
        this.data = data;
        this.rotulo = rotulo;
        this.agendamentos = agendamentos;
    }

    public LocalDate getData() { return data; }
    public String getRotulo() { return rotulo; }
    public List<Agendamento> getAgendamentos() { return agendamentos; }
    public boolean isHoje() { return data.equals(LocalDate.now()); }
}
