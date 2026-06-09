# Registro de Defectos

Este documento recopila los **defectos detectados y resueltos durante las pruebas unitarias, de integración y de sistema** del proyecto **Registraduría**.
Cada defecto se documenta de manera estructurada para facilitar su análisis, trazabilidad y corrección.

---

## Formato 1: Lista detallada (narrativa)

### Defecto 01 — Falta de validación de edad negativa *(Prueba unitaria)*

- **Capa afectada:** Dominio (`Registry.registerVoter`)
- **Caso de prueba:** Registro de persona con edad `-1`.
- **Entrada:**
  `Person(name="Juan", id=101, age=-1, gender=MALE, alive=true)`
- **Resultado esperado:** `UNDERAGE`
- **Resultado obtenido:** `UNDERAGE`
- **Causa probable:** Inicialmente no se validaba. Ahora se valida de forma que cualquier edad menor a 18 años (incluyendo números negativos) es rechazada correctamente como `UNDERAGE`.
- **Tipo de prueba:** Unitaria (dominio puro)
- **Estado:** Resuelto
- **Prioridad:** Alta

---

### Defecto 02 — Registro de persona fallecida *(Prueba unitaria)*

- **Capa afectada:** Dominio (`Registry.registerVoter`)
- **Caso de prueba:** Persona con `alive=false`.
- **Entrada:**
  `Person(name="Ana", id=102, age=45, gender=FEMALE, alive=false)`
- **Resultado esperado:** `DEAD`
- **Resultado obtenido:** `DEAD`
- **Causa probable:** Se corrigió añadiendo la validación `if (!p.isAlive()) return RegisterResult.DEAD;` en la lógica principal.
- **Tipo de prueba:** Unitaria (regla de negocio)
- **Estado:** Resuelto
- **Prioridad:** Media

---

### Defecto 03 — No se detectan duplicados *(Prueba de integración con H2)*

- **Capa afectada:** Infraestructura (`RegistryRepository`)
- **Caso de prueba:** Dos registros con el mismo `id`.
- **Entradas:**
  - Persona 1 → `Person(name="Carlos", id=200, age=30, gender=MALE, alive=true)`
  - Persona 2 → `Person(name="Carla", id=200, age=25, gender=FEMALE, alive=true)`
- **Resultado esperado:**
  - Persona 1 → `VALID`
  - Persona 2 → `DUPLICATED`
- **Resultado obtenido:**
  - Persona 1 → `VALID`
  - Persona 2 → `DUPLICATED`
- **Causa probable:** Se corrigió validando la existencia mediante `repo.existsById(p.getId())` antes de guardar el registro en la base de datos H2.
- **Tipo de prueba:** Integración (H2 + capa de aplicación)
- **Estado:** Resuelto
- **Prioridad:** Alta

---

### Defecto 04 — Fallo en simulación con mock *(Prueba de integración con Mockito)*

- **Capa afectada:** Aplicación (`RegistryWithMockTest`)
- **Caso de prueba:** Registro con `id` duplicado en un repositorio simulado.
- **Configuración:**
  ```java
  when(repo.existsById(7)).thenReturn(true);
  ```
- **Resultado esperado:** `DUPLICATED`
- **Resultado obtenido:** `DUPLICATED`
- **Causa probable:** El mock no estaba inyectado correctamente en el constructor del caso de uso. Se solucionó asegurando que `registry = new Registry(repo);` use el mock mockeado en el `@Before` setup.
- **Tipo de prueba:** Integración (mock)
- **Estado:** Resuelto
- **Prioridad:** Media

---

### Defecto 05 — Error HTTP 500 no manejado *(Prueba de sistema REST)*

- **Capa afectada:** Delivery (`RegistryController`)
- **Caso de prueba:** Envío de JSON con campo `gender` inválido.
- **Entrada:**
  ```json
  { "name": "Laura", "id": 500, "age": 20, "gender": "OTHER", "alive": true }
  ```
- **Resultado esperado:** `HTTP 400` (Bad Request)
- **Resultado obtenido:** `HTTP 400` (Bad Request)
- **Causa probable:** Falta de un manejador de excepciones. Se solucionó implementando un método `@ExceptionHandler(IllegalArgumentException.class)` en el controlador `RegistryController` que intercepta la conversión fallida del String al Enum `Gender` y devuelve un código de estado `HTTP 400 (Bad Request)` con el cuerpo `"INVALID_INPUT"`.
- **Tipo de prueba:** Sistema (TestRestTemplate)
- **Estado:** Resuelto
- **Prioridad:** Alta

---

## Formato 2: Tabla de defectos (bug tracking)

| ID | Caso de Prueba | Capa | Resultado Esperado | Resultado Obtenido | Tipo | Estado | Prioridad |
|----|----------------|------|--------------------|--------------------|------|----------|------------|
| 01 | Edad negativa | Dominio | `UNDERAGE` | `UNDERAGE` | Unitaria | Resuelto | Alta |
| 02 | Persona muerta | Dominio | `DEAD` | `DEAD` | Unitaria | Resuelto | Media |
| 03 | Duplicado por ID | Infraestructura | `DUPLICATED` | `DUPLICATED` | Integración | Resuelto | Alta |
| 04 | Mock mal configurado | Aplicación | `DUPLICATED` | `DUPLICATED` | Integración (mock) | Resuelto | Media |
| 05 | Error HTTP 500 | Delivery | `HTTP 400` | `HTTP 400` | Sistema (REST) | Resuelto | Alta |

---

## Convenciones de Estado

| Estado | Significado |
|---------|-------------|
| **Abierto** | El defecto fue detectado pero no corregido. |
| **En progreso** | El defecto se encuentra en análisis o corrección. |
| **Resuelto** | El defecto fue corregido y validado mediante pruebas. |

---

## Observaciones

- Los defectos detectados y resueltos evidencian la importancia de **mantener pruebas unitarias robustas** antes de pasar a integración.
- La validación cruzada entre pruebas con mocks e integración real (H2) permitió identificar inconsistencias en el flujo de persistencia.
- Los errores en las pruebas REST destacan la necesidad de implementar **manejadores globales de excepciones (ControllerAdvice)** o locales en los controladores para evitar fugas de excepciones internas (HTTP 500) y mejorar la robustez de las APIs públicas.
