# AWS CloudHSM

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: BaldrApiService.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `CloudHSM V2`
- **sigv4 signing name:** `cloudhsm`
- **Endpoint prefix:** `cloudhsmv2`
- **ARN namespace:** `cloudhsm`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `cloudhsm`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `DescribeClusters`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:cloudhsm" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: BaldrApiService.DescribeClusters' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a CloudHSM cluster in the default VPC, add an HSM to it, then remove both. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateCluster

A cluster spans the subnets it is given and starts in `CREATE_IN_PROGRESS`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.CreateCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "HsmType": "hsm2m.medium",
  "SubnetIds": [
    "subnet-00000000000000007",
    "subnet-00000000000000009"
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 450
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "Cluster": {
    "BackupPolicy": "DEFAULT",
    "BackupRetentionPolicy": {
      "Type": "DAYS",
      "Value": "90"
    },
    "ClusterId": "cluster-rRKjWBOSRUa",
    "CreateTimestamp": 1795717653.2281904,
    "Hsms": [],
    "HsmType": "hsm2m.medium",
    "SecurityGroup": "sg-9c213ae6b039d5882",
    "State": "CREATE_IN_PROGRESS",
    "SubnetMapping": {
      "us-east-1b": "subnet-00000000000000009",
      "us-east-1a": "subnet-00000000000000007"
    },
    "VpcId": "vpc-00000000000000001",
    "NetworkType": "IPV4",
    "Certificates": {},
    "Mode": "FIPS"
  }
}
```

### 2. DescribeClusters

Poll until the cluster is `UNINITIALIZED`: created, but not yet initialized with a signed certificate.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DescribeClusters
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Filters": {
    "clusterIds": [
      "cluster-rRKjWBOSRUa"
    ]
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 453
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "Clusters": [
    {
      "BackupPolicy": "DEFAULT",
      "BackupRetentionPolicy": {
        "Type": "DAYS",
        "Value": "90"
      },
      "ClusterId": "cluster-rRKjWBOSRUa",
      "CreateTimestamp": 1795717653.2281904,
      "Hsms": [],
      "HsmType": "hsm2m.medium",
      "SecurityGroup": "sg-9c213ae6b039d5882",
      "State": "CREATE_IN_PROGRESS",
      "SubnetMapping": {
        "us-east-1b": "subnet-00000000000000009",
        "us-east-1a": "subnet-00000000000000007"
      },
      "VpcId": "vpc-00000000000000001",
      "NetworkType": "IPV4",
      "Certificates": {},
      "Mode": "FIPS"
    }
  ]
}
```

### 3. CreateHsm

An HSM is placed in one of the cluster's Availability Zones and gets an elastic network interface in that zone's subnet.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.CreateHsm
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClusterId": "cluster-rRKjWBOSRUa",
  "AvailabilityZone": "us-east-1a"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 244
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "Hsm": {
    "AvailabilityZone": "us-east-1a",
    "ClusterId": "cluster-rRKjWBOSRUa",
    "SubnetId": "subnet-00000000000000007",
    "EniId": "eni-1ed8f9ea03d838f57",
    "EniIp": "10.0.0.4",
    "HsmId": "hsm-lRoHJDUBhYy",
    "HsmType": "hsm2m.medium",
    "State": "CREATE_IN_PROGRESS"
  }
}
```

### 4. DescribeClusters

The cluster now lists the HSM, its ENI and IP address.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DescribeClusters
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Filters": {
    "clusterIds": [
      "cluster-rRKjWBOSRUa"
    ]
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 689
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "Clusters": [
    {
      "BackupPolicy": "DEFAULT",
      "BackupRetentionPolicy": {
        "Type": "DAYS",
        "Value": "90"
      },
      "ClusterId": "cluster-rRKjWBOSRUa",
      "CreateTimestamp": 1795717653.2281904,
      "Hsms": [
        {
          "AvailabilityZone": "us-east-1a",
          "ClusterId": "cluster-rRKjWBOSRUa",
          "SubnetId": "subnet-00000000000000007",
          "EniId": "eni-1ed8f9ea03d838f57",
          "EniIp": "10.0.0.4",
          "HsmId": "hsm-lRoHJDUBhYy",
          "HsmType": "hsm2m.medium",
          "State": "CREATE_IN_PROGRESS"
        }
      ],
      "HsmType": "hsm2m.medium",
      "SecurityGroup": "sg-9c213ae6b039d5882",
      "State": "CREATE_IN_PROGRESS",
      "SubnetMapping": {
        "us-east-1b": "subnet-00000000000000009",
        "us-east-1a": "subnet-00000000000000007"
      },
      "VpcId": "vpc-00000000000000001",
      "NetworkType": "IPV4",
      "Certificates": {},
      "Mode": "FIPS"
    }
  ]
}
```

