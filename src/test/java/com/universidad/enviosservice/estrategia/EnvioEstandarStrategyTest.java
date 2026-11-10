package com.universidad.enviosservice.estrategia;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvioEstandarStrategyTest {

    private final EnvioEstandarStrategy estrategia = new EnvioEstandarStrategy();

    @ParameterizedTest(name = "{0} kg, {1} km -> {2}")
    @CsvSource({
            "5.0,  100.0, 50.0",   // límite superior del tramo 1 (pesoKg <= 5)
            "8.0,  100.0, 52.4",   // tramo 2 (5 < pesoKg <= 20)
            "20.0, 100.0, 62.0",   // límite superior del tramo 2
            "25.0, 100.0, 68.0"    // tramo 3 (pesoKg > 20)
    })
    void calculaCostoPorTramoDePeso(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.001);
    }

    @ParameterizedTest(name = "{1} km -> {2}")
    @CsvSource({
            "3.0, 500.0, 250.0",   // en el límite: sin recargo
            "3.0, 501.0, 260.5"    // por encima de 500 km: recargo de 10
    })
    void aplicaRecargoSoloPorEncimaDe500Km(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.001);
    }
}
