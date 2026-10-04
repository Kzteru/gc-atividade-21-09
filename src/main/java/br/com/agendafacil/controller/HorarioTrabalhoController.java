package br.com.agendafacil.controller;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.*;
import br.com.agendafacil.service.HorarioTrabalhoService;
import br.com.agendafacil.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.DayOfWeek;
import java.util.Map;

@Controller
@RequestMapping("/horarios")
public class HorarioTrabalhoController {
    private final HorarioTrabalhoService horarioService;
    private final UsuarioService usuarioService;

    public HorarioTrabalhoController(HorarioTrabalhoService horarioService, UsuarioService usuarioService) {
        this.horarioService = horarioService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Long profissionalId, Principal principal, Model model) {
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());
        boolean admin = usuario.getPerfil() == Perfil.ADMIN;
        model.addAttribute("admin", admin);
        model.addAttribute("profissionais", admin ? usuarioService.listarProfissionais() : java.util.List.of());
        Long alvoId = admin ? profissionalId : usuario.getId();
        model.addAttribute("profissionalId", alvoId);
        model.addAttribute("horarios", alvoId == null ? java.util.List.of() : horarioService.listar(alvoId));
        model.addAttribute("dias", Map.of(DayOfWeek.MONDAY, "Segunda-feira", DayOfWeek.TUESDAY, "Terça-feira",
                DayOfWeek.WEDNESDAY, "Quarta-feira", DayOfWeek.THURSDAY, "Quinta-feira",
                DayOfWeek.FRIDAY, "Sexta-feira", DayOfWeek.SATURDAY, "Sábado", DayOfWeek.SUNDAY, "Domingo"));
        model.addAttribute("form", new HorarioTrabalho());
        return "horarios/lista";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, @RequestParam(required = false) Long profissionalId,
                         Principal principal, Model model) {
        Usuario alvo = verificarAcesso(principal, profissionalId);
        HorarioTrabalho horario = horarioService.buscar(id);
        if (!alvo.getId().equals(horario.getProfissional().getId()))
            throw new RegraNegocioException("Horário não encontrado para este profissional.");
        listar(profissionalId, principal, model);
        model.addAttribute("form", horario);
        return "horarios/lista";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("form") HorarioTrabalho form,
                         @RequestParam(required = false) Long profissionalId,
                         Principal principal, Model model, RedirectAttributes redirect) {
        try {
            Usuario alvo = verificarAcesso(principal, profissionalId);
            if (form.getId() != null) {
                HorarioTrabalho atual = horarioService.buscar(form.getId());
                if (alvo == null || !alvo.getId().equals(atual.getProfissional().getId())) {
                    throw new RegraNegocioException("Horário não encontrado para este profissional.");
                }
                form.setId(atual.getId());
            }
            horarioService.salvar(form, alvo.getId());
            redirect.addFlashAttribute("sucesso", "Horário de trabalho salvo.");
            return "redirect:/horarios" + (usuarioService.buscarPorEmail(principal.getName()).getPerfil() == Perfil.ADMIN
                    ? "?profissionalId=" + alvo.getId() : "");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/horarios" + (profissionalId == null ? "" : "?profissionalId=" + profissionalId);
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, @RequestParam(required = false) Long profissionalId,
                          Principal principal, RedirectAttributes redirect) {
        try {
            Usuario alvo = verificarAcesso(principal, profissionalId);
            HorarioTrabalho horario = horarioService.buscar(id);
            if (alvo == null || !alvo.getId().equals(horario.getProfissional().getId()))
                throw new RegraNegocioException("Horário não encontrado para este profissional.");
            horarioService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Horário removido.");
        } catch (RegraNegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/horarios" + (profissionalId == null ? "" : "?profissionalId=" + profissionalId);
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
