package br.com.agendafacil.repository;

import br.com.agendafacil.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByAtivoTrueOrderByNome();
}
