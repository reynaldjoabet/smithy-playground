# Amazon Elastic Kubernetes Service

## Wire protocol

- **Protocol:** `restJson1`
- **SDK id:** `EKS`
- **sigv4 signing name:** `eks`
- **Endpoint prefix:** `eks`
- **ARN namespace:** `eks`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `eks`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListClusters`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:eks" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  'https://127.0.0.1:9297/clusters'
```

## End-to-end walkthrough

Create an IAM role for the control plane, create a cluster in the default VPC's subnets, and delete it. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. AWS Identity and Access Management: CreateRole

EKS needs a role it can assume.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120552Z
Content-Length: 315
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateRole
&Version=2010-05-08
&RoleName=docs-demo-eks
&AssumeRolePolicyDocument=%7B%22Version%22%3A%20%222012-10-17%22%2C%20%22Statement%22%3A%20%5B%7B%22Effect%22%3A%20%22Allow%22%2C%20%22Principal%22%3A%20%7B%22Service%22%3A%20%22eks.amazonaws.com%22%7D%2C%20%22Action%22%3A%20%22sts%3AAssumeRole%22%7D%5D%7D
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 778
date: Mon, 16 Nov 2026 12:05:52 GMT

<CreateRoleResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <CreateRoleResult>
    <Role xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
      <Path>/</Path>
      <RoleName>docs-demo-eks</RoleName>
      <RoleId>AROAOWVOJOY3VBWOJCSHY</RoleId>
      <Arn>arn:aws:iam::000000000000:role/docs-demo-eks</Arn>
      <CreateDate>2026-11-16T12:05:52.022Z</CreateDate>
      <AssumeRolePolicyDocument>%7B%22Version%22%3A%222012-10-17%22%2C%22Statement%22%3A%5B%7B%22Effect%22%3A%22Allow%22%2C%22Principal%22%3A%7B%22Service%22%3A%22eks.amazonaws.com%22%7D%2C%22Action%22%3A%22sts%3AAssumeRole%22%7D%5D%7D</AssumeRolePolicyDocument>
      <MaxSessionDuration>3600</MaxSessionDuration>
    </Role>
  </CreateRoleResult>
  <ResponseMetadata>
    <RequestId>f74ac674-0a47-4ebc-8f1d-b18d7bb3ce0e</RequestId>
  </ResponseMetadata>
</CreateRoleResponse>
```

### 2. CreateCluster

The subnets come from EC2; the cluster starts in `CREATING`.

**Request**

```http
POST /clusters HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120552Z
Content-Length: 256
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/eks/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "name": "docs-demo",
  "version": "1.31",
  "roleArn": "arn:aws:iam::000000000000:role/docs-demo-eks",
  "resourcesVpcConfig": {
    "subnetIds": [
      "subnet-00000000000000007",
      "subnet-00000000000000009"
    ]
  },
  "clientRequestToken": "12207b2a-d898-452e-a4b9-cca193163023"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 1449
date: Mon, 16 Nov 2026 12:05:52 GMT