### 5. DeleteCluster

A cluster that still holds an HSM cannot be deleted.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DeleteCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 35
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClusterId": "cluster-rRKjWBOSRUa"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 165
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "cluster cluster-rRKjWBOSRUa still contains the HSMs hsm-lRoHJDUBhYy; delete them before deleting the cluster"
}
```

### 6. DeleteHsm

Remove the HSM ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DeleteHsm
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClusterId": "cluster-rRKjWBOSRUa",
  "HsmId": "hsm-lRoHJDUBhYy"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 27
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "HsmId": "hsm-lRoHJDUBhYy"
}
```

### 7. DescribeClusters

... and wait until the cluster holds no HSMs.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DescribeClusters
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Filters": {
    "clusterIds": [
      "cluster-rRKjWBOSRUa"
    ]
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 689
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "Clusters": [
    {
      "BackupPolicy": "DEFAULT",
      "BackupRetentionPolicy": {
        "Type": "DAYS",
        "Value": "90"
      },
      "ClusterId": "cluster-rRKjWBOSRUa",
      "CreateTimestamp": 1795717653.2281904,
      "Hsms": [
        {
          "AvailabilityZone": "us-east-1a",
          "ClusterId": "cluster-rRKjWBOSRUa",
          "SubnetId": "subnet-00000000000000007",
          "EniId": "eni-1ed8f9ea03d838f57",
          "EniIp": "10.0.0.4",
          "HsmId": "hsm-lRoHJDUBhYy",
          "HsmType": "hsm2m.medium",
          "State": "DELETE_IN_PROGRESS"
        }
      ],
      "HsmType": "hsm2m.medium",
      "SecurityGroup": "sg-9c213ae6b039d5882",
      "State": "CREATE_IN_PROGRESS",
      "SubnetMapping": {
        "us-east-1b": "subnet-00000000000000009",
        "us-east-1a": "subnet-00000000000000007"
      },
      "VpcId": "vpc-00000000000000001",
      "NetworkType": "IPV4",
      "Certificates": {},
      "Mode": "FIPS"
    }
  ]
}
```

### 8. DeleteCluster

Now the cluster can be deleted; it moves to `DELETE_IN_PROGRESS`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DeleteCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 35
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClusterId": "cluster-rRKjWBOSRUa"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 450
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "Cluster": {
    "BackupPolicy": "DEFAULT",
    "BackupRetentionPolicy": {
      "Type": "DAYS",
      "Value": "90"
    },
    "ClusterId": "cluster-rRKjWBOSRUa",
    "CreateTimestamp": 1795717653.2281904,
    "Hsms": [],
    "HsmType": "hsm2m.medium",
    "SecurityGroup": "sg-9c213ae6b039d5882",
    "State": "DELETE_IN_PROGRESS",
    "SubnetMapping": {
      "us-east-1b": "subnet-00000000000000009",
      "us-east-1a": "subnet-00000000000000007"
    },
    "VpcId": "vpc-00000000000000001",
    "NetworkType": "IPV4",
    "Certificates": {},
    "Mode": "FIPS"
  }
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 18 operations, 1 accepted the placeholder call, 4 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### CopyBackupToRegion

Copy an CloudHSM cluster backup to a different region.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.CopyBackupToRegion
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DestinationRegion": "example",
  "BackupId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 397
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "2 validation errors detected: Value 'example' at 'destinationRegion' failed to satisfy constraint: Member must satisfy regular expression pattern: [a-z]{2}(-(gov))?-(east|west|north|south|central){1,2}-\\d; Value 'example' at 'backupId' failed to satisfy constraint: Member must satisfy regular expression pattern: backup-[2-7a-zA-Z]{11,16}"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation.

### CreateCluster

Creates a new CloudHSM cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.CreateCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "HsmType": "example",
  "SubnetIds": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 398
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "2 validation errors detected: Value 'example' at 'hsmType' failed to satisfy constraint: Member must satisfy regular expression pattern: ((p|)hsm[0-9][a-z.]*\\.[a-zA-Z]+); Value '[example]' at 'subnetIds' failed to satisfy constraint: Member must satisfy constraint: [Member must satisfy regular expression pattern: subnet-[0-9a-fA-F]{8,17}]"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createcluster) for a successful call.

### CreateHsm

Creates a new hardware security module (HSM) in the specified CloudHSM cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.CreateHsm
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClusterId": "example",
  "AvailabilityZone": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 407
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "2 validation errors detected: Value 'example' at 'clusterId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^cluster-[2-7a-zA-Z]{11,16}$; Value 'example' at 'availabilityZone' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-z]{2}(-(gov))?-(east|west|north|south|central){1,2}-\\d[a-z]$"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-createhsm) for a successful call.

### DeleteBackup

Deletes a specified CloudHSM backup.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DeleteBackup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "BackupId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 221
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "1 validation error detected: Value 'example' at 'backupId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^backup-[2-7a-zA-Z]{11,16}$"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation.

### DeleteCluster

Deletes the specified CloudHSM cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DeleteCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClusterId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 223
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "1 validation error detected: Value 'example' at 'clusterId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^cluster-[2-7a-zA-Z]{11,16}$"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#8-deletecluster) for a successful call.

### DeleteHsm

Deletes the specified HSM.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DeleteHsm
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClusterId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 223
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "1 validation error detected: Value 'example' at 'clusterId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^cluster-[2-7a-zA-Z]{11,16}$"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-deletehsm) for a successful call.

### DeleteResourcePolicy

Deletes an CloudHSM resource policy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DeleteResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 121
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteResourcePolicy is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeBackups

Gets information about backups of CloudHSM clusters.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DescribeBackups
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 116
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeBackups is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeClusters

Gets information about CloudHSM clusters.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.DescribeClusters
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 453
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "Clusters": [
    {
      "BackupPolicy": "DEFAULT",
      "BackupRetentionPolicy": {
        "Type": "DAYS",
        "Value": "90"
      },
      "ClusterId": "cluster-rRKjWBOSRUa",
      "CreateTimestamp": 1795717653.2281904,
      "Hsms": [],
      "HsmType": "hsm2m.medium",
      "SecurityGroup": "sg-9c213ae6b039d5882",
      "State": "DELETE_IN_PROGRESS",
      "SubnetMapping": {
        "us-east-1b": "subnet-00000000000000009",
        "us-east-1a": "subnet-00000000000000007"
      },
      "VpcId": "vpc-00000000000000001",
      "NetworkType": "IPV4",
… (5 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-describeclusters) for a successful call.

### GetResourcePolicy

Retrieves the resource policy document attached to a given resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.GetResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 118
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "NotImplemented",
  "message": "operation GetResourcePolicy is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### InitializeCluster

Claims an CloudHSM cluster by submitting the cluster certificate issued by your issuing certificate authority (CA) and the CA's root certificate.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.InitializeCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClusterId": "example",
  "SignedCert": "example",
  "TrustAnchor": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 223
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "1 validation error detected: Value 'example' at 'clusterId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^cluster-[2-7a-zA-Z]{11,16}$"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation.

### ListTags

Gets a list of tags for the specified CloudHSM cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.ListTags
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 235
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "1 validation error detected: Value 'example' at 'resourceId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^(?:cluster|backup)-[2-7a-zA-Z]{11,16}$"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation.

### ModifyBackupAttributes

Modifies attributes for CloudHSM backup.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.ModifyBackupAttributes
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "BackupId": "example",
  "NeverExpires": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 221
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "1 validation error detected: Value 'example' at 'backupId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^backup-[2-7a-zA-Z]{11,16}$"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation.

### ModifyCluster

Modifies CloudHSM cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.ModifyCluster
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 23
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ClusterId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 223
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "1 validation error detected: Value 'example' at 'clusterId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^cluster-[2-7a-zA-Z]{11,16}$"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation.

### PutResourcePolicy

Creates or updates an CloudHSM resource policy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.PutResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 118
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "NotImplemented",
  "message": "operation PutResourcePolicy is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RestoreBackup

Restores a specified CloudHSM backup that is in the PENDING_DELETION state.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.RestoreBackup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 22
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "BackupId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 221
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "1 validation error detected: Value 'example' at 'backupId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^backup-[2-7a-zA-Z]{11,16}$"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation.

### TagResource

Adds or overwrites one or more tags for the specified CloudHSM cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceId": "example",
  "TagList": [
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
content-length: 233
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "1 validation error detected: Value 'example' at 'resourceId' failed to satisfy constraint: Member must satisfy regular expression pattern: (?:cluster|backup)-[2-7a-zA-Z]{11,16}"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation.

### UntagResource

Removes the specified tag or tags from the specified CloudHSM cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: BaldrApiService.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/cloudhsm/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceId": "example",
  "TagKeyList": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 233
date: Thu, 26 Nov 2026 18:27:33 GMT

{
  "__type": "CloudHsmInvalidRequestException",
  "Message": "1 validation error detected: Value 'example' at 'resourceId' failed to satisfy constraint: Member must satisfy regular expression pattern: (?:cluster|backup)-[2-7a-zA-Z]{11,16}"
}
```

`400` `CloudHsmInvalidRequestException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/cloudhsm/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS CloudHSM answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `Message`.
