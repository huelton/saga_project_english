# Commit Strategy — SAGA Project

This document describes the commit strategy for the repository, **starting from scratch**. New commits must be **added on demand**, always keeping **chronological development order** at the end of the commits file (`commits.json`).

## Objective

- Commits in logical batches (~3h of work each).
- Interval between pushes: **2 hours** + random variation of **10 to 30 minutes** (jitter).
- Reference time: **local machine time**.
- No co-author in commits.
- Reproducible in other projects.

## Script Usage

- **Manual mode:** run the script to push only the next commit.
- **Scheduled mode:** the script schedules the next push in 2h + jitter and repeats until the list is finished.
- The script reads the last pushed commit (state in `.strategy_state.json`) and **never re-pushes** already pushed commits.

Data file: **`commits.json`** at the root (list of `message` and `files` per commit).

## Rules

1. **One commit at a time:** the script always pushes the next commit in the list.
2. **Local time:** “next execution” is calculated using local time.
3. **Jitter:** interval between pushes = 2h + random (10–30 min).
4. **No co-author:** commits with `git commit -m "..."` only.
5. **Persistent state:** progress in `.strategy_state.json` (not under version control).
6. **Chronological order:** when adding new commits, append them **at the end** of `commits.json` to reflect real development order.

## Syncing with Existing History

If the repository already has commits and you want to continue from the list:

1. Open `.strategy_state.json` (create at root if it does not exist).
2. Set `last_pushed_index` to the **index (0-based) of the last commit already pushed** in list order.
3. Save and run the script; the next push will be the following item.

## Chronological Order of Commits (Development from Scratch)

The list in **`commits.json`** follows chronological development order. Summary:

1. **Initial setup and documentation** — repository, README, architecture and requirements docs.
2. **Infrastructure** — docker-compose (data, admin, then observability).
3. **Shared modules** — saga-common, kafka-common, circuit-breaker (as in ARCHITECTURE.md).
4. **Observability** — Prometheus, Loki, Grafana in docker-compose; configs; OBSERVABILITY.md.
5. **Load tests** — JMeter documentation and sample test plan (JMETER.md, JMX).
6. **Saga Orchestrator** — module, saga-common usage, API, Kafka, tests.
7. **Microservices** — account, validation, currency, transaction, notification, audit (each with structure, Kafka, tests).
8. **Documentation and scripts** — TESTING.md, commit strategy, scheduler script, service READMEs.

**New commits:** when evolving the project, add new entries **at the end** of `commits.json` and, if needed, update this section with the new phase summary, keeping chronological order.
