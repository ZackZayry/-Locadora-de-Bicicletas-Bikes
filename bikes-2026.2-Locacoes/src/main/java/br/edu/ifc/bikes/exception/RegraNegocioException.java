package br.edu.ifc.bikes.exception;

// Violação de regra de negócio / estado inválido (bicicleta indisponível, locação já devolvida...) -> HTTP 409
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String message) {
        super(message);
    }
}
