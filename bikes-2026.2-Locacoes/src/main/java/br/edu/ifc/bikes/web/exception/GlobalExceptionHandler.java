package br.edu.ifc.bikes.web.exception;

import br.edu.ifc.bikes.exception.CartaoInvalidoException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.PasswordInvalidException;
import br.edu.ifc.bikes.exception.RecursoDuplicadoException;
import br.edu.ifc.bikes.exception.RegraNegocioException;
import br.edu.ifc.bikes.exception.UsernameUniqueViolationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 422 - campos inválidos (devolve o mapa campo -> mensagem)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> methodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request,
            BindingResult result) {
        return build(request, HttpStatus.UNPROCESSABLE_CONTENT, "Campo(s) inválido(s)", result);
    }

    // 404 - recurso não encontrado
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorMessage> entityNotFoundException(
            EntityNotFoundException ex, HttpServletRequest request) {
        return build(request, HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    // 409 - duplicidade (email, CPF, número de série, username) ou regra de negócio (bicicleta indisponível, locação já devolvida...)
    @ExceptionHandler({RecursoDuplicadoException.class, UsernameUniqueViolationException.class, RegraNegocioException.class})
    public ResponseEntity<ErrorMessage> conflito(RuntimeException ex, HttpServletRequest request) {
        return build(request, HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    // 409 - rede de segurança para violações de constraint do banco (ex.: duas requisições simultâneas)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorMessage> dataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(request, HttpStatus.CONFLICT, "Operação viola uma restrição de integridade dos dados", null);
    }

    // 422 - cartão recusado (ex.: vencido)
    @ExceptionHandler(CartaoInvalidoException.class)
    public ResponseEntity<ErrorMessage> cartaoInvalidoException(
            CartaoInvalidoException ex, HttpServletRequest request) {
        return build(request, HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), null);
    }

    // 400 - regras de senha (senha atual incorreta, confirmação divergente...)
    @ExceptionHandler(PasswordInvalidException.class)
    public ResponseEntity<ErrorMessage> passwordInvalidException(
            PasswordInvalidException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    // 400 - JSON malformado ou corpo ausente
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> httpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(request, HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou malformado", null);
    }

    // 500 - qualquer erro inesperado: registra o stack trace no console e devolve o motivo no corpo.
    // Sem isso, o erro cai no /error e, por causa da segurança, aparece só como "403 Forbidden".
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> erroInesperado(Exception ex, HttpServletRequest request) throws Exception {
        // deixa o Spring tratar normalmente os erros HTTP padrão (404, 405, 415...) e os de segurança
        if (ex instanceof ErrorResponse || ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(request, HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getClass().getSimpleName() + ": " + ex.getMessage(), null);
    }

    private ResponseEntity<ErrorMessage> build(HttpServletRequest request, HttpStatus status,
                                               String message, BindingResult result) {
        ErrorMessage body = (result == null)
                ? new ErrorMessage(request, status, message)
                : new ErrorMessage(request, status, message, result);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
