# Elastic Load Balancing (Classic)

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `Elastic Load Balancing`
- **sigv4 signing name:** `elasticloadbalancing`
- **Endpoint prefix:** `elasticloadbalancing`
- **ARN namespace:** `elasticloadbalancing`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `elasticloadbalancing`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

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
X-Amz-Date: 20261116T120837Z
Content-Length: 301
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
content-length: 306
date: Mon, 16 Nov 2026 12:08:37 GMT

<CreateLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <CreateLoadBalancerResult>
    <DNSName>127.0.0.1:37381</DNSName>
  </CreateLoadBalancerResult>
  <ResponseMetadata>
    <RequestId>1e5f8c51-c4c4-4f00-9c27-e12a09d938e6</RequestId>
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
X-Amz-Date: 20261116T120837Z
Content-Length: 227
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:08:37 GMT

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
    <RequestId>ec63e4c0-3b26-488e-b728-cf4690eb0d62</RequestId>
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
X-Amz-Date: 20261116T120837Z
Content-Length: 88
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancers
&Version=2012-06-01
&LoadBalancerNames.member.1=docs-demo-clb
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1584
date: Mon, 16 Nov 2026 12:08:37 GMT

<DescribeLoadBalancersResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <DescribeLoadBalancersResult>
    <LoadBalancerDescriptions>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
        <LoadBalancerName>docs-demo-clb</LoadBalancerName>
        <DNSName>127.0.0.1:37381</DNSName>
        <CanonicalHostedZoneName>docs-demo-clb-7282544588.us-east-1.elb.amazonaws.com</CanonicalHostedZoneName>
        <CanonicalHostedZoneNameID>Z35SXDOTRQ7X7K</CanonicalHostedZoneNameID>
        <ListenerDescriptions>
          <member>
            <Listener>
              <Protocol>HTTP</Protocol>
              <LoadBalancerPort>80</LoadBalancerPort>
              <InstanceProtocol>HTTP</InstanceProtocol>
              <InstancePort>8080</InstancePort>
            </Listener>
            <PolicyNames/>
          </member>
        </ListenerDescriptions>
        <Policies>
          <AppCookieStickinessPolicies/>
          <LBCookieStickinessPolicies/>
          <OtherPolicies/>
        </Policies>
        <BackendServerDescriptions/>
        <AvailabilityZones>
          <member>us-east-1a</member>
          <member>us-east-1b</member>
        </AvailabilityZones>
        <Subnets/>
        <Instances/>
        <HealthCheck>
          <Target>HTTP:8080/health</Target>
          <Interval>30</Interval>
          <Timeout>5</Timeout>
          <UnhealthyThreshold>2</UnhealthyThreshold>
          <HealthyThreshold>3</HealthyThreshold>
        </HealthCheck>
        <SecurityGroups/>
        <CreatedTime>2026-11-16T12:08:37.215637Z</CreatedTime>
        <Scheme>internet-facing</Scheme>
      </member>
    </LoadBalancerDescriptions>
  </DescribeLoadBalancersResult>
  <ResponseMetadata>
    <RequestId>cb1b7b28-08e9-4fff-853d-3e5ab30c780b</RequestId>
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
X-Amz-Date: 20261116T120837Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLoadBalancer
&Version=2012-06-01
&LoadBalancerName=docs-demo-clb
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 272
date: Mon, 16 Nov 2026 12:08:37 GMT

<DeleteLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <DeleteLoadBalancerResult/>
  <ResponseMetadata>
    <RequestId>4e9cd1eb-6e71-4ea8-a95e-a0574e01d046</RequestId>
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
X-Amz-Date: 20261116T120837Z
Content-Length: 88
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancers
&Version=2012-06-01
&LoadBalancerNames.member.1=docs-demo-clb
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 386ae01f-3b38-494e-9e71-6f15fc6f9362
content-length: 306
date: Mon, 16 Nov 2026 12:08:37 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'docs-demo-clb'</Message>
  </Error>
  <RequestId>386ae01f-3b38-494e-9e71-6f15fc6f9362</RequestId>
</ErrorResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 29 operations, 3 accepted the placeholder call, 5 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AddTags

Adds the specified tags to the specified load balancer.

```http
POST / HTTP/1.1

Action=AddTags
&Version=2012-06-01
&LoadBalancerNames.member.1=example
&Tags.member.1.Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>dd88aca5-cdef-4b00-af8a-2cac4b5dd6a4</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ApplySecurityGroupsToLoadBalancer

Associates one or more security groups with your load balancer in a virtual private cloud (VPC).

```http
POST / HTTP/1.1

Action=ApplySecurityGroupsToLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
&SecurityGroups.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>a4d4818f-f03a-4078-aacb-a10c185956f9</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AttachLoadBalancerToSubnets

