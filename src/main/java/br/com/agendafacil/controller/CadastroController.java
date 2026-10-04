package br.com.agendafacil.controller;

import br.com.agendafacil.dto.CadastroForm;
import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

/** Cadastro público de clientes. A rota /cadastro já é liberada no SecurityConfig. */
@Controller
@RequestMapping("/cadastro")
public class CadastroController {

    private final UsuarioService usuarioService;

    public CadastroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String formulario(Principal principal, Model model) {
        if (principal != null) {
            return "redirect:/"; // quem já está logado não precisa criar conta
        }
        model.addAttribute("form", new CadastroForm());
        return "cadastro";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("form") CadastroForm form,
                            BindingResult resultado,
                            Model model) {
        if (resultado.hasErrors()) {
            form.limparSenhas();
            return "cadastro";
        }
        try {
            usuarioService.cadastrarCliente(form);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            form.limparSenhas();
            return "cadastro";
        }
        return "redirect:/login?cadastrado";
    }
}
