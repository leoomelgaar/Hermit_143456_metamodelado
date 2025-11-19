#!/bin/bash

echo "🔍 Test de carga de ontología"
echo "=============================="
echo ""

cd /Users/joaquinvidal/Documents/Facultad/tesis/Hermit_143456_metamodelado

echo "1. Verificando archivo..."
FILE="ontologias/EscenarioE/BreastCancerRecommendationWithMetamodelling.owx"
if [ -f "$FILE" ]; then
    echo "   ✓ Archivo existe: $FILE"
    echo "   Tamaño: $(du -h "$FILE" | cut -f1)"
    echo "   Permisos: $(ls -l "$FILE" | awk '{print $1}')"
else
    echo "   ✗ Archivo NO existe: $FILE"
    exit 1
fi

echo ""
echo "2. Verificando primeras líneas del archivo..."
head -n 10 "$FILE"

echo ""
echo "3. Compilando proyecto..."
./gradlew build -x test -q

echo ""
echo "4. Ejecutando cuestionario con debug..."
./gradlew runGailQuestionnaire





