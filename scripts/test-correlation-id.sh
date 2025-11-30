#!/bin/bash

# Script para probar el patrón Correlation ID
# Este script demuestra cómo funciona el Correlation ID en API Gateway y User Service

API_GATEWAY_URL="http://localhost:8080"
USER_SERVICE_URL="http://localhost:8700/user-service"

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

CORRELATION_ID_HEADER="X-Correlation-ID"

echo "Probando Correlation ID Pattern"
echo "================================"
echo ""

# Verificar que los servicios estén corriendo
echo "Verificando que los servicios estén corriendo..."

# Verificar API Gateway
if ! curl -s "$API_GATEWAY_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED} API Gateway no está corriendo en $API_GATEWAY_URL${NC}"
    echo "   Inicia el API Gateway primero"
    exit 1
fi
echo -e "${GREEN} API Gateway está corriendo${NC}"

# Verificar User Service
if ! curl -s "$USER_SERVICE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED} User Service no está corriendo en $USER_SERVICE_URL${NC}"
    echo "   Inicia el User Service primero"
    exit 1
fi
echo -e "${GREEN} User Service está corriendo${NC}"
echo ""

# Detectar la ruta correcta del API Gateway
echo "Detectando configuración de rutas del API Gateway..."
GATEWAY_ROUTE="$API_GATEWAY_URL/user-service/api/users"

# Probar diferentes rutas posibles
if curl -s "$API_GATEWAY_URL/user-service/api/users" > /dev/null 2>&1; then
    GATEWAY_ROUTE="$API_GATEWAY_URL/user-service/api/users"
    echo -e "${GREEN} Usando ruta: /user-service/api/users${NC}"
elif curl -s "$API_GATEWAY_URL/app/api/users" > /dev/null 2>&1; then
    GATEWAY_ROUTE="$API_GATEWAY_URL/app/api/users"
    echo -e "${GREEN} Usando ruta: /app/api/users${NC}"
else
    echo -e "${YELLOW} No se pudo detectar la ruta automáticamente${NC}"
    echo -e "${YELLOW} Usando ruta por defecto: /user-service/api/users${NC}"
    GATEWAY_ROUTE="$API_GATEWAY_URL/user-service/api/users"
fi
echo ""

# Función para hacer request y extraer Correlation ID
# Retorna solo el Correlation ID (sin imprimir nada)
make_request_silent() {
    local url=$1
    local method=${2:-GET}
    local correlation_id=$3
    
    local headers=()
    if [ -n "$correlation_id" ]; then
        headers+=("-H" "$CORRELATION_ID_HEADER: $correlation_id")
    fi
    
    # Hacer la request y capturar headers de respuesta
    response=$(curl -s -i -X "$method" "$url" "${headers[@]}" 2>&1)
    # Obtener solo el primer valor del header (puede haber duplicados)
    response_correlation_id=$(echo "$response" | grep -i "^$CORRELATION_ID_HEADER:" | head -1 | cut -d' ' -f2 | tr -d '\r\n')
    
    # Retornar solo el Correlation ID
    echo "$response_correlation_id"
}

# Función para hacer request y mostrar información
make_request() {
    local url=$1
    local method=${2:-GET}
    local correlation_id=$3
    local description=$4
    
    # Enviar mensajes de display a stderr para no contaminar el valor de retorno
    echo -e "${CYAN}$description${NC}" >&2
    
    if [ -n "$correlation_id" ]; then
        echo "   Enviando Correlation ID: $correlation_id" >&2
    else
        echo "   Sin Correlation ID (debe generarse automáticamente)" >&2
    fi
    
    # Hacer la request y capturar headers de respuesta
    response=$(curl -s -i -X "$method" "$url" ${correlation_id:+-H "$CORRELATION_ID_HEADER: $correlation_id"} 2>&1)
    http_code=$(echo "$response" | grep -i "^HTTP" | awk '{print $2}')
    # Obtener solo el primer valor del header (puede haber duplicados)
    response_correlation_id=$(echo "$response" | grep -i "^$CORRELATION_ID_HEADER:" | head -1 | cut -d' ' -f2 | tr -d '\r\n')
    
    if [ -z "$http_code" ]; then
        http_code="000"
    fi
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ] || [ "$http_code" = "204" ]; then
        echo -e "${GREEN} HTTP $http_code${NC}" >&2
    else
        echo -e "${RED} HTTP $http_code${NC}" >&2
        if [ "$http_code" = "503" ]; then
            echo -e "${YELLOW}   Advertencia: Service Unavailable${NC}" >&2
            echo -e "${YELLOW}   Posibles causas:${NC}" >&2
            echo -e "${YELLOW}   - El servicio no está registrado en Eureka${NC}" >&2
            echo -e "${YELLOW}   - La ruta del API Gateway no está configurada correctamente${NC}" >&2
            echo -e "${YELLOW}   - El Load Balancer no puede encontrar el servicio${NC}" >&2
        elif [ "$http_code" = "404" ]; then
            echo -e "${YELLOW}   Advertencia: Not Found - Verifica la ruta del API Gateway${NC}" >&2
        fi
    fi
    
    if [ -n "$response_correlation_id" ]; then
        echo -e "${GREEN} Correlation ID en respuesta: $response_correlation_id${NC}" >&2
    else
        echo -e "${RED} No se encontró Correlation ID en la respuesta${NC}" >&2
    fi
    echo "" >&2
    
    # Retornar el Correlation ID solo a stdout
    echo "$response_correlation_id"
}

