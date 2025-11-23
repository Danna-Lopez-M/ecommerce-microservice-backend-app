# Guía de Contribución

¡Gracias por tu interés en contribuir a este proyecto! Esta guía te ayudará a entender cómo contribuir de manera efectiva.

## Tabla de Contenidos

1. [Código de Conducta](#código-de-conducta)
2. [Cómo Contribuir](#cómo-contribuir)
3. [Proceso de Desarrollo](#proceso-de-desarrollo)
4. [Estándares de Código](#estándares-de-código)
5. [Proceso de Pull Request](#proceso-de-pull-request)
6. [Preguntas Frecuentes](#preguntas-frecuentes)

## Código de Conducta

Este proyecto sigue un código de conducta. Al participar, se espera que mantengas este código. Por favor, reporta comportamientos inaceptables.

## Cómo Contribuir

### Reportar Bugs

Si encuentras un bug:

1. Verifica que el bug no haya sido reportado ya en [Issues](https://github.com/Danna-Lopez-M/ecommerce-microservice-backend-app/issues)
2. Si no existe, crea un nuevo issue con:
   - Título descriptivo
   - Descripción clara del problema
   - Pasos para reproducir
   - Comportamiento esperado vs actual
   - Ambiente (OS, versión de Java, etc.)
   - Screenshots si aplica

### Sugerir Mejoras

Para sugerir una nueva funcionalidad:

1. Verifica que la sugerencia no exista ya
2. Crea un issue con:
   - Descripción clara de la funcionalidad
   - Justificación del valor que aporta
   - Posible implementación (si tienes ideas)

### Contribuir con Código

1. Fork el repositorio
2. Crea una rama desde `main` (ver [Branching Strategy](./docs/BRANCHING_STRATEGY.md))
3. Realiza tus cambios
4. Asegúrate de que los tests pasen
5. Crea un Pull Request

## Proceso de Desarrollo

### Metodología

Este proyecto utiliza **Kanban** para la gestión del trabajo. Ver [Metodología Ágil](./docs/AGILE_METHODOLOGY.md) para más detalles.

### Estrategia de Branching

Seguimos **GitHub Flow**. Ver [Estrategia de Branching](./docs/BRANCHING_STRATEGY.md) para detalles completos.

**Resumen rápido**:
- `main` siempre está desplegable
- Crea ramas cortas desde `main`: `feature/`, `fix/`, `chore/`, `hotfix/`
- Crea PRs pequeños y enfocados
- Merge solo después de revisión y CI verde

## Estándares de Código

### Convenciones de Nombres

- **Clases**: PascalCase (`UserService`, `OrderController`)
- **Métodos/Variables**: camelCase (`getUserById`, `orderId`)
- **Constantes**: UPPER_SNAKE_CASE (`MAX_RETRY_ATTEMPTS`)
- **Paquetes**: lowercase (`com.selimhorri.app.service`)

### Estructura de Código

```
src/main/java/com/selimhorri/app/
├── config/          # Configuraciones
├── domain/          # Entidades de dominio
├── dto/             # Data Transfer Objects
├── repository/       # Repositorios JPA
├── service/          # Lógica de negocio
│   └── impl/        # Implementaciones
├── resource/         # Controladores REST
└── exception/       # Manejo de excepciones
```

### Convenciones de Commits

Seguimos [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Tipos**:
- `feat`: Nueva funcionalidad
- `fix`: Corrección de bug
- `docs`: Documentación
- `style`: Formato
- `refactor`: Refactorización
- `test`: Tests
- `chore`: Mantenimiento

**Ejemplos**:
```bash
feat(favourite-service): add endpoint to find favourites by userId
fix(api-gateway): resolve LocalDateTime serialization issue
docs: update branching strategy documentation
```

### Tests

- Escribe tests para toda nueva funcionalidad
- Mantén cobertura de código > 80%
- Tests deben ser rápidos y aislados
- Usa nombres descriptivos: `shouldReturnUserWhenValidIdProvided()`

### Documentación

- Documenta APIs públicas
- Actualiza README si es necesario
- Agrega comentarios para lógica compleja
- Mantén Javadoc actualizado

## Proceso de Pull Request

### Antes de Crear el PR

1. ✅ Asegúrate de que tu rama esté actualizada con `main`
2. ✅ Ejecuta los tests localmente
3. ✅ Verifica que el código compile sin warnings
4. ✅ Revisa tu propio código

### Crear el PR

1. Usa el [template de PR](.github/pull_request_template.md)
2. Proporciona descripción clara de los cambios
3. Vincula issues relacionados
4. Asigna revisores apropiados
5. Espera a que CI pase

### Durante la Revisión

1. Responde a comentarios de manera constructiva
2. Realiza cambios solicitados
3. Mantén el PR actualizado con `main` si es necesario
4. No hagas force push después de que comience la revisión (a menos que sea necesario)

### Después del Merge

1. Elimina tu rama local y remota
2. Actualiza `main` local
3. Celebra tu contribución 🎉

## Preguntas Frecuentes

### ¿Puedo trabajar en múltiples features simultáneamente?

Sí, pero respeta los límites WIP de Kanban (máximo 3 tareas en progreso).

### ¿Qué hago si mi PR tiene conflictos?

1. Actualiza tu rama con `main`: `git rebase main`
2. Resuelve conflictos
3. Push: `git push`

### ¿Cuánto tiempo tarda la revisión?

Objetivo: < 24 horas. Si no recibes respuesta, menciona a los revisores.

### ¿Puedo mergear mi propio PR?

No. Todos los PRs requieren al menos 1 aprobación de otro miembro del equipo.

### ¿Qué pasa si el CI falla?

Revisa los logs, corrige el problema, y push nuevamente. El CI debe estar verde antes del merge.

## Recursos Adicionales

- [Metodología Ágil - Kanban](./docs/AGILE_METHODOLOGY.md)
- [Estrategia de Branching](./docs/BRANCHING_STRATEGY.md)
- [README Principal](./README.md)

## Contacto

Si tienes preguntas, contacta al equipo.

¡Gracias por contribuir! 🚀

