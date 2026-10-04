package br.com.agendafacil.service;

import br.com.agendafacil.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Ensina o Spring Security a buscar o usuário no nosso banco pelo e-mail. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
       // Mesmo formato usado no cadastro: "Ana@Email.com " vira "ana@email.com"
        return usuarioRepository.findByEmail(UsuarioService.normalizarEmail(email))
                .filter(u -> u.isAtivo())
                .map(u -> User.withUsername(u.getEmail())
                        .password(u.getSenha())
                        .roles(u.getPerfil().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }
}