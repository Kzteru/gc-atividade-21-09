package br.com.agendafacil.service;

import br.com.agendafacil.dto.ResumoFaturamento;
import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.Perfil;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dupla 4: relatório de faturamento.
 * Usa datas de 2020 para não misturar com os atendimentos de exemplo criados na inicialização.
 *
 * Cenário (janeiro/2020):
 *   Carlos (profissional do seed): Corte R$ 40 concluído + Barba R$ 30 concluído + Corte com FALTA
 *   Diego (outro profissional):    Corte e barba R$ 65 concluído
 *   Fevereiro/2020: mais um Corte R$ 40 concluído do Carlos (fora do período de janeiro)
 */
@SpringBootTest
@Transactional
class RelatorioServiceTest {

    private static final LocalDate JAN_INICIO = LocalDate.of(2020, 1, 1);
    private static final LocalDate JAN_FIM = LocalDate.of(2020, 1, 31);

    @Autowired private RelatorioService relatorioService;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ServicoRepository servicoRepository;

    private Usuario cliente;
    private Usuario carlos;
    private Usuario diego;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        cliente = usuarioRepository.findByEmail("cliente@agendafacil.com").orElseThrow();
        carlos = usuarioRepository.findByEmail("profissional@agendafacil.com").orElseThrow();
        admin = usuarioRepository.findByEmail("admin@agendafacil.com").orElseThrow();

        diego = new Usuario();
        diego.setNome("Diego Barbeiro");
        diego.setEmail("diego.relatorio@teste.com");
        diego.setSenha("x");
        diego.setPerfil(Perfil.PROFISSIONAL);
        diego = usuarioRepository.save(diego);

        Servico corte = servico("Corte de cabelo");          // R$ 40
        Servico barba = servico("Barba");                    // R$ 30
        Servico corteEBarba = servico("Corte e barba");      // R$ 65

        criar(carlos, corte, LocalDateTime.of(2020, 1, 6, 10, 0), StatusAgendamento.CONCLUIDO);
        criar(carlos, barba, LocalDateTime.of(2020, 1, 7, 10, 0), StatusAgendamento.CONCLUIDO);
        criar(carlos, corte, LocalDateTime.of(2020, 1, 8, 10, 0), StatusAgendamento.FALTOU);
        criar(diego, corteEBarba, LocalDateTime.of(2020, 1, 6, 14, 0), StatusAgendamento.CONCLUIDO);
        criar(carlos, corte, LocalDateTime.of(2020, 2, 3, 10, 0), StatusAgendamento.CONCLUIDO);
    }

    // ---------------- Profissional ----------------

    @Test
    void profissionalVeSoOProprioFaturamento() {
        ResumoFaturamento r = relatorioService.faturamento(carlos, JAN_INICIO, JAN_FIM, null);

        assertValor("70.00", r.getTotal());          // 40 + 30; a falta e o Diego não entram
        assertEquals(2, r.getQuantidade());
        assertValor("35.00", r.getTicketMedio());
    }

    @Test
    void profissionalNaoVeOColegaMesmoMandandoOIdDele() {
        // Simula alguém editando a URL: /relatorios?profissionalId=<id do Diego>
        ResumoFaturamento r = relatorioService.faturamento(carlos, JAN_INICIO, JAN_FIM, diego.getId());

        assertValor("70.00", r.getTotal());          // continua vendo só o próprio, e não os R$ 65 do Diego
        assertTrue(r.getPorProfissional().isEmpty(), "Profissional não pode ver a divisão por profissional");
    }

    @Test
    void profissionalNaoRecebeDivisaoPorProfissional() {
        ResumoFaturamento r = relatorioService.faturamento(carlos, JAN_INICIO, JAN_FIM, null);
        assertTrue(r.getPorProfissional().isEmpty());
    }

    @Test
    void agrupaPorServicoDoMaiorParaOMenor() {
        ResumoFaturamento r = relatorioService.faturamento(carlos, JAN_INICIO, JAN_FIM, null);

        assertEquals(2, r.getPorServico().size());
        assertEquals("Corte de cabelo", r.getPorServico().get(0).getNome());
        assertValor("40.00", r.getPorServico().get(0).getTotal());
        assertEquals("Barba", r.getPorServico().get(1).getNome());
    }

    @Test
    void respeitaOPeriodoEscolhido() {
        ResumoFaturamento r = relatorioService.faturamento(carlos,
                LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29), null);

        assertValor("40.00", r.getTotal());
        assertEquals(1, r.getQuantidade());
    }

    // ---------------- Admin ----------------

    @Test
    void adminVeTodosEADivisaoPorProfissional() {
        ResumoFaturamento r = relatorioService.faturamento(admin, JAN_INICIO, JAN_FIM, null);

        assertValor("135.00", r.getTotal());         // 70 do Carlos + 65 do Diego
        assertEquals(3, r.getQuantidade());
        assertEquals(2, r.getPorProfissional().size());
        assertEquals("Carlos Barbeiro", r.getPorProfissional().get(0).getNome());
    }

    @Test
    void adminFiltraPorUmProfissional() {
        ResumoFaturamento r = relatorioService.faturamento(admin, JAN_INICIO, JAN_FIM, diego.getId());

        assertValor("65.00", r.getTotal());
        assertEquals(1, r.getQuantidade());
        assertTrue(r.getPorProfissional().isEmpty());
    }

    @Test
    void adminNaoPodeFiltrarPorQuemNaoEProfissional() {
        assertThrows(RegraNegocioException.class,
                () -> relatorioService.faturamento(admin, JAN_INICIO, JAN_FIM, cliente.getId()));
    }

    // ---------------- Outros casos ----------------

    @Test
    void clienteNaoTemAcesso() {
        assertThrows(RegraNegocioException.class,
                () -> relatorioService.faturamento(cliente, JAN_INICIO, JAN_FIM, null));
    }

    @Test
    void dataInicialDepoisDaFinalEhRecusada() {
        assertThrows(RegraNegocioException.class,
                () -> relatorioService.faturamento(admin, JAN_FIM, JAN_INICIO, null));
    }

    @Test
    void periodoSemAtendimentosDevolveZero() {
        ResumoFaturamento r = relatorioService.faturamento(admin,
                LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31), null);

        assertValor("0", r.getTotal());
        assertEquals(0, r.getQuantidade());
        assertValor("0", r.getTicketMedio());
    }

    // ---------------- Auxiliares ----------------

    private Servico servico(String nome) {
        return servicoRepository.findByAtivoTrueOrderByNome().stream()
                .filter(s -> s.getNome().equals(nome))
                .findFirst().orElseThrow();
    }

    /** Grava direto no banco para poder usar datas no passado. */
    private void criar(Usuario profissional, Servico servico, LocalDateTime inicio, StatusAgendamento status) {
        Agendamento a = new Agendamento();
        a.setCliente(cliente);
        a.setProfissional(profissional);
        a.setServico(servico);
        a.setInicio(inicio);
        a.setFim(inicio.plusMinutes(servico.getDuracaoMinutos()));
        a.setValor(servico.getPreco());
        a.setStatus(status);
        agendamentoRepository.save(a);
    }

    /** Compara valores em dinheiro sem se importar com casas decimais (70 == 70.00). */
    private static void assertValor(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual),
                "Esperado R$ " + esperado + " mas veio R$ " + atual);
    }
}
