# Cuestionario Médico - Modelo de Gail

## Descripción

Aplicación independiente que simula un cuestionario médico interactivo para evaluar el riesgo de cáncer de mama usando el **Modelo de Gail (BCRAT - Breast Cancer Risk Assessment Tool)**.

### Características

✅ **Carga automática de ontología**: La ontología `BreastCancerRecommendationWithMetamodelling.owx` se carga automáticamente al iniciar  
✅ **Preguntas hardcodeadas**: 7 preguntas del modelo Gail predefinidas  
✅ **Interfaz independiente**: No requiere navegar por tabs ni seleccionar ontologías manualmente  
✅ **Guardado automático**: Las respuestas se guardan en la ontología  
✅ **Navegación intuitiva**: Botones Anterior/Siguiente con progreso visual  
✅ **Interfaz moderna**: Diseño limpio con Material Design 3

## Ejecutar la aplicación

### Opción 1: Script directo (recomendado)

```bash
./run-gail-questionnaire.sh
```

### Opción 2: Comando Gradle

```bash
./gradlew runGailQuestionnaire
```

## Preguntas del Modelo Gail

El cuestionario incluye las siguientes 7 preguntas:

### 1. **Edad actual**
- Menor de 35 años
- 35 a 39 años
- 40 a 44 años
- 45 a 49 años
- 50 a 54 años
- 55 a 59 años
- 60 a 64 años
- 65 a 69 años
- 70 años o más

**Factor de riesgo**: Edad

### 2. **Edad de primera menstruación**
- Menor de 11 años
- 11 años
- 12 años
- 13 años
- 14 años o más
- No recuerdo

**Factor de riesgo**: Historia hormonal

### 3. **Edad del primer hijo nacido vivo**
- No he tenido hijos
- Menor de 20 años
- 20 a 24 años
- 25 a 29 años
- 30 años o más

**Factor de riesgo**: Historia reproductiva

### 4. **Familiares de primer grado con cáncer de mama**
- Ninguno
- Uno
- Dos
- Tres o más
- No estoy segura

**Factor de riesgo**: Historia familiar

### 5. **Número de biopsias de mama**
- Ninguna
- Una
- Dos o más
- No estoy segura

**Factor de riesgo**: Historia médica

### 6. **Hiperplasia atípica en biopsias**
- No
- Sí
- No lo sé
- No me han hecho biopsias

**Factor de riesgo**: Historia médica

### 7. **Raza/Etnicidad**
- Blanca
- Afroamericana
- Hispana
- Asiática
- Otra

**Factor de riesgo**: Demografía

## Funcionamiento

### 1. Inicio de la aplicación
Al ejecutar, la aplicación:
- Muestra una pantalla de carga
- Carga automáticamente la ontología desde `ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx`
- Verifica que la ontología existe y es válida
- Muestra el cuestionario

### 2. Responder el cuestionario
- Se muestra una pregunta a la vez
- El factor de riesgo asociado aparece destacado
- Selecciona una respuesta haciendo clic en ella
- La respuesta seleccionada se marca con ✓
- Usa "Siguiente →" para avanzar
- Usa "← Anterior" para retroceder
- El botón "Siguiente" solo se habilita si respondiste la pregunta actual

### 3. Finalizar
- Al responder todas las preguntas, el botón "Siguiente" cambia a "Finalizar"
- Al hacer clic en "Finalizar", las respuestas se guardan automáticamente en la ontología
- Se crea un archivo nuevo en `ontologias/custom/BreastCancer_GailResponses_[timestamp].owl`

### 4. Indicadores visuales
- **Barra de progreso**: Muestra qué proporción del cuestionario has completado
- **Contador**: "Pregunta X de 7"
- **Progreso de respuestas**: "X de 7 preguntas respondidas"

## Estructura del código

### Archivos principales

1. **GailQuestionnaireApp.kt**
   - Interfaz de usuario del cuestionario
   - Componentes visuales (preguntas, respuestas, navegación)
   - Preguntas hardcodeadas del modelo Gail

