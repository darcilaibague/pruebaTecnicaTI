package com.seguros.polizas.exception;

import com.seguros.polizas.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(NotFoundException ex) {
        return respuesta(404, ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(InvalidPolicyTypeException.class)
    public ResponseEntity<ErrorResponse> tipoPolizaInvalido(InvalidPolicyTypeException ex) {
        return respuesta(422, ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> reglaDeNegocio(BusinessException ex) {
        return respuesta(409, ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respuesta(400, "DATOS_INVALIDOS", mensaje);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        String mensaje = "Valor inválido para '" + ex.getName() + "': " + ex.getValue();
        Class<?> tipoEsperado = ex.getRequiredType();
        if (tipoEsperado != null && tipoEsperado.isEnum()) {
            mensaje += ". Valores permitidos: " + Arrays.toString(tipoEsperado.getEnumConstants());
        }
        return respuesta(400, "PARAMETRO_INVALIDO", mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return respuesta(400, "CUERPO_INVALIDO", "El cuerpo de la petición no es un JSON válido");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex) {
        return respuesta(404, "RUTA_NO_ENCONTRADA", "La ruta solicitada no existe");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        return respuesta(405, "METODO_NO_SOPORTADO", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorInesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return respuesta(500, "ERROR_INTERNO", "Ocurrió un error inesperado");
    }

    private ResponseEntity<ErrorResponse> respuesta(int status, String codigo, String mensaje) {
        return ResponseEntity.status(status).body(ErrorResponse.of(codigo, mensaje));
    }
}
