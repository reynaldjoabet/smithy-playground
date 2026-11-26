# Amazon Route 53

## Wire protocol

- **Protocol:** `restXml`
- **SDK id:** `Route 53`
- **sigv4 signing name:** `route53`
- **Endpoint prefix:** `route53`
- **ARN namespace:** `route53`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `route53`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed. Every header is shown exactly as it went over the wire, in order: the client sends no `User-Agent`, and each response keeps all of the emulator's headers.

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `ListHostedZones`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:route53" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  'https://127.0.0.1:9297/2013-04-01/hostedzone'
```

## End-to-end walkthrough

Create a hosted zone, add an A record, read it back, then delete both. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateHostedZone

Answers 201 with the zone, its change status and its four name servers.

**Request**

```http
POST /2013-04-01/hostedzone HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 216
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateHostedZoneRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Name>docs-demo.example.com</Name>
  <CallerReference>docs-demo-2026-11-16</CallerReference>
</CreateHostedZoneRequest>
```

**Response**

```http
HTTP/1.1 201 Created
content-type: application/xml
location: https://127.0.0.1:9297/2013-04-01/hostedzone/ZUBBF64LSMDW8FT
content-length: 1454
date: Thu, 26 Nov 2026 18:30:50 GMT

<CreateHostedZoneResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZone>
    <Id>/hostedzone/ZUBBF64LSMDW8FT</Id>
    <Name>docs-demo.example.com.</Name>
    <CallerReference>docs-demo-2026-11-16</CallerReference>
    <Config>
      <PrivateZone>false</PrivateZone>
      <HostedZoneTier>STANDARD</HostedZoneTier>
    </Config>
    <ResourceRecordSetCount>2</ResourceRecordSetCount>
    <Features>
      <AcceleratedRecoveryStatus>DISABLED</AcceleratedRecoveryStatus>
      <ChangeGuardrails>
        <ChangeGuardrail>
          <Name>NS_OCCLUSION_CHECK</Name>
          <Status>DISABLED</Status>
        </ChangeGuardrail>
        <ChangeGuardrail>
          <Name>NS_DELEGATION_AUTHORITY_CHECK</Name>
          <Status>DISABLED</Status>
        </ChangeGuardrail>
        <ChangeGuardrail>
          <Name>NS_FORMAT_CHECK</Name>
          <Status>DISABLED</Status>
        </ChangeGuardrail>
        <ChangeGuardrail>
          <Name>ALIAS_TARGET_CHECK</Name>
          <Status>DISABLED</Status>
        </ChangeGuardrail>
        <ChangeGuardrail>
          <Name>ACTIVE_RECORD_CHECK</Name>
          <Status>DISABLED</Status>
        </ChangeGuardrail>
        <ChangeGuardrail>
          <Name>ACTIVE_DELEGATION_CHECK</Name>
          <Status>DISABLED</Status>
        </ChangeGuardrail>
      </ChangeGuardrails>
    </Features>
  </HostedZone>
  <ChangeInfo>
    <Id>/change/CZNFE2VZH6165EG</Id>
    <Status>PENDING</Status>
    <SubmittedAt>2026-11-26T18:30:50.817Z</SubmittedAt>
  </ChangeInfo>
  <DelegationSet>
    <NameServers>
      <NameServer>ns-1716.awsdns-62.com</NameServer>
      <NameServer>ns-1108.awsdns-27.net</NameServer>
      <NameServer>ns-1832.awsdns-57.org</NameServer>
      <NameServer>ns-587.awsdns-62.co.uk</NameServer>
    </NameServers>
  </DelegationSet>
</CreateHostedZoneResponse>
```

### 2. ChangeResourceRecordSets

Record changes are sent as a batch in an XML body; the response is a change id in `PENDING`.

**Request**

```http
POST /2013-04-01/hostedzone/ZUBBF64LSMDW8FT/rrset HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 427
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<ChangeResourceRecordSetsRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeBatch>
    <Changes>
      <Change>
        <Action>UPSERT</Action>
        <ResourceRecordSet>
          <Name>www.docs-demo.example.com</Name>
          <Type>A</Type>
          <TTL>300</TTL>
          <ResourceRecords>
            <ResourceRecord>
              <Value>192.0.2.10</Value>
            </ResourceRecord>
          </ResourceRecords>
        </ResourceRecordSet>
      </Change>
    </Changes>
  </ChangeBatch>
</ChangeResourceRecordSetsRequest>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 255
date: Thu, 26 Nov 2026 18:30:50 GMT

<ChangeResourceRecordSetsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeInfo>
    <Id>/change/CZIFIES9F28W6QU</Id>
    <Status>PENDING</Status>
    <SubmittedAt>2026-11-26T18:30:50.819Z</SubmittedAt>
  </ChangeInfo>
</ChangeResourceRecordSetsResponse>
```

### 3. GetChange

Poll the change until it reports `INSYNC`, meaning it has reached every Route 53 name server.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
GET /2013-04-01/change/CZIFIES9F28W6QU HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 225
date: Thu, 26 Nov 2026 18:30:50 GMT

<GetChangeResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeInfo>
    <Id>/change/CZIFIES9F28W6QU</Id>
    <Status>PENDING</Status>
    <SubmittedAt>2026-11-26T18:30:50.819Z</SubmittedAt>
  </ChangeInfo>
</GetChangeResponse>
```

