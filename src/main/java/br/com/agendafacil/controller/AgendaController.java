package br.com.agendafacil.controller;

import br.com.agendafacil.dto.DiaAgenda;
import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.StatusAgendamento;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.service.AgendamentoService;
import br.com.agendafacil.service.HistoricoService;
import br.com.agendafacil.service.UsuarioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dupla 3: tela "Minha agenda" (/agenda), para PROFISSIONAL e ADMIN.
 * - Profissional vê só a própria agenda.
 * - Admin vê a agenda de todos e pode filtrar por profissional.
 * Visões: um dia ou a semana (segunda a domingo) que contém a data escolhida.
 * Ações: confirmar, cancelar (sem a antecedência exigida do cliente) e,
 * depois do horário, marcar como concluído ou falta (reaproveita o HistoricoService da dupla 4).
 */
@Controller
@RequestMapping("/agenda")
public class AgendaController {

    private final AgendamentoService agendamentoService;
    private final HistoricoService historicoService;
    private final UsuarioService usuarioService;

    public AgendaController(AgendamentoService agendamentoService,
                            HistoricoService historicoService,
                            UsuarioService usuarioService) {
        this.agendamentoService = agendamentoService;
        this.historicoService = historicoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String agenda(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         @RequestParam(required = false, defaultValue = "dia") String periodo,
                         @RequestParam(required = false) Long profissionalId,
                         Principal principal, Model model) {
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());
        boolean admin = usuario.getPerfil() == Perfil.ADMIN;
        Long alvoId = admin ? profissionalId : usuario.getId();
        boolean semana = "semana".equals(periodo);
        LocalDate dia = data != null ? data : LocalDate.now();

        LocalDate de = semana ? dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : dia;
        LocalDate ate = semana ? de.plusDays(6) : dia;
        List<Agendamento> agendamentos = agendamentoService.agenda(alvoId, de, ate);

        // Agrupa por dia (na visão de semana aparecem os 7 dias, mesmo os vazios)
        List<DiaAgenda> dias = new ArrayList<>();
        for (LocalDate d = de; !d.isAfter(ate); d = d.plusDays(1)) {
            LocalDate atual = d;
            List<Agendamento> doDia = agendamentos.stream()
                    .filter(a -> a.getInicio().toLocalDate().equals(atual))
                    .toList();
            dias.add(new DiaAgenda(atual, DatasFormatadas.porExtenso(atual), doDia));
        }

        // Quais botões aparecem em cada atendimento
        Set<Long> confirmaveis = filtrarIds(agendamentos, agendamentoService::podeConfirmar);
        Set<Long> cancelaveis = filtrarIds(agendamentos, agendamentoService::podeCancelarPeloProfissional);
        Set<Long> pendentes = filtrarIds(agendamentos, historicoService::podeRegistrarResultado);

        // Resumo do período (cancelados não contam)
        List<Agendamento> validos = agendamentos.stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .toList();
        BigDecimal valorPrevisto = validos.stream()
                .filter(a -> a.getStatus() != StatusAgendamento.FALTOU)
                .map(Agendamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("admin", admin);
        model.addAttribute("profissionais", admin ? usuarioService.listarProfissionais() : List.of());
        model.addAttribute("profissionalId", alvoId);
        model.addAttribute("data", dia);
        model.addAttribute("periodo", semana ? "semana" : "dia");
        model.addAttribute("anterior", semana ? dia.minusWeeks(1) : dia.minusDays(1));
        model.addAttribute("proximo", semana ? dia.plusWeeks(1) : dia.plusDays(1));
        model.addAttribute("tituloPeriodo", semana
                ? "Semana de " + DatasFormatadas.porExtenso(de).toLowerCase()
                : DatasFormatadas.porExtenso(dia));
        model.addAttribute("dias", dias);
        model.addAttribute("confirmaveis", confirmaveis);
        model.addAttribute("cancelaveis", cancelaveis);
        model.addAttribute("pendentes", pendentes);
        model.addAttribute("totalAtendimentos", validos.size());
        model.addAttribute("valorPrevisto", valorPrevisto);
        return "agenda/agenda";
    }

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable Long id, @ModelAttribute Retorno retorno,
                            Principal principal, RedirectAttributes redirect) {
        try {
            verificarAcesso(id, principal);
            agendamentoService.confirmar(id);
            redirect.addFlashAttribute("sucesso", "Agendamento confirmado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return retorno.redirect();
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, @ModelAttribute Retorno retorno,
                           Principal principal, RedirectAttributes redirect) {
        try {
            verificarAcesso(id, principal);
            agendamentoService.cancelarPeloProfissional(id);
            redirect.addFlashAttribute("sucesso", "Agendamento cancelado. O horário voltou a ficar livre.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return retorno.redirect();
    }

    @PostMapping("/{id}/resultado")
    public String registrarResultado(@PathVariable Long id, @RequestParam StatusAgendamento status,
                                     @ModelAttribute Retorno retorno,
                                     Principal principal, RedirectAttributes redirect) {
        try {
            verificarAcesso(id, principal);
            historicoService.registrarResultado(id, status);
            redirect.addFlashAttribute("sucesso", status == StatusAgendamento.CONCLUIDO
                    ? "Atendimento marcado como concluído."
                    : "Falta registrada.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return retorno.redirect();
    }

    /** Profissional só mexe nos próprios agendamentos; admin mexe em qualquer um. */
    private void verificarAcesso(Long agendamentoId, Principal principal) {
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());
        agendamentoService.buscarParaGestao(agendamentoId, usuario);
    }

    private static Set<Long> filtrarIds(List<Agendamento> lista, java.util.function.Predicate<Agendamento> regra) {
        return lista.stream().filter(regra).map(Agendamento::getId).collect(Collectors.toSet());
    }

    /** Filtros da tela, enviados como campos escondidos, para voltar ao mesmo dia/semana depois de uma ação. */
    public static class Retorno {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate data;
        private String periodo;
        private Long profissionalId;

        public LocalDate getData() { return data; }
        public void setData(LocalDate data) { this.data = data; }
        public String getPeriodo() { return periodo; }
        public void setPeriodo(String periodo) { this.periodo = periodo; }
        public Long getProfissionalId() { return profissionalId; }
        public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }

        String redirect() {
            return "redirect:" + UriComponentsBuilder.fromPath("/agenda")
                    .queryParamIfPresent("data", Optional.ofNullable(data))
                    .queryParamIfPresent("periodo", Optional.ofNullable("semana".equals(periodo) ? periodo : null))
                    .queryParamIfPresent("profissionalId", Optional.ofNullable(profissionalId))
                    .toUriString();
        }
    }
}
