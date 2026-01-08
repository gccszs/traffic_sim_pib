# Service Architecture

<cite>
**Referenced Files in This Document**   
- [web_app.py](file://web_app.py)
- [grpc_server.py](file://grpc_server.py)
- [start_services.py](file://start_services.py)
- [config.py](file://config.py)
- [proto/map_service.proto](file://proto/map_service.proto)
- [map_utils/mapmaker.py](file://map_utils/mapmaker.py)
- [map_utils/mapmaker_new.py](file://map_utils/mapmaker_new.py)
- [utils/file_response.py](file://utils/file_response.py)
- [Dockerfile](file://Dockerfile)
- [start.sh](file://start.sh)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)

## Introduction
The map_convert_services system implements a dual-service architecture that exposes the same business functionality through two distinct interfaces: a REST API using FastAPI and a gRPC service. This design allows clients to choose the most appropriate communication protocol based on their requirements while ensuring consistent processing logic across both interfaces. The services coexist within the same process space, sharing business logic components and configuration, with the FastAPI service delegating certain operations to the gRPC service for unified processing. The system is designed for traffic simulation scenarios, handling map conversion, simulation initialization, and plugin management.

## Project Structure

```mermaid
graph TD
subgraph "Services"
web_app[FastAPI Service<br>web_app.py]
grpc_server[gRPC Service<br>grpc_server.py]
end
subgraph "Configuration & Startup"
config[Configuration<br>config.py]
start_services[Service Orchestration<br>start_services.py]
start_sh[Shell Startup<br>start.sh]
end
subgraph "Business Logic"
map_utils[Map Conversion Utilities<br>map_utils/]
utils[Core Utilities<br>utils/]
vo[Data Transfer Objects<br>vo/]
end
subgraph "Protocols & Interfaces"
proto[gRPC Definitions<br>proto/]
end
subgraph "Data & Assets"
SimEngPI[Simulation Engine Inputs<br>SimEngPI/]
cache[Conversion Cache<br>cache/]
engine_sim_logs[Execution Logs<br>engine_sim_logs/]
plugins[Extension Plugins<br>plugins/]
end
web_app --> utils
web_app --> vo
web_app --> config
web_app --> map_utils
grpc_server --> map_utils
grpc_server --> proto
grpc_server --> config
start_services --> web_app
start_services --> grpc_server
start_services --> config
start_sh --> web_app
start_sh --> grpc_server
```

**Diagram sources**
- [web_app.py](file://web_app.py)
- [grpc_server.py](file://grpc_server.py)
- [start_services.py](file://start_services.py)
- [config.py](file://config.py)
- [proto/map_service.proto](file://proto/map_service.proto)

**Section sources**
- [web_app.py](file://web_app.py)
- [grpc_server.py](file://grpc_server.py)
- [start_services.py](file://start_services.py)
- [config.py](file://config.py)
- [proto/map_service.proto](file://proto/map_service.proto)

## Core Components

The system's core components consist of two primary services: the FastAPI web service (web_app.py) and the gRPC service (grpc_server.py), both orchestrated by the start_services.py module. The FastAPI service provides REST endpoints for file upload, simulation initialization, and plugin management, while the gRPC service offers efficient binary communication for map conversion and preview operations. Both services share business logic through utility modules in map_utils and utils directories, ensuring consistent processing regardless of the entry point. Configuration is centralized in config.py using Pydantic models, enabling environment-based settings management. The proto directory contains the gRPC service definition that establishes the contract between clients and the gRPC server.

**Section sources**
- [web_app.py](file://web_app.py)
- [grpc_server.py](file://grpc_server.py)
- [config.py](file://config.py)
- [proto/map_service.proto](file://proto/map_service.proto)

## Architecture Overview

```mermaid
graph TD
Client[External Clients] --> |HTTP/REST| FastAPI[FastAPI Service<br>web_app.py]
Client --> |gRPC| GRPC[gRPC Service<br>grpc_server.py]
FastAPI --> |Delegates| GRPC
FastAPI --> |Uses| BusinessLogic[Shared Business Logic<br>map_utils/ & utils/]
GRPC --> |Uses| BusinessLogic
subgraph "Startup & Configuration"
Config[Configuration<br>config.py]
StartServices[Service Orchestration<br>start_services.py]
StartSh[Shell Script<br>start.sh]
end
StartServices --> FastAPI
StartServices --> GRPC
FastAPI --> Config
GRPC --> Config
subgraph "Data Management"
Cache[Cache Directory<br>cache/]
SimEngPI[Simulation Inputs<br>SimEngPI/]
Logs[Execution Logs<br>engine_sim_logs/]
end
FastAPI --> Cache
FastAPI --> SimEngPI
FastAPI --> Logs
GRPC --> Cache
subgraph "Extensions"
Plugins[Plugin System<br>plugins/]
end
FastAPI --> Plugins
style FastAPI fill:#f9f,stroke:#333
style GRPC fill:#bbf,stroke:#333
```

**Diagram sources**
- [web_app.py](file://web_app.py#L1-L269)
- [grpc_server.py](file://grpc_server.py#L1-L283)
- [start_services.py](file://start_services.py#L1-L73)
- [config.py](file://config.py#L1-L21)

## Detailed Component Analysis

### FastAPI Service Analysis
The FastAPI service (web_app.py) serves as the primary HTTP interface for the system, exposing endpoints for file upload, simulation initialization, and plugin management. It uses Pydantic models for request validation and response formatting, with the CreateSimengRequest model defining the structure for simulation creation requests. The service handles file uploads through the /fileupload endpoint, which saves uploaded files to user-specific directories within the cache folder and delegates conversion logic to shared utilities. The /init_simeng endpoint orchestrates simulation setup by creating directory structures, processing OD (Origin-Destination) data from JSON to XML format, and launching the simulation engine as a separate process. WebSocket endpoints enable bidirectional communication between the frontend and backend systems for real-time updates.

```mermaid
sequenceDiagram
participant Frontend
participant FastAPI
participant Utils
participant SimulationEngine
Frontend->>FastAPI : POST /init_simeng
FastAPI->>FastAPI : Validate request with CreateSimengRequest
FastAPI->>FastAPI : Create user directories in SimEngPI/
FastAPI->>FastAPI : Process OD JSON to XML
FastAPI->>FastAPI : Copy selected plugins
FastAPI->>Utils : RunExe with simulation command
Utils->>SimulationEngine : Launch SimulationEngine.exe
SimulationEngine->>Utils : Return process status
Utils->>FastAPI : Return execution result
FastAPI->>Frontend : Return JSON response
```

**Diagram sources**
- [web_app.py](file://web_app.py#L90-L188)
- [vo/request_vo.py](file://vo/request_vo.py#L7-L10)
- [utils/command_runner.py](file://utils/command_runner.py#L11-L199)

**Section sources**
- [web_app.py](file://web_app.py#L1-L269)
- [vo/request_vo.py](file://vo/request_vo.py#L1-L10)
- [utils/command_runner.py](file://utils/command_runner.py#L1-L199)

### gRPC Service Analysis
The gRPC service (grpc_server.py) provides a high-performance interface for map conversion operations, implementing the MapConvertService defined in map_service.proto. The service exposes two primary methods: ConvertMap for full map conversion and PreviewMap for generating map statistics without full processing. Both methods share the same underlying conversion logic through the _convert_file and _convert_txt_to_xml helper methods, ensuring consistent behavior across different operation types. The service uses asynchronous processing to handle multiple requests concurrently, with proper error handling and logging implemented throughout. The conversion process supports multiple input formats (OSM and TXT), with OSM files first converted to TXT format before XML generation.

```mermaid
sequenceDiagram
participant Client
participant GRPC
participant MapUtils
participant FileResponse
Client->>GRPC : ConvertMap Request
GRPC->>GRPC : Create user work directory
GRPC->>GRPC : Save uploaded file
GRPC->>GRPC : Determine file type
alt OSM File
GRPC->>MapUtils : osm_to_txt conversion
MapUtils->>GRPC : Return TXT file path
GRPC->>GRPC : Attempt old conversion method
alt Success
GRPC->>GRPC : Return XML data
else Failure
GRPC->>GRPC : Attempt new conversion method
GRPC->>GRPC : Return XML data
end
else TXT File
GRPC->>GRPC : Attempt old conversion method
alt Success
GRPC->>GRPC : Return XML data
else Failure
GRPC->>GRPC : Attempt new conversion method
GRPC->>GRPC : Return XML data
end
end
GRPC->>Client : ConvertMap Response
```

**Diagram sources**
- [grpc_server.py](file://grpc_server.py#L35-L250)
- [proto/map_service.proto](file://proto/map_service.proto#L1-L66)
- [map_utils/osmtrans.py](file://map_utils/osmtrans.py)
- [map_utils/mapmaker.py](file://map_utils/mapmaker.py)
- [map_utils/mapmaker_new.py](file://map_utils/mapmaker_new.py)

**Section sources**
- [grpc_server.py](file://grpc_server.py#L1-L283)
- [proto/map_service.proto](file://proto/map_service.proto#L1-L66)

### Service Initialization Analysis
The service initialization process is orchestrated through multiple entry points, with start_services.py providing a Python-based unified startup mechanism and start.sh offering a shell-based alternative. The start_services.py script uses threading to run the FastAPI service in a background thread while running the gRPC service in the main asyncio event loop, ensuring both services operate concurrently. Configuration is loaded via the Pydantic Settings model in config.py, which reads environment variables with sensible defaults. The gRPC server is configured with a thread pool of 10 workers to handle concurrent requests, while the FastAPI service leverages Uvicorn's built-in async capabilities. Graceful shutdown is implemented through exception handling of KeyboardInterrupt, allowing for proper cleanup.

```mermaid
flowchart TD
Start([Start Services])
--> LoadConfig["Load Configuration via Pydantic Settings"]
--> StartFastAPI["Start FastAPI in Background Thread"]
--> StartGRPC["Start gRPC in Main Async Loop"]
--> AwaitTermination["Wait for Service Termination"]
--> HandleShutdown["Handle Graceful Shutdown"]
--> End([Services Stopped])
subgraph "Configuration"
LoadConfig --> |Reads| ENV["APP_HOST"]
LoadConfig --> |Reads| ENV["APP_PORT"]
LoadConfig --> |Reads| ENV["GRPC_PORT"]
LoadConfig --> |Reads| ENV["LOG_HOME"]
end
subgraph "Service Startup"
StartFastAPI --> |uvicorn.run| FastAPIServer
StartGRPC --> |asyncio.run| GRPCServer
GRPCServer --> |ThreadPoolExecutor<br>max_workers=10| WorkerPool
end
subgraph "Shutdown"
HandleShutdown --> |KeyboardInterrupt| StopGRPC["Stop gRPC Server"]
StopGRPC --> StopFastAPI["Stop FastAPI Thread"]
end
```

**Diagram sources**
- [start_services.py](file://start_services.py#L23-L72)
- [config.py](file://config.py#L1-L21)
- [grpc_server.py](file://grpc_server.py#L252-L282)

**Section sources**
- [start_services.py](file://start_services.py#L1-L73)
- [config.py](file://config.py#L1-L21)
- [start.sh](file://start.sh#L1-L42)

## Dependency Analysis

```mermaid
graph TD
web_app[web_app.py] --> config[config.py]
web_app --> utils[utils/]
web_app --> vo[vo/]
web_app --> map_utils[map_utils/]
web_app --> sim_plugin[sim_plugin.py]
grpc_server[grpc_server.py] --> config[config.py]
grpc_server --> proto[proto/]
grpc_server --> map_utils[map_utils/]
start_services[start_services.py] --> config[config.py]
start_services --> web_app[web_app.py]
start_services --> grpc_server[grpc_server.py]
utils[utils/] --> map_utils[map_utils/]
utils[utils/] --> vo[vo/]
map_utils[map_utils/] --> road[road.py]
map_utils[map_utils/] --> osmtrans[osmtrans.py]
map_utils[map_utils/] --> mapmaker[mapmaker.py]
map_utils[map_utils/] --> mapmaker_new[mapmaker_new.py]
proto[proto/] --> map_service_proto[map_service.proto]
Dockerfile[Dockerfile] --> start_sh[start.sh]
Dockerfile --> web_app[web_app.py]
Dockerfile --> grpc_server[grpc_server.py]
Dockerfile --> generate_grpc[generate_grpc.py]
start_sh[start.sh] --> web_app[web_app.py]
start_sh --> grpc_server[grpc_server.py]
style web_app fill:#f9f,stroke:#333
style grpc_server fill:#bbf,stroke:#333
style start_services fill:#ff9,stroke:#333
style config fill:#9f9,stroke:#333
```

**Diagram sources**
- [web_app.py](file://web_app.py)
- [grpc_server.py](file://grpc_server.py)
- [start_services.py](file://start_services.py)
- [config.py](file://config.py)
- [utils/](file://utils/)
- [vo/](file://vo/)
- [map_utils/](file://map_utils/)
- [proto/](file://proto/)
- [Dockerfile](file://Dockerfile)
- [start.sh](file://start.sh)

**Section sources**
- [web_app.py](file://web_app.py)
- [grpc_server.py](file://grpc_server.py)
- [start_services.py](file://start_services.py)
- [config.py](file://config.py)

## Performance Considerations
The system employs several resource allocation and performance optimization strategies. The gRPC service uses a ThreadPoolExecutor with 10 worker threads to handle concurrent requests efficiently, balancing resource usage against potential thread contention. File operations are optimized through the use of streaming and binary processing, minimizing memory overhead during large file conversions. The dual-service design allows clients to select the most appropriate protocol: REST for simplicity and gRPC for performance-critical operations. The system leverages asynchronous processing throughout, with both FastAPI and gRPC services designed to handle I/O-bound operations without blocking. For high-throughput scenarios, the architecture supports horizontal scaling through containerization, with Docker providing isolation and consistent deployment. Load balancing can be implemented at the ingress level, routing REST and gRPC traffic to separate service instances when needed. The caching strategy, using the cache/ directory for intermediate files, reduces redundant processing but requires monitoring to prevent disk space exhaustion.

## Troubleshooting Guide
Common issues in this system typically involve configuration mismatches, file permission problems, or process execution failures. The configuration system relies on environment variables that must align with the Pydantic Settings model in config.py; mismatches can cause services to bind to incorrect ports or use wrong paths. The simulation engine execution depends on proper file permissions and Wine configuration in non-Windows environments, with detailed error handling in RunExe providing diagnostic information. The gRPC service requires generated code from map_service.proto, which must be kept in sync using generate_grpc.py. Monitoring integration points include structured logging with timestamps and component names, execution logs written to the engine_sim_logs/ directory, and health status available through the WebSocket interface. For deployment issues, the Dockerfile provides a reference environment with all dependencies, including Wine for executing the Windows-based simulation engine.

**Section sources**
- [config.py](file://config.py#L1-L21)
- [utils/command_runner.py](file://utils/command_runner.py#L1-L199)
- [generate_grpc.py](file://generate_grpc.py#L1-L66)
- [Dockerfile](file://Dockerfile#L1-L74)

## Conclusion
The map_convert_services architecture successfully implements a dual-service design that provides flexible access to map conversion and traffic simulation functionality through both REST and gRPC interfaces. By sharing business logic while maintaining protocol-specific endpoints, the system accommodates diverse client requirements without duplicating core functionality. The initialization process through start_services.py demonstrates effective orchestration of multiple asynchronous services with proper configuration management and graceful shutdown handling. The container-ready design with Docker support enables consistent deployment across environments, while the modular structure allows for independent scaling of REST and gRPC workloads. Future enhancements could include health check endpoints, metrics collection, and more sophisticated load balancing strategies to further improve scalability and observability.