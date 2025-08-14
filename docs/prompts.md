# prompts.md — Registro de prompts usados

> Requerimiento de la prueba técnica: documentar las indicaciones usadas para generar código, workflows y documentación.
> Nota: Los prompts se registran de forma resumida, sin incluir respuestas largas del asistente.

| Fecha (UTC)   | Contexto | Prompt (texto enviado) | Artefacto/Resultado |
|---|---|---|---|
| 2025-08-13 | Setup Quarkus | "Necesito que me des las instrucciones para iniciar un proyecto en Quarkus" | Comandos `quarkus create` / Maven, extensiones base y `application.properties` mínimo. |
| 2025-08-13 | Corrección extensiones | "El CLI falló con rest-assured, ¿qué hago?" | Ajuste del comando de creación (remover `rest-assured` como extensión). |
| 2025-08-13 | Requisitos del PDF | "Ten en cuenta el PDF: CQRS + eventos, X-API-Key, idempotencia, observabilidad, pruebas de concurrencia" | `README.md` y `run.md` iniciales con arquitectura, endpoints y tests propuestos. |
| 2025-08-13 | Endpoints y seguridad | "API con GET inventario, PATCH ajustes, POST/DELETE reservas; proteger con X-API-Key: dev-key" | Especificación de endpoints y filtro `ApiKeyAuthFilter` (borrador). |
| 2025-08-13 | Estrategia de ramas | "Quiero main, laboratory, integration; merges encadenados; todo por PR" | Propuesta de flujo `feature/* → integration → laboratory → main`. |
| 2025-08-13 | Protecciones (UI) | "¿Cómo configuro Branch protection en GitHub?" | Pasos en Settings → Branches (PR requerido, checks, sin push directo). |
| 2025-08-13 | Automatizar con CI | "¿Puedo hacer estas configuraciones desde CI?" | Diseño de workflows para crear/proteger ramas vía API (PAT). |
| 2025-08-13 | Token PAT | "No entiendo el paso del token, sé más específico" | Guía paso a paso para crear **Fine-grained PAT** y guardarlo como `ADMIN_TOKEN`. |
| 2025-08-13 | Workflows CI | "Crea los tres workflows: ci.yml, enforce-branch-origins.yml, protect-branches.yml" | Archivos YAML con build Maven (JDK17), guardián de PR y protección de ramas. |
| 2025-08-13 | Fix YAML | "El protect-branches falló por sintaxis; dame uno simplificado" | Versión simplificada sin heredocs; ejecución exitosa. |
| 2025-08-13 | Bootstrap ramas | "Integration/Laboratory no ejecutan workflows; ¿Cómo los llevo allí?" | Procedimiento de bootstrap (PRs y ajustes temporales de checks). |
| 2025-08-13 | Aprobaciones y limpieza | "Soy único dev, no quiero aprobador y quiero borrar la rama tras merge" | Ajuste de `required_approving_review_count=0` y `delete_branch_on_merge=true`. |
| 2025-08-13 | Documentación final | "Genera un .md con estrategia de branch y CI; y otro con los prompts usados" | `docs/BRANCHING_CI.md` y este `prompts.md`. |

## Cómo reproducir
1. Los workflows están en `.github/workflows/`: `ci.yml`, `enforce-branch-origins.yml`, `protect-branches.yml`.
2. La estrategia está documentada en `docs/BRANCHING_CI.md`.
3. Los cambios se integran siempre con PR siguiendo el flujo: `feature/* → integration → laboratory → main`.