Adds one or more subnets to the set of configured subnets for the specified load balancer.

```http
POST / HTTP/1.1

Action=AttachLoadBalancerToSubnets
&Version=2012-06-01
&LoadBalancerName=example
&Subnets.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>f587072f-e5bc-422d-a302-655c9420d61e</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ConfigureHealthCheck

Specifies the health check settings to use when evaluating the health state of your EC2 instances.

```http
POST / HTTP/1.1

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

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>HealthCheck Target must begin with one of HTTP, TCP, HTTPS, SSL</Message>
  </Error>
  <RequestId>88d8ec76-7394-488b-97d6-4053e0628fa1</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-configurehealthcheck) for a successful call.

### CreateAppCookieStickinessPolicy

Generates a stickiness policy with sticky session lifetimes that follow that of an application-generated cookie.

```http
POST / HTTP/1.1

Action=CreateAppCookieStickinessPolicy
&Version=2012-06-01
&LoadBalancerName=example
&PolicyName=example
&CookieName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>9df53c2c-4d15-4671-a700-9e3356c98643</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateLBCookieStickinessPolicy

Generates a stickiness policy with sticky session lifetimes controlled by the lifetime of the browser (user-agent) or a specified expiration period.

```http
POST / HTTP/1.1

Action=CreateLBCookieStickinessPolicy
&Version=2012-06-01
&LoadBalancerName=example
&PolicyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>e18a92bd-3823-4930-acdf-57df85837a4f</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateLoadBalancer

Creates a Classic Load Balancer.

```http
POST / HTTP/1.1

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

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>at least one of AvailabilityZones or Subnets must be specified</Message>
  </Error>
  <RequestId>6eb5eeff-341d-4b8b-9a34-8136a40ad3b3</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createloadbalancer) for a successful call.

### CreateLoadBalancerListeners

Creates one or more listeners for the specified load balancer.

```http
POST / HTTP/1.1

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

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Protocol must be one of HTTP, TCP, HTTPS, SSL</Message>
  </Error>
  <RequestId>767bcc3e-99b4-4f7f-afb3-876f1e1f9c2d</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateLoadBalancerPolicy

Creates a policy with the specified attributes for the specified load balancer.

```http
POST / HTTP/1.1

Action=CreateLoadBalancerPolicy
&Version=2012-06-01
&LoadBalancerName=example
&PolicyName=example
&PolicyTypeName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>PolicyTypeNotFound</Code>
    <Message>There is no LoadBalancerPolicyType named 'example'</Message>
  </Error>
  <RequestId>eb471f4d-9545-4726-97cf-de644cf0f1ee</RequestId>
</ErrorResponse>
```

`400` `PolicyTypeNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteLoadBalancer

Deletes the specified load balancer.

```http
POST / HTTP/1.1

Action=DeleteLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeleteLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <DeleteLoadBalancerResult/>
  <ResponseMetadata>
    <RequestId>3d065fb1-8552-4103-8fc5-de8f691602ca</RequestId>
  </ResponseMetadata>
</DeleteLoadBalancerResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-deleteloadbalancer) for a successful call.

### DeleteLoadBalancerListeners

Deletes the specified listeners from the specified load balancer.

```http
POST / HTTP/1.1

Action=DeleteLoadBalancerListeners
&Version=2012-06-01
&LoadBalancerName=example
&LoadBalancerPorts.member.1=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>d2dea8fb-9222-44ae-986a-7e4d837f0731</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteLoadBalancerPolicy

Deletes the specified policy from the specified load balancer.

```http
POST / HTTP/1.1

Action=DeleteLoadBalancerPolicy
&Version=2012-06-01
&LoadBalancerName=example
&PolicyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>5643eb3e-dd44-4800-91d7-ce09b48f7945</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeregisterInstancesFromLoadBalancer

Deregisters the specified instances from the specified load balancer.

```http
POST / HTTP/1.1

Action=DeregisterInstancesFromLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>MalformedInput</Code>
    <Message>1 validation error detected: Value null at 'instances' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>f80ab235-6f14-4038-8016-14f22ea721bc</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### DescribeAccountLimits

Describes the current Elastic Load Balancing resource limits for your AWS account.

```http
POST / HTTP/1.1

Action=DescribeAccountLimits
&Version=2012-06-01
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeAccountLimits is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>29f2ad5b-2c70-44d6-b5b8-f730f33771b6</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeInstanceHealth

Describes the state of the specified instances with respect to the specified load balancer.

```http
POST / HTTP/1.1

Action=DescribeInstanceHealth
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>e377145b-5deb-4676-83f2-3b51cc832289</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeLoadBalancerAttributes

Describes the attributes for the specified load balancer.

```http
POST / HTTP/1.1

Action=DescribeLoadBalancerAttributes
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>27b2e222-d786-4ccf-9195-231139aee2fe</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeLoadBalancerPolicies

