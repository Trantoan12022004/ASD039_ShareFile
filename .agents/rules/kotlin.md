---
trigger: always_on
---

---
description: Recommended Kotlin coding style
globs:
- "**/*.kt"
alwaysApply: true
---

Write Kotlin code following official Kotlin and Android best practices.

Requirements:

- Follow official Kotlin coding conventions and Android best practices.
- Prioritize readability, maintainability, and idiomatic Kotlin.
- Use expression bodies when they improve readability.
- Use smart casts and null-safety features appropriately.
- Prefer `when` over multiple chained `if/else` statements when appropriate.
- Use scope functions (`let`, `apply`, `run`, `also`, `with`) only when they improve readability.
- Avoid deeply nested scope functions.
- Prefer immutable variables (`val`) by default.
- Use `var` only when reassignment is necessary.
- Use meaningful and descriptive variable and function names.
- Keep functions small and focused on a single responsibility.
- Avoid unnecessary temporary variables.
- Avoid overly complex one-line expressions.
- Prefer clear and idiomatic Kotlin over Java-style Kotlin.
- Use early returns when they improve readability and reduce nesting.
- Follow Kotlin null-safety practices and avoid unnecessary `!!`.
- Do not use `hashCode()` on a String unless explicitly required.
- Add comments only when they explain non-obvious logic.
- Do not comment obvious code.
- Optimize for readability, maintainability, and idiomatic Kotlin.