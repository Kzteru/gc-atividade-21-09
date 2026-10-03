package br.com.agendafacil.config;

import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.Servico;
import br.com.agendafacil.model.StatusAgendamento;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.repository.AgendamentoRepository;
import br.com.agendafacil.repository.ServicoRepository;
import br.com.agendafacil.repository.UsuarioRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

/**
 * Dupla 4: cria alguns atendimentos JÁ PASSADOS para o histórico e os relatórios
 * terem o que mostrar logo na primeira execução.
 *
 * Roda depois do DadosIniciais (no ApplicationReadyEvent) e só quando ainda
 * não existe nenhum agendamento no banco. Como são datas no passado, eles são
 * gravados direto pelo repository (o AgendamentoService só aceita horários futuros).
 */
@Component
public class DadosExemploHistorico {

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicoRepository servicoRepository;

    public DadosExemploHistorico(AgendamentoRepository agendamentoRepository,
                                 UsuarioRepository usuarioRepository,
                                 ServicoRepository servicoRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicoRepository = servicoRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void criarAtendimentosPassados() {
        if (agendamentoRepository.count() > 0) {
            return;
        }
        Optional<Usuario> cliente = usuarioRepository.findByEmail("cliente@agendafacil.com");
        Optional<Usuario> profissional = usuarioRepository.findByEmail("profissional@agendafacil.com");
        List<Servico> servicos = servicoRepository.findByAtivoTrueOrderByNome();
        if (cliente.isEmpty() || profissional.isEmpty() || servicos.isEmpty()) {
            return;
        }

        Servico corte = buscar(servicos, "Corte de cabelo");
        Servico barba = buscar(servicos, "Barba");
        Servico corteEBarba = buscar(servicos, "Corte e barba");

        // Sempre em dias úteis passados, dentro do expediente (09-12 / 13-18)
        LocalDate ultimaSegunda = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.MONDAY));

        criar(cliente.get(), profissional.get(), corte, ultimaSegunda.minusWeeks(3).atTime(10, 0),
                StatusAgendamento.CONCLUIDO, "Degradê baixo, tesoura em cima.");
        criar(cliente.get(), profissional.get(), corteEBarba, ultimaSegunda.minusWeeks(2).plusDays(2).atTime(14, 0),
                StatusAgendamento.CONCLUIDO, "Barba desenhada. Pediu para voltar em 15 dias.");
        criar(cliente.get(), profissional.get(), barba, ultimaSegunda.minusWeeks(1).plusDays(1).atTime(9, 30),
                StatusAgendamento.FALTOU, null);
        // Este fica em aberto de propósito: serve para testar os botões "Concluído" / "Faltou"
        criar(cliente.get(), profissional.get(), corte, ultimaSegunda.atTime(11, 0),
                StatusAgendamento.AGENDADO, null);
    }

    private Servico buscar(List<Servico> servicos, String nome) {
        return servicos.stream().filter(s -> s.getNome().equals(nome)).findFirst().orElse(servicos.get(0));
    }

    private void criar(Usuario cliente, Usuario profissional, Servico servico, LocalDateTime inicio,
                       StatusAgendamento status, String anotacoes) {
        Agendamento a = new Agendamento();
        a.setCliente(cliente);
        a.setProfissional(profissional);
        a.setServico(servico);
        a.setInicio(inicio);
        a.setFim(inicio.plusMinutes(servico.getDuracaoMinutos()));
        a.setValor(servico.getPreco());
        a.setStatus(status);
        a.setAnotacoesProfissional(anotacoes);
        a.setCriadoEm(inicio.minusDays(2));
        agendamentoRepository.save(a);
    }
}
