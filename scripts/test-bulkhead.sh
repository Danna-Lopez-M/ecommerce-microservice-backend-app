#!/bin/bash

# Script para probar el patrón Bulkhead
# Este script demuestra cómo el Bulkhead limita las llamadas concurrentes
# y muestra requests aceptadas, esperando y rechazadas

BASE_URL="http://localhost:8700/user-service"
ENDPOINT="$BASE_URL/api/users"

# Configuración
TOTAL_REQUESTS=100  # Muchas más requests para forzar rechazos
BATCH_SIZE=10      # Requests por segundo para generar carga progresiva

# Variable para almacenar métricas de rechazo del actuator
REJECTED_METRICS="0"

echo "Probando patrón Bulkhead"
echo "================================"
echo ""
echo "Configuración:"
echo "  - Límite de concurrencia: 5 llamadas simultáneas"
echo "  - Tiempo máximo de espera: 50ms"
echo "  - Total de requests: $TOTAL_REQUESTS"
echo "  - Nota: El fallback retorna lista vacía [] con HTTP 200"
echo ""

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Contadores
ACCEPTED=0
WAITING=0
REJECTED=0

echo "Métricas ANTES de generar carga:"
echo "-----------------------------------"
AVAILABLE_BEFORE=$(curl -s "$BASE_URL/actuator/metrics/resilience4j.bulkhead.available.concurrent.calls?tag=name:userServiceNonCritical" 2>/dev/null | jq -r '.measurements[0].value // "N/A"')
echo "Llamadas disponibles: $AVAILABLE_BEFORE"

PERMITTED_BEFORE=$(curl -s "$BASE_URL/actuator/metrics/resilience4j.bulkhead.calls.permitted?tag=name:userServiceNonCritical" 2>/dev/null | jq -r '.measurements[0].value // "0"')
echo "Llamadas permitidas: $PERMITTED_BEFORE"

REJECTED_BEFORE=$(curl -s "$BASE_URL/actuator/metrics/resilience4j.bulkhead.calls.rejected?tag=name:userServiceNonCritical" 2>/dev/null | jq -r '.measurements[0].value // "0"')
echo "Llamadas rechazadas: $REJECTED_BEFORE"
echo ""

echo -e "${YELLOW}Generando $TOTAL_REQUESTS requests en ráfagas de $BATCH_SIZE...${NC}"
echo ""

# Archivos temporales para resultados
RESULTS_FILE=$(mktemp)
TIMING_FILE=$(mktemp)

# Función para hacer una request y clasificarla
make_request() {
    local id=$1
    local start=$(date +%s%N)
    local status="UNKNOWN"
    local http_code="000"
    local duration=0
    
    # Hacer la request con timeout corto para detectar rechazos
    local response=$(curl -s -w "\n%{http_code}" --max-time 2 -o /tmp/response_$id.json "$ENDPOINT" 2>&1)
    local end=$(date +%s%N)
    duration=$(( (end - start) / 1000000 )) # en milisegundos
    http_code=$(echo "$response" | tail -1)
    
    # Verificar si la respuesta es una lista vacía (posible fallback del Bulkhead)
    # El fallback retorna [] muy rápido cuando el Bulkhead rechaza
    local response_body=""
    local is_fallback=false
    if [ -f "/tmp/response_$id.json" ]; then
        response_body=$(cat /tmp/response_$id.json 2>/dev/null | tr -d '\n\r\t ' | head -c 10)
        # Verificar si es una lista vacía JSON: [] (el fallback retorna esto)
        # El fallback se ejecuta muy rápido (< 20ms) cuando el Bulkhead rechaza
        if [ "$response_body" = "[]" ] || [ "$response_body" = "{\"content\":[]}" ]; then
            # Si es lista vacía Y muy rápida, probablemente es fallback
            if [ $duration -lt 20 ]; then
                is_fallback=true
            fi
        fi
    fi
    
    # Clasificar la request basándose en el código HTTP, tiempo y contenido
    if [ "$http_code" = "200" ]; then
        # Verificar si es fallback (lista vacía muy rápida = rechazo del Bulkhead)
        if [ "$is_fallback" = true ]; then
            # Lista vacía muy rápida - probablemente fallback del Bulkhead (rechazo)
            status="REJECTED"  # Rechazada y ejecutó fallback
            echo "$id:REJECTED:$duration:$http_code:FALLBACK" >> "$RESULTS_FILE"
        elif [ $duration -lt 150 ]; then
            status="ACCEPTED"  # Procesada inmediatamente (< 150ms)
            echo "$id:ACCEPTED:$duration:$http_code" >> "$RESULTS_FILE"
        elif [ $duration -lt 500 ]; then
            status="WAITING"  # Esperó un poco (150-500ms)
            echo "$id:WAITING:$duration:$http_code" >> "$RESULTS_FILE"
        else
            status="WAITING_LONG"  # Esperó mucho (> 500ms)
            echo "$id:WAITING_LONG:$duration:$http_code" >> "$RESULTS_FILE"
        fi
    elif [ "$http_code" = "503" ] || [ "$http_code" = "500" ]; then
        status="REJECTED"  # Rechazada por el Bulkhead (fallback con error)
        echo "$id:REJECTED:$duration:$http_code" >> "$RESULTS_FILE"
    elif [ "$http_code" = "000" ] || [ -z "$http_code" ]; then
        status="TIMEOUT"  # Timeout o conexión fallida (posible rechazo)
        echo "$id:TIMEOUT:$duration:$http_code" >> "$RESULTS_FILE"
    else
        status="ERROR"  # Otro error
        echo "$id:ERROR:$duration:$http_code" >> "$RESULTS_FILE"
    fi
    
    # Mostrar resultado en tiempo real con colores
    case $status in
        ACCEPTED)
            echo -e "${GREEN}[ACCEPTED] Request $id: OK (${duration}ms) - Procesada inmediatamente${NC}"
            ;;
        WAITING)
            echo -e "${YELLOW}[WAITING] Request $id: OK (${duration}ms) - Esperó en cola${NC}"
            ;;
        WAITING_LONG)
            echo -e "${BLUE}[WAITING] Request $id: OK (${duration}ms) - Esperó mucho tiempo${NC}"
            ;;
        REJECTED)
            if echo "$response" | grep -q "FALLBACK"; then
                echo -e "${RED}[REJECTED] Request $id: REJECTED (HTTP $http_code, ${duration}ms) - Bulkhead rechazó → Fallback ejecutado${NC}"
            else
                echo -e "${RED}[REJECTED] Request $id: REJECTED (HTTP $http_code, ${duration}ms) - Bulkhead rechazó${NC}"
            fi
            ;;
        TIMEOUT)
            echo -e "${RED}[TIMEOUT] Request $id: TIMEOUT (${duration}ms) - Posible rechazo${NC}"
            ;;
        *)
            echo -e "${RED}[ERROR] Request $id: FAILED (HTTP $http_code, ${duration}ms)${NC}"
            ;;
    esac
}