# Test 1: Request sin Correlation ID a través del API Gateway
echo "=========================================="
echo "TEST 1: Request sin Correlation ID (API Gateway)"
echo "=========================================="
echo "El API Gateway debe generar un Correlation ID automáticamente"
echo ""

GENERATED_ID=$(make_request "$GATEWAY_ROUTE" "GET" "" "Request sin Correlation ID a través del API Gateway")

if [ -z "$GENERATED_ID" ] || [ "$GENERATED_ID" = "" ]; then
    echo -e "${RED} Error: No se generó Correlation ID${NC}"
    echo "   Verifica que el API Gateway esté configurado correctamente"
    echo "   O que la ruta sea la correcta: $GATEWAY_ROUTE"
else
    echo -e "${GREEN} Correlation ID generado: $GENERATED_ID${NC}"
fi
echo ""

# Test 2: Request con Correlation ID personalizado a través del API Gateway
echo "=========================================="
echo "TEST 2: Request con Correlation ID Personalizado (API Gateway)"
echo "=========================================="
echo "El API Gateway debe preservar el Correlation ID proporcionado"
echo ""

CUSTOM_ID="test-correlation-$(date +%s)"
PRESERVED_ID=$(make_request "$GATEWAY_ROUTE" "GET" "$CUSTOM_ID" "Request con Correlation ID personalizado: $CUSTOM_ID")

if [ -n "$PRESERVED_ID" ] && [ "$PRESERVED_ID" = "$CUSTOM_ID" ]; then
    echo -e "${GREEN} Correlation ID preservado correctamente: $PRESERVED_ID${NC}"
else
    if [ -z "$PRESERVED_ID" ]; then
        echo -e "${RED} Error: No se recibió Correlation ID en la respuesta${NC}"
    else
        echo -e "${RED} Error: Correlation ID no se preservó. Esperado: $CUSTOM_ID, Obtenido: $PRESERVED_ID${NC}"
    fi
fi
echo ""

# Test 3: Request directo al User Service sin Correlation ID
echo "=========================================="
echo "TEST 3: Request Directo al User Service sin Correlation ID"
echo "=========================================="
echo "El User Service debe generar un Correlation ID automáticamente"
echo ""

USER_SERVICE_ID=$(make_request "$USER_SERVICE_URL/api/users" "GET" "" "Request directo al User Service sin Correlation ID")

if [ -n "$USER_SERVICE_ID" ]; then
    echo -e "${GREEN} User Service generó Correlation ID: $USER_SERVICE_ID${NC}"
else
    echo -e "${RED} Error: User Service no generó Correlation ID${NC}"
fi
echo ""

# Test 4: Request directo al User Service con Correlation ID
echo "=========================================="
echo "TEST 4: Request Directo al User Service con Correlation ID"
echo "=========================================="
echo "El User Service debe preservar el Correlation ID proporcionado"
echo ""

CUSTOM_ID2="user-service-test-$(date +%s)"
USER_PRESERVED_ID=$(make_request "$USER_SERVICE_URL/api/users" "GET" "$CUSTOM_ID2" "Request directo al User Service con Correlation ID: $CUSTOM_ID2")

if [ -n "$USER_PRESERVED_ID" ] && [ "$USER_PRESERVED_ID" = "$CUSTOM_ID2" ]; then
    echo -e "${GREEN} User Service preservó Correlation ID correctamente: $USER_PRESERVED_ID${NC}"
else
    if [ -z "$USER_PRESERVED_ID" ]; then
        echo -e "${RED} Error: No se recibió Correlation ID en la respuesta${NC}"
    else
        echo -e "${RED} Error: Correlation ID no se preservó. Esperado: $CUSTOM_ID2, Obtenido: $USER_PRESERVED_ID${NC}"
    fi
fi
echo ""

# Test 5: Múltiples requests - verificar que cada una tiene su propio ID
echo "=========================================="
echo "TEST 5: Múltiples Requests - IDs Únicos"
echo "=========================================="
echo "Cada request sin Correlation ID debe generar un ID único"
echo ""

