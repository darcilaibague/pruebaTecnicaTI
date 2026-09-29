package com.seguros.polizas.exception;

/** El recurso solicitado no existe. Se responde con HTTP 404. */
public class NotFoundException extends RuntimeException {

  private final String codigo;

  public NotFoundException(String codigo, String mensaje) {
    super(mensaje);
    this.codigo = codigo;
  }

  public String getCodigo() {
    return codigo;
  }
}