### 4. ListResourceRecordSets

The zone holds the SOA and NS records Route 53 created plus the new A record.

**Request**

```http
GET /2013-04-01/hostedzone/ZUBBF64LSMDW8FT/rrset HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 1101
date: Thu, 26 Nov 2026 18:30:50 GMT

<ListResourceRecordSetsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ResourceRecordSets>
    <ResourceRecordSet>
      <Name>docs-demo.example.com.</Name>
      <Type>NS</Type>
      <TTL>172800</TTL>
      <ResourceRecords>
        <ResourceRecord>
          <Value>ns-1716.awsdns-62.com.</Value>
        </ResourceRecord>
        <ResourceRecord>
          <Value>ns-1108.awsdns-27.net.</Value>
        </ResourceRecord>
        <ResourceRecord>
          <Value>ns-1832.awsdns-57.org.</Value>
        </ResourceRecord>
        <ResourceRecord>
          <Value>ns-587.awsdns-62.co.uk.</Value>
        </ResourceRecord>
      </ResourceRecords>
    </ResourceRecordSet>
    <ResourceRecordSet>
      <Name>docs-demo.example.com.</Name>
      <Type>SOA</Type>
      <TTL>900</TTL>
      <ResourceRecords>
        <ResourceRecord>
          <Value>ns-1716.awsdns-62.com. awsdns-hostmaster.amazon.com. 1 7200 900 1209600 86400</Value>
        </ResourceRecord>
      </ResourceRecords>
    </ResourceRecordSet>
    <ResourceRecordSet>
      <Name>www.docs-demo.example.com.</Name>
      <Type>A</Type>
      <TTL>300</TTL>
      <ResourceRecords>
        <ResourceRecord>
          <Value>192.0.2.10</Value>
        </ResourceRecord>
      </ResourceRecords>
    </ResourceRecordSet>
  </ResourceRecordSets>
  <IsTruncated>false</IsTruncated>
  <MaxItems>100</MaxItems>
</ListResourceRecordSetsResponse>
```

### 5. DeleteHostedZone

A zone that still holds records of its own is refused with `HostedZoneNotEmpty`.

**Request**

```http
DELETE /2013-04-01/hostedzone/ZUBBF64LSMDW8FT HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 397
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>HostedZoneNotEmpty</Code>
    <Message>The specified hosted zone contains resource record sets other than the default SOA and NS records and so cannot be deleted. Delete all non-default records and try again.</Message>
  </Error>
  <RequestId>0fefdb28-47f8-ca0a-65ec-d8692c61d023</RequestId>
</ErrorResponse>
```

### 6. ChangeResourceRecordSets

Delete the A record (the exact record must be repeated) ...

**Request**

```http
POST /2013-04-01/hostedzone/ZUBBF64LSMDW8FT/rrset HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 427
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<ChangeResourceRecordSetsRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeBatch>
    <Changes>
      <Change>
        <Action>DELETE</Action>
        <ResourceRecordSet>
          <Name>www.docs-demo.example.com</Name>
          <Type>A</Type>
          <TTL>300</TTL>
          <ResourceRecords>
            <ResourceRecord>
              <Value>192.0.2.10</Value>
            </ResourceRecord>
          </ResourceRecords>
        </ResourceRecordSet>
      </Change>
    </Changes>
  </ChangeBatch>
</ChangeResourceRecordSetsRequest>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 255
date: Thu, 26 Nov 2026 18:30:50 GMT

<ChangeResourceRecordSetsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeInfo>
    <Id>/change/C8OPXNIS6RL9U34</Id>
    <Status>PENDING</Status>
    <SubmittedAt>2026-11-26T18:30:50.824Z</SubmittedAt>
  </ChangeInfo>
</ChangeResourceRecordSetsResponse>
```

### 7. DeleteHostedZone

... and the zone deletes.

**Request**

```http
DELETE /2013-04-01/hostedzone/ZUBBF64LSMDW8FT HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 239
date: Thu, 26 Nov 2026 18:30:50 GMT

<DeleteHostedZoneResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeInfo>
    <Id>/change/CE0PCTJNQFFXW7H</Id>
    <Status>PENDING</Status>
    <SubmittedAt>2026-11-26T18:30:50.825Z</SubmittedAt>
  </ChangeInfo>
</DeleteHostedZoneResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 71 operations, 16 accepted the placeholder call, 1 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.629* answered `501` in 1.0.595. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### ActivateKeySigningKey

Activates a key-signing key (KSK) so that it can be used for signing by DNSSEC.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST /2013-04-01/keysigningkey/example/example/activate HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 266
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchKeySigningKey</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>3b93b215-e9ba-8f48-8120-6cdb0352bdd4</RequestId>
</ErrorResponse>
```

`404` `NoSuchKeySigningKey`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AssociateVPCWithHostedZone

Associates an Amazon VPC with a private hosted zone.

```http
POST /2013-04-01/hostedzone/example/associatevpc HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 153
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<AssociateVPCWithHostedZoneRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <VPC/>
</AssociateVPCWithHostedZoneRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 279
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidVPCId</Code>
    <Message>Invalid VPC ID: &quot;&quot; is not a valid Amazon VPC ID</Message>
  </Error>
  <RequestId>e2800949-ebcc-802a-f714-9a97393d9c3f</RequestId>
</ErrorResponse>
```

