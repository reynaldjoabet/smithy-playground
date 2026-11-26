# Amazon Elastic Container Service

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AmazonEC2ContainerServiceV20141113.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `ECS`
- **sigv4 signing name:** `ecs`
- **Endpoint prefix:** `ecs`
- **ARN namespace:** `ecs`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `ecs`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

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
X-Amz-Date: 20261126T182913Z
Content-Length: 27
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "clusterName": "docs-demo"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 412
date: Thu, 26 Nov 2026 18:29:13 GMT

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
X-Amz-Date: 20261126T182913Z
Content-Length: 253
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 795
date: Thu, 26 Nov 2026 18:29:13 GMT

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
    "registeredAt": 1795717753.527,
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
X-Amz-Date: 20261126T182913Z
Content-Length: 310
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 1495
date: Thu, 26 Nov 2026 18:29:13 GMT

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
            }
          ]
        }
      ],
      "attributes": [
        {
          "name": "ecs.cpu-architecture",
          "value": "x86_64"
        }
      ],
      "availabilityZone": "us-east-1a",
      "clusterArn": "arn:aws:ecs:us-east-1:000000000000:cluster/docs-demo",
      "containers": [
        {
          "containerArn": "arn:aws:ecs:us-east-1:000000000000:container/docs-demo/00000000000000000000000000000001/00000001-0000-0000-0000-000000000001",
          "taskArn": "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001",
          "name": "web",
          "image": "public.ecr.aws/nginx/nginx:latest",
          "lastStatus": "PENDING",
          "networkBindings": [],
          "networkInterfaces": [],
          "healthStatus": "UNKNOWN",
          "managedAgents": [],
          "cpu": "0"
        }
      ],
      "cpu": "256",
      "createdAt": 1795717753.529,
      "desiredStatus": "RUNNING",
      "enableExecuteCommand": false,
      "group": "family:docs-demo-web",
      "healthStatus": "UNKNOWN",
      "lastStatus": "PROVISIONING",
      "launchType": "FARGATE",
      "memory": "512",
      "overrides": {
        "containerOverrides": [
          {
            "name": "web"
          }
        ],
        "inferenceAcceleratorOverrides": []
      },
      "platformVersion": "1.4.0",
      "platformFamily": "Linux",
      "tags": [],
      "taskArn": "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001",
      "taskDefinitionArn": "arn:aws:ecs:us-east-1:000000000000:task-definition/docs-demo-web:1",
… (13 more lines)
```

### 4. DescribeTasks

The task's last status and its containers.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeTasks
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 118
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 1606
date: Thu, 26 Nov 2026 18:29:13 GMT

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
            }
          ]
        }
      ],
      "attributes": [
        {
          "name": "ecs.cpu-architecture",
          "value": "x86_64"
        }
      ],
      "availabilityZone": "us-east-1a",
      "clusterArn": "arn:aws:ecs:us-east-1:000000000000:cluster/docs-demo",
      "containers": [
        {
          "containerArn": "arn:aws:ecs:us-east-1:000000000000:container/docs-demo/00000000000000000000000000000001/00000001-0000-0000-0000-000000000001",
          "taskArn": "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001",
          "name": "web",
          "image": "public.ecr.aws/nginx/nginx:latest",
          "runtimeId": "00000000000000000000000000000001-1",
          "lastStatus": "PENDING",
          "networkBindings": [],
          "networkInterfaces": [],
          "healthStatus": "UNKNOWN",
          "managedAgents": [],
          "cpu": "0"
        }
      ],
      "cpu": "256",
      "createdAt": 1795717753.529,
      "desiredStatus": "RUNNING",
      "enableExecuteCommand": false,
      "group": "family:docs-demo-web",
      "healthStatus": "UNKNOWN",
      "lastStatus": "PROVISIONING",
      "launchType": "FARGATE",
      "memory": "512",
      "overrides": {
        "containerOverrides": [
          {
            "name": "web"
          }
        ],
        "inferenceAcceleratorOverrides": []
      },
      "platformVersion": "1.4.0",
      "platformFamily": "Linux",
      "pullStartedAt": 1795717753.529,
      "pullStoppedAt": 1795717753.529,
… (16 more lines)
```

