# AWS Network Firewall

## Wire protocol

- **Protocol:** `awsJson1_0`
- **Request routing:** `POST /` with header `X-Amz-Target: NetworkFirewall_20201112.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `Network Firewall`
- **sigv4 signing name:** `network-firewall`
- **Endpoint prefix:** `network-firewall`
- **ARN namespace:** `network-firewall`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `network-firewall`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListFirewalls`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:network-firewall" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.0' \
  -H 'X-Amz-Target: NetworkFirewall_20201112.ListFirewalls' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Build a stateful rule group, a firewall policy that uses it, and a firewall in the default VPC, then tear them down. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateRuleGroup

Stateful rules are written in Suricata syntax. Every write returns an `UpdateToken` that the next update must present.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 201
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "RuleGroupName": "docs-demo-allow-https",
  "RuleGroup": {
    "RulesSource": {
      "RulesString": "pass tcp $HOME_NET any -> $EXTERNAL_NET 443 (msg:\"allow https\"; sid:1; rev:1;)"
    }
  },
  "Type": "STATEFUL",
  "Capacity": 100
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 504
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "UpdateToken": "17ae59aa-167a-4cd9-afac-2d52f04d4565",
  "RuleGroupResponse": {
    "RuleGroupArn": "arn:aws:network-firewall:us-east-1:000000000000:stateful-rulegroup/docs-demo-allow-https",
    "RuleGroupName": "docs-demo-allow-https",
    "RuleGroupId": "a0d1e6d7-d43e-472b-9842-f6fd82806293",
    "Type": "STATEFUL",
    "Capacity": 100,
    "RuleGroupStatus": "ACTIVE",
    "ConsumedCapacity": 1,
    "NumberOfAssociations": 0,
    "EncryptionConfiguration": {
      "KeyId": "AWS_OWNED_KMS_KEY",
      "Type": "AWS_OWNED_KMS_KEY"
    },
    "LastModifiedTime": 1795717847.411451
  }
}
```

### 2. CreateFirewallPolicy

The policy forwards all traffic to the stateful engine, which applies the rule group.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateFirewallPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 305
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallPolicyName": "docs-demo-policy",
  "FirewallPolicy": {
    "StatelessDefaultActions": [
      "aws:forward_to_sfe"
    ],
    "StatelessFragmentDefaultActions": [
      "aws:forward_to_sfe"
    ],
    "StatefulRuleGroupReferences": [
      {
        "ResourceArn": "arn:aws:network-firewall:us-east-1:000000000000:stateful-rulegroup/docs-demo-allow-https"
      }
    ]
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 566
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "UpdateToken": "04d45493-bb79-4474-bc01-8f8bf51a04f7",
  "FirewallPolicyResponse": {
    "FirewallPolicyName": "docs-demo-policy",
    "FirewallPolicyArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy",
    "FirewallPolicyId": "ba24690a-867d-4d3d-ad7f-b3826f66595b",
    "FirewallPolicyStatus": "ACTIVE",
    "ConsumedStatelessRuleCapacity": 0,
    "ConsumedStatefulRuleCapacity": 100,
    "ConsumedStatefulDomainCapacity": 0,
    "NumberOfAssociations": 0,
    "EncryptionConfiguration": {
      "KeyId": "AWS_OWNED_KMS_KEY",
      "Type": "AWS_OWNED_KMS_KEY"
    },
    "LastModifiedTime": 1795717847.4133155
  }
}
```

### 3. CreateFirewall

