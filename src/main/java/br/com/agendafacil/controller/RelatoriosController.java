package br.com.agendafacil.controller;

import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.service.RelatorioService;
import br.com.agendafacil.service.UsuarioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dupla 4: tela de faturamento (/relatorios), para PROFISSIONAL e ADMIN.
 *   - Admin vê o faturamento de todos e pode filtrar por profissional.
 *   - Profissional vê só o próprio faturamento (o filtro nem aparece para ele).
 * A regra de quem vê o quê fica no RelatorioService; aqui só montamos a tela.
 */
@Controller
public class RelatoriosController {

    private final RelatorioService relatorioService;
    private final UsuarioService usuarioService;

    public RelatoriosController(RelatorioService relatorioService, UsuarioService usuarioService) {
        this.relatorioService = relatorioService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/relatorios")
    public String faturamento(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                              @RequestParam(required = false) Long profissionalId,
                              Principal principal, Model model) {
        Usuario usuario = usuarioService.buscarPorEmail(principal.getName());
        boolean admin = usuario.getPerfil() == Perfil.ADMIN;

        // Padrão: do dia 1º do mês atual até hoje
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = de != null ? de : hoje.withDayOfMonth(1);
        LocalDate fim = ate != null ? ate : hoje;
        // Para o profissional o filtro é descartado já aqui (e o service ignora de novo)
        Long filtro = admin ? profissionalId : null;

        model.addAttribute("admin", admin);
        model.addAttribute("profissionais", admin ? usuarioService.listarProfissionais() : List.of());
        model.addAttribute("profissionalId", filtro);
        model.addAttribute("de", inicio);
        model.addAttribute("ate", fim);
        try {
            model.addAttribute("resumo", relatorioService.faturamento(usuario, inicio, fim, filtro));
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
        }
        return "relatorios/faturamento";
    }
}
