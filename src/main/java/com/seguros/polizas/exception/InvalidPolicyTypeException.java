package com.seguros.polizas.exception;

public class InvalidPolicyTypeException extends BusinessException {
    public InvalidPolicyTypeException(String message) {

        super("TIPO_POLIZA_INVALIDO", message);
    }
}