The firewall places an endpoint in each mapped subnet and starts in `PROVISIONING`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateFirewall
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 225
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallName": "docs-demo-fw",
  "FirewallPolicyArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy",
  "VpcId": "vpc-00000000000000001",
  "SubnetMappings": [
    {
      "SubnetId": "subnet-00000000000000007"
    }
  ]
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 1022
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Firewall": {
    "FirewallName": "docs-demo-fw",
    "FirewallArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall/docs-demo-fw",
    "FirewallPolicyArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy",
    "VpcId": "vpc-00000000000000001",
    "SubnetMappings": [
      {
        "SubnetId": "subnet-00000000000000007"
      }
    ],
    "DeleteProtection": false,
    "SubnetChangeProtection": false,
    "FirewallPolicyChangeProtection": false,
    "FirewallId": "ab07d12e-1aef-4764-a312-221b3e6835f5",
    "EncryptionConfiguration": {
      "KeyId": "AWS_OWNED_KMS_KEY",
      "Type": "AWS_OWNED_KMS_KEY"
    },
    "AvailabilityZoneChangeProtection": false,
    "NoSourcePreservation": false
  },
  "FirewallStatus": {
    "Status": "PROVISIONING",
    "ConfigurationSyncStateSummary": "IN_SYNC",
    "SyncStates": {
      "us-east-1a": {
        "Attachment": {
          "SubnetId": "subnet-00000000000000007",
          "EndpointId": "vpce-000000000000000da",
          "Status": "CREATING"
        },
        "Config": {
          "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy": {
            "SyncStatus": "IN_SYNC",
            "UpdateToken": "c855acca-92e2-427d-97cb-eda68340ef63"
          }
        }
      }
    }
  }
}
```

### 4. DescribeFirewall

Poll until `READY`; the sync states then name the endpoint in each Availability Zone.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeFirewall
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallName": "docs-demo-fw"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 1085
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "UpdateToken": "c855acca-92e2-427d-97cb-eda68340ef63",
  "Firewall": {
    "FirewallName": "docs-demo-fw",
    "FirewallArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall/docs-demo-fw",
    "FirewallPolicyArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy",
    "VpcId": "vpc-00000000000000001",
    "SubnetMappings": [
      {
        "SubnetId": "subnet-00000000000000007"
      }
    ],
    "DeleteProtection": false,
    "SubnetChangeProtection": false,
    "FirewallPolicyChangeProtection": false,
    "FirewallId": "ab07d12e-1aef-4764-a312-221b3e6835f5",
    "Tags": [],
    "EncryptionConfiguration": {
      "KeyId": "AWS_OWNED_KMS_KEY",
      "Type": "AWS_OWNED_KMS_KEY"
    },
    "AvailabilityZoneChangeProtection": false,
    "NoSourcePreservation": false
  },
  "FirewallStatus": {
    "Status": "PROVISIONING",
    "ConfigurationSyncStateSummary": "IN_SYNC",
    "SyncStates": {
      "us-east-1a": {
        "Attachment": {
          "SubnetId": "subnet-00000000000000007",
          "EndpointId": "vpce-000000000000000da",
          "Status": "CREATING"
        },
        "Config": {
          "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy": {
            "SyncStatus": "IN_SYNC",
            "UpdateToken": "c855acca-92e2-427d-97cb-eda68340ef63"
          }
        }
      }
    }
  }
}
```

### 5. DeleteFirewallPolicy

A policy a firewall still uses is refused.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteFirewallPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallPolicyName": "docs-demo-policy"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 105
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidOperationException",
  "Message": "Unable to delete the object because it is still in use"
}
```

### 6. DeleteFirewall

The firewall moves to `DELETING`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteFirewall
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallName": "docs-demo-fw"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 1028
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Firewall": {
    "FirewallName": "docs-demo-fw",
    "FirewallArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall/docs-demo-fw",
    "FirewallPolicyArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy",
    "VpcId": "vpc-00000000000000001",
    "SubnetMappings": [
      {
        "SubnetId": "subnet-00000000000000007"
      }
    ],
    "DeleteProtection": false,
    "SubnetChangeProtection": false,
    "FirewallPolicyChangeProtection": false,
    "FirewallId": "ab07d12e-1aef-4764-a312-221b3e6835f5",
    "Tags": [],
    "EncryptionConfiguration": {
      "KeyId": "AWS_OWNED_KMS_KEY",
      "Type": "AWS_OWNED_KMS_KEY"
    },
    "AvailabilityZoneChangeProtection": false,
    "NoSourcePreservation": false
  },
  "FirewallStatus": {
    "Status": "DELETING",
    "ConfigurationSyncStateSummary": "IN_SYNC",
    "SyncStates": {
      "us-east-1a": {
        "Attachment": {
          "SubnetId": "subnet-00000000000000007",
          "EndpointId": "vpce-000000000000000da",
          "Status": "DELETING"
        },
        "Config": {
          "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy": {
            "SyncStatus": "IN_SYNC",
            "UpdateToken": "c855acca-92e2-427d-97cb-eda68340ef63"
          }
        }
      }
    }
  }
}
```

