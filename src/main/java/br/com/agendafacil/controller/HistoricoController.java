package br.com.agendafacil.controller;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.StatusAgendamento;
import br.com.agendafacil.service.HistoricoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dupla 4: histórico dos clientes.
 * Fica dentro de /relatorios porque o SecurityConfig já libera esse caminho
 * só para PROFISSIONAL e ADMIN (cliente não vê o histórico dos outros).
 */
@Controller
@RequestMapping("/relatorios/clientes")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping
    public String listarClientes(Model model) {
        model.addAttribute("clientes", historicoService.listarClientes());
        return "relatorios/clientes";
    }

    @GetMapping("/{clienteId}")
    public String historico(@PathVariable Long clienteId, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("cliente", historicoService.buscarCliente(clienteId));
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/relatorios/clientes";
        }
        List<Agendamento> agendamentos = historicoService.historicoDoCliente(clienteId);
        // ids dos atendimentos que já passaram e ainda esperam "Concluído" ou "Faltou"
        Set<Long> pendentes = agendamentos.stream()
                .filter(historicoService::podeRegistrarResultado)
                .map(Agendamento::getId)
                .collect(Collectors.toSet());
        model.addAttribute("agendamentos", agendamentos);
        model.addAttribute("pendentes", pendentes);
        return "relatorios/historico";
    }

    @PostMapping("/{clienteId}/observacoes")
    public String salvarObservacoes(@PathVariable Long clienteId,
                                    @RequestParam(required = false) String observacoes,
                                    RedirectAttributes redirect) {
        try {
            historicoService.salvarObservacoesCliente(clienteId, observacoes);
            redirect.addFlashAttribute("sucesso", "Preferências do cliente salvas.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/relatorios/clientes/" + clienteId;
    }

    @PostMapping("/{clienteId}/agendamentos/{agendamentoId}/anotacoes")
    public String salvarAnotacoes(@PathVariable Long clienteId,
                                  @PathVariable Long agendamentoId,
                                  @RequestParam(required = false) String anotacoes,
                                  RedirectAttributes redirect) {
        try {
            historicoService.salvarAnotacoes(agendamentoId, anotacoes);
            redirect.addFlashAttribute("sucesso", "Anotação salva.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/relatorios/clientes/" + clienteId;
    }

    @PostMapping("/{clienteId}/agendamentos/{agendamentoId}/resultado")
    public String registrarResultado(@PathVariable Long clienteId,
                                     @PathVariable Long agendamentoId,
                                     @RequestParam StatusAgendamento status,
                                     RedirectAttributes redirect) {
        try {
            historicoService.registrarResultado(agendamentoId, status);
            redirect.addFlashAttribute("sucesso", status == StatusAgendamento.CONCLUIDO
                    ? "Atendimento marcado como concluído."
                    : "Falta registrada.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/relatorios/clientes/" + clienteId;
    }
}