# Generar requests en fases para que algunas se procesen inmediatamente
echo "Fase 1: Generando primeras 5 requests UNA POR UNA (deberían procesarse inmediatamente)..."
for i in $(seq 1 5); do
    echo "  Enviando request $i..."
    make_request $i &
    # Esperar un poco para que cada request se procese antes de enviar la siguiente
    sleep 0.2
done

# Esperar a que las primeras 5 se completen completamente
echo ""
echo "Esperando a que las primeras 5 requests se completen..."
sleep 1.5

echo ""
echo "Fase 2: Generando ráfaga de $BATCH_SIZE requests (algunas esperarán)..."
for i in $(seq 6 $((5 + BATCH_SIZE))); do
    make_request $i &
done

# Esperar un momento para que algunas se procesen
sleep 0.3

echo ""
echo "Fase 3: Generando ráfaga de $BATCH_SIZE requests adicionales (más esperarán)..."
for i in $(seq $((6 + BATCH_SIZE)) $((5 + BATCH_SIZE * 2))); do
    make_request $i &
done

sleep 0.2

# Generar MUCHAS requests simultáneamente para forzar rechazos
echo ""
echo "Fase 4: Generando $((TOTAL_REQUESTS - 5 - BATCH_SIZE * 2)) requests TODAS AL MISMO TIEMPO (saturación extrema para forzar rechazos)..."
for i in $(seq $((6 + BATCH_SIZE * 2)) $TOTAL_REQUESTS); do
    make_request $i &
    # Sin delay - todas al mismo tiempo para saturar el Bulkhead
done

# Esperar a que todas terminen
wait

echo ""
echo "Esperando 3 segundos para que se actualicen las métricas..."
sleep 3

# Verificar métricas del actuator para rechazos reales
echo ""
echo "Verificando métricas del Bulkhead para rechazos reales..."
REJECTED_METRICS=$(curl -s "$BASE_URL/actuator/metrics/resilience4j.bulkhead.calls.rejected?tag=name:userServiceNonCritical" 2>/dev/null | jq -r '.measurements[0].value // "0"' 2>/dev/null)
if [ -z "$REJECTED_METRICS" ] || [ "$REJECTED_METRICS" = "null" ]; then
    REJECTED_METRICS="0"
fi
echo "Rechazos según métricas del actuator: $REJECTED_METRICS"

# Analizar resultados
echo ""
echo "=========================================="
echo "RESUMEN DE RESULTADOS"
echo "=========================================="
echo ""