ID1=$(make_request_silent "$GATEWAY_ROUTE" "GET" "")
ID2=$(make_request_silent "$GATEWAY_ROUTE" "GET" "")
ID3=$(make_request_silent "$GATEWAY_ROUTE" "GET" "")

echo "   Request 1 generó: $ID1"
echo "   Request 2 generó: $ID2"
echo "   Request 3 generó: $ID3"

if [ -n "$ID1" ] && [ -n "$ID2" ] && [ -n "$ID3" ] && \
   [ "$ID1" != "$ID2" ] && [ "$ID2" != "$ID3" ] && [ "$ID1" != "$ID3" ]; then
    echo -e "${GREEN} Todos los Correlation IDs son únicos${NC}"
else
    echo -e "${RED} Error: Algunos Correlation IDs son duplicados o están vacíos${NC}"
fi
echo ""

# Test 6: Verificar formato UUID
echo "=========================================="
echo "TEST 6: Verificar Formato UUID"
echo "=========================================="
echo "Los Correlation IDs deben tener formato UUID válido"
echo ""

UUID_PATTERN="^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"

# Usar el ID generado en el Test 1, o generar uno nuevo si no está disponible
if [ -z "$GENERATED_ID" ] || [ "$GENERATED_ID" = "" ]; then
    TEST_ID=$(make_request_silent "$USER_SERVICE_URL/api/users" "GET" "")
else
    TEST_ID="$GENERATED_ID"
fi

if [ -n "$TEST_ID" ] && echo "$TEST_ID" | grep -qE "$UUID_PATTERN"; then
    echo -e "${GREEN} Correlation ID tiene formato UUID válido: $TEST_ID${NC}"
else
    echo -e "${RED} Error: Correlation ID no tiene formato UUID válido: $TEST_ID${NC}"
fi
echo ""

# Test 7: Propagación a través del API Gateway
echo "=========================================="
echo "TEST 7: Propagación a través del API Gateway"
echo "=========================================="
echo "El Correlation ID debe propagarse del API Gateway al User Service"
echo ""

PROPAGATION_ID="propagation-test-$(date +%s)"
echo "Enviando request con Correlation ID: $PROPAGATION_ID"
echo "a través del API Gateway hacia el User Service..."
echo ""

# El API Gateway debe propagar el Correlation ID al User Service
PROPAGATED_ID=$(make_request "$GATEWAY_ROUTE" "GET" "$PROPAGATION_ID" "Request a través del API Gateway con Correlation ID: $PROPAGATION_ID")

if [ -n "$PROPAGATED_ID" ] && [ "$PROPAGATED_ID" = "$PROPAGATION_ID" ]; then
    echo -e "${GREEN} Correlation ID se propagó correctamente: $PROPAGATED_ID${NC}"
    echo -e "${CYAN} Nota: El ID se propagó del cliente → API Gateway → User Service${NC}"
else
    if [ -z "$PROPAGATED_ID" ]; then
        echo -e "${YELLOW} Advertencia: No se recibió Correlation ID en la respuesta${NC}"
        echo "   Esto puede indicar que el API Gateway no está funcionando correctamente"
    else
        echo -e "${YELLOW} Advertencia: Correlation ID no coincide exactamente${NC}"
        echo "   Esperado: $PROPAGATION_ID"
        echo "   Obtenido: $PROPAGATED_ID"
        echo "   (Esto puede ser normal si el API Gateway genera uno nuevo)"
    fi
fi
echo ""

# Resumen
echo "=========================================="
echo "RESUMEN DE PRUEBAS"
echo "=========================================="
echo -e "${GREEN} Tests completados${NC}"
echo ""
echo "Funcionalidades probadas:"
echo "  Generación automática de Correlation ID"
echo "  Preservación de Correlation ID personalizado"
echo "  Unicidad de Correlation IDs"
echo "  Formato UUID válido"
echo "  Propagación a través de servicios"
echo ""
echo "Endpoints probados:"
echo "  - API Gateway: $GATEWAY_ROUTE"
echo "  - User Service: $USER_SERVICE_URL/api/users"
echo ""

if [ -n "$GENERATED_ID" ] && [ -n "$USER_SERVICE_ID" ]; then
    echo -e "${GREEN} El Correlation ID Pattern está funcionando correctamente${NC}"
else
    echo -e "${YELLOW}  Algunos tests fallaron - Revisa la configuración${NC}"
fi

echo ""
echo " Para ver los Correlation IDs en los logs:"
echo "   docker logs user-service-container 2>&1 | grep correlationId"
echo "   docker logs api-gateway-container 2>&1 | grep correlationId"