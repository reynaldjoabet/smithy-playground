# Amazon Route 53

## Wire protocol

- **Protocol:** `restXml`
- **SDK id:** `Route 53`
- **sigv4 signing name:** `route53`
- **Endpoint prefix:** `route53`
- **ARN namespace:** `route53`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `route53`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed.

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
X-Amz-Date: 20261116T121550Z
Content-Length: 233
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
location: https://127.0.0.1:9297/2013-04-01/hostedzone/ZWAGVLJO3K5YOD2
content-length: 752
date: Mon, 16 Nov 2026 12:15:50 GMT

<CreateHostedZoneResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZone>
    <Id>/hostedzone/ZWAGVLJO3K5YOD2</Id>
    <Name>docs-demo.example.com.</Name>
    <CallerReference>docs-demo-2026-11-16</CallerReference>
    <Config>
      <PrivateZone>false</PrivateZone>
    </Config>
    <ResourceRecordSetCount>2</ResourceRecordSetCount>
    <Features/>
  </HostedZone>
  <ChangeInfo>
    <Id>/change/CCO888C9VELYTN1</Id>
    <Status>PENDING</Status>
    <SubmittedAt>2026-11-16T12:15:50.487Z</SubmittedAt>
  </ChangeInfo>
  <DelegationSet>
    <NameServers>
      <NameServer>ns-113.awsdns-48.com</NameServer>
      <NameServer>ns-823.awsdns-48.net</NameServer>
      <NameServer>ns-1198.awsdns-20.org</NameServer>
      <NameServer>ns-1529.awsdns-35.co.uk</NameServer>
    </NameServers>
  </DelegationSet>
</CreateHostedZoneResponse>
```

### 2. ChangeResourceRecordSets

Record changes are sent as a batch in an XML body; the response is a change id in `PENDING`.

**Request**

```http
POST /2013-04-01/hostedzone/ZWAGVLJO3K5YOD2/rrset HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121550Z
Content-Length: 444
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:15:50 GMT

<ChangeResourceRecordSetsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeInfo>
    <Id>/change/CI0LRN02C4BQAXG</Id>
    <Status>PENDING</Status>
    <SubmittedAt>2026-11-16T12:15:50.504Z</SubmittedAt>
  </ChangeInfo>
</ChangeResourceRecordSetsResponse>
```

### 3. GetChange

Poll the change until it reports `INSYNC`, meaning it has reached every Route 53 name server.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
GET /2013-04-01/change/CI0LRN02C4BQAXG HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121552Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 224
date: Mon, 16 Nov 2026 12:15:52 GMT

<GetChangeResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeInfo>
    <Id>/change/CI0LRN02C4BQAXG</Id>
    <Status>INSYNC</Status>
    <SubmittedAt>2026-11-16T12:15:50.504Z</SubmittedAt>
  </ChangeInfo>
</GetChangeResponse>
```

### 4. ListResourceRecordSets

The zone holds the SOA and NS records Route 53 created plus the new A record.

**Request**

```http
GET /2013-04-01/hostedzone/ZWAGVLJO3K5YOD2/rrset HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121552Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 1099
date: Mon, 16 Nov 2026 12:15:52 GMT

<ListResourceRecordSetsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ResourceRecordSets>
    <ResourceRecordSet>
      <Name>docs-demo.example.com.</Name>
      <Type>NS</Type>
      <TTL>172800</TTL>
      <ResourceRecords>
        <ResourceRecord>
          <Value>ns-113.awsdns-48.com.</Value>
        </ResourceRecord>
        <ResourceRecord>
          <Value>ns-823.awsdns-48.net.</Value>
        </ResourceRecord>
        <ResourceRecord>
          <Value>ns-1198.awsdns-20.org.</Value>
        </ResourceRecord>
        <ResourceRecord>
          <Value>ns-1529.awsdns-35.co.uk.</Value>
        </ResourceRecord>
      </ResourceRecords>
    </ResourceRecordSet>
    <ResourceRecordSet>
      <Name>docs-demo.example.com.</Name>
      <Type>SOA</Type>
      <TTL>900</TTL>
      <ResourceRecords>
        <ResourceRecord>
          <Value>ns-113.awsdns-48.com. awsdns-hostmaster.amazon.com. 1 7200 900 1209600 86400</Value>
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
  <MaxItems>300</MaxItems>
</ListResourceRecordSetsResponse>
```

