# Elastic Load Balancing (Classic)

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `Elastic Load Balancing`
- **sigv4 signing name:** `elasticloadbalancing`
- **Endpoint prefix:** `elasticloadbalancing`
- **ARN namespace:** `elasticloadbalancing`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `elasticloadbalancing`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `DescribeLoadBalancers`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:elasticloadbalancing" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded; charset=utf-8' \
  --data-binary 'Action=DescribeLoadBalancers&Version=2012-06-01' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a Classic Load Balancer, configure its health check, then delete it. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateLoadBalancer

Classic load balancers are addressed by name, not ARN. The DNS name that comes back is a local address and port the emulator opened for this load balancer.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 301
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLoadBalancer
&Version=2012-06-01
&LoadBalancerName=docs-demo-clb
&Listeners.member.1.Protocol=HTTP
&Listeners.member.1.LoadBalancerPort=80
&Listeners.member.1.InstanceProtocol=HTTP
&Listeners.member.1.InstancePort=8080
&AvailabilityZones.member.1=us-east-1a
&AvailabilityZones.member.2=us-east-1b
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 339
date: Thu, 26 Nov 2026 18:29:58 GMT

<CreateLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <CreateLoadBalancerResult>
    <DNSName>docs-demo-clb-7282544588.us-east-1.elb.localhost</DNSName>
  </CreateLoadBalancerResult>
  <ResponseMetadata>
    <RequestId>1a5c879e-8d23-40d9-9140-a5192b997122</RequestId>
  </ResponseMetadata>
</CreateLoadBalancerResponse>
```

### 2. ConfigureHealthCheck

The health check target is `PROTOCOL:PORT/path`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 227
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ConfigureHealthCheck
&Version=2012-06-01
&LoadBalancerName=docs-demo-clb
&HealthCheck.Target=HTTP%3A8080%2Fhealth
&HealthCheck.Interval=30
&HealthCheck.Timeout=5
&HealthCheck.UnhealthyThreshold=2
&HealthCheck.HealthyThreshold=3
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 529
date: Thu, 26 Nov 2026 18:29:58 GMT

<ConfigureHealthCheckResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <ConfigureHealthCheckResult>
    <HealthCheck xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
      <Target>HTTP:8080/health</Target>
      <Interval>30</Interval>
      <Timeout>5</Timeout>
      <UnhealthyThreshold>2</UnhealthyThreshold>
      <HealthyThreshold>3</HealthyThreshold>
    </HealthCheck>
  </ConfigureHealthCheckResult>
  <ResponseMetadata>
    <RequestId>a6fda215-ef9e-4873-b3e2-93163ee49a7c</RequestId>
  </ResponseMetadata>
</ConfigureHealthCheckResponse>
```

### 3. DescribeLoadBalancers

Listeners, health check and zones.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 88
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancers
&Version=2012-06-01
&LoadBalancerNames.member.1=docs-demo-clb
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1613
date: Thu, 26 Nov 2026 18:29:58 GMT

<DescribeLoadBalancersResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <DescribeLoadBalancersResult>
    <LoadBalancerDescriptions>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
        <LoadBalancerName>docs-demo-clb</LoadBalancerName>
        <DNSName>docs-demo-clb-7282544588.us-east-1.elb.localhost</DNSName>
        <CanonicalHostedZoneName>docs-demo-clb-7282544588.us-east-1.elb.localhost</CanonicalHostedZoneName>
        <CanonicalHostedZoneNameID>Z35SXDOTRQ7X7K</CanonicalHostedZoneNameID>
        <ListenerDescriptions>
          <member>
            <Listener>
              <Protocol>HTTP</Protocol>
              <LoadBalancerPort>80</LoadBalancerPort>
              <InstanceProtocol>HTTP</InstanceProtocol>
              <InstancePort>8080</InstancePort>
            </Listener>
            <PolicyNames></PolicyNames>
          </member>
        </ListenerDescriptions>
        <Policies>
          <AppCookieStickinessPolicies></AppCookieStickinessPolicies>
          <LBCookieStickinessPolicies></LBCookieStickinessPolicies>
          <OtherPolicies></OtherPolicies>
        </Policies>
        <BackendServerDescriptions></BackendServerDescriptions>
        <AvailabilityZones>
          <member>us-east-1a</member>
          <member>us-east-1b</member>
        </AvailabilityZones>
        <Subnets></Subnets>
        <Instances></Instances>
        <HealthCheck>
          <Target>HTTP:8080/health</Target>
          <Interval>30</Interval>
          <Timeout>5</Timeout>
          <UnhealthyThreshold>2</UnhealthyThreshold>
          <HealthyThreshold>3</HealthyThreshold>
        </HealthCheck>
        <SecurityGroups></SecurityGroups>
        <CreatedTime>2026-11-26T18:29:58.672528Z</CreatedTime>
        <Scheme>internet-facing</Scheme>
      </member>
    </LoadBalancerDescriptions>
  </DescribeLoadBalancersResult>
  <ResponseMetadata>
    <RequestId>0c3e6055-b5cd-476b-92b8-92d38f251f66</RequestId>
  </ResponseMetadata>
</DescribeLoadBalancersResponse>
```

### 4. DeleteLoadBalancer

Deletes the load balancer.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLoadBalancer
&Version=2012-06-01
&LoadBalancerName=docs-demo-clb
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 272
date: Thu, 26 Nov 2026 18:29:58 GMT

<DeleteLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <DeleteLoadBalancerResult></DeleteLoadBalancerResult>
  <ResponseMetadata>
    <RequestId>5f49c205-ca04-4cbe-9856-8b42ec755410</RequestId>
  </ResponseMetadata>
</DeleteLoadBalancerResponse>
```

### 5. DescribeLoadBalancers

Now `LoadBalancerNotFound`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 88
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancers
&Version=2012-06-01
&LoadBalancerNames.member.1=docs-demo-clb
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 53f18006-52a0-496f-ae5e-0d57cac571fc
content-length: 306
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;docs-demo-clb&apos;</Message>
  </Error>
  <RequestId>53f18006-52a0-496f-ae5e-0d57cac571fc</RequestId>
</ErrorResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 29 operations, 3 accepted the placeholder call, 4 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AddTags

Adds the specified tags to the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddTags
&Version=2012-06-01
&LoadBalancerNames.member.1=example
&Tags.member.1.Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 1fb8349f-25e4-455c-a128-60fd8928c5f0
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>1fb8349f-25e4-455c-a128-60fd8928c5f0</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ApplySecurityGroupsToLoadBalancer

Associates one or more security groups with your load balancer in a virtual private cloud (VPC).

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 116
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ApplySecurityGroupsToLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
&SecurityGroups.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 00c33ac4-98ec-4c57-922e-2a6d01abcb04
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>00c33ac4-98ec-4c57-922e-2a6d01abcb04</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AttachLoadBalancerToSubnets

Adds one or more subnets to the set of configured subnets for the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 103
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AttachLoadBalancerToSubnets
&Version=2012-06-01
&LoadBalancerName=example
&Subnets.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: ccd04490-0300-42ca-a72e-8d2215fefcd0
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>ccd04490-0300-42ca-a72e-8d2215fefcd0</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ConfigureHealthCheck

Specifies the health check settings to use when evaluating the health state of your EC2 instances.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 207
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ConfigureHealthCheck
&Version=2012-06-01
&LoadBalancerName=example
&HealthCheck.Target=example
&HealthCheck.Interval=5
&HealthCheck.Timeout=2
&HealthCheck.UnhealthyThreshold=2
&HealthCheck.HealthyThreshold=2
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: aaeba6ef-0a8e-4d51-961b-db268138bfed
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>HealthCheck Target must begin with one of HTTP, TCP, HTTPS, SSL</Message>
  </Error>
  <RequestId>aaeba6ef-0a8e-4d51-961b-db268138bfed</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-configurehealthcheck) for a successful call.

