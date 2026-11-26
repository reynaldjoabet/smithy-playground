# Amazon API Gateway V2 (HTTP and WebSocket APIs)

## Wire protocol

- **Protocol:** `restJson1`
- **SDK id:** `ApiGatewayV2`
- **sigv4 signing name:** `apigateway`
- **Endpoint prefix:** `apigateway`
- **ARN namespace:** `apigateway`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `apigateway`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `GetApis`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:apigateway" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  'https://127.0.0.1:9297/v2/apis'
```

## End-to-end walkthrough

Create an HTTP API with a route backed by an HTTP proxy integration and a stage. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateApi

Answers 201 with the API's id and its `apiEndpoint`, which keeps the AWS `execute-api` hostname format.

**Request**

```http
POST /v2/apis HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182725Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "name": "orders-api",
  "protocolType": "HTTP"
}
```

**Response**

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 390
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "apiEndpoint": "https://127.0.0.1:39581/_aws/execute-api/mp67t4dbd4",
  "apiId": "mp67t4dbd4",
  "apiKeySelectionExpression": "$request.header.x-api-key",
  "createdDate": "2026-11-26T18:27:26.003782Z",
  "disableSchemaValidation": false,
  "disableExecuteApiEndpoint": false,
  "ipAddressType": "ipv4",
  "name": "orders-api",
  "protocolType": "HTTP",
  "routeSelectionExpression": "$request.method $request.path",
  "tags": {}
}
```

### 2. CreateIntegration

An integration is the backend a route forwards to.

**Request**

```http
POST /v2/apis/mp67t4dbd4/integrations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 133
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "integrationMethod": "GET",
  "integrationType": "HTTP_PROXY",
  "integrationUri": "https://example.com/orders",
  "payloadFormatVersion": "1.0"
}
```

**Response**

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 280
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "connectionType": "INTERNET",
  "integrationId": "ur7ayid",
  "integrationMethod": "GET",
  "integrationType": "HTTP_PROXY",
  "integrationUri": "https://example.com/orders",
  "payloadFormatVersion": "1.0",
  "requestParameters": {},
  "requestTemplates": {},
  "responseParameters": {},
  "timeoutInMillis": 30000
}
```

### 3. CreateRoute

A route key is the HTTP method and path; `Target` wires it to the integration.

**Request**

```http
POST /v2/apis/mp67t4dbd4/routes HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "routeKey": "GET /orders",
  "target": "integrations/ur7ayid"
}
```

**Response**

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 128
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "apiKeyRequired": false,
  "authorizationType": "NONE",
  "routeId": "t1iw09q",
  "routeKey": "GET /orders",
  "target": "integrations/ur7ayid"
}
```

### 4. CreateStage

`$default` with auto-deploy serves the API at its root URL.

**Request**

```http
POST /v2/apis/mp67t4dbd4/stages HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 42
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "autoDeploy": true,
  "stageName": "$default"
}
```

**Response**

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 351
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "autoDeploy": true,
  "createdDate": "2026-11-26T18:27:26.008172Z",
  "defaultRouteSettings": {
    "detailedMetricsEnabled": false
  },
  "deploymentId": "wjslhw",
  "lastDeploymentStatusMessage": "Successfully deployed stage with deployment id 'wjslhw'",
  "lastUpdatedDate": "2026-11-26T18:27:26.008172Z",
  "routeSettings": {},
  "stageName": "$default",
  "stageVariables": {},
  "tags": {}
}
```

### 5. GetRoutes

Lists the API's routes.

**Request**

```http
GET /v2/apis/mp67t4dbd4/routes HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 140
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "items": [
    {
      "apiKeyRequired": false,
      "authorizationType": "NONE",
      "routeId": "t1iw09q",
      "routeKey": "GET /orders",
      "target": "integrations/ur7ayid"
    }
  ]
}
```

### 6. GetApi

An unknown id answers 404 `NotFoundException`.

**Request**

```http
GET /v2/apis/nosuchapi HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 98
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:nosuchapi"
}
```

### 7. DeleteApi

Deleting the API removes its routes, integrations and stages; 204 No Content.

**Request**

```http
DELETE /v2/apis/mp67t4dbd4 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 204 No Content
content-type: application/json
date: Thu, 26 Nov 2026 18:27:26 GMT
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 103 operations, 8 accepted the placeholder call, 4 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### CreateApi

