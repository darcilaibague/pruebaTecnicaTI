package com.seguros.polizas.config;

import com.seguros.polizas.PolizasApplication;
import com.seguros.polizas.domain.Poliza;
import com.seguros.polizas.repository.PolizaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DataLoader implements CommandLineRunner{


    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final PolizaRepository polizaRepository;
    private final BigDecimal ipc;


    public DataLoader(PolizaRepository polizaRepository,@Value("${app.ipc}") BigDecimal ipc) {
        this.polizaRepository = polizaRepository;
        this.ipc = ipc;
    }

    @Override
    public void run(String... args) {
        if (polizaRepository.count() > 0) {
            return;
        }

        // id 1: individual vigente con su único riesgo
        Poliza individual = Poliza.individual("POL-IND-001", "Carlos Pérez", "María Gómez",
                "Calle 10 # 5-20, Bogotá", LocalDate.of(2026, 1, 1), 12, new BigDecimal("1500000"));

        // id 2: colectiva vigente con dos riesgos
        Poliza colectiva = Poliza.colectiva("POL-COL-001", "Inmobiliaria Los Andes S.A.S.",
                LocalDate.of(2026, 3, 1), 12, new BigDecimal("4200000"));
        colectiva.agregarRiesgo("Carrera 7 # 45-10, Apto 301, Bogotá", "Laura Rodríguez", "Jorge Martínez");
        colectiva.agregarRiesgo("Avenida 68 # 12-30, Apto 502, Bogotá", "Andrés López", "Patricia Herrera");

        // id 3: colectiva cancelada
        Poliza colectivaCancelada = Poliza.colectiva("POL-COL-002", "Administración Torres del Parque",
                LocalDate.of(2025, 6, 1), 12, new BigDecimal("3000000"));
        colectivaCancelada.agregarRiesgo("Calle 26 # 5-60, Apto 1203, Bogotá", "Sofía Vargas", "Ricardo Ortiz");
        colectivaCancelada.cancelar();

        // id 4: individual ya renovada una vez
        Poliza individualRenovada = Poliza.individual("POL-IND-002", "Diana Castro", "Luis Ramírez",
                "Carrera 43A # 1-50, Medellín", LocalDate.of(2025, 2, 1), 12, new BigDecimal("2000000"));
        individualRenovada.renovar(ipc);

        polizaRepository.saveAll(List.of(individual, colectiva, colectivaCancelada, individualRenovada));
        log.info("Datos de ejemplo cargados: {} pólizas", polizaRepository.count());
    }
}
