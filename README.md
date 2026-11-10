# Envios Service: Diagnóstico y Mejora Guiada por Métricas

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

## Decisiones de diseño (resumen)

1. **Entorno de análisis:** SonarQube local con Docker, porque el plan gratuito de SonarQube Cloud no
   permite el Quality Profile ni el Quality Gate propios que exige la Parte 2.
2. **Exclusiones:** ninguna; la única clase es la lógica de negocio que se quiere medir.
3. **Señal principal:** complejidad ciclomática, porque cuenta los caminos que hay que probar y
   explica la cobertura baja.
4. **Patrón:** Strategy; el método no guarda estado (descarta State) y sus ramas son excluyentes y
   sin orden (descarta Chain of Responsibility).
5. **Pruebas:** una por cada rama que JaCoCo mostró sin cubrir, con valores en los límites.
6. **Quality Gate:** complejidad 10 por método y 70 % de cobertura, exigentes sin forzar pruebas vacías.

## Parte 1: diagnóstico con métricas reales

### Entorno de análisis

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

### Exclusiones del análisis

**Decisión de diseño 2: no excluir nada.** El proyecto tiene una sola clase de producción,
`ServicioCalculoEnvio`, y es justamente la lógica de negocio que se quiere medir. No hay clase de
arranque (`main`), configuración ni código generado que distorsione la cobertura. Excluir algo
solo serviría para mejorar el número, que es lo contrario del objetivo de esta unidad. Si el
proyecto creciera con una clase `Main` sin lógica, la excluiría con `sonar.exclusions`.

Nota sobre la configuración: el análisis se ejecuta con `mvn sonar:sonar` (SonarScanner for
Maven), que toma sus parámetros del `pom.xml`. El archivo `sonar-project.properties` que pide la
guía está en la raíz, pero lo usa el escáner de línea de comandos (`sonar-scanner`); por eso las
mismas propiedades están también en la sección `<properties>` del `pom.xml`.

### Estado inicial del análisis

| Métrica | Valor real observado |
|---|---|
| Complejidad ciclomática de `calcularCosto()` | 20 |
| Cobertura de líneas de `ServicioCalculoEnvio` | 23.8 % (10 de 42 líneas) |
| Cobertura de ramas de `ServicioCalculoEnvio` | 18.4 % (7 de 38 ramas) |
| Code Smells reportados | 20.0 % (2 de 10) |

### Interpretación

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


## Parte 2: refactorización guiada por métricas

### Patrón elegido

**Decisión de diseño 4: Strategy.** Antes de escribir código revisé qué hacen realmente los
condicionales de `calcularCosto()` con las tres preguntas de la guía:

- **¿Guarda estado entre llamadas?** No. La clase no tenía ningún campo: cada llamada recibe todos
  sus datos como parámetros y no deja nada para la siguiente. Eso **descarta State**, que existe
  para un objeto que recuerda en qué estado está y cuyo comportamiento depende de transiciones.
- **¿Hay una secuencia de responsables donde el orden importa?** No. Las ramas de `metodoEnvio`
  comparan por igualdad contra valores distintos, así que exactamente una aplica en cada llamada;
  reordenarlas no cambia ningún resultado. Eso **descarta Chain of Responsibility**, que se
  justifica cuando varios manejadores podrían aplicar y el orden de evaluación decide cuál lo hace.
- **¿Cada rama calcula el mismo tipo de resultado con su propia fórmula?** Sí. Las cuatro ramas
  producen un costo, cada una con tarifas, tramos y recargos propios, elegida una sola vez por el
  valor de `metodoEnvio`. Es la señal de **Strategy**.

**Qué se movió y qué se quedó.** Cada estrategia (`EnvioEstandarStrategy`, `EnvioExpressStrategy`,
`EnvioOvernightStrategy`, `EnvioInternacionalStrategy`) se llevó todo lo que dependía solo de su
método: sus tramos de peso, su recargo por distancia, el factor 1.3 de EXPRESS y la restricción de
300 km de OVERNIGHT. En `ServicioCalculoEnvio` se quedaron la selección de la estrategia (un
`Map`) y los dos ajustes que se aplican igual a cualquier método: fragilidad y tipo de cliente.
Moverlos a las estrategias habría duplicado esa lógica cuatro veces; por eso el orquestador no
baja a complejidad 1, sino a 5 (base, estrategia nula, fragilidad y las dos condiciones de
cliente). Cada estrategia queda con complejidad 4.

