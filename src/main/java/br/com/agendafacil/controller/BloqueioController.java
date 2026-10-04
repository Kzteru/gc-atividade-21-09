package br.com.agendafacil.controller;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Bloqueio;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.service.BloqueioService;
import br.com.agendafacil.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/bloqueios")
public class BloqueioController {
    private final BloqueioService bloqueioService;
    private final UsuarioService usuarioService;

    public BloqueioController(BloqueioService bloqueioService, UsuarioService usuarioService) {
        this.bloqueioService = bloqueioService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Long profissionalId, Principal principal, Model model) {
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());
        boolean admin = usuario.getPerfil() == Perfil.ADMIN;
        Long alvoId = admin ? profissionalId : usuario.getId();
        model.addAttribute("admin", admin);
        model.addAttribute("profissionais", admin ? usuarioService.listarProfissionais() : java.util.List.of());
        model.addAttribute("profissionalId", alvoId);
        model.addAttribute("bloqueios", alvoId == null ? java.util.List.of() : bloqueioService.listar(alvoId));
        model.addAttribute("form", new Bloqueio());
        return "bloqueios/lista";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, @RequestParam(required = false) Long profissionalId,
                         Principal principal, Model model) {
        Usuario alvo = verificarAcesso(principal, profissionalId);
        Bloqueio bloqueio = bloqueioService.buscar(id);
        if (!alvo.getId().equals(bloqueio.getProfissional().getId()))
            throw new RegraNegocioException("Bloqueio não encontrado para este profissional.");
        listar(profissionalId, principal, model);
        model.addAttribute("form", bloqueio);
        return "bloqueios/lista";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("form") Bloqueio form,
                         @RequestParam(required = false) Long profissionalId,
                         Principal principal, RedirectAttributes redirect) {
        try {
            Usuario alvo = verificarAcesso(principal, profissionalId);
            if (form.getId() != null) {
                Bloqueio existente = bloqueioService.buscar(form.getId());
                if (!alvo.getId().equals(existente.getProfissional().getId()))
                    throw new RegraNegocioException("Bloqueio não encontrado para este profissional.");
                form.setId(existente.getId());
            }
            bloqueioService.salvar(form, alvo.getId());
            redirect.addFlashAttribute("sucesso", "Bloqueio salvo.");
            return "redirect:/bloqueios" + (profissionalId == null ? "" : "?profissionalId=" + profissionalId);
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/bloqueios" + (profissionalId == null ? "" : "?profissionalId=" + profissionalId);
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, @RequestParam(required = false) Long profissionalId,
                          Principal principal, RedirectAttributes redirect) {
        try {
            Usuario alvo = verificarAcesso(principal, profissionalId);
            Bloqueio bloqueio = bloqueioService.buscar(id);
            if (!alvo.getId().equals(bloqueio.getProfissional().getId()))
                throw new RegraNegocioException("Bloqueio não encontrado para este profissional.");
            bloqueioService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Bloqueio removido.");
        } catch (RegraNegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/bloqueios" + (profissionalId == null ? "" : "?profissionalId=" + profissionalId);
    }

    private Usuario verificarAcesso(Principal principal, Long profissionalId) {
        Usuario atual = usuarioService.buscarPorEmail(principal.getName());
        if (atual.getPerfil() == Perfil.PROFISSIONAL) return atual;
        if (atual.getPerfil() != Perfil.ADMIN || profissionalId == null)
            throw new RegraNegocioException("Selecione um profissional.");
        return usuarioService.listarProfissionais().stream().filter(u -> u.getId().equals(profissionalId))
                .findFirst().orElseThrow(() -> new RegraNegocioException("Profissional não encontrado."));
    }
}
