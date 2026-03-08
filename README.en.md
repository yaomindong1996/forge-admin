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
forge/
├── forge-admin/                 # Main application module
├── forge-framework/            # Framework core
│   ├── forge-plugin-parent/    # Plugin parent module
│   │   ├── forge-plugin-system/     # System management plugin
│   │   ├── forge-plugin-generator/  # Code generation plugin
│   │   ├── forge-plugin-job/        # Job scheduling plugin
│   │   └── forge-plugin-message/    # Message plugin
│   └── forge-starter-parent/   # Starter parent module
│       ├── forge-starter-auth/      # Authentication & authorization
│       ├── forge-starter-cache/     # Cache management
│       ├── forge-starter-config/    # Configuration center
│       └── forge-starter-api-config/# API configuration
```

### Frontend Project

```
forge-admin-ui/
├── src/
│   ├── api/            # API interfaces
│   ├── assets/         # Static resources
│   ├── components/     # Common components
│   ├── composables/    # Composition API
│   ├── layouts/       # Layout components
│   ├── router/        # Routing configuration
│   ├── store/         # State management
│   ├── styles/        # Global styles
│   ├── utils/         # Utility functions
│   └── views/         # Page views
└── ...
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

2. Import the database

Execute `forge/forge-admin/sql/sys.sql` to create the base database tables.

3. Modify configuration

Edit `forge/forge-admin/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/forge_admin?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
```

4. Start the service

```bash
cd forge/forge-admin
mvn spring-boot:run
```

The service will run by default at `http://localhost:8080`

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