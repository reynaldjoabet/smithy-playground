# Amazon Elastic Container Registry

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `ECR`
- **sigv4 signing name:** `ecr`
- **Endpoint prefix:** `api.ecr`
- **ARN namespace:** `ecr`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `ecr`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `DescribeRepositories`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:ecr" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeRepositories' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a repository, attach a lifecycle policy, get a registry login token, then delete it. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateRepository

The repository URI points at the emulator itself (`127.0.0.1:9297/docs-demo/app`), so that is what `docker push` targets.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.CreateRepository
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 116
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "docs-demo/app",
  "imageTagMutability": "IMMUTABLE",
  "imageScanningConfiguration": {
    "scanOnPush": true
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 391
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "repository": {
    "repositoryArn": "arn:aws:ecr:us-east-1:000000000000:repository/docs-demo/app",
    "registryId": "000000000000",
    "repositoryName": "docs-demo/app",
    "repositoryUri": "000000000000.dkr.ecr.us-east-1.localhost/docs-demo/app",
    "createdAt": 1795717753.325,
    "imageTagMutability": "IMMUTABLE",
    "imageScanningConfiguration": {
      "scanOnPush": true
    },
    "encryptionConfiguration": {
      "encryptionType": "AES256"
    }
  }
}
```

### 2. PutLifecyclePolicy

A lifecycle policy expires old images.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.PutLifecyclePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 262
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "docs-demo/app",
  "lifecyclePolicyText": "{\"rules\": [{\"rulePriority\": 1, \"description\": \"keep 10\", \"selection\": {\"tagStatus\": \"any\", \"countType\": \"imageCountMoreThan\", \"countNumber\": 10}, \"action\": {\"type\": \"expire\"}}]}"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 276
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "registryId": "000000000000",
  "repositoryName": "docs-demo/app",
  "lifecyclePolicyText": "{\"rules\":[{\"rulePriority\":1,\"description\":\"keep 10\",\"selection\":{\"tagStatus\":\"any\",\"countType\":\"imageCountMoreThan\",\"countNumber\":10},\"action\":{\"type\":\"expire\"}}]}"
}
```

### 3. DescribeRepositories

The repository and its settings.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeRepositories
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 37
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryNames": [
    "docs-demo/app"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 395
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "repositories": [
    {
      "repositoryArn": "arn:aws:ecr:us-east-1:000000000000:repository/docs-demo/app",
      "registryId": "000000000000",
      "repositoryName": "docs-demo/app",
      "repositoryUri": "000000000000.dkr.ecr.us-east-1.localhost/docs-demo/app",
      "createdAt": 1795717753.325,
      "imageTagMutability": "IMMUTABLE",
      "imageScanningConfiguration": {
        "scanOnPush": true
      },
      "encryptionConfiguration": {
        "encryptionType": "AES256"
      }
    }
  ]
}
```

### 4. GetAuthorizationToken

The token (redacted here) is the base64 of `AWS:<password>` that `docker login` uses.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.GetAuthorizationToken
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 1073
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "authorizationData": [
    {
      "authorizationToken": "<redacted>",
      "expiresAt": 1795760953.329,
      "proxyEndpoint": "https://127.0.0.1:9297"
    }
  ]
}
```

### 5. DescribeImages

No images have been pushed yet.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeImages
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 34
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "docs-demo/app"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 19
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "imageDetails": []
}
```

### 6. DeleteRepository

`force` deletes the repository even if it holds images.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DeleteRepository
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "docs-demo/app",
  "force": true
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 391
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "repository": {
    "repositoryArn": "arn:aws:ecr:us-east-1:000000000000:repository/docs-demo/app",
    "registryId": "000000000000",
    "repositoryName": "docs-demo/app",
    "repositoryUri": "000000000000.dkr.ecr.us-east-1.localhost/docs-demo/app",
    "createdAt": 1795717753.325,
    "imageTagMutability": "IMMUTABLE",
    "imageScanningConfiguration": {
      "scanOnPush": true
    },
    "encryptionConfiguration": {
      "encryptionType": "AES256"
    }
  }
}
```

