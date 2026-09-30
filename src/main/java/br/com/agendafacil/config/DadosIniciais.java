package br.com.agendafacil.config;

import br.com.agendafacil.model.*;
import br.com.agendafacil.repository.HorarioTrabalhoRepository;
import br.com.agendafacil.repository.ServicoRepository;
import br.com.agendafacil.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Cria dados de exemplo quando o banco está vazio, para todo mundo
 * conseguir testar sem cadastrar nada. Senha de todos: 123456
 */
@Component
public class DadosIniciais implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ServicoRepository servicoRepository;
    private final HorarioTrabalhoRepository horarioTrabalhoRepository;
    private final PasswordEncoder passwordEncoder;

    public DadosIniciais(UsuarioRepository usuarioRepository,
                         ServicoRepository servicoRepository,
                         HorarioTrabalhoRepository horarioTrabalhoRepository,
                         PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.servicoRepository = servicoRepository;
        this.horarioTrabalhoRepository = horarioTrabalhoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        criarUsuario("Administrador", "admin@agendafacil.com", Perfil.ADMIN);
        Usuario profissional = criarUsuario("Carlos Barbeiro", "profissional@agendafacil.com", Perfil.PROFISSIONAL);
        criarUsuario("Ana Cliente", "cliente@agendafacil.com", Perfil.CLIENTE);

        criarServico("Corte de cabelo", 30, "40.00");
        criarServico("Barba", 30, "30.00");
        criarServico("Corte e barba", 60, "65.00");

        // Segunda a sexta com pausa para almoço; sábado só de manhã
        for (DayOfWeek dia : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            criarHorario(profissional, dia, LocalTime.of(9, 0), LocalTime.of(12, 0));
            criarHorario(profissional, dia, LocalTime.of(13, 0), LocalTime.of(18, 0));
        }
        criarHorario(profissional, DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));
    }

    private Usuario criarUsuario(String nome, String email, Perfil perfil) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenha(passwordEncoder.encode("123456"));
        u.setPerfil(perfil);
        return usuarioRepository.save(u);
    }

    private void criarServico(String nome, int duracaoMinutos, String preco) {
        Servico s = new Servico();
        s.setNome(nome);
        s.setDuracaoMinutos(duracaoMinutos);
        s.setPreco(new BigDecimal(preco));
        servicoRepository.save(s);
    }

    private void criarHorario(Usuario profissional, DayOfWeek dia, LocalTime inicio, LocalTime fim) {
        HorarioTrabalho h = new HorarioTrabalho();
        h.setProfissional(profissional);
        h.setDiaSemana(dia);
        h.setHoraInicio(inicio);
        h.setHoraFim(fim);
        horarioTrabalhoRepository.save(h);
    }
}
