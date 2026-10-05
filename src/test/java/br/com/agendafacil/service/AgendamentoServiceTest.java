package br.com.agendafacil.service;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.Servico;
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
 * Testes da regra mais importante do sistema.
 * Rode com: mvn test   (ou botão verde ao lado da classe no IntelliJ)
 * Usa os dados criados em DadosIniciais (expediente seg-sex 09-12 e 13-18).
 */
@SpringBootTest
@Transactional
class AgendamentoServiceTest {

    @Autowired private AgendamentoService service;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ServicoRepository servicoRepository;

    private Usuario cliente;
    private Usuario outroCliente;
    private Usuario profissional;
    private Servico corte; // 30 minutos

    @BeforeEach
    void preparar() {
        cliente = usuarioRepository.findByEmail("cliente@agendafacil.com").orElseThrow();
        profissional = usuarioRepository.findByEmail("profissional@agendafacil.com").orElseThrow();
        corte = servicoRepository.findByAtivoTrueOrderByNome().stream()
                .filter(s -> s.getNome().equals("Corte de cabelo"))
                .findFirst().orElseThrow();

        outroCliente = new Usuario();
        outroCliente.setNome("Bruno Cliente");
        outroCliente.setEmail("bruno@teste.com");
        outroCliente.setSenha("x");
        outroCliente.setPerfil(Perfil.CLIENTE);
        outroCliente = usuarioRepository.save(outroCliente);
    }

    /** Uma segunda-feira daqui a mais de uma semana: sempre no futuro e fora do prazo de cancelamento. */
    private LocalDate segundaFutura() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
    }

    private LocalDateTime segundaAs(int hora, int minuto) {
        return segundaFutura().atTime(hora, minuto);
    }

    private Agendamento agendar(Usuario quem, LocalDateTime inicio) {
        return service.agendar(quem.getId(), profissional.getId(), corte.getId(), inicio, null);
    }

    @Test
    void deveAgendarHorarioLivre() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));
        assertNotNull(a.getId());
        assertEquals(segundaAs(10, 30), a.getFim());
        assertEquals(corte.getPreco(), a.getValor());
    }

    @Test
    void naoDevePermitirMesmoHorarioParaOutroCliente() {
        agendar(cliente, segundaAs(10, 0));
        assertThrows(RegraNegocioException.class, () -> agendar(outroCliente, segundaAs(10, 0)));
    }

    @Test
    void naoDevePermitirSobreposicaoParcial() {
        agendar(cliente, segundaAs(10, 0));                 // 10:00 - 10:30
        assertThrows(RegraNegocioException.class, () -> agendar(outroCliente, segundaAs(10, 15)));
        assertThrows(RegraNegocioException.class, () -> agendar(outroCliente, segundaAs(9, 45)));
    }

    @Test
    void devePermitirHorarioQueComecaQuandoOutroTermina() {
        agendar(cliente, segundaAs(10, 0));                 // termina 10:30
        assertDoesNotThrow(() -> agendar(outroCliente, segundaAs(10, 30)));
    }

    @Test
    void naoDevePermitirForaDoExpediente() {
        assertThrows(RegraNegocioException.class, () -> agendar(cliente, segundaAs(12, 0))); // almoço
        assertThrows(RegraNegocioException.class, () -> agendar(cliente, segundaAs(11, 45))); // invade o almoço
        assertThrows(RegraNegocioException.class, () -> agendar(cliente, segundaAs(20, 0)));
        LocalDateTime domingo = segundaFutura().minusDays(1).atTime(10, 0);
        assertThrows(RegraNegocioException.class, () -> agendar(cliente, domingo));
    }

    @Test
    void naoDevePermitirAgendarNoPassado() {
        assertThrows(RegraNegocioException.class, () -> agendar(cliente, LocalDateTime.now().minusDays(1)));
    }

    @Test
    void horarioCanceladoVoltaASerLivre() {
        Agendamento a = agendar(cliente, segundaAs(10, 0));
        service.cancelar(a.getId());
        assertDoesNotThrow(() -> agendar(outroCliente, segundaAs(10, 0)));
    }

    @Test
    void horariosDisponiveisNaoMostramOcupadosNemAlmoco() {
        agendar(cliente, segundaAs(10, 0));
        List<LocalDateTime> livres = service.horariosDisponiveis(profissional.getId(), corte.getId(), segundaFutura(), agendamento.getId());

        assertTrue(livres.contains(segundaAs(9, 0)));
        assertFalse(livres.contains(segundaAs(10, 0)));
        assertTrue(livres.contains(segundaAs(10, 30)));
        assertFalse(livres.contains(segundaAs(12, 0)));
        assertTrue(livres.contains(segundaAs(17, 30)));
        assertFalse(livres.contains(segundaAs(18, 0)));
    }
}
