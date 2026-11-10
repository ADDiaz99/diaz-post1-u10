# Envios Service: Análisis de Métricas de Calidad

**Post-contenido 1, Unidad 10: Métricas de Calidad y SonarQube**
Andrés David Díaz · Ingeniería de Sistemas · Patrones de Diseño de Software · UDES

Proyecto Maven plano (sin Spring Boot) que calcula el costo de un envío según el método de
transporte, el peso, la distancia, la fragilidad y el tipo de cliente. La Parte 1 diagnostica con
métricas reales; la Parte 2 refactoriza guiada por esas métricas.

| Elemento | Versión |
|---|---|
| Java | JDK 26 instalado, compilando con `release 21` (el nivel que pide la guía) |
| Maven | 3.9 |
| Pruebas | JUnit 5.14 |
| Cobertura | JaCoCo 0.8.15 (primera versión con soporte oficial para Java 26) |
| Análisis estático | SonarQube Community Build en Docker, SonarScanner for Maven 5.8 |

## Entorno de análisis

**Decisión de diseño 1: SonarQube local con Docker.** Elegí Docker por tres razones:

1. **Configuración (decisiva).** La Parte 2 exige crear un Quality Profile y un Quality Gate
   propios ("Estándar Laboratorio"). Según la documentación de SonarQube Cloud, los Quality
   Profiles y Quality Gates personalizados solo existen en los planes Team y Enterprise; el plan
   gratuito se queda con "Sonar way". En la instancia local tengo control total de la
   configuración, que es la ventaja de la tabla de la guía.
2. **Conectividad y límites.** El análisis local no depende de internet ni de los límites de un
   plan gratuito, y puedo repetirlo las veces que haga falta.
3. **Costo aceptado.** Docker consume memoria y hay que mantener el contenedor corriendo durante
   el laboratorio. Con 32 GB de RAM en mi equipo, ese costo es menor.

## Exclusiones del análisis

**Decisión de diseño 2: no excluir nada.** El proyecto tiene una sola clase de producción,
`ServicioCalculoEnvio`, y es justamente la lógica de negocio que se quiere medir. No hay clase de
arranque (`main`), configuración ni código generado que distorsione la cobertura. Excluir algo
solo serviría para mejorar el número, que es lo contrario del objetivo de esta unidad. Si el
proyecto creciera con una clase `Main` sin lógica, la excluiría con `sonar.exclusions`.

Nota sobre la configuración: el análisis se ejecuta con `mvn sonar:sonar` (SonarScanner for
Maven), que toma sus parámetros del `pom.xml`. El archivo `sonar-project.properties` que pide la
guía está en la raíz, pero lo usa el escáner de línea de comandos (`sonar-scanner`); por eso las
mismas propiedades están también en la sección `<properties>` del `pom.xml`.

## Estado inicial del análisis

| Métrica | Valor real observado |
|---|---|
| Complejidad ciclomática de `calcularCosto()` | 20 |
| Cobertura de líneas de `ServicioCalculoEnvio` | 23.8 % (10 de 42 líneas) |
| Cobertura de ramas de `ServicioCalculoEnvio` | 18.4 % (7 de 38 ramas) |
| Code Smells reportados | [COMPLETAR con el valor de tu dashboard] |

## Interpretación

**El problema está en un solo método: `calcularCosto()`.** Es el único método con lógica de la
clase, y sus 19 puntos de decisión (4 por el método de envío, 12 por los tramos de peso y los
recargos de cada método, y 3 por fragilidad y tipo de cliente) le dan una complejidad
ciclomática de 20: veinte caminos independientes que una suite completa tendría que ejercitar.

**Qué ramas quedaron sin cubrir.** Las dos pruebas de partida usan ESTANDAR, 3 kg, 100 km y un
paquete no frágil, cambiando solo el cliente (REGULAR y PREMIUM). El reporte de JaCoCo marca en
rojo o amarillo todo lo demás:

| Zona del método | Ramas sin cubrir |
|---|---|
| ESTANDAR | tramos de 5 a 20 kg y de más de 20 kg; recargo por más de 500 km |
| EXPRESS | el método completo: tres tramos de peso, recargo y factor 1.3 |
| OVERNIGHT | el método completo, incluida la excepción por más de 300 km |
| INTERNACIONAL | el método completo, incluido el recargo por más de 5000 km |
| Método no soportado | la excepción final |
| Ajustes comunes | el recargo por fragilidad y el descuento CORPORATIVO |

Solo 7 de las 38 ramas se ejecutaron: la entrada a ESTANDAR, el primer tramo de peso, la
distancia sin recargo, el caso no frágil y las dos salidas de la condición PREMIUM más la falsa
de CORPORATIVO.

**Decisión de diseño 3: la complejidad ciclomática como señal principal.** SonarQube ofrece tres
señales: complejidad ciclomática, complejidad cognitiva y cobertura. Aquí las tres apuntan al
mismo método porque la clase tiene uno solo, pero elegí la ciclomática como señal principal por
lo que mide: el número de caminos independientes, es decir, cuántos casos de prueba hacen falta
para cubrir el método (McCabe, 1976). Eso explica directamente el otro síntoma: con 20 caminos y
2 pruebas, la cobertura de ramas no podía ser alta. La cobertura confirma el diagnóstico, pero
por sí sola no dice dónde está la causa; una cobertura baja se arreglaría "agregando pruebas",
mientras que la complejidad muestra que el método mezcla cuatro algoritmos distintos y que el
problema es de estructura. La complejidad cognitiva también es alta por el anidamiento, pero
mide legibilidad, no cantidad de casos a probar, que es lo que este laboratorio busca mejorar.

## Capturas del análisis inicial

![Dashboard SonarQube](docs/sonar-dashboard-inicial.png)
![Reporte JaCoCo](docs/jacoco-inicial.png)

## Cómo ejecutar

```bash
# SonarQube local (una sola vez)
docker run -d --name sonarqube -p 9000:9000 -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true sonarqube:community

mvn clean verify                                   # pruebas + reporte JaCoCo
mvn clean verify sonar:sonar -Dsonar.token=TOKEN   # pruebas + JaCoCo + análisis
```

## Referencias

- McCabe, T. J. (1976). A complexity measure. *IEEE Transactions on Software Engineering, SE-2*(4), 308-320.
- SonarSource. (s. f.). *Managing custom quality gates*. SonarQube Cloud documentation. https://docs.sonarsource.com/sonarqube-cloud/standards/managing-quality-gates/managing-custom-quality-gates
