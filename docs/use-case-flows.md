# Diagramas de flujo de casos de uso

Este documento describe los flujos internos de los casos de uso que componen la API de inventario: **reservar**, **confirmar**, **liberar** y **ajustar** stock. Cada sección incluye un diagrama Mermaid y una explicación paso a paso del proceso.

## Reserva

```mermaid
flowchart TB
  A([POST /v1/inventory/reserve]):::step --> B{Header Idempotency-Key?}:::decision

  B --> N0[[No]]
  N0 --> E400[400 Bad Request]:::error

  B --> N1[[Si]]
  N1 --> C[Calcular hash sha256 metodo+ruta+cuerpo]:::step

  subgraph TX["Transaccion reactiva con WithTransaction"]
    C --> D[Buscar clave por PK en idempotency_keys]:::step
    D --> K{Existe la clave?}:::decision

    K --> Y1[[Si]]
    Y1 --> H{requestHash coincide?}:::decision
    H --> HN[[No]]
    HN --> E409A[409 Conflict Idempotency-Key con otro payload]:::error
    H --> HY[[Si]]
    HY --> S{status = COMPLETED?}:::decision
    S --> SY[[Si]]
    SY --> RCACHE[Devolver respuesta almacenada]:::step
    S --> SN[[No]]
    SN --> E202[202 Processing]:::warn

    K --> Y0[[No]]
    Y0 --> I[Insertar PENDING en idempotency_keys]:::step --> J[Obtener o crear inventario skuId]:::step
    J --> Q{quantity > 0?}:::decision
    Q --> QN[[No]]
    QN --> E422Q[422 quantity invalida]:::error
    Q --> QY[[Si]]
    QY --> AV{available >= quantity?}:::decision
    AV --> AVN[[No]]
    AVN --> E422A[422 insufficient_stock]:::error
    AV --> AVY[[Si]]
    AVY --> U[inv.reserved += quantity]:::step --> V{Conflicto de version optimistic locking}:::decision
    V --> VN[[Si]]
    VN --> E409V[409 Conflict reintentar]:::error
    V --> VY[[No]]
    VY --> W[Insertar evento StockReserved en outbox_events]:::step --> M[Completar idempotency_keys con status COMPLETED httpStatus 201 responseJson]:::step
    M --> R201[201 Created respuesta con skuId onHand reserved available version]:::ok
  end

  Outbox[(outbox_events)]:::db -.-> PUB[Outbox Publisher job]:::ext
  PUB -.-> BUS[Event Bus]:::ext
  BUS -.-> PRJ[Projector actualiza vistas de lectura]:::ext

  Idem[(idempotency_keys)]:::db
  Inv[(inventory)]:::db
  I --- Idem
  D --- Idem
  J --- Inv
  W --- Outbox

  classDef step fill:#f5fff8,stroke:#6b8,stroke-width:1
  classDef decision fill:#fffbe6,stroke:#cc9,stroke-width:1
  classDef db fill:#eef7ff,stroke:#58c,stroke-width:1.2
  classDef ok fill:#e8fff0,stroke:#3b915f,stroke-width:1.2
  classDef error fill:#ffeaea,stroke:#e06666,stroke-width:1.2
  classDef warn fill:#fff7e6,stroke:#cc9900,stroke-width:1.2
  classDef ext fill:#f3e8ff,stroke:#8b5cf6,stroke-width:1.2
```

### Análisis
1. El endpoint exige un **Idempotency-Key**; su ausencia provoca un `400 Bad Request`.
2. Con la clave presente se calcula un hash del método, ruta y cuerpo para verificar reintentos con el mismo payload.
3. Dentro de una transacción reactiva se busca la clave:
   - Si ya existe y el `requestHash` difiere, se devuelve `409 Conflict` por uso indebido de la clave.
   - Si la clave existe y está **COMPLETED**, se responde con el resultado almacenado evitando reprocesar.
   - Si la clave existe pero no está completada, se informa `202 Processing` indicando que la operación original sigue en curso.
