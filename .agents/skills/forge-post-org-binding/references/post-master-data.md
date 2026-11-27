# Post Master Data

## Entity

`com.mdframe.forge.plugin.system.entity.SysPost` → table `sys_post`

Key fields: `postCode` (tenant-unique), `postName`, `orgId`, `postStatus` (0/1), `postType`, `sort`, audit + `del_flag`.

## APIs (`SysPostController` `/system/post`)

| Action | Method | Path |
|--------|--------|------|
| Page | GET | `/page` |
| List | GET | `/list` |
| Detail | POST | `/getById` |
| Add | POST | `/add` |
| Edit | POST | `/edit` |
| Remove | POST | `/remove` |

DTO: `SysPostDTO` — `postCode` / `postName` required.

## Seeds

Prefer guarded Flyway inserts into `sys_post` with `tenant_id = 1`. Follow current logical-delete uniqueness conventions for the table version in tree.
