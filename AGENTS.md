# Boox E-Ink Draw - Agent Handoff

This document is for future agents working on this project.
It captures intent, architecture, known pitfalls, and guardrails so work can continue without repeating regressions.

## TL;DR (Read This First)

- Build target: ultra-fast Boox e-ink drawing app where latency and stability matter more than feature completeness.
- **Core Strategy**: We use the native Onyx hardware render for zero-latency drawing, with a background coordinate ingestion pipeline that reconstructs the permanent stroke on our `DrawingView` canvas.

## 1) What We Are Building

- Android drawing app for Onyx Boox (e-ink tablet).
- Priority order:
  1. Lowest possible pen latency.
  2. Stable behavior (no crashes, no invisible strokes, no erratic replay).
  3. Realism/Texture (e.g., true pencil grain).
- Canvas is portrait `930 x 1240`.
- Supports zooming/panning via standard Android gestures.

## 2) The "Stippled" Pencil Brush (Crucial Find)


