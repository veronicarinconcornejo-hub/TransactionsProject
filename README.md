# Transaction Execution API

API REST que gestiona la ejecución de transacciones financieras de crédito y débito.

El servicio recibe solicitudes de transacción, aplica reglas de negocio, se comunica
con un proveedor externo, persiste el resultado en MongoDB y permite consultar las
transacciones almacenadas mediante filtros y paginación.

La integración con el proveedor incluye mecanismos de resiliencia mediante
**timeouts, Retry y Circuit Breaker**, además del envío de una **API Key**
en las solicitudes realizadas al proveedor externo.

---

## Tabla de contenidos

- [Stack tecnológico](#stack-tecnológico)
- [Requisitos previos](#requisitos-previos)
- [Cómo levantar el proyecto](#cómo-levantar-el-proyecto)
- [Configuración](#configuración)
- [Base de datos](#base-de-datos)
- [Documentación de API con Swagger](#documentación-de-api-con-swagger)
- [Endpoints](#endpoints)
- [Colección de Postman](#colección-de-postman)
- [Reglas de negocio](#reglas-de-negocio)
- [Proveedor externo con WireMock](#proveedor-externo-con-wiremock)
- [Seguridad](#seguridad)
- [Resiliencia](#resiliencia)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Decisiones de diseño](#decisiones-de-diseño)
- [Manejo de errores](#manejo-de-errores)
- [Pruebas unitarias](#pruebas-unitarias)

---

## Stack tecnológico

- **Java 17**
- **Spring Boot 4.1.0**
- **Spring Web MVC**
- **Spring Data MongoDB**
- **MongoDB**
- **RestClient** para comunicación HTTP
- **Resilience4j** para Retry y Circuit Breaker
- **WireMock 3.13.2** para simular el proveedor externo
- **Swagger / OpenAPI** para documentación interactiva
- **Postman** para pruebas manuales
- **JUnit 5**
- **Mockito**
- **Maven**

---

## Requisitos previos

Antes de levantar el proyecto se requiere:

- Java 17 o superior
- Maven o Maven Wrapper incluido en el proyecto
- MongoDB
- WireMock standalone incluido en el proyecto
- Postman (opcional, para importar la colección de pruebas)

Los siguientes puertos deben estar disponibles:

| Servicio | Puerto |
|---|---:|
| Aplicación Spring Boot | `8080` |
| WireMock | `8081` |
| MongoDB | `27017` |

---

## Cómo levantar el proyecto

El sistema se compone de tres componentes principales que deben ejecutarse
simultáneamente:

1. MongoDB
2. WireMock
3. Aplicación Spring Boot

### 1. Levantar MongoDB

MongoDB debe estar disponible en:

```text
localhost:27017
```

En Windows, si MongoDB está instalado como servicio, puede iniciarse desde una
terminal ejecutada como administrador:

```bash
net start MongoDB
```

Para comprobar la conexión:

```bash
mongosh
```

La aplicación utiliza MongoDB para almacenar las transacciones procesadas.

No es necesario entregar una base de datos previamente creada.

MongoDB crea la base de datos y la colección cuando la aplicación realiza las
primeras operaciones de persistencia.

---

### 2. Levantar el proveedor externo con WireMock

El proyecto contiene el JAR standalone de WireMock junto con los mappings
utilizados para simular el proveedor externo.

Estructura:

```text
mock/
├── __files/
├── mappings/
│   ├── approved.json
│   └── failed.json
└── wiremock-standalone-3.13.2.jar
```

Desde la carpeta `mock`, ejecutar:

```bash
java -jar wiremock-standalone-3.13.2.jar --port 8081
```

WireMock quedará disponible en:

```text
http://localhost:8081
```

---

### 3. Levantar la aplicación

Desde la raíz del proyecto ejecutar:

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

También puede ejecutarse utilizando Maven:

```bash
mvn spring-boot:run
```

La API quedará disponible en:

```text
http://localhost:8080
```

---

## Configuración

La configuración principal se encuentra en:

```text
src/main/resources/application.properties
```

La aplicación requiere configurar:

- conexión a MongoDB;
- URL base del proveedor;
- API Key;
- parámetros de Retry;
- parámetros de Circuit Breaker.

La URL del proveedor local debe apuntar a WireMock:

```properties
provider.base-url=http://localhost:8081
```

La conexión local a MongoDB debe apuntar a:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/test
```

Los valores exactos de configuración pueden consultarse en el archivo
`application.properties` incluido en el proyecto.

> En un ambiente productivo, las API Keys y otros secretos no deberían
> almacenarse directamente en el código fuente. Deben utilizarse variables
> de entorno o un sistema especializado de gestión de secretos.

---

## Base de datos

La aplicación utiliza **MongoDB** para persistir las transacciones.

Por defecto, en el entorno local MongoDB se encuentra disponible en:

```text
mongodb://localhost:27017/test
```

La colección utilizada es:

```text
transactions
```

No es necesario importar previamente una base de datos.

Al ejecutar una transacción por primera vez, Spring Data MongoDB persiste
el documento y MongoDB crea la colección cuando sea necesario.

La entidad principal almacenada contiene información como:

- identificador de la transacción;
- `accountId`;
- tipo de transacción;
- monto;
- moneda;
- descripción;
- estado;
- identificador del proveedor;
- balance posterior;
- fecha de creación.

---

## Documentación de API con Swagger

La API cuenta con documentación interactiva mediante **Swagger / OpenAPI**.

Swagger permite:

- consultar los endpoints disponibles;
- visualizar los parámetros;
- consultar los modelos de datos;
- visualizar las posibles respuestas HTTP;
- proporcionar la API Key;
- ejecutar solicitudes directamente desde el navegador.

Una vez levantada la aplicación, Swagger UI se encuentra disponible en:

```text
http://localhost:8080/swagger-ui/index.html
```

La especificación OpenAPI puede consultarse en:

```text
http://localhost:8080/v3/api-docs
```

### Autenticación desde Swagger

Los endpoints de negocio están protegidos mediante el header:

```http
X-API-KEY
```

Swagger está configurado con un esquema de seguridad de tipo **API Key**.

Para ejecutar solicitudes desde Swagger UI:

1. Abrir Swagger UI.
2. Seleccionar **Authorize**.
3. Introducir la API Key configurada para el entorno.
4. Confirmar mediante **Authorize**.
5. Seleccionar el endpoint.
6. Presionar **Try it out**.
7. Introducir los parámetros necesarios.
8. Presionar **Execute**.

Swagger enviará automáticamente el header:

```http
X-API-KEY: <API_KEY>
```

Las rutas correspondientes a la documentación:

```text
/swagger-ui/**
/v3/api-docs/**
```

pueden consultarse sin API Key, mientras que los endpoints de negocio
continúan protegidos.

### Endpoints documentados

Swagger detecta y documenta los endpoints principales:

```text
POST /transactions
GET  /transactions
GET  /transactions/all
```

`GET /transactions` permite probar directamente los filtros opcionales:

- `accountId`
- `status`
- `type`
- `page`
- `limit`

### Respuestas HTTP documentadas

La documentación OpenAPI incluye tanto los escenarios exitosos como los
principales escenarios de error.

#### POST /transactions

| Código | Descripción |
|---|---|
| `201 Created` | Transacción ejecutada correctamente |
| `400 Bad Request` | Datos de entrada inválidos |
| `401 Unauthorized` | API Key inválida o ausente |
| `422 Unprocessable Content` | Transacción rechazada o regla de negocio no cumplida |
| `502 Bad Gateway` | Error técnico de comunicación con el proveedor, cuando aplique |

#### GET /transactions

| Código | Descripción |
|---|---|
| `200 OK` | Consulta realizada correctamente |
| `400 Bad Request` | Parámetros de consulta inválidos |
| `401 Unauthorized` | API Key inválida o ausente |

#### GET /transactions/all

| Código | Descripción |
|---|---|
| `200 OK` | Consulta realizada correctamente |
| `401 Unauthorized` | API Key inválida o ausente |

Los modelos utilizados por la API pueden visualizarse en la sección
**Schemas** de Swagger UI.

---

## Endpoints

### 1. Ejecutar transacción

```http
POST /transactions
```

Header requerido:

```http
X-API-KEY: <API_KEY>
```

Ejemplo de request:

```json
{
  "accountId": "userId1",
  "type": "CREDIT",
  "amount": 155.00,
  "currency": "MXN",
  "description": "Transferencia recibida"
}
```

Tipos soportados:

```text
CREDIT
DEBIT
```

Cuando el proveedor aprueba la operación, la transacción se almacena con:

```text
EXECUTED
```

Ejemplo de respuesta:

```json
{
  "id": "uuid-generado",
  "accountId": "userId1",
  "type": "CREDIT",
  "amount": 155.00,
  "currency": "MXN",
  "description": "Transferencia recibida",
  "status": "EXECUTED",
  "providerTransactionId": "txn-789",
  "balanceAfter": 5500.00,
  "createdAt": "2026-08-20T02:56:54Z"
}
```

Cuando la operación es rechazada por el proveedor o se produce un fallo
controlado de comunicación, la transacción se almacena con estado:

```text
REJECTED
```

---

### 2. Consultar transacciones

```http
GET /transactions
```

Header requerido:

```http
X-API-KEY: <API_KEY>
```

Permite consultar las transacciones almacenadas mediante filtros opcionales.

Parámetros disponibles:

| Parámetro | Descripción |
|---|---|
| `accountId` | Filtra por cuenta |
| `status` | `EXECUTED` o `REJECTED` |
| `type` | `CREDIT` o `DEBIT` |
| `page` | Número de página |
| `limit` | Cantidad de elementos por página |

Ejemplo:

```http
GET /transactions?status=EXECUTED&type=CREDIT&accountId=userId1&page=0&limit=20
```

Los filtros pueden utilizarse individualmente o combinarse.

Los valores de los enums deben enviarse sin comillas.

Correcto:

```text
status=EXECUTED
```

Incorrecto:

```text
status="EXECUTED"
```

Los resultados se ordenan de forma descendente utilizando la fecha de creación.

---

### 3. Consultar todas las transacciones

```http
GET /transactions/all
```

Header requerido:

```http
X-API-KEY: <API_KEY>
```

Este endpoint devuelve las transacciones almacenadas sin aplicar paginación.

---

## Colección de Postman

El proyecto incluye una colección de Postman para facilitar las pruebas manuales
de la API.

La colección se encuentra en:

```text
postmanCollection/
└── Transacciones.postman_collection.json
```

### Importar la colección

1. Abrir Postman.
2. Seleccionar **Import**.
3. Seleccionar el archivo:

```text
postmanCollection/Transacciones.postman_collection.json
```

4. Importar la colección `Transacciones`.

Antes de ejecutar las solicitudes deben estar levantados:

```text
MongoDB     -> localhost:27017
WireMock    -> localhost:8081
Aplicación  -> localhost:8080
```

### Requests incluidos

La colección contiene requests para diferentes escenarios de prueba.

#### Ejecutar transacción

Request:

```text
ejecutar transaccion
```

Realiza:

```http
POST http://localhost:8080/transactions
```

Permite ejecutar una transacción completa pasando por la API.

---

#### Consultar transacciones

Request:

```text
consultar transacciones
```

Realiza una consulta utilizando filtros.

Ejemplo:

```http
GET http://localhost:8080/transactions?status=EXECUTED&type=CREDIT&accountId=userId1
```

---

#### Circuit Breaker

Request:

```text
pruebaCircuitBreaker
```

Permite realizar solicitudes contra la aplicación para provocar fallos
del proveedor y observar el comportamiento del Circuit Breaker.

Cuando el número de fallos alcanza el umbral configurado, el Circuit Breaker
puede pasar al estado:

```text
OPEN
```

En este estado se evitan temporalmente nuevas llamadas al proveedor externo.

El cambio de comportamiento puede observarse en los logs de la aplicación.

---

#### Retry / proveedor WireMock

La colección también contiene el request:

```text
retries
```

que apunta al endpoint mock del proveedor:

```http
POST http://localhost:8081/provider/v1/execute
```

Este request permite comprobar directamente el comportamiento configurado
en WireMock.

Para comprobar los reintentos ejecutados por **Resilience4j**, la solicitud
debe realizarse a través de la aplicación:

```http
POST http://localhost:8080/transactions
```

De esta forma, el flujo pasa por el cliente HTTP de la aplicación y
posteriormente por WireMock.

Los diferentes intentos pueden observarse en los logs de la aplicación.

---

## Reglas de negocio

Las reglas se validan antes de realizar la llamada al proveedor externo.

### Monto mínimo

Toda transacción debe tener un monto mayor a:

```text
$1.00
```

Una cantidad igual o menor produce una violación de regla de negocio.

---

### Límite para DEBIT

Una transacción:

```text
DEBIT
```

no puede superar:

```text
$10,000.00
```

Las transacciones `CREDIT` no utilizan este límite.

---

### Moneda

La única moneda soportada actualmente es:

```text
MXN
```

Cualquier otra moneda genera una violación de regla de negocio antes de
contactar al proveedor.

---

## Proveedor externo con WireMock

La aplicación se comunica con un proveedor externo mediante:

```http
POST /provider/v1/execute
```

Para ejecutar el proyecto localmente, este proveedor se simula mediante WireMock.

El flujo principal es:

```text
TransactionController
        ↓
TransactionService
        ↓
PaymentProviderClient
        ↓
HttpPaymentProviderClient
        ↓
RestClient
        ↓
WireMock :8081
```

### Transacción aprobada

Cuando el proveedor aprueba la operación, devuelve información como:

- identificador de la transacción;
- estado;
- balance actualizado.

La respuesta del proveedor se transforma internamente en un `ProviderResult`.

Posteriormente la transacción se almacena con estado:

```text
EXECUTED
```

---

### Transacción rechazada

Cuando WireMock simula un rechazo, el cliente interpreta la respuesta del proveedor.

Si el proveedor responde con:

```text
REJECTED
```

la aplicación genera un `ProviderResult` rechazado y la transacción se persiste
con estado:

```text
REJECTED
```

---

## Seguridad

La API utiliza una **API Key** como mecanismo de autenticación para proteger
los endpoints.

El cliente debe enviar:

```http
X-API-KEY: <API_KEY>
```

Si el header no está presente o la API Key no coincide con la configurada,
la aplicación devuelve:

```text
401 Unauthorized
```

Ejemplo:

```json
{
  "code": "UNAUTHORIZED",
  "message": "API Key inválida o ausente"
}
```

### Comunicación con el proveedor

La comunicación entre la aplicación y el proveedor externo también incluye
el header:

```http
X-API-KEY
```

La aplicación envía la API Key al realizar solicitudes al proveedor.

Este mecanismo permite simular autenticación entre servicios durante la
ejecución local del challenge.

> En un entorno productivo, los secretos no deberían almacenarse directamente
> en el código fuente. Se recomienda utilizar variables de entorno o un
> sistema de gestión de secretos.

---

## Resiliencia

La comunicación con el proveedor externo incorpora mecanismos de resiliencia
para evitar que los fallos del proveedor afecten indefinidamente a la aplicación.

Se implementaron:

- **Timeout de conexión**
- **Timeout de lectura**
- **Retry**
- **Circuit Breaker**

### Timeouts

El cliente HTTP utiliza límites de tiempo para evitar quedar bloqueado
indefinidamente cuando el proveedor no responde.

La configuración se encuentra en:

```text
RestClientConfig
```

El timeout de conexión limita el tiempo disponible para establecer comunicación
con el proveedor.

El timeout de lectura limita el tiempo máximo de espera por la respuesta.

---

### Retry

La integración utiliza **Resilience4j Retry**.

Cuando se produce un fallo técnico de comunicación, el cliente puede volver a
intentar la operación de acuerdo con la configuración definida.

Los intentos pueden observarse en los logs de la aplicación.

El Retry se aplica sobre la comunicación con el proveedor y no sobre las reglas
de negocio de la transacción.

---

### Circuit Breaker

La integración también utiliza **Resilience4j Circuit Breaker**.

Su objetivo es evitar realizar llamadas continuamente a un proveedor que está
presentando fallos.

El Circuit Breaker puede encontrarse principalmente en los siguientes estados:

```text
CLOSED
   ↓
OPEN
   ↓
HALF_OPEN
   ↓
CLOSED
```

- **CLOSED:** las llamadas se ejecutan normalmente.
- **OPEN:** las llamadas al proveedor se bloquean temporalmente.
- **HALF_OPEN:** se permiten llamadas de prueba para comprobar si el proveedor se recuperó.
- **CLOSED nuevamente:** si las llamadas de prueba son exitosas, se restablece el funcionamiento normal.

Cuando una llamada es bloqueada por el Circuit Breaker, Resilience4j genera:

```text
CallNotPermittedException
```

La aplicación controla este escenario y evita que una indisponibilidad repetida
del proveedor provoque llamadas innecesarias.

---

## Estructura del proyecto

```text
Transactions/
├── mock/
│   ├── __files/
│   ├── mappings/
│   │   ├── approved.json
│   │   └── failed.json
│   └── wiremock-standalone-3.13.2.jar
│
├── postmanCollection/
│   └── Transacciones.postman_collection.json
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.financial.transactions/
│   │   │       ├── client/
│   │   │       │   ├── HttpPaymentProviderClient.java
│   │   │       │   └── PaymentProviderClient.java
│   │   │       │
│   │   │       ├── config/
│   │   │       │   ├── ApiKeyFilter.java
│   │   │       │   └── RestClientConfig.java
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   └── TransactionController.java
│   │   │       │
│   │   │       ├── dto/
│   │   │       │   ├── ProviderRequest.java
│   │   │       │   ├── ProviderResponse.java
│   │   │       │   ├── ProviderResult.java
│   │   │       │   ├── TransactionRequest.java
│   │   │       │   └── TransactionResponse.java
│   │   │       │
│   │   │       ├── exceptions/
│   │   │       │   ├── BusinessRuleException.java
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   └── ProviderException.java
│   │   │       │
│   │   │       ├── model/
│   │   │       │   ├── Transaction.java
│   │   │       │   ├── TransactionStatus.java
│   │   │       │   └── TransactionType.java
│   │   │       │
│   │   │       ├── repository/
│   │   │       │   └── TransactionRepository.java
│   │   │       │
│   │   │       ├── service/
│   │   │       │   └── TransactionService.java
│   │   │       │
│   │   │       └── TransactionsApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/
│           └── com.financial.transactions/
│               ├── controller/
│               │   └── TransactionControllerTest.java
│               ├── service/
│               │   └── TransactionServiceTest.java
│               └── TransactionsApplicationTests.java
│
├── pom.xml
├── mvnw
└── mvnw.cmd
```

### Responsabilidades

- **client:** comunicación con el proveedor externo.
- **config:** configuración del cliente HTTP, seguridad y componentes relacionados.
- **controller:** exposición de endpoints REST.
- **dto:** contratos de entrada, salida y comunicación con el proveedor.
- **exceptions:** excepciones y manejo global de errores.
- **model:** modelo de dominio.
- **repository:** acceso a MongoDB.
- **service:** reglas de negocio y orquestación.
- **mock:** configuración utilizada para simular el proveedor externo.
- **postmanCollection:** requests preparados para probar la aplicación.

---

## Decisiones de diseño

### Persistencia: MongoDB

Se eligió MongoDB porque una transacción puede representarse naturalmente como
un documento autocontenido y no requiere relaciones complejas entre diferentes
entidades.

Este modelo permite almacenar cada operación junto con sus datos principales,
estado e información proporcionada por el proveedor.

Además, MongoDB permite escalar horizontalmente, característica relevante para
sistemas que pueden manejar grandes volúmenes de transacciones.

---

### Índices

La entidad utiliza índices sobre campos utilizados frecuentemente durante las
consultas.

Entre ellos se encuentran:

```text
id
accountId
status
```

Esto permite mejorar las búsquedas realizadas mediante los filtros disponibles.

No se agregó un índice individual sobre `transactionType` debido a su baja
cardinalidad, ya que actualmente únicamente contiene:

```text
CREDIT
DEBIT
```

---

### Manejo de dinero con BigDecimal

Los montos monetarios se representan mediante `BigDecimal`.

Por ejemplo:

```java
BigDecimal amount;
```

Se evita utilizar `double` para valores monetarios debido a los posibles errores
de precisión asociados a la representación de números en punto flotante.

---

### Desacoplamiento del proveedor externo

La comunicación con el proveedor se define mediante la interfaz:

```text
PaymentProviderClient
```

y su implementación:

```text
HttpPaymentProviderClient
```

De esta manera, `TransactionService` no necesita conocer los detalles de
comunicación HTTP.

Esto permite:

- reducir el acoplamiento;
- facilitar las pruebas unitarias;
- sustituir la implementación del proveedor;
- mantener la lógica de negocio independiente de la infraestructura HTTP.

La respuesta externa `ProviderResponse` se transforma en `ProviderResult`
antes de ser utilizada por el servicio.

---

### Separación de capas

El proyecto mantiene separadas las principales responsabilidades:

```text
Controller
    ↓
Service
    ↓
Client / Repository
    ↓
Proveedor externo / MongoDB
```

El controller gestiona las solicitudes HTTP.

El service contiene las reglas de negocio y coordina la operación.

El repository gestiona la persistencia.

El client encapsula la comunicación con el proveedor externo.

---

### Persistencia de transacciones fallidas

Cuando ocurre un fallo controlado durante la comunicación con el proveedor,
la aplicación construye una transacción con estado:

```text
REJECTED
```

y la persiste en MongoDB.

Esto permite conservar un registro de la operación solicitada incluso cuando
no pudo ejecutarse correctamente contra el proveedor.

---

## Manejo de errores

La aplicación utiliza un `@RestControllerAdvice` para centralizar el manejo
de excepciones.

### Errores de validación

Las validaciones de entrada utilizan Jakarta Validation.

Ejemplos:

```text
@NotBlank
@NotNull
@DecimalMin
```

Cuando el request no cumple con el contrato esperado se devuelve:

```text
400 Bad Request
```

con código:

```text
VALIDATION_ERROR
```

Ejemplo:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Datos de entrada inválidos"
}
```

---

### API Key inválida o ausente

Cuando un endpoint protegido recibe una API Key incorrecta o no recibe
el header correspondiente, se devuelve:

```text
401 Unauthorized
```

Ejemplo:

```json
{
  "code": "UNAUTHORIZED",
  "message": "API Key inválida o ausente"
}
```

---

### Reglas de negocio

Cuando se incumple una regla de negocio se lanza:

```text
BusinessRuleException
```

y se devuelve:

```text
422 Unprocessable Content
```

con código:

```text
BUSINESS_RULE_VIOLATION
```

Ejemplo:

```json
{
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "Solo se aceptan transacciones en MXN"
}
```

---

### Errores del proveedor

Los errores técnicos de comunicación con el proveedor se encapsulan mediante:

```text
ProviderException
```

Esto permite mantener aislados los errores de infraestructura respecto de la
lógica principal de negocio.

Cuando corresponde que la excepción llegue al manejador global, se traduce en:

```text
502 Bad Gateway
```

con código:

```text
PROVIDER_ERROR
```

---

## Pruebas unitarias

El proyecto contiene pruebas automatizadas organizadas por componente.

La estructura de pruebas es:

```text
src/test/java/com.financial.transactions/
├── controller/
│   └── TransactionControllerTest.java
├── service/
│   └── TransactionServiceTest.java
└── TransactionsApplicationTests.java
```

### TransactionServiceTest

Las pruebas del service permiten validar la lógica de negocio de forma aislada.

Se utilizan mocks para dependencias como:

```text
PaymentProviderClient
TransactionRepository
MongoTemplate
```

Esto permite probar el comportamiento del servicio sin necesidad de levantar
MongoDB ni WireMock.

Entre los escenarios que pueden validarse se encuentran:

- ejecución correcta de una transacción;
- respuesta rechazada por el proveedor;
- errores de comunicación con el proveedor;
- validación del monto mínimo;
- validación del límite máximo para `DEBIT`;
- validación de moneda;
- persistencia de la transacción;
- consulta y paginación de transacciones.

### TransactionControllerTest

Las pruebas del controller validan el comportamiento de los endpoints HTTP
sin depender de la implementación real del service.

Entre los escenarios se encuentran:

- creación exitosa de una transacción;
- respuesta de transacción rechazada;
- validación de datos de entrada;
- consulta de transacciones;
- manejo de códigos HTTP.

### TransactionsApplicationTests

La prueba de contexto verifica que el contexto principal de Spring Boot
pueda cargarse correctamente.

### Ejecutar las pruebas

Desde la raíz del proyecto:

```bash
./mvnw test
```

En Windows:

```bash
mvnw.cmd test
```

También puede utilizarse Maven directamente:

```bash
mvn test
```

Las pruebas unitarias utilizan mocks para mantenerlas independientes de
servicios externos, permitiendo ejecutarlas sin depender del proveedor WireMock
ni de una base de datos real.
