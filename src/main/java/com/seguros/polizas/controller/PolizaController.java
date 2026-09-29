package com.seguros.polizas.controller;

import com.seguros.polizas.domain.EstadoPoliza;
import com.seguros.polizas.domain.TipoPoliza;
import com.seguros.polizas.dto.PolizaResponse;
import com.seguros.polizas.dto.RiesgoRequest;
import com.seguros.polizas.dto.RiesgoResponse;
import com.seguros.polizas.service.PolizaService;
import com.seguros.polizas.service.RiesgoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/polizas")
public class PolizaController {

    private final PolizaService polizaService;
    private final RiesgoService riesgoService;

    public PolizaController(PolizaService polizaService, RiesgoService riesgoService) {
        this.polizaService = polizaService;
        this.riesgoService = riesgoService;
    }

    @GetMapping
    public List<PolizaResponse> listar(
            @RequestParam(name = "tipo", required = false) TipoPoliza tipo,
            @RequestParam(name = "estado", required = false) EstadoPoliza estado) {
        return polizaService.listar(tipo, estado);
    }

    @GetMapping("/{id}/riesgos")
    public List<RiesgoResponse> listarRiesgos(@PathVariable("id") Long id) {
        return polizaService.listarRiesgos(id);
    }

    @PostMapping("/{id}/renovar")
    public PolizaResponse renovar(@PathVariable("id") Long id) {
        return polizaService.renovar(id);
    }

    @PostMapping("/{id}/cancelar")
    public PolizaResponse cancelar(@PathVariable("id") Long id) {
        return polizaService.cancelar(id);
    }

    @PostMapping("/{id}/riesgos")
    public ResponseEntity<RiesgoResponse> agregarRiesgo(@PathVariable("id") Long id,
                                                        @Valid @RequestBody RiesgoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(riesgoService.agregar(id, request));
    }
}