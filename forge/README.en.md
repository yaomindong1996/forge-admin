The current project directory structure shows this is a multi-module Java project based on Spring Boot, containing multiple functional modules such as system management, job scheduling, message notification, file storage, and ID generation. This project is primarily designed for enterprise-level application development, providing foundational capabilities including permission management, data dictionaries, logging, file uploads, scheduled tasks, and message pushing.

## Project Overview

Forge is an enterprise-level development framework based on Spring Boot, offering a suite of ready-to-use foundational modules to help developers rapidly build enterprise applications. This project adopts a modular design and supports multi-tenant architecture, making it suitable for enterprise scenarios requiring complex permission control, job scheduling, message notifications, and data management.

## Main Functional Modules

- **forge-admin**: The main module, providing the startup class and core configuration.
- **forge-plugin-system**: System management module, including foundational features such as users, roles, permissions, organizational structure, positions, and tenants.
- **forge-plugin-job**: Job scheduling module, providing management and execution capabilities for scheduled tasks.
- **forge-plugin-message**: Message notification module, supporting sending, receiving, and managing internal system messages.
- **forge-plugin-generator**: Code generation module, supporting automatic code generation based on database table structures.
- **forge-starter-auth**: Authentication module, providing login, registration, and permission verification functionalities.
- **forge-starter-cache**: Cache module, providing distributed caching support based on Redisson.
- **forge-starter-config**: Configuration center module, supporting dynamic configuration loading from the database and real-time refresh.
- **forge-starter-datascope**: Data scope module, providing role-based data range control.
- **forge-starter-excel**: Excel export module, supporting generic Excel export functionality.
- **forge-starter-file**: File storage module, supporting local and cloud file storage with upload, download, and delete operations.
- **forge-starter-id**: ID generation module, providing distributed ID generation capabilities.
- **forge-starter-log**: Logging module, supporting recording of operation logs and login logs.
- **forge-starter-message**: Message channel module, supporting multiple message delivery methods such as SMS and in-system messages.
- **forge-starter-trans**: Data translation module, supporting dictionary translation and enum conversion.
- **forge-starter-orm**: ORM module, providing database access capabilities based on MyBatis Plus.
- **forge-starter-web**: Web support module, offering generic web-layer support.

## Technology Stack

- **Java 17+**
- **Spring Boot 2.7+**
- **MyBatis Plus**
- **Redisson (Caching)**
- **Quartz (Job Scheduling)**
- **Sa-Token (Permission Control)**
- **MySQL (Default Database Support)**
- **Distributed ID Generator (Based on UidGenerator)**
- **Multi-Tenant Architecture Support**

## Project Features

- **Modular Design**: Decoupled functional modules for easy extension and maintenance.
- **Multi-Tenant Support**: Supports multi-tenant architecture, ideal for SaaS scenarios.
- **Data Permission Control**: Supports fine-grained data permission configuration.
- **Code Generator**: Automatically generates code based on database table structures to improve development efficiency.
- **Integrated Generic Features**: Integrates common functionalities such as file upload, logging, message notification, and scheduled tasks.
- **Configuration Center Support**: Supports dynamic configuration loading from the database with real-time refresh.
- **Internationalization Support**: Supports multiple languages including Chinese and English.

## Usage Instructions

### Starting the Project

1. Ensure JDK 17+ and Maven are installed.
2. Import the database schema (SQL files are located in the `sql` directory of each module).
3. Update the database connection information in `forge-admin/src/main/resources/application.yml`.
4. Execute `mvn clean install` to install dependencies.
5. Run `ForgeAdminApplication.java` to start the project.

### Configuration Details

- **Database Configuration**: Configure database connection details in `application.yml`.
- **Multi-Tenant Configuration**: Supports tenant isolation; enable or disable via configuration file.
- **File Storage Configuration**: Supports local storage, MinIO, and OSS: //gitee.com/ForgeLab/forge

### Project Structure

```
forge
├── forge-admin
│   └── Startup module containing the main class and core configuration
├── forge-framework
│   └── forge-plugin-parent
│       ├── forge-plugin-system       # System management module
│       ├── forge-plugin-job          # Job scheduling module
│       ├── forge-plugin-message      # Message notification module
│       ├── forge-plugin-generator    # Code generation module
│       └── ...
│   └── forge-starter-parent
│       ├── forge-starter-auth        # Authentication module
│       ├── forge-starter-cache       # Cache module
│       ├── forge-starter-config      # Configuration center module
│       ├── forge-starter-datascope   # Data scope module
│       ├── forge-starter-excel       # Excel export module
│       ├── forge-starter-file        # File storage module
│       ├── forge-starter-id          # ID generation module
│       ├── forge-starter-log         # Logging module
│       ├── forge-starter-message     # Message channel module
│       ├── forge-starter-trans       # Data translation module
│       └── forge-starter-orm         # ORM module
└── pom.xml
```

## Development Recommendations

- **Module Extension**: To add new business modules, follow the structure under `forge-plugin-parent`.
- **Multi-Tenant Development**: Pay attention to tenant isolation logic; ensure all data operations include the tenant ID.
- **Permission Control**: Use Sa-Token for permission control, combined with `@DataScopeIgnore` to manage data scope.
- **Logging**: Use the `@OperationLog` annotation to record operation logs.
- **Internationalization**: Multi-language resource files are located at `forge-admin/src/main/resources/i18n/`.

## Open Source License

This project is licensed under the **Apache-2.0** license. See the `LICENSE` file in the project root directory for details.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork this project.
2. Create a new branch.
3. Submit a Pull Request.

## Contact

For technical support or feedback, please submit an Issue or contact the project maintainers.

---

**Project URL**: [Gitee Forge Project](https://gitee.com/ForgeLab/forge)