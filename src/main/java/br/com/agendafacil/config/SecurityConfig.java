package br.com.agendafacil.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Páginas públicas
                        .requestMatchers(antMatcher("/login"), antMatcher("/cadastro"),
                                antMatcher("/css/**"), antMatcher("/js/**"), antMatcher("/error")).permitAll()
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        // Áreas restritas por perfil (as telas ainda serão criadas pelas duplas)
                        .requestMatchers(antMatcher("/agenda/**"), antMatcher("/servicos/**"),
                                antMatcher("/horarios/**"), antMatcher("/bloqueios/**"))
                        .hasAnyRole("PROFISSIONAL", "ADMIN")
                        // Dupla 3: marcar, ver, cancelar e remarcar são telas do cliente
                        .requestMatchers(antMatcher("/agendamentos/**")).hasRole("CLIENTE")
                        .requestMatchers(antMatcher("/relatorios/**")).hasAnyRole("PROFISSIONAL", "ADMIN")
                        // Administração (dupla 1): cadastro de profissionais
                        .requestMatchers(antMatcher("/admin/**")).hasRole("ADMIN")
                        // Todo o resto exige login
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?saiu")
                        .permitAll())
                // Necessário apenas para o console do H2 funcionar no navegador
                .csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
