---
name: naming-conventions
description: Reglas de nombramiento de identificadores basadas en Clean Code (Robert C. Martin) — aplican a todo el proyecto de reconstrucción
metadata:
  type: reference
---

# Convenciones de Nombramiento

Basadas en Clean Code (Robert C. Martin). Aplican a todo código producido en los
features de reconstrucción y sirven como vara de medición en el diagnóstico.

## Principios generales

1. **Revelar intención**: el nombre responde por qué existe, qué hace y cómo se usa — sin necesidad de comentario adicional.
2. **Sin desinformación**: el nombre no debe sugerir algo distinto a lo que representa.
3. **Distinciones significativas**: si dos cosas son distintas, sus nombres deben expresarlo — sin ruido (`data`, `info`, `the`, `obj`).
4. **Pronunciable**: si no se puede pronunciar, no se puede discutir en equipo.
5. **Buscable**: evitar nombres de una letra o literales numéricos no buscables.
6. **Sin encodings**: prohibido notación húngara, prefijos de tipo o de miembro (`m_`, `_`, `str`, `i`).
7. **Una palabra por concepto**: no mezclar `get`/`fetch`/`retrieve` para el mismo propósito en el mismo codebase.
8. **Sin juegos de palabras**: una palabra, un solo significado en todo el codebase.

## Clases

- DEBE: sustantivos o frases nominales.
- PROHIBIDO: sufijos vagos — `Manager`, `Processor`, `Data`, `Info`, `Helper`, `Support`, `Handler`, `Util`.
- PROHIBIDO: sufijo `Impl` — el nombre debe expresar qué hace, no que es una implementación.
- PROHIBIDO: verbos como nombre de clase.
- PROHIBIDO: sufijo `Dto` — nombrar por el concepto que transporta.
- PROHIBIDO: sufijo `Inner` — indica clase generada automáticamente, no un concepto de negocio.
- PROHIBIDO: sufijos que revelan posición estructural en lugar de significado (`Inner`, `Outer`, `Base`, `Abstract` como sufijo visible al consumidor).
- Interfaces: sin prefijo `I` — el nombre debe ser el contrato puro.

## Métodos

- DEBE: verbos o frases verbales.
- Accesores: prefijo `get`/`set`.
- Predicados: prefijo `is`/`has`/`can` — nunca `flag`, `check`, `verify` para retornos booleanos.
- PROHIBIDO: verbos genéricos sin contexto como nombre completo del método (`process`, `handle`, `manage`, `execute` solos).

## Variables y parámetros

- DEBE: revelar intención.
- Una letra solo en índices de bucle (`i`, `j`, `k`) — nunca en variables de negocio.
- PROHIBIDO: sufijos de ruido (`obj`, `data`, `info`).
- PROHIBIDO: abreviaciones que requieran traducción mental.
- Booleanos: siempre con prefijo `is`/`has`/`can`.

## Constantes

- DEBE: `SCREAMING_SNAKE_CASE`.
- DEBE: el nombre expresa el concepto, no el valor (una constante cuyo nombre repite su valor es tautológica).
- DEBE: una sola definición — nunca duplicadas entre clases.

## Enums

- Nombre del tipo: sustantivo singular en PascalCase.
- Valores: `SCREAMING_SNAKE_CASE`, sustantivos o adjetivos.
- PROHIBIDO: prefijo del tipo repetido en cada valor.

## Tests

- DEBE: patrón `methodName_condition_expectedResult` o `should_expectedBehavior_when_condition`.
- PROHIBIDO: números mágicos — usar constantes con nombre.

## Paquetes

- DEBE: minúsculas, sustantivos singulares.
- DEBE: reflejar la capa o responsabilidad del artefacto que contienen.

## Abreviaciones

- PROHIBIDO: abreviar palabras del dominio o del lenguaje — si requiere traducción mental, no es válido.
- PROHIBIDO: abreviaciones de palabras en español dentro de código en inglés.
- EXCEPCIÓN: abreviaciones universalmente conocidas en la industria (`id`, `url`, `http`, `json`, `api`).

## Siglas y acrónimos

- PROHIBIDO: siglas formadas por iniciales de palabras del dominio de negocio — el lector nuevo no puede deducir su significado.
- PROHIBIDO: siglas de conceptos internos de la organización que no forman parte del lenguaje técnico universal.
- EXCEPCIÓN: siglas que son el nombre oficial e irremplazable del concepto de negocio (nombre legal o regulatorio), siempre que estén definidas en el glosario del proyecto.
- Cuando una sigla es excepción: se integra en PascalCase — nunca en mayúsculas totales dentro de un identificador (`ItfRate`, no `ITFRate`).

## Sufijos estructurales o generados

- PROHIBIDO: sufijo `Inner` — indica clase anidada generada, no un concepto de negocio.
- PROHIBIDO: sufijo `Id` cuando la clase representa una clave compuesta con identidad propia — nombrar por el concepto que identifica.
- PROHIBIDO: sufijos `Cod`, `Txn`, `Opn` y similares — son abreviaciones de dominio (ver sección Abreviaciones).
