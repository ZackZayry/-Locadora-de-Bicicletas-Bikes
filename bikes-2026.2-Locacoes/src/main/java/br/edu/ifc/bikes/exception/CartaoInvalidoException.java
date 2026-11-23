package br.edu.ifc.bikes.exception;

// Cartão recusado por regra de negócio (ex.: vencido) -> HTTP 422
public class CartaoInvalidoException extends RuntimeException {
    public CartaoInvalidoException(String message) {
        super(message);
    }
}
