package com.seguros.polizas.repository;

import com.seguros.polizas.domain.EstadoPoliza;
import com.seguros.polizas.domain.Poliza;
import com.seguros.polizas.domain.TipoPoliza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PolizaRepository extends JpaRepository <Poliza, Long> {

    @Query("""
            SELECT p FROM Poliza p
            WHERE (: tipo IS NULL OR p.tipo = :tipo)
                        AND (:estado IS NULL OR p.estado = :estado)
            ORDER BY p.id
            """)
    List<Poliza> buscar (@Param("tipo")TipoPoliza tipo, @Param("estado")EstadoPoliza estado);

    Optional<Poliza> findByNumero(String numero);
}
