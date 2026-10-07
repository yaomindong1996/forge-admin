---
name: forge-project-init
description: Bootstrap or maintain projects generated from Forge, initialize a clean template database, and install, upgrade or remove Forge source plugins in template or generated Admin projects. Use for forge:create presets, package/artifact/database renaming, local configuration, init-db.sh/clean-db.sh maintenance, and forge:plugin delivery. Not for Codex app plugins or commercial license implementation.
---

# Forge Project Init

## Purpose

Turn this repository into a clean, independently developable project:

- **Code**: renamed packages, artifacts and frontends, with only the selected modules.
- **Project context**: `AGENTS.md`, `.agents/skills`, and `code-copilot` rules, memory and knowledge are kept. `code-copilot/changes` starts empty, with no history and no unarchived changes.
- **Database**: only the default tenant, the super admin (with its role and root org), menus/permissions, dictionaries, configs, regions, job configs, built-in templates and the Flyway history table are kept.

Details of the database rules: `code-copilot/changes/db-clean-template-init/spec.md` in the template repo.

## Workflow

### 1. Generate code (`forge:create`)

Run from the template repo root:

```bash
pnpm forge:create -- ../<project-dir> \
  --preset full \
  --base-package com.company.project \
  --display-name 系统中文名 \
  [--java-name ProjectName] [--database-name project_db] [--include business-core]
```

- Presets: `minimal-admin`, `data-app`, `ai-report`, `full`. See `scripts/forge-create/module-catalog.json`.
- `full` includes admin UI, report UI, H5 (`<project>-h5-ui`) and Docker deploy (`docker-<project>`). Other presets can add them with `--include h5-ui,docker`; H5 pulls in app-server + flow-server, Docker pulls in admin-server + flow-server + admin UI.
- Renames:
  - `com.mdframe.forge` becomes the new package / groupId.
  - `forge-*` artifacts and directories get the new prefix.
  - `forge_admin` becomes the new database name.
  - `forge_schema_history` becomes `<snake>_schema_history`, consistently in `application.yml`, the full SQL, the Flyway runner and `clean-db.sh`.
  - H5: `/forge-h5` and `/forge-h5-api` become `/<project>-h5` and `/<project>-h5-api`; the client ID `forge_h5` becomes `<snake>_h5`, in the `.env` files and in `sys_client` in the full SQL.
  - Docker:
    - `nginx.conf` and `Dockerfile.ui` are rewritten to the generated admin public path and API prefix (defaults `/` and `/api`);
    - service, container and network names use the project name;
    - `init-sql/01-init.sql` is overwritten with the generated full SQL.
- Not copied:
  - `.git`, `node_modules`, `target`, `logs`;
  - `application-dev.yml`, `.env.local`, `docker-forge-admin/.env`;
  - `code-copilot/changes`;
  - `db/backup` (old migrations already folded into the full SQL) and `db/community-export`.
- Default: ask for the module preset, base package, display name and database name if the user has not given them. Do not guess.

### 2. Local config (in the generated project)

```bash
cp <prefix>-server/<prefix>-admin-server/src/main/resources/application-dev.example.yml \
   <prefix>-server/<prefix>-admin-server/src/main/resources/application-dev.yml
cp <prefix>-admin-ui/.env.example <prefix>-admin-ui/.env.local
```

- Fill in the MySQL and Redis settings.
- If the flow server is included, copy its example config the same way. It uses the same database.
- Never commit real passwords.

### 3. Clean database

Needs a mysql client, Maven and MySQL 8. Run from `<prefix>-server`:

```bash
MYSQL_PWD=*** bash scripts/db/init-db.sh --database <db> --recreate --clean
```

This runs: recreate the database → full SQL → required seed → Flyway migrations → `clean-db.sh`.

Without Maven:

1. Run `init-db.sh --database <db> --recreate`.
2. Start the admin server once so Flyway migrates.
3. Run `clean-db.sh --database <db>` to preview.
4. Add `--execute` to clean.

With Docker:

1. In `docker-<project>`, run `cp .env.example .env && docker compose up -d`. MySQL imports the full SQL; admin runs Flyway.
2. Once admin is up, run `MYSQL_PWD=*** bash <prefix>-server/scripts/db/clean-db.sh --host 127.0.0.1 --database <db> --execute`.
3. Run `docker compose restart <module-prefix>-admin` so caches reload.

### 4. Verify

