package com.seguros.polizas.service;

import com.seguros.polizas.client.CoreClient;
import com.seguros.polizas.domain.EstadoPoliza;
import com.seguros.polizas.domain.Poliza;
import com.seguros.polizas.domain.TipoPoliza;
import com.seguros.polizas.dto.PolizaResponse;
import com.seguros.polizas.dto.RiesgoResponse;
import com.seguros.polizas.exception.NotFoundException;
import com.seguros.polizas.repository.PolizaRepository;
import com.seguros.polizas.repository.RiesgoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PolizaService {

    private static final Logger log = LoggerFactory.getLogger(PolizaService.class);

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final CoreClient coreClient;
    private final BigDecimal ipc;

    public PolizaService(PolizaRepository polizaRepository,
                         RiesgoRepository riesgoRepository,
                         CoreClient coreClient,
                         @Value("${app.ipc}") BigDecimal ipc) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.coreClient = coreClient;
        this.ipc = ipc;
    }

    @Transactional(readOnly = true)
    public List<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado) {
        return polizaRepository.buscar(tipo, estado).stream()
                .map(PolizaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RiesgoResponse> listarRiesgos(Long polizaId) {
        if (!polizaRepository.existsById(polizaId)) {
            throw polizaNoEncontrada(polizaId);
        }
        return riesgoRepository.findByPolizaIdOrderByIdAsc(polizaId).stream()
                .map(RiesgoResponse::from)
                .toList();
    }

    @Transactional
    public PolizaResponse renovar(Long polizaId) {
        Poliza poliza = buscar(polizaId);
        poliza.renovar(ipc);
        coreClient.notificarActualizacion(poliza.getId());
        log.info("Póliza {} renovada con IPC {}. Nuevo canon {}, nueva prima {}",
                poliza.getNumero(), ipc, poliza.getCanonMensual(), poliza.getPrima());
        return PolizaResponse.from(poliza);
    }

    @Transactional
    public PolizaResponse cancelar(Long polizaId) {
        Poliza poliza = buscar(polizaId);
        poliza.cancelar();
        coreClient.notificarActualizacion(poliza.getId());
        log.info("Póliza {} cancelada junto con todos sus riesgos", poliza.getNumero());
        return PolizaResponse.from(poliza);
    }

    private Poliza buscar(Long polizaId) {
        return polizaRepository.findById(polizaId).orElseThrow(() -> polizaNoEncontrada(polizaId));
    }

    private NotFoundException polizaNoEncontrada(Long polizaId) {
        return new NotFoundException("POLIZA_NO_ENCONTRADA", "No existe la póliza con id " + polizaId);
    }
}