`400` `InvalidVPCId`.

### ChangeCidrCollection

Creates, changes, or deletes CIDR blocks within a collection.

```http
POST /2013-04-01/cidrcollection/example HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 268
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<ChangeCidrCollectionRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Changes>
    <member>
      <LocationName>example</LocationName>
      <Action>PUT</Action>
      <CidrList>
        <Cidr>example</Cidr>
      </CidrList>
    </member>
  </Changes>
</ChangeCidrCollectionRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 439
date: Thu, 26 Nov 2026 18:30:50 GMT

<?xml version="1.0"?>
<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>1 validation error detected: Value &apos;example&apos; at &apos;id&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$</Message>
  </Error>
  <RequestId>c96fbacb-2a70-96e2-ae6f-79b05ba5f204</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ChangeResourceRecordSets

Creates, changes, or deletes a resource record set, which contains authoritative DNS information for a specified domain name or subdomain name.

```http
POST /2013-04-01/hostedzone/example/rrset HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 304
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<ChangeResourceRecordSetsRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeBatch>
    <Changes>
      <Change>
        <Action>CREATE</Action>
        <ResourceRecordSet>
          <Name>example</Name>
          <Type>SOA</Type>
        </ResourceRecordSet>
      </Change>
    </Changes>
  </ChangeBatch>
</ChangeResourceRecordSetsRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 431
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid request: Expected exactly one of [AliasTarget, all of [TTL, and ResourceRecords], or TrafficPolicyInstanceId], but found none in Change with [Action=CREATE, Name=example., Type=SOA, SetIdentifier=null]</Message>
  </Error>
  <RequestId>cc5a1f92-3495-c54f-2fcf-d8ac731c0dc6</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-changeresourcerecordsets) for a successful call.

### ChangeTagsForResource

Adds, edits, or deletes tags for a health check or a hosted zone.

```http
POST /2013-04-01/tags/healthcheck/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 273
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>f8dd9402-fbf6-0d49-42d3-143caa83c040</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateCidrCollection

Creates a CIDR collection in the current Amazon Web Services account.

```http
POST /2013-04-01/cidrcollection HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 197
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateCidrCollectionRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Name>example</Name>
  <CallerReference>example</CallerReference>
</CreateCidrCollectionRequest>
```

```http
HTTP/1.1 201 Created
content-type: application/xml
location: https://127.0.0.1:9297/2013-04-01/cidrcollection/04901489-b6d7-c741-4622-f150d9bd988f
content-length: 305
date: Thu, 26 Nov 2026 18:30:50 GMT

<CreateCidrCollectionResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Collection>
    <Arn>arn:aws:route53:::cidrcollection/04901489-b6d7-c741-4622-f150d9bd988f</Arn>
    <Id>04901489-b6d7-c741-4622-f150d9bd988f</Id>
    <Name>example</Name>
    <Version>1</Version>
  </Collection>
</CreateCidrCollectionResponse>
```

Accepted: the emulator answered `201 Created` even with placeholder values.

### CreateHealthCheck

Creates a new health check.

```http
POST /2013-04-01/healthcheck HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 227
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateHealthCheckRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <CallerReference>example</CallerReference>
  <HealthCheckConfig>
    <Type>HTTP</Type>
  </HealthCheckConfig>
</CreateHealthCheckRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 345
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid request: a HTTP health check must specify HealthCheckConfig.IPAddress or HealthCheckConfig.FullyQualifiedDomainName</Message>
  </Error>
  <RequestId>6fb366a2-3ad0-1ef4-38b9-8b796643b3d9</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### CreateHostedZone

Creates a new public or private hosted zone.

```http
POST /2013-04-01/hostedzone HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 189
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateHostedZoneRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Name>example</Name>
  <CallerReference>example</CallerReference>
</CreateHostedZoneRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 254
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidDomainName</Code>
    <Message>example is reserved by AWS!</Message>
  </Error>
  <RequestId>bc8d4fa2-13eb-878c-ec1f-8d6552679b88</RequestId>
</ErrorResponse>
```