Creates an Api resource.

```http
POST /v2/apis HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "name": "example",
  "protocolType": "WEBSOCKET"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 77
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid routeSelectionExpression"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createapi) for a successful call.

### CreateApiMapping

Creates an API mapping.

```http
POST /v2/domainnames/example/apimappings HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 37
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "apiId": "example",
  "stage": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateAuthorizer

Creates an Authorizer for an API.

```http
POST /v2/apis/example/authorizers HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 74
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "authorizerType": "REQUEST",
  "identitySource": [
    "example"
  ],
  "name": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateDeployment

Creates a Deployment for an API.

```http
POST /v2/apis/example/deployments HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateDomainName

Creates a domain name.

```http
POST /v2/domainnames HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "domainName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 98
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid input. Expected one domain name configuration"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### CreateIntegration

Creates an Integration.

```http
POST /v2/apis/example/integrations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "integrationType": "AWS"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#2-createintegration) for a successful call.

### CreateIntegrationResponse

Creates an IntegrationResponses.

```http
POST /v2/apis/example/integrations/example/integrationresponses HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 36
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "integrationResponseKey": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateModel

Creates a Model for an API.

```http
POST /v2/apis/example/models HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 37
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "name": "example",
  "schema": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreatePortal

Creates a portal.

```http
POST /v2/portals HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 275
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "authorization": {},
  "endpointConfiguration": {},
  "portalContent": {
    "displayName": "example",
    "theme": {
      "customColors": {
        "accentColor": "example",
        "backgroundColor": "example",
        "errorValidationColor": "example",
        "headerColor": "example",
        "navigationColor": "example",
        "textColor": "example"
      }
    }
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 108
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "Authorization must specify exactly one of: CognitoConfig, None."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### CreatePortalProduct

Creates a new portal product.

```http
POST /v2/portalproducts HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "displayName": "example"
}
```

```http
HTTP/1.1 201 Created
content-type: application/json
content-length: 253
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "displayName": "example",
  "displayOrder": {
    "contents": [],
    "productPageArns": []
  },
  "lastModified": "2026-11-26T18:27:26.021346Z",
  "portalProductArn": "arn:aws:apigateway:us-east-1:000000000000:/portalproducts/cetidi75uc",
  "portalProductId": "cetidi75uc",
  "tags": {}
}
```

Accepted: the emulator answered `201 Created` even with placeholder values.

### CreateProductPage

Creates a new product page for a portal product.

```http
POST /v2/portalproducts/example/productpages HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "displayContent": {
    "body": "example",
    "title": "example"
  }
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 100
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Product not found in account 000000000000 with id example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateProductRestEndpointPage

Creates a product REST endpoint page for a portal product.

```http
POST /v2/portalproducts/example/productrestendpointpages HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 29
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "restEndpointIdentifier": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 113
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "RestEndpointIdentifier must specify exactly one of: IdentifierParts."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### CreateRoute

Creates a Route for an API.

```http
POST /v2/apis/example/routes HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "routeKey": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#3-createroute) for a successful call.

### CreateRouteResponse

Creates a RouteResponse for a Route.

```http
POST /v2/apis/example/routes/example/routeresponses HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 30
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "routeResponseKey": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateRoutingRule

Creates a RoutingRule.

```http
POST /v2/domainnames/example/routingrules HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "actions": [
    {
      "invokeApi": {
        "apiId": "example",
        "stage": "example"
      }
    }
  ],
  "conditions": [
    {}
  ],
  "priority": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 146
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid conditions for RoutingRule. Each condition can only be either matchBasePaths or matchHeaders."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### CreateStage

Creates a Stage for an API.

```http
POST /v2/apis/example/stages HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "stageName": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#4-createstage) for a successful call.

### CreateVpcLink

Creates a VPC link.

```http
POST /v2/vpclinks HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 42
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "name": "example",
  "subnetIds": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 156
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid value for SubnetIds: 'example'. Each value must be of the form subnet-<8 or 17 hexadecimal characters>."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### DeleteAccessLogSettings