### 5. DeleteHostedZone

A zone that still holds records of its own is refused with `HostedZoneNotEmpty`.

**Request**

```http
DELETE /2013-04-01/hostedzone/ZWAGVLJO3K5YOD2 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121552Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: application/xml
content-length: 397
date: Mon, 16 Nov 2026 12:15:52 GMT

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>HostedZoneNotEmpty</Code>
    <Message>The specified hosted zone contains resource record sets other than the default SOA and NS records and so cannot be deleted. Delete all non-default records and try again.</Message>
  </Error>
  <RequestId>a9c4213a-3214-0d4b-3ae6-bc356a7f35e6</RequestId>
</ErrorResponse>
```

### 6. ChangeResourceRecordSets

Delete the A record (the exact record must be repeated) ...

**Request**

```http
POST /2013-04-01/hostedzone/ZWAGVLJO3K5YOD2/rrset HTTP/1.1
Content-Type: application/xml
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121552Z
Content-Length: 444
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/route53/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:15:52 GMT

<ChangeResourceRecordSetsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeInfo>
    <Id>/change/C9B0U930FDCVX9U</Id>
    <Status>PENDING</Status>
    <SubmittedAt>2026-11-16T12:15:52.566Z</SubmittedAt>
  </ChangeInfo>
</ChangeResourceRecordSetsResponse>
```

### 7. DeleteHostedZone

... and the zone deletes.

**Request**

```http
DELETE /2013-04-01/hostedzone/ZWAGVLJO3K5YOD2 HTTP/1.1
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121552Z
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/route53/aws4_request, SignedHeaders=host;x-amz-date, Signature=<signature>
```

**Response**

```http
HTTP/1.1 200 OK
content-type: application/xml
content-length: 239
date: Mon, 16 Nov 2026 12:15:52 GMT

<DeleteHostedZoneResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <ChangeInfo>
    <Id>/change/COHVM955J74J0ZT</Id>
    <Status>PENDING</Status>
    <SubmittedAt>2026-11-16T12:15:52.574Z</SubmittedAt>
  </ChangeInfo>
</DeleteHostedZoneResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`) are left out here; the walkthrough shows them in full.

Of 71 operations, 16 accepted the placeholder call, 2 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### ActivateKeySigningKey

Activates a key-signing key (KSK) so that it can be used for signing by DNSSEC.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST /2013-04-01/keysigningkey/example/example/activate HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchKeySigningKey</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>9e0a90da-bb83-21cb-3593-08b508cbf82b</RequestId>
</ErrorResponse>
```

`404` `NoSuchKeySigningKey`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AssociateVPCWithHostedZone

Associates an Amazon VPC with a private hosted zone.

```http
POST /2013-04-01/hostedzone/example/associatevpc HTTP/1.1
Content-Type: application/xml

<?xml version="1.0" ?>
<AssociateVPCWithHostedZoneRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <VPC/>
</AssociateVPCWithHostedZoneRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidVPCId</Code>
    <Message>Invalid VPC ID: &quot;&quot; is not a valid Amazon VPC ID</Message>
  </Error>
  <RequestId>ca609b84-d329-8dd6-c85a-1240669b3eb1</RequestId>
</ErrorResponse>
```

`400` `InvalidVPCId`.

### ChangeCidrCollection

Creates, changes, or deletes CIDR blocks within a collection.

```http
POST /2013-04-01/cidrcollection/example HTTP/1.1
Content-Type: application/xml

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

<?xml version="1.0" ?>
<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>1 validation error detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$</Message>
  </Error>
  <RequestId>a142836a-8f8a-c908-5f21-d91679dfb6b3</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ChangeResourceRecordSets

Creates, changes, or deletes a resource record set, which contains authoritative DNS information for a specified domain name or subdomain name.

```http
POST /2013-04-01/hostedzone/example/rrset HTTP/1.1
Content-Type: application/xml

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

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid request: Expected exactly one of [AliasTarget, all of [TTL, and ResourceRecords], or TrafficPolicyInstanceId], but found none in Change with [Action=CREATE, Name=example., Type=SOA, SetIdentifier=null]</Message>
  </Error>
  <RequestId>e042cd3f-7078-db7f-a05a-9f52b485ef68</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-changeresourcerecordsets) for a successful call.

### ChangeTagsForResource

Adds, edits, or deletes tags for a health check or a hosted zone.

```http
POST /2013-04-01/tags/healthcheck/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>7a7a0aea-baf7-a6a8-8044-b4a3c6f62b64</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateCidrCollection

