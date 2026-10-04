package br.com.agendafacil.service;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Servico;
import br.com.agendafacil.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    @Transactional(readOnly = true)
    public List<Servico> listarAtivos() {
        return servicoRepository.findByAtivoTrueOrderByNome();
    }

    @Transactional(readOnly = true)
    public List<Servico> listarTodos() {
        return servicoRepository.findAll();
    }

    @Transactional
    public Servico salvar(Servico servico) {
        validar(servico);

        if (servico.getNome() != null) {
            servico.setNome(servico.getNome().trim());
        }

        if (servico.getDescricao() != null) {
            servico.setDescricao(servico.getDescricao().trim());
        }

        return servicoRepository.save(servico);
    }

    @Transactional
    public Servico alternarAtivo(Long id) {
        Servico servico = buscarPorId(id);
        servico.setAtivo(!servico.isAtivo());
        return servico;
    }

    @Transactional(readOnly = true)
    public Servico buscarPorId(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Serviço não encontrado."));
    }

    private void validar(Servico servico) {
        if (servico.getNome() == null || servico.getNome().isBlank()) {
            throw new RegraNegocioException("O nome do serviço é obrigatório.");
        }

        if (servico.getDuracaoMinutos() == null || servico.getDuracaoMinutos() < 5) {
            throw new RegraNegocioException("A duração do serviço deve ser de pelo menos 5 minutos.");
        }

        if (servico.getPreco() == null || servico.getPreco().signum() < 0) {
            throw new RegraNegocioException("O preço do serviço não pode ser negativo.");
        }
    }
}