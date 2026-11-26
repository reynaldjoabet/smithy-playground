# AWS Network Manager

## Wire protocol

- **Protocol:** `restJson1`
- **SDK id:** `NetworkManager`
- **sigv4 signing name:** `networkmanager`
- **Endpoint prefix:** `networkmanager`
- **ARN namespace:** `networkmanager`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `networkmanager`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `DescribeGlobalNetworks`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:networkmanager" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  'https://127.0.0.1:9297/global-networks'
```

## End-to-end walkthrough

Model a global network with a site, a device and a link, then remove it. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateGlobalNetwork

The global network is the container for everything else.

**Request**

```http
POST /global-networks HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 27
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "Description": "docs demo"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 265
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "GlobalNetwork": {
    "GlobalNetworkId": "global-network-00000000000000001",
    "GlobalNetworkArn": "arn:aws:networkmanager::000000000000:global-network/global-network-00000000000000001",
    "Description": "docs demo",
    "CreatedAt": 1795717847.7038841,
    "State": "PENDING",
    "Tags": []
  }
}
```

### 2. CreateSite

A site is a physical location.

**Request**

```http
POST /global-networks/global-network-00000000000000001/sites HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 108
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "Description": "Seattle office",
  "Location": {
    "Address": "Seattle, WA",
    "Latitude": "47.6",
    "Longitude": "-122.3"
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 375
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Site": {
    "SiteId": "site-00000000000000002",
    "SiteArn": "arn:aws:networkmanager::000000000000:site/global-network-00000000000000001/site-00000000000000002",
    "GlobalNetworkId": "global-network-00000000000000001",
    "Description": "Seattle office",
    "Location": {
      "Address": "Seattle, WA",
      "Latitude": "47.6",
      "Longitude": "-122.3"
    },
    "CreatedAt": 1795717847.7059965,
    "State": "PENDING",
    "Tags": []
  }
}
```

### 3. CreateDevice

A device lives at a site.

**Request**

```http
POST /global-networks/global-network-00000000000000001/devices HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "Description": "edge router",
  "Type": "router",
  "Vendor": "example",
  "SiteId": "site-00000000000000002"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 377
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Device": {
    "DeviceId": "device-00000000000000003",
    "DeviceArn": "arn:aws:networkmanager::000000000000:device/global-network-00000000000000001/device-00000000000000003",
    "GlobalNetworkId": "global-network-00000000000000001",
    "Description": "edge router",
    "Type": "router",
    "Vendor": "example",
    "SiteId": "site-00000000000000002",
    "CreatedAt": 1795717847.7076519,
    "State": "PENDING",
    "Tags": []
  }
}
```

### 4. CreateLink

A link is a site's connection to the internet.

**Request**

```http
POST /global-networks/global-network-00000000000000001/links HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 139
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "Description": "ISP uplink",
  "Bandwidth": {
    "UploadSpeed": 100,
    "DownloadSpeed": 500
  },
  "Provider": "example-isp",
  "SiteId": "site-00000000000000002"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 406
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Link": {
    "LinkId": "link-00000000000000004",
    "LinkArn": "arn:aws:networkmanager::000000000000:link/global-network-00000000000000001/link-00000000000000004",
    "GlobalNetworkId": "global-network-00000000000000001",
    "SiteId": "site-00000000000000002",
    "Description": "ISP uplink",
    "Bandwidth": {
      "UploadSpeed": 100,
      "DownloadSpeed": 500
    },
    "Provider": "example-isp",
    "CreatedAt": 1795717847.7091522,
    "State": "PENDING",
    "Tags": []
  }
}
```

### 5. GetDevices

Devices of the global network.

**Request**

```http
GET /global-networks/global-network-00000000000000001/devices HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 380
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Devices": [
    {
      "DeviceId": "device-00000000000000003",
      "DeviceArn": "arn:aws:networkmanager::000000000000:device/global-network-00000000000000001/device-00000000000000003",
      "GlobalNetworkId": "global-network-00000000000000001",
      "Description": "edge router",
      "Type": "router",
      "Vendor": "example",
      "SiteId": "site-00000000000000002",
      "CreatedAt": 1795717847.7076519,
      "State": "PENDING",
      "Tags": []
    }
  ]
}
```

### 6. DeleteSite

A site that still has a device and a link associated with it is refused with a `ValidationException`.

**Request**

```http
DELETE /global-networks/global-network-00000000000000001/sites/site-00000000000000002 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 144
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Site with ID site-00000000000000002 cannot be deleted due to existing association.",
  "Reason": "Other"
}
```

### 7. DeleteLink

Remove the link ...

**Request**

```http
DELETE /global-networks/global-network-00000000000000001/links/link-00000000000000004 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 407
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Link": {
    "LinkId": "link-00000000000000004",
    "LinkArn": "arn:aws:networkmanager::000000000000:link/global-network-00000000000000001/link-00000000000000004",
    "GlobalNetworkId": "global-network-00000000000000001",
    "SiteId": "site-00000000000000002",
    "Description": "ISP uplink",
    "Bandwidth": {
      "UploadSpeed": 100,
      "DownloadSpeed": 500
    },
    "Provider": "example-isp",
    "CreatedAt": 1795717847.7091522,
    "State": "DELETING",
    "Tags": []
  }
}
```

### 8. DeleteDevice

... the device ...

**Request**

```http
DELETE /global-networks/global-network-00000000000000001/devices/device-00000000000000003 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 378
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Device": {
    "DeviceId": "device-00000000000000003",
    "DeviceArn": "arn:aws:networkmanager::000000000000:device/global-network-00000000000000001/device-00000000000000003",
    "GlobalNetworkId": "global-network-00000000000000001",
    "Description": "edge router",
    "Type": "router",
    "Vendor": "example",
    "SiteId": "site-00000000000000002",
    "CreatedAt": 1795717847.7076519,
    "State": "DELETING",
    "Tags": []
  }
}
```

### 9. DeleteSite

... the site ...

**Request**

```http
DELETE /global-networks/global-network-00000000000000001/sites/site-00000000000000002 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 376
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Site": {
    "SiteId": "site-00000000000000002",
    "SiteArn": "arn:aws:networkmanager::000000000000:site/global-network-00000000000000001/site-00000000000000002",
    "GlobalNetworkId": "global-network-00000000000000001",
    "Description": "Seattle office",
    "Location": {
      "Address": "Seattle, WA",
      "Latitude": "47.6",
      "Longitude": "-122.3"
    },
    "CreatedAt": 1795717847.7059965,
    "State": "DELETING",
    "Tags": []
  }
}
```

### 10. DeleteGlobalNetwork

... and the global network.

**Request**

```http
DELETE /global-networks/global-network-00000000000000001 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 266
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "GlobalNetwork": {
    "GlobalNetworkId": "global-network-00000000000000001",
    "GlobalNetworkArn": "arn:aws:networkmanager::000000000000:global-network/global-network-00000000000000001",
    "Description": "docs demo",
    "CreatedAt": 1795717847.7038841,
    "State": "DELETING",
    "Tags": []
  }
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 95 operations, 8 accepted the placeholder call, 1 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AcceptAttachment

