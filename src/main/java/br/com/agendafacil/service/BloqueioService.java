package br.com.agendafacil.service;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Bloqueio;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.repository.BloqueioRepository;
import br.com.agendafacil.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BloqueioService {
    private final BloqueioRepository bloqueioRepository;
    private final UsuarioRepository usuarioRepository;

    public BloqueioService(BloqueioRepository bloqueioRepository, UsuarioRepository usuarioRepository) {
        this.bloqueioRepository = bloqueioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Bloqueio> listar(Long profissionalId) {
        return bloqueioRepository.findByProfissionalIdOrderByInicio(profissionalId);
    }

    @Transactional(readOnly = true)
    public Bloqueio buscar(Long id) {
        return bloqueioRepository.findById(id).orElseThrow(() -> new RegraNegocioException("Bloqueio não encontrado."));
    }

    @Transactional
    public Bloqueio salvar(Bloqueio bloqueio, Long profissionalId) {
        if (bloqueio.getInicio() == null || bloqueio.getFim() == null) {
            throw new RegraNegocioException("Informe a data e o horário de início e fim.");
        }
        if (!bloqueio.getInicio().isBefore(bloqueio.getFim())) {
            throw new RegraNegocioException("O fim do bloqueio deve ser posterior ao início.");
        }
        Usuario profissional = usuarioRepository.findById(profissionalId)
                .filter(u -> u.getPerfil() == Perfil.PROFISSIONAL && u.isAtivo())
                .orElseThrow(() -> new RegraNegocioException("Profissional não encontrado ou inativo."));
        if (bloqueioRepository.contarSobreposicoesExceto(profissionalId, bloqueio.getInicio(),
                bloqueio.getFim(), bloqueio.getId()) > 0) {
            throw new RegraNegocioException("Este bloqueio se sobrepõe a outro bloqueio.");
        }
        bloqueio.setProfissional(profissional);
        if (bloqueio.getMotivo() != null) bloqueio.setMotivo(bloqueio.getMotivo().trim());
        return bloqueioRepository.save(bloqueio);
    }

    @Transactional
    public void excluir(Long id) {
        bloqueioRepository.delete(buscar(id));
    }
}