### 7. DescribeFirewall

Poll until it is gone.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeFirewall
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 31
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallName": "docs-demo-fw"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 116
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "No object with the provided name or resource identifier was found"
}
```

### 8. DeleteFirewallPolicy

With the firewall gone, the policy deletes ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteFirewallPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallPolicyName": "docs-demo-policy"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 525
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "FirewallPolicyResponse": {
    "FirewallPolicyName": "docs-demo-policy",
    "FirewallPolicyArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy",
    "FirewallPolicyId": "ba24690a-867d-4d3d-ad7f-b3826f66595b",
    "FirewallPolicyStatus": "DELETING",
    "Tags": [],
    "ConsumedStatelessRuleCapacity": 0,
    "ConsumedStatefulRuleCapacity": 100,
    "ConsumedStatefulDomainCapacity": 0,
    "NumberOfAssociations": 0,
    "EncryptionConfiguration": {
      "KeyId": "AWS_OWNED_KMS_KEY",
      "Type": "AWS_OWNED_KMS_KEY"
    },
    "LastModifiedTime": 1795717847.42743
  }
}
```

### 9. DeleteRuleGroup

... and so does the rule group.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "RuleGroupName": "docs-demo-allow-https",
  "Type": "STATEFUL"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 463
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "RuleGroupResponse": {
    "RuleGroupArn": "arn:aws:network-firewall:us-east-1:000000000000:stateful-rulegroup/docs-demo-allow-https",
    "RuleGroupName": "docs-demo-allow-https",
    "RuleGroupId": "a0d1e6d7-d43e-472b-9842-f6fd82806293",
    "Type": "STATEFUL",
    "Capacity": 100,
    "RuleGroupStatus": "DELETING",
    "Tags": [],
    "ConsumedCapacity": 1,
    "NumberOfAssociations": 0,
    "EncryptionConfiguration": {
      "KeyId": "AWS_OWNED_KMS_KEY",
      "Type": "AWS_OWNED_KMS_KEY"
    },
    "LastModifiedTime": 1795717847.428491
  }
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 85 operations, 3 accepted the placeholder call, 38 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.629* answered `501` in 1.0.595. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AcceptNetworkFirewallTransitGatewayAttachment

Accepts a transit gateway attachment request for Network Firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.AcceptNetworkFirewallTransitGatewayAttachment
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TransitGatewayAttachmentId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 212
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'transitGatewayAttachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^tgw-attach-[0-9a-z]+$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### AssociateAvailabilityZones

Associates the specified Availability Zones with a transit gateway-attached firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.AssociateAvailabilityZones
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AvailabilityZoneMappings": [
    {
      "AvailabilityZone": "example"
    }
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 127
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation AssociateAvailabilityZones is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### AssociateFirewallPolicy

Associates a FirewallPolicy to a Firewall .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.AssociateFirewallPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallPolicyArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### AssociateSubnets

