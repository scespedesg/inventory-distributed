# Branching strategy and CI configuration

## Branching model

- `main` es la rama estable utilizada para despliegues a producción.
- `laboratory` funciona como entorno de pruebas antes de fusionar a `main`.
- `integration` integra el trabajo proveniente de ramas de características.
- Se generan ramas de tipo `feature/*`, `bugfix/*`, `hotfix/*` o `chore/*` a partir de `integration`.
- El flujo de fusiones es: ramas de trabajo → `integration` → `laboratory` → `main`.
- Un flujo de GitHub Actions **Enforce PR branch origins** valida que solo se creen PRs siguiendo este flujo.

## Workflows de GitHub Actions

### CI
- Archivo: `.github/workflows/ci.yml`.
- Se ejecuta en pushes a `integration` y en pull requests dirigidos a `integration`, `laboratory` y `main`.
- Usa Java 17 (Temurin) y ejecuta `./mvnw -B -ntp verify` para compilar y ejecutar pruebas.

### Enforce PR branch origins
- Archivo: `.github/workflows/enforce-branch-origins.yml`.
- Verifica que las PRs hacia `main` provengan de `laboratory`, las PRs hacia `laboratory` de `integration` y las PRs hacia `integration` de ramas `feature/*`, `bugfix/*`, `hotfix/*` o `chore/*`.

### Protect branches
- Archivo: `.github/workflows/protect-branches.yml`.
- Asegura la existencia de las ramas `integration` y `laboratory`.
- Aplica protecciones a `main`, `laboratory` e `integration` para exigir el chequeo de estado `build`, prohibir push forzado y eliminar automáticamente ramas fusionadas.
