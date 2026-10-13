# HermiT Ontology Editor - Interfaz de Usuario con Compose Desktop

## Descripción

Esta es una interfaz gráfica desarrollada en **Kotlin + Compose Desktop** para crear y editar ontologías usando **OWLAPI**. Permite crear ontologías de forma visual con una interfaz moderna y reactiva.

## Características

### Funcionalidades Principales
- ✅ **Crear nueva ontología** vacía
- ✅ **Agregar clases** a la ontología
- ✅ **Agregar propiedades de objeto** (Object Properties)
- ✅ **Agregar propiedades de datos** (Data Properties)
- ✅ **Agregar individuos** (Named Individuals)
- ✅ **Crear relaciones de subclase** (SubClassOf)
- ✅ **Listar ontologías disponibles** del directorio `ontologias/`
- ✅ **Verificar ontología individual** seleccionada
- ✅ **Verificar todas las ontologías** en lote
- ✅ **Resultados de verificación** con estadísticas detalladas
- ✅ **Visualización en tiempo real** de elementos agregados
- ✅ **Mensajes de estado** informativos
- ✅ **UI moderna y reactiva** con Material 3 y tabs

### Tecnologías Utilizadas
- **Kotlin 1.9.20** - Lenguaje principal
- **Compose Desktop 1.5.11** - Framework de UI moderna
- **OWLAPI 4.5.26** - Manipulación de ontologías
- **Gradle 8.5** - Gestión de dependencias y build
- **Material 3** - Sistema de diseño

## Instalación y Ejecución

### Requisitos
- **Java 8+** (recomendado Java 11 o superior)
- **Maven 3.6+**
- **JavaFX** (incluido en las dependencias)

### Compilar y Ejecutar

1. **Compilar el proyecto:**
   ```bash
   mvn clean compile
   ```

2. **Ejecutar la aplicación:**
   ```bash
   ./run-ui.sh
   ```
   
   O manualmente:
   ```bash
   mvn exec:java -Dexec.mainClass="org.semanticweb.hermit.ui.OntologyEditorAppKt"
   ```

## Uso de la Aplicación

### Interfaz Principal

La aplicación tiene **dos tabs principales**:

#### **Tab 1: Editor de Ontologías**
1. **Barra de herramientas superior:**
   - Botón "Nueva Ontología" - Crea una ontología vacía

2. **Panel izquierdo - Formularios de entrada:**
   - **Nueva Clase:** Agregar clases OWL
   - **Nueva Propiedad de Objeto:** Agregar propiedades que relacionan individuos
   - **Nueva Propiedad de Datos:** Agregar propiedades de datos primitivos
   - **Nuevo Individuo:** Agregar instancias nombradas
   - **Nueva Relación SubClase:** Establecer jerarquías de clases

3. **Panel derecho - Vista de elementos:**
   - Muestra información de la ontología actual
   - Lista todos los elementos agregados
   - Actualización en tiempo real

#### **Tab 2: Verificador de Ontologías**
1. **Controles superiores:**
   - **Recargar** - Actualiza la lista de ontologías
   - **Verificar Seleccionada** - Verifica la ontología elegida
   - **Verificar Todas** - Procesa todas las ontologías disponibles
   - **Limpiar** - Borra los resultados

2. **Panel izquierdo - Ontologías disponibles:**
   - Lista todas las ontologías del directorio `ontologias/`
   - Agrupadas por escenario (EscenarioC, EscenarioD, etc.)
   - Selección visual de la ontología a verificar

3. **Panel derecho - Resultados:**
   - Muestra resultados de verificación con colores
   - **Verde:** ✓ Consistente
   - **Rojo:** ✗ Inconsistente o Error
   - Estadísticas: número de clases y axiomas
   - Mensajes de error detallados

4. **Barra de progreso:**
   - Indicador visual durante verificaciones en lote

### Ejemplo de Uso

#### **Editor de Ontologías:**
1. **Crear una ontología sobre animales:**
   ```
   1. Tab "Editor de Ontologías"
   2. Clic en "Nueva Ontología"
   3. Agregar clase: "Animal"
   4. Agregar clase: "Mamifero"
   5. Agregar clase: "Perro"
   6. Crear relación: "Mamifero" subclase de "Animal"
   7. Crear relación: "Perro" subclase de "Mamifero"
   8. Agregar individuo: "Fido"
   ```

#### **Verificador de Ontologías:**
1. **Verificar ontologías existentes:**
   ```
   1. Tab "Verificador de Ontologías"
   2. La app carga automáticamente todas las ontologías del directorio
   3. Seleccionar una ontología (ej: "TestCycles4" del EscenarioC)
   4. Clic en "Verificar Seleccionada"
   5. Ver resultado: ✓ Consistente o ✗ Inconsistente
   ```

2. **Verificación en lote:**
   ```
   1. Clic en "Verificar Todas"
   2. La barra de progreso muestra el avance
   3. Resultados aparecen con códigos de color
   4. Resumen: "X consistentes, Y inconsistentes, Z errores"
   ```

### Verificación de Consistencia

El botón "Verificar Consistencia" ejecuta el razonador HermiT para:
- ✅ **Detectar contradicciones** en la ontología
- ✅ **Validar la estructura lógica**
- ✅ **Mostrar resultado visual** (verde = consistente, rojo = inconsistente)

## Arquitectura del Código

### Componentes Principales

```
src/main/kotlin/org/semanticweb/hermit/ui/
├── OntologyRepository.kt          # Wrapper de OWLAPI/HermiT
├── SimpleOntologyViewModel.kt     # Lógica de estado de la UI
├── OntologyEditorApp.kt          # Aplicación JavaFX principal
└── OntologyViewModel.kt          # ViewModel con StateFlow (no usado)
```

### Flujo de Datos

1. **UI (JavaFX)** → Eventos de usuario
2. **ViewModel** → Procesa lógica de negocio
3. **Repository** → Interactúa con OWLAPI/HermiT
4. **Repository** → Actualiza ontología
5. **ViewModel** → Notifica cambios a UI
6. **UI** → Se actualiza automáticamente

## Limitaciones Actuales

- ❌ **Guardar/Cargar archivos** - No implementado aún
- ❌ **Edición de axiomas complejos** - Solo operaciones básicas
- ❌ **Visualización gráfica** - Solo lista textual
- ❌ **Deshacer/Rehacer** - No implementado
- ❌ **Validación avanzada** - Solo consistencia básica

## Próximas Mejoras

1. **Persistencia:** Guardar/cargar ontologías en OWL/XML, Turtle, etc.
2. **Editor avanzado:** Axiomas complejos, restricciones, anotaciones
3. **Visualización:** Grafo de la ontología, árbol de clases
4. **Validación:** Más checks de calidad y sugerencias
5. **Usabilidad:** Autocompletar, validación en tiempo real

## Problemas Conocidos

- La actualización de la UI usa polling simple (cada 500ms-1s)
- No hay validación de nombres duplicados
- Los hilos de actualización pueden consumir recursos

## Contribuir

Para agregar nuevas funcionalidades:

1. **Repository:** Agregar métodos en `OntologyRepository.kt`
2. **ViewModel:** Exponer funcionalidad en `SimpleOntologyViewModel.kt`  
3. **UI:** Crear componentes en `OntologyEditorApp.kt`
4. **Testing:** Probar con ontologías de ejemplo

---

**¡La aplicación está lista para usar!** 🚀

Ejecuta `./run-ui.sh` y comienza a crear ontologías de forma visual.