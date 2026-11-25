# AWS Network Firewall

## Wire protocol

- **Protocol:** `awsJson1_0`
- **Request routing:** `POST /` with header `X-Amz-Target: NetworkFirewall_20201112.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `Network Firewall`
- **sigv4 signing name:** `network-firewall`
- **Endpoint prefix:** `network-firewall`
- **ARN namespace:** `network-firewall`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `network-firewall`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

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
X-Amz-Date: 20261116T124406Z
Content-Length: 210
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:44:06 GMT

{
  "UpdateToken": "a5668a9f-8f1c-4dd9-8094-99bbfac1018b",
  "RuleGroupResponse": {
    "RuleGroupArn": "arn:aws:network-firewall:us-east-1:000000000000:stateful-rulegroup/docs-demo-allow-https",
    "RuleGroupName": "docs-demo-allow-https",
    "RuleGroupId": "e374e972-f36a-4814-bfdd-dbe80f870fe2",
    "Type": "STATEFUL",
    "Capacity": 100,
    "RuleGroupStatus": "ACTIVE",
    "ConsumedCapacity": 1,
    "NumberOfAssociations": 0,
    "EncryptionConfiguration": {
      "KeyId": "AWS_OWNED_KMS_KEY",
      "Type": "AWS_OWNED_KMS_KEY"
    },
    "LastModifiedTime": 1794833046.2013283
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
X-Amz-Date: 20261116T124406Z
Content-Length: 314
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:44:06 GMT

{
  "UpdateToken": "55ae92dd-da63-4730-ad1e-22014f7211f1",
  "FirewallPolicyResponse": {
    "FirewallPolicyName": "docs-demo-policy",
    "FirewallPolicyArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy",
    "FirewallPolicyId": "b4f78d8c-ef22-4bcd-85f5-57ec78cc6a12",
    "FirewallPolicyStatus": "ACTIVE",
    "ConsumedStatelessRuleCapacity": 0,
    "ConsumedStatefulRuleCapacity": 100,
    "ConsumedStatefulDomainCapacity": 0,
    "NumberOfAssociations": 0,
    "EncryptionConfiguration": {
      "KeyId": "AWS_OWNED_KMS_KEY",
      "Type": "AWS_OWNED_KMS_KEY"
    },
    "LastModifiedTime": 1794833046.2324905
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
X-Amz-Date: 20261116T124406Z
Content-Length: 233
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:44:06 GMT

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
    "FirewallId": "2b836f48-32a4-4294-86ee-ef675b82c96d",
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
          "EndpointId": "vpce-000000000000000c3",
          "Status": "CREATING"
        },
        "Config": {
          "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy": {
            "SyncStatus": "IN_SYNC",
            "UpdateToken": "127dca65-360c-4531-9d21-4b97476e7460"
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
X-Amz-Date: 20261116T124408Z
Content-Length: 32
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallName": "docs-demo-fw"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 1075
date: Mon, 16 Nov 2026 12:44:08 GMT

{
  "UpdateToken": "127dca65-360c-4531-9d21-4b97476e7460",
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
    "FirewallId": "2b836f48-32a4-4294-86ee-ef675b82c96d",
    "Tags": [],
    "EncryptionConfiguration": {
      "KeyId": "AWS_OWNED_KMS_KEY",
      "Type": "AWS_OWNED_KMS_KEY"
    },
    "AvailabilityZoneChangeProtection": false,
    "NoSourcePreservation": false
  },
  "FirewallStatus": {
    "Status": "READY",
    "ConfigurationSyncStateSummary": "IN_SYNC",
    "SyncStates": {
      "us-east-1a": {
        "Attachment": {
          "SubnetId": "subnet-00000000000000007",
          "EndpointId": "vpce-000000000000000c3",
          "Status": "READY"
        },
        "Config": {
          "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy": {
            "SyncStatus": "IN_SYNC",
            "UpdateToken": "127dca65-360c-4531-9d21-4b97476e7460"
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
X-Amz-Date: 20261116T124408Z
Content-Length: 42
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallPolicyName": "docs-demo-policy"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 105
date: Mon, 16 Nov 2026 12:44:08 GMT

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
X-Amz-Date: 20261116T124408Z
Content-Length: 32
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallName": "docs-demo-fw"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 1028
date: Mon, 16 Nov 2026 12:44:08 GMT

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
    "FirewallId": "2b836f48-32a4-4294-86ee-ef675b82c96d",
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
          "EndpointId": "vpce-000000000000000c3",
          "Status": "DELETING"
        },
        "Config": {
          "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy": {
            "SyncStatus": "IN_SYNC",
            "UpdateToken": "127dca65-360c-4531-9d21-4b97476e7460"
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
X-Amz-Date: 20261116T124408Z
Content-Length: 32
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallName": "docs-demo-fw"
}
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0
content-length: 116
date: Mon, 16 Nov 2026 12:44:08 GMT

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
X-Amz-Date: 20261116T124408Z
Content-Length: 42
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "FirewallPolicyName": "docs-demo-policy"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0
content-length: 525
date: Mon, 16 Nov 2026 12:44:08 GMT

{
  "FirewallPolicyResponse": {
    "FirewallPolicyName": "docs-demo-policy",
    "FirewallPolicyArn": "arn:aws:network-firewall:us-east-1:000000000000:firewall-policy/docs-demo-policy",
    "FirewallPolicyId": "b4f78d8c-ef22-4bcd-85f5-57ec78cc6a12",
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
    "LastModifiedTime": 1794833048.7178829
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
X-Amz-Date: 20261116T124408Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/network-firewall/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:44:08 GMT

{
  "RuleGroupResponse": {
    "RuleGroupArn": "arn:aws:network-firewall:us-east-1:000000000000:stateful-rulegroup/docs-demo-allow-https",
    "RuleGroupName": "docs-demo-allow-https",
    "RuleGroupId": "e374e972-f36a-4814-bfdd-dbe80f870fe2",
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
    "LastModifiedTime": 1794833048.7296875
  }
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 85 operations, 3 accepted the placeholder call, 39 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AcceptNetworkFirewallTransitGatewayAttachment

Accepts a transit gateway attachment request for Network Firewall.

```http
POST / HTTP/1.1
X-Amz-Target: NetworkFirewall_20201112.AcceptNetworkFirewallTransitGatewayAttachment