### 7. DescribeRepositories

Now `RepositoryNotFoundException`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeRepositories
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 37
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryNames": [
    "docs-demo/app"
  ]
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 147
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'docs-demo/app' does not exist in the registry with id '000000000000'"
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 58 operations, 13 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### BatchCheckLayerAvailability

Checks the availability of one or more image layers in a repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.BatchCheckLayerAvailability
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "layerDigests": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### BatchDeleteImage

Deletes a list of specified images within a repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.BatchDeleteImage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "imageIds": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### BatchGetImage

Gets detailed information for an image.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.BatchGetImage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "imageIds": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### BatchGetRepositoryScanningConfiguration

Gets the scanning configuration for one or more repositories.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.BatchGetRepositoryScanningConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryNames": [
    "example"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 147
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "scanningConfigurations": [],
  "failures": [
    {
      "repositoryName": "example",
      "failureCode": "REPOSITORY_NOT_FOUND",
      "failureReason": "REPOSITORY_NOT_FOUND"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CompleteLayerUpload

Informs Amazon ECR that the image layer upload has completed for a specified registry, repository name, and upload ID.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.CompleteLayerUpload
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "uploadId": "example",
  "layerDigests": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 386
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'uploadId' failed to satisfy constraint: 'Member must satisfy regular expression pattern: ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'; Invalid parameter at 'layerDigest' failed to satisfy constraint: 'Member must satisfy regular expression pattern: [a-zA-Z0-9-_+.]+:[a-fA-F0-9]+'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### CreatePullThroughCacheRule

Creates a pull through cache rule.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.CreatePullThroughCacheRule
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ecrRepositoryPrefix": "example",
  "upstreamRegistryUrl": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 159
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "UnsupportedUpstreamRegistryException",
  "message": "The upstream registry URL example is invalid. Specify a valid upstream registry URL and try again"
}
```

`400` `UnsupportedUpstreamRegistryException`.

### CreateRepository

Creates a repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.CreateRepository
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 372
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "repository": {
    "repositoryArn": "arn:aws:ecr:us-east-1:000000000000:repository/example",
    "registryId": "000000000000",
    "repositoryName": "example",
    "repositoryUri": "000000000000.dkr.ecr.us-east-1.localhost/example",
    "createdAt": 1795717753.339,
    "imageTagMutability": "MUTABLE",
    "imageScanningConfiguration": {
      "scanOnPush": false
    },
    "encryptionConfiguration": {
      "encryptionType": "AES256"
    }
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-createrepository) for a successful call.

### CreateRepositoryCreationTemplate

Creates a repository creation template.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.CreateRepositoryCreationTemplate
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "prefix": "example",
  "appliedFor": [
    "REPLICATION"
  ]
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 247
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "registryId": "000000000000",
  "repositoryCreationTemplate": {
    "prefix": "example",
    "encryptionConfiguration": {
      "encryptionType": "AES256"
    },
    "imageTagMutability": "MUTABLE",
    "appliedFor": [
      "REPLICATION"
    ],
    "createdAt": 1795717753.339,
    "updatedAt": 1795717753.339
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteLifecyclePolicy

Deletes the lifecycle policy associated with the specified repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DeleteLifecyclePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 167
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "LifecyclePolicyNotFoundException",
  "message": "Lifecycle policy does not exist for the repository with name 'example' in the registry with id '000000000000'"
}
```

`400` `LifecyclePolicyNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeletePullThroughCacheRule

Deletes a pull through cache rule.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DeletePullThroughCacheRule
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 33
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ecrRepositoryPrefix": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 175
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "PullThroughCacheRuleNotFoundException",
  "message": "A pull-through cache rule with repository prefix 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `PullThroughCacheRuleNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRegistryPolicy

Deletes the registry permissions policy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DeleteRegistryPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 126
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RegistryPolicyNotFoundException",
  "message": "Registry policy does not exist in the registry with id '000000000000'"
}
```

`400` `RegistryPolicyNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRepository

