package com.seguros.polizas.client;

import com.seguros.polizas.config.ApiKeyFilter;
import com.seguros.polizas.dto.CoreEventoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

public class CoreClient {

    private static final Logger log = LoggerFactory.getLogger(CoreClient.class);
    private static final String EVENTO_ACTUALIZACION = "ACTUALIZACION";

    private final RestClient restClient;
    private final String url;
    private final String apiKey;

    public CoreClient(RestClient.Builder builder,
                      @Value("${app.core.url}") String url,
                      @Value("${app.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = builder.requestFactory(requestFactory).build();
        this.url = url;
        this.apiKey = apiKey;
    }

    /** Envía el evento cuando la transacción se confirma, para no avisar al CORE de algo que luego se revierte. */
    public void notificarActualizacion(Long polizaId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(EVENTO_ACTUALIZACION, polizaId);
                }
            });
        } else {
            enviar(EVENTO_ACTUALIZACION, polizaId);
        }
    }

    private void enviar(String evento, Long polizaId) {
        try {
            restClient.post()
                    .uri(url)
                    .header(ApiKeyFilter.HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CoreEventoRequest(evento, polizaId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento {} de la póliza {} enviado al CORE", evento, polizaId);
        } catch (RestClientException ex) {
            log.warn("No fue posible notificar al CORE el evento {} de la póliza {}: {}",
                    evento, polizaId, ex.getMessage());
        }
    }
}
