# Amazon Elastic Container Service

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AmazonEC2ContainerServiceV20141113.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `ECS`
- **sigv4 signing name:** `ecs`
- **Endpoint prefix:** `ecs`
- **ARN namespace:** `ecs`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `ecs`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListClusters`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:ecs" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListClusters' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a cluster, register a Fargate task definition, run a task, then clean up. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateCluster

A cluster is a namespace for services and tasks.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "clusterName": "docs-demo"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 412
date: Mon, 16 Nov 2026 12:05:34 GMT

{
  "cluster": {
    "clusterArn": "arn:aws:ecs:us-east-1:000000000000:cluster/docs-demo",
    "clusterName": "docs-demo",
    "status": "ACTIVE",
    "registeredContainerInstancesCount": 0,
    "runningTasksCount": 0,
    "pendingTasksCount": 0,
    "activeServicesCount": 0,
    "statistics": [],
    "tags": [],
    "settings": [
      {
        "name": "containerInsights",
        "value": "disabled"
      }
    ],
    "capacityProviders": [],
    "defaultCapacityProviderStrategy": [],
    "attachments": []
  },
  "clusterCount": 1
}
```

### 2. RegisterTaskDefinition

Each registration creates a new revision of the family (`docs-demo-web:1`).

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.RegisterTaskDefinition
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 272
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "family": "docs-demo-web",
  "networkMode": "awsvpc",
  "containerDefinitions": [
    {
      "name": "web",
      "image": "public.ecr.aws/nginx/nginx:latest",
      "portMappings": [
        {
          "containerPort": 80
        }
      ],
      "essential": true
    }
  ],
  "requiresCompatibilities": [
    "FARGATE"
  ],
  "cpu": "256",
  "memory": "512"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 801
date: Mon, 16 Nov 2026 12:05:34 GMT

{
  "taskDefinition": {
    "taskDefinitionArn": "arn:aws:ecs:us-east-1:000000000000:task-definition/docs-demo-web:1",
    "containerDefinitions": [
      {
        "name": "web",
        "image": "public.ecr.aws/nginx/nginx:latest",
        "cpu": 0,
        "portMappings": [
          {
            "containerPort": 80,
            "hostPort": 80,
            "protocol": "tcp"
          }
        ],
        "essential": true,
        "environment": [],
        "mountPoints": [],
        "volumesFrom": [],
        "systemControls": []
      }
    ],
    "family": "docs-demo-web",
    "networkMode": "awsvpc",
    "revision": 1,
    "volumes": [],
    "status": "ACTIVE",
    "requiresAttributes": [
      {
        "name": "com.amazonaws.ecs.capability.docker-remote-api.1.18"
      },
      {
        "name": "ecs.capability.task-eni"
      }
    ],
    "placementConstraints": [],
    "compatibilities": [
      "EC2",
      "FARGATE"
    ],
    "requiresCompatibilities": [
      "FARGATE"
    ],
    "cpu": "256",
    "memory": "512",
    "registeredAt": 1794830734.1675153,
    "registeredBy": "arn:aws:iam::000000000000:user/emulator"
  },
  "tags": []
}
```

### 3. RunTask

The task starts in `PROVISIONING`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.RunTask
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 325
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "docs-demo",
  "count": 1,
  "launchType": "FARGATE",
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": [
        "subnet-00000000000000007"
      ],
      "assignPublicIp": "ENABLED"
    }
  },
  "taskDefinition": "arn:aws:ecs:us-east-1:000000000000:task-definition/docs-demo-web:1",
  "clientToken": "73e0bc44-cdcf-4b55-8c59-21acb96db6ec"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 1392
date: Mon, 16 Nov 2026 12:05:34 GMT

{
  "tasks": [
    {
      "attachments": [
        {
          "id": "00000001-0000-0000-0000-000000000001",
          "type": "ElasticNetworkInterface",
          "status": "PRECREATED",
          "details": [
            {
              "name": "subnetId",
              "value": "subnet-00000000000000007"
            },
            {
              "name": "networkInterfaceId",
              "value": "eni-00000000000000001"
            },
            {
              "name": "macAddress",
              "value": "0a:00:00:00:00:01"
            },
            {
              "name": "privateIPv4Address",
              "value": "172.31.0.5"
            }
          ]
        }
      ],
      "clusterArn": "arn:aws:ecs:us-east-1:000000000000:cluster/docs-demo",
      "containers": [
        {
          "containerArn": "arn:aws:ecs:us-east-1:000000000000:container/docs-demo/00000000000000000000000000000001/00000000000000000000000000000001",
          "taskArn": "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001",
          "name": "web",
          "image": "public.ecr.aws/nginx/nginx:latest",
          "lastStatus": "PROVISIONING",
          "networkBindings": [],
          "networkInterfaces": [
            {
              "attachmentId": "00000001-0000-0000-0000-000000000001",
              "privateIpv4Address": "172.31.0.5"
            }
          ],
          "healthStatus": "UNKNOWN",
          "managedAgents": [],
          "cpu": "0"
        }
      ],
      "cpu": "256",
      "createdAt": 1794830734.1777656,
      "desiredStatus": "RUNNING",
      "enableExecuteCommand": false,
      "group": "family:docs-demo-web",
      "healthStatus": "UNKNOWN",
      "lastStatus": "PROVISIONING",
      "launchType": "FARGATE",
      "memory": "512",
      "tags": [],
      "taskArn": "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001",
      "taskDefinitionArn": "arn:aws:ecs:us-east-1:000000000000:task-definition/docs-demo-web:1",
… (5 more lines)
```