### 5. StopTask

Stopping records the reason.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.StopTask
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 136
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 1677
date: Thu, 26 Nov 2026 18:29:13 GMT

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
          }
        ]
      }
    ],
    "attributes": [
      {
        "name": "ecs.cpu-architecture",
        "value": "x86_64"
      }
    ],
    "availabilityZone": "us-east-1a",
    "clusterArn": "arn:aws:ecs:us-east-1:000000000000:cluster/docs-demo",
    "containers": [
      {
        "containerArn": "arn:aws:ecs:us-east-1:000000000000:container/docs-demo/00000000000000000000000000000001/00000001-0000-0000-0000-000000000001",
        "taskArn": "arn:aws:ecs:us-east-1:000000000000:task/docs-demo/00000000000000000000000000000001",
        "name": "web",
        "image": "public.ecr.aws/nginx/nginx:latest",
        "runtimeId": "00000000000000000000000000000001-1",
        "lastStatus": "DEACTIVATING",
        "networkBindings": [],
        "networkInterfaces": [],
        "healthStatus": "UNKNOWN",
        "managedAgents": [],
        "cpu": "0"
      }
    ],
    "cpu": "256",
    "createdAt": 1795717753.529,
    "desiredStatus": "STOPPED",
    "enableExecuteCommand": false,
    "group": "family:docs-demo-web",
    "healthStatus": "UNKNOWN",
    "lastStatus": "DEACTIVATING",
    "launchType": "FARGATE",
    "memory": "512",
    "overrides": {
      "containerOverrides": [
        {
          "name": "web"
        }
      ],
      "inferenceAcceleratorOverrides": []
    },
    "platformVersion": "1.4.0",
    "platformFamily": "Linux",
    "pullStartedAt": 1795717753.529,
    "pullStoppedAt": 1795717753.529,
    "stopCode": "UserInitiated",
… (16 more lines)
```

### 6. DeregisterTaskDefinition

The revision becomes `INACTIVE`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeregisterTaskDefinition
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 87
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "taskDefinition": "arn:aws:ecs:us-east-1:000000000000:task-definition/docs-demo-web:1"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 845
date: Thu, 26 Nov 2026 18:29:13 GMT

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
    "registeredAt": 1795717753.527,
    "deregisteredAt": 1795717753.533,
    "registeredBy": "arn:aws:iam::000000000000:user/emulator",
    "previousStatus": "ACTIVE"
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
X-Amz-Date: 20261126T182913Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "docs-demo"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 414
date: Thu, 26 Nov 2026 18:29:13 GMT

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

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 88 operations, 19 accepted the placeholder call, 9 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.629* answered `501` in 1.0.595. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AssociateGatewayWithCluster

Associates a gateway with an Amazon ECS cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.AssociateGatewayWithCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "gateway": "example",
  "cluster": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 128
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ContinueServiceDeployment
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "serviceDeploymentArn": "arn:aws:ecs:us-east-1:000000000000:example/example",
  "hookId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 87
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "The Service ARN provided is invalid."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateCapacityProvider

Creates a capacity provider.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateCapacityProvider
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 18
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "name": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 128
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 408
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateDaemon
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 138
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 185
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "ClientException",
  "message": "daemonTaskDefinition must be a family, family:revision or daemon-task-definition ARN; got \"arn:aws:ecs:us-east-1:000000000000:example/example\""
}
```

`400` `ClientException`.

### CreateExpressGatewayService

Creates an Express service that simplifies deploying containerized web applications on Amazon ECS with managed Amazon Web Services infrastructure.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateExpressGatewayService
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "infrastructureRoleArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 113
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "either primaryContainer or taskDefinitionArn must be specified"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateGateway

Creates a gateway.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateGateway
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 185
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 102
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "gatewayArn": "arn:aws:ecs:us-east-1:000000000000:gateway/example",
  "name": "example",
  "status": "ACTIVE"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateService

Runs and maintains your desired number of tasks from a specified task definition.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateService
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "serviceName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 74
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.CreateTaskSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "service": "example",
  "cluster": "example",
  "taskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteAccountSetting
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "name": "serviceLongArnFormat"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 35
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 123
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "attributes.targetId is required to name the resource the attribute is on"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeleteCapacityProvider

Deletes the specified capacity provider.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteCapacityProvider
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 30
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "capacityProvider": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 89
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "ClientException",
  "message": "Failed to get info for provided CapacityProvider"
}
```

`400` `ClientException`.

### DeleteCluster

Deletes the specified cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteDaemon
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "daemonArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 74
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "DaemonNotFoundException",
  "message": "No daemon metadata found."
}
```

`400` `DaemonNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDaemonTaskDefinition

Deletes the specified daemon task definition.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteDaemonTaskDefinition
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 34
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "daemonTaskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 149
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteExpressGatewayService
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "serviceArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 67
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteGateway
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "gateway": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 114
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteService
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "service": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 37
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "ServiceNotFoundException"
}
```

`400` `ServiceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTaskDefinitions

Deletes one or more task definitions.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteTaskDefinitions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "taskDefinitions": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 150
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeleteTaskSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "example",
  "service": "example",
  "taskSet": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeregisterContainerInstance
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "containerInstance": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 99
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DeregisterTaskDefinition
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "taskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 70
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeCapacityProviders
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 350
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeClusters
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 344
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeContainerInstances
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 34
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "containerInstances": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 129
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeDaemon
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "daemonArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 117
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeDaemonDeployments
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 36
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "daemonDeploymentArns": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 84
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Request parameter(s) are invalid."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeDaemonRevisions

Describes one or more of your daemon revisions.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeDaemonRevisions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 34
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "daemonRevisionArns": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 86
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "The Daemon Revision ARN is invalid."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeDaemonTaskDefinition

Describes a daemon task definition.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeDaemonTaskDefinition
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 34
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "daemonTaskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 83
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeExpressGatewayService
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "serviceArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "ResourceNotFoundException",
  "message": "Service not found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeGateway

Describes a gateway, including its configuration, status, and the type-specific infrastructure settings that it uses.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeGateway
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "gateway": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 535
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "gateway": {
    "gatewayArn": "arn:aws:ecs:us-east-1:000000000000:gateway/example",
    "name": "example",
    "status": "ACTIVE",
    "infrastructureRoleArn": "arn:aws:ecs:us-east-1:000000000000:example/example",
    "deletionProtection": true,
    "configuration": {
      "hostSharedAlb": {
        "networkConfiguration": {
          "ipAddressType": "dualstack"
        },
        "scheme": "internet-facing",
        "port": 443,
        "protocol": "HTTPS",
        "maxAlbCount": 10,
        "currentAlbCount": 0,
        "httpDesync": "defensive"
      }
    },
… (7 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeGatewayResources

Returns the infrastructure that a gateway has created and manages, so that you can follow the traffic path from top-level shared resources, such as load balancers, to the service-specific resources represented as gateway routes.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeGatewayResources
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "gateway": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 125
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeGatewayRoute
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "gatewayRouteArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 121
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeServiceDeployments
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 37
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "serviceDeploymentArns": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 87
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "The Service ARN provided is invalid."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeServiceRevisions

Describes one or more service revisions.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeServiceRevisions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 35
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "serviceRevisionArns": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 87
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "The Service ARN provided is invalid."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeServices

Describes the specified services running in your cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeServices
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "services": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 116
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeTaskDefinition
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "taskDefinition": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 76
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeTaskSets
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "example",
  "service": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DescribeTasks
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "tasks": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 102
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DisassociateGatewayFromCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "gateway": "example",
  "cluster": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 131
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.DiscoverPollEndpoint
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 161
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "endpoint": "https://127.0.0.1:9297/",
  "telemetryEndpoint": "https://127.0.0.1:9297/telemetry/",
  "serviceConnectEndpoint": "https://127.0.0.1:9297/service-connect/"
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ExecuteCommand

Runs a command remotely on a container within a task.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ExecuteCommand
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "command": "example",
  "interactive": false,
  "task": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 149
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Amazon ECS only supports initiating interactive sessions, so you must specify true for this value."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetTaskProtection

Retrieves the protection status of tasks in an Amazon ECS service.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.GetTaskProtection
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListAccountSettings
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 15
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "settings": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListAttributes

Lists the attributes for Amazon ECS resources within a specified target type and cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 35
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "targetType": "container-instance"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 17
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "attributes": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListClusters

Returns a list of existing clusters.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListClusters
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 70
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListContainerInstances
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 28
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "containerInstanceArns": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListDaemonDeployments

Returns a list of daemon deployments for a specified daemon.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListDaemonDeployments
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "daemonArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 109
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListDaemonTaskDefinitions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 28
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "daemonTaskDefinitions": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListDaemons

Returns a list of daemons.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListDaemons
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 26
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "daemonSummariesList": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListGatewayClusterAssociations

Returns a list of the associations between gateways and clusters.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListGatewayClusterAssociations
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 131
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListGatewayRoutes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 118
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListGateways
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 113
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListServiceDeployments
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "service": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListServices
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 18
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "serviceArns": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListServicesByNamespace

This operation lists all of the services that are associated with a Cloud Map namespace.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListServicesByNamespace
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "namespace": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 92
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListTagsForResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "resourceArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 250
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListTaskDefinitionFamilies
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 30
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListTaskDefinitions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 25
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "taskDefinitionArns": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTasks

Returns a list of tasks.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.ListTasks
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 15
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "taskArns": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PutAccountSetting

Modifies an account setting.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.PutAccountSetting
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "name": "serviceLongArnFormat",
  "value": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 74
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.PutAccountSettingDefault
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "name": "serviceLongArnFormat",
  "value": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 74
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid setting 'value'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### PutAttributes

Create or update an attribute on an Amazon ECS resource.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.PutAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 35
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 123
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "attributes.targetId is required to name the resource the attribute is on"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### PutClusterCapacityProviders

Modifies the available capacity providers and the default capacity provider strategy for a cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.PutClusterCapacityProviders
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 120
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.RegisterContainerInstance
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 120
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.RegisterDaemonTaskDefinition
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 84
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.RegisterTaskDefinition
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 48
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 84
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.RunTask
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "taskDefinition": "example",
  "clientToken": "b1a9d722-6ab9-4405-b5fe-d678a7d0b255"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 66
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.StartTask
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 93
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.StopServiceDeployment
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "serviceDeploymentArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 87
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "The Service ARN provided is invalid."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### StopTask

Stops a running task.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.StopTask
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 18
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "task": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 84
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.SubmitAttachmentStateChanges
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 107
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 95
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.SubmitContainerStateChange
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 84
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.SubmitTaskStateChange
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 84
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 250
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 250
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "resourceArn must be an Amazon ECS resource ARN naming one of capacity-provider, cluster, container-instance, service, task, task-definition; got \"arn:aws:ecs:us-east-1:000000000000:example/example\""
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateCapacityProvider

Modifies the parameters for a capacity provider.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateCapacityProvider
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 18
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "name": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 128
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "either autoScalingGroupProvider or managedInstancesProvider must be specified"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateCluster

Updates the cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateClusterSettings
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 37
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 74
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateContainerAgent
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "containerInstance": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 99
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateContainerInstancesState
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 93
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateDaemon
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 180
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 185
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "ClientException",
  "message": "daemonTaskDefinition must be a family, family:revision or daemon-task-definition ARN; got \"arn:aws:ecs:us-east-1:000000000000:example/example\""
}
```

`400` `ClientException`.

### UpdateExpressGatewayService

Updates an existing Express service configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateExpressGatewayService
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "serviceArn": "arn:aws:ecs:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 121
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateGateway
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "gateway": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 114
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateService
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "service": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateServicePrimaryTaskSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "cluster": "example",
  "service": "example",
  "primaryTaskSet": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateTaskProtection
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerServiceV20141113.UpdateTaskSet
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecs/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
content-length: 68
date: Thu, 26 Nov 2026 18:29:13 GMT

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
