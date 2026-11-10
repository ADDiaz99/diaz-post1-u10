package com.universidad.enviosservice.estrategia;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvioInternacionalStrategyTest {

    private final EnvioInternacionalStrategy estrategia = new EnvioInternacionalStrategy();

    @ParameterizedTest(name = "{0} kg, {1} km -> {2}")
    @CsvSource({
            "3.0,  100.0, 230.0",    // tramo 1: 180 + 50
            "8.0,  100.0, 239.0",    // tramo 2: 230 + 3 * 3.0
            "25.0, 100.0, 297.5"     // tramo 3: 230 + 45 + 5 * 4.5
    })
    void calculaCostoPorTramoDePeso(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.001);
    }

    @ParameterizedTest(name = "{1} km -> {2}")
    @CsvSource({
            "3.0, 5000.0, 9050.0",   // en el límite: sin recargo
            "3.0, 5001.0, 9151.8"    // por encima de 5000 km: recargo de 100
    })
    void aplicaRecargoSoloPorEncimaDe5000Km(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.001);
    }
}
