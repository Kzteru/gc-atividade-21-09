package br.com.agendafacil.repository;

import br.com.agendafacil.model.HorarioTrabalho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface HorarioTrabalhoRepository extends JpaRepository<HorarioTrabalho, Long> {

    List<HorarioTrabalho> findByProfissionalIdAndDiaSemanaOrderByHoraInicio(Long profissionalId, DayOfWeek diaSemana);

    List<HorarioTrabalho> findByProfissionalIdOrderByDiaSemanaAscHoraInicioAsc(Long profissionalId);
}