Creates a CIDR collection in the current Amazon Web Services account.

```http
POST /2013-04-01/cidrcollection HTTP/1.1
Content-Type: application/xml

<?xml version="1.0" ?>
<CreateCidrCollectionRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Name>example</Name>
  <CallerReference>example</CallerReference>
</CreateCidrCollectionRequest>
```

```http
HTTP/1.1 201 Created
content-type: application/xml
location: https://127.0.0.1:9297/2013-04-01/cidrcollection/ce58732d-73e3-f376-8c6a-f9c7fdb62910

<CreateCidrCollectionResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Collection>
    <Arn>arn:aws:route53::000000000000:cidrcollection/ce58732d-73e3-f376-8c6a-f9c7fdb62910</Arn>
    <Id>ce58732d-73e3-f376-8c6a-f9c7fdb62910</Id>
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

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>Invalid request: a HTTP health check must specify HealthCheckConfig.IPAddress or HealthCheckConfig.FullyQualifiedDomainName</Message>
  </Error>
  <RequestId>c55835aa-50b8-8208-52eb-1329ea76cb6f</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### CreateHostedZone

Creates a new public or private hosted zone.

```http
POST /2013-04-01/hostedzone HTTP/1.1
Content-Type: application/xml

<?xml version="1.0" ?>
<CreateHostedZoneRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Name>example</Name>
  <CallerReference>example</CallerReference>
</CreateHostedZoneRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidDomainName</Code>
    <Message>example is reserved by AWS!</Message>
  </Error>
  <RequestId>e0fc54f3-829f-34e1-d69b-5dcd1af2bd2c</RequestId>
</ErrorResponse>
```

`400` `InvalidDomainName`. See [the walkthrough](#1-createhostedzone) for a successful call.

### CreateKeySigningKey

Creates a new key-signing key (KSK) associated with a hosted zone.

```http
POST /2013-04-01/keysigningkey HTTP/1.1
Content-Type: application/xml

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

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidKeySigningKeyStatus</Code>
    <Message>The key-signing key (KSK) status isn't valid or another KSK has the status INTERNAL_FAILURE.</Message>
  </Error>
  <RequestId>c587f7bc-fdf3-9936-3cdc-92480a482c5b</RequestId>
</ErrorResponse>
```

`400` `InvalidKeySigningKeyStatus`.

### CreateQueryLoggingConfig

Creates a configuration for DNS query logging.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST /2013-04-01/queryloggingconfig HTTP/1.1
Content-Type: application/xml

<?xml version="1.0" ?>
<CreateQueryLoggingConfigRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZoneId>example</HostedZoneId>
  <CloudWatchLogsLogGroupArn>arn:aws:route53:us-east-1:000000000000:example/example</CloudWatchLogsLogGroupArn>
</CreateQueryLoggingConfigRequest>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>c452a213-61fd-544d-a6cc-0337772810c4</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateReusableDelegationSet

Creates a delegation set (a group of four name servers) that can be reused by multiple hosted zones that were created by the same Amazon Web Services account.

```http
POST /2013-04-01/delegationset HTTP/1.1
Content-Type: application/xml

<?xml version="1.0" ?>
<CreateReusableDelegationSetRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <CallerReference>example</CallerReference>
</CreateReusableDelegationSetRequest>
```

```http
HTTP/1.1 201 Created
content-type: application/xml
location: https://127.0.0.1:9297/2013-04-01/delegationset/NG3MYLLVLRECXGN

<CreateReusableDelegationSetResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <DelegationSet>
    <Id>/delegationset/NG3MYLLVLRECXGN</Id>
    <CallerReference>example</CallerReference>
    <NameServers>
      <NameServer>ns-323.awsdns-00.com</NameServer>
      <NameServer>ns-104.awsdns-07.net</NameServer>
      <NameServer>ns-1850.awsdns-07.org</NameServer>
      <NameServer>ns-697.awsdns-13.co.uk</NameServer>
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

<?xml version="1.0" ?>
<CreateTrafficPolicyRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Name>example</Name>
  <Document>example</Document>
</CreateTrafficPolicyRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidTrafficPolicyDocument</Code>
    <Message>The traffic policy document must be declared as valid JSON.</Message>
  </Error>
  <RequestId>b5544503-76cb-e326-68e2-51f594703769</RequestId>
</ErrorResponse>
```

`400` `InvalidTrafficPolicyDocument`.

### CreateTrafficPolicyInstance

Creates resource record sets in a specified hosted zone based on the settings in a specified traffic policy version.

```http
POST /2013-04-01/trafficpolicyinstance HTTP/1.1
Content-Type: application/xml

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

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>6dbd5c94-22e4-6728-2b47-e26ed4e269a6</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateTrafficPolicyVersion

Creates a new version of an existing traffic policy.

```http
POST /2013-04-01/trafficpolicy/example HTTP/1.1
Content-Type: application/xml

<?xml version="1.0" ?>
<CreateTrafficPolicyVersionRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Document>example</Document>
</CreateTrafficPolicyVersionRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidTrafficPolicyDocument</Code>
    <Message>The traffic policy document must be declared as valid JSON.</Message>
  </Error>
  <RequestId>92cd5c20-6c97-047b-30a5-d45e45c5a696</RequestId>
</ErrorResponse>
```

`400` `InvalidTrafficPolicyDocument`.

### CreateVPCAssociationAuthorization

Authorizes the Amazon Web Services account that created a specified VPC to submit an AssociateVPCWithHostedZone request to associate the VPC with a specified hosted zone that was created by a different account.

```http
POST /2013-04-01/hostedzone/example/authorizevpcassociation HTTP/1.1
Content-Type: application/xml

<?xml version="1.0" ?>
<CreateVPCAssociationAuthorizationRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <VPC/>
</CreateVPCAssociationAuthorizationRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidVPCId</Code>
    <Message>Invalid VPC ID: &quot;&quot; is not a valid Amazon VPC ID</Message>
  </Error>
  <RequestId>4d6648d8-6fd1-a2d1-2928-f2ae4eac942b</RequestId>
</ErrorResponse>
```

`400` `InvalidVPCId`.

### DeactivateKeySigningKey

Deactivates a key-signing key (KSK) so that it will not be used for signing by DNSSEC.

```http
POST /2013-04-01/keysigningkey/example/example/deactivate HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchKeySigningKey</Code>
    <Message>The Key Signing Key with name 'example', could not be found.</Message>
  </Error>
  <RequestId>2cc2e511-87ff-f6a8-e2da-d478dba1a6d4</RequestId>
</ErrorResponse>
```

`404` `NoSuchKeySigningKey`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteCidrCollection

Deletes a CIDR collection in the current Amazon Web Services account.

```http
DELETE /2013-04-01/cidrcollection/example HTTP/1.1
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<?xml version="1.0" ?>
<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>1 validation error detected: Value 'example' at 'id' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$</Message>
  </Error>
  <RequestId>2efb5857-8665-087f-ca7f-842905796708</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### DeleteHealthCheck

Deletes a health check.

```http
DELETE /2013-04-01/healthcheck/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>63eeecc9-e8d7-68d1-fc1a-e1021c6e7fd9</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteHostedZone

Deletes a hosted zone.

```http
DELETE /2013-04-01/hostedzone/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>e62b8d5d-ff1c-d5c9-ac5e-cc00383239d1</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deletehostedzone) for a successful call.

### DeleteKeySigningKey

Deletes a key-signing key (KSK).

```http
DELETE /2013-04-01/keysigningkey/example/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchKeySigningKey</Code>
    <Message>The Key Signing Key with name 'example', could not be found.</Message>
  </Error>
  <RequestId>43797f97-bea9-0818-4f09-7407d6001501</RequestId>
</ErrorResponse>
```

`404` `NoSuchKeySigningKey`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteQueryLoggingConfig

Deletes a configuration for DNS query logging.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
DELETE /2013-04-01/queryloggingconfig/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchQueryLoggingConfig</Code>
    <Message>The query logging configuration does not exist.</Message>
  </Error>
  <RequestId>a93fa375-aaeb-3e26-4731-893a8b7e644c</RequestId>
</ErrorResponse>
```

`404` `NoSuchQueryLoggingConfig`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteReusableDelegationSet

Deletes a reusable delegation set.

