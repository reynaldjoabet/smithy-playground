# Amazon Route 53 Domains

## Wire protocol

- **Protocol:** `awsJson1_1`
- **Request routing:** `POST /` with header `X-Amz-Target: Route53Domains_v20140515.<OperationName>`, body is JSON with the modeled member names.
- **SDK id:** `Route 53 Domains`
- **sigv4 signing name:** `route53domains`
- **Endpoint prefix:** `route53domains`
- **ARN namespace:** `route53domains`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `route53domains`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListDomains`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:route53domains" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-amz-json-1.1' \
  -H 'X-Amz-Target: Route53Domains_v20140515.ListDomains' \
  --data-binary '{}' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Check a domain, register it, inspect it and the operation that registered it. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CheckDomainAvailability

Availability of a name under a supported TLD.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.CheckDomainAvailability
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 38
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "docs-demo-example.com"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 91
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "Availability": "AVAILABLE",
  "Price": {
    "Price": 16.0,
    "Currency": "USD",
    "PriceType": "STANDARD"
  }
}
```

### 2. RegisterDomain

Registration is asynchronous: the response is only an operation id.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.RegisterDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 752
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "docs-demo-example.com",
  "DurationInYears": 1,
  "AdminContact": {
    "FirstName": "Ada",
    "LastName": "Lovelace",
    "ContactType": "PERSON",
    "AddressLine1": "1 Main St",
    "City": "Seattle",
    "State": "WA",
    "CountryCode": "US",
    "ZipCode": "98101",
    "PhoneNumber": "+1.2065550100",
    "Email": "ada@example.com"
  },
  "RegistrantContact": {
    "FirstName": "Ada",
    "LastName": "Lovelace",
    "ContactType": "PERSON",
    "AddressLine1": "1 Main St",
    "City": "Seattle",
    "State": "WA",
    "CountryCode": "US",
    "ZipCode": "98101",
    "PhoneNumber": "+1.2065550100",
    "Email": "ada@example.com"
  },
  "TechContact": {
    "FirstName": "Ada",
    "LastName": "Lovelace",
    "ContactType": "PERSON",
    "AddressLine1": "1 Main St",
    "City": "Seattle",
    "State": "WA",
    "CountryCode": "US",
    "ZipCode": "98101",
    "PhoneNumber": "+1.2065550100",
    "Email": "ada@example.com"
  }
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 54
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "OperationId": "52cd0871-d6cf-46ca-ad00-7c24a97e65e9"
}
```

### 3. GetOperationDetail

The operation's status and type.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.GetOperationDetail
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "OperationId": "52cd0871-d6cf-46ca-ad00-7c24a97e65e9"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 201
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "OperationId": "52cd0871-d6cf-46ca-ad00-7c24a97e65e9",
  "Status": "SUBMITTED",
  "DomainName": "docs-demo-example.com",
  "Type": "REGISTER_DOMAIN",
  "SubmittedDate": 1795717850.675,
  "LastUpdatedDate": 1795717850.675
}
```

### 4. ListDomains

Registered domains of the account.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.ListDomains
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 113
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "Domains": [
    {
      "DomainName": "docs-demo-example.com",
      "AutoRenew": true,
      "TransferLock": true,
      "Expiry": 1827253850.675
    }
  ]
}
```

### 5. GetDomainDetail

Contacts, name servers and expiry of the registered domain.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.GetDomainDetail
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 38
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "docs-demo-example.com"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 1156
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "DomainName": "docs-demo-example.com",
  "Nameservers": [
    {
      "Name": "ns-650.awsdns-05.com"
    },
    {
      "Name": "ns-3148.awsdns-36.net"
    },
    {
      "Name": "ns-2897.awsdns-32.org"
    },
    {
      "Name": "ns-73.awsdns-27.co.uk"
    }
  ],
  "AutoRenew": true,
  "AdminContact": {
    "FirstName": "Ada",
    "LastName": "Lovelace",
    "ContactType": "PERSON",
    "AddressLine1": "1 Main St",
    "City": "Seattle",
    "State": "WA",
    "CountryCode": "US",
    "ZipCode": "98101",
    "PhoneNumber": "+1.2065550100",
    "Email": "ada@example.com"
  },
  "RegistrantContact": {
    "FirstName": "Ada",
    "LastName": "Lovelace",
    "ContactType": "PERSON",
    "AddressLine1": "1 Main St",
    "City": "Seattle",
    "State": "WA",
    "CountryCode": "US",
    "ZipCode": "98101",
    "PhoneNumber": "+1.2065550100",
    "Email": "ada@example.com"
  },
  "TechContact": {
    "FirstName": "Ada",
    "LastName": "Lovelace",
    "ContactType": "PERSON",
    "AddressLine1": "1 Main St",
    "City": "Seattle",
    "State": "WA",
    "CountryCode": "US",
    "ZipCode": "98101",
    "PhoneNumber": "+1.2065550100",
    "Email": "ada@example.com"
  },
  "AdminPrivacy": true,
  "RegistrantPrivacy": true,
  "TechPrivacy": true,
  "RegistrarName": "Amazon Registrar, Inc.",
  "CreationDate": 1795717850.675,
  "UpdatedDate": 1795717850.675,
  "ExpirationDate": 1827253850.675,
… (5 more lines)
```