Describes the specified policies.

```http
POST / HTTP/1.1

Action=DescribeLoadBalancerPolicies
&Version=2012-06-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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

Action=DescribeLoadBalancerPolicyTypes
&Version=2012-06-01
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeLoadBalancerPolicyTypes is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>984136fb-8ade-481c-9863-1147b1c7cca1</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeLoadBalancers

Describes the specified the load balancers.

```http
POST / HTTP/1.1

Action=DescribeLoadBalancers
&Version=2012-06-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeLoadBalancersResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <DescribeLoadBalancersResult>
    <LoadBalancerDescriptions/>
  </DescribeLoadBalancersResult>
  <ResponseMetadata>
    <RequestId>a058b0c9-a376-48ee-9387-fb00c41dd522</RequestId>
  </ResponseMetadata>
</DescribeLoadBalancersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#3-describeloadbalancers) for a successful call.

### DescribeTags

Describes the tags associated with the specified load balancers.

```http
POST / HTTP/1.1

Action=DescribeTags
&Version=2012-06-01
&LoadBalancerNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>87271fec-1c71-4116-87c7-09a9c4d104d4</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DetachLoadBalancerFromSubnets

Removes the specified subnets from the set of configured subnets for the load balancer.

```http
POST / HTTP/1.1

Action=DetachLoadBalancerFromSubnets
&Version=2012-06-01
&LoadBalancerName=example
&Subnets.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>dd1bae32-721b-400d-aaba-caf820f30799</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisableAvailabilityZonesForLoadBalancer

Removes the specified Availability Zones from the set of Availability Zones for the specified load balancer in EC2-Classic or a default VPC.

```http
POST / HTTP/1.1

Action=DisableAvailabilityZonesForLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
&AvailabilityZones.member.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DisableAvailabilityZonesForLoadBalancer is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>c093b427-a31b-43bb-ba2d-919e10fac009</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableAvailabilityZonesForLoadBalancer

Adds the specified Availability Zones to the set of Availability Zones for the specified load balancer in EC2-Classic or a default VPC.

```http
POST / HTTP/1.1

Action=EnableAvailabilityZonesForLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
&AvailabilityZones.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>d51bd400-3586-4a58-80d3-fa9e60aef73f</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyLoadBalancerAttributes

Modifies the attributes of the specified load balancer.

```http
POST / HTTP/1.1

Action=ModifyLoadBalancerAttributes
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>MalformedInput</Code>
    <Message>1 validation error detected: Value null at 'loadBalancerAttributes' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>06bed626-a55c-4584-984d-f2fbe760527f</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### RegisterInstancesWithLoadBalancer

Adds the specified instances to the specified load balancer.

```http
POST / HTTP/1.1

Action=RegisterInstancesWithLoadBalancer
&Version=2012-06-01
&LoadBalancerName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>MalformedInput</Code>
    <Message>1 validation error detected: Value null at 'instances' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>7cbdeb36-9788-4677-951b-c23198e1927b</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### RemoveTags

Removes one or more tags from the specified load balancer.

```http
POST / HTTP/1.1

Action=RemoveTags
&Version=2012-06-01
&LoadBalancerNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>MalformedInput</Code>
    <Message>1 validation error detected: Value null at 'tags' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>e42e877b-10ea-4762-8ec4-0f44e6b34ecb</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### SetLoadBalancerListenerSSLCertificate

Sets the certificate that terminates the specified listener's SSL connections.

```http
POST / HTTP/1.1

Action=SetLoadBalancerListenerSSLCertificate
&Version=2012-06-01
&LoadBalancerName=example
&LoadBalancerPort=1
&SSLCertificateId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation SetLoadBalancerListenerSSLCertificate is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>ad01e482-ab6e-42e5-8562-123d305ca83f</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### SetLoadBalancerPoliciesForBackendServer

Replaces the set of policies associated with the specified port on which the EC2 instance is listening with a new set of policies.

```http
POST / HTTP/1.1

Action=SetLoadBalancerPoliciesForBackendServer
&Version=2012-06-01
&LoadBalancerName=example
&InstancePort=1
&PolicyNames.member.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation SetLoadBalancerPoliciesForBackendServer is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>bf44e64f-2fed-4e40-85b9-4ce085450e79</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### SetLoadBalancerPoliciesOfListener

Replaces the current set of policies for the specified load balancer port with the specified set of policies.

```http
POST / HTTP/1.1

Action=SetLoadBalancerPoliciesOfListener
&Version=2012-06-01
&LoadBalancerName=example
&LoadBalancerPort=1
&PolicyNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>There is no ACTIVE Load Balancer named 'example'</Message>
  </Error>
  <RequestId>5a4bbdca-19f8-4928-9b01-15cd9ceba1d9</RequestId>
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