4. Si la clave no existe se registra como **PENDING** y se obtiene o crea el inventario del `skuId` solicitado.
5. Se validan reglas de negocio: `quantity > 0` y que haya `available` suficiente.
6. El agregado incrementa la reserva (`reserved += quantity`) usando bloqueo optimista; un conflicto de versión retorna `409` para que el cliente reintente.
7. Al confirmar la actualización se genera un evento `StockReserved` en la *outbox* y se completa la entrada de idempotencia con estado `COMPLETED` y respuesta `201`.
8. Un *publisher* externo procesa la outbox y emite el evento al bus, desde donde los *projectors* actualizan las vistas de lectura.

## Confirmación

```mermaid
flowchart TB
  A([POST /v1/inventory/confirm]):::step --> B{Header Idempotency-Key?}:::decision

  B --> N0[[No]]
  N0 --> E400[400 Bad Request]:::error

  B --> N1[[Si]]
  N1 --> C[Calcular hash sha256 metodo+ruta+cuerpo]:::step

  subgraph TX["Transaccion reactiva con WithTransaction"]
    C --> D[Buscar clave por PK en idempotency_keys]:::step
    D --> K{Existe la clave?}:::decision

    K --> Y1[[Si]]
    Y1 --> H{requestHash coincide?}:::decision
    H --> HN[[No]]
    HN --> E409A[409 Conflict Idempotency-Key con otro payload]:::error
    H --> HY[[Si]]
    HY --> S{status = COMPLETED?}:::decision
    S --> SY[[Si]]
    SY --> RCACHE[Devolver respuesta almacenada]:::step
    S --> SN[[No]]
    SN --> E202[202 Processing]:::warn

    K --> Y0[[No]]
    Y0 --> I[Insertar PENDING en idempotency_keys]:::step --> J[Buscar inventario por skuId]:::step
    J --> JN[[No]]
    JN --> E404[404 sku_not_found]:::error
    J --> JY[[Si]]
    JY --> Q{quantity > 0?}:::decision
    Q --> QN[[No]]
    QN --> E422Q[422 quantity invalida]:::error
    Q --> QY[[Si]]
    QY --> RV{reserved >= quantity?}:::decision
    RV --> RVN[[No]]
    RVN --> E422R[422 invalid_confirmation]:::error
    RV --> RVY[[Si]]
    RVY --> U[reserved -= quantity y onHand -= quantity]:::step --> V{Conflicto de version optimistic locking}:::decision
    V --> VN[[Si]]
    VN --> E409V[409 Conflict reintentar]:::error
    V --> VY[[No]]
    VY --> W[Insertar evento StockCommitted en outbox_events]:::step --> M[Completar idempotency_keys con status COMPLETED httpStatus 204]:::step
    M --> R204[204 No Content]:::ok
  end

  Outbox[(outbox_events)]:::db -.-> PUB[Outbox Publisher job]:::ext
  PUB -.-> BUS[Event Bus]:::ext
  BUS -.-> PRJ[Projector actualiza vistas de lectura]:::ext

  Idem[(idempotency_keys)]:::db
  Inv[(inventory)]:::db
  I --- Idem
  D --- Idem
  J --- Inv
  W --- Outbox

  classDef step fill:#f5fff8,stroke:#6b8,stroke-width:1
  classDef decision fill:#fffbe6,stroke:#cc9,stroke-width:1
  classDef db fill:#eef7ff,stroke:#58c,stroke-width:1.2
  classDef ok fill:#e8fff0,stroke:#3b915f,stroke-width:1.2
  classDef error fill:#ffeaea,stroke:#e06666,stroke-width:1.2
  classDef warn fill:#fff7e6,stroke:#cc9900,stroke-width:1.2
  classDef ext fill:#f3e8ff,stroke:#8b5cf6,stroke-width:1.2
```