{
  "TransitGatewayAttachmentId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.AssociateAvailabilityZones

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
X-Amz-Target: NetworkFirewall_20201112.AssociateFirewallPolicy

{
  "FirewallPolicyArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.AssociateSubnets

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
X-Amz-Target: NetworkFirewall_20201112.AttachRuleGroupsToProxyConfiguration

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
X-Amz-Target: NetworkFirewall_20201112.CreateContainerAssociation

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
X-Amz-Target: NetworkFirewall_20201112.CreateFirewall

{
  "FirewallName": "example",
  "FirewallPolicyArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.CreateFirewallPolicy

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
X-Amz-Target: NetworkFirewall_20201112.CreateProxy

{
  "ProxyName": "example",
  "NatGatewayId": "example",
  "TlsInterceptProperties": {}
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.CreateProxyConfiguration

{
  "ProxyConfigurationName": "example",
  "DefaultRulePhaseActions": {}
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.CreateProxyRuleGroup

{
  "ProxyRuleGroupName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.CreateProxyRules

{
  "Rules": {}
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.CreateRuleGroup

{
  "RuleGroupName": "example",
  "Type": "STATELESS",
  "Capacity": 1
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.CreateTLSInspectionConfiguration

{
  "TLSInspectionConfigurationName": "example",
  "TLSInspectionConfiguration": {}
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.CreateVpcEndpointAssociation

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
X-Amz-Target: NetworkFirewall_20201112.DeleteContainerAssociation

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteFirewall

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteFirewallPolicy

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteNetworkFirewallTransitGatewayAttachment

{
  "TransitGatewayAttachmentId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteProxy

{
  "NatGatewayId": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteProxyConfiguration

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteProxyRuleGroup

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteProxyRules

{
  "Rules": [
    "example"
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteResourcePolicy

{
  "ResourceArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteRuleGroup

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteTLSInspectionConfiguration

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DeleteVpcEndpointAssociation

{
  "VpcEndpointAssociationArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeContainerAssociation

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeFirewall

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeFirewallMetadata

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeFirewallPolicy

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeFlowOperation

{
  "FirewallArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "FlowOperationId": "example00000000000000000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeLoggingConfiguration

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeProxy

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeProxyConfiguration

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeProxyRule

{
  "ProxyRuleName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeProxyRuleGroup

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeResourcePolicy

{
  "ResourceArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeRuleGroup

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeRuleGroupMetadata

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeRuleGroupSummary

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeTLSInspectionConfiguration

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DescribeVpcEndpointAssociation

{
  "VpcEndpointAssociationArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DetachRuleGroupsFromProxyConfiguration

{
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.DisassociateAvailabilityZones

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
X-Amz-Target: NetworkFirewall_20201112.DisassociateSubnets

{
  "SubnetIds": [
    "example"
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.GetAnalysisReportResults

{
  "AnalysisReportId": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.ListAnalysisReports

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.ListContainerAssociations

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.ListFirewallPolicies

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "FirewallPolicies": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListFirewalls

Retrieves the metadata for the firewalls that you have defined.

```http
POST / HTTP/1.1
X-Amz-Target: NetworkFirewall_20201112.ListFirewalls

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "Firewalls": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListFlowOperationResults

Returns the results of a specific flow operation.

```http
POST / HTTP/1.1
X-Amz-Target: NetworkFirewall_20201112.ListFlowOperationResults

{
  "FirewallArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "FlowOperationId": "example00000000000000000000000000000"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.ListFlowOperations

{
  "FirewallArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.ListProxies

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.ListProxyConfigurations

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.ListProxyRuleGroups

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.ListRuleGroups

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.0

{
  "RuleGroups": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTLSInspectionConfigurations

Retrieves the metadata for the TLS inspection configurations that you have defined.

```http
POST / HTTP/1.1
X-Amz-Target: NetworkFirewall_20201112.ListTLSInspectionConfigurations

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.ListTagsForResource

{
  "ResourceArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.ListVpcEndpointAssociations

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.PutResourcePolicy

{
  "ResourceArn": "arn:aws:networkfirewall:us-east-1:000000000000:example/example",
  "Policy": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.RejectNetworkFirewallTransitGatewayAttachment

{
  "TransitGatewayAttachmentId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.StartAnalysisReport

{
  "AnalysisType": "TLS_SNI"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.StartFlowCapture

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
X-Amz-Target: NetworkFirewall_20201112.StartFlowFlush

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
X-Amz-Target: NetworkFirewall_20201112.TagResource

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
X-Amz-Target: NetworkFirewall_20201112.UntagResource

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
X-Amz-Target: NetworkFirewall_20201112.UpdateAvailabilityZoneChangeProtection

{
  "AvailabilityZoneChangeProtection": false
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateContainerAssociation

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

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateFirewallAnalysisSettings

Enables specific types of firewall analysis on a specific firewall you define.

```http
POST / HTTP/1.1
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallAnalysisSettings

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

{
  "__type": "NotImplemented",
  "message": "operation UpdateFirewallAnalysisSettings is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateFirewallDeleteProtection

Modifies the flag, DeleteProtection , which indicates whether it is possible to delete the firewall.

```http
POST / HTTP/1.1
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallDeleteProtection

{
  "DeleteProtection": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallDescription

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallEncryptionConfiguration

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallPolicy

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
X-Amz-Target: NetworkFirewall_20201112.UpdateFirewallPolicyChangeProtection

{
  "FirewallPolicyChangeProtection": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateLoggingConfiguration

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateProxy

{
  "NatGatewayId": "example",
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateProxyConfiguration

{
  "DefaultRulePhaseActions": {},
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateProxyRule

{
  "ProxyRuleName": "example",
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateProxyRuleGroupPriorities

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
X-Amz-Target: NetworkFirewall_20201112.UpdateProxyRulePriorities

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
X-Amz-Target: NetworkFirewall_20201112.UpdateProxySettings

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateRuleGroup

{
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value at 'updateToken' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9a-f]{8})-([0-9a-f]{4}-){3}([0-9a-f]{12})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateSubnetChangeProtection

```http
POST / HTTP/1.1
X-Amz-Target: NetworkFirewall_20201112.UpdateSubnetChangeProtection

{
  "SubnetChangeProtection": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
X-Amz-Target: NetworkFirewall_20201112.UpdateTLSInspectionConfiguration

{
  "TLSInspectionConfiguration": {},
  "UpdateToken": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.0

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
