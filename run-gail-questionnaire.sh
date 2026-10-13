#!/bin/bash

echo "🏥 Cuestionario Médico - Modelo de Gail"
echo "=========================================="
echo ""
echo "🔧 Compilando proyecto..."
./gradlew build -x test -q

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa!"
    echo ""
    echo "🚀 Iniciando cuestionario..."
    echo "📋 Este cuestionario evalúa el riesgo de cáncer de mama usando el modelo Gail"
    echo ""
    ./gradlew runGailQuestionnaire
else
    echo "❌ Error en la compilación"
    echo "💡 Verifica que tengas Java 11+ instalado"
    exit 1
fi

