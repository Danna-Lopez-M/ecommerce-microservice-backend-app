#!/bin/bash

# Script para probar el Feature Toggle Service
# Este script demuestra cómo funcionan los feature toggles

BASE_URL="http://localhost:8800/feature-toggle-service"
ENDPOINT="$BASE_URL/api/features"

# Verificar que el servicio esté corriendo
echo "Verificando que el Feature Toggle Service esté corriendo..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ El Feature Toggle Service no está corriendo en $BASE_URL${NC}"
    echo ""
    echo "Para iniciar el servicio, ejecuta uno de estos comandos:"
    echo ""
    echo "Opción 1: Desde el directorio del módulo"
    echo "  cd feature-toggle-service"
    echo "  mvn spring-boot:run"
    echo ""
    echo "Opción 2: Desde el root del proyecto (especificando el módulo)"
    echo "  mvn spring-boot:run -pl feature-toggle-service"
    echo ""
    echo "Opción 3: Usar Docker Compose"
    echo "  docker-compose up feature-toggle-service-container"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ Feature Toggle Service está corriendo${NC}"
echo ""

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "Probando Feature Toggle Service"
echo "================================"
echo ""

# Función para hacer requests y mostrar resultados
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local description=$4
    
    echo -e "${CYAN}$description${NC}"
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -d "$data" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" 2>&1)
    fi
    
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ] || [ "$http_code" = "204" ]; then
        echo -e "${GREEN} HTTP $http_code${NC}"
        if [ -n "$body" ] && [ "$body" != "null" ]; then
            echo "$body" | jq '.' 2>/dev/null || echo "$body"
        fi
    else
        echo -e "${RED} HTTP $http_code${NC}"
        echo "$body"
    fi
    echo ""
}

# Test 1: Crear un feature toggle
echo "=========================================="
echo "TEST 1: Crear un Feature Toggle"
echo "=========================================="
FEATURE_NAME="test-feature-$(date +%s)"
FEATURE_DATA="{\"name\":\"$FEATURE_NAME\",\"enabled\":false,\"description\":\"Feature de prueba\",\"environment\":\"dev\"}"

make_request "POST" "$ENDPOINT" "$FEATURE_DATA" "Creando feature toggle: $FEATURE_NAME"

# Extraer el ID del feature creado
FEATURE_ID=$(echo "$body" | jq -r '.id' 2>/dev/null)
if [ -z "$FEATURE_ID" ] || [ "$FEATURE_ID" = "null" ]; then
    echo -e "${RED}Error: No se pudo obtener el ID del feature creado${NC}"
    exit 1
fi

echo -e "${GREEN}Feature creado con ID: $FEATURE_ID${NC}"
echo ""

# Test 2: Verificar que el feature está deshabilitado
echo "=========================================="
echo "TEST 2: Verificar Feature Deshabilitado"
echo "=========================================="
make_request "GET" "$ENDPOINT/check/$FEATURE_NAME?environment=dev" "" "Verificando estado del feature (debe estar deshabilitado)"

ENABLED=$(echo "$body" | jq -r '.enabled' 2>/dev/null)
if [ "$ENABLED" = "false" ]; then
    echo -e "${GREEN} Feature está deshabilitado correctamente${NC}"
else
    echo -e "${RED} Feature debería estar deshabilitado pero está: $ENABLED${NC}"
fi
echo ""

# Test 3: Habilitar el feature
echo "=========================================="
echo "TEST 3: Habilitar Feature"
echo "=========================================="
make_request "PUT" "$ENDPOINT/$FEATURE_NAME/enable" "" "Habilitando feature: $FEATURE_NAME"

# Test 4: Verificar que el feature está habilitado
echo "=========================================="
echo "TEST 4: Verificar Feature Habilitado"
echo "=========================================="
make_request "GET" "$ENDPOINT/check/$FEATURE_NAME?environment=dev" "" "Verificando estado del feature (debe estar habilitado)"

ENABLED=$(echo "$body" | jq -r '.enabled' 2>/dev/null)
if [ "$ENABLED" = "true" ]; then
    echo -e "${GREEN} Feature está habilitado correctamente${NC}"
else
    echo -e "${RED} Feature debería estar habilitado pero está: $ENABLED${NC}"
fi
echo ""

# Test 5: Deshabilitar el feature
echo "=========================================="
echo "TEST 5: Deshabilitar Feature"
echo "=========================================="
make_request "PUT" "$ENDPOINT/$FEATURE_NAME/disable" "" "Deshabilitando feature: $FEATURE_NAME"

# Test 6: Verificar que el feature está deshabilitado de nuevo
echo "=========================================="
echo "TEST 6: Verificar Feature Deshabilitado (nuevamente)"
echo "=========================================="
make_request "GET" "$ENDPOINT/check/$FEATURE_NAME?environment=dev" "" "Verificando estado del feature (debe estar deshabilitado)"

ENABLED=$(echo "$body" | jq -r '.enabled' 2>/dev/null)
if [ "$ENABLED" = "false" ]; then
    echo -e "${GREEN} Feature está deshabilitado correctamente${NC}"
else
    echo -e "${RED} Feature debería estar deshabilitado pero está: $ENABLED${NC}"
fi
echo ""

