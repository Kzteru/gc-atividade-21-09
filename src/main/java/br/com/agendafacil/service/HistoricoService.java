package br.com.agendafacil.service;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.StatusAgendamento;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.repository.AgendamentoRepository;
import br.com.agendafacil.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dupla 4: histórico do cliente.
 * Mostra os atendimentos de um cliente, guarda as anotações do profissional
 * e marca cada atendimento como CONCLUÍDO ou FALTOU depois que ele acontece.
 */
@Service
public class HistoricoService {

    private static final int LIMITE_ANOTACOES = 1000;

    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;

    public HistoricoService(UsuarioRepository usuarioRepository, AgendamentoRepository agendamentoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarClientes() {
        return usuarioRepository.findByPerfilAndAtivoTrueOrderByNome(Perfil.CLIENTE);
    }

    @Transactional(readOnly = true)
    public Usuario buscarCliente(Long clienteId) {
        Usuario usuario = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado."));
        if (usuario.getPerfil() != Perfil.CLIENTE) {
            throw new RegraNegocioException("Este usuário não é um cliente.");
        }
        return usuario;
    }

    /** Atendimentos do cliente, do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public List<Agendamento> historicoDoCliente(Long clienteId) {
        return agendamentoRepository.findByClienteIdOrderByInicioDesc(clienteId);
    }

    /** Preferências gerais do cliente (ex.: "prefere máquina 2"). */
    @Transactional
    public void salvarObservacoesCliente(Long clienteId, String observacoes) {
        Usuario cliente = buscarCliente(clienteId);
        cliente.setObservacoes(limitar(observacoes, LIMITE_ANOTACOES));
    }

    /** Anotações do profissional sobre um atendimento específico. Devolve o id do cliente. */
    @Transactional
    public Long salvarAnotacoes(Long agendamentoId, String anotacoes) {
        Agendamento agendamento = buscarAgendamento(agendamentoId);
        agendamento.setAnotacoesProfissional(limitar(anotacoes, LIMITE_ANOTACOES));
        return agendamento.getCliente().getId();
    }

    /**
     * Marca o atendimento como CONCLUÍDO ou FALTOU. Devolve o id do cliente.
     * Regras: só depois do horário de início, e só para atendimentos ainda em aberto
     * (AGENDADO ou CONFIRMADO). Atendimentos CONCLUÍDOS entram no faturamento.
     */
    @Transactional
    public Long registrarResultado(Long agendamentoId, StatusAgendamento resultado) {
        if (resultado != StatusAgendamento.CONCLUIDO && resultado != StatusAgendamento.FALTOU) {
            throw new RegraNegocioException("Escolha \"Concluído\" ou \"Faltou\".");
        }
        Agendamento agendamento = buscarAgendamento(agendamentoId);
        if (!estaEmAberto(agendamento)) {
            throw new RegraNegocioException("Este atendimento já foi finalizado ou cancelado.");
        }
        if (agendamento.getInicio().isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("Só é possível registrar o resultado depois do horário do atendimento.");
        }
        agendamento.setStatus(resultado);
        return agendamento.getCliente().getId();
    }

    /** Usado pela tela para decidir se mostra os botões "Concluído" e "Faltou". */
    public boolean podeRegistrarResultado(Agendamento agendamento) {
        return estaEmAberto(agendamento) && !agendamento.getInicio().isAfter(LocalDateTime.now());
    }

    private boolean estaEmAberto(Agendamento agendamento) {
        return agendamento.getStatus() == StatusAgendamento.AGENDADO
                || agendamento.getStatus() == StatusAgendamento.CONFIRMADO;
    }

    private Agendamento buscarAgendamento(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Agendamento não encontrado."));
    }

    private String limitar(String texto, int maximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.length() > maximo ? limpo.substring(0, maximo) : limpo;
    }
}
