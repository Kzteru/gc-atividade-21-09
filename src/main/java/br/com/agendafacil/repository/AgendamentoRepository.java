package br.com.agendafacil.repository;

import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // ------------------------------------------------------------------
    // Validação de conflitos (usado pelo AgendamentoService)
    // Regra de sobreposição: inicioA < fimB  E  fimA > inicioB.
    // Agendamentos CANCELADOS não ocupam horário.
    // "ignorarId" serve para a remarcação não conflitar consigo mesma.
    // ------------------------------------------------------------------

    @Query("""
            select count(a) from Agendamento a
            where a.profissional.id = :profissionalId
              and a.status <> :cancelado
              and a.inicio < :fim
              and a.fim > :inicio
              and (:ignorarId is null or a.id <> :ignorarId)
            """)
    long contarConflitosProfissional(@Param("profissionalId") Long profissionalId,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim,
                                     @Param("ignorarId") Long ignorarId,
                                     @Param("cancelado") StatusAgendamento cancelado);

    @Query("""
            select count(a) from Agendamento a
            where a.cliente.id = :clienteId
              and a.status <> :cancelado
              and a.inicio < :fim
              and a.fim > :inicio
              and (:ignorarId is null or a.id <> :ignorarId)
            """)
    long contarConflitosCliente(@Param("clienteId") Long clienteId,
                                @Param("inicio") LocalDateTime inicio,
                                @Param("fim") LocalDateTime fim,
                                @Param("ignorarId") Long ignorarId,
                                @Param("cancelado") StatusAgendamento cancelado);

    default boolean existeConflitoProfissional(Long profissionalId, LocalDateTime inicio, LocalDateTime fim, Long ignorarId) {
        return contarConflitosProfissional(profissionalId, inicio, fim, ignorarId, StatusAgendamento.CANCELADO) > 0;
    }

    default boolean existeConflitoCliente(Long clienteId, LocalDateTime inicio, LocalDateTime fim, Long ignorarId) {
        return contarConflitosCliente(clienteId, inicio, fim, ignorarId, StatusAgendamento.CANCELADO) > 0;
    }

    // ------------------------------------------------------------------
    // Consultas prontas para os outros módulos
    // ------------------------------------------------------------------

    /** Agenda do profissional em um período (dupla 3 / tela "Minha agenda"). */
    List<Agendamento> findByProfissionalIdAndInicioBetweenOrderByInicio(Long profissionalId,
                                                                        LocalDateTime de,
                                                                        LocalDateTime ate);

    /** Agenda de todos os profissionais em um período (tela "Minha agenda" do admin). */
    List<Agendamento> findByInicioBetweenOrderByInicio(LocalDateTime de, LocalDateTime ate);

    /** Agendamentos do cliente, do mais recente para o mais antigo (histórico - dupla 4). */
    List<Agendamento> findByClienteIdOrderByInicioDesc(Long clienteId);

    /** Lembretes ainda não enviados para atendimentos próximos (dupla 5). */
    List<Agendamento> findByStatusAndLembreteEnviadoFalseAndInicioBetween(StatusAgendamento status,
                                                                          LocalDateTime de,
                                                                          LocalDateTime ate);

    /** Faturamento: soma dos atendimentos CONCLUÍDOS em um período (dupla 4). */
    @Query("""
            select coalesce(sum(a.valor), 0) from Agendamento a
            where a.status = br.com.agendafacil.model.StatusAgendamento.CONCLUIDO
              and a.inicio >= :de
              and a.inicio < :ate
            """)
    BigDecimal faturamentoNoPeriodo(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);
}
