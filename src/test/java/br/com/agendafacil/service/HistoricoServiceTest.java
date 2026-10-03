package br.com.agendafacil.service;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.Servico;
import br.com.agendafacil.model.StatusAgendamento;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.repository.AgendamentoRepository;
import br.com.agendafacil.repository.ServicoRepository;
import br.com.agendafacil.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Dupla 4: regras do histórico (marcar concluído/faltou e anotações). */
@SpringBootTest
@Transactional
class HistoricoServiceTest {

    @Autowired private HistoricoService historicoService;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ServicoRepository servicoRepository;

    private Usuario cliente;
    private Usuario profissional;
    private Servico servico;

    @BeforeEach
    void preparar() {
        cliente = usuarioRepository.findByEmail("cliente@agendafacil.com").orElseThrow();
        profissional = usuarioRepository.findByEmail("profissional@agendafacil.com").orElseThrow();
        servico = servicoRepository.findByAtivoTrueOrderByNome().get(0);
    }

    /** Grava direto no banco para poder usar datas no passado. */
    private Agendamento criar(LocalDateTime inicio, StatusAgendamento status) {
        Agendamento a = new Agendamento();
        a.setCliente(cliente);
        a.setProfissional(profissional);
        a.setServico(servico);
        a.setInicio(inicio);
        a.setFim(inicio.plusMinutes(servico.getDuracaoMinutos()));
        a.setValor(servico.getPreco());
        a.setStatus(status);
        return agendamentoRepository.save(a);
    }

    @Test
    void deveMarcarAtendimentoPassadoComoConcluido() {
        Agendamento a = criar(LocalDateTime.now().minusDays(1), StatusAgendamento.AGENDADO);
        historicoService.registrarResultado(a.getId(), StatusAgendamento.CONCLUIDO);
        assertEquals(StatusAgendamento.CONCLUIDO, agendamentoRepository.findById(a.getId()).orElseThrow().getStatus());
    }

    @Test
    void deveRegistrarFalta() {
        Agendamento a = criar(LocalDateTime.now().minusHours(2), StatusAgendamento.CONFIRMADO);
        historicoService.registrarResultado(a.getId(), StatusAgendamento.FALTOU);
        assertEquals(StatusAgendamento.FALTOU, agendamentoRepository.findById(a.getId()).orElseThrow().getStatus());
    }

    @Test
    void naoDeveConcluirAtendimentoFuturo() {
        Agendamento a = criar(LocalDateTime.now().plusDays(3), StatusAgendamento.AGENDADO);
        assertThrows(RegraNegocioException.class,
                () -> historicoService.registrarResultado(a.getId(), StatusAgendamento.CONCLUIDO));
        assertFalse(historicoService.podeRegistrarResultado(a));
    }

    @Test
    void naoDeveAlterarAtendimentoCancelado() {
        Agendamento a = criar(LocalDateTime.now().minusDays(1), StatusAgendamento.CANCELADO);
        assertThrows(RegraNegocioException.class,
                () -> historicoService.registrarResultado(a.getId(), StatusAgendamento.CONCLUIDO));
    }

    @Test
    void soAceitaConcluidoOuFaltou() {
        Agendamento a = criar(LocalDateTime.now().minusDays(1), StatusAgendamento.AGENDADO);
        assertThrows(RegraNegocioException.class,
                () -> historicoService.registrarResultado(a.getId(), StatusAgendamento.CANCELADO));
    }

    @Test
    void deveSalvarAnotacoesDoProfissional() {
        Agendamento a = criar(LocalDateTime.now().minusDays(1), StatusAgendamento.CONCLUIDO);
        historicoService.salvarAnotacoes(a.getId(), "  Prefere máquina 2  ");
        assertEquals("Prefere máquina 2",
                agendamentoRepository.findById(a.getId()).orElseThrow().getAnotacoesProfissional());
    }

    @Test
    void historicoVemDoMaisRecenteParaOMaisAntigo() {
        criar(LocalDateTime.now().minusDays(400), StatusAgendamento.CONCLUIDO);
        Agendamento recente = criar(LocalDateTime.now().minusMinutes(30), StatusAgendamento.CONCLUIDO);
        assertEquals(recente.getId(), historicoService.historicoDoCliente(cliente.getId()).get(0).getId());
    }

    @Test
    void naoDeveAbrirHistoricoDeQuemNaoEhCliente() {
        assertThrows(RegraNegocioException.class, () -> historicoService.buscarCliente(profissional.getId()));
    }
}
