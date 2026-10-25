package br.com.agendafacil.service;

import br.com.agendafacil.dto.LinhaFaturamento;
import br.com.agendafacil.dto.ResumoFaturamento;
import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.repository.AgendamentoRepository;
import br.com.agendafacil.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Dupla 4: relatório de faturamento por período, serviço e profissional.
 *
 * Regra de acesso (decidida AQUI, e não na tela, para não depender do controller):
 *   - ADMIN vê o faturamento de todos e pode filtrar por um profissional;
 *   - PROFISSIONAL vê SEMPRE só o próprio faturamento, mesmo que mande outro id na URL;
 *   - CLIENTE não tem acesso.
 * Só entram atendimentos CONCLUÍDOS: faltas, cancelados e os ainda em aberto não contam.
 */
@Service
public class RelatorioService {

    /** Evita consultas gigantes por engano (ex.: de 2000 a 2030). */
    private static final long MAXIMO_DIAS = 366;

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;

    public RelatorioService(AgendamentoRepository agendamentoRepository, UsuarioRepository usuarioRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * @param usuario               quem está logado
     * @param de                    primeiro dia do período (inclusive)
     * @param ate                   último dia do período (inclusive)
     * @param profissionalEscolhido filtro do admin (nulo = todos). Ignorado para o profissional.
     */
    @Transactional(readOnly = true)
    public ResumoFaturamento faturamento(Usuario usuario, LocalDate de, LocalDate ate, Long profissionalEscolhido) {
        Long profissionalId = definirProfissional(usuario, profissionalEscolhido);
        validarPeriodo(de, ate);

        List<Agendamento> concluidos = agendamentoRepository.concluidosNoPeriodo(
                profissionalId, de.atStartOfDay(), ate.plusDays(1).atStartOfDay());

        BigDecimal total = somar(concluidos);
        long quantidade = concluidos.size();
        BigDecimal ticketMedio = quantidade == 0
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);

        List<LinhaFaturamento> porServico = agrupar(concluidos,
                a -> a.getServico().getId(), a -> a.getServico().getNome());

        // A divisão por profissional só existe para o admin olhando "todos os profissionais"
        boolean mostrarPorProfissional = usuario.getPerfil() == Perfil.ADMIN && profissionalId == null;
        List<LinhaFaturamento> porProfissional = mostrarPorProfissional
                ? agrupar(concluidos, a -> a.getProfissional().getId(), a -> a.getProfissional().getNome())
                : List.of();

        return new ResumoFaturamento(total, quantidade, ticketMedio, porServico, porProfissional);
    }

    /** Devolve o id do profissional cujo faturamento será mostrado (nulo = todos, só para admin). */
    private Long definirProfissional(Usuario usuario, Long profissionalEscolhido) {
        if (usuario.getPerfil() == Perfil.PROFISSIONAL) {
            // O parâmetro vindo da tela é ignorado de propósito: o profissional só vê o que é dele.
            return usuario.getId();
        }
        if (usuario.getPerfil() != Perfil.ADMIN) {
            throw new RegraNegocioException("Você não tem acesso ao relatório de faturamento.");
        }
        if (profissionalEscolhido == null) {
            return null;
        }
        Usuario escolhido = usuarioRepository.findById(profissionalEscolhido)
                .filter(u -> u.getPerfil() == Perfil.PROFISSIONAL)
                .orElseThrow(() -> new RegraNegocioException("Profissional não encontrado."));
        return escolhido.getId();
    }

    private void validarPeriodo(LocalDate de, LocalDate ate) {
        if (de == null || ate == null) {
            throw new RegraNegocioException("Informe a data inicial e a final.");
        }
        if (de.isAfter(ate)) {
            throw new RegraNegocioException("A data inicial precisa ser anterior ou igual à data final.");
        }
        if (ChronoUnit.DAYS.between(de, ate) > MAXIMO_DIAS) {
            throw new RegraNegocioException("Escolha um período de no máximo um ano.");
        }
    }

    private static BigDecimal somar(List<Agendamento> agendamentos) {
        return agendamentos.stream()
                .map(Agendamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Agrupa pelo id (dois serviços com o mesmo nome não se misturam) e ordena do maior total para o menor. */
    private static List<LinhaFaturamento> agrupar(List<Agendamento> agendamentos,
                                                  Function<Agendamento, Long> chave,
                                                  Function<Agendamento, String> nome) {
        Map<Long, List<Agendamento>> grupos = new LinkedHashMap<>();
        for (Agendamento a : agendamentos) {
            grupos.computeIfAbsent(chave.apply(a), k -> new ArrayList<>()).add(a);
        }
        List<LinhaFaturamento> linhas = new ArrayList<>();
        for (List<Agendamento> grupo : grupos.values()) {
            linhas.add(new LinhaFaturamento(nome.apply(grupo.get(0)), grupo.size(), somar(grupo)));
        }
        linhas.sort(Comparator.comparing(LinhaFaturamento::getTotal).reversed()
                .thenComparing(LinhaFaturamento::getNome));
        return linhas;
    }
}