```http
DELETE /2013-04-01/delegationset/example HTTP/1.1
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchDelegationSet</Code>
    <Message>example</Message>
  </Error>
  <RequestId>5e981a83-2607-27f7-c76c-2fd971268ab4</RequestId>
</ErrorResponse>
```

`400` `NoSuchDelegationSet`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTrafficPolicy

Deletes a traffic policy.

```http
DELETE /2013-04-01/trafficpolicy/example/1 HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicy</Code>
  </Error>
  <RequestId>9e004a6f-d22d-eca0-1d20-57c4cee7e5e1</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicy`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTrafficPolicyInstance

Deletes a traffic policy instance and all of the resource record sets that Amazon Route 53 created when you created the instance.

```http
DELETE /2013-04-01/trafficpolicyinstance/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicyInstance</Code>
  </Error>
  <RequestId>7c0df26c-9367-2066-7fb5-efa653f3f45d</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicyInstance`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVPCAssociationAuthorization

Removes authorization to submit an AssociateVPCWithHostedZone request to associate a specified VPC with a hosted zone that was created by a different account.

```http
POST /2013-04-01/hostedzone/example/deauthorizevpcassociation HTTP/1.1
Content-Type: application/xml

<?xml version="1.0" ?>
<DeleteVPCAssociationAuthorizationRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <VPC/>
</DeleteVPCAssociationAuthorizationRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidVPCId</Code>
    <Message>Invalid VPC ID: &quot;&quot; is not a valid Amazon VPC ID</Message>
  </Error>
  <RequestId>bfb0f70e-c352-2ad6-ae21-5fe1b496ea87</RequestId>
</ErrorResponse>
```

`400` `InvalidVPCId`.

### DisableHostedZoneDNSSEC

Disables DNSSEC signing in a specific hosted zone.

```http
POST /2013-04-01/hostedzone/example/disable-dnssec HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>9fa0cc74-1cc9-9220-0b63-03121947afe6</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisassociateVPCFromHostedZone

Disassociates an Amazon Virtual Private Cloud (Amazon VPC) from an Amazon Route 53 private hosted zone.

```http
POST /2013-04-01/hostedzone/example/disassociatevpc HTTP/1.1
Content-Type: application/xml

<?xml version="1.0" ?>
<DisassociateVPCFromHostedZoneRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <VPC/>
</DisassociateVPCFromHostedZoneRequest>
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidVPCId</Code>
    <Message>Invalid VPC ID: &quot;&quot; is not a valid Amazon VPC ID</Message>
  </Error>
  <RequestId>8f0cb7d9-c150-ce8f-7547-a0d02af34a66</RequestId>
</ErrorResponse>
```

`400` `InvalidVPCId`.

### EnableHostedZoneDNSSEC

Enables DNSSEC signing in a specific hosted zone.

```http
POST /2013-04-01/hostedzone/example/enable-dnssec HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>1ec40e9c-a92a-912d-72fd-89c9a8acb643</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetAccountLimit

Gets the specified limit for the current account, for example, the maximum number of health checks that you can create using the account.

```http
GET /2013-04-01/accountlimit/MAX_HEALTH_CHECKS_BY_OWNER HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

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
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchChange</Code>
    <Message>Could not find resource with ID: example</Message>
  </Error>
  <RequestId>41b89424-e551-38d2-4308-1dcc4ce881dd</RequestId>
</ErrorResponse>
```

`404` `NoSuchChange`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#3-getchange) for a successful call.

### GetCheckerIpRanges

Route 53 does not perform authorization for this API because it retrieves information that is already available to the public.

```http
GET /2013-04-01/checkeripranges HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

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
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>9cc7dc7b-5e2b-294d-27f7-3d275a855259</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetGeoLocation

Gets information about whether a specified geographic location is supported for Amazon Route 53 geolocation resource record sets.

```http
GET /2013-04-01/geolocation HTTP/1.1
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>a geolocation is asked about by continentcode, by countrycode, or by countrycode and subdivisioncode together; this request names none of them</Message>
  </Error>
  <RequestId>bc613571-a43c-484d-9c72-4fa61ff083ef</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### GetHealthCheck

Gets information about a specified health check.