Deletes a repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DeleteRepository
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 372
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "repository": {
    "repositoryArn": "arn:aws:ecr:us-east-1:000000000000:repository/example",
    "registryId": "000000000000",
    "repositoryName": "example",
    "repositoryUri": "000000000000.dkr.ecr.us-east-1.localhost/example",
    "createdAt": 1795717753.339,
    "imageTagMutability": "MUTABLE",
    "imageScanningConfiguration": {
      "scanOnPush": false
    },
    "encryptionConfiguration": {
      "encryptionType": "AES256"
    }
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#6-deleterepository) for a successful call.

### DeleteRepositoryCreationTemplate

Deletes a repository creation template.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DeleteRepositoryCreationTemplate
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 20
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "prefix": "example"
}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 247
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "registryId": "000000000000",
  "repositoryCreationTemplate": {
    "prefix": "example",
    "encryptionConfiguration": {
      "encryptionType": "AES256"
    },
    "imageTagMutability": "MUTABLE",
    "appliedFor": [
      "REPLICATION"
    ],
    "createdAt": 1795717753.339,
    "updatedAt": 1795717753.339
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteRepositoryPolicy

Deletes the repository policy associated with the specified repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DeleteRepositoryPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSigningConfiguration

Deletes the registry's signing configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DeleteSigningConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 142
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "SigningConfigurationNotFoundException",
  "message": "The signing configuration does not exist in the registry with id '000000000000'"
}
```

`400` `SigningConfigurationNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeregisterPullTimeUpdateExclusion

Removes a principal from the pull time update exclusion list for a registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DeregisterPullTimeUpdateExclusion
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "principalArn": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 242
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'principalArn' failed to satisfy constraint: 'Member must satisfy regular expression pattern: ^arn:aws(-[a-z]+)*:iam::[0-9]{12}:(role|user)/[\\w+=,.@-]+(/[\\w+=,.@-]+)*$'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### DescribeImageReplicationStatus

Returns the replication status for a specified image.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeImageReplicationStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "imageId": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeImageScanFindings

Returns the scan findings for the specified image.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeImageScanFindings
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "imageId": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeImageSigningStatus

Returns the signing status for a specified image.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeImageSigningStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "imageId": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeImages

Returns metadata about the images in a repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeImages
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#5-describeimages) for a successful call.

### DescribePullThroughCacheRules

Returns the pull through cache rules for a registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribePullThroughCacheRules
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 28
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "pullThroughCacheRules": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeRegistry

