# Estrategia de Branching - GitHub Flow

## 1. Introducción

Este proyecto utiliza **GitHub Flow** como estrategia de branching. GitHub Flow es un flujo de trabajo simple y ágil diseñado para equipos que realizan despliegues frecuentes.

## 2. Principios Fundamentales

### 2.1 Main Branch - Siempre Desplegable

- La rama `main` **siempre** debe estar en un estado desplegable
- Cualquier commit en `main` debe poder desplegarse a producción
- **Nunca** hacer commit directo a `main` (excepto hotfixes críticos con aprobación)

### 2.2 Ramas de Corta Duración

- Todas las ramas se crean desde `main`
- Las ramas deben ser de corta duración (idealmente < 3 días)
- Se eliminan inmediatamente después del merge

### 2.3 Integración Continua

- Cada PR debe pasar todos los checks de CI antes del merge
- Los tests deben pasar en todas las ramas
- El código debe compilar sin errores

## 3. Tipos de Ramas

### 3.1 Feature Branches (`feature/`)

**Propósito**: Desarrollo de nuevas funcionalidades

**Nomenclatura**: `feature/descripcion-corta`

**Ejemplos**:
- `feature/user-authentication`
- `feature/payment-integration`
- `feature/product-search`

**Flujo**:
1. Crear rama desde `main`: `git checkout -b feature/nombre-feature main`
2. Desarrollar y hacer commits frecuentes
3. Crear PR a `main`
4. Obtener aprobación y merge
5. Eliminar rama después del merge

### 3.2 Fix Branches (`fix/`)

**Propósito**: Corrección de bugs

**Nomenclatura**: `fix/descripcion-bug`

**Ejemplos**:
- `fix/login-validation-error`
- `fix/order-calculation-bug`
- `fix/api-gateway-timeout`

**Flujo**:
1. Crear rama desde `main`: `git checkout -b fix/descripcion-bug main`
2. Corregir el bug y agregar tests
3. Crear PR a `main`
4. Obtener aprobación y merge
5. Eliminar rama después del merge

### 3.3 Chore Branches (`chore/`)

**Propósito**: Tareas de mantenimiento, refactoring, actualizaciones de dependencias

**Nomenclatura**: `chore/descripcion-tarea`

**Ejemplos**:
- `chore/update-dependencies`
- `chore/refactor-service-layer`
- `chore/improve-documentation`

**Flujo**:
1. Crear rama desde `main`: `git checkout -b chore/descripcion-tarea main`
2. Realizar la tarea
3. Crear PR a `main`
4. Obtener aprobación y merge
5. Eliminar rama después del merge

### 3.4 Hotfix Branches (`hotfix/`)

**Propósito**: Correcciones críticas que requieren despliegue inmediato

**Nomenclatura**: `hotfix/descripcion-issue`

**Ejemplos**:
- `hotfix/security-vulnerability`
- `hotfix/critical-data-loss`
- `hotfix/production-outage`

**Flujo**:
1. Crear rama desde `main`: `git checkout -b hotfix/descripcion-issue main`
2. Implementar la corrección (prioridad máxima)
3. Crear PR a `main` marcado como `hotfix`
4. Revisión rápida (máximo 2 revisores)
5. Merge y despliegue inmediato
6. Eliminar rama después del merge

**Nota**: Los hotfixes requieren aprobación explícita del Tech Lead o Product Owner.

## 4. Flujo de Trabajo para Desarrolladores

### 4.1 Iniciar una Nueva Tarea

```bash
# 1. Asegurarse de estar en main y actualizada
git checkout main
git pull origin main

# 2. Crear nueva rama
git checkout -b feature/nombre-feature main

# 3. Verificar que la rama se creó correctamente
git branch
```

### 4.2 Trabajar en la Rama

```bash
# Hacer cambios y commits frecuentes
git add .
git commit -m "feat: descripción del cambio"

# Push regular para backup
git push origin feature/nombre-feature
```

### 4.3 Preparar para PR

```bash
# 1. Asegurarse de que main está actualizada
git checkout main
git pull origin main

# 2. Rebase de la rama feature sobre main
git checkout feature/nombre-feature
git rebase main

# 3. Resolver conflictos si existen
# 4. Push (con force si fue necesario rebase)
git push origin feature/nombre-feature
# o si hubo rebase:
git push origin feature/nombre-feature --force-with-lease
```

### 4.4 Crear Pull Request

1. Ir a GitHub y crear PR desde la rama hacia `main`
2. Usar el template de PR (ver `.github/pull_request_template.md`)
3. Asignar revisores
4. Esperar aprobación y CI verde