```http
GET /2013-04-01/healthcheck/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>4771ba02-6e30-a203-3782-aee195fe6558</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetHealthCheckCount

Retrieves the number of health checks that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/healthcheckcount HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<GetHealthCheckCountResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HealthCheckCount>0</HealthCheckCount>
</GetHealthCheckCountResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetHealthCheckLastFailureReason

Gets the reason that a specified health check failed most recently.

```http
GET /2013-04-01/healthcheck/example/lastfailurereason HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>58d1dfdd-ea1d-250c-53da-94095b127fd8</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetHealthCheckStatus

Gets status of a specified health check.

```http
GET /2013-04-01/healthcheck/example/status HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>3cd54269-46d1-342a-426a-8c3bfbd88bc8</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetHostedZone

Gets information about a specified hosted zone including the four name servers assigned to the hosted zone. returns the VPCs associated with the specified hosted zone and does not reflect the VPC associations by Route 53 Profiles.

```http
GET /2013-04-01/hostedzone/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>06306351-1054-bfcb-ec7d-92735a1ee87e</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetHostedZoneCount

Retrieves the number of hosted zones that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/hostedzonecount HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<GetHostedZoneCountResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZoneCount>0</HostedZoneCount>
</GetHostedZoneCountResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetHostedZoneLimit

Gets the specified limit for a specified hosted zone, for example, the maximum number of records that you can create in the hosted zone.

```http
GET /2013-04-01/hostedzonelimit/example/MAX_RRSETS_BY_ZONE HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>fa570473-a9a8-ee00-1c8d-b74cfc5cb101</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetQueryLoggingConfig

Gets information about a specified configuration for DNS query logging.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /2013-04-01/queryloggingconfig/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchQueryLoggingConfig</Code>
    <Message>The query logging configuration does not exist.</Message>
  </Error>
  <RequestId>1b6af74c-2da8-922e-d9cb-3f1b4b9384cb</RequestId>
</ErrorResponse>
```

`404` `NoSuchQueryLoggingConfig`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetReusableDelegationSet

Retrieves information about a specified reusable delegation set, including the four name servers that are assigned to the delegation set.

```http
GET /2013-04-01/delegationset/example HTTP/1.1
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchDelegationSet</Code>
    <Message>example</Message>
  </Error>
  <RequestId>35111a07-41f7-06e8-6dcc-91b87c93530f</RequestId>
</ErrorResponse>
```

`400` `NoSuchDelegationSet`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetReusableDelegationSetLimit

Gets the maximum number of hosted zones that you can associate with the specified reusable delegation set.

```http
GET /2013-04-01/reusabledelegationsetlimit/example/MAX_ZONES_BY_REUSABLE_DELEGATION_SET HTTP/1.1
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchDelegationSet</Code>
    <Message>example</Message>
  </Error>
  <RequestId>fdda18e3-7d5e-30c0-b62f-c8cc16000a45</RequestId>
</ErrorResponse>
```

`400` `NoSuchDelegationSet`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTrafficPolicy

Gets information about a specific traffic policy version.

```http
GET /2013-04-01/trafficpolicy/example/1 HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicy</Code>
    <Message>The policy does not exist.</Message>
  </Error>
  <RequestId>99d4d292-898d-2f53-ff03-416243292542</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicy`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTrafficPolicyInstance

Gets information about a specified traffic policy instance.

```http
GET /2013-04-01/trafficpolicyinstance/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicyInstance</Code>
  </Error>
  <RequestId>de62437a-e021-8df0-5fab-6aea375e887c</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicyInstance`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTrafficPolicyInstanceCount

Gets the number of traffic policy instances that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/trafficpolicyinstancecount HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<GetTrafficPolicyInstanceCountResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <TrafficPolicyInstanceCount>0</TrafficPolicyInstanceCount>
</GetTrafficPolicyInstanceCountResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListCidrBlocks

Returns a paginated list of location objects and their CIDR blocks.

```http
GET /2013-04-01/cidrcollection/example/cidrblocks HTTP/1.1
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<?xml version="1.0" ?>
<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>1 validation error detected: Value 'example' at 'collectionId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$</Message>
  </Error>
  <RequestId>cf8eb94a-15b7-0beb-da9a-17255e47c3f1</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListCidrCollections

Returns a paginated list of CIDR collections in the Amazon Web Services account (metadata only).

```http
GET /2013-04-01/cidrcollection HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<ListCidrCollectionsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <CidrCollections>
    <member>
      <Arn>arn:aws:route53::000000000000:cidrcollection/ce58732d-73e3-f376-8c6a-f9c7fdb62910</Arn>
      <Id>ce58732d-73e3-f376-8c6a-f9c7fdb62910</Id>
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
```

```http
HTTP/1.1 400 Bad Request
content-type: application/xml