Accepts a core network attachment request.

```http
POST /attachments/example/accept HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 215
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### AssociateConnectPeer

Associates a core network Connect peer with a device and optionally, with a link.

```http
POST /global-networks/example/connect-peer-associations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 48
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "ConnectPeerId": "example",
  "DeviceId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'connectPeerId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^connect-peer-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### AssociateCustomerGateway

Associates a customer gateway with a device and optionally, with a link.

```http
POST /global-networks/example/customer-gateway-associations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 107
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "CustomerGatewayArn": "arn:aws:networkmanager:us-east-1:000000000000:example/example",
  "DeviceId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 270
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "customerGatewayArn is not a customer gateway ARN",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "customerGatewayArn",
      "Message": "must read arn:<partition>:ec2:<region>:<account>:customer-gateway/cgw-<8 to 17 hex digits>"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### AssociateLink

Associates a link to a device.

```http
POST /global-networks/example/link-associations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 41
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "DeviceId": "example",
  "LinkId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 179
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Global network not found.",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "globalNetworkId",
      "Message": "Global network not found."
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### AssociateTransitGatewayConnectPeer

Associates a transit gateway Connect peer with a device, and optionally, with a link.

```http
POST /global-networks/example/transit-gateway-connect-peer-associations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 117
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "TransitGatewayConnectPeerArn": "arn:aws:networkmanager:us-east-1:000000000000:example/example",
  "DeviceId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 270
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "must name a transit gateway Connect peer and the Region it is in",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "transitGatewayConnectPeerArn",
      "Message": "must name a transit gateway Connect peer and the Region it is in"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateConnectAttachment

Creates a core network Connect attachment from a specified core network attachment.

```http
POST /connect-attachments HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 152
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "CoreNetworkId": "example",
  "EdgeLocation": "example",
  "TransportAttachmentId": "example",
  "Options": {},
  "ClientToken": "1a631405-73b6-46ee-bbdf-c568b2349b00"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 371
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$; Value 'example' at 'transportAttachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateConnectPeer

Creates a core network Connect peer for a specified core network connect attachment between a core network and an appliance.

```http
POST /connect-peers HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 110
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "ConnectAttachmentId": "example",
  "PeerAddress": "example",
  "ClientToken": "6706f90b-8d45-4e21-99ca-c055e21effcb"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 222
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'connectAttachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateConnection

Creates a connection between two devices.

```http
POST /global-networks/example/connections HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "DeviceId": "example",
  "ConnectedDeviceId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 179
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Global network not found.",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "globalNetworkId",
      "Message": "Global network not found."
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateCoreNetwork

Creates a core network as part of your global network, and optionally, with a core network policy.

```http
POST /core-networks HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 82
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "GlobalNetworkId": "example",
  "ClientToken": "ca447fb2-03a6-4430-973c-bf004723667d"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 179
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Global network not found.",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "globalNetworkId",
      "Message": "Global network not found."
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateCoreNetworkPrefixListAssociation

Creates an association between a core network and a prefix list for routing control.

```http
POST /prefix-list HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 188
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "CoreNetworkId": "example",
  "PrefixListArn": "arn:aws:networkmanager:us-east-1:000000000000:example/example",
  "PrefixListAlias": "example",
  "ClientToken": "cc13b84c-0316-4e84-a3a1-e14071fe14ef"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateDevice

Creates a new device in a global network.

```http
POST /global-networks/example/devices HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 179
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Global network not found.",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "globalNetworkId",
      "Message": "Global network not found."
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-createdevice) for a successful call.

### CreateDirectConnectGatewayAttachment

Creates an Amazon Web Services Direct Connect gateway attachment

```http
POST /direct-connect-gateway-attachments HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 144
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "CoreNetworkId": "example",
  "DirectConnectGatewayArn": "example",
  "EdgeLocations": [
    "example"
  ],
  "ClientToken": "44c47bd0-ba93-4064-be3d-94b767a91c97"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 441
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$; Value 'example' at 'directConnectGatewayArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:[^:]{1,63}:directconnect::[^:]{0,63}:dx-gateway\\/[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateGlobalNetwork

Creates a new, empty global network.

```http
POST /global-networks HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 239
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "GlobalNetwork": {
    "GlobalNetworkId": "global-network-00000000000000005",
    "GlobalNetworkArn": "arn:aws:networkmanager::000000000000:global-network/global-network-00000000000000005",
    "CreatedAt": 1795717847.7284489,
    "State": "PENDING",
    "Tags": []
  }
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#1-createglobalnetwork) for a successful call.

### CreateLink

Creates a new link for a specified site.

```http
POST /global-networks/example/links HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 35
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "Bandwidth": {},
  "SiteId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 150
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Site not found.",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "siteId",
      "Message": "Site not found."
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-createlink) for a successful call.

### CreateSite

Creates a new site in a global network.

