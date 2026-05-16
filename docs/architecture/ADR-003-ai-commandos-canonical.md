# ADR-003: CommandOS is the canonical in-app AI path

**Status:** Accepted  
**Date:** 2026-05-16  
**Sacred impact:** None

## Context

Three AI stacks coexist: CommandOS (`SuperCommandService`), legacy `AIKhataAssistant`, and unused `domain.ai.AICommandRouter` (depends on `TransactionViewModel`).

## Decision

1. **Ship path:** `AIChatScreen` → `SuperCommandService` → `DomainAdapter` → repositories.
2. **Legacy fallback:** `AIKhataAssistant` only when Super Command returns unhandled.
3. **Do not wire** `domain.ai.AICommandRouter` to new UI without removing `TransactionViewModel` dependency.
4. Ledger mutations from AI must go through `PolicyGuard` + user confirmation for HIGH-risk intents.

## Future work

- Extract commands from `AICommandRouter` into repository-level operations, then deprecate the class.