<?xml version="1.0" ?>
<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidInput</Code>
    <Message>1 validation error detected: Value 'example' at 'collectionId' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$</Message>
  </Error>
  <RequestId>bf3f62b7-ebd3-6e87-58d7-e60a348838ac</RequestId>
</ErrorResponse>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### ListGeoLocations

Retrieves a list of supported geographic locations.

```http
GET /2013-04-01/geolocations HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

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
… (1320 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListHealthChecks

Retrieve a list of the health checks that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/healthcheck HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<ListHealthChecksResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HealthChecks/>
  <Marker/>
  <IsTruncated>false</IsTruncated>
  <MaxItems>100</MaxItems>
</ListHealthChecksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListHostedZones

Retrieves a list of the public and private hosted zones that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/hostedzone HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<ListHostedZonesResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZones/>
  <Marker/>
  <IsTruncated>false</IsTruncated>
  <MaxItems>100</MaxItems>
</ListHostedZonesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListHostedZonesByName

Retrieves a list of your hosted zones in lexicographic order.

```http
GET /2013-04-01/hostedzonesbyname HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<ListHostedZonesByNameResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <HostedZones/>
  <IsTruncated>false</IsTruncated>
  <MaxItems>100</MaxItems>
</ListHostedZonesByNameResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListHostedZonesByVPC

Lists all the private hosted zones that a specified VPC is associated with, regardless of which Amazon Web Services account or Amazon Web Services service owns the hosted zones.

```http
GET /2013-04-01/hostedzonesbyvpc?vpcid=example&vpcregion=us-east-1 HTTP/1.1
```

```http
HTTP/1.1 403 Forbidden
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>The VPC: example in region us-east-1 that you provided is not owned by you.</Message>
  </Error>
  <RequestId>fc7e82e0-9b9b-1faa-4ec0-d1fa8ce954b6</RequestId>
</ErrorResponse>
```

`403` `AccessDenied`: the request was refused by authorization.

### ListQueryLoggingConfigs

Lists the configurations for DNS query logging that are associated with the current Amazon Web Services account or the configuration that is associated with a specified hosted zone.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
GET /2013-04-01/queryloggingconfig HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<ListQueryLoggingConfigsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <QueryLoggingConfigs/>
</ListQueryLoggingConfigsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListResourceRecordSets

Lists the resource record sets in a specified hosted zone.

```http
GET /2013-04-01/hostedzone/example/rrset HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>39b8349d-e143-8c22-6612-14656a63f345</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#4-listresourcerecordsets) for a successful call.

### ListReusableDelegationSets

Retrieves a list of the reusable delegation sets that are associated with the current Amazon Web Services account.

```http
GET /2013-04-01/delegationset HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<ListReusableDelegationSetsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <DelegationSets>
    <DelegationSet>
      <Id>/delegationset/NG3MYLLVLRECXGN</Id>
      <CallerReference>example</CallerReference>
      <NameServers>
        <NameServer>ns-323.awsdns-00.com</NameServer>
        <NameServer>ns-104.awsdns-07.net</NameServer>
        <NameServer>ns-1850.awsdns-07.org</NameServer>
        <NameServer>ns-697.awsdns-13.co.uk</NameServer>
      </NameServers>
    </DelegationSet>
  </DelegationSets>
  <Marker/>
  <IsTruncated>false</IsTruncated>
  <MaxItems>100</MaxItems>
</ListReusableDelegationSetsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTagsForResource

Lists tags for one health check or hosted zone.

```http
GET /2013-04-01/tags/healthcheck/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>e4ca73e5-df98-7763-e3d8-615e987925d6</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTagsForResources

Lists tags for up to 10 health checks or hosted zones.

```http
POST /2013-04-01/tags/healthcheck HTTP/1.1
Content-Type: application/xml

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

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>d9f989df-2ba0-8dd4-19e6-a5f91be17f55</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTrafficPolicies

Gets information about the latest version for every traffic policy that is associated with the current Amazon Web Services account.

