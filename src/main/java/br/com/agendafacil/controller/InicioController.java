package br.com.agendafacil.controller;

import br.com.agendafacil.repository.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/**
 * Exemplo de controller para as duplas copiarem o padrão:
 * recebe a requisição, busca dados, coloca no Model e devolve o nome do template.
 */
@Controller
public class InicioController {

    private final UsuarioRepository usuarioRepository;

    public InicioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/login")
    public String login(Principal principal) {
        if (principal != null) {
            return "redirect:/"; // já está logado
        }
        return "login";
    }

    @GetMapping("/")
    public String inicio(Principal principal, Model model) {
        usuarioRepository.findByEmail(principal.getName())
                .ifPresent(usuario -> model.addAttribute("usuario", usuario));
        return "index";
    }
}