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
- ✅ **Visualización en tiempo real** de elementos agregados
- ✅ **Mensajes de estado** informativos
- ✅ **UI moderna y reactiva** con Material 3

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

La aplicación se divide en tres secciones principales:

1. **Barra de herramientas superior:**
   - Botón "Nueva Ontología" - Crea una ontología vacía
   - Botón "Verificar Consistencia" - Ejecuta HermiT para verificar consistencia

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

4. **Barra de estado inferior:**
   - Mensajes informativos sobre operaciones
   - Estado de consistencia (✓ Consistente / ✗ Inconsistente)

### Ejemplo de Uso

1. **Crear una ontología sobre animales:**
   ```
   1. Clic en "Nueva Ontología"
   2. Agregar clase: "Animal"
   3. Agregar clase: "Mamifero"
   4. Agregar clase: "Perro"
   5. Crear relación: "Mamifero" subclase de "Animal"
   6. Crear relación: "Perro" subclase de "Mamifero"
   7. Agregar individuo: "Fido"
   8. Clic en "Verificar Consistencia"
   ```

2. **Resultado esperado:**
   - La ontología debería ser **consistente**
   - El panel derecho mostrará todos los elementos
   - Los mensajes de estado confirmarán cada operación

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