```http
GET /2013-04-01/trafficpolicies HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<ListTrafficPoliciesResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <TrafficPolicySummaries/>
  <IsTruncated>false</IsTruncated>
  <TrafficPolicyIdMarker/>
  <MaxItems>100</MaxItems>
</ListTrafficPoliciesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListTrafficPolicyInstances

Gets information about the traffic policy instances that you created by using the current Amazon Web Services account.

```http
GET /2013-04-01/trafficpolicyinstances HTTP/1.1
```

```http
HTTP/1.1 200 OK
content-type: application/xml

<ListTrafficPolicyInstancesResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <TrafficPolicyInstances/>
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
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>71bc5b10-a31c-9a82-92cb-e440d9606262</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTrafficPolicyInstancesByPolicy

Gets information about the traffic policy instances that you created by using a specify traffic policy version.

```http
GET /2013-04-01/trafficpolicyinstances/trafficpolicy?id=example&version=1 HTTP/1.1
```

```http
HTTP/1.1 501 Not Implemented
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ListTrafficPolicyInstancesByPolicy is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>02de1a51-c602-c404-a6d2-abcb34924140</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListTrafficPolicyVersions

Gets information about all of the versions for a specified traffic policy.

```http
GET /2013-04-01/trafficpolicies/example/versions HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicy</Code>
    <Message>The policy does not exist.</Message>
  </Error>
  <RequestId>cb9854a6-c519-40a0-4104-9c9eb9aeee07</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicy`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListVPCAssociationAuthorizations

Gets a list of the VPCs that were created by other accounts and that can be associated with a specified hosted zone because you've submitted one or more CreateVPCAssociationAuthorization requests.

```http
GET /2013-04-01/hostedzone/example/authorizevpcassociation HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>2fbf9f2b-5c49-3e47-102c-7632bcebfa3b</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TestDNSAnswer

Gets the value that Amazon Route 53 returns in response to a DNS request for a specified record name and type.

```http
GET /2013-04-01/testdnsanswer?hostedzoneid=example&recordname=example&recordtype=SOA HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>876ebc83-6bf9-3a63-90fb-7e65bd7808d6</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateHealthCheck

Updates an existing health check.

```http
POST /2013-04-01/healthcheck/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHealthCheck</Code>
    <Message>A health check with id example does not exist.</Message>
  </Error>
  <RequestId>2dd77fd3-9cba-40fb-aad7-f49a26610720</RequestId>
</ErrorResponse>
```

`404` `NoSuchHealthCheck`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateHostedZoneComment

Updates the comment for a specified hosted zone.

```http
POST /2013-04-01/hostedzone/example HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>f462a3e1-07b4-d937-6914-265d2fe8f67b</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateHostedZoneFeatures

Updates the features configuration for a hosted zone.

```http
POST /2013-04-01/hostedzone/example/features HTTP/1.1
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchHostedZone</Code>
    <Message>No hosted zone found with ID: example</Message>
  </Error>
  <RequestId>16c394e8-4b4e-86c2-b547-4db5697ef3b9</RequestId>
</ErrorResponse>
```

`404` `NoSuchHostedZone`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateTrafficPolicyComment

Updates the comment for a specified traffic policy version.

```http
POST /2013-04-01/trafficpolicy/example/1 HTTP/1.1
Content-Type: application/xml

<?xml version="1.0" ?>
<UpdateTrafficPolicyCommentRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Comment>example</Comment>
</UpdateTrafficPolicyCommentRequest>
```

```http
HTTP/1.1 404 Not Found
content-type: application/xml

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Sender</Type>
    <Code>NoSuchTrafficPolicy</Code>
    <Message>The policy does not exist.</Message>
  </Error>
  <RequestId>7a354e10-7491-3eb8-b7de-f6a8444f053f</RequestId>
</ErrorResponse>
```

`404` `NoSuchTrafficPolicy`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### UpdateTrafficPolicyInstance

After you submit a UpdateTrafficPolicyInstance request, there's a brief delay while Route 53 creates the resource record sets that are specified in the traffic policy definition.

```http
POST /2013-04-01/trafficpolicyinstance/example HTTP/1.1
Content-Type: application/xml

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

<ErrorResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation UpdateTrafficPolicyInstance is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>7e494601-85db-7673-96b9-eb01de4a65d1</RequestId>
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
