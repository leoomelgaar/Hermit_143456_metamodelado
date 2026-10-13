# Cuestionario Médico Interactivo - HermiT Ontology Editor

## Descripción

Esta nueva funcionalidad permite simular un cuestionario médico interactivo basado en modelos de riesgo de cáncer de mama (como el modelo de Gail) usando la ontología `BreastCancerRecommendationWithMetamodelling.owx`.

## Características

- **Interfaz independiente**: Nueva pestaña "Cuestionario Médico" en la aplicación
- **Selección de modelos**: Permite elegir entre diferentes modelos de riesgo (Gail, IBIS, ACS, UY, etc.)
- **Preguntas interactivas**: Muestra las preguntas asociadas a cada modelo con sus factores de riesgo
- **Respuestas dinámicas**: Permite seleccionar respuestas de múltiples opciones
- **Modificación de ontología**: Las respuestas se guardan automáticamente en la ontología
- **Navegación**: Botones para avanzar y retroceder entre preguntas
- **Progreso visual**: Indicador de progreso del cuestionario

## Cómo usar

### 1. Cargar la ontología

Primero, carga la ontología `BreastCancerRecommendationWithMetamodelling.owx`:

1. Abre la aplicación con `./run-ui.sh`
2. Ve a la pestaña "Editor de Ontologías"
3. Selecciona la ontología `BreastCancerRecommendationWithMetamodelling` de la lista del escenario E
4. La ontología se cargará automáticamente

### 2. Acceder al cuestionario

1. Haz clic en la pestaña **"Cuestionario Médico"** en la parte superior
2. Los modelos disponibles se cargarán automáticamente desde la ontología

### 3. Seleccionar un modelo

En el panel izquierdo verás los modelos disponibles:
- **Gail model (BCRAT-Calculator)**: Modelo de riesgo de Gail
- **IBIS model**: Modelo IBIS/Tyrer-Cuzick
- **ACS model**: Modelo de la American Cancer Society
- **UY model**: Modelo uruguayo

Haz clic en el modelo que desees usar (por ejemplo, **Gail model**).

### 4. Responder el cuestionario

Para cada pregunta:

1. **Lee la pregunta** mostrada en la parte superior
2. **Observa el factor de riesgo** asociado (si está disponible)
3. **Selecciona una respuesta** de las opciones disponibles
4. La respuesta se guarda automáticamente en la ontología
5. Usa los botones **"Anterior"** y **"Siguiente"** para navegar

### 5. Verificar el progreso

En la parte inferior verás:
- Número de preguntas respondidas
- Total de preguntas del modelo
- Barra de progreso visual

### 6. Guardar cambios (opcional)

Si deseas guardar la ontología modificada:

1. Vuelve a la pestaña "Editor de Ontologías"
2. Haz clic en **"Guardar Como..."**
3. Ingresa un nombre para el archivo
4. La ontología con las respuestas se guardará en `ontologias/custom/`

## Estructura técnica

### Componentes creados

1. **MedicalQuestionnaireModels.kt**: Modelos de datos
   - `MedicalModel`: Representa un modelo de riesgo
   - `MedicalQuestion`: Representa una pregunta con sus respuestas
   - `MedicalAnswer`: Representa una opción de respuesta
   - `QuestionnaireState`: Estado del cuestionario

2. **SimpleOntologyRepository.kt** (extendido):
   - `getMedicalModels()`: Extrae modelos de la ontología
   - `getQuestionsForModel()`: Obtiene preguntas de un modelo específico
   - `addPatientAnswer()`: Guarda respuestas en la ontología

3. **OntologyViewModel.kt** (extendido):
   - `loadMedicalModels()`: Carga modelos disponibles
   - `selectMedicalModel()`: Selecciona un modelo y carga sus preguntas
   - `answerQuestion()`: Guarda una respuesta
   - `nextQuestion()` / `previousQuestion()`: Navegación
   - `resetQuestionnaire()`: Reinicia el cuestionario

4. **MedicalQuestionnaireView.kt**: Interfaz de usuario
   - Vista de selección de modelos
   - Vista de preguntas con navegación
   - Tarjetas de respuestas interactivas
   - Indicadores de progreso

5. **Main.kt** (modificado):
   - Agrega pestaña "Cuestionario Médico"
   - Integra la nueva funcionalidad

### Cómo funciona internamente

1. **Extracción de modelos**: 
   - Busca individuos de la clase `Model` en la ontología
   - Extrae sus anotaciones (labels) para nombres legibles

2. **Extracción de preguntas**:
   - Usa la propiedad `hasModelQuestion` para encontrar preguntas asociadas a un modelo
   - Para cada pregunta, extrae:
     - El factor de riesgo asociado (`aboutRiskFactor`)
     - Las respuestas posibles (`hasAnswer`)
     - Anotaciones para textos legibles

3. **Guardado de respuestas**:
   - Crea un individuo `CurrentPatient` en la ontología
   - Usa la propiedad `hasAnswerValue` para vincular respuestas
   - Reemplaza respuestas anteriores si existen

## Ejemplo de uso: Modelo Gail

El modelo Gail típicamente incluye preguntas sobre:
- Edad actual
- Edad de primera menstruación
- Edad del primer hijo
- Número de biopsias previas
- Historia familiar de cáncer de mama
- Raza/etnicidad

Cada pregunta tiene múltiples opciones de respuesta que permiten calcular el riesgo de la paciente.

## Limitaciones actuales

- Las preguntas se muestran en el orden que aparecen en la ontología
- No se valida que todas las preguntas estén respondidas
- El cálculo de riesgo no se realiza automáticamente (requeriría razonamiento adicional)
- Las respuestas se guardan pero no se visualizan en sesiones posteriores

## Próximas mejoras sugeridas

1. **Validación de respuestas**: Asegurar que todas las preguntas obligatorias estén respondidas
2. **Persistencia de sesión**: Cargar respuestas anteriores del mismo paciente
3. **Cálculo de riesgo**: Integrar razonamiento para determinar nivel de riesgo basado en respuestas
4. **Visualización de resultados**: Mostrar el riesgo calculado y recomendaciones
5. **Múltiples pacientes**: Gestionar perfiles de diferentes pacientes
6. **Exportar respuestas**: Generar reportes con las respuestas del paciente

## Soporte

Para problemas o preguntas sobre esta funcionalidad, consulta el código fuente en:
- `src/main/kotlin/org/semanticweb/hermit/ui/MedicalQuestionnaireModels.kt`
- `src/main/kotlin/org/semanticweb/hermit/ui/compose/MedicalQuestionnaireView.kt`
- `src/main/kotlin/org/semanticweb/hermit/ui/SimpleOntologyRepository.kt` (métodos `getMedicalModels`, `getQuestionsForModel`, `addPatientAnswer`)






