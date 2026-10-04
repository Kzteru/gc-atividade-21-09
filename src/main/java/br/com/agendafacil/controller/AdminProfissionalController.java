package br.com.agendafacil.controller;

import br.com.agendafacil.dto.CadastroForm;
import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Cadastro de profissionais pelo admin.
 * O acesso a /admin/** é restrito ao perfil ADMIN no SecurityConfig.
 */
@Controller
@RequestMapping("/admin/profissionais")
public class AdminProfissionalController {

    private final UsuarioService usuarioService;

    public AdminProfissionalController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("profissionais", usuarioService.listarProfissionais());
        return "admin/profissionais";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new CadastroForm());
        return "admin/profissional-form";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("form") CadastroForm form,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                Usuario profissional = usuarioService.cadastrarProfissional(form);
                redirect.addFlashAttribute("sucesso",
                        profissional.getNome() + " foi cadastrado. Ele já pode entrar com o e-mail e a senha informados.");
                return "redirect:/admin/profissionais";
            } catch (RegraNegocioException e) {
                model.addAttribute("erro", e.getMessage());
            }
        }
        form.limparSenhas();
        return "admin/profissional-form";
    }

    @PostMapping("/{id}/ativo")
    public String alternarAtivo(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            Usuario profissional = usuarioService.alternarAtivo(id);
            redirect.addFlashAttribute("sucesso", profissional.isAtivo()
                    ? profissional.getNome() + " foi reativado."
                    : profissional.getNome() + " foi desativado e não consegue mais entrar.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/profissionais";
    }
}
