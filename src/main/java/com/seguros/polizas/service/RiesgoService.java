package com.seguros.polizas.service;

import com.seguros.polizas.client.CoreClient;
import com.seguros.polizas.domain.Poliza;
import com.seguros.polizas.domain.Riesgo;
import com.seguros.polizas.dto.RiesgoRequest;
import com.seguros.polizas.dto.RiesgoResponse;
import com.seguros.polizas.repository.PolizaRepository;
import com.seguros.polizas.repository.RiesgoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiesgoService {

    private static final Logger log = LoggerFactory.getLogger(RiesgoService.class);

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final CoreClient coreClient;

    public RiesgoService(PolizaRepository polizaRepository,
                         RiesgoRepository riesgoRepository,
                         CoreClient coreClient) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.coreClient = coreClient;
    }

    @Transactional
    public RiesgoResponse agregar(Long polizaId, RiesgoRequest request) {
        Poliza poliza = polizaRepository.findById(polizaId)
                .orElseThrow(() -> new NotFoundException("POLIZA_NO_ENCONTRADA",
                        "No existe la póliza con id " + polizaId));

        Riesgo riesgo = poliza.agregarRiesgo(
                request.direccionInmueble(), request.arrendatario(), request.arrendador());
        riesgoRepository.save(riesgo);

        coreClient.notificarActualizacion(poliza.getId());
        log.info("Riesgo {} agregado a la póliza {}", riesgo.getId(), poliza.getNumero());
        return RiesgoResponse.from(riesgo);
    }

    @Transactional
    public RiesgoResponse cancelar(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new NotFoundException("RIESGO_NO_ENCONTRADO",
                        "No existe el riesgo con id " + riesgoId));

        riesgo.cancelar();

        coreClient.notificarActualizacion(riesgo.getPoliza().getId());
        log.info("Riesgo {} de la póliza {} cancelado", riesgo.getId(), riesgo.getPoliza().getNumero());
        return RiesgoResponse.from(riesgo);
    }
}
