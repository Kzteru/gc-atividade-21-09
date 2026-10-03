package br.com.agendafacil.repository;

import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByPerfilAndAtivoTrueOrderByNome(Perfil perfil);

    /** Todos do perfil, inclusive inativos (tela de profissionais do admin). */
    List<Usuario> findByPerfilOrderByNome(Perfil perfil);
}