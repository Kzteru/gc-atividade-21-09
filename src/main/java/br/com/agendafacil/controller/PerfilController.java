package br.com.agendafacil.controller;

import br.com.agendafacil.dto.PerfilForm;
import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** "Meu perfil": qualquer usuário logado edita os próprios dados. */
@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String formulario(Principal principal, Model model) {
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());
        model.addAttribute("usuario", usuario);
        model.addAttribute("form", PerfilForm.de(usuario));
        return "perfil";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("form") PerfilForm form,
                         BindingResult resultado,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                usuarioService.atualizarPerfil(principal.getName(), form);
                redirect.addFlashAttribute("sucesso", "Perfil atualizado.");
                return "redirect:/perfil";
            } catch (RegraNegocioException e) {
                model.addAttribute("erro", e.getMessage());
            }
        }
        // Volta para a mesma página mostrando os erros
        model.addAttribute("usuario", usuarioService.buscarPorEmail(principal.getName()));
        form.limparSenhas();
        return "perfil";
    }
}
