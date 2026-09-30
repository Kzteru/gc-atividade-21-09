package br.com.agendafacil.exception;

/**
 * Lançada quando uma regra do negócio é violada (conflito de horário, fora do expediente etc.).
 * A mensagem é pensada para ser mostrada diretamente ao usuário na tela.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
