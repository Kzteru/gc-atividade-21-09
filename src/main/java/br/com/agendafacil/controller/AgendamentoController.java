package br.com.agendafacil.controller;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.StatusAgendamento;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.service.AgendamentoService;
import br.com.agendafacil.service.UsuarioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dupla 3: telas do CLIENTE.
 *   GET  /agendamentos                 Meus agendamentos (próximos e anteriores)
 *   GET  /agendamentos/novo            Escolher serviço, profissional, dia e horário livre
 *   POST /agendamentos/novo            Confirmar a marcação
 *   POST /agendamentos/{id}/cancelar
 *   GET  /agendamentos/{id}/remarcar   Escolher novo dia e horário
 *   POST /agendamentos/{id}/remarcar
 * Toda gravação passa pelo AgendamentoService (validação de conflitos).
 * O SecurityConfig libera /agendamentos/** só para o perfil CLIENTE.
 */
@Controller
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private static final int LIMITE_OBSERVACOES = 500;

    private final AgendamentoService agendamentoService;
    private final UsuarioService usuarioService;

    public AgendamentoController(AgendamentoService agendamentoService, UsuarioService usuarioService) {
        this.agendamentoService = agendamentoService;
        this.usuarioService = usuarioService;
    }

    // ==================================================================
    // Meus agendamentos
    // ==================================================================

    @GetMapping
    public String listar(Principal principal, Model model) {
        Usuario cliente = usuarioService.buscarPorEmail(principal.getName());
        List<Agendamento> todos = agendamentoService.agendamentosDoCliente(cliente.getId());
        LocalDateTime agora = LocalDateTime.now();

        List<Agendamento> proximos = new ArrayList<>();
        List<Agendamento> anteriores = new ArrayList<>();
        for (Agendamento a : todos) {
            boolean emAberto = a.getStatus() == StatusAgendamento.AGENDADO
                    || a.getStatus() == StatusAgendamento.CONFIRMADO;
            if (emAberto && a.getFim().isAfter(agora)) {
                proximos.add(a);
            } else {
                anteriores.add(a);
            }
        }
        // Próximos: o mais perto primeiro. Anteriores: o mais recente primeiro (já vem assim do banco).
        proximos.sort(Comparator.comparing(Agendamento::getInicio));

        Set<Long> alteraveis = proximos.stream()
                .filter(agendamentoService::clientePodeAlterar)
                .map(Agendamento::getId)
                .collect(Collectors.toSet());

        model.addAttribute("proximos", proximos);
        model.addAttribute("anteriores", anteriores);
        model.addAttribute("alteraveis", alteraveis);
        return "agendamentos/lista";
    }

    // ==================================================================
    // Novo agendamento
    // ==================================================================

    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) Long servicoId,
                       @RequestParam(required = false) Long profissionalId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                       Model model) {
        LocalDate hoje = LocalDate.now();
        model.addAttribute("servicos", agendamentoService.servicosAtivos());
        model.addAttribute("profissionais", agendamentoService.profissionaisAtivos());
        model.addAttribute("servicoId", servicoId);
        model.addAttribute("profissionalId", profissionalId);
        model.addAttribute("data", data != null ? data : hoje);
        model.addAttribute("hoje", hoje);

        boolean escolhaCompleta = servicoId != null && profissionalId != null && data != null;
        model.addAttribute("buscou", escolhaCompleta);
        if (escolhaCompleta) {
            model.addAttribute("dataPorExtenso", DatasFormatadas.porExtenso(data));
            if (data.isBefore(hoje)) {
                model.addAttribute("erro", "Escolha uma data a partir de hoje.");
                model.addAttribute("horarios", List.of());
            } else {
                try {
                    Agendamento agendamento = new Agendamento();
                    model.addAttribute("horarios",
                            agendamentoService.horariosDisponiveis(profissionalId, servicoId, data, agendamento.getId()));
                } catch (RegraNegocioException e) {
                    model.addAttribute("erro", e.getMessage());
                    model.addAttribute("horarios", List.of());
                }
            }
        }
        return "agendamentos/novo";
    }

    @PostMapping("/novo")
    public String agendar(@RequestParam Long servicoId,
                          @RequestParam Long profissionalId,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                          @RequestParam(required = false) String observacoes,
                          Principal principal,
                          RedirectAttributes redirect) {
        Usuario cliente = usuarioService.buscarPorEmail(principal.getName());
        try {
            if (inicio == null) {
                throw new RegraNegocioException("Escolha um dos horários livres.");
            }
            agendamentoService.agendar(cliente.getId(), profissionalId, servicoId, inicio,
                    limitar(observacoes, LIMITE_OBSERVACOES));
            redirect.addFlashAttribute("sucesso", "Agendamento marcado para "
                    + DatasFormatadas.porExtenso(inicio.toLocalDate()).toLowerCase()
                    + " às " + String.format("%02d:%02d", inicio.getHour(), inicio.getMinute()) + ".");
            return "redirect:/agendamentos";
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            LocalDate data = inicio != null ? inicio.toLocalDate() : null;
            return "redirect:" + UriComponentsBuilder.fromPath("/agendamentos/novo")
                    .queryParam("servicoId", servicoId)
                    .queryParam("profissionalId", profissionalId)
                    .queryParamIfPresent("data", java.util.Optional.ofNullable(data))
                    .toUriString();
        }
    }

    // ==================================================================
    // Cancelar e remarcar
    // ==================================================================

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        Usuario cliente = usuarioService.buscarPorEmail(principal.getName());
        try {
            agendamentoService.buscarDoCliente(id, cliente.getId());
            agendamentoService.cancelar(id);
            redirect.addFlashAttribute("sucesso", "Agendamento cancelado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/agendamentos";
    }

    @GetMapping("/{id}/remarcar")
    public String remarcar(@PathVariable Long id,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                           Principal principal, Model model, RedirectAttributes redirect) {
        Usuario cliente = usuarioService.buscarPorEmail(principal.getName());
        Agendamento agendamento = null;
        try {
            agendamentoService.buscarDoCliente(id, cliente.getId());
            agendamentoService.verificarSeClientePodeAlterar(agendamento);
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/agendamentos";
        }

        LocalDate hoje = LocalDate.now();
        LocalDate dia = data != null ? data : agendamento.getInicio().toLocalDate();
        model.addAttribute("agendamento", agendamento);
        model.addAttribute("data", dia);
        model.addAttribute("hoje", hoje);
        model.addAttribute("dataPorExtenso", DatasFormatadas.porExtenso(dia));
        if (dia.isBefore(hoje)) {
            model.addAttribute("erro", "Escolha uma data a partir de hoje.");
            model.addAttribute("horarios", List.of());
        } else {
            try {
                model.addAttribute("horarios", agendamentoService.horariosDisponiveis(
                        agendamento.getProfissional().getId(), agendamento.getServico().getId(), dia, agendamento.getId()));
            } catch (RegraNegocioException e) {
                model.addAttribute("erro", e.getMessage());
                model.addAttribute("horarios", List.of());
            }
        }
        return "agendamentos/remarcar";
    }

    @PostMapping("/{id}/remarcar")
    public String confirmarRemarcacao(@PathVariable Long id,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                      Principal principal, RedirectAttributes redirect) {
        Usuario cliente = usuarioService.buscarPorEmail(principal.getName());
        try {
            agendamentoService.buscarDoCliente(id, cliente.getId());
            if (inicio == null) {
                throw new RegraNegocioException("Escolha um dos horários livres.");
            }
            agendamentoService.remarcar(id, inicio);
            redirect.addFlashAttribute("sucesso", "Agendamento remarcado para "
                    + DatasFormatadas.porExtenso(inicio.toLocalDate()).toLowerCase()
                    + " às " + String.format("%02d:%02d", inicio.getHour(), inicio.getMinute()) + ".");
            return "redirect:/agendamentos";
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            String destino = "/agendamentos/" + id + "/remarcar";
            return "redirect:" + (inicio == null ? destino : destino + "?data=" + inicio.toLocalDate());
        }
    }

    private static String limitar(String texto, int maximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.length() > maximo ? limpo.substring(0, maximo) : limpo;
    }
}