`400` `InvalidDomainName`. See [the walkthrough](#1-createhostedzone) for a successful call.

### CreateKeySigningKey

Creates a new key-signing key (KSK) associated with a hosted zone.

```http
POST /2013-04-01/keysigningkey HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 360
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateKeySigningKeyRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <CallerReference>example</CallerReference>
  <HostedZoneId>example</HostedZoneId>
  <KeyManagementServiceArn>arn:aws:route53:us-east-1:000000000000:example/example</KeyManagementServiceArn>
  <Name>example</Name>
  <Status>example</Status>
</CreateKeySigningKeyRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 333
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidKeySigningKeyStatus</Code>
    <Message>The key-signing key (KSK) status isn&apos;t valid or another KSK has the status INTERNAL_FAILURE.</Message>
  </Error>
  <RequestId>003f4539-1535-a9b6-3da4-bcd57c3838c4</RequestId>
</ErrorResponse>
```

`400` `InvalidKeySigningKeyStatus`.

### CreateQueryLoggingConfig

Creates a configuration for DNS query logging.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST /2013-04-01/queryloggingconfig HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 288
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateQueryLoggingConfigRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZoneId>example</HostedZoneId>
  <CloudWatchLogsLogGroupArn>arn:aws:route53:us-east-1:000000000000:example/example</CloudWatchLogsLogGroupArn>
</CreateQueryLoggingConfigRequest>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>6bf6bc73-71f6-6ae4-18f4-bb7be0973156</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateReusableDelegationSet

Creates a delegation set (a group of four name servers) that can be reused by multiple hosted zones that were created by the same Amazon Web Services account.

```http
POST /2013-04-01/delegationset HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 191
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateReusableDelegationSetRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <CallerReference>example</CallerReference>
</CreateReusableDelegationSetRequest>
```

```http
HTTP/1.1 201 Created
content-type: application/xml
location: https://127.0.0.1:9297/2013-04-01/delegationset/N0XBS6XRVL4A8H6
content-length: 453
date: Thu, 26 Nov 2026 18:30:50 GMT

<CreateReusableDelegationSetResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <DelegationSet>
    <Id>/delegationset/N0XBS6XRVL4A8H6</Id>
    <CallerReference>example</CallerReference>
    <NameServers>
      <NameServer>ns-127.awsdns-62.com</NameServer>
      <NameServer>ns-1494.awsdns-62.net</NameServer>
      <NameServer>ns-1706.awsdns-36.org</NameServer>
      <NameServer>ns-1455.awsdns-37.co.uk</NameServer>
    </NameServers>
  </DelegationSet>
</CreateReusableDelegationSetResponse>
```

Accepted: the emulator answered `201 Created` even with placeholder values.

### CreateTrafficPolicy

Creates a traffic policy, which you use to create multiple DNS resource record sets for one domain name (such as example.com) or one subdomain name (such as www.example.com).

```http
POST /2013-04-01/trafficpolicy HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 181
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateTrafficPolicyRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Name>example</Name>
  <Document>example</Document>
</CreateTrafficPolicyRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 297
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidTrafficPolicyDocument</Code>
    <Message>The traffic policy document must be declared as valid JSON.</Message>
  </Error>
  <RequestId>e01e4850-4deb-d71e-8441-63760bdc3345</RequestId>
</ErrorResponse>
```

`400` `InvalidTrafficPolicyDocument`.

### CreateTrafficPolicyInstance

Creates resource record sets in a specified hosted zone based on the settings in a specified traffic policy version.

```http
POST /2013-04-01/trafficpolicyinstance HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 305
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateTrafficPolicyInstanceRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZoneId>example</HostedZoneId>
  <Name>example</Name>
  <TTL>0</TTL>
  <TrafficPolicyId>example</TrafficPolicyId>
  <TrafficPolicyVersion>1</TrafficPolicyVersion>
</CreateTrafficPolicyInstanceRequest>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>bd16f28f-0aef-d890-6ef7-a8bde23fb5e5</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateTrafficPolicyVersion

Creates a new version of an existing traffic policy.

```http
POST /2013-04-01/trafficpolicy/example HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 175
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateTrafficPolicyVersionRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Document>example</Document>
</CreateTrafficPolicyVersionRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 297
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidTrafficPolicyDocument</Code>
    <Message>The traffic policy document must be declared as valid JSON.</Message>
  </Error>
  <RequestId>944aae45-9cea-76b6-da50-60582a5d47b9</RequestId>
</ErrorResponse>
```

`400` `InvalidTrafficPolicyDocument`.

### CreateVPCAssociationAuthorization

Authorizes the Amazon Web Services account that created a specified VPC to submit an AssociateVPCWithHostedZone request to associate the VPC with a specified hosted zone that was created by a different account.

```http
POST /2013-04-01/hostedzone/example/authorizevpcassociation HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 167
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<CreateVPCAssociationAuthorizationRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <VPC/>
</CreateVPCAssociationAuthorizationRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 279
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidVPCId</Code>
    <Message>Invalid VPC ID: &quot;&quot; is not a valid Amazon VPC ID</Message>
  </Error>
  <RequestId>5d1abb25-9234-97c1-4724-c37b0c55a090</RequestId>
</ErrorResponse>
```

`400` `InvalidVPCId`.

### DeactivateKeySigningKey

Deactivates a key-signing key (KSK) so that it will not be used for signing by DNSSEC.

```http
POST /2013-04-01/keysigningkey/example/example/deactivate HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 266
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchKeySigningKey</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>eb00662d-efb4-3f41-e0e1-affabdcd6d3b</RequestId>
</ErrorResponse>
```

`404` `NoSuchKeySigningKey`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteCidrCollection

Deletes a CIDR collection in the current Amazon Web Services account.

```http
DELETE /2013-04-01/cidrcollection/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 439
date: Thu, 26 Nov 2026 18:30:50 GMT

<?xml version="1.0"?>
<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>1 validation error detected: Value &apos;example&apos; at &apos;id&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$</Message>
  </Error>
  <RequestId>140421d5-731e-5c37-003e-892fd687ba86</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### DeleteHealthCheck

Deletes a health check.

```http
DELETE /2013-04-01/healthcheck/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 273
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>e1e09332-0c2f-2318-0fed-c497bc494355</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteHostedZone

Deletes a hosted zone.

```http
DELETE /2013-04-01/hostedzone/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>87cf0419-94e6-5333-845e-3c6284123242</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deletehostedzone) for a successful call.

### DeleteKeySigningKey

Deletes a key-signing key (KSK).

