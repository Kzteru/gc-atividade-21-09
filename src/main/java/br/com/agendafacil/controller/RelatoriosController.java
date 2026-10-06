package br.com.agendafacil.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entrada da área de Relatórios (/relatorios).
 * Por enquanto a única tela pronta é o histórico de clientes, então quem acessa
 * /relatorios é levado direto para lá. Quando a tela de faturamento for criada,
 * troque o redirect por ela.
 */
@Controller
public class RelatoriosController {

    @GetMapping("/relatorios")
    public String inicio() {
        return "redirect:/relatorios/clientes";
    }
}
