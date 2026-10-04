package br.com.agendafacil.service;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.HorarioTrabalho;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.repository.HorarioTrabalhoRepository;
import br.com.agendafacil.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HorarioTrabalhoService {
    private final HorarioTrabalhoRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;

    public HorarioTrabalhoService(HorarioTrabalhoRepository horarioRepository, UsuarioRepository usuarioRepository) {
        this.horarioRepository = horarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<HorarioTrabalho> listar(Long profissionalId) {
        return horarioRepository.findByProfissionalIdOrderByDiaSemanaAscHoraInicioAsc(profissionalId);
    }

    @Transactional(readOnly = true)
    public HorarioTrabalho buscar(Long id) {
        return horarioRepository.findById(id).orElseThrow(() -> new RegraNegocioException("Horário não encontrado."));
    }

    @Transactional
    public HorarioTrabalho salvar(HorarioTrabalho horario, Long profissionalId) {
        if (horario.getDiaSemana() == null || horario.getHoraInicio() == null || horario.getHoraFim() == null) {
            throw new RegraNegocioException("Informe o dia e os horários de início e fim.");
        }
        if (!horario.getHoraInicio().isBefore(horario.getHoraFim())) {
            throw new RegraNegocioException("O horário final deve ser posterior ao inicial.");
        }
        Usuario profissional = usuarioRepository.findById(profissionalId)
                .filter(u -> u.getPerfil() == Perfil.PROFISSIONAL && u.isAtivo())
                .orElseThrow(() -> new RegraNegocioException("Profissional não encontrado ou inativo."));
        Long ignorarId = horario.getId();
        if (horarioRepository.contarSobreposicoes(profissionalId, horario.getDiaSemana(),
                horario.getHoraInicio(), horario.getHoraFim(), ignorarId) > 0) {
            throw new RegraNegocioException("Este período se sobrepõe a outro horário de trabalho.");
        }
        horario.setProfissional(profissional);
        return horarioRepository.save(horario);
    }

    @Transactional
    public void excluir(Long id) {
        horarioRepository.delete(buscar(id));
    }
}
