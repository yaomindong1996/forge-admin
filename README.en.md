# Forge Admin

Forge Admin is an enterprise-level mid-to-back-end management system built on Spring Boot and Vue 3, adopting a frontend-backend separation architecture. It provides comprehensive enterprise-grade features including user permission management, multi-tenancy, and system monitoring.

## Project Overview

Forge Admin is a modern enterprise-grade admin system designed to serve as a foundational framework for rapid development and business expansion. The system employs a microkernel + plugin-based architecture, where core functionalities exist as plugins, enabling on-demand integration and extensibility.

### Core Features

- **Microkernel Architecture**: Lightweight core framework with functionality extended via plugins
- **Multi-Tenancy Support**: Robust multi-tenant system with data isolation
- **Permission Management**: Fine-grained access control based on RBAC
- **Code Generation**: Visual code generation for rapid business module construction
- **Dynamic API**: Runtime API configuration management with dynamic interface behavior adjustment
- **Task Scheduling**: Distributed task scheduling with Cron expression support
- **Message Center**: Unified message management supporting multiple notification channels
- **System Monitoring**: Real-time system monitoring to track server status

## Technology Stack

### Backend Technologies

| Technology | Description |
|------------|-------------|
| Spring Boot | Application development framework |
| Spring Cloud | Microservices framework (optional) |
| MyBatis-Plus | ORM framework |
| Sa-Token | Authentication and authorization framework |
| Redisson | Distributed caching |
| Quartz | Task scheduling |
| Spring Cloud Gateway | API gateway (optional) |

### Frontend Technologies

| Technology | Description |
|------------|-------------|
| Vue 3 | Progressive frontend framework |
| Naive UI | Vue 3 component library |
| Pinia | State management |
| Vue Router | Routing management |
| Vite | Build tool |
| UnoCSS | Atomic CSS |

## Module Structure

### Backend Modules

```
forge-server/                    # Backend root
├── forge-admin-server/          # Main admin application
├── forge-report-server/         # AI dashboard service
├── forge-app-server/            # App / H5 API service
├── forge-flow/                  # Standalone Flowable service and client
│   ├── forge-flow-server/
│   └── forge-flow-client/
├── forge-business/              # Business modules
├── forge-framework/             # Framework core
│   ├── forge-plugin-parent/     # Plugin parent module
│   │   ├── forge-plugin-system/     # System management plugin
│   │   ├── forge-plugin-generator/  # Code generation plugin
│   │   ├── forge-plugin-job/        # Job scheduling plugin
│   │   └── forge-plugin-message/    # Message plugin
│   └── forge-starter-parent/    # Starter parent module
│       ├── forge-starter-auth/      # Authentication & authorization
│       ├── forge-starter-cache/     # Cache management
│       ├── forge-starter-config/    # Configuration center
│       └── forge-starter-api-config/# API configuration
├── db/                          # Flyway migrations, seed data, full init SQL
└── scripts/                     # Database init and community export scripts
```

### Frontend Project

```
forge-admin-ui/                  # Admin console frontend
forge-report-ui/                 # AI dashboard frontend
forge-h5-ui/                     # Mobile H5
```

## Quick Start

### Environment Requirements

- JDK 17+
- Node.js 18+
- pnpm 8+
- MySQL 8.0+
- Redis 6.0+

### Backend Deployment

1. Clone the project

```bash
git clone https://gitee.com/ForgeLab/forge-admin.git
cd forge-admin
```

2. Initialize the database

From the repository root, run the unified init script. It executes `forge-server/db/全量初始化SQL.sql` and `forge-server/db/seed/required`. Incremental schema changes are applied by Flyway from `forge-server/db/migration` when `forge-admin-server` starts.

```bash
bash forge-server/scripts/db/init-db.sh \
  --host 127.0.0.1 \
  --port 3306 \
  --database forge_admin \
  --user root \
  --password your_password
```

3. Prepare local configuration

```bash
cp forge-server/forge-admin-server/src/main/resources/application-dev.example.yml \
   forge-server/forge-admin-server/src/main/resources/application-dev.yml
```

Then edit MySQL, Redis, and other local settings in `application-dev.yml`. Do not commit this file.

4. Start the service

```bash
cd forge-server/forge-admin-server
mvn spring-boot:run
```

The admin service runs at `http://localhost:8580` by default.

Optional services:

```bash
# AI dashboard service: http://localhost:8581
cd forge-server/forge-report-server
mvn spring-boot:run

# Flow service: http://localhost:8081
cd forge-server/forge-flow/forge-flow-server
mvn spring-boot:run

# App / H5 API service: http://localhost:8583
cd forge-server/forge-app-server
mvn spring-boot:run
```

### Frontend Deployment

1. Install dependencies

```bash
cd forge-admin-ui
pnpm install
```

2. Start the development server

```bash
pnpm dev
```

3. Build the production version

```bash
pnpm build
```

Optional frontends:

```bash
# AI dashboard UI: http://localhost:3021/forge-report
cd forge-report-ui
pnpm install
pnpm dev

# Mobile H5: http://localhost:3009 (requires forge-app-server)
cd forge-h5-ui
pnpm install
pnpm dev:h5
```

Production backend build:

```bash
cd forge-server
mvn clean install -DskipTests
```

## Functional Modules

### System Management

| Module | Description |
|--------|-------------|
| User Management | Create, read, update, delete users; assign roles and organizational associations |
| Role Management | Configure role permissions and bind resources |
| Menu Management | Dynamic menu configuration and page routing management |
| Department Management | Organizational structure management with tree hierarchy |
| Position Management | Position configuration and user-position associations |
| Tenant Management | Multi-tenant configuration and tenant isolation |

### System Monitoring

| Module | Description |
|--------|-------------|
| Online Users | View currently online users and forcibly log them out |
| Scheduled Jobs | Configure and dynamically schedule tasks |
| System Logs | Query operation logs and login logs |
| System Monitoring | Monitor CPU, memory, and disk usage |

### Operations Tools

| Module | Description |
|--------|-------------|
| Cache Management | Visual operations for Redis cache |
| File Management | File upload and storage configuration |
| Dictionary Management | Maintain static dictionaries |
| Notifications & Announcements | Publish notifications and track read status |

### Developer Tools

| Module | Description |
|--------|-------------|
| Code Generation | Visual configuration and code generation |
| API Configuration | Dynamically configure interface behavior |
| Data Source Management | Configure multiple data sources |

## Plugin Descriptions

### System Management Plugin (forge-plugin-system)

Provides comprehensive system management features including user, role, menu, department, position, and tenant management.

### Code Generation Plugin (forge-plugin-generator)

Visual code generation tool supporting:
- Database table import
- Field configuration
- Template management
- Code preview and download

### Job Scheduling Plugin (forge-plugin-job)

Distributed task scheduling based on Quartz, supporting:
- Cron expression configuration
- Manual task triggering
- Task execution logs

### Message Plugin (forge-plugin-message)

Unified message center supporting:
- System notifications
- In-app messages
- Message templates

## Contribution Guidelines

Issues and Pull Requests are welcome.

## License

This project is open-sourced under the [MIT](LICENSE) license.