### Análisis
1. El flujo inicial es idéntico al caso de reserva respecto a la validación de la clave de idempotencia y del `requestHash`.
2. Tras insertar la clave **PENDING** se busca el inventario del `skuId`; si no existe se responde `404 sku_not_found`.
3. Se valida que la cantidad sea positiva y que la reserva actual (`reserved`) sea suficiente para confirmar.
4. La confirmación reduce tanto `reserved` como `onHand`, reflejando una venta definitiva.
5. El update usa bloqueo optimista; los conflictos generan `409 Conflict` para reintentar.
6. Se emite un evento `StockCommitted` y se marca la transacción como **COMPLETED** con respuesta `204 No Content`.
7. Los projectores de lectura consumen el evento del bus y actualizan sus vistas.

## Liberación

```mermaid
flowchart TB
  A([POST /v1/inventory/release]):::step --> B{Header Idempotency-Key?}:::decision

  B --> N0[[No]]
  N0 --> E400[400 Bad Request]:::error

  B --> N1[[Si]]
  N1 --> C[Calcular hash sha256 metodo+ruta+cuerpo]:::step

  subgraph TX["Transaccion reactiva con WithTransaction"]
    C --> D[Buscar clave por PK en idempotency_keys]:::step
    D --> K{Existe la clave?}:::decision

    K --> Y1[[Si]]
    Y1 --> H{requestHash coincide?}:::decision
    H --> HN[[No]]
    HN --> E409A[409 Conflict Idempotency-Key con otro payload]:::error
    H --> HY[[Si]]
    HY --> S{status = COMPLETED?}:::decision
    S --> SY[[Si]]
    SY --> RCACHE[Devolver respuesta almacenada]:::step
    S --> SN[[No]]
    SN --> E202[202 Processing]:::warn

    K --> Y0[[No]]
    Y0 --> I[Insertar PENDING en idempotency_keys]:::step --> J[Buscar inventario por skuId]:::step
    J --> JN[[No]]
    JN --> E404[404 sku_not_found]:::error
    J --> JY[[Si]]
    JY --> Q{quantity > 0?}:::decision
    Q --> QN[[No]]
    QN --> E422Q[422 quantity invalida]:::error
    Q --> QY[[Si]]
    QY --> RV{reserved >= quantity?}:::decision
    RV --> RVN[[No]]
    RVN --> E422R[422 invalid_release]:::error
    RV --> RVY[[Si]]
    RVY --> U[reserved -= quantity]:::step --> V{Conflicto de version optimistic locking}:::decision
    V --> VN[[Si]]
    VN --> E409V[409 Conflict reintentar]:::error
    V --> VY[[No]]
    VY --> W[Insertar evento StockReleased en outbox_events]:::step --> M[Completar idempotency_keys con status COMPLETED httpStatus 204]:::step
    M --> R204[204 No Content]:::ok
  end

  Outbox[(outbox_events)]:::db -.-> PUB[Outbox Publisher job]:::ext
  PUB -.-> BUS[Event Bus]:::ext
  BUS -.-> PRJ[Projector actualiza vistas de lectura]:::ext

  Idem[(idempotency_keys)]:::db
  Inv[(inventory)]:::db
  I --- Idem
  D --- Idem
  J --- Inv
  W --- Outbox

  classDef step fill:#f5fff8,stroke:#6b8,stroke-width:1
  classDef decision fill:#fffbe6,stroke:#cc9,stroke-width:1
  classDef db fill:#eef7ff,stroke:#58c,stroke-width:1.2
  classDef ok fill:#e8fff0,stroke:#3b915f,stroke-width:1.2
  classDef error fill:#ffeaea,stroke:#e06666,stroke-width:1.2
  classDef warn fill:#fff7e6,stroke:#cc9900,stroke-width:1.2
  classDef ext fill:#f3e8ff,stroke:#8b5cf6,stroke-width:1.2
```