### CreateAppCookieStickinessPolicy

Generates a stickiness policy with sticky session lifetimes that follow that of an application-generated cookie.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 120
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateAppCookieStickinessPolicy
&Version=2012-06-01
&LoadBalancerName=example
&PolicyName=example
&CookieName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 8afefbb7-1247-44da-9910-c0c382d9f8ae
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>8afefbb7-1247-44da-9910-c0c382d9f8ae</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateLBCookieStickinessPolicy

Generates a stickiness policy with sticky session lifetimes controlled by the lifetime of the browser (user-agent) or a specified expiration period.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 100
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLBCookieStickinessPolicy
&Version=2012-06-01
&LoadBalancerName=example
&PolicyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 3c53807c-ec05-4336-a493-07b430baf545
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>3c53807c-ec05-4336-a493-07b430baf545</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateLoadBalancer

Creates a Classic Load Balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 177
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
&Listeners.member.1.Protocol=example
&Listeners.member.1.LoadBalancerPort=1
&Listeners.member.1.InstancePort=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9c91587d-4cae-4bcd-8967-0e1f4bdc64d6
content-length: 299
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>at least one of AvailabilityZones or Subnets must be specified</Message>
  </Error>
  <RequestId>9c91587d-4cae-4bcd-8967-0e1f4bdc64d6</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createloadbalancer) for a successful call.

### CreateLoadBalancerListeners

Creates one or more listeners for the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 186
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLoadBalancerListeners
&Version=2012-06-01
&LoadBalancerName=example
&Listeners.member.1.Protocol=example
&Listeners.member.1.LoadBalancerPort=1
&Listeners.member.1.InstancePort=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a8ece898-d446-44e1-8ffd-5f69a22a19ee
content-length: 282
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Protocol must be one of HTTP, TCP, HTTPS, SSL</Message>
  </Error>
  <RequestId>a8ece898-d446-44e1-8ffd-5f69a22a19ee</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateLoadBalancerPolicy

Creates a policy with the specified attributes for the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 117
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLoadBalancerPolicy
&Version=2012-06-01
&LoadBalancerName=example
&PolicyName=example
&PolicyTypeName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 0ff16fc2-7a31-4011-9abf-e0ac6090c3db
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>PolicyTypeNotFound</Code>
    <Message>There is no LoadBalancerPolicyType named &apos;example&apos;</Message>
  </Error>
  <RequestId>0ff16fc2-7a31-4011-9abf-e0ac6090c3db</RequestId>
</ErrorResponse>
```

`400` `PolicyTypeNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteLoadBalancer

Deletes the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 272
date: Thu, 26 Nov 2026 18:29:58 GMT

<DeleteLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <DeleteLoadBalancerResult></DeleteLoadBalancerResult>
  <ResponseMetadata>
    <RequestId>b076c444-ec80-44e3-a88a-b50168add53c</RequestId>
  </ResponseMetadata>
</DeleteLoadBalancerResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-deleteloadbalancer) for a successful call.

### DeleteLoadBalancerListeners

Deletes the specified listeners from the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 107
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLoadBalancerListeners
&Version=2012-06-01
&LoadBalancerName=example
&LoadBalancerPorts.member.1=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 77617515-4974-4f25-9e75-4c92420ace42
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>77617515-4974-4f25-9e75-4c92420ace42</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteLoadBalancerPolicy

Deletes the specified policy from the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLoadBalancerPolicy
&Version=2012-06-01
&LoadBalancerName=example
&PolicyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c23dfabe-9648-4bd9-834c-f2f9785e5389
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>c23dfabe-9648-4bd9-834c-f2f9785e5389</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeregisterInstancesFromLoadBalancer

