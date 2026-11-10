package com.universidad.enviosservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas del orquestador: qué estrategia elige y los ajustes comunes a todos los
 * métodos (fragilidad y tipo de cliente). Las fórmulas de cada método se prueban en
 * las pruebas de su estrategia.
 */
class ServicioCalculoEnvioTest {

    private final ServicioCalculoEnvio servicio = new ServicioCalculoEnvio();

    // Las dos pruebas de partida se conservan sin cambios.

    @Test
    void calculaEnvioEstandarPesoBajo() {
        double costo = servicio.calcularCosto("ESTANDAR", 3.0, 100.0, "REGULAR", false);
        assertEquals(50.0, costo, 0.01);
    }

    @Test
    void aplicaDescuentoClientePremium() {
        double costo = servicio.calcularCosto("ESTANDAR", 3.0, 100.0, "PREMIUM", false);
        assertEquals(45.0, costo, 0.01);
    }

    // Pruebas nuevas: ramas que el reporte JaCoCo de la Parte 1 mostró sin cubrir.

    // Ajustes comunes: fragilidad (+15 %) y tipo de cliente (PREMIUM -10 %, CORPORATIVO -15 %).
    // Se agrupan en una prueba parametrizada; como pruebas separadas casi idénticas,
    // SonarQube las marcaría como Code Smell (regla S5976).
    @ParameterizedTest(name = "{0}, fragil={1} -> {2}")
    @CsvSource({
            "REGULAR,     true,  57.5",     // 50 * 1.15
            "CORPORATIVO, false, 42.5",     // 50 * 0.85
            "CORPORATIVO, true,  48.875",   // fragilidad primero: 57.5 * 0.85
            "PREMIUM,     true,  51.75"     // 57.5 * 0.9
    })
    void aplicaAjustesPorFragilidadYTipoDeCliente(String tipoCliente, boolean esFragil, double esperado) {
        double costo = servicio.calcularCosto("ESTANDAR", 3.0, 100.0, tipoCliente, esFragil);
        assertEquals(esperado, costo, 0.001);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "EXPRESS,       156.0",
            "OVERNIGHT,     220.0",
            "INTERNACIONAL, 230.0"
    })
    void delegaEnLaEstrategiaDeCadaMetodo(String metodoEnvio, double esperado) {
        double costo = servicio.calcularCosto(metodoEnvio, 3.0, 100.0, "REGULAR", false);
        assertEquals(esperado, costo, 0.001);
    }

    @Test
    void rechazaUnMetodoDeEnvioNoSoportado() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.calcularCosto("MARITIMO", 3.0, 100.0, "REGULAR", false));
        assertEquals("Metodo de envio no soportado: MARITIMO", error.getMessage());
    }
}
