package br.com.agendafacil.service;

import br.com.agendafacil.dto.CadastroForm;
import br.com.agendafacil.dto.PerfilForm;
import br.com.agendafacil.exception.RegraNegocioException;
import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Regras de cadastro e perfil (dupla 1).
 * Assim como no AgendamentoService, controllers não salvam Usuario direto pelo repository:
 * é aqui que a senha é criptografada e o e-mail é verificado.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================================================================
    // Cadastro
    // ==================================================================

    /** Cadastro público feito pelo próprio cliente na tela /cadastro. */
    @Transactional
    public Usuario cadastrarCliente(CadastroForm form) {
        return criar(form, Perfil.CLIENTE);
    }

    /** Cadastro de profissional, feito apenas pelo admin. */
    @Transactional
    public Usuario cadastrarProfissional(CadastroForm form) {
        return criar(form, Perfil.PROFISSIONAL);
    }

    private Usuario criar(CadastroForm form, Perfil perfil) {
        String email = normalizarEmail(form.getEmail());
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }
        validarNovaSenha(form.getSenha(), form.getConfirmacaoSenha());

        Usuario usuario = new Usuario();
        usuario.setNome(form.getNome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(textoOuNulo(form.getTelefone()));
        usuario.setSenha(passwordEncoder.encode(form.getSenha()));
        usuario.setPerfil(perfil);
        try {
            // saveAndFlush força o INSERT agora, para pegar o caso raro de duas
            // pessoas cadastrando o mesmo e-mail ao mesmo tempo (coluna unique).
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }
    }

    // ==================================================================
    // Perfil
    // ==================================================================

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
    }

    /**
     * Atualiza nome e telefone. Se "novaSenha" vier preenchida, troca a senha,
     * mas só depois de conferir a senha atual.
     */
    @Transactional
    public void atualizarPerfil(String emailLogado, PerfilForm form) {
        Usuario usuario = buscarPorEmail(emailLogado);

        // Senha primeiro: se der erro, nada é alterado.
        if (temTexto(form.getNovaSenha())) {
            if (!temTexto(form.getSenhaAtual())
                    || !passwordEncoder.matches(form.getSenhaAtual(), usuario.getSenha())) {
                throw new RegraNegocioException("A senha atual está incorreta.");
            }
            validarNovaSenha(form.getNovaSenha(), form.getConfirmacaoNovaSenha());
            usuario.setSenha(passwordEncoder.encode(form.getNovaSenha()));
        }

        usuario.setNome(form.getNome().trim());
        usuario.setTelefone(textoOuNulo(form.getTelefone()));
    }

    // ==================================================================
    // Administração de profissionais
    // ==================================================================

    /** Todos os profissionais, ativos e inativos, para a tela do admin. */
    @Transactional(readOnly = true)
    public List<Usuario> listarProfissionais() {
        return usuarioRepository.findByPerfilOrderByNome(Perfil.PROFISSIONAL);
    }

    /**
     * Desativa ou reativa um profissional. Desativado, ele não consegue entrar
     * (o UsuarioDetailsService já filtra por "ativo").
     */
    @Transactional
    public Usuario alternarAtivo(Long profissionalId) {
        Usuario usuario = usuarioRepository.findById(profissionalId)
                .orElseThrow(() -> new RegraNegocioException("Profissional não encontrado."));
        if (usuario.getPerfil() != Perfil.PROFISSIONAL) {
            throw new RegraNegocioException("Só é possível desativar contas de profissionais por aqui.");
        }
        usuario.setAtivo(!usuario.isAtivo());
        return usuario;
    }

    // ==================================================================
    // Auxiliares
    // ==================================================================

    /** E-mail sempre minúsculo e sem espaços, no cadastro e no login. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void validarNovaSenha(String senha, String confirmacao) {
        if (senha == null || senha.length() < 6) {
            throw new RegraNegocioException("A senha precisa ter pelo menos 6 caracteres.");
        }
        if (!senha.equals(confirmacao)) {
            throw new RegraNegocioException("As duas senhas digitadas não são iguais.");
        }
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String textoOuNulo(String valor) {
        return temTexto(valor) ? valor.trim() : null;
    }
}
