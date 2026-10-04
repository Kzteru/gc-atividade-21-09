package br.com.agendafacil.controller;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Servico;
import br.com.agendafacil.service.ServicoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/servicos")
public class ServicoController {
    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) { this.servicoService = servicoService; }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("servicos", servicoService.listarTodos());
        return "servicos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("servico", new Servico());
        return "servicos/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("servico", servicoService.buscarPorId(id));
        return "servicos/form";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("servico") Servico form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) return "servicos/form";
        try {
            if (form.getId() == null) {
                servicoService.salvar(form);
            } else {
                Servico atual = servicoService.buscarPorId(form.getId());
                atual.setNome(form.getNome());
                atual.setDescricao(form.getDescricao());
                atual.setDuracaoMinutos(form.getDuracaoMinutos());
                atual.setPreco(form.getPreco());
                servicoService.salvar(atual);
            }
            redirect.addFlashAttribute("sucesso", "Serviço salvo com sucesso.");
            return "redirect:/servicos";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "servicos/form";
        }
    }

    @PostMapping("/{id}/ativo")
    public String alternarAtivo(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            Servico servico = servicoService.alternarAtivo(id);
            redirect.addFlashAttribute("sucesso", servico.isAtivo() ? "Serviço ativado." : "Serviço desativado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/servicos";
    }
}
