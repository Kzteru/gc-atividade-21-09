package br.com.agendafacil.service;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.Servico;
import br.com.agendafacil.model.StatusAgendamento;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.repository.ServicoRepository;
import br.com.agendafacil.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dupla 3: testes das telas de agendamento (Meus agendamentos, Agendar, Remarcar e Minha agenda).
 * Usa os dados de DadosIniciais (expediente seg-sex 09-12 e 13-18).
 */
@SpringBootTest
@Transactional
class AgendaServiceTest {

    @Autowired private AgendamentoService service;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ServicoRepository servicoRepository;

    private Usuario cliente;
    private Usuario outroCliente;
    private Usuario profissional;
    private Usuario admin;
    private Servico corte; // 30 minutos

    @BeforeEach
    void preparar() {
        cliente = usuarioRepository.findByEmail("cliente@agendafacil.com").orElseThrow();
        profissional = usuarioRepository.findByEmail("profissional@agendafacil.com").orElseThrow();
        admin = usuarioRepository.findByEmail("admin@agendafacil.com").orElseThrow();
        corte = servicoRepository.findByAtivoTrueOrderByNome().stream()
                .filter(s -> s.getNome().equals("Corte de cabelo"))
                .findFirst().orElseThrow();

        outroCliente = new Usuario();
        outroCliente.setNome("Bruno Cliente");
        outroCliente.setEmail("bruno.agenda@teste.com");
        outroCliente.setSenha("x");
        outroCliente.setPerfil(Perfil.CLIENTE);
        outroCliente = usuarioRepository.save(outroCliente);
    }

    private LocalDate segundaFutura() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
    }

    private LocalDateTime segundaAs(int hora, int minuto) {
        return segundaFutura().atTime(hora, minuto);
    }

    private Agendamento agendar(Usuario quem, LocalDateTime inicio) {
        return service.agendar(quem.getId(), profissional.getId(), corte.getId(), inicio, null);
    }

    // ---------------- Cliente ----------------

    @Test
    void clienteNaoAcessaAgendamentoDeOutroCliente() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));
        assertDoesNotThrow(() -> service.buscarDoCliente(a.getId(), cliente.getId()));
        assertThrows(RegraNegocioException.class, () -> service.buscarDoCliente(a.getId(), outroCliente.getId()));
    }

    @Test
    void clientePodeAlterarComAntecedencia() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));
        assertTrue(service.clientePodeAlterar(a));
        service.cancelar(a.getId());
        assertFalse(service.clientePodeAlterar(a));
    }

    @Test
    void remarcacaoContinuaOferecendoOProprioHorario() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));

        List<LocalDateTime> semIgnorar = service.horariosDisponiveis(profissional.getId(), corte.getId(), segundaFutura(), agendamento.getId());
        List<LocalDateTime> ignorando = service.horariosDisponiveis(profissional.getId(), corte.getId(), segundaFutura(), a.getId());

        assertFalse(semIgnorar.contains(segundaAs(10, 0)));
        assertTrue(ignorando.contains(segundaAs(10, 0)));
    }

    @Test
    void deveRemarcarParaHorarioLivre() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));
        service.remarcar(a.getId(), segundaAs(15, 0));
        assertEquals(segundaAs(15, 30), a.getFim());
        assertTrue(service.horariosDisponiveis(profissional.getId(), corte.getId(), segundaFutura(), agendamento.getId()).contains(segundaAs(10, 0)));
    }

    @Test
    void naoDeveAgendarComProfissionalInativo() {
        profissional.setAtivo(false);
        assertThrows(RegraNegocioException.class, () -> agendar(cliente, segundaAs(10, 0)));
        assertFalse(service.profissionaisAtivos().contains(profissional));
    }

    // ---------------- Profissional e admin ----------------

    @Test
    void profissionalConfirmaAgendamento() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));
        service.confirmar(a.getId());
        assertEquals(StatusAgendamento.CONFIRMADO, a.getStatus());
        // não confirma duas vezes
        assertThrows(RegraNegocioException.class, () -> service.confirmar(a.getId()));
    }

    @Test
    void profissionalCancelaELiberaOHorario() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));
        service.cancelarPeloProfissional(a.getId());
        assertEquals(StatusAgendamento.CANCELADO, a.getStatus());
        assertDoesNotThrow(() -> agendar(outroCliente, segundaAs(10, 0)));
        assertThrows(RegraNegocioException.class, () -> service.cancelarPeloProfissional(a.getId()));
    }

    @Test
    void naoConfirmaAgendamentoCancelado() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));
        service.cancelar(a.getId());
        assertThrows(RegraNegocioException.class, () -> service.confirmar(a.getId()));
    }

    @Test
    void somenteDonoOuAdminGerenciamOAgendamento() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));

        Usuario outroProfissional = new Usuario();
        outroProfissional.setNome("Outra Profissional");
        outroProfissional.setEmail("outra.prof@teste.com");
        outroProfissional.setSenha("x");
        outroProfissional.setPerfil(Perfil.PROFISSIONAL);
        Usuario outro = usuarioRepository.save(outroProfissional);

        assertDoesNotThrow(() -> service.buscarParaGestao(a.getId(), profissional));
        assertDoesNotThrow(() -> service.buscarParaGestao(a.getId(), admin));
        assertThrows(RegraNegocioException.class, () -> service.buscarParaGestao(a.getId(), outro));
        assertThrows(RegraNegocioException.class, () -> service.buscarParaGestao(a.getId(), cliente));
    }

    @Test
    void agendaListaPorProfissionalEParaTodos() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));
        Agendamento b = agendar(outroCliente, segundaAs(11, 0));

        List<Agendamento> doDia = service.agenda(profissional.getId(), segundaFutura(), segundaFutura());
        assertEquals(List.of(a, b), doDia);

        List<Agendamento> todos = service.agenda(null, segundaFutura(), segundaFutura().plusDays(6));
        assertTrue(todos.containsAll(List.of(a, b)));

        assertTrue(service.agenda(profissional.getId(), segundaFutura().plusDays(1), segundaFutura().plusDays(1)).isEmpty());
    }
}