- `mvn -pl <prefix>-admin-server -am compile -DskipTests`, and `pnpm install && pnpm build` in the admin UI (and in the H5 UI if generated).
- The database has one row each in `sys_user`, `sys_tenant` and `sys_role`. `ACT_RU_TASK` is empty. `sys_job_config` is not empty.
- Start admin: Flyway reports the schema is up to date. `admin` / `123456` logs in, no low-code app menus appear, and a file upload works with local storage.
- `ls -a code-copilot/changes` shows only `.gitkeep`.
- Then run `git init` in the new project and make the first commit.

### Using the template repo directly (no rename)

Run only step 3 against a new database. Keep the template repo's own `code-copilot/changes` untouched unless the user explicitly asks to delete them there.

### 5. Optional source plugins

Only when plugins are requested, read [references/plugins.md](references/plugins.md) before installation,
upgrade or removal. It also applies to an existing generated project without repeating database initialization.

- Confirm the source package, target project and requested operation; do not infer permission to upgrade,
  deploy, grant roles or change a database from a request to inspect a plugin.
- Finish baseline initialization/cleanup before installing plugins. Later cleanup may remove plugin data;
  preview it separately and preserve both main and plugin migration histories.
- Generated projects carry `forge.config.json`, the root `forge:plugin` command and its runtime tools.
  They do not carry samples or template-only `check:edition`; installed customer plugins are committable.
- Use the existing CLI rather than editing POM/config records by hand. `--force` replaces the entire package,
  never merges customizations or bypasses dirty-file checks. Uninstall does not delete database objects.
- For authoring new delivery packages, use the original template's `plugins-samples/README.md`, not renamed
  host source as the package format. Plugin authoring guidance is not copied into customer projects.

## Rules

- **Order**: cleanup must run after all migrations. V1.0.105, V1.0.107, V1.0.114 and V1.0.158 insert demo data. `clean-db.sh` refuses to run while migrations are pending.
- **Low-code menus**:
  - Seed deletion from `/ai/crud-page/%` paths, `ai:business:application:%` perms and `ai_crud_config.menu_resource_id`.
  - Do this before truncating `ai_crud_config`.
  - Never seed from `ai_lowcode_domain.menu_parent_id`; it points at system directories.
- **Code-backed tables**: tables referenced by `@TableName` or Mapper XML (`sample_purchase_order`, `biz_leave_request`, `sys_employee`, `worker_node` …) must be truncated, never dropped.
- **Flowable**: keep `ACT_GE_PROPERTY`, `ACT_ID_PROPERTY` and the Flowable liquibase tables; truncate the other `ACT_*` / `FLW_*` tables.
- **Project-specific tweaks**: use `--extra-sql FILE`, `--keep-table`, `--drop-table` or `--keep-business-tables`. Do not fork the scripts per project.
- **Passwords**: pass them via `MYSQL_PWD` or `--password`. The scripts forward them with a temporary `--defaults-extra-file`, never on the mysql command line.
- **Destructive scope**: `clean-db.sh` physically deletes data. Use it only on template or dev databases, and offer `--backup-file` first.
- **bash 3.2 compatibility** (macOS `/bin/bash`):
  - no associative arrays, no `mapfile`, no `${var,,}`;
  - guard empty arrays under `set -u`;
  - write `${VAR}` when a variable is directly followed by non-ASCII text.

## Maintenance

- **New framework table holding runtime or test data**: add it to `TRUNCATE_REGEXES` in `clean-db.sh`.
- **New built-in template table**: add it to `KEEP_EXACT_REGEX`, or give it a selective rule in section 5 of the plan.
- **New migration that seeds demo data**: make sure cleanup still removes it, and extend `clean-db.test.mjs`.
- **New template-only directory** (history, exports, local artifacts): add it to `ignoredTemplatePathPrefixes` in `scripts/forge-create/create-project.mjs`.
- **Order of text replacements matters**: specific prefixes (`docker-forge-admin`, `/forge-h5`) must come before generic ones (`forge-admin/`, `VITE_PUBLIC_PATH=/forge`).
- **Template's own `docker-forge-admin/init-sql/01-init.sql`**: keep it byte-identical to `forge-server/db/全量初始化SQL.sql`.
- **After any change**, run both checks:
  - `cd forge-server/scripts/db && node --test init-db.test.mjs clean-db.test.mjs`
  - generate a throwaway project with `forge:create --preset full` and run the same tests inside it.
