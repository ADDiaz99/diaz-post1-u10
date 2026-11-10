package com.universidad.enviosservice.estrategia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvioOvernightStrategyTest {

    private final EnvioOvernightStrategy estrategia = new EnvioOvernightStrategy();

    // Ajuste sobre la guía: para 25 kg y 100 km la guía espera 250.0, pero la fórmula da
    // 100 * 2.0 + 20 + 15 * 2.5 + 5 * 3.0 = 272.5. Con 250.0 la prueba fallaría.
    @ParameterizedTest(name = "{0} kg, {1} km -> {2}")
    @CsvSource({
            "3.0,  100.0, 220.0",    // pesoKg <= 5
            "8.0,  100.0, 227.5",    // 5 < pesoKg <= 20
            "25.0, 100.0, 272.5"     // pesoKg > 20
    })
    void calculaCostoPorTramoDePeso(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.001);
    }

    @Test
    void aceptaExactamente300Km() {
        assertEquals(620.0, estrategia.calcular(3.0, 300.0), 0.001);
    }

    @Test
    void rechazaDistanciasMayoresA300Km() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> estrategia.calcular(3.0, 301.0));
        assertEquals("OVERNIGHT no disponible para mas de 300 km", error.getMessage());
    }
}