**Mismo comportamiento.** Antes de reemplazar el método comparé la versión original con la
refactorizada en 3600 combinaciones de entrada (los cinco métodos, incluido uno no soportado,
cuatro tipos de cliente, pesos y distancias en los límites de cada tramo, frágil y no frágil): los
resultados y las excepciones fueron idénticos. Las dos pruebas de partida siguen pasando sin
cambios.

### Pruebas agregadas

**Decisión de diseño 5: una prueba por rama sin cubrir, con valores en los límites.** El criterio
fue que cada prueba nueva cubra una rama que el reporte JaCoCo de la Parte 1 mostró sin ejercitar
y verifique un valor **distinto** y calculado a mano. Los casos están en los límites de cada
condición (5 y 20 kg, 300, 500 y 5000 km), porque es donde un `<=` cambiado por `<` pasaría
inadvertido.

| Rama sin cubrir en la Parte 1 | Prueba que la cubre | Valor verificado |
|---|---|---|
| ESTANDAR, tramos 5-20 kg y más de 20 kg | `EnvioEstandarStrategyTest.calculaCostoPorTramoDePeso` | 5 kg: 50.0; 8 kg: 52.4; 20 kg: 62.0; 25 kg: 68.0 |
| ESTANDAR, recargo por más de 500 km | `EnvioEstandarStrategyTest.aplicaRecargoSoloPorEncimaDe500Km` | 500 km: 250.0; 501 km: 260.5 |
| EXPRESS, los tres tramos y el factor 1.3 | `EnvioExpressStrategyTest.calculaCostoPorTramoDePesoConFactorExpress` | 156.0; 161.85; 198.25 |
| EXPRESS, recargo antes del factor | `EnvioExpressStrategyTest.aplicaRecargoAntesDelFactor` | 500 km: 780.0; 600 km: 968.5 |
| OVERNIGHT, los tres tramos | `EnvioOvernightStrategyTest.calculaCostoPorTramoDePeso` | 220.0; 227.5; 272.5 |
| OVERNIGHT, límite de 300 km y excepción | `aceptaExactamente300Km`, `rechazaDistanciasMayoresA300Km` | 620.0; excepción con su mensaje |
| INTERNACIONAL, los tres tramos | `EnvioInternacionalStrategyTest.calculaCostoPorTramoDePeso` | 230.0; 239.0; 297.5 |
| INTERNACIONAL, recargo por más de 5000 km | `EnvioInternacionalStrategyTest.aplicaRecargoSoloPorEncimaDe5000Km` | 5000 km: 9050.0; 5001 km: 9151.8 |
| Fragilidad y CORPORATIVO | `ServicioCalculoEnvioTest.aplicaAjustesPorFragilidadYTipoDeCliente` | 57.5; 42.5; 48.875; 51.75 |
| Selección de la estrategia en el orquestador | `ServicioCalculoEnvioTest.delegaEnLaEstrategiaDeCadaMetodo` | EXPRESS 156.0; OVERNIGHT 220.0; INTERNACIONAL 230.0 |
| Método no soportado | `ServicioCalculoEnvioTest.rechazaUnMetodoDeEnvioNoSoportado` | excepción con su mensaje |

En total la suite pasa de 2 a 31 casos (contando cada fila de una prueba parametrizada). Ninguno
repite lo que ya verificaba otra prueba: las filas de `delegaEnLaEstrategiaDeCadaMetodo` usan
entradas que también aparecen en las pruebas de cada estrategia, pero verifican otra cosa: que el
orquestador elija la estrategia correcta.

Corrección a la guía: para OVERNIGHT con 25 kg y 100 km la guía espera 250.0, pero la fórmula da
100 x 2.0 + 20 + 15 x 2.5 + 5 x 3.0 = 272.5. Con el valor de la guía, esa prueba fallaría.

### Quality Gate

Configuración en SonarQube:

- **Quality Profile "Estándar Laboratorio"**: copia de "Sonar way" para Java con la regla S1541
  (*Cyclomatic Complexity of methods should not exceed the authorized threshold*) activada y
  `Threshold = 10`, asignado al proyecto.
- **Quality Gate "Estándar Laboratorio"**, asignado al proyecto, con tres condiciones:
  Coverage menor a 70 %, New Code Smells mayor a 0 y Duplicated Lines (%) mayor a 5 %.