```http
POST /global-networks/example/sites HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 179
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Global network not found.",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "globalNetworkId",
      "Message": "Global network not found."
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createsite) for a successful call.

### CreateSiteToSiteVpnAttachment

Creates an Amazon Web Services site-to-site VPN attachment on an edge location of a core network.

```http
POST /site-to-site-vpn-attachments HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 109
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "CoreNetworkId": "example",
  "VpnConnectionArn": "example",
  "ClientToken": "647e6cbe-3019-480c-98df-7cb22f720b2a"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 415
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$; Value 'example' at 'vpnConnectionArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:[^:]{1,63}:ec2:[^:]{0,63}:[^:]{0,63}:vpn-connection\\/vpn-[0-9a-f]{8,17}$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateTransitGatewayPeering

Creates a transit gateway peering connection.

```http
POST /transit-gateway-peerings HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 164
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "CoreNetworkId": "example",
  "TransitGatewayArn": "arn:aws:networkmanager:us-east-1:000000000000:example/example",
  "ClientToken": "91d0f77f-6e42-47ba-a4a1-83d3ffe7092d"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateTransitGatewayRouteTableAttachment

Creates a transit gateway route table attachment.

```http
POST /transit-gateway-route-table-attachments HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 170
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "PeeringId": "example",
  "TransitGatewayRouteTableArn": "arn:aws:networkmanager:us-east-1:000000000000:example/example",
  "ClientToken": "94485be5-fc5e-4211-8bfe-8505962ff646"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 209
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'peeringId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^peering-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### CreateVpcAttachment

Creates a VPC attachment on an edge location of a core network.

```http
POST /vpc-attachments HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 124
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "CoreNetworkId": "example",
  "VpcArn": "example",
  "SubnetArns": [
    "example"
  ],
  "ClientToken": "5e9b317e-722d-4497-8b12-6bec686ce75a"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 591
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "3 validation errors detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$; Value 'example' at 'vpcArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:[^:]{1,63}:ec2:[^:]{0,63}:[^:]{0,63}:vpc\\/vpc-[0-9a-f]{8,17}$; Value 'example' at 'subnetArns.1.member' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:[^:]{1,63}:ec2:[^:]{0,63}:[^:]{0,63}:subnet\\/subnet-[0-9a-f]{8,17}$|^$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteAttachment

Deletes an attachment.

```http
DELETE /attachments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 215
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteConnectPeer

Deletes a Connect peer.

```http
DELETE /connect-peers/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'connectPeerId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^connect-peer-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteConnection

Deletes the specified connection in your global network.

```http
DELETE /global-networks/example/connections/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteCoreNetwork

Deletes a core network along with all core network policies.

```http
DELETE /core-networks/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteCoreNetworkPolicyVersion

Deletes a policy version from a core network.

```http
DELETE /core-networks/example/core-network-policy-versions/1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteCoreNetworkPrefixListAssociation

Deletes an association between a core network and a prefix list.

```http
DELETE /prefix-list/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample/core-network/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteDevice

Deletes an existing device.

```http
DELETE /global-networks/example/devices/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#8-deletedevice) for a successful call.

### DeleteGlobalNetwork

Deletes an existing global network.

```http
DELETE /global-networks/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#10-deleteglobalnetwork) for a successful call.

### DeleteLink

Deletes an existing link.

```http
DELETE /global-networks/example/links/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deletelink) for a successful call.

### DeletePeering

Deletes an existing peering connection.

```http
DELETE /peerings/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 209
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'peeringId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^peering-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DeleteResourcePolicy

Deletes a resource policy for the specified resource.

```http
DELETE /resource-policy/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 2
date: Thu, 26 Nov 2026 18:30:47 GMT

{}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteSite

Deletes an existing site.

```http
DELETE /global-networks/example/sites/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#9-deletesite) for a successful call.

### DeregisterTransitGateway

Deregisters a transit gateway from your global network.

```http
DELETE /global-networks/example/transit-gateway-registrations/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 233
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "must name a transit gateway and the Region it is in",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "transitGatewayArn",
      "Message": "must name a transit gateway and the Region it is in"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DescribeGlobalNetworks

Describes one or more global networks.

