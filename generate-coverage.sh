#!/bin/bash

# Script para generar reportes de cobertura con JaCoCo
# Uso: ./generate-coverage.sh [module-name]

set -e

MODULE=${1:-""}

echo "🔍 Generando reportes de cobertura con JaCoCo..."

if [ -z "$MODULE" ]; then
    echo "📊 Generando cobertura para todos los módulos..."
    ./mvnw clean test jacoco:report
    echo ""
    echo "✅ Reportes generados en:"
    echo "   - feature-toggle-service/target/site/jacoco/index.html"
    echo "   - user-service/target/site/jacoco/index.html"
    echo "   - api-gateway/target/site/jacoco/index.html"
    echo "   - proxy-client/target/site/jacoco/index.html"
    echo ""
    echo "🌐 Para ver los reportes, abre los archivos index.html en tu navegador"
else
    echo "📊 Generando cobertura para módulo: $MODULE"
    ./mvnw clean test jacoco:report -pl "$MODULE"
    echo ""
    echo "✅ Reporte generado en:"
    echo "   - $MODULE/target/site/jacoco/index.html"
    echo ""
    echo "🌐 Para ver el reporte, abre el archivo index.html en tu navegador"
fi