2. **GailQuestionnaireMain.kt**
   - Punto de entrada de la aplicación
   - Carga automática de ontología
   - Guardado de respuestas en la ontología
   - Gestión de estados (carga, error, listo)

3. **run-gail-questionnaire.sh**
   - Script de ejecución simplificado

4. **build.gradle.kts**
   - Tarea `runGailQuestionnaire` configurada

### Modelos de datos

```kotlin
data class GailQuestion(
    val id: String,              // Identificador único
    val text: String,            // Texto de la pregunta
    val riskFactor: String,      // Factor de riesgo asociado
    val answers: List<GailAnswer> // Opciones de respuesta
)

data class GailAnswer(
    val id: String,              // Identificador único
    val text: String             // Texto de la respuesta
)

data class GailQuestionnaireState(
    val currentQuestionIndex: Int,     // Pregunta actual
    val responses: Map<String, String> // Respuestas seleccionadas
)
```

## Guardado de respuestas

Las respuestas se guardan en la ontología de la siguiente manera:

1. Se crea un individuo `CurrentPatient` en la ontología
2. Cada respuesta se vincula usando la propiedad `hasAnswerValue`
3. Los identificadores de respuestas se convierten a IRIs de la ontología
4. La ontología modificada se guarda en un archivo nuevo con timestamp

Ejemplo de IRI generado:
```
http://purl.org/ontology/breast_cancer_recommendation#age_40_44
```

## Requisitos

- Java 11+
- La ontología debe existir en: `ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx`

## Diferencias con el editor principal

Esta aplicación es **completamente independiente** del editor de ontologías principal:

| Característica | Editor Principal | Cuestionario Gail |
|----------------|------------------|-------------------|
| **Carga de ontología** | Manual | Automática |
| **Navegación** | Tabs múltiples | Interfaz única |
| **Preguntas** | Extraídas dinámicamente | Hardcodeadas |
| **Modelos** | Múltiples (Gail, IBIS, ACS, UY) | Solo Gail |
| **Propósito** | Editor genérico | Cuestionario específico |

## Ventajas de esta implementación

✅ **Simplicidad**: No requiere conocimientos previos de la ontología  
✅ **Rapidez**: Inicia directamente en el cuestionario  
✅ **Foco**: Diseñada específicamente para el modelo Gail  
✅ **Portabilidad**: Puede ejecutarse como aplicación independiente  
✅ **Mantenimiento**: Preguntas hardcodeadas facilitan modificaciones

## Ejemplo de uso

```bash
# 1. Ejecutar la aplicación
./run-gail-questionnaire.sh

# 2. Esperar la carga (automática)

# 3. Responder las 7 preguntas

# 4. Hacer clic en "Finalizar"

# 5. Las respuestas quedan guardadas en:
#    ontologias/custom/BreastCancer_GailResponses_[timestamp].owl
```

## Solución de problemas

### Error: "No se pudo cargar la ontología"

**Causa**: El archivo de ontología no existe en la ruta esperada

**Solución**: Verifica que existe el archivo:
```
ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx
```

### La aplicación no inicia

**Causa**: Java no está instalado o es una versión incorrecta

**Solución**: Verifica tu versión de Java:
```bash
java -version
# Debe ser Java 11 o superior
```

### No se guardan las respuestas

**Causa**: No hay permisos de escritura en el directorio `ontologias/custom/`

**Solución**: Verifica permisos o crea el directorio:
```bash
mkdir -p ontologias/custom
chmod 755 ontologias/custom
```

## Extensiones futuras

Posibles mejoras a implementar:

- [ ] Agregar más preguntas del modelo Gail completo
- [ ] Calcular el puntaje de riesgo basado en respuestas
- [ ] Mostrar recomendaciones según el riesgo calculado
- [ ] Soporte para múltiples pacientes
- [ ] Exportar respuestas a PDF
- [ ] Validación de respuestas obligatorias
- [ ] Integración con otros modelos (IBIS, ACS)

---

**Versión**: 1.0  
**Fecha**: Noviembre 2024  
**Modelo**: Gail (BCRAT)