{
  "cluster": {
    "name": "docs-demo",
    "arn": "arn:aws:eks:us-east-1:000000000000:cluster/docs-demo",
    "createdAt": 1794830752.0792463,
    "version": "1.31",
    "roleArn": "arn:aws:iam::000000000000:role/docs-demo-eks",
    "resourcesVpcConfig": {
      "subnetIds": [
        "subnet-00000000000000007",
        "subnet-00000000000000009"
      ],
      "securityGroupIds": [],
      "vpcId": "vpc-00000000000000001",
      "endpointPublicAccess": true,
      "endpointPrivateAccess": false,
      "publicAccessCidrs": [
        "0.0.0.0/0"
      ],
      "controlPlaneEgressMode": "AWS_MANAGED"
    },
    "kubernetesNetworkConfig": {
      "serviceIpv4Cidr": "10.100.0.0/16",
      "ipFamily": "ipv4",
      "elasticLoadBalancing": {
        "enabled": false
      }
    },
    "logging": {
      "clusterLogging": [
        {
          "types": [
            "api",
            "audit",
            "authenticator",
            "controllerManager",
            "scheduler"
          ],
          "enabled": false
        }
      ]
    },
    "status": "CREATING",
    "certificateAuthority": {},
    "platformVersion": "eks.4",
    "tags": {},
    "accessConfig": {
      "bootstrapClusterCreatorAdminPermissions": true,
      "authenticationMode": "CONFIG_MAP"
    },
    "upgradePolicy": {
      "supportType": "EXTENDED"
    },
    "deletionProtection": false,
    "controlPlaneScalingConfig": {
      "tier": "standard"
    },
    "kubeApiServerConfig": {
      "eventTtl": "60m",
      "serviceNodePortRange": {
… (31 more lines)
```

### 3. DescribeCluster

Poll until `ACTIVE`. The cluster then reports its certificate authority and an API server endpoint at a local address (`https://127.0.0.1:<port>`).

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
GET /clusters/docs-demo HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120554Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/eks/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 2423
date: Mon, 16 Nov 2026 12:05:54 GMT

{
  "cluster": {
    "name": "docs-demo",
    "arn": "arn:aws:eks:us-east-1:000000000000:cluster/docs-demo",
    "createdAt": 1794830752.0792463,
    "version": "1.31",
    "endpoint": "https://127.0.0.1:34531",
    "roleArn": "arn:aws:iam::000000000000:role/docs-demo-eks",
    "resourcesVpcConfig": {
      "subnetIds": [
        "subnet-00000000000000007",
        "subnet-00000000000000009"
      ],
      "securityGroupIds": [],
      "clusterSecurityGroupId": "sg-000000000000000a5",
      "vpcId": "vpc-00000000000000001",
      "endpointPublicAccess": true,
      "endpointPrivateAccess": false,
      "publicAccessCidrs": [
        "0.0.0.0/0"
      ],
      "controlPlaneEgressMode": "AWS_MANAGED"
    },
    "kubernetesNetworkConfig": {
      "serviceIpv4Cidr": "10.100.0.0/16",
      "ipFamily": "ipv4",
      "elasticLoadBalancing": {
        "enabled": false
      }
    },
    "logging": {
      "clusterLogging": [
        {
          "types": [
            "api",
            "audit",
            "authenticator",
            "controllerManager",
            "scheduler"
          ],
          "enabled": false
        }
      ]
    },
    "identity": {
      "oidc": {
        "issuer": "https://oidc.eks.us-east-1.amazonaws.com/id/3FA665443E5D315DA0C3B70B71F19C7B"
      }
    },
    "status": "ACTIVE",
    "certificateAuthority": {
      "data": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUJiekNDQVJXZ0F3SUJBZ0lVSnF1bzhhQStIQ1lYQ1M3Z2Y1dzFVUzQrY3Vnd0NnWUlLb1pJemowRUF3SXcKRlRFVE1CRUdBMVVFQXd3S2EzVmlaWEp1WlhSbGN6QWVGdzB5TmpFeE1UWXhNakExTlRKYUZ3MHpOakV4TVRNeApNakExTlRKYU1CVXhFekFSQmdOVkJBTU1DbXQxWW1WeWJtVjBaWE13V1RBVEJnY3Foa2pPUFFJQkJnZ3Foa2pPClBRTUJCd05DQUFURWJXUkJvZjk0SXhwV09wenJrTWN6Q0tKVk9SSlBOeS9US1JtN0VkUEgrSWZ5aUFsRUx5NXIKNWFjNlZTMFhOVWJwSTdOWTlZeFg1ZEV0am5yOVltcXVvME13UVRBUEJnTlZIUThCQWY4RUJRTURCNFlBTUIwRwpBMVVkRGdRV0JCUTNEdzRzNzlXc1prTGxCZytpdjBjUjR1Q3JLakFQQmdOVkhSTUJBZjhFQlRBREFRSC9NQW9HCkNDcUdTTTQ5QkFNQ0EwZ0FNRVVDSUgxeG5Uci9FTzVMZUpmVENMV1NTQ0VkZS8xaUpick4yb1ZPdTV5SlRMUTIKQWlFQTFUUXlidy9zVEVhSXdUL1VQWTRFbkhQbDFiditndDBSNVp5YkIrSmViUzA9Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K",
      "active": {
        "id": "cfaeca89-c5f1-4169-9ae0-d2767d57d077",
        "activatedBy": "EKS"
      }
    },
    "platformVersion": "eks.4",
    "tags": {},
    "accessConfig": {
… (43 more lines)
```

### 4. ListClusters

Cluster names of the account in this Region.

**Request**

```http
GET /clusters HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120554Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/eks/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 26
date: Mon, 16 Nov 2026 12:05:54 GMT

{
  "clusters": [
    "docs-demo"
  ]
}
```

### 5. DescribeCluster

An unknown name answers 404 `ResourceNotFoundException`.

**Request**

```http
GET /clusters/nope HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120554Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/eks/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 104
date: Mon, 16 Nov 2026 12:05:54 GMT

{
  "__type": "ResourceNotFoundException",
  "clusterName": "nope",
  "message": "No cluster found for name: nope."
}
```

### 6. DeleteCluster

The cluster moves to `DELETING`.

**Request**

```http
DELETE /clusters/docs-demo HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120554Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/eks/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 1992
date: Mon, 16 Nov 2026 12:05:54 GMT

{
  "cluster": {
    "name": "docs-demo",
    "arn": "arn:aws:eks:us-east-1:000000000000:cluster/docs-demo",
    "createdAt": 1794830752.0792463,
    "version": "1.31",
    "endpoint": "https://127.0.0.1:34531",
    "roleArn": "arn:aws:iam::000000000000:role/docs-demo-eks",
    "resourcesVpcConfig": {
      "subnetIds": [
        "subnet-00000000000000007",
        "subnet-00000000000000009"
      ],
      "securityGroupIds": [],
      "clusterSecurityGroupId": "sg-000000000000000a5",
      "vpcId": "vpc-00000000000000001",
      "endpointPublicAccess": true,
      "endpointPrivateAccess": false,
      "publicAccessCidrs": [
        "0.0.0.0/0"
      ],
      "controlPlaneEgressMode": "AWS_MANAGED"
    },
    "kubernetesNetworkConfig": {
      "serviceIpv4Cidr": "10.100.0.0/16",
      "ipFamily": "ipv4",
      "elasticLoadBalancing": {
        "enabled": false
      }
    },
    "logging": {
      "clusterLogging": [
        {
          "types": [
            "api",
            "audit",
            "authenticator",
            "controllerManager",
            "scheduler"
          ],
          "enabled": false
        }
      ]
    },
    "identity": {
      "oidc": {
        "issuer": "https://oidc.eks.us-east-1.amazonaws.com/id/3FA665443E5D315DA0C3B70B71F19C7B"
      }
    },
    "status": "DELETING",
    "certificateAuthority": {
      "data": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUJiekNDQVJXZ0F3SUJBZ0lVSnF1bzhhQStIQ1lYQ1M3Z2Y1dzFVUzQrY3Vnd0NnWUlLb1pJemowRUF3SXcKRlRFVE1CRUdBMVVFQXd3S2EzVmlaWEp1WlhSbGN6QWVGdzB5TmpFeE1UWXhNakExTlRKYUZ3MHpOakV4TVRNeApNakExTlRKYU1CVXhFekFSQmdOVkJBTU1DbXQxWW1WeWJtVjBaWE13V1RBVEJnY3Foa2pPUFFJQkJnZ3Foa2pPClBRTUJCd05DQUFURWJXUkJvZjk0SXhwV09wenJrTWN6Q0tKVk9SSlBOeS9US1JtN0VkUEgrSWZ5aUFsRUx5NXIKNWFjNlZTMFhOVWJwSTdOWTlZeFg1ZEV0am5yOVltcXVvME13UVRBUEJnTlZIUThCQWY4RUJRTURCNFlBTUIwRwpBMVVkRGdRV0JCUTNEdzRzNzlXc1prTGxCZytpdjBjUjR1Q3JLakFQQmdOVkhSTUJBZjhFQlRBREFRSC9NQW9HCkNDcUdTTTQ5QkFNQ0EwZ0FNRVVDSUgxeG5Uci9FTzVMZUpmVENMV1NTQ0VkZS8xaUpick4yb1ZPdTV5SlRMUTIKQWlFQTFUUXlidy9zVEVhSXdUL1VQWTRFbkhQbDFiditndDBSNVp5YkIrSmViUzA9Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K",
      "active": {
        "id": "cfaeca89-c5f1-4169-9ae0-d2767d57d077",
        "activatedBy": "EKS"
      }
    },
    "platformVersion": "eks.4",
    "tags": {},
    "accessConfig": {
… (11 more lines)
```

### 7. DescribeCluster

Poll until it is gone.

**Request**

```http
GET /clusters/docs-demo HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120554Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/eks/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 114
date: Mon, 16 Nov 2026 12:05:54 GMT

{
  "__type": "ResourceNotFoundException",
  "clusterName": "docs-demo",
  "message": "No cluster found for name: docs-demo."
}
```

### 8. AWS Identity and Access Management: DeleteRole

Clean up the role.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120554Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/iam/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteRole
&Version=2010-05-08
&RoleName=docs-demo-eks
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 224
date: Mon, 16 Nov 2026 12:05:54 GMT

<DeleteRoleResponse xmlns="https://iam.amazonaws.com/doc/2010-05-08/">
  <DeleteRoleResult/>
  <ResponseMetadata>
    <RequestId>f6284914-fb40-4aaf-be4d-eff52a075801</RequestId>
  </ResponseMetadata>
</DeleteRoleResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`) are left out here; the walkthrough shows them in full.

Of 70 operations, 5 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### ActivateCertificateAuthority

Activates a successor certificate authority (CA) as the signing certificate authority for your cluster, completing a CA rotation.

```http
POST /clusters/example/certificate-authorities/example/activate HTTP/1.1
Content-Type: application/json

{
  "clientRequestToken": "47f6ad46-a4de-4788-8eb9-b70502e05305"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AssociateAccessPolicy

Associates an access policy and its scope to an access entry.

```http
POST /clusters/example/access-entries/arn%3Aaws%3Aeks%3Aus-east-1%3A000000000000%3Aexample%2Fexample/access-policies HTTP/1.1
Content-Type: application/json

{
  "policyArn": "arn:aws:eks:us-east-1:000000000000:example/example",
  "accessScope": {}
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AssociateEncryptionConfig

Associates an encryption configuration to an existing cluster.

```http
POST /clusters/example/encryption-config/associate HTTP/1.1
Content-Type: application/json

{
  "encryptionConfig": [
    {}
  ],
  "clientRequestToken": "55be53ad-63a5-4d08-83fe-91a9bc90f52e"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AssociateIdentityProviderConfig

Associates an identity provider configuration to a cluster.

```http
POST /clusters/example/identity-provider-configs/associate HTTP/1.1
Content-Type: application/json

{
  "oidc": {
    "identityProviderConfigName": "example",
    "issuerUrl": "example",
    "clientId": "example"
  },
  "clientRequestToken": "2af0893b-eec1-4a58-a4b1-849aebcd435d"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CancelUpdate

Cancels an in-progress update to an Amazon EKS cluster on a best-effort basis.

```http
POST /clusters/example/updates/example/cancel-update HTTP/1.1
Content-Type: application/json

{
  "clientRequestToken": "63a700e0-adbc-4a74-a269-8df9b226747a"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateAccessEntry

Creates an access entry.

```http
POST /clusters/example/access-entries HTTP/1.1
Content-Type: application/json

{
  "principalArn": "arn:aws:eks:us-east-1:000000000000:example/example",
  "clientRequestToken": "636b8d0b-848c-4418-9776-f39c47763155"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateAddon

Creates an Amazon EKS add-on.

```http
POST /clusters/example/addons HTTP/1.1
Content-Type: application/json

{
  "addonName": "example",
  "clientRequestToken": "6ee3ee2c-867f-4b7c-b1d4-1fecdf548e57"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateCapability

Creates a managed capability resource for an Amazon EKS cluster.

```http
POST /clusters/example/capabilities HTTP/1.1
Content-Type: application/json

{
  "capabilityName": "example",
  "clientRequestToken": "70fcee3d-f22d-481f-957d-cf9b8ddeb2e2",
  "type": "ACK",
  "roleArn": "arn:aws:eks:us-east-1:000000000000:example/example",
  "deletePropagationPolicy": "RETAIN"
}
```

```http
HTTP/1.1 403 Forbidden
content-type: application/json
x-amzn-errortype: AccessDeniedException

{
  "__type": "AccessDeniedException",
  "message": "Cross-account pass role is not allowed."
}
```

`403` `AccessDeniedException`: the request was refused by authorization.

### CreateCertificateAuthority

Appends a successor certificate authority (CA) to your cluster, beginning the CA rotation process.

```http
POST /clusters/example/certificate-authorities HTTP/1.1
Content-Type: application/json

{
  "clientRequestToken": "6acf5df2-6b00-4770-82cf-8ad1ff54cadd"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateCluster

Creates an Amazon EKS control plane.

```http
POST /clusters HTTP/1.1
Content-Type: application/json

{
  "name": "example",
  "roleArn": "arn:aws:eks:us-east-1:000000000000:example/example",
  "resourcesVpcConfig": {},
  "clientRequestToken": "948cf703-b3e9-482b-bf73-4d600990c71f"
}
```

```http
HTTP/1.1 403 Forbidden
content-type: application/json
x-amzn-errortype: AccessDeniedException

{
  "__type": "AccessDeniedException",
  "message": "Cross-account pass role is not allowed."
}
```

`403` `AccessDeniedException`: the request was refused by authorization. See [the walkthrough](#2-createcluster) for a successful call.

### CreateEksAnywhereSubscription

Creates an EKS Anywhere subscription.

```http
POST /eks-anywhere-subscriptions HTTP/1.1
Content-Type: application/json

{
  "name": "example",
  "term": {},
  "clientRequestToken": "d5988626-ef19-429a-908a-721701d1d104"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidParameterException

{
  "__type": "InvalidParameterException",
  "message": "term duration not one of: 12,36"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreateFargateProfile

Creates an Fargate profile for your Amazon EKS cluster.

```http
POST /clusters/example/fargate-profiles HTTP/1.1
Content-Type: application/json

{
  "fargateProfileName": "example",
  "podExecutionRoleArn": "arn:aws:eks:us-east-1:000000000000:example/example",
  "clientRequestToken": "235d3c81-6fee-41cd-96c4-f6f6ab398fc6"
}
```

```http
HTTP/1.1 403 Forbidden
content-type: application/json
x-amzn-errortype: AccessDeniedException

{
  "__type": "AccessDeniedException",
  "message": "Cross-account pass role is not allowed."
}
```

`403` `AccessDeniedException`: the request was refused by authorization.

### CreateNodegroup

Creates a managed node group for an Amazon EKS cluster.

```http
POST /clusters/example/node-groups HTTP/1.1
Content-Type: application/json

{
  "nodegroupName": "example",
  "subnets": [
    "example"
  ],
  "nodeRole": "example",
  "clientRequestToken": "ce52a63d-5907-4768-ae99-2977be0adc4b"
}
```

```http
HTTP/1.1 403 Forbidden
content-type: application/json
x-amzn-errortype: AccessDeniedException

{
  "__type": "AccessDeniedException",
  "message": "Cross-account pass role is not allowed."
}
```

`403` `AccessDeniedException`: the request was refused by authorization.

### CreatePodIdentityAssociation

Creates an EKS Pod Identity association between a service account in an Amazon EKS cluster and an IAM role with EKS Pod Identity .

```http
POST /clusters/example/pod-identity-associations HTTP/1.1
Content-Type: application/json

{
  "namespace": "example",
  "serviceAccount": "example",
  "roleArn": "arn:aws:eks:us-east-1:000000000000:example/example",
  "clientRequestToken": "5e5bc5fd-2bd4-4bcd-af05-9c589098c30d"
}
```

```http
HTTP/1.1 403 Forbidden
content-type: application/json
x-amzn-errortype: AccessDeniedException

{
  "__type": "AccessDeniedException",
  "message": "Cross-account pass role is not allowed."
}
```

`403` `AccessDeniedException`: the request was refused by authorization.

### DeleteAccessEntry

Deletes an access entry.

```http
DELETE /clusters/example/access-entries/arn%3Aaws%3Aeks%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteAddon

Deletes an Amazon EKS add-on.

```http
DELETE /clusters/example/addons/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteCapability

Deletes a managed capability from your Amazon EKS cluster.

```http
DELETE /clusters/example/capabilities/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteCertificateAuthority

Deletes a certificate authority (CA) from your cluster.

```http
DELETE /clusters/example/certificate-authorities/example?clientRequestToken=db8d0ab1-9fc8-4eb6-a323-eef278062129 HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteCluster

Deletes an Amazon EKS cluster control plane.

```http
DELETE /clusters/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#6-deletecluster) for a successful call.

### DeleteEksAnywhereSubscription

Deletes an expired or inactive subscription.

```http
DELETE /eks-anywhere-subscriptions/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "subscription not found."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteFargateProfile

Deletes an Fargate profile.

```http
DELETE /clusters/example/fargate-profiles/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteNodegroup

Deletes a managed node group.

```http
DELETE /clusters/example/node-groups/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeletePodIdentityAssociation

Deletes a EKS Pod Identity association.

```http
DELETE /clusters/example/pod-identity-associations/example HTTP/1.1
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidParameterException

{
  "__type": "InvalidParameterException",
  "clusterName": "example",
  "message": "The parameter associationId should be 19 characters long."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DeregisterCluster

Deregisters a connected cluster to remove it from the Amazon EKS control plane.

```http
DELETE /cluster-registrations/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeAccessEntry

Describes an access entry.

```http
GET /clusters/example/access-entries/arn%3Aaws%3Aeks%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeAddon

Describes an Amazon EKS add-on.

```http
GET /clusters/example/addons/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeAddonConfiguration

Returns configuration options.

```http
GET /addons/configuration-schemas?addonName=example&addonVersion=example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "Schema not found for requested addonName=[example] and addonVersion=[example]"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeAddonVersions

Describes the versions for an add-on.

```http
GET /addons/supported-versions HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/json

{
  "addons": [
    {
      "addonName": "accuknox_kubearmor",
      "type": "security",
      "addonVersions": [
        {
          "addonVersion": "v1.11.3-eksbuild.1",
          "architecture": [
            "amd64",
            "arm64"
          ],
          "computeTypes": [
            "ec2"
          ],
          "compatibilities": [
            {
              "clusterVersion": "1.37",
              "platformVersions": [
                "*"
… (19398 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCapability

Returns detailed information about a specific managed capability in your Amazon EKS cluster, including its current status, configuration, health information, and any issues that may be affecting its operation.

```http
GET /clusters/example/capabilities/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeCertificateAuthority

Returns detailed information about a certificate authority (CA) in your cluster, including its validity period, signing and distribution status, provenance, scheduled auto-activation events, and public certificate data.

```http
GET /clusters/example/certificate-authorities/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeCluster

Describes an Amazon EKS cluster.

```http
GET /clusters/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#3-describecluster) for a successful call.

### DescribeClusterVersions

Lists available Kubernetes versions for Amazon EKS clusters.

```http
GET /cluster-versions HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/json

{
  "clusterVersions": [
    {
      "clusterVersion": "1.37",
      "clusterType": "eks",
      "defaultPlatformVersion": "eks.4",
      "defaultVersion": false,
      "releaseDate": 1790812800,
      "endOfStandardSupportDate": 1827619200,
      "endOfExtendedSupportDate": 1859241600,
      "status": "STANDARD_SUPPORT",
      "versionStatus": "STANDARD_SUPPORT",
      "kubernetesPatchVersion": "1.37.0",
      "controlPlaneScalingTiers": [
        {
          "tierName": "tier-xl",
          "apiRequestConcurrency": 2000,
          "podSchedulingRatePerSecond": 167,
          "clusterDatabaseSizeGb": 16,
          "controlPlaneComponentConfigOverrides": {
… (7004 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeEksAnywhereSubscription

Returns descriptive information about a subscription.

```http
GET /eks-anywhere-subscriptions/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "subscription not found."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeFargateProfile

Describes an Fargate profile.

```http
GET /clusters/example/fargate-profiles/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeIdentityProviderConfig

Describes an identity provider configuration.

```http
POST /clusters/example/identity-provider-configs/describe HTTP/1.1
Content-Type: application/json

{
  "identityProviderConfig": {
    "type": "example",
    "name": "example"
  }
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeInsight

Returns details about an insight that you specify using its ID.

```http
GET /clusters/example/insights/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeInsightsRefresh

Returns the status of the latest on-demand cluster insights refresh operation.

```http
GET /clusters/example/insights-refresh HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeNodegroup

Describes a managed node group.

```http
GET /clusters/example/node-groups/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribePodIdentityAssociation

Returns descriptive information about an EKS Pod Identity association.

```http
GET /clusters/example/pod-identity-associations/example HTTP/1.1
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidParameterException

{
  "__type": "InvalidParameterException",
  "clusterName": "example",
  "message": "The parameter associationId should be 19 characters long."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeUpdate

Describes an update to an Amazon EKS resource.

```http
GET /clusters/example/updates/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisassociateAccessPolicy

Disassociates an access policy from an access entry.

```http
DELETE /clusters/example/access-entries/arn%3Aaws%3Aeks%3Aus-east-1%3A000000000000%3Aexample%2Fexample/access-policies/arn%3Aaws%3Aeks%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisassociateIdentityProviderConfig

Disassociates an identity provider configuration from a cluster.

```http
POST /clusters/example/identity-provider-configs/disassociate HTTP/1.1
Content-Type: application/json

{
  "identityProviderConfig": {
    "type": "example",
    "name": "example"
  },
  "clientRequestToken": "9c293bf8-48b3-4c8f-bea6-b3f1ad30814b"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidParameterException

{
  "__type": "InvalidParameterException",
  "clusterName": "example",
  "message": "identityProviderConfig.type must be oidc, got example"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ListAccessEntries

Lists the access entries for your cluster.

```http
GET /clusters/example/access-entries HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListAccessPolicies

Lists the available access policies.

```http
GET /access-policies HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/json

{
  "accessPolicies": [
    {
      "name": "AIDevOpsAgentAccessPolicy",
      "arn": "arn:aws:eks::aws:cluster-access-policy/AIDevOpsAgentAccessPolicy"
    },
    {
      "name": "AWSBatchClusterPolicy",
      "arn": "arn:aws:eks::aws:cluster-access-policy/AWSBatchClusterPolicy"
    },
    {
      "name": "AWSBatchNamespacePolicy",
      "arn": "arn:aws:eks::aws:cluster-access-policy/AWSBatchNamespacePolicy"
    },
    {
      "name": "AmazonAIOpsAssistantPolicy",
      "arn": "arn:aws:eks::aws:cluster-access-policy/AmazonAIOpsAssistantPolicy"
    },
    {
      "name": "AmazonARCRegionSwitchScalingPolicy",
… (140 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListAddons

Lists the installed add-ons.

```http
GET /clusters/example/addons HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListAssociatedAccessPolicies

Lists the access policies associated with an access entry.

```http
GET /clusters/example/access-entries/arn%3Aaws%3Aeks%3Aus-east-1%3A000000000000%3Aexample%2Fexample/access-policies HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListCapabilities

Lists all managed capabilities in your Amazon EKS cluster.

```http
GET /clusters/example/capabilities HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListCertificateAuthorities

Lists the certificate authorities (CAs) for your cluster.

```http
GET /clusters/example/certificate-authorities HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListClusters

Lists the Amazon EKS clusters in your Amazon Web Services account in the specified Amazon Web Services Region.

```http
GET /clusters HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/json

{
  "clusters": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-listclusters) for a successful call.

### ListEksAnywhereSubscriptions

Displays the full description of the subscription.

```http
GET /eks-anywhere-subscriptions HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/json

{
  "subscriptions": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListFargateProfiles

Lists the Fargate profiles associated with the specified cluster in your Amazon Web Services account in the specified Amazon Web Services Region.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /clusters/example/fargate-profiles HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListIdentityProviderConfigs

Lists the identity provider configurations for your cluster.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /clusters/example/identity-provider-configs HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListInsights

Returns a list of all insights checked for against the specified cluster.

```http
POST /clusters/example/insights HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListNodegroups

Lists the managed node groups associated with the specified cluster in your Amazon Web Services account in the specified Amazon Web Services Region.

```http
GET /clusters/example/node-groups HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListPodIdentityAssociations

List the EKS Pod Identity associations in a cluster.

```http
GET /clusters/example/pod-identity-associations HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTagsForResource

List the tags for an Amazon EKS resource.

```http
GET /tags/arn%3Aaws%3Aeks%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException

{
  "__type": "NotFoundException",
  "message": "Resource was not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListUpdates

Lists the updates associated with an Amazon EKS resource in your Amazon Web Services account, in the specified Amazon Web Services Region.

```http
GET /clusters/example/updates HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RegisterCluster

Connects a Kubernetes cluster to the Amazon EKS control plane.

```http
POST /cluster-registrations HTTP/1.1
Content-Type: application/json

{
  "name": "example",
  "connectorConfig": {
    "roleArn": "arn:aws:eks:us-east-1:000000000000:example/example",
    "provider": "EKS_ANYWHERE"
  },
  "clientRequestToken": "f0365c04-828d-4acb-a038-b05c39d04bf3"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidParameterException

{
  "__type": "InvalidParameterException",
  "clusterName": "example",
  "message": "The provided roleArn is invalid."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### StartInsightsRefresh

Initiates an on-demand refresh operation for cluster insights, getting the latest analysis outside of the standard refresh schedule.

```http
POST /clusters/example/insights-refresh HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagResource

Associates the specified tags to an Amazon EKS resource with the specified resourceArn .

```http
POST /tags/arn%3Aaws%3Aeks%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Content-Type: application/json

{
  "tags": {
    "key": "example"
  }
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException

{
  "__type": "NotFoundException",
  "message": "Resource was not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UntagResource

Deletes specified tags from an Amazon EKS resource.

```http
DELETE /tags/arn%3Aaws%3Aeks%3Aus-east-1%3A000000000000%3Aexample%2Fexample?tagKeys=example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: NotFoundException

{
  "__type": "NotFoundException",
  "message": "Resource was not found"
}
```

`404` `NotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateAccessEntry

Updates an access entry.

```http
POST /clusters/example/access-entries/arn%3Aaws%3Aeks%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Content-Type: application/json

{
  "clientRequestToken": "bc12dfee-8101-4f61-a189-43aaac908457"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateAddon

Updates an Amazon EKS add-on.

```http
POST /clusters/example/addons/example/update HTTP/1.1
Content-Type: application/json

{
  "clientRequestToken": "11fc329b-2747-40c1-a58e-d38d1d6b46e5"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateCapability

Updates the configuration of a managed capability in your Amazon EKS cluster.

```http
POST /clusters/example/capabilities/example HTTP/1.1
Content-Type: application/json

{
  "clientRequestToken": "d6121380-d96c-476b-a931-561124527478"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateClusterConfig

Updates an Amazon EKS cluster configuration.

```http
POST /clusters/example/update-config HTTP/1.1
Content-Type: application/json

{
  "clientRequestToken": "090a7ca0-24c4-4e37-9181-f3f3b79bbc8c"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateClusterVersion

Updates an Amazon EKS cluster to the specified Kubernetes version.

```http
POST /clusters/example/updates HTTP/1.1
Content-Type: application/json

{
  "version": "example",
  "clientRequestToken": "b60fb252-ccc5-4bd5-80d8-c490133b3380"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "clusterName": "example",
  "message": "No cluster found for name: example."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateEksAnywhereSubscription

Update an EKS Anywhere Subscription.

```http
POST /eks-anywhere-subscriptions/example HTTP/1.1
Content-Type: application/json

{
  "autoRenew": false,
  "clientRequestToken": "22a7e1a6-9298-41f1-a656-d28b508c3a2a"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "subscription not found."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateNodegroupConfig

Updates an Amazon EKS managed node group configuration.

```http
POST /clusters/example/node-groups/example/update-config HTTP/1.1
Content-Type: application/json

{
  "clientRequestToken": "2de304ed-2024-4179-a09d-a3953652dbba"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateNodegroupVersion

Updates the Kubernetes version or AMI version of an Amazon EKS managed node group.

```http
POST /clusters/example/node-groups/example/update-version HTTP/1.1
Content-Type: application/json

{
  "clientRequestToken": "e03a826d-9773-4fe5-8c91-f02cc80df8dc"
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException

{
  "__type": "ResourceNotFoundException",
  "message": "The requested resource does not exist."
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdatePodIdentityAssociation

Updates a EKS Pod Identity association.

```http
POST /clusters/example/pod-identity-associations/example HTTP/1.1
Content-Type: application/json

{
  "clientRequestToken": "f1fabcf4-8359-4351-8344-9873a4e2ee50"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: InvalidParameterException

{
  "__type": "InvalidParameterException",
  "clusterName": "example",
  "message": "The parameter associationId should be 19 characters long."
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/eks/aws4_request` in the `Authorization` header) and the operation from the request itself: an HTTP method and path per operation (the `HTTP binding`), with identifiers in the path, options in the query string or headers, and the rest of the input as a JSON body.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Elastic Kubernetes Service answers in AWS: a JSON body and an HTTP status chosen per operation. Errors captured on this page are a JSON body with `__type`, `message`, plus the `x-amzn-errortype` header.

### Local endpoints

Where AWS returns a regional hostname, the emulator returns a local address: the Kubernetes API server endpoint (`DescribeCluster` → `endpoint`) is `127.0.0.1:<port>`, a port the emulator opened for that resource. Those addresses are local to the emulator; the [Docker setup](README.md#running-the-emulator-in-docker) publishes only port 9297, so from your machine only the API endpoint is reachable.

### Other services involved

The walkthrough also called [AWS Identity and Access Management](iam.md). They are separate services behind the same endpoint, sharing one account and one in-memory engine, so a resource created through one is immediately visible to the other.