### 4.5 Después del Merge

```bash
# 1. Volver a main
git checkout main

# 2. Actualizar main
git pull origin main

# 3. Eliminar rama local
git branch -d feature/nombre-feature

# 4. Eliminar rama remota (si no se eliminó automáticamente)
git push origin --delete feature/nombre-feature
```

## 5. Flujo de Trabajo para Operations

El equipo de operaciones sigue el mismo GitHub Flow para cambios operacionales:

### 5.1 Cambios de Infraestructura

- **Ramas**: `fix/`, `chore/`, `hotfix/`
- **Ejemplos**:
  - `fix/kubernetes-config-error`
  - `chore/update-terraform-modules`
  - `hotfix/database-connection-pool`

### 5.2 Cambios de CI/CD

- **Ramas**: `fix/`, `chore/`
- **Ejemplos**:
  - `fix/pipeline-failure`
  - `chore/improve-deployment-scripts`

### 5.3 Cambios de Configuración

- **Ramas**: `fix/`, `chore/`
- **Ejemplos**:
  - `fix/environment-variables`
  - `chore/update-docker-compose`

## 6. Convenciones de Commits

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
- `style`: Formato (sin cambios de código)
- `refactor`: Refactorización
- `test`: Tests
- `chore`: Mantenimiento

**Ejemplos**:
```
feat(favourite-service): add endpoint to find favourites by userId

fix(api-gateway): resolve LocalDateTime serialization issue

chore: update Spring Boot to 2.7.0
```

## 7. Protección de Ramas

### 7.1 Main Branch Protection

La rama `main` tiene las siguientes protecciones:
  
- ✅ **Require status checks to pass before merging**
  - CI pipeline debe pasar
  - Code coverage debe mantenerse o mejorar
  
- ✅ **Require branches to be up to date before merging**
  - La rama debe estar actualizada con `main`

### 7.2 Checks Requeridos

Antes de mergear a `main`, deben pasar:

1. **Build**: Compilación exitosa
2. **Tests**: Todos los tests unitarios e integración
3. **Linting**: Análisis estático de código
4. **Security Scan**: Escaneo de vulnerabilidades
5. **Code Coverage**: Cobertura de código mantenida

## 8. Continuous Deployment (CD)

### 8.1 Merge a Main = Despliegue

- Cada merge a `main` dispara automáticamente el pipeline de CD
- El despliegue se realiza al ambiente objetivo según la configuración
- Los hotfixes tienen prioridad en la cola de despliegue

### 8.2 Ambientes

1. **Development**: Despliegue automático desde `main`
2. **Staging**: Despliegue automático
3. **Production**: Despliegue manual con aprobación requerida

## 9. Resolución de Conflictos

### 9.1 Prevenir Conflictos

- Mantener ramas actualizadas con `main`
- Hacer rebase regularmente
- Commits pequeños y frecuentes

### 9.2 Resolver Conflictos

```bash
# 1. Actualizar main
git checkout main
git pull origin main

# 2. Rebase sobre main
git checkout feature/nombre-feature
git rebase main

# 3. Resolver conflictos en archivos
# Editar archivos con conflictos
git add archivo-resuelto
git rebase --continue

# 4. Push
git push origin feature/nombre-feature --force-with-lease
```

## 10. Buenas Prácticas

### 10.1 DO ✅

- Crear ramas descriptivas y cortas
- Hacer commits frecuentes y pequeños
- Mantener ramas actualizadas con `main`
- Escribir mensajes de commit claros
- Crear PRs pequeños y enfocados
- Responder rápidamente a comentarios de revisión
- Eliminar ramas después del merge

### 10.2 DON'T ❌

- No hacer commit directo a `main`
- No crear ramas muy largas (> 1 semana)
- No hacer force push a `main`
- No mergear PRs sin revisión
- No mergear PRs con CI rojo
- No dejar ramas huérfanas
- No hacer commits grandes y monolíticos

## 11. Diagrama de Flujo

```
main (siempre desplegable)
  │
  ├── feature/nueva-funcionalidad
  │     │
  │     ├── commits...
  │     │
  │     └── PR ──→ main ──→ CD ──→ Deploy
  │
  ├── fix/correccion-bug
  │     │
  │     └── PR ──→ main ──→ CD ──→ Deploy
  │
  └── hotfix/issue-critico
        │
        └── PR (prioridad) ──→ main ──→ CD ──→ Deploy inmediato
```

## 12. Referencias

- [GitHub Flow](https://guides.github.com/introduction/flow/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Git Branching Best Practices](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)

