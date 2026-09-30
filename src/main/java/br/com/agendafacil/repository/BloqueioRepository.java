package br.com.agendafacil.repository;

import br.com.agendafacil.model.Bloqueio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {

    /**
     * Conta bloqueios que se sobrepõem ao intervalo [inicio, fim).
     * Dois intervalos se sobrepõem quando: inicioA < fimB  E  fimA > inicioB.
     */
    @Query("""
            select count(b) from Bloqueio b
            where b.profissional.id = :profissionalId
              and b.inicio < :fim
              and b.fim > :inicio
            """)
    long contarSobreposicoes(@Param("profissionalId") Long profissionalId,
                             @Param("inicio") LocalDateTime inicio,
                             @Param("fim") LocalDateTime fim);

    List<Bloqueio> findByProfissionalIdAndFimAfterOrderByInicio(Long profissionalId, LocalDateTime aPartirDe);
}