Deletes the AccessLogSettings for a Stage.

```http
DELETE /v2/apis/example/stages/example/accesslogsettings HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteApi

Deletes an Api resource.

```http
DELETE /v2/apis/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deleteapi) for a successful call.

### DeleteApiMapping

Deletes an API mapping.

```http
DELETE /v2/domainnames/example/apimappings/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteAuthorizer

Deletes an Authorizer.

```http
DELETE /v2/apis/example/authorizers/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteCorsConfiguration

Deletes a CORS configuration.

```http
DELETE /v2/apis/example/cors HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDeployment

Deletes a Deployment.

```http
DELETE /v2/apis/example/deployments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 82
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid Deployment identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDomainName

Deletes a domain name.

```http
DELETE /v2/domainnames/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteIntegration

Deletes an Integration.

```http
DELETE /v2/apis/example/integrations/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteIntegrationResponse

Deletes an IntegrationResponses.

```http
DELETE /v2/apis/example/integrations/example/integrationresponses/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteModel

Deletes a Model.

```http
DELETE /v2/apis/example/models/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeletePortal

Deletes a portal.

```http
DELETE /v2/portals/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 204 No Content
content-type: application/json
date: Thu, 26 Nov 2026 18:27:26 GMT
```

Accepted: the emulator answered `204 No Content` even with placeholder values.

### DeletePortalProduct

Deletes a portal product.

```http
DELETE /v2/portalproducts/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 204 No Content
content-type: application/json
date: Thu, 26 Nov 2026 18:27:26 GMT
```

Accepted: the emulator answered `204 No Content` even with placeholder values.

### DeletePortalProductSharingPolicy

Deletes the sharing policy for a portal product.

```http
DELETE /v2/portalproducts/example/sharingpolicy HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 100
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Product not found in account 000000000000 with id example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteProductPage

Deletes a product page of a portal product.

```http
DELETE /v2/portalproducts/example/productpages/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 100
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Product not found in account 000000000000 with id example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteProductRestEndpointPage

Deletes a product REST endpoint page.

```http
DELETE /v2/portalproducts/example/productrestendpointpages/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 100
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Product not found in account 000000000000 with id example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRoute

Deletes a Route.

```http
DELETE /v2/apis/example/routes/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRouteRequestParameter

Deletes a route request parameter.

```http
DELETE /v2/apis/example/routes/example/requestparameters/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRouteResponse

Deletes a RouteResponse.

```http
DELETE /v2/apis/example/routes/example/routeresponses/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRouteSettings

Deletes the RouteSettings for a stage.

```http
DELETE /v2/apis/example/stages/example/routesettings/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRoutingRule

Deletes a routing rule.

```http
DELETE /v2/domainnames/example/routingrules/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteStage

Deletes a Stage.

```http
DELETE /v2/apis/example/stages/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVpcLink

Deletes a VPC link.

```http
DELETE /v2/vpclinks/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 80
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid Vpc Link identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisablePortal

Deletes the publication of a portal portal.

```http
DELETE /v2/portals/example/publish HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 59
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Portal not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ExportApi

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /v2/apis/example/exports/example?outputType=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 125
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid export type detected. Currently, the only specification version is OAS30"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### GetApi

Gets an Api resource.

```http
GET /v2/apis/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetApiMapping

Gets an API mapping.

```http
GET /v2/domainnames/example/apimappings/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetApiMappings

Gets API mappings.

```http
GET /v2/domainnames/example/apimappings HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetApis

Gets a collection of Api resources.

```http
GET /v2/apis HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 12
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "items": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetAuthorizer

Gets an Authorizer.

```http
GET /v2/apis/example/authorizers/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 82
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid Authorizer identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetAuthorizers

Gets the Authorizers for an API.

```http
GET /v2/apis/example/authorizers HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetDeployment

Gets a Deployment.

```http
GET /v2/apis/example/deployments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 82
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid Deployment identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetDeployments

Gets the Deployments for an API.

