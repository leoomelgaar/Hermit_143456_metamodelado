#!/bin/bash

echo "🔧 Compilando proyecto HermiT Ontology Editor con Compose Desktop..."
./gradlew build -q

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa!"
    echo "🚀 Ejecutando la aplicación Compose Desktop..."
    echo "📱 Se abrirá una ventana con el editor de ontologías..."
    echo ""
    ./gradlew run
else
    echo "❌ Error en la compilación"
    echo "💡 Verifica que tengas Java 11+ instalado"
    exit 1
fi