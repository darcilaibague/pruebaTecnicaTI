package com.seguros.polizas.controller;

import com.seguros.polizas.repository.PolizaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PolizaApiIntegrationTest {

    private static final String HEADER = "x-api-key";
    private static final String API_KEY = "123456";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PolizaRepository polizaRepository;

    private Long idDe(String numero) {
        return polizaRepository.findByNumero(numero).orElseThrow().getId();
    }

    @Test
    void sinApiKeyResponde401() throws Exception {
        mvc.perform(get("/polizas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("API_KEY_INVALIDA"));
    }

    @Test
    void conApiKeyIncorrectaResponde401() throws Exception {
        mvc.perform(get("/polizas").header(HEADER, "000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listaPolizasFiltrandoPorTipoYEstado() throws Exception {
        mvc.perform(get("/polizas").param("tipo", "COLECTIVA").param("estado", "VIGENTE").header(HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value("POL-COL-001"));
    }

    @Test
    void listaTodasLasPolizasSinFiltros() throws Exception {
        mvc.perform(get("/polizas").header(HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void filtroConValorInvalidoResponde400() throws Exception {
        mvc.perform(get("/polizas").param("tipo", "OTRO").header(HEADER, API_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    void listaLosRiesgosDeUnaPoliza() throws Exception {
        mvc.perform(get("/polizas/{id}/riesgos", idDe("POL-COL-001")).header(HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void polizaInexistenteResponde404() throws Exception {
        mvc.perform(get("/polizas/{id}/riesgos", 9999).header(HEADER, API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("POLIZA_NO_ENCONTRADA"));
    }

    @Test
    void renovarUnaPolizaVigenteLaDejaRenovada() throws Exception {
        mvc.perform(post("/polizas/{id}/renovar", idDe("POL-IND-001")).header(HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"));
    }

    @Test
    void renovarUnaPolizaCanceladaResponde409() throws Exception {
        mvc.perform(post("/polizas/{id}/renovar", idDe("POL-COL-002")).header(HEADER, API_KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("POLIZA_CANCELADA"));
    }

    @Test
    void cancelarUnaPolizaCancelaTodosSusRiesgos() throws Exception {
        Long id = idDe("POL-COL-001");

        mvc.perform(post("/polizas/{id}/cancelar", id).header(HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mvc.perform(get("/polizas/{id}/riesgos", id).header(HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].estado", everyItem(is("CANCELADO"))));
    }

    @Test
    void agregarRiesgoAUnaColectivaResponde201() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", idDe("POL-COL-001"))
                        .header(HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direccionInmueble": "Calle 100 # 15-20, Bogotá",
                                 "arrendatario": "Camila Torres",
                                 "arrendador": "Felipe Rojas"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void agregarRiesgoAUnaIndividualResponde422() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", idDe("POL-IND-001"))
                        .header(HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direccionInmueble": "Otra dirección",
                                 "arrendatario": "Otro",
                                 "arrendador": "Otro"}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("TIPO_POLIZA_INVALIDO"));
    }

    @Test
    void agregarRiesgoSinDatosObligatoriosResponde400() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", idDe("POL-COL-001"))
                        .header(HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void cancelarUnRiesgoDeColectiva() throws Exception {
        Long riesgoId = polizaRepository.findByNumero("POL-COL-001").orElseThrow().getRiesgos().get(0).getId();

        mvc.perform(post("/riesgos/{id}/cancelar", riesgoId).header(HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    @Test
    void cancelarElUnicoRiesgoDeUnaIndividualResponde409() throws Exception {
        Long riesgoId = polizaRepository.findByNumero("POL-IND-001").orElseThrow().getRiesgos().get(0).getId();

        mvc.perform(post("/riesgos/{id}/cancelar", riesgoId).header(HEADER, API_KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RIESGO_UNICO_INDIVIDUAL"));
    }

    @Test
    void elMockDelCoreRegistraElEvento() throws Exception {
        mvc.perform(post("/core-mock/evento")
                        .header(HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evento": "ACTUALIZACION", "polizaId": 555}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.resultado").value("REGISTRADO"));
    }
}