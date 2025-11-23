# Metodología Ágil - Kanban

## 1. Introducción

Este proyecto utiliza la metodología **Kanban** para la gestión del desarrollo de software

## 2. Principios de Kanban

### 2.1 Visualización del Flujo de Trabajo

El tablero Kanban visualiza todas las tareas y su estado actual:

```
┌─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐
│   Backlog   │    To Do    │  In Progress│   Review    │    Done     │
├─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤
│             │             │             │             │             │
│  Tareas     │  Tareas     │  Tareas     │  Tareas     │  Tareas     │
│  pendientes │  listas     │  en         │  en         │  completadas│
│             │  para       │  desarrollo │  revisión   │             │
│             │  empezar    │             │             │             │
└─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘
```

### 2.2 Limitación del Trabajo en Progreso (WIP)

- **In Progress**: Máximo 3 tareas por desarrollador
- **Review**: Máximo 2 PRs en revisión simultáneamente
- Esto asegura que el equipo se enfoque en completar tareas antes de iniciar nuevas

### 2.3 Gestión del Flujo

- Las tareas se mueven de izquierda a derecha en el tablero
- Se prioriza el flujo continuo sobre la velocidad
- Se identifican y resuelven cuellos de botella rápidamente

### 2.4 Mejora Continua

- Reuniones diarias de sincronización (Daily Standup)
- Retrospectivas al final de cada iteración
- Métricas: Lead Time, Cycle Time, Throughput

## 3. Columnas del Tablero Kanban

### 3.1 Backlog
- **Descripción**: Todas las tareas identificadas pero no priorizadas
- **Criterio de entrada**: Nueva funcionalidad, bug, o mejora identificada
- **Responsable**: Product Owner / Tech Lead

### 3.2 To Do
- **Descripción**: Tareas priorizadas y listas para ser trabajadas
- **Criterio de entrada**: 
  - Tarea tiene descripción clara
  - Criterios de aceptación definidos
  - Dependencias identificadas
- **Responsable**: Desarrollador asignado

### 3.3 In Progress
- **Descripción**: Tareas actualmente en desarrollo
- **Criterio de entrada**: Desarrollador ha comenzado a trabajar en la tarea
- **Criterio de salida**: 
  - Código implementado
  - Tests escritos y pasando
  - PR creado
- **Límite WIP**: 3 tareas máximo por desarrollador

### 3.4 Review
- **Descripción**: Pull Requests en revisión
- **Criterio de entrada**: PR abierto y listo para revisión
- **Criterio de salida**: 
  - Al menos 1 aprobación
  - Todos los comentarios resueltos
  - CI/CD pipeline exitoso
- **Límite WIP**: 2 PRs máximo

### 3.5 Done
- **Descripción**: Tareas completadas y desplegadas
- **Criterio de entrada**: 
  - PR mergeado a `main`
  - Desplegado en ambiente objetivo
  - Documentación actualizada (si aplica)

## 4. Roles y Responsabilidades

### 4.1 Desarrollador
- Crear ramas siguiendo la estrategia de branching
- Desarrollar y probar código
- Crear PRs con descripción clara
- Responder a comentarios de revisión
- Mover tarjetas en el tablero Kanban

### 4.2 Revisor
- Revisar PRs dentro de 24 horas
- Proporcionar feedback constructivo
- Aprobar o solicitar cambios
- Verificar que los criterios de aceptación se cumplan

### 4.3 Tech Lead / Product Owner
- Priorizar el backlog
- Definir criterios de aceptación
- Resolver bloqueos
- Aprobar cambios críticos

## 5. Ceremonias Kanban

### 5.1 Daily Standup (15 minutos)
- **Cuándo**: Diariamente a la misma hora
- **Participantes**: Todo el equipo
- **Formato**:
  - ¿Qué hice ayer?
  - ¿Qué haré hoy?
  - ¿Hay algún bloqueo?

### 5.2 Revisión del Tablero (Semanal)
- **Cuándo**: Una vez por semana
- **Objetivo**: 
  - Identificar cuellos de botella
  - Ajustar límites WIP si es necesario
  - Priorizar backlog

### 5.3 Retrospectiva (Mensual)
- **Cuándo**: Al final de cada mes
- **Formato**: Start, Stop, Continue
- **Objetivo**: Mejora continua del proceso

## 6. Métricas y KPIs

### 6.1 Lead Time
- Tiempo desde que una tarea entra al backlog hasta que se completa
- **Objetivo**: Reducir continuamente

### 6.2 Cycle Time
- Tiempo desde que una tarea entra a "In Progress" hasta "Done"
- **Objetivo**: < 5 días para tareas pequeñas, < 10 días para tareas medianas

### 6.3 Throughput
- Número de tareas completadas por semana
- **Objetivo**: Mantener consistencia

### 6.4 WIP
- Trabajo en progreso actual
- **Objetivo**: Respetar límites establecidos

## 7. Herramientas

- **GitHub Projects**: Tablero Kanban digital
- **GitHub Issues**: Gestión de tareas
- **GitHub Pull Requests**: Revisión de código
- **GitHub Actions**: CI/CD pipeline

## 8. Mejora Continua

El proceso Kanban es evolutivo. El equipo debe:
- Revisar regularmente la efectividad del proceso
- Ajustar límites WIP según la capacidad del equipo
- Experimentar con mejoras y medir resultados
- Documentar lecciones aprendidas

## 9. Referencias

- [Kanban Guide](https://kanban.university/kanban-guide/)
- [Kanban Methodology](https://www.atlassian.com/agile/kanban)
- [GitHub Projects](https://docs.github.com/en/issues/planning-and-tracking-with-projects)