```http
DELETE /2013-04-01/keysigningkey/example/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 266
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchKeySigningKey</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>ba87bc25-0d07-37e2-813c-0498ac3763c1</RequestId>
</ErrorResponse>
```

`404` `NoSuchKeySigningKey`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteQueryLoggingConfig

Deletes a configuration for DNS query logging.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
DELETE /2013-04-01/queryloggingconfig/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 281
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchQueryLoggingConfig</Code>
    <Message>The query logging configuration does not exist.</Message>
  </Error>
  <RequestId>949fef6a-2c85-7929-fb1b-4d40965d09ec</RequestId>
</ErrorResponse>
```

`404` `NoSuchQueryLoggingConfig`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteReusableDelegationSet

Deletes a reusable delegation set.

```http
DELETE /2013-04-01/delegationset/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchDelegationSet</Code>
    <Message>example</Message>
  </Error>
  <RequestId>9f810589-4940-e9b3-d95e-db3bda986175</RequestId>
</ErrorResponse>
```

`400` `NoSuchDelegationSet`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTrafficPolicy

Deletes a traffic policy.

```http
DELETE /2013-04-01/trafficpolicy/example/1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 210
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicy</Code>
  </Error>
  <RequestId>0980f3ea-6d0b-f8ef-4bf9-d7c287283f1b</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicy`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTrafficPolicyInstance

Deletes a traffic policy instance and all of the resource record sets that Amazon Route 53 created when you created the instance.

```http
DELETE /2013-04-01/trafficpolicyinstance/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 218
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicyInstance</Code>
  </Error>
  <RequestId>c546d541-d574-e85d-782a-662df9826ca3</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicyInstance`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVPCAssociationAuthorization

Removes authorization to submit an AssociateVPCWithHostedZone request to associate a specified VPC with a hosted zone that was created by a different account.

```http
POST /2013-04-01/hostedzone/example/deauthorizevpcassociation HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 167
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<DeleteVPCAssociationAuthorizationRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <VPC/>
</DeleteVPCAssociationAuthorizationRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 279
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidVPCId</Code>
    <Message>Invalid VPC ID: &quot;&quot; is not a valid Amazon VPC ID</Message>
  </Error>
  <RequestId>ab713de9-5aa2-76d4-cd10-5450e6b1f29f</RequestId>
</ErrorResponse>
```

`400` `InvalidVPCId`.

### DisableHostedZoneDNSSEC

Disables DNSSEC signing in a specific hosted zone.

```http
POST /2013-04-01/hostedzone/example/disable-dnssec HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>ac009c9e-cbed-c45b-1188-7cbc496e362f</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisassociateVPCFromHostedZone

Disassociates an Amazon Virtual Private Cloud (Amazon VPC) from an Amazon Route 53 private hosted zone.

```http
POST /2013-04-01/hostedzone/example/disassociatevpc HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 159
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<DisassociateVPCFromHostedZoneRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <VPC/>
</DisassociateVPCFromHostedZoneRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 279
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidVPCId</Code>
    <Message>Invalid VPC ID: &quot;&quot; is not a valid Amazon VPC ID</Message>
  </Error>
  <RequestId>79dff726-0d30-b251-a3be-d18586d39d8a</RequestId>
</ErrorResponse>
```

`400` `InvalidVPCId`.

### EnableHostedZoneDNSSEC

Enables DNSSEC signing in a specific hosted zone.

```http
POST /2013-04-01/hostedzone/example/enable-dnssec HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>8691ee07-63aa-ecbd-08ff-ef37cb963274</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetAccountLimit

Gets the specified limit for the current account, for example, the maximum number of health checks that you can create using the account.

```http
GET /2013-04-01/accountlimit/MAX_HEALTH_CHECKS_BY_OWNER HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 193
date: Thu, 26 Nov 2026 18:30:50 GMT

<GetAccountLimitResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Limit>
    <Type>MAX_HEALTH_CHECKS_BY_OWNER</Type>
    <Value>200</Value>
  </Limit>
  <Count>0</Count>
</GetAccountLimitResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetChange

Returns the current status of a change batch request.

```http
GET /2013-04-01/change/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 262
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchChange</Code>
    <Message>Could not find resource with ID: example</Message>
  </Error>
  <RequestId>f58b0606-ecb6-892d-6754-adafa1881e20</RequestId>
</ErrorResponse>
```

`404` `NoSuchChange`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#3-getchange) for a successful call.

### GetCheckerIpRanges

Route 53 does not perform authorization for this API because it retrieves information that is already available to the public.

```http
GET /2013-04-01/checkeripranges HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 1169
date: Thu, 26 Nov 2026 18:30:50 GMT

<GetCheckerIpRangesResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <CheckerIpRanges>
    <member>15.177.2.0/23</member>
    <member>15.177.6.0/23</member>
    <member>15.177.10.0/23</member>
    <member>15.177.14.0/23</member>
    <member>15.177.18.0/23</member>
    <member>15.177.22.0/23</member>
    <member>15.177.26.0/23</member>
    <member>15.177.30.0/23</member>
    <member>15.177.34.0/23</member>
    <member>15.177.38.0/23</member>
    <member>15.177.42.0/23</member>
    <member>15.177.46.0/23</member>
    <member>15.177.50.0/23</member>
    <member>15.177.54.0/23</member>
    <member>15.177.58.0/23</member>
    <member>15.177.62.0/23</member>
    <member>54.183.255.160/32</member>
    <member>54.228.16.32/32</member>
… (16 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetDNSSEC

Returns information about DNSSEC for a specific hosted zone, including the key-signing keys (KSKs) in the hosted zone.

```http
GET /2013-04-01/hostedzone/example/dnssec HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>19fbad23-2918-6bd4-8d55-a4d1788b0060</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetGeoLocation

