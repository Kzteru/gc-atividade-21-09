package br.com.agendafacil.service;

import br.com.agendafacil.dto.CadastroForm;
import br.com.agendafacil.dto.PerfilForm;
import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/** Testes do cadastro e do perfil (dupla 1). Rode com: mvn test */
@SpringBootTest
@Transactional
class UsuarioServiceTest {

    @Autowired private UsuarioService service;
    @Autowired private PasswordEncoder passwordEncoder;

    private CadastroForm form(String email) {
        CadastroForm f = new CadastroForm();
        f.setNome("Maria Teste");
        f.setEmail(email);
        f.setTelefone("(64) 99999-0000");
        f.setSenha("segredo1");
        f.setConfirmacaoSenha("segredo1");
        return f;
    }

    @Test
    void deveCadastrarClienteComSenhaCriptografadaEEmailNormalizado() {
        Usuario u = service.cadastrarCliente(form("  Maria@Teste.COM "));

        assertEquals(Perfil.CLIENTE, u.getPerfil());
        assertEquals("maria@teste.com", u.getEmail());
        assertNotEquals("segredo1", u.getSenha());
        assertTrue(passwordEncoder.matches("segredo1", u.getSenha()));
    }

    @Test
    void naoDevePermitirEmailRepetidoMesmoComMaiusculas() {
        service.cadastrarCliente(form("maria@teste.com"));
        assertThrows(RegraNegocioException.class, () -> service.cadastrarCliente(form("MARIA@teste.com")));
    }

    @Test
    void naoDevePermitirSenhasDiferentes() {
        CadastroForm f = form("maria@teste.com");
        f.setConfirmacaoSenha("outra-senha");
        assertThrows(RegraNegocioException.class, () -> service.cadastrarCliente(f));
    }

    @Test
    void trocarSenhaExigeSenhaAtualCorreta() {
        service.cadastrarCliente(form("maria@teste.com"));

        PerfilForm p = new PerfilForm();
        p.setNome("Maria Nova");
        p.setSenhaAtual("errada");
        p.setNovaSenha("novaSenha1");
        p.setConfirmacaoNovaSenha("novaSenha1");
        assertThrows(RegraNegocioException.class, () -> service.atualizarPerfil("maria@teste.com", p));

        p.setSenhaAtual("segredo1");
        service.atualizarPerfil("maria@teste.com", p);
        Usuario u = service.buscarPorEmail("maria@teste.com");
        assertEquals("Maria Nova", u.getNome());
        assertTrue(passwordEncoder.matches("novaSenha1", u.getSenha()));
    }

    @Test
    void adminDesativaEReativaProfissional() {
        Usuario prof = service.cadastrarProfissional(form("joao@teste.com"));
        assertFalse(service.alternarAtivo(prof.getId()).isAtivo());
        assertTrue(service.alternarAtivo(prof.getId()).isAtivo());
    }

    @Test
    void naoDeveDesativarClientePelaTelaDeProfissionais() {
        Usuario cliente = service.cadastrarCliente(form("maria@teste.com"));
        assertThrows(RegraNegocioException.class, () -> service.alternarAtivo(cliente.getId()));
    }
}
