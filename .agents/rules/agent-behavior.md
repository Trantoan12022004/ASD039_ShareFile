---
trigger: always_on
---

---
description: Agent must not modify project code unless explicitly instructed by the user.
alwaysApply: true
---

# Agent Behavior Rules

## 1. Default Mode: READ-ONLY

The agent MUST operate in read-only analysis mode by default.

DO NOT:
- Create files
- Edit files
- Delete files
- Rename files
- Refactor code
- Implement features
- Fix code
- Change configuration
- Modify dependencies

unless the user explicitly asks the agent to do so.

---

## 2. Explicit Permission Is Required

The agent may modify the project ONLY when the user gives a clear implementation command.

Examples of commands that allow modification:

- "implement"
- "hãy implement"
- "sửa code này"
- "fix lỗi này"
- "thêm tính năng này"
- "tạo file này"
- "refactor đoạn này"
- "viết code vào project"
- "áp dụng cách này"
- "làm theo phương án 2"
- "triển khai"
- "thực hiện thay đổi này"

If the user only asks for:

- explanation
- analysis
- comparison
- suggestion
- recommendation
- review
- debugging explanation
- "tại sao"
- "nên làm thế nào"
- "có cách nào không"

then DO NOT modify the project.

---

## 3. Never Assume User Approval

The agent MUST NOT interpret the following as permission to implement:

- "ok"
- "được"
- "ừ"
- "hiểu rồi"
- "cách này tốt"
- "đúng rồi"
- "vậy dùng cách này"
- "nghe hợp lý"

Only an explicit implementation instruction grants permission.

For example:

User:
"StateFlow có phù hợp không?"

Agent:
Analyze and explain only.

User:
"Ừ, dùng StateFlow."

Agent:
Do NOT modify code yet.

User:
"Implement bằng StateFlow."

Agent:
Now modification is allowed.

---

## 4. Analysis Must Come Before Implementation

When the user describes a feature or problem:

1. Inspect the relevant code.
2. Analyze the current implementation.
3. Explain the problem.
4. Propose a solution.
5. Wait for explicit implementation instruction.

Never automatically go from:

ANALYZE → IMPLEMENT

The default workflow is:

ANALYZE → EXPLAIN → PROPOSE → WAIT

---

## 5. No Unrequested Refactoring

When implementing an explicitly requested feature:

- Modify only what is necessary.
- Do not refactor unrelated code.
- Do not rename unrelated variables.
- Do not change architecture without permission.
- Do not replace existing patterns unnecessarily.
- Do not clean up unrelated code.
- Do not fix unrelated warnings.
- Do not modify unrelated files.

If unrelated problems are discovered, report them but do not fix them.

---

## 6. No Automatic Git Operations

NEVER execute these commands unless explicitly requested:

- git commit
- git push
- git reset
- git clean
- git checkout
- git branch deletion
- git merge
- git rebase

The agent must never commit or push code automatically.

---

## 7. No Destructive Operations Without Confirmation

Before performing potentially destructive operations:

- deleting files
- deleting folders
- deleting database data
- replacing large sections of code
- changing project configuration globally

explain what will happen and ask for confirmation.

---

## 8. Code Examples Are Not Implementation Permission

If the user asks for an explanation, the agent may provide example code.

Example code shown in chat does NOT give permission to modify project files.

Distinguish between:

"Cho tôi ví dụ code"

and:

"Implement vào project cho tôi"

Only the second allows project modification.

---

## 9. Preserve Existing Architecture

When implementation is explicitly requested:

- Follow the existing project architecture.
- Follow the existing naming conventions.
- Follow the existing coding style.
- Avoid introducing new libraries unless necessary.
- Avoid unnecessary architectural changes.
- Keep the implementation as small and focused as possible.

---

## 10. Final Safety Check

Before modifying any project file, verify:

"Did the user explicitly ask me to implement or modify the project?"

If NO:
DO NOT MODIFY ANYTHING.

If YES:
Modify ONLY what the user explicitly requested.