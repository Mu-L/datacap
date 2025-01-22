**DataCap Released!**

| Release Version |  Published   |
|:---------------:|:------------:|
|   `2024.4.1`    | `2024-12-31` |

Dear DataCap users,

DataCap 2024.4.1 is now generally available. This update includes a number of important feature upgrades and performance improvements, and the main updates are as follows:

### Core function upgrades

1. **Database Enhancements (Implementations)**
    - Added database management feature: You can create, delete, and switch databases
    - Improved table management features: You can create, delete, and insert data

2. **MongoDB driver upgrade**
    - Optimize query performance: Replace find with aggregate
    - Added metadata support
    - Supports com.dbschema.MongoJdbcDriver adaptation
    - Improve version control and index fetching functions

3. **Workflow Engine Optimization**
    - Added workflow task submission feature
    - Workflow restart operations are supported
    - Optimized the SeatTunnel executor to support custom node types

4. **SQL parser enhancements**
    - Optimized the structure of G4 expressions
    - Added support for SHOW statements
    - Improve the formatting function of SELECT statements
    - Support for CREATE DATABASE syntax

### Bug fixes
- Fixed the issue of creating query history
- Fixed the JSON conversion issue of the LocalDateTime type
- The issue of historical data acquisition failure is fixed
- Optimized the acquisition of the Dameng database plug-in version

### Other optimizations
- Improved Windows platform support
- Optimized release scripts
- Optimize your CI/CD process

### How to get it
- GitHub：https://github.com/devlive-community/datacap
- Official website: https://datacap.devlive.org/
- Docker: The latest image has been updated

Thank you to the community for your continued support and feedback. If you have questions or suggestions, please feel free to contact us via GitHub Issues.