Associates the specified subnets in the Amazon VPC to the firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.AssociateSubnets
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SubnetMappings": [
    {
      "SubnetId": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### AttachRuleGroupsToProxyConfiguration

Attaches ProxyRuleGroup resources to a ProxyConfiguration A Proxy Configuration defines the monitoring and protection behavior for a Proxy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.AttachRuleGroupsToProxyConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "RuleGroups": [
    {}
  ],
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateContainerAssociation

Creates a Network Firewall container association.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateContainerAssociation
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 169
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ContainerAssociationName": "example",
  "Type": "ECS",
  "ContainerMonitoringConfigurations": [
    {
      "ClusterArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
    }
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 127
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation CreateContainerAssociation is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateFirewall

Creates an Network Firewall Firewall and accompanying FirewallStatus for a VPC.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateFirewall
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 111
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallName": "example",
  "FirewallPolicyArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 113
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "VpcId and SubnetMappings are both required to create a firewall."
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-createfirewall) for a successful call.

### CreateFirewallPolicy

Creates the firewall policy for the firewall according to the specifications.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateFirewallPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 135
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallPolicyName": "example",
  "FirewallPolicy": {
    "StatelessDefaultActions": [
      "example"
    ],
    "StatelessFragmentDefaultActions": [
      "example"
    ]
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 139
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "StatelessDefaultActions is invalid, parameter: [example], context: StatelessDefaultActions"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createfirewallpolicy) for a successful call.

### CreateProxy

Creates an Network Firewall Proxy Attaches a Proxy configuration to a NAT Gateway.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateProxy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ProxyName": "example",
  "NatGatewayId": "example",
  "TlsInterceptProperties": {}
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 112
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation CreateProxy is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateProxyConfiguration

Creates an Network Firewall ProxyConfiguration A Proxy Configuration defines the monitoring and protection behavior for a Proxy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateProxyConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 65
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ProxyConfigurationName": "example",
  "DefaultRulePhaseActions": {}
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation CreateProxyConfiguration is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateProxyRuleGroup

Creates an Network Firewall ProxyRuleGroup Collections of related proxy filtering rules.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateProxyRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 32
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ProxyRuleGroupName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 121
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation CreateProxyRuleGroup is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateProxyRules

Creates Network Firewall ProxyRule resources.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateProxyRules
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 12
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Rules": {}
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 117
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation CreateProxyRules is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateRuleGroup

Creates the specified stateless or stateful rule group, which includes the rules for network traffic inspection, a capacity setting, and tags.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "RuleGroupName": "example",
  "Type": "STATELESS",
  "Capacity": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 108
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "You must provide either the RuleGroup or the Rules setting."
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createrulegroup) for a successful call.

### CreateTLSInspectionConfiguration

Creates an Network Firewall TLS inspection configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateTLSInspectionConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TLSInspectionConfigurationName": "example",
  "TLSInspectionConfiguration": {}
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 133
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation CreateTLSInspectionConfiguration is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateVpcEndpointAssociation

Creates a firewall endpoint for an Network Firewall firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.CreateVpcEndpointAssociation
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 137
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "VpcId": "example",
  "SubnetMapping": {
    "SubnetId": "example"
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 184
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'vpcId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^vpc-[0-9a-f]+$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteContainerAssociation

Deletes a container association.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteContainerAssociation
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 127
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteContainerAssociation is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteFirewall

Deletes the specified Firewall and its FirewallStatus .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteFirewall
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-deletefirewall) for a successful call.

### DeleteFirewallPolicy

Deletes the specified FirewallPolicy .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteFirewallPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 116
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallPolicyName or FirewallPolicyArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#8-deletefirewallpolicy) for a successful call.

### DeleteNetworkFirewallTransitGatewayAttachment

Deletes a transit gateway attachment from a Network Firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteNetworkFirewallTransitGatewayAttachment
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TransitGatewayAttachmentId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 212
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'transitGatewayAttachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^tgw-attach-[0-9a-z]+$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteProxy

Deletes the specified Proxy .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteProxy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "NatGatewayId": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 112
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteProxy is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteProxyConfiguration

Deletes the specified ProxyConfiguration .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteProxyConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteProxyConfiguration is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteProxyRuleGroup

Deletes the specified ProxyRuleGroup .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteProxyRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 121
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteProxyRuleGroup is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteProxyRules

Deletes the specified ProxyRule (s). currently attached to a ProxyRuleGroup

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteProxyRules
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 21
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Rules": [
    "example"
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 117
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteProxyRules is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteResourcePolicy

Deletes a resource policy that you created in a PutResourcePolicy request.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 116
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "No object with the provided name or resource identifier was found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRuleGroup

Deletes the specified RuleGroup .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 106
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of RuleGroupName or RuleGroupArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#9-deleterulegroup) for a successful call.

### DeleteTLSInspectionConfiguration

Deletes the specified TLSInspectionConfiguration .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteTLSInspectionConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 133
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteTLSInspectionConfiguration is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteVpcEndpointAssociation

Deletes the specified VpcEndpointAssociation .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DeleteVpcEndpointAssociation
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "VpcEndpointAssociationArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 129
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteVpcEndpointAssociation is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeContainerAssociation

Retrieves the configuration and status of a container association.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeContainerAssociation
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 129
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeContainerAssociation is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeFirewall

Returns the data objects for the specified firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeFirewall
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-describefirewall) for a successful call.

### DescribeFirewallMetadata

Returns the high-level information about a firewall, including the Availability Zones where the Firewall is currently in use.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeFirewallMetadata
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeFirewallMetadata is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeFirewallPolicy

Returns the data objects for the specified firewall policy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeFirewallPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 116
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallPolicyName or FirewallPolicyArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### DescribeFlowOperation

Returns key information about a specific flow operation.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeFlowOperation
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 137
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "FlowOperationId": "example00000000000000000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 226
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'flowOperationId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeLoggingConfiguration

Returns the logging configuration for the specified firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeLoggingConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### DescribeProxy

Returns the data objects for the specified proxy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeProxy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 114
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeProxy is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeProxyConfiguration

Returns the data objects for the specified proxy configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeProxyConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 127
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeProxyConfiguration is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeProxyRule

Returns the data objects for the specified proxy configuration for the specified proxy rule group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeProxyRule
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 27
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ProxyRuleName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 118
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeProxyRule is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeProxyRuleGroup

Returns the data objects for the specified proxy rule group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeProxyRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 123
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeProxyRuleGroup is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeResourcePolicy

Retrieves a resource policy that you created in a PutResourcePolicy request.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 116
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "No object with the provided name or resource identifier was found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeRuleGroup

Returns the data objects for the specified rule group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 106
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of RuleGroupName or RuleGroupArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### DescribeRuleGroupMetadata

High-level information about a rule group, returned by operations like create and describe.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeRuleGroupMetadata
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 126
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeRuleGroupMetadata is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeRuleGroupSummary

Returns detailed information for a stateful rule group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeRuleGroupSummary
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeRuleGroupSummary is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeTLSInspectionConfiguration

Returns the data objects for the specified TLS inspection configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeTLSInspectionConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 135
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeTLSInspectionConfiguration is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeVpcEndpointAssociation

Returns the data object for the specified VPC endpoint association.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DescribeVpcEndpointAssociation
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "VpcEndpointAssociationArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 131
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DescribeVpcEndpointAssociation is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DetachRuleGroupsFromProxyConfiguration

Detaches ProxyRuleGroup resources from a ProxyConfiguration A Proxy Configuration defines the monitoring and protection behavior for a Proxy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DetachRuleGroupsFromProxyConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DisassociateAvailabilityZones

Removes the specified Availability Zone associations from a transit gateway-attached firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DisassociateAvailabilityZones
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AvailabilityZoneMappings": [
    {
      "AvailabilityZone": "example"
    }
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 130
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation DisassociateAvailabilityZones is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateSubnets

Removes the specified subnet associations from the firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.DisassociateSubnets
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SubnetIds": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 328
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'subnetIds' failed to satisfy constraint: Member must satisfy constraint: [Member must have length less than or equal to 128, Member must have length greater than or equal to 1, Member must satisfy regular expression pattern: ^subnet-[0-9a-f]+$]"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetAnalysisReportResults

The results of a COMPLETED analysis report generated with StartAnalysisReport .

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.GetAnalysisReportResults
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 30
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AnalysisReportId": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation GetAnalysisReportResults is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListAnalysisReports

Returns a list of all traffic analysis reports generated within the last 30 days.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListAnalysisReports
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 120
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListAnalysisReports is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListContainerAssociations

Lists the container associations in your account and Region.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListContainerAssociations
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 126
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListContainerAssociations is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListFirewallPolicies

Retrieves the metadata for the firewall policies that you have defined.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListFirewallPolicies
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 23
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "FirewallPolicies": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListFirewalls

Retrieves the metadata for the firewalls that you have defined.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListFirewalls
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 16
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Firewalls": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListFlowOperationResults

Returns the results of a specific flow operation.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListFlowOperationResults
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 137
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "FlowOperationId": "example00000000000000000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 226
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'flowOperationId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListFlowOperations

Returns a list of all flow operations ran in a specific firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListFlowOperations
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 116
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "No object with the provided name or resource identifier was found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListProxies

Retrieves the metadata for the proxies that you have defined.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListProxies
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 112
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListProxies is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListProxyConfigurations

Retrieves the metadata for the proxy configuration that you have defined.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListProxyConfigurations
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 124
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListProxyConfigurations is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListProxyRuleGroups

Retrieves the metadata for the proxy rule groups that you have defined.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListProxyRuleGroups
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 120
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListProxyRuleGroups is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListRuleGroups

Retrieves the metadata for the rule groups that you have defined.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListRuleGroups
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 17
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "RuleGroups": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTLSInspectionConfigurations

Retrieves the metadata for the TLS inspection configurations that you have defined.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListTLSInspectionConfigurations
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 132
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListTLSInspectionConfigurations is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListTagsForResource

Retrieves the tags associated with the specified resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListTagsForResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 194
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "resource ARN does not name a firewall, firewall policy or rule group, parameter: [arn:aws:networkfirewall:us-east-1:000000000000:example/example]"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### ListVpcEndpointAssociations

Retrieves the metadata for the VPC endpoint associations that you have defined.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.ListVpcEndpointAssociations
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 128
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListVpcEndpointAssociations is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### PutResourcePolicy

Creates or updates an IAM policy for your rule group, firewall policy, or firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.PutResourcePolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "Policy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 186
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "resource ARN does not name a rule group or a firewall policy, parameter: [arn:aws:networkfirewall:us-east-1:000000000000:example/example]"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### RejectNetworkFirewallTransitGatewayAttachment

Rejects a transit gateway attachment request for Network Firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.RejectNetworkFirewallTransitGatewayAttachment
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TransitGatewayAttachmentId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 212
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'transitGatewayAttachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^tgw-attach-[0-9a-z]+$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### StartAnalysisReport

Generates a traffic analysis report for the timeframe and traffic type you specify.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.StartAnalysisReport
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AnalysisType": "TLS_SNI"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 120
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation StartAnalysisReport is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### StartFlowCapture