### 4. DescribeTasks

The task's last status and its containers.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeTasks
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 121
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "docs-demo",
  "tasks": [
    "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 1456
date: Mon, 16 Nov 2026 12:05:34 GMT

{
  "tasks": [
    {
      "attachments": [
        {
          "id": "00000001-0000-0000-0000-000000000001",
          "type": "ElasticNetworkInterface",
          "status": "PRECREATED",
          "details": [
            {
              "name": "subnetId",
              "value": "subnet-00000000000000007"
            },
            {
              "name": "networkInterfaceId",
              "value": "eni-00000000000000001"
            },
            {
              "name": "macAddress",
              "value": "0a:00:00:00:00:01"
            },
            {
              "name": "privateIPv4Address",
              "value": "172.31.0.5"
            }
          ]
        }
      ],
      "clusterArn": "arn:aws:ecs:us-east-1:000000000000:cluster/docs-demo",
      "containers": [
        {
          "containerArn": "arn:aws:ecs:us-east-1:000000000000:container/docs-demo/00000000000000000000000000000001/00000000000000000000000000000001",
          "taskArn": "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001",
          "name": "web",
          "image": "public.ecr.aws/nginx/nginx:latest",
          "lastStatus": "PROVISIONING",
          "networkBindings": [],
          "networkInterfaces": [
            {
              "attachmentId": "00000001-0000-0000-0000-000000000001",
              "privateIpv4Address": "172.31.0.5"
            }
          ],
          "healthStatus": "UNKNOWN",
          "managedAgents": [],
          "cpu": "0"
        }
      ],
      "cpu": "256",
      "createdAt": 1794830734.1777656,
      "desiredStatus": "RUNNING",
      "enableExecuteCommand": false,
      "group": "family:docs-demo-web",
      "healthStatus": "UNKNOWN",
      "lastStatus": "PROVISIONING",
      "launchType": "FARGATE",
      "memory": "512",
      "pullStartedAt": 1794830734.1791248,
      "pullStoppedAt": 1794830734.1791248,
      "taskArn": "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001",
… (6 more lines)
```

### 5. StopTask

Stopping records the reason.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.StopTask
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 141
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "docs-demo",
  "task": "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001",
  "reason": "docs demo"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 1538
date: Mon, 16 Nov 2026 12:05:34 GMT

{
  "task": {
    "attachments": [
      {
        "id": "00000001-0000-0000-0000-000000000001",
        "type": "ElasticNetworkInterface",
        "status": "PRECREATED",
        "details": [
          {
            "name": "subnetId",
            "value": "subnet-00000000000000007"
          },
          {
            "name": "networkInterfaceId",
            "value": "eni-00000000000000001"
          },
          {
            "name": "macAddress",
            "value": "0a:00:00:00:00:01"
          },
          {
            "name": "privateIPv4Address",
            "value": "172.31.0.5"
          }
        ]
      }
    ],
    "clusterArn": "arn:aws:ecs:us-east-1:000000000000:cluster/docs-demo",
    "containers": [
      {
        "containerArn": "arn:aws:ecs:us-east-1:000000000000:container/docs-demo/00000000000000000000000000000001/00000000000000000000000000000001",
        "taskArn": "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001",
        "name": "web",
        "image": "public.ecr.aws/nginx/nginx:latest",
        "lastStatus": "DEACTIVATING",
        "networkBindings": [],
        "networkInterfaces": [
          {
            "attachmentId": "00000001-0000-0000-0000-000000000001",
            "privateIpv4Address": "172.31.0.5"
          }
        ],
        "healthStatus": "UNKNOWN",
        "managedAgents": [],
        "cpu": "0"
      }
    ],
    "cpu": "256",
    "createdAt": 1794830734.1777656,
    "desiredStatus": "STOPPED",
    "enableExecuteCommand": false,
    "group": "family:docs-demo-web",
    "healthStatus": "UNKNOWN",
    "lastStatus": "DEACTIVATING",
    "launchType": "FARGATE",
    "memory": "512",
    "pullStartedAt": 1794830734.1791248,
    "pullStoppedAt": 1794830734.1791248,
    "stopCode": "UserInitiated",
    "stoppedReason": "docs demo",
… (7 more lines)
```

### 6. DeregisterTaskDefinition

The revision becomes `INACTIVE`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeregisterTaskDefinition
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 88
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "taskDefinition": "arn:aws:ecs:us-east-1:000000000000:task-definition/docs-demo-web:1"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 831
date: Mon, 16 Nov 2026 12:05:34 GMT

{
  "taskDefinition": {
    "taskDefinitionArn": "arn:aws:ecs:us-east-1:000000000000:task-definition/docs-demo-web:1",
    "containerDefinitions": [
      {
        "name": "web",
        "image": "public.ecr.aws/nginx/nginx:latest",
        "cpu": 0,
        "portMappings": [
          {
            "containerPort": 80,
            "hostPort": 80,
            "protocol": "tcp"
          }
        ],
        "essential": true,
        "environment": [],
        "mountPoints": [],
        "volumesFrom": [],
        "systemControls": []
      }
    ],
    "family": "docs-demo-web",
    "networkMode": "awsvpc",
    "revision": 1,
    "volumes": [],
    "status": "INACTIVE",
    "requiresAttributes": [
      {
        "name": "com.amazonaws.ecs.capability.docker-remote-api.1.18"
      },
      {
        "name": "ecs.capability.task-eni"
      }
    ],
    "placementConstraints": [],
    "compatibilities": [
      "EC2",
      "FARGATE"
    ],
    "requiresCompatibilities": [
      "FARGATE"
    ],
    "cpu": "256",
    "memory": "512",
    "registeredAt": 1794830734.1675153,
    "deregisteredAt": 1794830734.1955457,
    "registeredBy": "arn:aws:iam::000000000000:user/emulator"
  }
}
```

### 7. DeleteCluster

With its task stopped, the cluster can be deleted; it is reported `INACTIVE`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "docs-demo"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 414
date: Mon, 16 Nov 2026 12:05:34 GMT

{
  "cluster": {
    "clusterArn": "arn:aws:ecs:us-east-1:000000000000:cluster/docs-demo",
    "clusterName": "docs-demo",
    "status": "INACTIVE",
    "registeredContainerInstancesCount": 0,
    "runningTasksCount": 0,
    "pendingTasksCount": 0,
    "activeServicesCount": 0,
    "statistics": [],
    "tags": [],
    "settings": [
      {
        "name": "containerInsights",
        "value": "disabled"
      }
    ],
    "capacityProviders": [],
    "defaultCapacityProviderStrategy": [],
    "attachments": []
  },
  "clusterCount": 0
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 88 operations, 21 accepted the placeholder call, 14 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AssociateGatewayWithCluster

Associates a gateway with an Amazon ECS cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.AssociateGatewayWithCluster

{
  "gateway": "example",
  "cluster": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation AssociateGatewayWithCluster is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ContinueServiceDeployment

Continues or rolls back an Amazon ECS service deployment that is paused at a lifecycle hook.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ContinueServiceDeployment

{
  "serviceDeploymentArn": "arn:aws:ecs:us-east-1:000000000000:example/example",
  "hookId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "The ServiceDeployment ARN is invalid."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateCapacityProvider

Creates a capacity provider.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateCapacityProvider

{
  "name": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "either autoScalingGroupProvider or managedInstancesProvider must be specified"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateCluster

Creates a new Amazon ECS cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateCluster

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "cluster": {
    "clusterArn": "arn:aws:ecs:us-east-1:000000000000:cluster/default",
    "clusterName": "default",
    "status": "ACTIVE",
    "registeredContainerInstancesCount": 0,
    "runningTasksCount": 0,
    "pendingTasksCount": 0,
    "activeServicesCount": 0,
    "statistics": [],
    "tags": [],
    "settings": [
      {
        "name": "containerInsights",
        "value": "disabled"
      }
    ],
    "capacityProviders": [],
    "defaultCapacityProviderStrategy": [],
    "attachments": []
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-createcluster) for a successful call.

### CreateDaemon

Creates a new daemon in the specified cluster and capacity providers.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateDaemon

{
  "daemonName": "example",
  "daemonTaskDefinitionArn": "arn:aws:ecs:us-east-1:000000000000:example/example",
  "capacityProviderArns": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "The specified capacity provider was not found. Specify a valid capacity provider and try again."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateExpressGatewayService

Creates an Express service that simplifies deploying containerized web applications on Amazon ECS with managed Amazon Web Services infrastructure.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateExpressGatewayService

{
  "infrastructureRoleArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "either primaryContainer or taskDefinitionArn must be specified"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateGateway

Creates a gateway.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateGateway

{
  "name": "example",
  "infrastructureRoleArn": "arn:aws:ecs:us-east-1:000000000000:example/example",
  "clientToken": "51301a90-bff8-4a69-9fb7-059f20ea8387",
  "configuration": {
    "hostSharedAlb": {}
  }
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation CreateGateway is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateService

Runs and maintains your desired number of tasks from a specified task definition.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateService

{
  "serviceName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "DesiredCount is missing"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateTaskSet

Create a task set in the specified cluster and service.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateTaskSet

{
  "service": "example",
  "cluster": "example",
  "taskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClusterNotFoundException",
  "message": "Cluster not found."
}
```

`400` `ClusterNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteAccountSetting

Disables an account setting for a specified user, role, or the root user for an account.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteAccountSetting

{
  "name": "serviceLongArnFormat"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "Referenced setting doesn't exist"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteAttributes

Deletes one or more custom attributes from an Amazon ECS resource.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteAttributes

{
  "attributes": [
    {
      "name": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "attributes.targetId is required to name the resource the attribute is on"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteCapacityProvider

Deletes the specified capacity provider.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteCapacityProvider

{
  "capacityProvider": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation DeleteCapacityProvider is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteCluster

Deletes the specified cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteCluster

{
  "cluster": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClusterNotFoundException",
  "message": "Cluster not found."
}
```

`400` `ClusterNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deletecluster) for a successful call.

### DeleteDaemon

Deletes the specified daemon.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteDaemon

{
  "daemonArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "DaemonNotFoundException",
  "message": "Daemon not found: arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

`400` `DaemonNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDaemonTaskDefinition

Deletes the specified daemon task definition.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteDaemonTaskDefinition

{
  "daemonTaskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClientException",
  "message": "daemonTaskDefinition must name a revision (family:revision or a daemon-task-definition ARN); got \"example\""
}
```

`400` `ClientException`.

### DeleteExpressGatewayService

Deletes an Express service and removes all associated Amazon Web Services resources.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteExpressGatewayService

{
  "serviceArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ServiceNotFoundException",
  "message": "Service not found"
}
```

`400` `ServiceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteGateway

Deletes a gateway.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteGateway

{
  "gateway": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation DeleteGateway is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteService

Deletes a specified service within a cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteService

{
  "service": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ServiceNotFoundException",
  "message": ""
}
```

`400` `ServiceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTaskDefinitions

Deletes one or more task definitions.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteTaskDefinitions

{
  "taskDefinitions": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClientException",
  "message": "taskDefinitions entries must each name a revision (family:revision or a task-definition ARN); got \"example\""
}
```

`400` `ClientException`.

### DeleteTaskSet

Deletes a specified task set within a service.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteTaskSet

{
  "cluster": "example",
  "service": "example",
  "taskSet": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClusterNotFoundException",
  "message": "Cluster not found."
}
```

`400` `ClusterNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeregisterContainerInstance

Deregisters an Amazon ECS container instance from the specified cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeregisterContainerInstance

{
  "containerInstance": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "Referenced container instance example not found."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeregisterTaskDefinition

Deregisters the specified task definition by family and revision.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeregisterTaskDefinition

{
  "taskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "Revision is missing"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-deregistertaskdefinition) for a successful call.

### DescribeCapacityProviders

Describes one or more of your capacity providers.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeCapacityProviders

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "capacityProviders": [
    {
      "capacityProviderArn": "arn:aws:ecs:us-east-1:000000000000:capacity-provider/FARGATE",
      "name": "FARGATE",
      "status": "ACTIVE",
      "tags": [],
      "type": "FARGATE"
    },
    {
      "capacityProviderArn": "arn:aws:ecs:us-east-1:000000000000:capacity-provider/FARGATE_SPOT",
      "name": "FARGATE_SPOT",
      "status": "ACTIVE",
      "tags": [],
      "type": "FARGATE_SPOT"
    }
  ],
  "failures": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeClusters

Describes one or more of your clusters.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeClusters

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "clusters": [
    {
      "clusterArn": "arn:aws:ecs:us-east-1:000000000000:cluster/default",
      "clusterName": "default",
      "status": "ACTIVE",
      "registeredContainerInstancesCount": 0,
      "runningTasksCount": 0,
      "pendingTasksCount": 0,
      "activeServicesCount": 0,
      "statistics": [],
      "tags": [],
      "settings": [],
      "capacityProviders": [],
      "defaultCapacityProviderStrategy": []
    }
  ],
  "failures": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeContainerInstances

Describes one or more container instances.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeContainerInstances

{
  "containerInstances": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "containerInstances": [],
  "failures": [
    {
      "arn": "arn:aws:ecs:us-east-1:000000000000:container-instance/example",
      "reason": "MISSING"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDaemon

Describes the specified daemon.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeDaemon

{
  "daemonArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "DaemonNotFoundException",
  "message": "Daemon not found: arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

`400` `DaemonNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDaemonDeployments

Describes one or more of your daemon deployments.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeDaemonDeployments

{
  "daemonDeploymentArns": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "failures": [
    {
      "arn": "example",
      "reason": "MISSING"
    }
  ],
  "daemonDeployments": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDaemonRevisions

Describes one or more of your daemon revisions.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeDaemonRevisions

{
  "daemonRevisionArns": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "daemonRevisions": [],
  "failures": [
    {
      "arn": "example",
      "reason": "MISSING"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDaemonTaskDefinition

Describes a daemon task definition.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeDaemonTaskDefinition

{
  "daemonTaskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClientException",
  "message": "Unable to describe daemon task definition."
}
```

`400` `ClientException`.

### DescribeExpressGatewayService

Retrieves detailed information about an Express service, including current status, configuration, managed infrastructure, and service revisions.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeExpressGatewayService

{
  "serviceArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ResourceNotFoundException",
  "message": "Service not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeGateway

Describes a gateway, including its configuration, status, and the type-specific infrastructure settings that it uses.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeGateway

{
  "gateway": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation DescribeGateway is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeGatewayResources

Returns the infrastructure that a gateway has created and manages, so that you can follow the traffic path from top-level shared resources, such as load balancers, to the service-specific resources represented as gateway routes.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeGatewayResources

{
  "gateway": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation DescribeGatewayResources is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeGatewayRoute

Describes a gateway route, including its status, the service it routes traffic to, and the service-specific infrastructure, such as target groups and listener rules, that the gateway manages for it.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeGatewayRoute

{
  "gatewayRouteArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation DescribeGatewayRoute is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeServiceDeployments

Describes one or more of your service deployments.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeServiceDeployments

{
  "serviceDeploymentArns": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "serviceDeployments": [],
  "failures": [
    {
      "arn": "example",
      "reason": "MISSING"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeServiceRevisions

Describes one or more service revisions.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeServiceRevisions

{
  "serviceRevisionArns": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "serviceRevisions": [],
  "failures": [
    {
      "arn": "example",
      "reason": "MISSING"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeServices

Describes the specified services running in your cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeServices

{
  "services": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "services": [],
  "failures": [
    {
      "arn": "arn:aws:ecs:us-east-1:000000000000:service/default/example",
      "reason": "MISSING"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTaskDefinition

Describes a task definition.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeTaskDefinition

{
  "taskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClientException",
  "message": "Unable to describe task definition."
}
```

`400` `ClientException`.

### DescribeTaskSets

Describes the task sets in the specified cluster and service.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeTaskSets

{
  "cluster": "example",
  "service": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClusterNotFoundException",
  "message": "Cluster not found."
}
```

`400` `ClusterNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeTasks

Describes a specified task or tasks.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeTasks

{
  "tasks": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "tasks": [],
  "failures": [
    {
      "arn": "arn:aws:ecs:us-east-1:000000000000:task/example",
      "reason": "MISSING"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-describetasks) for a successful call.

### DisassociateGatewayFromCluster

Removes the association between a gateway and an Amazon ECS cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DisassociateGatewayFromCluster

{
  "gateway": "example",
  "cluster": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation DisassociateGatewayFromCluster is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DiscoverPollEndpoint

This action is only used by the Amazon ECS agent, and it is not intended for use outside of the agent.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DiscoverPollEndpoint

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "endpoint": "https://ecs-a-1.us-east-1.amazonaws.com/",
  "telemetryEndpoint": "https://ecs-t-1.us-east-1.amazonaws.com/",
  "serviceConnectEndpoint": "https://ecs-sc.us-east-1.amazonaws.com/"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ExecuteCommand

Runs a command remotely on a container within a task.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ExecuteCommand

{
  "command": "example",
  "interactive": false,
  "task": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "The specified task was not found. Specify a valid task ID and try again."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetTaskProtection

Retrieves the protection status of tasks in an Amazon ECS service.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.GetTaskProtection

{
  "cluster": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClusterNotFoundException",
  "message": "Cluster not found."
}
```

`400` `ClusterNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListAccountSettings

Lists the account settings for a specified principal.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListAccountSettings

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "settings": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListAttributes

Lists the attributes for Amazon ECS resources within a specified target type and cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListAttributes

{
  "targetType": "container-instance"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "attributes": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListClusters

Returns a list of existing clusters.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListClusters

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "clusterArns": [
    "arn:aws:ecs:us-east-1:000000000000:cluster/default"
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListContainerInstances

Returns a list of container instances in a specified cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListContainerInstances

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "containerInstanceArns": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListDaemonDeployments

Returns a list of daemon deployments for a specified daemon.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListDaemonDeployments

{
  "daemonArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClientException",
  "message": "Daemon not found: arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

`400` `ClientException`.

### ListDaemonTaskDefinitions

Returns a list of daemon task definitions that are registered to your account.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListDaemonTaskDefinitions

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "daemonTaskDefinitions": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListDaemons

Returns a list of daemons.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListDaemons

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "daemonSummariesList": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListGatewayClusterAssociations

Returns a list of the associations between gateways and clusters.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListGatewayClusterAssociations

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation ListGatewayClusterAssociations is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListGatewayRoutes

Returns a list of gateway routes.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListGatewayRoutes

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation ListGatewayRoutes is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListGateways

Returns a list of gateways in your account.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListGateways

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation ListGateways is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListServiceDeployments

This operation lists all the service deployments that meet the specified filter criteria.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListServiceDeployments

{
  "service": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ServiceNotFoundException",
  "message": "Service not found."
}
```

`400` `ServiceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListServices

Returns a list of services.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListServices

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "serviceArns": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListServicesByNamespace

This operation lists all of the services that are associated with a Cloud Map namespace.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListServicesByNamespace

{
  "namespace": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "NamespaceNotFoundException",
  "message": "Failed to retrieve namespace for example"
}
```

`400` `NamespaceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTagsForResource

List the tags for an Amazon ECS resource.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListTagsForResource

{
  "resourceArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "resourceArn must be an Amazon ECS resource ARN naming one of capacity-provider, cluster, container-instance, service, task, task-definition; got \"arn:aws:ecs:us-east-1:000000000000:example/example\""
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListTaskDefinitionFamilies

Returns a list of task definition families that are registered to your account.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListTaskDefinitionFamilies

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "families": [
    "docs-demo-web"
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTaskDefinitions

Returns a list of task definitions that are registered to your account.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListTaskDefinitions

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "taskDefinitionArns": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTasks

Returns a list of tasks.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListTasks

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1

{
  "taskArns": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PutAccountSetting

Modifies an account setting.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.PutAccountSetting

{
  "name": "serviceLongArnFormat",
  "value": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "Invalid setting 'value'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### PutAccountSettingDefault

Modifies an account setting for all users on an account for whom no individual account setting has been specified.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.PutAccountSettingDefault

{
  "name": "serviceLongArnFormat",
  "value": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "Invalid setting 'value'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### PutAttributes

Create or update an attribute on an Amazon ECS resource.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.PutAttributes

{
  "attributes": [
    {
      "name": "example"
    }
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation PutAttributes is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### PutClusterCapacityProviders

Modifies the available capacity providers and the default capacity provider strategy for a cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.PutClusterCapacityProviders

{
  "cluster": "example",
  "capacityProviders": [
    "example"
  ],
  "defaultCapacityProviderStrategy": [
    {
      "capacityProvider": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClusterNotFoundException",
  "message": "Cluster not found."
}
```

`400` `ClusterNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RegisterContainerInstance

This action is only used by the Amazon ECS agent, and it is not intended for use outside of the agent.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.RegisterContainerInstance

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "The identity document and identity document signature were not valid."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### RegisterDaemonTaskDefinition

Registers a new daemon task definition from the supplied family and containerDefinitions .

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.RegisterDaemonTaskDefinition

{
  "family": "example",
  "containerDefinitions": [
    {
      "image": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClientException",
  "message": "Container.name should not be null or empty."
}
```

`400` `ClientException`.

### RegisterTaskDefinition

Registers a new task definition from the supplied family and containerDefinitions .

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.RegisterTaskDefinition

{
  "family": "example",
  "containerDefinitions": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClientException",
  "message": "Container.name should not be null or empty."
}
```

`400` `ClientException`. See [the walkthrough](#2-registertaskdefinition) for a successful call.

### RunTask

Starts a new task using the specified task definition.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.RunTask

{
  "taskDefinition": "example",
  "clientToken": "b1a9d722-6ab9-4405-b5fe-d678a7d0b255"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClientException",
  "message": "TaskDefinition not found."
}
```

`400` `ClientException`. See [the walkthrough](#3-runtask) for a successful call.

### StartTask

Starts a new task from the specified task definition on the specified container instance or instances.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.StartTask

{
  "containerInstances": [
    "example"
  ],
  "taskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "instanceId length should be one of [32,36]"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### StopServiceDeployment

Stops an ongoing service deployment.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.StopServiceDeployment

{
  "serviceDeploymentArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ServiceDeploymentNotFoundException",
  "message": "The service deployment ARN arn:aws:ecs:us-east-1:000000000000:example/example that you specified doesn't exist. You can use ListServiceDeployments to retrieve the service deployment ARNs."
}
```

`400` `ServiceDeploymentNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StopTask

Stops a running task.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.StopTask

{
  "task": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "The referenced task was not found"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#5-stoptask) for a successful call.

### SubmitAttachmentStateChanges

This action is only used by the Amazon ECS agent, and it is not intended for use outside of the agent.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.SubmitAttachmentStateChanges

{
  "attachments": [
    {
      "attachmentArn": "arn:aws:ecs:us-east-1:000000000000:example/example",
      "status": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "attachmentStatus should be one of [ATTACHED]"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### SubmitContainerStateChange

This action is only used by the Amazon ECS agent, and it is not intended for use outside of the agent.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.SubmitContainerStateChange

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "The referenced task was not found"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### SubmitTaskStateChange

This action is only used by the Amazon ECS agent, and it is not intended for use outside of the agent.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.SubmitTaskStateChange

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "The referenced task was not found"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### TagResource

Associates the specified tags to a resource with the specified resourceArn .

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.TagResource

{
  "resourceArn": "arn:aws:ecs:us-east-1:000000000000:example/example",
  "tags": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "resourceArn must be an Amazon ECS resource ARN naming one of capacity-provider, cluster, container-instance, service, task, task-definition; got \"arn:aws:ecs:us-east-1:000000000000:example/example\""
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UntagResource

Deletes specified tags from a resource.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UntagResource

{
  "resourceArn": "arn:aws:ecs:us-east-1:000000000000:example/example",
  "tagKeys": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "resourceArn must be an Amazon ECS resource ARN naming one of capacity-provider, cluster, container-instance, service, task, task-definition; got \"arn:aws:ecs:us-east-1:000000000000:example/example\""
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateCapacityProvider

Modifies the parameters for a capacity provider.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateCapacityProvider

{
  "name": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation UpdateCapacityProvider is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateCluster

Updates the cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateCluster

{
  "cluster": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClusterNotFoundException",
  "message": "Cluster not found."
}
```

`400` `ClusterNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateClusterSettings

Modifies the settings to use for a cluster.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateClusterSettings

{
  "cluster": "example",
  "settings": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "Invalid setting 'value'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateContainerAgent

Updates the Amazon ECS container agent on a specified container instance.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateContainerAgent

{
  "containerInstance": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "ContainerInstanceId must match ^[a-f0-9-]{1,36}$"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateContainerInstancesState

Modifies the status of an Amazon ECS container instance.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateContainerInstancesState

{
  "containerInstances": [
    "example"
  ],
  "status": "ACTIVE"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "instanceId length should be one of [32,36]"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateDaemon

Updates the specified daemon.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateDaemon

{
  "daemonArn": "arn:aws:ecs:us-east-1:000000000000:example/example",
  "daemonTaskDefinitionArn": "arn:aws:ecs:us-east-1:000000000000:example/example",
  "capacityProviderArns": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "DaemonNotFoundException",
  "message": "Daemon not found: arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

`400` `DaemonNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateExpressGatewayService

Updates an existing Express service configuration.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateExpressGatewayService

{
  "serviceArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "InvalidParameterException",
  "message": "Resource not found: arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateGateway

Modifies the configuration of a gateway.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateGateway

{
  "gateway": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1

{
  "__type": "NotImplemented",
  "message": "operation UpdateGateway is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateService

Modifies the parameters of a service.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateService

{
  "service": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ServiceNotFoundException",
  "message": "Service not found."
}
```

`400` `ServiceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateServicePrimaryTaskSet

Modifies which task set in a service is the primary task set.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateServicePrimaryTaskSet

{
  "cluster": "example",
  "service": "example",
  "primaryTaskSet": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClusterNotFoundException",
  "message": "Cluster not found."
}
```

`400` `ClusterNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateTaskProtection

Updates the protection status of a task.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateTaskProtection

{
  "cluster": "example",
  "tasks": [
    "example"
  ],
  "protectionEnabled": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClusterNotFoundException",
  "message": "Cluster not found."
}
```

`400` `ClusterNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateTaskSet

Modifies a task set.

```http
POST / HTTP/1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateTaskSet

{
  "cluster": "example",
  "service": "example",
  "taskSet": "example",
  "scale": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1

{
  "__type": "ClusterNotFoundException",
  "message": "Cluster not found."
}
```

`400` `ClusterNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/ecs/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Elastic Container Service answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `message`.