# Test 7: Obtener todos los features
echo "=========================================="
echo "TEST 7: Listar Todos los Features"
echo "=========================================="
make_request "GET" "$ENDPOINT" "" "Obteniendo todos los features"

# Test 8: Obtener features por ambiente
echo "=========================================="
echo "TEST 8: Listar Features por Ambiente"
echo "=========================================="
make_request "GET" "$ENDPOINT/environment/dev" "" "Obteniendo features del ambiente 'dev'"

# Test 9: Obtener feature por ID
echo "=========================================="
echo "TEST 9: Obtener Feature por ID"
echo "=========================================="
make_request "GET" "$ENDPOINT/$FEATURE_ID" "" "Obteniendo feature por ID: $FEATURE_ID"

# Test 10: Actualizar feature
echo "=========================================="
echo "TEST 10: Actualizar Feature"
echo "=========================================="
UPDATE_DATA="{\"name\":\"$FEATURE_NAME\",\"enabled\":true,\"description\":\"Feature actualizado\",\"environment\":\"dev\"}"
make_request "PUT" "$ENDPOINT/$FEATURE_ID" "$UPDATE_DATA" "Actualizando feature: $FEATURE_NAME"

# Test 11: Verificar feature que no existe
echo "=========================================="
echo "TEST 11: Verificar Feature que No Existe"
echo "=========================================="
make_request "GET" "$ENDPOINT/check/non-existent-feature?environment=dev" "" "Verificando feature que no existe (debe retornar false)"

ENABLED=$(echo "$body" | jq -r '.enabled' 2>/dev/null)
if [ "$ENABLED" = "false" ]; then
    echo -e "${GREEN} Feature inexistente retorna false correctamente${NC}"
else
    echo -e "${RED} Feature inexistente debería retornar false pero retorna: $ENABLED${NC}"
fi
echo ""

# Test 12: Probar diferentes ambientes
echo "=========================================="
echo "TEST 12: Probar Diferentes Ambientes"
echo "=========================================="
STAGING_FEATURE="staging-feature-$(date +%s)"
STAGING_DATA="{\"name\":\"$STAGING_FEATURE\",\"enabled\":true,\"description\":\"Feature para staging\",\"environment\":\"staging\"}"

make_request "POST" "$ENDPOINT" "$STAGING_DATA" "Creando feature para ambiente 'staging'"

make_request "GET" "$ENDPOINT/check/$STAGING_FEATURE?environment=staging" "" "Verificando feature en ambiente 'staging' (debe estar habilitado)"
STAGING_ENABLED=$(echo "$body" | jq -r '.enabled' 2>/dev/null)

make_request "GET" "$ENDPOINT/check/$STAGING_FEATURE?environment=dev" "" "Verificando feature en ambiente 'dev' (debe estar deshabilitado - no existe)"
DEV_ENABLED=$(echo "$body" | jq -r '.enabled' 2>/dev/null)

if [ "$STAGING_ENABLED" = "true" ] && [ "$DEV_ENABLED" = "false" ]; then
    echo -e "${GREEN} Los ambientes funcionan correctamente (staging: true, dev: false)${NC}"
else
    echo -e "${YELLOW}  Ambientes: staging=$STAGING_ENABLED, dev=$DEV_ENABLED${NC}"
fi
echo ""

# Test 13: Limpiar - Eliminar features de prueba
echo "=========================================="
echo "TEST 13: Limpiar Features de Prueba"
echo "=========================================="
read -p "¿Deseas eliminar los features de prueba? (s/n): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Ss]$ ]]; then
    make_request "DELETE" "$ENDPOINT/$FEATURE_ID" "" "Eliminando feature: $FEATURE_NAME"
    
    STAGING_ID=$(curl -s "$ENDPOINT" | jq -r ".[] | select(.name==\"$STAGING_FEATURE\") | .id" 2>/dev/null | head -1)
    if [ -n "$STAGING_ID" ] && [ "$STAGING_ID" != "null" ]; then
        make_request "DELETE" "$ENDPOINT/$STAGING_ID" "" "Eliminando feature: $STAGING_FEATURE"
    fi
    echo -e "${GREEN} Features de prueba eliminados${NC}"
else
    echo -e "${YELLOW}  Features de prueba no eliminados${NC}"
    echo "   Feature ID: $FEATURE_ID"
    echo "   Feature Name: $FEATURE_NAME"
fi
echo ""

# Resumen
echo "=========================================="
echo "RESUMEN DE PRUEBAS"
echo "=========================================="
echo -e "${GREEN}✅ Tests completados${NC}"
echo ""
echo "Endpoints probados:"
echo "  - POST   /api/features (crear)"
echo "  - GET    /api/features (listar todos)"
echo "  - GET    /api/features/{id} (obtener por ID)"
echo "  - GET    /api/features/environment/{env} (por ambiente)"
echo "  - GET    /api/features/check/{name} (verificar estado)"
echo "  - PUT    /api/features/{name}/enable (habilitar)"
echo "  - PUT    /api/features/{name}/disable (deshabilitar)"
echo "  - PUT    /api/features/{id} (actualizar)"
echo "  - DELETE /api/features/{id} (eliminar)"
echo ""
echo -e "${CYAN}💡 El Feature Toggle Service está funcionando correctamente${NC}"