### 6. DeleteDomain

Deletion is also asynchronous and returns an operation id.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.DeleteDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 38
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "docs-demo-example.com"
}
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 54
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "OperationId": "0a88f1f3-e77c-42c8-afbf-aaab7bae2dc2"
}
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 35 operations, 3 accepted the placeholder call, 19 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AcceptDomainTransferFromAnotherAwsAccount

Accepts the transfer of a domain from another Amazon Web Services account to the currentAmazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.AcceptDomainTransferFromAnotherAwsAccount
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "Password": "<redacted>"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 142
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation AcceptDomainTransferFromAnotherAwsAccount is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### AssociateDelegationSignerToDomain

Creates a delegation signer (DS) record in the registry zone for this domain name.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.AssociateDelegationSignerToDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "SigningAttributes": {}
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 134
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation AssociateDelegationSignerToDomain is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CancelDomainTransferToAnotherAwsAccount

Cancels the transfer of a domain from the current Amazon Web Services account to another Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.CancelDomainTransferToAnotherAwsAccount
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 140
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation CancelDomainTransferToAnotherAwsAccount is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CheckDomainAvailability

This operation checks the availability of one domain name.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.CheckDomainAvailability
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-checkdomainavailability) for a successful call.

### CheckDomainTransferability

Checks whether a domain name can be transferred to Amazon Route 53.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.CheckDomainTransferability
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### DeleteDomain

This operation deletes the specified domain.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.DeleteDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-deletedomain) for a successful call.

### DeleteTagsForDomain

This operation deletes the specified tags for a domain.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.DeleteTagsForDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "TagsToDelete": [
    "example"
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 120
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation DeleteTagsForDomain is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisableDomainAutoRenew

This operation disables automatic renewal of domain registration for the specified domain.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.DisableDomainAutoRenew
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### DisableDomainTransferLock

This operation removes the transfer lock on the domain (specifically the clientTransferProhibited status) to allow domain transfers.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.DisableDomainTransferLock
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### DisassociateDelegationSignerFromDomain

Deletes a delegation signer (DS) record in the registry zone for this domain name.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.DisassociateDelegationSignerFromDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 39
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "Id": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 139
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation DisassociateDelegationSignerFromDomain is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableDomainAutoRenew

This operation configures Amazon Route 53 to automatically renew the specified domain before the domain registration expires.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.EnableDomainAutoRenew
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### EnableDomainTransferLock

This operation sets the transfer lock on the domain (specifically the clientTransferProhibited status) to prevent domain transfers.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.EnableDomainTransferLock
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 125
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation EnableDomainTransferLock is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetContactReachabilityStatus

For operations that require confirmation that the email address for the registrant contact is valid, such as registering a new domain, this operation returns information about whether the registrant contact has responded.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.GetContactReachabilityStatus
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 92
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Invalid request. Problems: Domain name cannot be blank"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetDomainDetail

This operation returns detailed information about a specified domain that is associated with the current Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.GetDomainDetail
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#5-getdomaindetail) for a successful call.

### GetDomainPrices

Returns the following prices for the specified domain: Registration Transfer Owner change Renewal Restoration For most domains, the prices match the standard prices for the TLD that the ListPrices operation returns.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.GetDomainPrices
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 116
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation GetDomainPrices is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetDomainSuggestions

The GetDomainSuggestions operation returns a list of suggested domain names.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.GetDomainSuggestions
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "SuggestionCount": 1,
  "OnlyAvailable": false
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetOperationDetail

This operation returns the current status of an operation that is not completed.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.GetOperationDetail
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "OperationId": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 109
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Invalid request. Problems: No operation found for operationId : example"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-getoperationdetail) for a successful call.

### ListDomains

This operation returns all the domain names registered with Amazon Route 53 for the current Amazon Web Services account if no filtering conditions are used.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.ListDomains
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 14
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "Domains": []
}
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-listdomains) for a successful call.

### ListOperations

Returns information about all of the operations that return an operation ID and that have ever been performed on domains that were registered by the current account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.ListOperations
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 418
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "Operations": [
    {
      "OperationId": "52cd0871-d6cf-46ca-ad00-7c24a97e65e9",
      "Status": "SUBMITTED",
      "Type": "REGISTER_DOMAIN",
      "SubmittedDate": 1795717850.675,
      "DomainName": "docs-demo-example.com",
      "LastUpdatedDate": 1795717850.675
    },
    {
      "OperationId": "0a88f1f3-e77c-42c8-afbf-aaab7bae2dc2",
      "Status": "SUBMITTED",
      "Type": "DELETE_DOMAIN",
      "SubmittedDate": 1795717850.679,
      "DomainName": "docs-demo-example.com",
      "LastUpdatedDate": 1795717850.679
    }
  ]
}
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListPrices

Lists the following prices for either all the TLDs supported by Route 53, or the specified TLD: Registration Transfer Owner change Renewal Restoration The prices that ListPrices returns are the standard rates for a TLD.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.ListPrices
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 200 OK
content-type: application/x-amz-json-1.1
content-length: 9944
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "Prices": [
    {
      "Name": "ai",
      "RegistrationPrice": {
        "Price": 110.0,
        "Currency": "USD",
        "PriceType": "STANDARD"
      },
      "TransferPrice": {
        "Price": 110.0,
        "Currency": "USD",
        "PriceType": "STANDARD"
      },
      "RenewalPrice": {
        "Price": 110.0,
        "Currency": "USD",
        "PriceType": "STANDARD"
      },
      "ChangeOwnershipPrice": {
… (712 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsForDomain

This operation returns all of the tags that are associated with the specified domain.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.ListTagsForDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### PushDomain

Moves a domain from Amazon Web Services to another registrar.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.PushDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "Target": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 111
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation PushDomain is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RegisterDomain

This operation registers a domain.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.RegisterDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 102
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "DurationInYears": 1,
  "AdminContact": {},
  "RegistrantContact": {},
  "TechContact": {}
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-registerdomain) for a successful call.

### RejectDomainTransferFromAnotherAwsAccount

Rejects the transfer of a domain from another Amazon Web Services account to the current Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.RejectDomainTransferFromAnotherAwsAccount
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 142
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation RejectDomainTransferFromAnotherAwsAccount is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RenewDomain

This operation renews a domain for the specified number of years.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.RenewDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "CurrentExpiryYear": 1
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 112
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation RenewDomain is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ResendContactReachabilityEmail

For operations that require confirmation that the email address for the registrant contact is valid, such as registering a new domain, this operation resends the confirmation email to the current email address for the registrant contact.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.ResendContactReachabilityEmail
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 131
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation ResendContactReachabilityEmail is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ResendOperationAuthorization

Resend the form of authorization email for this operation.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.ResendOperationAuthorization
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 25
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "OperationId": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 129
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation ResendOperationAuthorization is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RetrieveDomainAuthCode

This operation returns the authorization code for the domain.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.RetrieveDomainAuthCode
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 123
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation RetrieveDomainAuthCode is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### TransferDomain

Transfers a domain from another registrar to Amazon Route 53.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.TransferDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 82
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "AdminContact": {},
  "RegistrantContact": {},
  "TechContact": {}
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 115
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation TransferDomain is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### TransferDomainToAnotherAwsAccount

Transfers a domain from the current Amazon Web Services account to another Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.TransferDomainToAnotherAwsAccount
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "AccountId": "100000000000"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 134
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation TransferDomainToAnotherAwsAccount is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateDomainContact

This operation updates the contact information for a particular domain.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.UpdateDomainContact
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 120
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation UpdateDomainContact is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateDomainContactPrivacy

This operation updates the specified domain contact's privacy setting.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.UpdateDomainContactPrivacy
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 127
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation UpdateDomainContactPrivacy is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateDomainNameservers

This operation replaces the current set of name servers for the domain with the specified set of name servers.

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.UpdateDomainNameservers
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example",
  "Nameservers": [
    {
      "Name": "example"
    }
  ]
}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 124
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation UpdateDomainNameservers is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateTagsForDomain

This operation adds or updates tags for a specified domain.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.UpdateTagsForDomain
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 24
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{
  "DomainName": "example"
}
```

```http
HTTP/1.1 400 Bad Request
content-type: application/x-amz-json-1.1
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "InvalidInput",
  "message": "Value 'example' at 'domainName' failed to satisfy constraint: Member must be a domain name of one label under a top-level domain, where every label is made up of letters, digits and interior hyphens"
}
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ViewBilling

Returns all the domain-related billing records for the current Amazon Web Services account for a specified period

```http
POST / HTTP/1.1
Content-Type: application/x-amz-json-1.1
X-Amz-Target: Route53Domains_v20140515.ViewBilling
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 2
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53domains/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=<signature>

{}
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/x-amz-json-1.1
content-length: 112
date: Thu, 26 Nov 2026 18:30:50 GMT

{
  "__type": "NotImplemented",
  "message": "operation ViewBilling is scaffolded but not implemented in this emulator"
}
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/route53domains/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with the operation named in the `X-Amz-Target` header and a JSON body of the input's member names.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Route 53 Domains answers in AWS: a JSON body. Errors captured on this page are a JSON body with `__type`, `message`.