![Quality Profile](docs/quality-profile.png)
![Quality Gate](docs/quality-gate.png)

**Decisión de diseño 6: por qué complejidad 10 y cobertura 70 %.**

- **Complejidad 10 por método.** Es el techo del rango "Moderada" de la Tabla 3.2 de la guía y el
  límite que McCabe (1976) propuso originalmente. Con 5, métodos razonables como el orquestador
  (complejidad 5) quedarían en el límite y cualquier regla nueva obligaría a partirlos sin
  necesidad; con 20, el método original de este proyecto, con 20 caminos, habría pasado. Un
  matiz: la guía dice que 10 es "el valor por defecto del propio perfil Sonar Way", pero eso no es
  exacto. 10 es el valor por defecto del parámetro de la regla S1541, pero la regla **no está
  activa** en Sonar way, que controla la complejidad con la regla cognitiva S3776. Por eso hubo que
  crear el perfil propio para activarla.
- **Cobertura 70 %.** Exigir 100 % empuja a escribir pruebas triviales solo para llenar el número
  (el incentivo perverso de la Sección 2.3 de la guía), y deja sin margen el código de
  infraestructura que no vale la pena probar. Con 70 % la exigencia es real sin obligar a mentir.
  Que este proyecto llegue al 100 % es una consecuencia de su tamaño, no el objetivo.
- **Code Smells nuevos mayor a 0.** Es la forma de convertir la regla S1541 en un bloqueo: un
  Quality Gate no puede poner un límite de complejidad por método, pero sí puede rechazar
  cualquier Code Smell nuevo, y un método de más de 10 genera uno.

## Comparación antes / después

| Métrica | Antes (Parte 1) | Después (Parte 2) |
|---|---|---|
| Complejidad ciclomática de `calcularCosto()` | 20 | 5 (75 % menos) |
| Complejidad máxima de un método de la lógica de envío | 20 | 5 (orquestador); 4 cada estrategia |
| Cobertura de ramas de la lógica de envío | 18.4 % (7/38) | 100 % (32/32) |
| Cobertura de líneas de la lógica de envío | 23.8 % (10/42) | 100 % (48/48) |
| Pruebas | 2 | 31 |
| Code Smells | 20.0 % (2/10) | 0.0 % (0/10) |
| Quality Gate | No configurado | Passed |


La reducción de complejidad supera el 50 % que pide la guía y la cobertura de ramas sube 81.6
puntos, muy por encima de los 15 pedidos. El orquestador no baja de 5 a propósito, por las reglas
transversales explicadas en la Decisión de diseño 4.

## Cómo ejecutar

```bash
# SonarQube local (una sola vez)
docker run -d --name sonarqube -p 9000:9000 -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true sonarqube:community

mvn clean verify                                   # 31 pruebas + reporte JaCoCo
mvn clean verify sonar:sonar -Dsonar.token=TOKEN   # pruebas + JaCoCo + análisis
```

## Herramientas utilizadas

- Java (JDK 26, release 21), Apache Maven 3.9
- SonarQube Community Build (Docker), SonarScanner for Maven, JaCoCo, JUnit 5
- Git, GitHub

## Conclusiones

Las métricas sirvieron para diagnosticar, no como meta. La complejidad ciclomática de 20 no solo
señalaba un método difícil de leer: explicaba por qué dos pruebas cubrían apenas el 18 % de las
ramas, y apuntaba a la causa, cuatro algoritmos mezclados en un solo método. Elegir el patrón por
la naturaleza real de los condicionales, sin estado y sin orden, llevó a Strategy y redujo la
complejidad sin cambiar el comportamiento, como lo confirmó la comparación de 3600 casos. Después,
las pruebas nuevas subieron la cobertura porque cada una verifica un valor en un límite, no porque
se buscara el número. Por último, configurar el Quality Gate mostró que un umbral es una decisión
de diseño que hay que justificar: 10 y 70 % son exigentes sin premiar las pruebas vacías.

## Referencias

- McCabe, T. J. (1976). A complexity measure. *IEEE Transactions on Software Engineering, SE-2*(4), 308-320.
- SonarSource. (s. f.). *Managing custom quality gates*. SonarQube Cloud documentation. https://docs.sonarsource.com/sonarqube-cloud/standards/managing-quality-gates/managing-custom-quality-gates
- Sonar Community. (2022). *Is S3776 the new S1541?* https://community.sonarsource.com/t/is-s3776-the-new-s1541/56605