### Análisis
1. La liberación comparte el mismo patrón de idempotencia y búsqueda del inventario que los casos anteriores.
2. Se valida que la cantidad sea positiva y que exista una reserva suficiente para liberar.
3. La operación sólo decrementa el campo `reserved`, devolviendo el stock al disponible sin modificar `onHand`.
4. Ante conflictos de versión se responde `409 Conflict` para permitir reintentos controlados.
5. Se genera un evento `StockReleased` y se completa la idempotencia con respuesta `204`.

## Ajuste

```mermaid
flowchart TB
  A([POST /v1/inventory/adjust]):::step --> B{Header Idempotency-Key?}:::decision

  B --> N0[[No]]
  N0 --> E400[400 Bad Request]:::error

  B --> N1[[Si]]
  N1 --> C[Calcular hash sha256 metodo+ruta+cuerpo]:::step

  subgraph TX["Transaccion reactiva con WithTransaction"]
    C --> D[Buscar clave por PK en idempotency_keys]:::step
    D --> K{Existe la clave?}:::decision

    K --> Y1[[Si]]
    Y1 --> H{requestHash coincide?}:::decision
    H --> HN[[No]]
    HN --> E409A[409 Conflict Idempotency-Key con otro payload]:::error
    H --> HY[[Si]]
    HY --> S{status = COMPLETED?}:::decision
    S --> SY[[Si]]
    SY --> RCACHE[Devolver respuesta almacenada]:::step
    S --> SN[[No]]
    SN --> E202[202 Processing]:::warn

    K --> Y0[[No]]
    Y0 --> I[Insertar PENDING en idempotency_keys]:::step --> J[Obtener o crear inventario skuId]:::step
    J --> Q{delta diferente de 0?}:::decision
    Q --> QN[[No]]
    QN --> E422Q[422 delta invalido]:::error
    Q --> QY[[Si]]
    QY --> AV{onHand + delta >= reserved?}:::decision
    AV --> AVN[[No]]
    AVN --> E422A[422 adjust_would_break_reserved]:::error
    AV --> AVY[[Si]]
    AVY --> U[onHand = onHand + delta]:::step --> V{Conflicto de version optimistic locking}:::decision
    V --> VN[[Si]]
    VN --> E409V[409 Conflict reintentar]:::error
    V --> VY[[No]]
    VY --> W[Insertar evento StockAdjusted en outbox_events]:::step --> M[Completar idempotency_keys con status COMPLETED httpStatus 200]:::step
    M --> R200[200 OK respuesta con skuId onHand reserved available version]:::ok
  end

  Outbox[(outbox_events)]:::db -.-> PUB[Outbox Publisher job]:::ext
  PUB -.-> BUS[Event Bus]:::ext
  BUS -.-> PRJ[Projector actualiza vistas de lectura]:::ext

  Idem[(idempotency_keys)]:::db
  Inv[(inventory)]:::db
  I --- Idem
  D --- Idem
  J --- Inv
  W --- Outbox

  classDef step fill:#f5fff8,stroke:#6b8,stroke-width:1
  classDef decision fill:#fffbe6,stroke:#cc9,stroke-width:1
  classDef db fill:#eef7ff,stroke:#58c,stroke-width:1.2
  classDef ok fill:#e8fff0,stroke:#3b915f,stroke-width:1.2
  classDef error fill:#ffeaea,stroke:#e06666,stroke-width:1.2
  classDef warn fill:#fff7e6,stroke:#cc9900,stroke-width:1.2
  classDef ext fill:#f3e8ff,stroke:#8b5cf6,stroke-width:1.2
```

### Análisis
1. Se verifica el `Idempotency-Key` y el `requestHash` como en los demás casos.
2. Se obtiene o crea el inventario del `skuId` y se valida que el **delta** no sea cero.
3. La regla `onHand + delta >= reserved` evita dejar reservas sin respaldo de stock físico.
4. Si las validaciones pasan, se ajusta `onHand` sumando el delta positivo o negativo.
5. Un conflicto de versión produce `409 Conflict`; de lo contrario se emite `StockAdjusted` y se responde con los nuevos valores de inventario (`200 OK`).
6. Los projectores actualizarán las vistas de lectura tras consumir el evento.
