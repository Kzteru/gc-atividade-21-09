package br.com.agendafacil.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados digitados na tela de cadastro do cliente (/cadastro)
 * e no cadastro de profissional feito pelo admin (/admin/profissionais/novo).
 * Não é uma entidade: só carrega o formulário até o UsuarioService.
 */
public class CadastroForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "Digite um e-mail válido, como nome@exemplo.com.")
    private String email;

    @Size(max = 20, message = "O telefone pode ter no máximo 20 caracteres.")
    private String telefone;

    /** BCrypt só considera os primeiros 72 bytes, por isso o limite. */
    @NotBlank(message = "Crie uma senha.")
    @Size(min = 6, max = 72, message = "A senha precisa ter entre 6 e 72 caracteres.")
    private String senha;

    @NotBlank(message = "Repita a senha.")
    private String confirmacaoSenha;

    /** Chamado antes de mostrar o formulário de novo, para a senha não voltar preenchida na página. */
    public void limparSenhas() {
        this.senha = null;
        this.confirmacaoSenha = null;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getConfirmacaoSenha() { return confirmacaoSenha; }
    public void setConfirmacaoSenha(String confirmacaoSenha) { this.confirmacaoSenha = confirmacaoSenha; }
}