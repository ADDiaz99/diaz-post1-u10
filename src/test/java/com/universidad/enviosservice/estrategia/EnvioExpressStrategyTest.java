package com.universidad.enviosservice.estrategia;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvioExpressStrategyTest {

    private final EnvioExpressStrategy estrategia = new EnvioExpressStrategy();

    @ParameterizedTest(name = "{0} kg, {1} km -> {2}")
    @CsvSource({
            "3.0,  100.0, 156.0",    // tramo 1: 100 * 1.2 = 120, por 1.3
            "8.0,  100.0, 161.85",   // tramo 2: (120 + 3 * 1.5) * 1.3
            "25.0, 100.0, 198.25"    // tramo 3: (120 + 22.5 + 5 * 2.0) * 1.3
    })
    void calculaCostoPorTramoDePesoConFactorExpress(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.001);
    }

    @ParameterizedTest(name = "{1} km -> {2}")
    @CsvSource({
            "3.0, 500.0, 780.0",     // en el límite: 600 * 1.3, sin recargo
            "3.0, 600.0, 968.5"      // recargo de 25 ANTES del factor: (720 + 25) * 1.3
    })
    void aplicaRecargoAntesDelFactor(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.001);
    }
}