Deregisters the specified instances from the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeregisterInstancesFromLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: aadbbbe8-e538-479d-ae5d-890e3317a28e
content-length: 309
date: Thu, 26 Nov 2026 18:29:58 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>MalformedInput</Code>
    <Message>1 validation error detected: Value null at &apos;instances&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>aadbbbe8-e538-479d-ae5d-890e3317a28e</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### DescribeAccountLimits

Describes the current Elastic Load Balancing resource limits for your AWS account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAccountLimits
&Version=2012-06-01
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: c4ae7c05-498f-4961-b000-49db8bbe877c
content-length: 320
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeAccountLimits is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>c4ae7c05-498f-4961-b000-49db8bbe877c</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeInstanceHealth

Describes the state of the specified instances with respect to the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeInstanceHealth
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 33272cc6-2927-4914-8b16-886b0a1453b6
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>33272cc6-2927-4914-8b16-886b0a1453b6</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeLoadBalancerAttributes

Describes the attributes for the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancerAttributes
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: aa847b16-53c8-4616-ac9c-4b23ca5fb61d
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>aa847b16-53c8-4616-ac9c-4b23ca5fb61d</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeLoadBalancerPolicies

Describes the specified policies.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancerPolicies
&Version=2012-06-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 39073
date: Thu, 26 Nov 2026 18:29:58 GMT

<DescribeLoadBalancerPoliciesResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <DescribeLoadBalancerPoliciesResult>
    <PolicyDescriptions>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
        <PolicyName>ELBSample-ELBSecurityPolicy-2016-08</PolicyName>
        <PolicyTypeName>SSLNegotiationPolicyType</PolicyTypeName>
        <PolicyAttributeDescriptions>
          <member>
            <AttributeName>Protocol-TLSv1</AttributeName>
            <AttributeValue>true</AttributeValue>
          </member>
          <member>
            <AttributeName>Protocol-SSLv3</AttributeName>
            <AttributeValue>false</AttributeValue>
          </member>
          <member>
            <AttributeName>Protocol-TLSv1.1</AttributeName>
            <AttributeValue>true</AttributeValue>
          </member>
          <member>
