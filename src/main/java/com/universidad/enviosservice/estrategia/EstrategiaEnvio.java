package com.universidad.enviosservice.estrategia;

/**
 * Strategy: cada método de envío calcula el mismo tipo de resultado (el costo base)
 * con una fórmula propia. Los ajustes comunes (fragilidad, tipo de cliente) no viven
 * aquí, sino en el orquestador.
 */
public interface EstrategiaEnvio {

    double calcular(double pesoKg, double distanciaKm);
}