```http
GET /v2/apis/example/deployments HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetDomainName

Gets a domain name.

```http
GET /v2/domainnames/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetDomainNames

Gets the domain names for an AWS account.

```http
GET /v2/domainnames HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 12
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "items": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetIntegration

Gets an Integration.

```http
GET /v2/apis/example/integrations/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid Integration identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetIntegrationResponse

Gets an IntegrationResponses.

```http
GET /v2/apis/example/integrations/example/integrationresponses/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetIntegrationResponses

Gets the IntegrationResponses for an Integration.

```http
GET /v2/apis/example/integrations/example/integrationresponses HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetIntegrations

Gets the Integrations for an API.

```http
GET /v2/apis/example/integrations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetModel

Gets a Model.

```http
GET /v2/apis/example/models/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 86
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid model identifier specified: example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetModelTemplate

Gets a model template.

```http
GET /v2/apis/example/models/example/template HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 86
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid model identifier specified: example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetModels

Gets the Models for an API.

```http
GET /v2/apis/example/models HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetPortal

Gets a portal.

```http
GET /v2/portals/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 59
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Portal not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetPortalProduct

Gets a portal product.

```http
GET /v2/portalproducts/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 100
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Product not found in account 000000000000 with id example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetPortalProductSharingPolicy

Gets the sharing policy for a portal product.

```http
GET /v2/portalproducts/example/sharingpolicy HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 100
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Product not found in account 000000000000 with id example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetProductPage

Gets a product page of a portal product.

```http
GET /v2/portalproducts/example/productpages/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 64
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "ProductPage not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetProductRestEndpointPage

Gets a product REST endpoint page.

```http
GET /v2/portalproducts/example/productrestendpointpages/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 76
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "ProductRestEndpointPage not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRoute

Gets a Route.

```http
GET /v2/apis/example/routes/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRouteResponse

Gets a RouteResponse.

```http
GET /v2/apis/example/routes/example/routeresponses/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRouteResponses

Gets the RouteResponses for a Route.

```http
GET /v2/apis/example/routes/example/routeresponses HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRoutes

Gets the Routes for an API.

```http
GET /v2/apis/example/routes HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#5-getroutes) for a successful call.

### GetRoutingRule

Gets a routing rule.

```http
GET /v2/domainnames/example/routingrules/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetStage

Gets a Stage.

```http
GET /v2/apis/example/stages/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetStages

Gets the Stages for an API.

```http
GET /v2/apis/example/stages HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTags

Gets a collection of Tag resources.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /v2/tags/arn%3Aaws%3Aapigatewayv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 81
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid ARN specified in the request"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### GetVpcLink

Gets a VPC link.

```http
GET /v2/vpclinks/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 80
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid Vpc Link identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetVpcLinks

Gets a collection of VPC links.

```http
GET /v2/vpclinks HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 12
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "items": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ImportApi

Imports an API.

```http
PUT /v2/apis HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 18
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "body": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 122
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "Unable to build importer with provided input. Unable to determine OAS version"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ListPortalProducts

Lists portal products.

```http
GET /v2/portalproducts HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 307
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "items": [
    {
      "description": "",
      "displayName": "example",
      "lastModified": "2026-11-26T18:27:26.021346Z",
      "portalProductArn": "arn:aws:apigateway:us-east-1:000000000000:/portalproducts/cetidi75uc",
      "portalProductId": "cetidi75uc",
      "tags": {}
    }
  ],
  "nextToken": "706f7274616c70726f64756374732f4f544845525f4143434f554e54531f30"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListPortals

Lists portals.

```http
GET /v2/portals HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 12
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "items": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListProductPages

Lists the product pages for a portal product.

```http
GET /v2/portalproducts/example/productpages HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 117
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListProductPages is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListProductRestEndpointPages

Lists the product REST endpoint pages of a portal product.

```http
GET /v2/portalproducts/example/productrestendpointpages HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 129
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListProductRestEndpointPages is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListRoutingRules

Lists routing rules.

```http
GET /v2/domainnames/example/routingrules HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PreviewPortal

Creates a portal preview.

