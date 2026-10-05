package br.com.agendafacil.service;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.*;
import br.com.agendafacil.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Coração do sistema: toda criação, remarcação e cancelamento de agendamento
 * DEVE passar por aqui. Nunca salve um Agendamento direto pelo repository
 * em um controller, senão a validação de conflitos é pulada.
 */
@Service
public class AgendamentoService {

    /** De quanto em quanto tempo a tela oferece horários (09:00, 09:30, 10:00...). */
    private static final int INTERVALO_HORARIOS_MINUTOS = 30;

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicoRepository servicoRepository;
    private final HorarioTrabalhoRepository horarioTrabalhoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final long horasAntecedenciaCancelamento;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              UsuarioRepository usuarioRepository,
                              ServicoRepository servicoRepository,
                              HorarioTrabalhoRepository horarioTrabalhoRepository,
                              BloqueioRepository bloqueioRepository,
                              @Value("${agendafacil.cancelamento.horas-antecedencia:24}") long horasAntecedenciaCancelamento) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicoRepository = servicoRepository;
        this.horarioTrabalhoRepository = horarioTrabalhoRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.horasAntecedenciaCancelamento = horasAntecedenciaCancelamento;
    }

    // ==================================================================
    // Operações principais
    // ==================================================================

    @Transactional
    public Agendamento agendar(Long clienteId, Long profissionalId, Long servicoId,
                               LocalDateTime inicio, String observacoesCliente) {
        Usuario cliente = buscarUsuario(clienteId);
        Usuario profissional = buscarUsuario(profissionalId);
        if (profissional.getPerfil() != Perfil.PROFISSIONAL) {
            throw new RegraNegocioException("O usuário escolhido não é um profissional.");
        }

        Servico servico = buscarServico(servicoId);
        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());

        validarHorario(profissional.getId(), cliente.getId(), inicio, fim, null);

        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setInicio(inicio);
        agendamento.setFim(fim);
        agendamento.setValor(servico.getPreco());
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setObservacoesCliente(observacoesCliente);
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento remarcar(Long agendamentoId, LocalDateTime novoInicio) {
        Agendamento agendamento = buscarAgendamento(agendamentoId);
        verificarSePodeAlterar(agendamento);

        LocalDateTime novoFim = novoInicio.plusMinutes(agendamento.getServico().getDuracaoMinutos());
        validarHorario(agendamento.getProfissional().getId(), agendamento.getCliente().getId(),
                novoInicio, novoFim, agendamento.getId());

        agendamento.setInicio(novoInicio);
        agendamento.setFim(novoFim);
        agendamento.setLembreteEnviado(false);
        return agendamento;
    }

    /**
     * Cancelamento feito pelo CLIENTE: respeita a antecedência mínima configurada
     * em application.properties (agendafacil.cancelamento.horas-antecedencia).
     */
    @Transactional
    public void cancelar(Long agendamentoId) {
        Agendamento agendamento = buscarAgendamento(agendamentoId);
        verificarSePodeAlterar(agendamento);
        agendamento.setStatus(StatusAgendamento.CANCELADO);
    }

    /**
     * Lista os horários de início livres para um serviço em um dia.
     * É isso que a tela de agendamento mostra ao cliente.
     */
    @Transactional(readOnly = true)
    public List<LocalDateTime> horariosDisponiveis(Long profissionalId, Long servicoId, LocalDate data, Long id) {
        Servico servico = buscarServico(servicoId);
        int duracao = servico.getDuracaoMinutos();
        LocalDateTime agora = LocalDateTime.now();
        List<LocalDateTime> livres = new ArrayList<>();

        List<HorarioTrabalho> expedientes =
                horarioTrabalhoRepository.findByProfissionalIdAndDiaSemanaOrderByHoraInicio(profissionalId, data.getDayOfWeek());

        for (HorarioTrabalho expediente : expedientes) {
            LocalDateTime horario = data.atTime(expediente.getHoraInicio());
            LocalDateTime limite = data.atTime(expediente.getHoraFim());

            while (!horario.plusMinutes(duracao).isAfter(limite)) {
                LocalDateTime fim = horario.plusMinutes(duracao);
                // Simples e suficiente para o MVP: uma consulta por horário.
                if (horario.isAfter(agora) && estaLivre(profissionalId, horario, fim)) {
                    livres.add(horario);
                }
                horario = horario.plusMinutes(INTERVALO_HORARIOS_MINUTOS);
            }
        }
        return livres;
    }

    // ==================================================================
    // Regras de validação
    // ==================================================================

    private void validarHorario(Long profissionalId, Long clienteId,
                                LocalDateTime inicio, LocalDateTime fim, Long ignorarId) {
        if (inicio == null) {
            throw new RegraNegocioException("Informe a data e o horário do agendamento.");
        }
        if (!inicio.isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("Não é possível agendar em um horário que já passou.");
        }
        if (!inicio.toLocalDate().equals(fim.toLocalDate())) {
            throw new RegraNegocioException("O atendimento precisa começar e terminar no mesmo dia.");
        }
        if (!dentroDoExpediente(profissionalId, inicio, fim)) {
            throw new RegraNegocioException("Este horário está fora do expediente do profissional.");
        }
        if (bloqueioRepository.contarSobreposicoes(profissionalId, inicio, fim) > 0) {
            throw new RegraNegocioException("O profissional não atende neste horário.");
        }
        if (agendamentoRepository.existeConflitoProfissional(profissionalId, inicio, fim, ignorarId)) {
            throw new RegraNegocioException("Este horário já está ocupado. Escolha outro horário.");
        }
        if (agendamentoRepository.existeConflitoCliente(clienteId, inicio, fim, ignorarId)) {
            throw new RegraNegocioException("Você já tem outro agendamento neste horário.");
        }
    }

    private boolean estaLivre(Long profissionalId, LocalDateTime inicio, LocalDateTime fim) {
        return bloqueioRepository.contarSobreposicoes(profissionalId, inicio, fim) == 0
                && !agendamentoRepository.existeConflitoProfissional(profissionalId, inicio, fim, null);
    }

    /** O atendimento inteiro precisa caber dentro de UM período de trabalho. */
    private boolean dentroDoExpediente(Long profissionalId, LocalDateTime inicio, LocalDateTime fim) {
        return horarioTrabalhoRepository
                .findByProfissionalIdAndDiaSemanaOrderByHoraInicio(profissionalId, inicio.getDayOfWeek())
                .stream()
                .anyMatch(h -> !inicio.toLocalTime().isBefore(h.getHoraInicio())
                        && !fim.toLocalTime().isAfter(h.getHoraFim()));
    }

    private void verificarSePodeAlterar(Agendamento agendamento) {
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO
                || agendamento.getStatus() == StatusAgendamento.CONCLUIDO
                || agendamento.getStatus() == StatusAgendamento.FALTOU) {
            throw new RegraNegocioException("Este agendamento não pode mais ser alterado.");
        }
        LocalDateTime prazo = agendamento.getInicio().minusHours(horasAntecedenciaCancelamento);
        if (LocalDateTime.now().isAfter(prazo)) {
            throw new RegraNegocioException("Cancelamentos e remarcações precisam ser feitos com pelo menos "
                    + horasAntecedenciaCancelamento + " horas de antecedência.");
        }
    }

    // ==================================================================
    // Auxiliares
    // ==================================================================

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
    }

    private Servico buscarServico(Long id) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Serviço não encontrado."));
        if (!servico.isAtivo()) {
            throw new RegraNegocioException("Este serviço não está mais disponível.");
        }
        return servico;
    }

    private Agendamento buscarAgendamento(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Agendamento não encontrado."));
    }

    public List<Agendamento> agenda(Long alvoId, LocalDate de, LocalDate ate) {
        return List.of();
    }

    public void buscarDoCliente(Long id, Long id1) {
    }

    public boolean podeConfirmar(Agendamento agendamento) {
        return false;
    }

    public boolean podeCancelarPeloProfissional(Agendamento agendamento) {
        return false;
    }

    public void verificarSeClientePodeAlterar(Agendamento agendamento) {
    }

    public List<Agendamento> agendamentosDoCliente(Long id) {
        return List.of();
    }

    public void confirmar(Long id) {
    }

    public void cancelarPeloProfissional(Long id) {
    }

    public void buscarParaGestao(Long agendamentoId, Usuario usuario) {
    }

    public boolean clientePodeAlterar(Agendamento agendamento) {
        return false;
    }

    public Object servicosAtivos() {
        return null;
    }

    public Object profissionaisAtivos() {
        return null;
    }
}
