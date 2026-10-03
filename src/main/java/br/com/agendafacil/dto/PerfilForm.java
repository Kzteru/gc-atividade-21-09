package br.com.agendafacil.dto;

import br.com.agendafacil.model.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Tela "Meu perfil" (/perfil). O e-mail não entra aqui porque é o login:
 * trocá-lo exigiria refazer a sessão do usuário.
 * Os campos de senha são opcionais: se "novaSenha" ficar vazia, a senha não muda.
 */
public class PerfilForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
    private String nome;

    @Size(max = 20, message = "O telefone pode ter no máximo 20 caracteres.")
    private String telefone;

    private String senhaAtual;

    @Size(max = 72, message = "A senha pode ter no máximo 72 caracteres.")
    private String novaSenha;

    private String confirmacaoNovaSenha;

    public static PerfilForm de(Usuario usuario) {
        PerfilForm form = new PerfilForm();
        form.setNome(usuario.getNome());
        form.setTelefone(usuario.getTelefone());
        return form;
    }

    public void limparSenhas() {
        this.senhaAtual = null;
        this.novaSenha = null;
        this.confirmacaoNovaSenha = null;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }

    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }

    public String getConfirmacaoNovaSenha() { return confirmacaoNovaSenha; }
    public void setConfirmacaoNovaSenha(String confirmacaoNovaSenha) { this.confirmacaoNovaSenha = confirmacaoNovaSenha; }
}