Gets information about whether a specified geographic location is supported for Amazon Route 53 geolocation resource record sets.

```http
GET /2013-04-01/geolocation HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 364
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>a geolocation is asked about by continentcode, by countrycode, or by countrycode and subdivisioncode together; this request names none of them</Message>
  </Error>
  <RequestId>cad75048-8415-3d92-fd26-f6bcbaca9770</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetHealthCheck

Gets information about a specified health check.

```http
GET /2013-04-01/healthcheck/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 273
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>b4940225-bade-d8c6-b822-2a6d73b5885c</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetHealthCheckCount

Retrieves the number of health checks that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/healthcheckcount HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 151
date: Thu, 26 Nov 2026 18:30:50 GMT

<GetHealthCheckCountResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HealthCheckCount>0</HealthCheckCount>
</GetHealthCheckCountResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetHealthCheckLastFailureReason

Gets the reason that a specified health check failed most recently.

```http
GET /2013-04-01/healthcheck/example/lastfailurereason HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 273
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>e0e90d5f-5edf-fd31-acdf-386cf523ea76</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetHealthCheckStatus

Gets status of a specified health check.

```http
GET /2013-04-01/healthcheck/example/status HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 273
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>4070095b-d8a4-40fa-0930-95fb2ba00ae9</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetHostedZone

Gets information about a specified hosted zone including the four name servers assigned to the hosted zone. returns the VPCs associated with the specified hosted zone and does not reflect the VPC associations by Route 53 Profiles.

```http
GET /2013-04-01/hostedzone/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>b805a327-cba7-87f9-f774-33444e777595</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetHostedZoneCount

Retrieves the number of hosted zones that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/hostedzonecount HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 147
date: Thu, 26 Nov 2026 18:30:50 GMT

<GetHostedZoneCountResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZoneCount>0</HostedZoneCount>
</GetHostedZoneCountResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetHostedZoneLimit

Gets the specified limit for a specified hosted zone, for example, the maximum number of records that you can create in the hosted zone.

```http
GET /2013-04-01/hostedzonelimit/example/MAX_RRSETS_BY_ZONE HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>555eb273-030b-31a7-d337-15118c0d48b0</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetQueryLoggingConfig

Gets information about a specified configuration for DNS query logging.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /2013-04-01/queryloggingconfig/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 281
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchQueryLoggingConfig</Code>
    <Message>The query logging configuration does not exist.</Message>
  </Error>
  <RequestId>063b07e9-2742-2b17-8724-a8b6d99ed973</RequestId>
</ErrorResponse>
```

`404` `NoSuchQueryLoggingConfig`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetReusableDelegationSet

Retrieves information about a specified reusable delegation set, including the four name servers that are assigned to the delegation set.

```http
GET /2013-04-01/delegationset/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchDelegationSet</Code>
    <Message>example</Message>
  </Error>
  <RequestId>4faea44a-bc2c-10ae-dbb3-dd650625b0dd</RequestId>
</ErrorResponse>
```

`400` `NoSuchDelegationSet`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetReusableDelegationSetLimit

Gets the maximum number of hosted zones that you can associate with the specified reusable delegation set.

```http
GET /2013-04-01/reusabledelegationsetlimit/example/MAX_ZONES_BY_REUSABLE_DELEGATION_SET HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 236
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchDelegationSet</Code>
    <Message>example</Message>
  </Error>
  <RequestId>021618de-c848-e0d6-3144-54079ea9ea17</RequestId>
</ErrorResponse>
```

`400` `NoSuchDelegationSet`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTrafficPolicy

Gets information about a specific traffic policy version.

```http
GET /2013-04-01/trafficpolicy/example/1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 210
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicy</Code>
  </Error>
  <RequestId>269fb0dd-cfa5-6a5c-f09b-97a3ab38e24d</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicy`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTrafficPolicyInstance

Gets information about a specified traffic policy instance.

```http
GET /2013-04-01/trafficpolicyinstance/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 218
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicyInstance</Code>
  </Error>
  <RequestId>4fac0846-4922-b6a2-b1cb-c63beab9b0e6</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicyInstance`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTrafficPolicyInstanceCount

Gets the number of traffic policy instances that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/trafficpolicyinstancecount HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 191
date: Thu, 26 Nov 2026 18:30:50 GMT

<GetTrafficPolicyInstanceCountResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <TrafficPolicyInstanceCount>0</TrafficPolicyInstanceCount>
</GetTrafficPolicyInstanceCountResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListCidrBlocks

Returns a paginated list of location objects and their CIDR blocks.

```http
GET /2013-04-01/cidrcollection/example/cidrblocks HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 449
date: Thu, 26 Nov 2026 18:30:50 GMT

<?xml version="1.0"?>
<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>1 validation error detected: Value &apos;example&apos; at &apos;collectionId&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$</Message>
  </Error>
  <RequestId>0863409f-f074-84ef-e592-1ce621e53ad0</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListCidrCollections

Returns a paginated list of CIDR collections in the Amazon Web Services account (metadata only).

```http
GET /2013-04-01/cidrcollection HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 330
date: Thu, 26 Nov 2026 18:30:50 GMT

<ListCidrCollectionsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <CidrCollections>
    <member>
      <Arn>arn:aws:route53:::cidrcollection/04901489-b6d7-c741-4622-f150d9bd988f</Arn>
      <Id>04901489-b6d7-c741-4622-f150d9bd988f</Id>
      <Name>example</Name>
      <Version>1</Version>
    </member>
  </CidrCollections>
</ListCidrCollectionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListCidrLocations

Returns a paginated list of CIDR locations for the given collection (metadata only, does not include CIDR blocks).

```http
GET /2013-04-01/cidrcollection/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 449
date: Thu, 26 Nov 2026 18:30:50 GMT

<?xml version="1.0"?>
<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>1 validation error detected: Value &apos;example&apos; at &apos;collectionId&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$</Message>
  </Error>
  <RequestId>ee0793ae-92f1-5822-b931-90588da198dd</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListGeoLocations

Retrieves a list of supported geographic locations.

```http
GET /2013-04-01/geolocations HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 42276
date: Thu, 26 Nov 2026 18:30:50 GMT

<ListGeoLocationsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <GeoLocationDetailsList>
    <GeoLocationDetails>
      <CountryCode>*</CountryCode>
      <CountryName>Default</CountryName>
    </GeoLocationDetails>
    <GeoLocationDetails>
      <CountryCode>AD</CountryCode>
      <CountryName>Andorra</CountryName>
    </GeoLocationDetails>
    <GeoLocationDetails>
      <CountryCode>AE</CountryCode>
      <CountryName>United Arab Emirates</CountryName>
    </GeoLocationDetails>
    <GeoLocationDetails>
      <CountryCode>AF</CountryCode>
      <CountryName>Afghanistan</CountryName>
    </GeoLocationDetails>
    <GeoLocationDetails>
      <CountryCode>AG</CountryCode>
… (1470 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListHealthChecks

Retrieve a list of the health checks that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/healthcheck HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 209
date: Thu, 26 Nov 2026 18:30:50 GMT

<ListHealthChecksResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HealthChecks></HealthChecks>
  <Marker></Marker>
  <IsTruncated>false</IsTruncated>
  <MaxItems>100</MaxItems>
</ListHealthChecksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListHostedZones

Retrieves a list of the public and private hosted zones that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/hostedzone HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 205
date: Thu, 26 Nov 2026 18:30:50 GMT

<ListHostedZonesResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZones></HostedZones>
  <Marker></Marker>
  <IsTruncated>false</IsTruncated>
  <MaxItems>100</MaxItems>
</ListHostedZonesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListHostedZonesByName

Retrieves a list of your hosted zones in lexicographic order.

```http
GET /2013-04-01/hostedzonesbyname HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 200
date: Thu, 26 Nov 2026 18:30:50 GMT

<ListHostedZonesByNameResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZones></HostedZones>
  <IsTruncated>false</IsTruncated>
  <MaxItems>100</MaxItems>
</ListHostedZonesByNameResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListHostedZonesByVPC

Lists all the private hosted zones that a specified VPC is associated with, regardless of which Amazon Web Services account or Amazon Web Services service owns the hosted zones.

```http
GET /2013-04-01/hostedzonesbyvpc?vpcid=example&vpcregion=us-east-1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 403 Forbidden
content-type: application/xml
content-length: 297
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>The VPC: example in region us-east-1 that you provided is not owned by you.</Message>
  </Error>
  <RequestId>282e2e62-71b8-af6d-e471-e0513b7a7778</RequestId>
</ErrorResponse>
```

`403` `AccessDenied`: the request was refused by authorization.

### ListQueryLoggingConfigs

Lists the configurations for DNS query logging that are associated with the current Amazon Web Services account or the configuration that is associated with a specified hosted zone.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /2013-04-01/queryloggingconfig HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 164
date: Thu, 26 Nov 2026 18:30:50 GMT

<ListQueryLoggingConfigsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <QueryLoggingConfigs></QueryLoggingConfigs>
</ListQueryLoggingConfigsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListResourceRecordSets

Lists the resource record sets in a specified hosted zone.

```http
GET /2013-04-01/hostedzone/example/rrset HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>20b71809-b607-9c1e-3205-d6d0c4f0aae2</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#4-listresourcerecordsets) for a successful call.

### ListReusableDelegationSets

Retrieves a list of the reusable delegation sets that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/delegationset HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 557
date: Thu, 26 Nov 2026 18:30:50 GMT

<ListReusableDelegationSetsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <DelegationSets>
    <DelegationSet>
      <Id>/delegationset/N0XBS6XRVL4A8H6</Id>
      <CallerReference>example</CallerReference>
      <NameServers>
        <NameServer>ns-127.awsdns-62.com</NameServer>
        <NameServer>ns-1494.awsdns-62.net</NameServer>
        <NameServer>ns-1706.awsdns-36.org</NameServer>
        <NameServer>ns-1455.awsdns-37.co.uk</NameServer>
      </NameServers>
    </DelegationSet>
  </DelegationSets>
  <Marker></Marker>
  <IsTruncated>false</IsTruncated>
  <MaxItems>100</MaxItems>
</ListReusableDelegationSetsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsForResource

Lists tags for one health check or hosted zone.

```http
GET /2013-04-01/tags/healthcheck/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 273
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>5f104c89-9783-dd1f-3483-36282533bef5</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTagsForResources

Lists tags for up to 10 health checks or hosted zones.

```http
POST /2013-04-01/tags/healthcheck HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 194
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<ListTagsForResourcesRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ResourceIds>
    <ResourceId>example</ResourceId>
  </ResourceIds>
</ListTagsForResourcesRequest>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 273
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>7f17c05f-df55-0892-9218-cdbb2442d653</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTrafficPolicies

Gets information about the latest version for every traffic policy that is associated with the current Amazon Web Services account.

```http
GET /2013-04-01/trafficpolicies HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 265
date: Thu, 26 Nov 2026 18:30:50 GMT

<ListTrafficPoliciesResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <TrafficPolicySummaries></TrafficPolicySummaries>
  <IsTruncated>false</IsTruncated>
  <TrafficPolicyIdMarker></TrafficPolicyIdMarker>
  <MaxItems>100</MaxItems>
</ListTrafficPoliciesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTrafficPolicyInstances

Gets information about the traffic policy instances that you created by using the current Amazon Web Services account.

```http
GET /2013-04-01/trafficpolicyinstances HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 232
date: Thu, 26 Nov 2026 18:30:50 GMT

<ListTrafficPolicyInstancesResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <TrafficPolicyInstances></TrafficPolicyInstances>
  <IsTruncated>false</IsTruncated>
  <MaxItems>100</MaxItems>
</ListTrafficPolicyInstancesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTrafficPolicyInstancesByHostedZone

Gets information about the traffic policy instances that you created in a specified hosted zone.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /2013-04-01/trafficpolicyinstances/hostedzone?id=example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>8167035e-e439-fccd-0cb5-982dfaaed557</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTrafficPolicyInstancesByPolicy

Gets information about the traffic policy instances that you created by using a specify traffic policy version.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
GET /2013-04-01/trafficpolicyinstances/trafficpolicy?id=example&version=1 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 210
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicy</Code>
  </Error>
  <RequestId>794fcf2b-6099-3974-ba05-eb3b16c9c617</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicy`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTrafficPolicyVersions

Gets information about all of the versions for a specified traffic policy.

```http
GET /2013-04-01/trafficpolicies/example/versions HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 210
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicy</Code>
  </Error>
  <RequestId>5a8f10c5-99a7-c020-3bfb-e7f5fb29e31c</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicy`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListVPCAssociationAuthorizations

Gets a list of the VPCs that were created by other accounts and that can be associated with a specified hosted zone because you've submitted one or more CreateVPCAssociationAuthorization requests.

```http
GET /2013-04-01/hostedzone/example/authorizevpcassociation HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>04fe7b74-2b85-2bb9-8925-28cede4803cc</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TestDNSAnswer

Gets the value that Amazon Route 53 returns in response to a DNS request for a specified record name and type.

```http
GET /2013-04-01/testdnsanswer?hostedzoneid=example&recordname=example&recordtype=SOA HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>2ec00137-7aff-9c86-1247-7a961d5688db</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateHealthCheck

Updates an existing health check.

```http
POST /2013-04-01/healthcheck/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 273
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>fdb20d51-e8a7-1821-a738-60211f72919e</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateHostedZoneComment

Updates the comment for a specified hosted zone.

```http
POST /2013-04-01/hostedzone/example HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>f467d3bf-771e-fea2-9b07-10d826e5098c</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateHostedZoneFeatures

Updates the features configuration for a hosted zone.

```http
POST /2013-04-01/hostedzone/example/features HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 263
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>7b55f383-a3a4-e2f7-f15d-a643a848b174</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateTrafficPolicyComment

Updates the comment for a specified traffic policy version.

```http
POST /2013-04-01/trafficpolicy/example/1 HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 173
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<UpdateTrafficPolicyCommentRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Comment>example</Comment>
</UpdateTrafficPolicyCommentRequest>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml
content-length: 210
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicy</Code>
  </Error>
  <RequestId>57072671-8577-3c22-24e1-3965515eb3ac</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicy`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateTrafficPolicyInstance

After you submit a UpdateTrafficPolicyInstance request, there's a brief delay while Route 53 creates the resource record sets that are specified in the traffic policy definition.

```http
POST /2013-04-01/trafficpolicyinstance/example HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183050Z
Content-Length: 249
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

<?xml version="1.0" ?>
<UpdateTrafficPolicyInstanceRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <TTL>0</TTL>
  <TrafficPolicyId>example</TrafficPolicyId>
  <TrafficPolicyVersion>1</TrafficPolicyVersion>
</UpdateTrafficPolicyInstanceRequest>
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/xml
content-length: 314
date: Thu, 26 Nov 2026 18:30:50 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation UpdateTrafficPolicyInstance is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>90725445-8f02-fdea-7f17-ec675cd7a15c</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/route53/aws4_request` in the `Authorization` header) and the operation from the request itself: an HTTP method and path per operation, with identifiers in the path, options in the query string or headers, and a structured input as an XML body.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Route 53 answers in AWS: XML bodies and metadata in headers. Errors captured on this page are an XML document rooted at `<ErrorResponse>`.
