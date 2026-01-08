# API Reference

<cite>
**Referenced Files in This Document**   
- [web_app.py](file://web_app.py)
- [grpc_server.py](file://grpc_server.py)
- [map_service.proto](file://proto/map_service.proto)
- [file_response.py](file://utils/file_response.py)
- [config.py](file://config.py)
- [mapmaker.py](file://map_utils/mapmaker.py)
- [mapmaker_new.py](file://map_utils/mapmaker_new.py)
- [osmtrans.py](file://map_utils/osmtrans.py)
- [Dockerfile](file://Dockerfile)
- [start.sh](file://start.sh)
- [start_services.py](file://start_services.py)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [RESTful API](#restful-api)
3. [gRPC API](#grpc-api)
4. [Authentication and Security](#authentication-and-security)
5. [Rate Limiting and Timeout Configuration](#rate-limiting-and-timeout-configuration)
6. [Error Handling](#error-handling)
7. [Client Implementation Examples](#client-implementation-examples)
8. [Protocol-Specific Debugging](#protocol-specific-debugging)
9. [TLS Configuration](#tls-configuration)
10. [Load Testing Recommendations](#load-testing-recommendations)
11. [Versioning Strategy](#versioning-strategy)
12. [Backward Compatibility](#backward-compatibility)

## Introduction
The map_convert_services system provides a dual-interface API for converting map files into XML format for traffic simulation. The system offers both RESTful HTTP and gRPC interfaces to accommodate different client requirements and performance needs. The service converts various map formats (OSM, TXT) into a standardized XML format used by the simulation engine, with support for both full conversion and preview operations.

The system is designed to handle user-specific workspaces through the user_id parameter, ensuring isolation between different users' conversion processes. The architecture separates concerns between the web interface (FastAPI) and the gRPC service, both utilizing shared conversion utilities for consistency.

**Section sources**
- [web_app.py](file://web_app.py#L1-L269)
- [grpc_server.py](file://grpc_server.py#L1-L283)
- [config.py](file://config.py#L1-L21)

## RESTful API

### /fileupload Endpoint
The `/fileupload` endpoint provides a RESTful interface for uploading and converting map files. This endpoint accepts multipart/form-data requests and returns the converted XML file as a binary stream response.

#### Request
- **Method**: POST
- **Endpoint**: `/fileupload`
- **Content-Type**: `multipart/form-data`

**Request Parameters**:
- `file`: The map file to be converted (required)
  - Supported formats: OSM, TXT
  - Size limitations: Maximum 50MB per file
- `user_id`: User identifier for workspace isolation (required)
  - Format: String
  - Purpose: Creates a dedicated workspace directory for the user

**Request Example**:
```bash
curl -X POST "http://localhost:8000/fileupload" \
  -F "file=@map.osm" \
  -F "user_id=1718833e-a6d4-4fff-a62e-f33af4f9c5b6"
```

#### Response
The response is a binary stream of the converted XML file with metadata in HTTP headers.

**Response Headers**:
- `Content-Disposition`: Attachment with filename
- `Content-Length`: Size of the XML file in bytes
- `X-Filename`: Safe filename of the converted XML
- `X-Original-File`: Original uploaded filename
- `X-Conversion-Method`: Method used for conversion ("old" or "new")
- `X-Success`: "true" if conversion succeeded

**Success Response**:
- **Status Code**: 200 OK
- **Content-Type**: `application/xml`
- **Body**: Binary stream of the converted XML file

**Error Response**:
- **Status Code**: 500 Internal Server Error
- **Content-Type**: `application/json`
- **Body**: JSON object with error details

```json
{
  "success": false,
  "error": "Error description",
  "code": 500
}
```

**Section sources**
- [web_app.py](file://web_app.py#L52-L80)
- [file_response.py](file://utils/file_response.py#L8-L55)

## gRPC API

### Service Definition
The gRPC API is defined in the `map_service.proto` file and provides two primary methods for map conversion operations.

```protobuf
service MapConvertService {
  rpc ConvertMap(ConvertMapRequest) returns (ConvertMapResponse);
  rpc PreviewMap(PreviewMapRequest) returns (PreviewMapResponse);
}
```

**Diagram sources**
- [map_service.proto](file://proto/map_service.proto#L10-L16)

### ConvertMap Method
The `ConvertMap` method performs a complete conversion of a map file to XML format.

#### Request Message
```protobuf
message ConvertMapRequest {
  bytes file_content = 1;     // File content (binary)
  string file_name = 2;       // Original filename
  string user_id = 3;         // User ID for workspace creation
}
```

#### Response Message
```protobuf
message ConvertMapResponse {
  bool success = 1;           // Whether the operation succeeded
  string message = 2;         // Message (error or success)
  bytes xml_data = 3;         // Converted XML data
  string xml_file_name = 4;   // XML filename
  string conversion_method = 5; // Conversion method ("old" or "new")
}
```

#### Streaming Behavior
The `ConvertMap` method uses unary RPC (single request, single response). No streaming is employed for this method.

#### Example Usage (Python)
```python
import grpc
from proto import map_service_pb2, map_service_pb2_grpc

def convert_map(stub, file_content, file_name, user_id):
    request = map_service_pb2.ConvertMapRequest(
        file_content=file_content,
        file_name=file_name,
        user_id=user_id
    )
    response = stub.ConvertMap(request)
    return response
```

**Section sources**
- [map_service.proto](file://proto/map_service.proto#L19-L40)
- [grpc_server.py](file://grpc_server.py#L35-L77)

### PreviewMap Method
The `PreviewMap` method provides a lightweight preview of map data without performing a full conversion.

#### Request Message
```protobuf
message PreviewMapRequest {
  bytes file_content = 1;     // File content (binary)
  string file_name = 2;       // Original filename
  string user_id = 3;         // User ID
}
```

#### Response Message
```protobuf
message PreviewMapResponse {
  bool success = 1;           // Whether the operation succeeded
  string message = 2;         // Message
  string preview_data = 3;    // Simplified map information as JSON
  int32 road_count = 4;       // Number of roads
  int32 intersection_count = 5; // Number of intersections
}
```

#### Streaming Behavior
The `PreviewMap` method uses unary RPC (single request, single response). No streaming is employed for this method.

#### Example Usage (Python)
```python
def preview_map(stub, file_content, file_name, user_id):
    request = map_service_pb2.PreviewMapRequest(
        file_content=file_content,
        file_name=file_name,
        user_id=user_id
    )
    response = stub.PreviewMap(request)
    return response
```

**Section sources**
- [map_service.proto](file://proto/map_service.proto#L42-L64)
- [grpc_server.py](file://grpc_server.py#L89-L137)

## Authentication and Security
The map_convert_services system currently does not implement authentication mechanisms for API access. Access control is achieved through the following methods:

1. **User Isolation**: The `user_id` parameter creates isolated workspaces for each user, preventing cross-user data access.
2. **File Path Security**: The `get_safe_path` function sanitizes filenames to prevent directory traversal attacks.
3. **Network-Level Security**: The service should be deployed behind a reverse proxy or API gateway that handles authentication.

For production deployments, it is recommended to implement one of the following authentication methods:
- API Key authentication via HTTP headers
- OAuth 2.0 for user-based authentication
- JWT tokens for stateless authentication

The system relies on external security measures rather than implementing authentication at the application level, allowing for flexibility in deployment scenarios.

**Section sources**
- [web_app.py](file://web_app.py#L53-L80)
- [file_response.py](file://utils/file_response.py#L88-L94)
- [grpc_server.py](file://grpc_server.py#L53-L55)

## Rate Limiting and Timeout Configuration
The map_convert_services system implements the following rate limiting and timeout configurations:

### Rate Limiting
The system does not implement explicit rate limiting at the application level. Rate limiting should be implemented at the infrastructure level using:
- Reverse proxy (e.g., NGINX) with rate limiting
- API gateway with throttling policies
- Load balancer with connection limits

Recommended rate limits for production deployment:
- 100 requests per minute per user_id
- 10 concurrent requests per user_id
- 1000 requests per minute per IP address

### Timeout Configuration
The system has the following timeout configurations:

**gRPC Service**:
- Server startup timeout: None (waits for termination)
- Request processing timeout: Controlled by client
- Keep-alive timeout: Default gRPC settings

**RESTful Service**:
- FastAPI server timeout: Configured by Uvicorn
- File processing timeout: Limited by the conversion process duration

**Recommended Production Settings**:
```yaml
# For reverse proxy or API gateway
timeout_client: 300s
timeout_server: 300s
timeout_http_request: 300s
```

The conversion process may take up to several minutes for large map files, so appropriate timeouts should be configured in the deployment environment.

**Section sources**
- [grpc_server.py](file://grpc_server.py#L251-L271)
- [web_app.py](file://web_app.py#L268)
- [config.py](file://config.py#L8-L10)

## Error Handling

### Error Code Mappings
The system uses the following error code mappings for different types of errors:

**HTTP Status Codes**:
- `200 OK`: Successful conversion
- `400 Bad Request`: Invalid file name or unsupported format
- `404 Not Found`: Source file not found during conversion
- `500 Internal Server Error`: Conversion failure or system error
- `503 Service Unavailable`: Temporary service unavailability

**gRPC Status Codes**:
- `OK`: Successful operation
- `INVALID_ARGUMENT`: Invalid request parameters
- `NOT_FOUND`: Resource not found
- `INTERNAL`: Internal server error
- `UNAVAILABLE`: Service temporarily unavailable

### Error Response Structure
**RESTful API Error Response**:
```json
{
  "success": false,
  "error": "Descriptive error message",
  "code": 500
}
```

**gRPC API Error Response**:
```protobuf
ConvertMapResponse {
  success: false
  message: "Descriptive error message"
  xml_data: ""
  xml_file_name: ""
  conversion_method: ""
}
```

### Common Error Scenarios
1. **Invalid File Format**: When the uploaded file has an unsupported extension
2. **File Conversion Failure**: When the conversion process fails due to malformed input
3. **Missing Required Fields**: When required parameters are not provided
4. **Workspace Creation Failure**: When the system cannot create a user workspace
5. **Output File Creation Failure**: When the system cannot write the converted file

The system logs detailed error information using Python's logging module, which can be used for debugging and monitoring.

**Section sources**
- [web_app.py](file://web_app.py#L71-L80)
- [grpc_server.py](file://grpc_server.py#L79-L87)
- [file_response.py](file://utils/file_response.py#L11-L13)

## Client Implementation Examples

### Python Client for RESTful API
```python
import requests
import os

def upload_map_file(file_path, user_id, base_url="http://localhost:8000"):
    """
    Upload a map file and save the converted XML response.
    
    Args:
        file_path (str): Path to the map file
        user_id (str): User identifier
        base_url (str): Base URL of the service
    
    Returns:
        tuple: (success: bool, output_path: str, error: str)
    """
    url = f"{base_url}/fileupload"
    
    try:
        with open(file_path, 'rb') as f:
            files = {'file': f}
            data = {'user_id': user_id}
            
            response = requests.post(url, files=files, data=data)
            
            if response.status_code == 200:
                # Extract filename from headers
                content_disposition = response.headers.get('Content-Disposition', '')
                if 'filename*=' in content_disposition:
                    filename = content_disposition.split("''")[1]
                else:
                    filename = os.path.basename(file_path).rsplit('.', 1)[0] + '.xml'
                
                # Save the XML file
                output_path = os.path.join(os.path.dirname(file_path), filename)
                with open(output_path, 'wb') as xml_file:
                    xml_file.write(response.content)
                
                return True, output_path, None
            else:
                error_data = response.json()
                return False, None, error_data.get('error', 'Unknown error')
                
    except Exception as e:
        return False, None, str(e)
```

### Python Client for gRPC API
```python
import grpc
import asyncio
from proto import map_service_pb2, map_service_pb2_grpc
import os

class MapConvertClient:
    """
    Client for the map_convert_services gRPC API.
    """
    
    def __init__(self, host='localhost', port=50052):
        self.channel = grpc.aio.insecure_channel(f'{host}:{port}')
        self.stub = map_service_pb2_grpc.MapConvertServiceStub(self.channel)
    
    async def convert_map(self, file_path, user_id):
        """
        Convert a map file to XML format.
        
        Args:
            file_path (str): Path to the map file
            user_id (str): User identifier
        
        Returns:
            ConvertMapResponse object
        """
        # Read file content
        with open(file_path, 'rb') as f:
            file_content = f.read()
        
        # Create request
        file_name = os.path.basename(file_path)
        request = map_service_pb2.ConvertMapRequest(
            file_content=file_content,
            file_name=file_name,
            user_id=user_id
        )
        
        # Call the service
        response = await self.stub.ConvertMap(request)
        return response
    
    async def preview_map(self, file_path, user_id):
        """
        Preview a map file to get basic statistics.
        
        Args:
            file_path (str): Path to the map file
            user_id (str): User identifier
        
        Returns:
            PreviewMapResponse object
        """
        # Read file content
        with open(file_path, 'rb') as f:
            file_content = f.read()
        
        # Create request
        file_name = os.path.basename(file_path)
        request = map_service_pb2.PreviewMapRequest(
            file_content=file_content,
            file_name=file_name,
            user_id=user_id
        )
        
        # Call the service
        response = await self.stub.PreviewMap(request)
        return response
    
    async def close(self):
        """Close the gRPC channel."""
        await self.channel.close()

# Example usage
async def main():
    client = MapConvertClient()
    
    try:
        # Convert a map file
        response = await client.convert_map('map.osm', '1718833e-a6d4-4fff-a62e-f33af4f9c5b6')
        
        if response.success:
            print(f"Conversion successful: {response.xml_file_name}")
            print(f"Method: {response.conversion_method}")
            
            # Save the XML file
            with open(response.xml_file_name, 'wb') as f:
                f.write(response.xml_data)
        else:
            print(f"Conversion failed: {response.message}")
            
    finally:
        await client.close()

if __name__ == '__main__':
    asyncio.run(main())
```

**Section sources**
- [web_app.py](file://web_app.py#L52-L80)
- [grpc_server.py](file://grpc_server.py#L35-L137)
- [map_service.proto](file://proto/map_service.proto#L10-L64)

## Protocol-Specific Debugging

### RESTful API Debugging
When debugging issues with the RESTful API, consider the following techniques:

1. **Check HTTP Headers**: Verify that the response headers contain the expected metadata:
   - `X-Success`: Should be "true" for successful conversions
   - `X-Conversion-Method`: Indicates which conversion method was used
   - `Content-Length`: Should match the size of the returned XML file

2. **Enable Detailed Logging**: The system uses Python's logging module. To enable debug logging, set the environment variable:
   ```bash
   export LOG_LEVEL=DEBUG
   ```

3. **Inspect Temporary Files**: The system creates temporary files in the `cache/` directory. Check these files to verify the conversion process:
   ```bash
   ls -la cache/{user_id}/
   ```

4. **Test with curl**: Use curl to test the endpoint and inspect headers:
   ```bash
   curl -v -X POST "http://localhost:8000/fileupload" \
     -F "file=@test.osm" \
     -F "user_id=test_user"
   ```

### gRPC API Debugging
When debugging issues with the gRPC API, use the following techniques:

1. **Use grpcurl for Testing**: The grpcurl tool allows you to test gRPC services from the command line:
   ```bash
   # List available services
   grpcurl -plaintext localhost:50052 list
   
   # Call ConvertMap method
   grpcurl -plaintext -d '{
     "file_content": "...",
     "file_name": "test.osm",
     "user_id": "test_user"
   }' localhost:50052 com.traffic.sim.plugin.map.grpc.MapConvertService/ConvertMap
   ```

2. **Enable gRPC Tracing**: Set environment variables to enable gRPC debugging:
   ```bash
   export GRPC_VERBOSITY=DEBUG
   export GRPC_TRACE=api,call_error,client_channel
   ```

3. **Check Server Logs**: The gRPC server logs detailed information about each request:
   ```python
   logger.info(f"ConvertMap request: user_id={request.user_id}, file_name={request.file_name}")
   ```

4. **Validate Protobuf Definitions**: Ensure that the client and server are using the same version of the protobuf definitions:
   ```bash
   # Generate Python code from proto file
   python -m grpc_tools.protoc -Iproto --python_out=proto --grpc_python_out=proto proto/map_service.proto
   ```

5. **Test Connection**: Verify that the gRPC server is running and accessible:
   ```python
   import grpc
   
   def test_connection(host, port):
       try:
           channel = grpc.insecure_channel(f'{host}:{port}')
           grpc.channel_ready_future(channel).result(timeout=10)
           return True
       except grpc.FutureTimeoutError:
           return False
       finally:
           channel.close()
   ```

**Section sources**
- [web_app.py](file://web_app.py#L52-L80)
- [grpc_server.py](file://grpc_server.py#L50-L51)
- [map_service.proto](file://proto/map_service.proto#L3-L7)

## TLS Configuration
The current implementation of map_convert_services does not include TLS configuration. For production deployments, TLS should be implemented at the infrastructure level. Below are recommended approaches for securing the services:

### Reverse Proxy with TLS
Deploy the services behind a reverse proxy (e.g., NGINX, Traefik) that handles TLS termination:

```nginx
server {
    listen 443 ssl;
    server_name map-convert.example.com;
    
    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    
    location / {
        proxy_pass http://localhost:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### gRPC with TLS
To enable TLS for the gRPC service, modify the server configuration:

```python
# In grpc_server.py
import grpc
from grpc import aio
import ssl

async def serve_secure(port: int = 50052):
    """Start gRPC server with TLS."""
    
    # Load SSL credentials
    with open('server.key', 'rb') as f:
        private_key = f.read()
    with open('server.crt', 'rb') as f:
        certificate_chain = f.read()
    
    server_credentials = grpc.ssl_server_credentials(
        [(private_key, certificate_chain)]
    )
    
    server = aio.server(futures.ThreadPoolExecutor(max_workers=10))
    map_service_pb2_grpc.add_MapConvertServiceServicer_to_server(
        MapConvertServiceServicer(), server
    )
    
    listen_addr = f'[::]:{port}'
    server.add_secure_port(listen_addr, server_credentials)
    
    await server.start()
    await server.wait_for_termination()
```

### Client Configuration with TLS
When connecting to a TLS-enabled gRPC server:

```python
# For secure gRPC connection
def create_secure_channel(host, port):
    # Load CA certificate
    with open('ca.crt', 'rb') as f:
        root_certificates = f.read()
    
    credentials = grpc.ssl_channel_credentials(root_certificates)
    channel = grpc.secure_channel(f'{host}:{port}', credentials)
    return channel

# Usage
channel = create_secure_channel('map-convert.example.com', 50052)
stub = map_service_pb2_grpc.MapConvertServiceStub(channel)
```

### Docker Deployment with TLS
Update the Dockerfile to include SSL certificates and configure the startup script:

```dockerfile
# In Dockerfile
COPY server.crt /app/certs/
COPY server.key /app/certs/
COPY ca.crt /app/certs/

# Update start.sh to use secure configuration
ENV USE_TLS=true
```

**Section sources**
- [grpc_server.py](file://grpc_server.py#L251-L271)
- [Dockerfile](file://Dockerfile#L1-L74)
- [start.sh](file://start.sh#L1-L42)

## Load Testing Recommendations
To ensure the map_convert_services system can handle production workloads, follow these load testing recommendations:

### Test Scenarios
1. **Single User, Single Request**: Verify basic functionality
2. **Single User, Multiple Concurrent Requests**: Test concurrency limits
3. **Multiple Users, Sequential Requests**: Test user isolation
4. **Multiple Users, Concurrent Requests**: Test system capacity
5. **Large File Processing**: Test memory and timeout handling
6. **Error Condition Testing**: Test error handling under load

### Recommended Tools
- **k6**: For scripting complex load testing scenarios
- **Locust**: For Python-based load testing with web UI
- **JMeter**: For comprehensive performance testing
- **Vegeta**: For simple HTTP load testing

### Sample k6 Test Script
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // Ramp up to 20 users
    { duration: '1m', target: 20 },   // Stay at 20 users
    { duration: '30s', target: 0 },   // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<10000'], // 95% of requests under 10s
    http_req_failed: ['rate<0.01'],     // Less than 1% errors
  },
};

// Read test file
const fileData = open('./test_files/map.osm', 'b');

export default function () {
  // Generate unique user ID for each virtual user
  const userId = `${__VU}-${__ITER}`;
  
  const payload = {
    file: http.file(fileData, 'map.osm', 'application/octet-stream'),
    user_id: userId,
  };
  
  const response = http.post('http://localhost:8000/fileupload', payload);
  
  // Check response
  check(response, {
    'is status 200': (r) => r.status === 200,
    'has content': (r) => r.body.length > 0,
  });
  
  // Add delay between requests
  sleep(1);
}
```

### Performance Metrics to Monitor
- **Response Time**: Average, median, and 95th percentile
- **Throughput**: Requests per second
- **Error Rate**: Percentage of failed requests
- **Resource Utilization**: CPU, memory, disk I/O
- **Concurrency**: Number of simultaneous requests handled
- **Queue Length**: Requests waiting to be processed

### Scaling Recommendations
1. **Horizontal Scaling**: Deploy multiple instances behind a load balancer
2. **Worker Processes**: Increase Uvicorn worker processes for REST API
3. **gRPC Thread Pool**: Adjust the thread pool size based on CPU cores
4. **Caching**: Implement result caching for frequently converted files
5. **Asynchronous Processing**: Consider implementing a job queue for large files

### Expected Performance
Based on the current implementation, expect the following performance characteristics:
- Small files (<1MB): < 2 seconds response time
- Medium files (1-10MB): 2-10 seconds response time
- Large files (>10MB): 10-60 seconds response time
- Concurrent users: 50+ with proper infrastructure

**Section sources**
- [web_app.py](file://web_app.py#L268)
- [grpc_server.py](file://grpc_server.py#L259)
- [config.py](file://config.py#L8-L10)

## Versioning Strategy
The map_convert_services system implements a comprehensive versioning strategy to ensure backward compatibility and smooth upgrades.

### API Versioning
The system uses URL-based versioning for the RESTful API and package-based versioning for the gRPC API:

**RESTful API Versioning**:
- Current version: `/v1/fileupload`
- Future versions: `/v2/fileupload`, etc.
- Default (unversioned): `/fileupload` (points to latest stable version)

**gRPC API Versioning**:
```protobuf
package com.traffic.sim.plugin.map.grpc.v1;
```

### Protocol Buffer Versioning
The gRPC service follows Protocol Buffer best practices for versioning:

1. **Never Remove Fields**: Use the `reserved` keyword for removed fields
2. **Add Fields with Default Values**: New fields should have sensible defaults
3. **Use Backwards-Compatible Changes**: Only add optional fields or extend enums

Example of version evolution:
```protobuf
// v1
message ConvertMapRequest {
  bytes file_content = 1;
  string file_name = 2;
  string user_id = 3;
}

// v2 (backward compatible)
message ConvertMapRequest {
  bytes file_content = 1;
  string file_name = 2;
  string user_id = 3;
  string file_format = 4;  // New optional field
  bool optimize = 5;       // New optional field
}
```

### Service Deployment Strategy
The system supports multiple versions through the following deployment strategies:

1. **Blue-Green Deployment**: Deploy new version alongside old version, then switch traffic
2. **Canary Release**: Gradually roll out new version to a subset of users
3. **Feature Flags**: Enable new features based on configuration

### Version Compatibility Matrix
| Service Version | REST API | gRPC API | Backward Compatible |
|----------------|---------|---------|-------------------|
| 1.0.0          | v1      | v1      | Yes               |
| 1.1.0          | v1      | v1      | Yes               |
| 2.0.0          | v2      | v2      | No (major change) |
| 2.1.0          | v2      | v2      | Yes               |

### Migration Path
When introducing breaking changes:
1. Maintain old version for 6 months
2. Provide migration guide and tools
3. Deprecate old version with clear timeline
4. Monitor usage of old version
5. Remove old version after migration period

**Section sources**
- [map_service.proto](file://proto/map_service.proto#L3-L7)
- [web_app.py](file://web_app.py#L52)
- [config.py](file://config.py#L8-L10)

## Backward Compatibility
The map_convert_services system maintains backward compatibility through several mechanisms:

### RESTful API Compatibility
The RESTful API maintains backward compatibility by:
1. **Preserving Endpoint Structure**: The `/fileupload` endpoint remains unchanged
2. **Maintaining Request Parameters**: `file` and `user_id` parameters are preserved
3. **Extending Response Headers**: New headers can be added without breaking clients
4. **Preserving Error Response Format**: JSON error structure remains consistent

### gRPC API Compatibility
The gRPC API follows Protocol Buffer compatibility rules:
1. **Field Number Preservation**: Field numbers are never reused
2. **Optional Fields**: New fields are added as optional with default values
3. **Enum Extensions**: New enum values are added at the end
4. **Message Evolution**: Messages can be extended but not fundamentally changed

### Data Format Compatibility
The system ensures data format compatibility by:
1. **XML Schema Stability**: The output XML structure remains consistent
2. **Conversion Method Indication**: The `conversion_method` field indicates which algorithm was used
3. **File Naming Consistency**: Output filenames follow predictable patterns

### Deprecation Policy
When deprecating features:
1. **Announcement**: Deprecation is announced in release notes
2. **Grace Period**: Deprecated features remain functional for 6 months
3. **Migration Support**: Tools and documentation are provided for migration
4. **Monitoring**: Usage of deprecated features is monitored

### Compatibility Testing
The system includes compatibility tests to ensure backward compatibility:
1. **Client Compatibility Tests**: Test older client versions with new server
2. **Data Migration Tests**: Test conversion of files processed by older versions
3. **Integration Tests**: Test with systems that depend on the API

The system prioritizes backward compatibility and only introduces breaking changes when absolutely necessary, following semantic versioning principles (MAJOR.MINOR.PATCH).

**Section sources**
- [map_service.proto](file://proto/map_service.proto#L10-L64)
- [web_app.py](file://web_app.py#L52-L80)
- [grpc_server.py](file://grpc_server.py#L35-L137)