Begins capturing the flows in a firewall, according to the filters you define.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.StartFlowCapture
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "FlowFilters": [
    {}
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 117
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation StartFlowCapture is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### StartFlowFlush

Begins the flushing of traffic from the firewall, according to the filters you define.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.StartFlowFlush
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "FlowFilters": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 116
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "No object with the provided name or resource identifier was found"
}
```

`400` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TagResource

Adds the specified tags to the specified resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.TagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 125
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "Tags": [
    {
      "Key": "example",
      "Value": "example"
    }
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 194
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "resource ARN does not name a firewall, firewall policy or rule group, parameter: [arn:aws:networkfirewall:us-east-1:000000000000:example/example]"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### UntagResource

Removes the tags with the specified keys from the specified resource.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UntagResource
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 102
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ResourceArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "TagKeys": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 194
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "resource ARN does not name a firewall, firewall policy or rule group, parameter: [arn:aws:networkfirewall:us-east-1:000000000000:example/example]"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### UpdateAvailabilityZoneChangeProtection

Modifies the AvailabilityZoneChangeProtection setting for a transit gateway-attached firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateAvailabilityZoneChangeProtection
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 42
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "AvailabilityZoneChangeProtection": false
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 139
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation UpdateAvailabilityZoneChangeProtection is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateContainerAssociation

Updates the monitoring configurations and description of a container association.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateContainerAssociation
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 156
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "Type": "ECS",
  "ContainerMonitoringConfigurations": [
    {
      "ClusterArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
    }
  ],
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateFirewallAnalysisSettings

Enables specific types of firewall analysis on a specific firewall you define.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallAnalysisSettings
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### UpdateFirewallDeleteProtection

Modifies the flag, DeleteProtection , which indicates whether it is possible to delete the firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallDeleteProtection
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 26
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DeleteProtection": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### UpdateFirewallDescription

Modifies the description for the specified firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallDescription
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### UpdateFirewallEncryptionConfiguration

A complex type that contains settings for encryption of your firewall resources.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallEncryptionConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 138
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation UpdateFirewallEncryptionConfiguration is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateFirewallPolicy

Updates the properties of the specified firewall policy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallPolicy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 128
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UpdateToken": "example",
  "FirewallPolicy": {
    "StatelessDefaultActions": [
      "example"
    ],
    "StatelessFragmentDefaultActions": [
      "example"
    ]
  }
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateFirewallPolicyChangeProtection

Modifies the flag, ChangeProtection , which indicates whether it is possible to change the firewall.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallPolicyChangeProtection
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallPolicyChangeProtection": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### UpdateLoggingConfiguration

Sets the logging configuration for the specified firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateLoggingConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### UpdateProxy

Updates the properties of the specified proxy.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateProxy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "NatGatewayId": "example",
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateProxyConfiguration

Updates the properties of the specified proxy configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateProxyConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DefaultRulePhaseActions": {},
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateProxyRule

Updates the properties of the specified proxy rule.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateProxyRule
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "ProxyRuleName": "example",
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateProxyRuleGroupPriorities

Updates proxy rule group priorities within a proxy configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateProxyRuleGroupPriorities
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "RuleGroups": [
    {}
  ],
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateProxyRulePriorities

Updates proxy rule priorities within a proxy rule group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateProxyRulePriorities
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "RuleGroupRequestPhase": "PRE_DNS",
  "Rules": [
    {}
  ],
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateProxySettings

Modifies the proxy listener configuration of a proxy mode firewall.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateProxySettings
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0
content-length: 120
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation UpdateProxySettings is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateRuleGroup

Updates the rule settings for the specified rule group.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateRuleGroup
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateSubnetChangeProtection

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateSubnetChangeProtection
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 32
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "SubnetChangeProtection": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 104
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "InvalidRequestException",
  "Message": "At least one of FirewallName or FirewallArn must be set"
}
```

`400` `InvalidRequestException`: the placeholder values failed the operation's input validation.

### UpdateTLSInspectionConfiguration

Updates the TLS inspection configuration settings for the specified TLS inspection configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.0
X-Amz-Target: NetworkFirewall_20201112.UpdateTLSInspectionConfiguration
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 57
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "TLSInspectionConfiguration": {},
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/network-firewall/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS Network Firewall answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `Message`, `message`.
