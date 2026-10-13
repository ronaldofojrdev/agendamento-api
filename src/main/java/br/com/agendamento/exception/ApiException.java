package br.com.agendamento.exception;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(HttpStatus status, String message) { super(message); this.status = status; }
    public HttpStatus getStatus() { return status; }
    public static ApiException naoEncontrado() { return new ApiException(HttpStatus.NOT_FOUND, "Registro nao encontrado"); }
}