```http
GET /global-networks HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 242
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "GlobalNetworks": [
    {
      "GlobalNetworkId": "global-network-00000000000000005",
      "GlobalNetworkArn": "arn:aws:networkmanager::000000000000:global-network/global-network-00000000000000005",
      "CreatedAt": 1795717847.7284489,
      "State": "PENDING",
      "Tags": []
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DisassociateConnectPeer

Disassociates a core network Connect peer from a device and a link.

```http
DELETE /global-networks/example/connect-peer-associations/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'connectPeerId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^connect-peer-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DisassociateCustomerGateway

Disassociates a customer gateway from a device and a link.

```http
DELETE /global-networks/example/customer-gateway-associations/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 270
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "customerGatewayArn is not a customer gateway ARN",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "customerGatewayArn",
      "Message": "must read arn:<partition>:ec2:<region>:<account>:customer-gateway/cgw-<8 to 17 hex digits>"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### DisassociateLink

Disassociates an existing device from a link.

```http
DELETE /global-networks/example/link-associations?deviceId=example&linkId=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisassociateTransitGatewayConnectPeer

Disassociates a transit gateway Connect peer from a device and link.

```http
DELETE /global-networks/example/transit-gateway-connect-peer-associations/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 270
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "must name a transit gateway Connect peer and the Region it is in",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "transitGatewayConnectPeerArn",
      "Message": "must name a transit gateway Connect peer and the Region it is in"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ExecuteCoreNetworkChangeSet

Executes a change set on your core network.

```http
POST /core-networks/example/core-network-change-sets/1/execute HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetConnectAttachment

Returns information about a core network Connect attachment.

```http
GET /connect-attachments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 215
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetConnectPeer

Returns information about a core network Connect peer.

```http
GET /connect-peers/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'connectPeerId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^connect-peer-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetConnectPeerAssociations

Returns information about a core network Connect peer associations.

```http
GET /global-networks/example/connect-peer-associations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetConnections

Gets information about one or more of your connections in a global network.

```http
GET /global-networks/example/connections HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetCoreNetwork

Returns information about the LIVE policy for a core network.

```http
GET /core-networks/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetCoreNetworkChangeEvents

Returns information about a core network change event.

```http
GET /core-networks/example/core-network-change-events/1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetCoreNetworkChangeSet

Returns a change set between the LIVE core network policy and a submitted policy.

```http
GET /core-networks/example/core-network-change-sets/1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetCoreNetworkPolicy

Returns details about a core network policy.

```http
GET /core-networks/example/core-network-policy HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetCustomerGatewayAssociations

Gets the association information for customer gateways that are associated with devices and links in your global network.

```http
GET /global-networks/example/customer-gateway-associations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetDevices

Gets information about one or more of your devices in a global network.

```http
GET /global-networks/example/devices HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#5-getdevices) for a successful call.

### GetDirectConnectGatewayAttachment

Returns information about a specific Amazon Web Services Direct Connect gateway attachment.

