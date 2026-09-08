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

## Android SDK Version Rules

- Before adding any `Build.VERSION.SDK_INT` check, always verify the project's `minSdk`.
- Never write an SDK version condition that is impossible based on `minSdk`.

Examples:
- If `minSdk >= 29`, do NOT write:
  `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)`
  because the condition is always true.
- If `minSdk >= 29`, do NOT write checks such as:
  `Build.VERSION.SDK_INT < 29`
  because they are always false.

- Remove unnecessary compatibility branches for Android versions below `minSdk`.
- Only use `Build.VERSION.SDK_INT` checks when the API level condition can actually vary on supported devices.

## Code Cleanliness Rules

- Do not use redundant qualifier names.
- If a class, object, or member can be referenced directly without ambiguity, prefer the shorter direct reference.
- Avoid unnecessary fully-qualified names such as:
  `android.os.Build.VERSION.SDK_INT`
  when `Build.VERSION.SDK_INT` is sufficient.
- Remove redundant imports, qualifiers, conditions, and compatibility code.