# Contar por categoría y limpiar resultados (eliminar saltos de línea y espacios)
ACCEPTED_COUNT=$(grep -c ":ACCEPTED:" "$RESULTS_FILE" 2>/dev/null | tr -d '\n\r\t ' || echo "0")
WAITING_COUNT=$(grep -c ":WAITING:" "$RESULTS_FILE" 2>/dev/null | tr -d '\n\r\t ' || echo "0")
WAITING_LONG_COUNT=$(grep -c ":WAITING_LONG:" "$RESULTS_FILE" 2>/dev/null | tr -d '\n\r\t ' || echo "0")
REJECTED_COUNT=$(grep -c ":REJECTED:" "$RESULTS_FILE" 2>/dev/null | tr -d '\n\r\t ' || echo "0")
TIMEOUT_COUNT=$(grep -c ":TIMEOUT:" "$RESULTS_FILE" 2>/dev/null | tr -d '\n\r\t ' || echo "0")
ERROR_COUNT=$(grep -c ":ERROR:" "$RESULTS_FILE" 2>/dev/null | tr -d '\n\r\t ' || echo "0")

# Asegurar que los valores sean numéricos (convertir a enteros)
# Si está vacío o no es numérico, usar 0
ACCEPTED_COUNT=$(echo "$ACCEPTED_COUNT" | grep -E '^[0-9]+$' || echo "0")
WAITING_COUNT=$(echo "$WAITING_COUNT" | grep -E '^[0-9]+$' || echo "0")
WAITING_LONG_COUNT=$(echo "$WAITING_LONG_COUNT" | grep -E '^[0-9]+$' || echo "0")
REJECTED_COUNT=$(echo "$REJECTED_COUNT" | grep -E '^[0-9]+$' || echo "0")
TIMEOUT_COUNT=$(echo "$TIMEOUT_COUNT" | grep -E '^[0-9]+$' || echo "0")
ERROR_COUNT=$(echo "$ERROR_COUNT" | grep -E '^[0-9]+$' || echo "0")

# Asegurar que todas las variables tengan un valor numérico válido
# Convertir a enteros usando evaluación aritmética
ACCEPTED_COUNT=$((ACCEPTED_COUNT + 0))
WAITING_COUNT=$((WAITING_COUNT + 0))
WAITING_LONG_COUNT=$((WAITING_LONG_COUNT + 0))
REJECTED_COUNT=$((REJECTED_COUNT + 0))
TIMEOUT_COUNT=$((TIMEOUT_COUNT + 0))
ERROR_COUNT=$((ERROR_COUNT + 0))

# Calcular totales de forma segura
# Los TIMEOUTs también se consideran rechazos (el Bulkhead puede causar timeouts cuando rechaza)
TOTAL_PROCESSED=$((ACCEPTED_COUNT + WAITING_COUNT + WAITING_LONG_COUNT + REJECTED_COUNT + TIMEOUT_COUNT + ERROR_COUNT))
TOTAL_WAITING=$((WAITING_COUNT + WAITING_LONG_COUNT))
TOTAL_REJECTED=$((REJECTED_COUNT + TIMEOUT_COUNT))

echo -e "${GREEN}✅ Requests ACEPTADAS (procesadas inmediatamente): $ACCEPTED_COUNT${NC}"
echo -e "${YELLOW}⏳ Requests ESPERANDO (en cola): $TOTAL_WAITING${NC}"
echo "   - Espera corta (150-500ms): $WAITING_COUNT"
echo "   - Espera larga (>500ms): $WAITING_LONG_COUNT"
# Mostrar rechazos detectados en el script Y en las métricas
echo -e "${RED}❌ Requests RECHAZADAS: $TOTAL_REJECTED${NC}"
if [ "$TOTAL_REJECTED" -gt 0 ]; then
    if [ "$REJECTED_COUNT" -gt 0 ]; then
        echo "   - Rechazadas explícitamente: $REJECTED_COUNT"
    fi
    if [ "$TIMEOUT_COUNT" -gt 0 ]; then
        echo "   - Timeouts (posibles rechazos del Bulkhead): $TIMEOUT_COUNT"
    fi
    if [ "$REJECTED_METRICS" != "0" ] && [ -n "$REJECTED_METRICS" ] && [ "$REJECTED_METRICS" != "null" ]; then
        echo -e "   ${CYAN}Rechazos según métricas del actuator: $REJECTED_METRICS${NC}"
    fi
else
    echo ""
fi
if [ "$ERROR_COUNT" -gt 0 ]; then
    echo -e "${RED}⚠️  Requests con ERROR: $ERROR_COUNT${NC}"
fi
echo ""
echo "Total procesadas: $TOTAL_PROCESSED / $TOTAL_REQUESTS"
echo ""

# Limpiar archivos temporales
rm -f "$RESULTS_FILE" "$TIMING_FILE" /tmp/response_*.json 2>/dev/null