```http
GET /direct-connect-gateway-attachments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 215
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetLinkAssociations

Gets the link associations for a device or a link.

```http
GET /global-networks/example/link-associations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 282
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "either DeviceId or LinkId must be specified",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "DeviceId",
      "Message": "either DeviceId or LinkId must be specified"
    },
    {
      "Name": "LinkId",
      "Message": "either DeviceId or LinkId must be specified"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetLinks

Gets information about one or more links in a specified global network.

```http
GET /global-networks/example/links HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetNetworkResourceCounts

Gets the count of network resources, by resource type, for the specified global network.

```http
GET /global-networks/example/network-resource-count HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 179
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Global network not found.",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "globalNetworkId",
      "Message": "Global network not found."
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetNetworkResourceRelationships

Gets the network resource relationships for the specified global network.

```http
GET /global-networks/example/network-resource-relationships HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetNetworkResources

Describes the network resources for the specified global network.

```http
GET /global-networks/example/network-resources HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetNetworkRoutes

Gets the network routes of the specified global network.

```http
POST /global-networks/example/network-routes HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 27
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "RouteTableIdentifier": {}
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetNetworkTelemetry

Gets the network telemetry of the specified global network.

```http
GET /global-networks/example/network-telemetry HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetResourcePolicy

Returns information about a resource policy.

```http
GET /resource-policy/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 2
date: Thu, 26 Nov 2026 18:30:47 GMT

{}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetRouteAnalysis

Gets information about the specified route analysis.

```http
GET /global-networks/example/route-analyses/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetSiteToSiteVpnAttachment

Returns information about a site-to-site VPN attachment.

```http
GET /site-to-site-vpn-attachments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 215
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetSites

Gets information about one or more of your sites in a global network.

```http
GET /global-networks/example/sites HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTransitGatewayConnectPeerAssociations

Gets information about one or more of your transit gateway Connect peer associations in a global network.

```http
GET /global-networks/example/transit-gateway-connect-peer-associations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTransitGatewayPeering

Returns information about a transit gateway peer.

```http
GET /transit-gateway-peerings/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 209
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'peeringId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^peering-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetTransitGatewayRegistrations

Gets information about the transit gateway registrations in a specified global network.

```http
GET /global-networks/example/transit-gateway-registrations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTransitGatewayRouteTableAttachment

Returns information about a transit gateway route table attachment.

```http
GET /transit-gateway-route-table-attachments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 215
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### GetVpcAttachment

Returns information about a VPC attachment.

```http
GET /vpc-attachments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 215
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListAttachmentRoutingPolicyAssociations

Lists the routing policy associations for attachments in a core network.

```http
GET /routing-policy-label/core-network/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListAttachments

Returns a list of core network attachments.

```http
GET /attachments HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 18
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Attachments": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListConnectPeers

Returns a list of core network Connect peers.

```http
GET /connect-peers HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 19
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "ConnectPeers": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListCoreNetworkPolicyVersions

Returns a list of core network policy versions.

```http
GET /core-networks/example/core-network-policy-versions HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListCoreNetworkPrefixListAssociations

Lists the prefix list associations for a core network.

```http
GET /prefix-list/core-network/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListCoreNetworkRoutingInformation

Lists routing information for a core network, including routes and their attributes.

```http
POST /core-networks/example/core-network-routing-information HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "SegmentName": "example",
  "EdgeLocation": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ListCoreNetworks

Returns a list of owned and shared core networks.

```http
GET /core-networks HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/json
x-amzn-errortype: NotImplemented
content-length: 117
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "NotImplemented",
  "message": "operation ListCoreNetworks is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListOrganizationServiceAccessStatus

Gets the status of the Service Linked Role (SLR) deployment for the accounts in a given Amazon Web Services Organization.

```http
GET /organizations/service-access HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 2
date: Thu, 26 Nov 2026 18:30:47 GMT

{}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListPeerings

Lists the peerings for a core network.

```http
GET /peerings HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/json
content-length: 15
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "Peerings": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsForResource

Lists the tags for a specified resource.

```http
GET /tags/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 118
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "example"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PutAttachmentRoutingPolicyLabel

Applies a routing policy label to an attachment for traffic routing decisions.

```http
POST /routing-policy-label HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 136
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "CoreNetworkId": "example",
  "AttachmentId": "example",
  "RoutingPolicyLabel": "example",
  "ClientToken": "7dcf5eb0-ea92-40a4-89c3-5e291aba5d62"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 362
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$; Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutCoreNetworkPolicy

Creates a new, immutable version of a core network policy.

```http
POST /core-networks/example/core-network-policy HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "PolicyDocument": "example",
  "ClientToken": "47187213-b8b2-49c1-bb32-90e0476e757e"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### PutResourcePolicy

Creates or updates a resource policy.

```http
POST /resource-policy/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 28
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "PolicyDocument": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 206
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Invalid policy.",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "ResourceArn",
      "Message": "must read arn:<partition>:directconnect::<account>:dx-gateway/<id>"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RegisterTransitGateway

Registers a transit gateway in your global network.

```http
POST /global-networks/example/transit-gateway-registrations HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 85
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "TransitGatewayArn": "arn:aws:networkmanager:us-east-1:000000000000:example/example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 233
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "must name a transit gateway and the Region it is in",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "transitGatewayArn",
      "Message": "must name a transit gateway and the Region it is in"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RejectAttachment

Rejects a core network attachment request.

```http
POST /attachments/example/reject HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 215
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RemoveAttachmentRoutingPolicyLabel

Removes a routing policy label from an attachment.

```http
DELETE /routing-policy-label/core-network/example/attachment/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 362
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "2 validation errors detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$; Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RestoreCoreNetworkPolicyVersion

Restores a previous policy version as a new, immutable version of a core network policy.

```http
POST /core-networks/example/core-network-policy-versions/1/restore HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### StartOrganizationServiceAccessUpdate

Enables the Network Manager service for an Amazon Web Services Organization.

```http
POST /organizations/service-access HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 20
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "Action": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 194
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Action must be one of ENABLE, DISABLE",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "Action",
      "Message": "Action must be one of ENABLE, DISABLE"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### StartRouteAnalysis

Starts analyzing the routing path between the specified source and destination.

```http
POST /global-networks/example/route-analyses HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 30
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "Source": {},
  "Destination": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 179
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "Global network not found.",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "globalNetworkId",
      "Message": "Global network not found."
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### TagResource

Tags a specified resource.

```http
POST /tags/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 13
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "Tags": [
    {}
  ]
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 172
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "Message": "a tag key must not be empty",
  "Reason": "FieldValidationFailed",
  "Fields": [
    {
      "Name": "Tags",
      "Message": "a tag key must not be empty"
    }
  ]
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UntagResource

Removes tags from a specified resource.

```http
DELETE /tags/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample?tagKeys=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 118
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "example"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateConnection

Updates the information for an existing connection.

```http
PATCH /global-networks/example/connections/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateCoreNetwork

Updates the description of a core network.

```http
PATCH /core-networks/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 218
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'coreNetworkId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^core-network-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateDevice

Updates the details for an existing device.

```http
PATCH /global-networks/example/devices/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateDirectConnectGatewayAttachment

Updates the edge locations associated with an Amazon Web Services Direct Connect gateway attachment.

```http
PATCH /direct-connect-gateway-attachments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 215
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### UpdateGlobalNetwork

Updates an existing global network.

```http
PATCH /global-networks/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateLink

Updates the details for an existing link.

```http
PATCH /global-networks/example/links/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateNetworkResourceMetadata

Updates the resource metadata for the specified global network.

```http
PATCH /global-networks/example/network-resources/arn%3Aaws%3Anetworkmanager%3Aus-east-1%3A000000000000%3Aexample%2Fexample/metadata HTTP/1.1
Content-Type: application/json
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Content-Length: 30
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

{
  "Metadata": {
    "key": "example"
  }
}
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateSite

Updates the information for an existing site.

```http
PATCH /global-networks/example/sites/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/json
x-amzn-errortype: ResourceNotFoundException
content-length: 125
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ResourceNotFoundException",
  "Message": "Resource not found.",
  "ResourceId": "example",
  "ResourceType": "global-network"
}
```

`404` `ResourceNotFoundException`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateVpcAttachment

Updates a VPC attachment.

```http
PATCH /vpc-attachments/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183047Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/networkmanager/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/json
x-amzn-errortype: ValidationException
content-length: 215
date: Thu, 26 Nov 2026 18:30:47 GMT

{
  "__type": "ValidationException",
  "message": "1 validation error detected: Value 'example' at 'attachmentId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^attachment-([0-9a-f]{8,17})$"
}
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/networkmanager/aws4_request` in the `Authorization` header) and the operation from the request itself: an HTTP method and path per operation (the `HTTP binding`), with identifiers in the path, options in the query string or headers, and the rest of the input as a JSON body.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way AWS Network Manager answers in AWS: a JSON body and an HTTP status chosen per operation. Errors captured on this page are a JSON body with `__type`, `Message`, `message`, plus the `x-amzn-errortype` header.
