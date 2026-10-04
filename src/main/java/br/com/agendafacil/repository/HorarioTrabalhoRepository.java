package br.com.agendafacil.repository;

import br.com.agendafacil.model.HorarioTrabalho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HorarioTrabalhoRepository extends JpaRepository<HorarioTrabalho, Long> {

    List<HorarioTrabalho> findByProfissionalIdAndDiaSemanaOrderByHoraInicio(Long profissionalId, DayOfWeek diaSemana);

    List<HorarioTrabalho> findByProfissionalIdOrderByDiaSemanaAscHoraInicioAsc(Long profissionalId);

    @Query("select count(h) from HorarioTrabalho h where h.profissional.id = :profissionalId " +
            "and h.diaSemana = :diaSemana and h.horaInicio < :horaFim and h.horaFim > :horaInicio " +
            "and (:ignorarId is null or h.id <> :ignorarId)")
    long contarSobreposicoes(@Param("profissionalId") Long profissionalId,
                             @Param("diaSemana") DayOfWeek diaSemana,
                             @Param("horaInicio") java.time.LocalTime horaInicio,
                             @Param("horaFim") java.time.LocalTime horaFim,
                             @Param("ignorarId") Long ignorarId);
}