```http
POST /v2/portals/example/preview HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 59
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Portal not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PublishPortal

Publishes a portal.

```http
POST /v2/portals/example/publish HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 59
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Portal not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PutPortalProductSharingPolicy

Updates the sharing policy for a portal product.

```http
PUT /v2/portalproducts/example/sharingpolicy HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "policyDocument": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 100
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Product not found in account 000000000000 with id example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PutRoutingRule

Updates a routing rule.

```http
PUT /v2/domainnames/example/routingrules/example HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "actions": [
    {
      "invokeApi": {
        "apiId": "example",
        "stage": "example"
      }
    }
  ],
  "conditions": [
    {}
  ],
  "priority": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 146
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid conditions for RoutingRule. Each condition can only be either matchBasePaths or matchHeaders."
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### ReimportApi

Puts an Api resource.

```http
PUT /v2/apis/example HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 18
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "body": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ResetAuthorizersCache

Resets all authorizer cache entries on a stage.

```http
DELETE /v2/apis/example/stages/example/cache/authorizers HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagResource

Creates a new Tag resource to represent a tag.

```http
POST /v2/tags/arn%3Aaws%3Aapigatewayv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 112
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotImplemented",
  "message": "operation TagResource is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UntagResource

Deletes a Tag.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
DELETE /v2/tags/arn%3Aaws%3Aapigatewayv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample?tagKeys=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: BadRequestException
content-length: 81
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "BadRequestException",
  "message": "Invalid ARN specified in the request"
}
```

`400` `BadRequestException`: the placeholder values failed the operation's input validation.

### UpdateApi

Updates an Api resource.

```http
PATCH /v2/apis/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateApiMapping

The API mapping.

```http
PATCH /v2/domainnames/example/apimappings/example HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Content-Length: 19
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "apiId": "example"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateAuthorizer

Updates an Authorizer.

```http
PATCH /v2/apis/example/authorizers/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateDeployment

Updates a Deployment.

```http
PATCH /v2/apis/example/deployments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 117
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotImplemented",
  "message": "operation UpdateDeployment is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateDomainName

Updates a domain name.

```http
PATCH /v2/domainnames/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 83
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid domain name identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateIntegration

Updates an Integration.

```http
PATCH /v2/apis/example/integrations/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateIntegrationResponse

Updates an IntegrationResponses.

```http
PATCH /v2/apis/example/integrations/example/integrationresponses/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateModel

Updates a Model.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
PATCH /v2/apis/example/models/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdatePortal

Updates a portal.

```http
PATCH /v2/portals/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 59
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Portal not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdatePortalProduct

Updates the portal product.

```http
PATCH /v2/portalproducts/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 100
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Product not found in account 000000000000 with id example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateProductPage

Updates a product page of a portal product.

```http
PATCH /v2/portalproducts/example/productpages/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 64
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "ProductPage not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateProductRestEndpointPage

Updates a product REST endpoint page.

```http
PATCH /v2/portalproducts/example/productrestendpointpages/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 76
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "ProductRestEndpointPage not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateRoute

Updates a Route.

```http
PATCH /v2/apis/example/routes/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateRouteResponse

Updates a RouteResponse.

```http
PATCH /v2/apis/example/routes/example/routeresponses/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateStage

Updates a Stage.

```http
PATCH /v2/apis/example/stages/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 96
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid API identifier specified 000000000000:example"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateVpcLink

Updates a VPC link.

```http
PATCH /v2/vpclinks/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182726Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/apigateway/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException
content-length: 80
date: Thu, 26 Nov 2026 18:27:26 GMT

{
  "__type": "NotFoundException",
  "message": "Invalid Vpc Link identifier specified"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/apigateway/aws4_request` in the `Authorization` header) and the operation from the request itself: an HTTP method and path per operation (the `HTTP binding`), with identifiers in the path, options in the query string or headers, and the rest of the input as a JSON body.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon API Gateway V2 (HTTP and WebSocket APIs) answers in AWS: a JSON body and an HTTP status chosen per operation. Errors captured on this page are a JSON body with `__type`, `message`, plus the `x-amzn-errortype` header.