Describes the settings for a registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeRegistry
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 69
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "registryId": "000000000000",
  "replicationConfiguration": {
    "rules": []
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeRepositories

Describes image repositories in a registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeRepositories
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 19
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "repositories": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#3-describerepositories) for a successful call.

### DescribeRepositoryCreationTemplates

Returns details about the repository creation templates in a registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.DescribeRepositoryCreationTemplates
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 62
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "registryId": "000000000000",
  "repositoryCreationTemplates": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetAccountSetting

Retrieves the account setting value for the specified setting name.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.GetAccountSetting
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 18
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "name": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 148
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'name' failed to satisfy constraint: 'Invalid account setting name provided'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetAuthorizationToken

Retrieves an authorization token.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.GetAuthorizationToken
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 1073
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "authorizationData": [
    {
      "authorizationToken": "<redacted>",
      "expiresAt": 1795760953.356,
      "proxyEndpoint": "https://127.0.0.1:9297"
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-getauthorizationtoken) for a successful call.

### GetDownloadUrlForLayer

Retrieves the pre-signed Amazon S3 download URL corresponding to an image layer.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.GetDownloadUrlForLayer
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "layerDigest": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 195
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'layerDigest' failed to satisfy constraint: 'Member must satisfy regular expression pattern: [a-zA-Z0-9-_+.]+:[a-fA-F0-9]+'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### GetLifecyclePolicy

Retrieves the lifecycle policy for the specified repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.GetLifecyclePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetLifecyclePolicyPreview

Retrieves the results of the lifecycle policy preview request for the specified repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.GetLifecyclePolicyPreview
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRegistryPolicy

Retrieves the permissions policy for a registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.GetRegistryPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 126
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RegistryPolicyNotFoundException",
  "message": "Registry policy does not exist in the registry with id '000000000000'"
}
```

`400` `RegistryPolicyNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRegistryScanningConfiguration

Retrieves the scanning configuration for a registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.GetRegistryScanningConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 85
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "registryId": "000000000000",
  "scanningConfiguration": {
    "scanType": "BASIC",
    "rules": []
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetRepositoryPolicy

Retrieves the repository policy for the specified repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.GetRepositoryPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetSigningConfiguration

Retrieves the registry's signing configuration, which defines rules for automatically signing images using Amazon Web Services Signer.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.GetSigningConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 142
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "SigningConfigurationNotFoundException",
  "message": "The signing configuration does not exist in the registry with id '000000000000'"
}
```

`400` `SigningConfigurationNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### InitiateLayerUpload

Notifies Amazon ECR that you intend to upload an image layer.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.InitiateLayerUpload
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListImageReferrers

Lists the artifacts associated with a specified subject image.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.ListImageReferrers
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "subjectId": {
    "imageDigest": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListImages

Lists all the image IDs for the specified repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.ListImages
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListPullTimeUpdateExclusions

Lists the IAM principals that are excluded from having their image pull times recorded.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.ListPullTimeUpdateExclusions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 31
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "pullTimeUpdateExclusions": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsForResource

List the tags for an Amazon ECR resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.ListTagsForResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "resourceArn": "arn:aws:ecr:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 213
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "resourceArn 'arn:aws:ecr:us-east-1:000000000000:example/example' is not a valid Amazon ECR repository ARN; the only supported resource is an Amazon ECR repository"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### PutAccountSetting

Allows you to change the basic scan type version or registry policy scope.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.PutAccountSetting
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 36
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "name": "example",
  "value": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 148
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'name' failed to satisfy constraint: 'Invalid account setting name provided'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### PutImage

Creates or updates the image manifest and tags associated with an image.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.PutImage
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "imageManifest": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PutImageScanningConfiguration

The PutImageScanningConfiguration API is being deprecated, in favor of specifying the image scanning configuration at the registry level.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.PutImageScanningConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "imageScanningConfiguration": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PutImageTagMutability

Updates the image tag mutability settings for the specified repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.PutImageTagMutability
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "imageTagMutability": "MUTABLE"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PutLifecyclePolicy

Creates or updates the lifecycle policy for the specified repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.PutLifecyclePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 153
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "lifecyclePolicyText": "example000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 208
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'LifecyclePolicyText' failed to satisfy constraint: 'Lifecycle policy parsing failure: Could not map policyString into LifecyclePolicy.'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-putlifecyclepolicy) for a successful call.

### PutRegistryPolicy

Creates or updates the permissions policy for your registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.PutRegistryPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "policyText": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 149
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'PolicyText' failed to satisfy constraint: 'Invalid registry policy provided'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### PutRegistryScanningConfiguration

Creates or updates the scanning configuration for your private registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.PutRegistryScanningConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 65
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "registryScanningConfiguration": {
    "scanType": "BASIC",
    "rules": []
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### PutReplicationConfiguration

Creates or updates the replication configuration for a registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.PutReplicationConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "replicationConfiguration": {
    "rules": [
      {
        "destinations": [
          {}
        ]
      }
    ]
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 198
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'replicationConfiguration.rules.1.member.destinations.1.member.region' failed to satisfy constraint: 'Member must not be null'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### PutSigningConfiguration

Creates or updates the registry's signing configuration, which defines rules for automatically signing images with Amazon Web Services Signer.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.PutSigningConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "signingConfiguration": {
    "rules": [
      {
        "signingProfileArn": "example"
      }
    ]
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 177
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'signingProfileArn' failed to satisfy constraint: 'The signing profile ARN 'example' must be well formed'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### RegisterPullTimeUpdateExclusion

Adds an IAM principal to the pull time update exclusion list for a registry.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.RegisterPullTimeUpdateExclusion
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "principalArn": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 242
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'principalArn' failed to satisfy constraint: 'Member must satisfy regular expression pattern: ^arn:aws(-[a-z]+)*:iam::[0-9]{12}:(role|user)/[\\w+=,.@-]+(/[\\w+=,.@-]+)*$'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### SetRepositoryPolicy

Applies a repository policy to the specified repository to control access permissions.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.SetRepositoryPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "policyText": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartImageScan

Starts a basic image vulnerability scan.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.StartImageScan
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "imageId": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartLifecyclePolicyPreview

Starts a preview of a lifecycle policy for the specified repository.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.StartLifecyclePolicyPreview
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagResource

Adds specified tags to a resource with the specified ARN.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 113
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "resourceArn": "arn:aws:ecr:us-east-1:000000000000:example/example",
  "tags": [
    {
      "Key": "example",
      "Value": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 213
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "resourceArn 'arn:aws:ecr:us-east-1:000000000000:example/example' is not a valid Amazon ECR repository ARN; the only supported resource is an Amazon ECR repository"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UntagResource

Deletes specified tags from a resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "resourceArn": "arn:aws:ecr:us-east-1:000000000000:example/example",
  "tagKeys": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 213
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "resourceArn 'arn:aws:ecr:us-east-1:000000000000:example/example' is not a valid Amazon ECR repository ARN; the only supported resource is an Amazon ECR repository"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### UpdateImageStorageClass

Transitions an image between storage classes.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.UpdateImageStorageClass
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "imageId": {},
  "targetStorageClass": "STANDARD"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 141
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "RepositoryNotFoundException",
  "message": "The repository with name 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `RepositoryNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdatePullThroughCacheRule

Updates an existing pull through cache rule.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.UpdatePullThroughCacheRule
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 33
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ecrRepositoryPrefix": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 175
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "PullThroughCacheRuleNotFoundException",
  "message": "A pull-through cache rule with repository prefix 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `PullThroughCacheRuleNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateRepositoryCreationTemplate

Updates an existing repository creation template.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.UpdateRepositoryCreationTemplate
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 20
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "prefix": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 155
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "TemplateNotFoundException",
  "message": "A repository creation template with prefix 'example' is not found in the registry with id '000000000000'"
}
```

`400` `TemplateNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UploadLayerPart

Uploads an image layer part to Amazon ECR.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.UploadLayerPart
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 115
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "repositoryName": "example",
  "uploadId": "example",
  "partFirstByte": 0,
  "partLastByte": 0,
  "layerPartBlob": "ZXhhbXBsZQ=="
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 240
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "InvalidParameterException",
  "message": "Invalid parameter at 'uploadId' failed to satisfy constraint: 'Member must satisfy regular expression pattern: ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'"
}
```

`400` `InvalidParameterException`: the placeholder values failed the operation's input validation.

### ValidatePullThroughCacheRule

Validates an existing pull through cache rule for an upstream registry that requires authentication.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: AmazonEC2ContainerRegistry_V20150921.ValidatePullThroughCacheRule
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182913Z
Content-Length: 33
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ecr/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ecrRepositoryPrefix": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 175
date: Thu, 26 Nov 2026 18:29:13 GMT

{
  "__type": "PullThroughCacheRuleNotFoundException",
  "message": "A pull-through cache rule with repository prefix 'example' does not exist in the registry with id '000000000000'"
}
```

`400` `PullThroughCacheRuleNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/ecr/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Elastic Container Registry answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `message`.
