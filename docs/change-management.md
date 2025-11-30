# Gestión de Cambios, Releases y Rollback

Este documento define el proceso formal de gestión de cambios, la generación de release notes, el etiquetado de versiones y los planes de rollback para los entornos dev/stage/prod.

## Flujo de Change Management
1. **Solicitud de cambio**: crear Issue con tipo Feature/Fix/Chore y alcance claro.
2. **Rama de trabajo**: `feature/*`, `fix/*`, `chore/*`, `hotfix/*` (ver BRANCHING_STRATEGY.md).
3. **Pull Request**:
   - Completar plantilla de PR (impacto, tests, riesgos, plan de rollback).
   - Al menos 1 aprobación y CI verde.
4. **Merge a main**:
   - Dispara pipelines de dev/stage/prod según estrategia.
   - Release Drafter genera el draft de release notes automáticamente.
5. **Cut Release**:
   - Crear tag `v<MAJOR>.<MINOR>.<PATCH>` sobre main.
   - Publicar release en GitHub usando el draft generado.
6. **Deploy**:
   - Imágenes Docker etiquetadas con el tag de release (`<service>:vX.Y.Z`).
   - Despliegue progresivo (primero stage, luego prod).

## Etiquetado de Releases
- Formato: `v<MAJOR>.<MINOR>.<PATCH>` (ej. `v1.2.0`).
- Imagen Docker: `<service>:v<MAJOR>.<MINOR>.<PATCH>`.
- Rama asociada: `main` (no se taggean ramas de feature).

## Release Notes Automáticas
- **Release Drafter** (GitHub Actions) genera un draft en cada push/PR a `main`.
- Categorías sugeridas: Features, Fixes, Chores, Docs, Perf, Tests.
- Al publicar el release, el draft se convierte en notas oficiales.

## Planes de Rollback
- **Dev/Stage**:
  - Revertir a la última imagen estable: `kubectl set image ...=<image>:<tag-anterior>`.
  - Revertir tag en Helm/manifest si aplica.
  - Verificar health checks y métricas post-rollback.
- **Prod**:
  - Preferir rollback automático de pipeline (si el despliegue falla).
  - Si ya está en prod: despliegue del tag anterior (`vX.Y.(Z-1)`) y ejecución de smoke tests.
  - Documentar la causa y abrir Issue con remediación.

## Evidencias y Auditoría
- PRs con checklist y referencia a Issue.
- Release Notes generadas en GitHub Releases (draft automático).
- Tags de release trazables a commits firmados en `main`.

## Referencias
- `docs/BRANCHING_STRATEGY.md`
- `docs/observability-monitoring.md`
- `.github/workflows/release-drafter.yml`
