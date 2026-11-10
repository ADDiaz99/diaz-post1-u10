package com.universidad.enviosservice.service;

import com.universidad.enviosservice.estrategia.EnvioEstandarStrategy;
import com.universidad.enviosservice.estrategia.EnvioExpressStrategy;
import com.universidad.enviosservice.estrategia.EnvioInternacionalStrategy;
import com.universidad.enviosservice.estrategia.EnvioOvernightStrategy;
import com.universidad.enviosservice.estrategia.EstrategiaEnvio;

import java.util.Map;

/**
 * Orquestador: elige la estrategia por el método de envío y aplica los ajustes que son
 * comunes a todos los métodos (fragilidad y tipo de cliente). Esos ajustes se quedan
 * aquí a propósito: moverlos a cada estrategia los duplicaría cuatro veces.
 */
public class ServicioCalculoEnvio {

    private final Map<String, EstrategiaEnvio> estrategias = Map.of(
            "ESTANDAR", new EnvioEstandarStrategy(),
            "EXPRESS", new EnvioExpressStrategy(),
            "OVERNIGHT", new EnvioOvernightStrategy(),
            "INTERNACIONAL", new EnvioInternacionalStrategy());

    public double calcularCosto(String metodoEnvio, double pesoKg, double distanciaKm,
                                String tipoCliente, boolean esFragil) {
        EstrategiaEnvio estrategia = estrategias.get(metodoEnvio);
        if (estrategia == null) {
            throw new IllegalArgumentException("Metodo de envio no soportado: " + metodoEnvio);
        }

        double costo = estrategia.calcular(pesoKg, distanciaKm);

        if (esFragil) {
            costo = costo + costo * 0.15;
        }

        if (tipoCliente.equals("PREMIUM")) {
            costo = costo * 0.9;
        } else if (tipoCliente.equals("CORPORATIVO")) {
            costo = costo * 0.85;
        }

        return costo;
    }
}