… (1463 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLoadBalancerPolicyTypes

Describes the specified load balancer policy types or all load balancer policy types.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 57
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancerPolicyTypes
&Version=2012-06-01
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: bfa46972-2df6-4497-a3e2-1529a1ef6671
content-length: 330
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeLoadBalancerPolicyTypes is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>bfa46972-2df6-4497-a3e2-1529a1ef6671</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeLoadBalancers

Describes the specified the load balancers.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancers
&Version=2012-06-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 337
date: Thu, 26 Nov 2026 18:29:58 GMT

<DescribeLoadBalancersResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <DescribeLoadBalancersResult>
    <LoadBalancerDescriptions></LoadBalancerDescriptions>
  </DescribeLoadBalancersResult>
  <ResponseMetadata>
    <RequestId>b160ca8e-12c6-451f-aefb-78c8f5ea7956</RequestId>
  </ResponseMetadata>
</DescribeLoadBalancersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#3-describeloadbalancers) for a successful call.

### DescribeTags

Describes the tags associated with the specified load balancers.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTags
&Version=2012-06-01
&LoadBalancerNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2cebe101-be20-4c09-9db2-307dddff2496
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>2cebe101-be20-4c09-9db2-307dddff2496</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DetachLoadBalancerFromSubnets

Removes the specified subnets from the set of configured subnets for the load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 105
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DetachLoadBalancerFromSubnets
&Version=2012-06-01
&LoadBalancerName=example
&Subnets.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c19db7ea-a660-4218-af8d-809f5d71e540
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>c19db7ea-a660-4218-af8d-809f5d71e540</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisableAvailabilityZonesForLoadBalancer

Removes the specified Availability Zones from the set of Availability Zones for the specified load balancer in EC2-Classic or a default VPC.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 125
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DisableAvailabilityZonesForLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
&AvailabilityZones.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 531851ea-746a-4c75-8164-fcfde3e935dc
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>531851ea-746a-4c75-8164-fcfde3e935dc</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### EnableAvailabilityZonesForLoadBalancer

Adds the specified Availability Zones to the set of Availability Zones for the specified load balancer in EC2-Classic or a default VPC.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 124
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=EnableAvailabilityZonesForLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
&AvailabilityZones.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 5c22018d-4cc8-449e-8f19-744c5c26b950
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>5c22018d-4cc8-449e-8f19-744c5c26b950</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyLoadBalancerAttributes

Modifies the attributes of the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 79
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyLoadBalancerAttributes
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f11220ac-4ab3-4fa7-b84a-e24a613fd3af
content-length: 322
date: Thu, 26 Nov 2026 18:29:58 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>MalformedInput</Code>
    <Message>1 validation error detected: Value null at &apos;loadBalancerAttributes&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>f11220ac-4ab3-4fa7-b84a-e24a613fd3af</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### RegisterInstancesWithLoadBalancer

Adds the specified instances to the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RegisterInstancesWithLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 5a04ae02-435b-4d42-9795-9b427aa1e507
content-length: 309
date: Thu, 26 Nov 2026 18:29:58 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>MalformedInput</Code>
    <Message>1 validation error detected: Value null at &apos;instances&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>5a04ae02-435b-4d42-9795-9b427aa1e507</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### RemoveTags

Removes one or more tags from the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveTags
&Version=2012-06-01
&LoadBalancerNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e2d8983b-d794-4e15-842a-0b705d19e88e
content-length: 304
date: Thu, 26 Nov 2026 18:29:58 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>MalformedInput</Code>
    <Message>1 validation error detected: Value null at &apos;tags&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>e2d8983b-d794-4e15-842a-0b705d19e88e</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### SetLoadBalancerListenerSSLCertificate

Sets the certificate that terminates the specified listener's SSL connections.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 132
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetLoadBalancerListenerSSLCertificate
&Version=2012-06-01
&LoadBalancerName=example
&LoadBalancerPort=1
&SSLCertificateId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: 21fed701-5b95-4610-aec1-3b528df5c7ed
content-length: 336
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation SetLoadBalancerListenerSSLCertificate is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>21fed701-5b95-4610-aec1-3b528df5c7ed</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### SetLoadBalancerPoliciesForBackendServer

Replaces the set of policies associated with the specified port on which the EC2 instance is listening with a new set of policies.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 134
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetLoadBalancerPoliciesForBackendServer
&Version=2012-06-01
&LoadBalancerName=example
&InstancePort=1
&PolicyNames.member.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: da876cf5-7b52-4143-8b48-fa6f6c70539a
content-length: 338
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation SetLoadBalancerPoliciesForBackendServer is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>da876cf5-7b52-4143-8b48-fa6f6c70539a</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### SetLoadBalancerPoliciesOfListener

Replaces the current set of policies for the specified load balancer port with the specified set of policies.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182958Z
Content-Length: 132
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetLoadBalancerPoliciesOfListener
&Version=2012-06-01
&LoadBalancerName=example
&LoadBalancerPort=1
&PolicyNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 0328264e-3601-409b-918d-99db0e620b16
content-length: 300
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named &apos;example&apos;</Message>
  </Error>
  <RequestId>0328264e-3601-409b-918d-99db0e620b16</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/elasticloadbalancing/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with a form-encoded body: `Action` names the operation, `Version` the API version, and every input member is a flattened key such as `Tags.member.1.Key`.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Elastic Load Balancing (Classic) answers in AWS: XML wrapped in `<OperationResponse><OperationResult>`. Errors captured on this page are an XML document rooted at `<ErrorResponse>`.

### Local endpoints

Where AWS returns a regional hostname, the emulator returns a local address: the load balancer's DNS name is `127.0.0.1:<port>`, a port the emulator opened for that resource. Those addresses are local to the emulator; the [Docker setup](README.md#running-the-emulator-in-docker) publishes only port 9297, so from your machine only the API endpoint is reachable.
