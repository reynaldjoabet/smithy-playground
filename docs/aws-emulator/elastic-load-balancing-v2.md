# Elastic Load Balancing v2 (Application, Network and Gateway)

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `Elastic Load Balancing v2`
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
  --data-binary 'Action=DescribeLoadBalancers&Version=2015-12-01' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create an Application Load Balancer with a target group and a listener in the default VPC. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateLoadBalancer

An ALB needs subnets in at least two Availability Zones; it starts in `provisioning`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120836Z
Content-Length: 187
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLoadBalancer
&Version=2015-12-01
&Name=docs-demo-alb
&Subnets.member.1=subnet-00000000000000007
&Subnets.member.2=subnet-00000000000000009
&Scheme=internet-facing
&Type=application
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1339
date: Mon, 16 Nov 2026 12:08:36 GMT

<CreateLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <CreateLoadBalancerResult>
    <LoadBalancers>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <LoadBalancerArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:loadbalancer/app/docs-demo-alb/f508d3a06baad0fb</LoadBalancerArn>
        <DNSName>127.0.0.1:36109</DNSName>
        <CanonicalHostedZoneId>Z35SXDOTRQ7X7K</CanonicalHostedZoneId>
        <CreatedTime>2026-11-16T12:08:36.488766Z</CreatedTime>
        <LoadBalancerName>docs-demo-alb</LoadBalancerName>
        <Scheme>internet-facing</Scheme>
        <VpcId>vpc-00000000000000001</VpcId>
        <State>
          <Code>provisioning</Code>
        </State>
        <Type>application</Type>
        <AvailabilityZones>
          <member>
            <ZoneName>us-east-1a</ZoneName>
            <SubnetId>subnet-00000000000000007</SubnetId>
            <LoadBalancerAddresses/>
          </member>
          <member>
            <ZoneName>us-east-1b</ZoneName>
            <SubnetId>subnet-00000000000000009</SubnetId>
            <LoadBalancerAddresses/>
          </member>
        </AvailabilityZones>
        <SecurityGroups>
          <member>sg-0000000000000000d</member>
        </SecurityGroups>
        <IpAddressType>ipv4</IpAddressType>
        <EnablePrefixForIpv6SourceNat>off</EnablePrefixForIpv6SourceNat>
      </member>
    </LoadBalancers>
  </CreateLoadBalancerResult>
  <ResponseMetadata>
    <RequestId>0b568a6b-14e1-4f1a-8fc6-370621527546</RequestId>
  </ResponseMetadata>
</CreateLoadBalancerResponse>
```

### 2. CreateTargetGroup

Targets register with the group, not with the load balancer.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120836Z
Content-Length: 151
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateTargetGroup
&Version=2015-12-01
&Name=docs-demo-tg
&Protocol=HTTP
&Port=80
&VpcId=vpc-00000000000000001
&HealthCheckPath=%2Fhealth
&TargetType=ip
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1207
date: Mon, 16 Nov 2026 12:08:36 GMT

<CreateTargetGroupResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <CreateTargetGroupResult>
    <TargetGroups>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <TargetGroupArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:targetgroup/docs-demo-tg/524c4058e34a75e4</TargetGroupArn>
        <TargetGroupName>docs-demo-tg</TargetGroupName>
        <Protocol>HTTP</Protocol>
        <Port>80</Port>
        <VpcId>vpc-00000000000000001</VpcId>
        <HealthCheckProtocol>HTTP</HealthCheckProtocol>
        <HealthCheckPort>traffic-port</HealthCheckPort>
        <HealthCheckEnabled>true</HealthCheckEnabled>
        <HealthCheckIntervalSeconds>30</HealthCheckIntervalSeconds>
        <HealthCheckTimeoutSeconds>5</HealthCheckTimeoutSeconds>
        <HealthyThresholdCount>5</HealthyThresholdCount>
        <UnhealthyThresholdCount>2</UnhealthyThresholdCount>
        <HealthCheckPath>/health</HealthCheckPath>
        <Matcher>
          <HttpCode>200</HttpCode>
        </Matcher>
        <LoadBalancerArns/>
        <TargetType>ip</TargetType>
        <ProtocolVersion>HTTP1</ProtocolVersion>
        <IpAddressType>ipv4</IpAddressType>
      </member>
    </TargetGroups>
  </CreateTargetGroupResult>
  <ResponseMetadata>
    <RequestId>db9c16f8-7ceb-4d68-845d-9af5aacc4fc1</RequestId>
  </ResponseMetadata>
</CreateTargetGroupResponse>
```

### 3. CreateListener

The listener forwards port 80 to the target group.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120836Z
Content-Length: 378
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateListener
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Aloadbalancer%2Fapp%2Fdocs-demo-alb%2Ff508d3a06baad0fb
&Protocol=HTTP
&Port=80
&DefaultActions.member.1.Type=forward
&DefaultActions.member.1.TargetGroupArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Atargetgroup%2Fdocs-demo-tg%2F524c4058e34a75e4
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1229
date: Mon, 16 Nov 2026 12:08:36 GMT

<CreateListenerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <CreateListenerResult>
    <Listeners>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <ListenerArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:listener/app/docs-demo-alb/f508d3a06baad0fb/9c17eb7bda07d3bf</ListenerArn>
        <LoadBalancerArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:loadbalancer/app/docs-demo-alb/f508d3a06baad0fb</LoadBalancerArn>
        <Port>80</Port>
        <Protocol>HTTP</Protocol>
        <Certificates/>
        <DefaultActions>
          <member>
            <Type>forward</Type>
            <TargetGroupArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:targetgroup/docs-demo-tg/524c4058e34a75e4</TargetGroupArn>
            <ForwardConfig>
              <TargetGroups>
                <member>
                  <TargetGroupArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:targetgroup/docs-demo-tg/524c4058e34a75e4</TargetGroupArn>
                  <Weight>1</Weight>
                </member>
              </TargetGroups>
              <TargetGroupStickinessConfig>
                <Enabled>false</Enabled>
              </TargetGroupStickinessConfig>
            </ForwardConfig>
          </member>
        </DefaultActions>
        <AlpnPolicy/>
      </member>
    </Listeners>
  </CreateListenerResult>
  <ResponseMetadata>
    <RequestId>0d734ec9-8cea-44a4-ad91-83131a17882f</RequestId>
  </ResponseMetadata>
</CreateListenerResponse>
```

### 4. DescribeLoadBalancers

The load balancer is now `active`. Its DNS name is a local address and port the emulator opened for it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120836Z
Content-Length: 189
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancers
&Version=2015-12-01
&LoadBalancerArns.member.1=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Aloadbalancer%2Fapp%2Fdocs-demo-alb%2Ff508d3a06baad0fb
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1345
date: Mon, 16 Nov 2026 12:08:36 GMT

<DescribeLoadBalancersResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeLoadBalancersResult>
    <LoadBalancers>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <LoadBalancerArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:loadbalancer/app/docs-demo-alb/f508d3a06baad0fb</LoadBalancerArn>
        <DNSName>127.0.0.1:36109</DNSName>
        <CanonicalHostedZoneId>Z35SXDOTRQ7X7K</CanonicalHostedZoneId>
        <CreatedTime>2026-11-16T12:08:36.488766Z</CreatedTime>
        <LoadBalancerName>docs-demo-alb</LoadBalancerName>
        <Scheme>internet-facing</Scheme>
        <VpcId>vpc-00000000000000001</VpcId>
        <State>
          <Code>active</Code>
        </State>
        <Type>application</Type>
        <AvailabilityZones>
          <member>
            <ZoneName>us-east-1a</ZoneName>
            <SubnetId>subnet-00000000000000007</SubnetId>
            <LoadBalancerAddresses/>
          </member>
          <member>
            <ZoneName>us-east-1b</ZoneName>
            <SubnetId>subnet-00000000000000009</SubnetId>
            <LoadBalancerAddresses/>
          </member>
        </AvailabilityZones>
        <SecurityGroups>
          <member>sg-0000000000000000d</member>
        </SecurityGroups>
        <IpAddressType>ipv4</IpAddressType>
        <EnablePrefixForIpv6SourceNat>off</EnablePrefixForIpv6SourceNat>
      </member>
    </LoadBalancers>
  </DescribeLoadBalancersResult>
  <ResponseMetadata>
    <RequestId>b2f16998-bcb6-46f4-beac-281ffb7aefe2</RequestId>
  </ResponseMetadata>
</DescribeLoadBalancersResponse>
```

### 5. DeleteTargetGroup

A target group a listener still forwards to is in use: `ResourceInUse`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120837Z
Content-Length: 166
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTargetGroup
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Atargetgroup%2Fdocs-demo-tg%2F524c4058e34a75e4
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 41e4df94-f945-4ca4-b08e-e01f605f86f3
content-length: 397
date: Mon, 16 Nov 2026 12:08:37 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ResourceInUse</Code>
    <Message>Target group 'arn:aws:elasticloadbalancing:us-east-1:000000000000:targetgroup/docs-demo-tg/524c4058e34a75e4' is currently in use by a listener or a rule</Message>
  </Error>
  <RequestId>41e4df94-f945-4ca4-b08e-e01f605f86f3</RequestId>
</ErrorResponse>
```

### 6. DeleteListener

Remove the listener ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120837Z
Content-Length: 183
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteListener
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Alistener%2Fapp%2Fdocs-demo-alb%2Ff508d3a06baad0fb%2F9c17eb7bda07d3bf
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 256
date: Mon, 16 Nov 2026 12:08:37 GMT

<DeleteListenerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DeleteListenerResult/>
  <ResponseMetadata>
    <RequestId>159d50ec-ae26-4921-b2c1-6125eed6dbde</RequestId>
  </ResponseMetadata>
</DeleteListenerResponse>
```

### 7. DeleteTargetGroup

... then the target group ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120837Z
Content-Length: 166
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTargetGroup
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Atargetgroup%2Fdocs-demo-tg%2F524c4058e34a75e4
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 268
date: Mon, 16 Nov 2026 12:08:37 GMT

<DeleteTargetGroupResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DeleteTargetGroupResult/>
  <ResponseMetadata>
    <RequestId>51470493-5061-4a27-a2bf-45894816aa29</RequestId>
  </ResponseMetadata>
</DeleteTargetGroupResponse>
```

### 8. DeleteLoadBalancer

... and the load balancer.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120837Z
Content-Length: 176
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLoadBalancer
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Aloadbalancer%2Fapp%2Fdocs-demo-alb%2Ff508d3a06baad0fb
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 272
date: Mon, 16 Nov 2026 12:08:37 GMT

<DeleteLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DeleteLoadBalancerResult/>
  <ResponseMetadata>
    <RequestId>ea6c9a2f-38ef-4bfc-a6d6-2ca56e76b027</RequestId>
  </ResponseMetadata>
</DeleteLoadBalancerResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 51 operations, 9 accepted the placeholder call, 5 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AddListenerCertificates

Adds the specified SSL server certificate to the certificate list for the specified HTTPS or TLS listener.

```http
POST / HTTP/1.1

Action=AddListenerCertificates
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at 'certificates' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>9faebeb2-f1a8-4e9e-bb61-1aa59f65979d</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AddTags

Adds the specified tags to the specified Elastic Load Balancing resource.

```http
POST / HTTP/1.1

Action=AddTags
&Version=2015-12-01
&ResourceArns.member.1=example
&Tags.member.1.Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>'example' in ResourceArns is not the ARN of an Elastic Load Balancing resource</Message>
  </Error>
  <RequestId>92ced4b4-f85d-4539-b66c-6eb45bd49ec8</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AddTrustStoreRevocations

Adds the specified revocation file to the specified trust store.

```http
POST / HTTP/1.1

Action=AddTrustStoreRevocations
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation AddTrustStoreRevocations is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>93e2b5f2-2e50-4ef7-93fa-933e9f09dc9d</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateListener

Creates a listener for the specified Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1

Action=CreateListener
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&DefaultActions.member.1.Type=forward
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>Load balancer 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>1cc8821e-1db7-47d3-a71e-8ee191505725</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#3-createlistener) for a successful call.

### CreateLoadBalancer

Creates an Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1

Action=CreateLoadBalancer
&Version=2015-12-01
&Name=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidConfigurationRequest</Code>
    <Message>You must specify either Subnets or SubnetMappings</Message>
  </Error>
  <RequestId>5589ef77-fdf7-4230-9a92-5fca1a4ea8ff</RequestId>
</ErrorResponse>
```

`400` `InvalidConfigurationRequest`. See [the walkthrough](#1-createloadbalancer) for a successful call.

### CreateRule

Creates a rule for the specified listener.

```http
POST / HTTP/1.1

Action=CreateRule
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Priority=1
&Actions.member.1.Type=forward
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at 'conditions' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>ff016454-00de-481c-bc3a-269756ae1b19</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateTargetGroup

Creates a target group.

```http
POST / HTTP/1.1

Action=CreateTargetGroup
&Version=2015-12-01
&Name=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateTargetGroupResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <CreateTargetGroupResult>
    <TargetGroups>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <TargetGroupArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:targetgroup/example/a6acbd5f4ced3490</TargetGroupArn>
        <TargetGroupName>example</TargetGroupName>
        <HealthCheckProtocol>TCP</HealthCheckProtocol>
        <HealthCheckPort>traffic-port</HealthCheckPort>
        <HealthCheckEnabled>true</HealthCheckEnabled>
        <HealthCheckIntervalSeconds>30</HealthCheckIntervalSeconds>
        <HealthCheckTimeoutSeconds>10</HealthCheckTimeoutSeconds>
        <HealthyThresholdCount>5</HealthyThresholdCount>
        <UnhealthyThresholdCount>2</UnhealthyThresholdCount>
        <LoadBalancerArns/>
        <TargetType>instance</TargetType>
        <IpAddressType>ipv4</IpAddressType>
      </member>
    </TargetGroups>
  </CreateTargetGroupResult>
  <ResponseMetadata>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-createtargetgroup) for a successful call.

### CreateTrustStore

Creates a trust store.

```http
POST / HTTP/1.1

Action=CreateTrustStore
&Version=2015-12-01
&Name=example
&CaCertificatesBundleS3Bucket=example
&CaCertificatesBundleS3Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>CaCertificatesBundleNotFound</Code>
    <Message>Provided CA certificate bundle was not found</Message>
  </Error>
  <RequestId>a22c0bbf-d68a-4b0f-a8db-7d7c4a93bbce</RequestId>
</ErrorResponse>
```

`400` `CaCertificatesBundleNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteListener

Deletes the specified listener.

```http
POST / HTTP/1.1

Action=DeleteListener
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' is not a valid listener ARN</Message>
  </Error>
  <RequestId>fe30139d-9cc1-45b6-ae06-ba4837eaa0e1</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-deletelistener) for a successful call.

### DeleteLoadBalancer

Deletes the specified Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1

Action=DeleteLoadBalancer
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeleteLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DeleteLoadBalancerResult/>
  <ResponseMetadata>
    <RequestId>0970d90a-d797-4074-96cc-c2ee4712fe69</RequestId>
  </ResponseMetadata>
</DeleteLoadBalancerResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#8-deleteloadbalancer) for a successful call.

### DeleteRule

Deletes the specified rule.

```http
POST / HTTP/1.1

Action=DeleteRule
&Version=2015-12-01
&RuleArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>RuleNotFound</Code>
    <Message>Rule 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>95073873-233e-44e9-9a16-2b4dc8f4b112</RequestId>
</ErrorResponse>
```

`400` `RuleNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSharedTrustStoreAssociation

Deletes a shared trust store association.

```http
POST / HTTP/1.1

Action=DeleteSharedTrustStoreAssociation
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ResourceArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>dd01d117-c3ba-4cd5-b74a-ecf8897dcc10</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTargetGroup

Deletes the specified target group.

```http
POST / HTTP/1.1

Action=DeleteTargetGroup
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeleteTargetGroupResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DeleteTargetGroupResult/>
  <ResponseMetadata>
    <RequestId>d3dfe62a-10ed-4ce5-a6cb-741009d7ea11</RequestId>
  </ResponseMetadata>
</DeleteTargetGroupResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#7-deletetargetgroup) for a successful call.

### DeleteTrustStore

Deletes a trust store.

```http
POST / HTTP/1.1

Action=DeleteTrustStore
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>54b8cbf9-6809-4c64-9481-f32a64988edf</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeregisterTargets

Deregisters the specified targets from the specified target group.

```http
POST / HTTP/1.1

Action=DeregisterTargets
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Targets.member.1.Id=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TargetGroupNotFound</Code>
    <Message>Target groups 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>80cc18e7-5719-43d4-bb75-9ab5343c308e</RequestId>
</ErrorResponse>
```

`400` `TargetGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeAccountLimits

Describes the current Elastic Load Balancing resource limits for your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=DescribeAccountLimits
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeAccountLimitsResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeAccountLimitsResult>
    <Limits>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <Name>application-load-balancers</Name>
        <Max>1000</Max>
      </member>
      <member>
        <Name>application-load-balancer-capacity-units</Name>
        <Max>3000</Max>
      </member>
      <member>
        <Name>ca-certificates-per-trust-store</Name>
        <Max>25</Max>
      </member>
      <member>
        <Name>ca-certificates-subject-size-per-trust-store</Name>
        <Max>256</Max>
      </member>
      <member>
… (121 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCapacityReservation

Describes the capacity reservation status for the specified load balancer.

```http
POST / HTTP/1.1

Action=DescribeCapacityReservation
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>Load balancer 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>fb0bb367-1c74-466a-b093-bbd7118ea7ca</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeListenerAttributes

Describes the attributes for the specified listener.

```http
POST / HTTP/1.1

Action=DescribeListenerAttributes
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' is not a valid listener ARN</Message>
  </Error>
  <RequestId>70d6bad0-926a-4368-ab7b-cbdb66325d45</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeListenerCertificates

Describes the default certificate and the certificate list for the specified HTTPS or TLS listener.

```http
POST / HTTP/1.1

Action=DescribeListenerCertificates
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' is not a valid listener ARN</Message>
  </Error>
  <RequestId>453545fe-3b8b-47fa-8882-6300dee35f4f</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeListeners

Describes the specified listeners or the listeners for the specified Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1

Action=DescribeListeners
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeListenersResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeListenersResult>
    <Listeners/>
  </DescribeListenersResult>
  <ResponseMetadata>
    <RequestId>3f11408c-44f6-4ff3-8a07-c03b50319002</RequestId>
  </ResponseMetadata>
</DescribeListenersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLoadBalancerAttributes

Describes the attributes for the specified Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1

Action=DescribeLoadBalancerAttributes
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>Load balancer 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>5fbf8ed4-b1be-4265-8c95-5c74d2b44c14</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeLoadBalancers

Describes the specified load balancers or all of your load balancers.

```http
POST / HTTP/1.1

Action=DescribeLoadBalancers
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeLoadBalancersResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeLoadBalancersResult>
    <LoadBalancers/>
  </DescribeLoadBalancersResult>
  <ResponseMetadata>
    <RequestId>9859d055-cee2-41c1-ad92-2167b0f6c7db</RequestId>
  </ResponseMetadata>
</DescribeLoadBalancersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-describeloadbalancers) for a successful call.

### DescribeRules

Describes the specified rules or the rules for the specified listener.

```http
POST / HTTP/1.1

Action=DescribeRules
&Version=2015-12-01
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>You must specify either ListenerArn or RuleArns</Message>
  </Error>
  <RequestId>19d542d2-5532-4417-96fc-b89056f04eae</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeSSLPolicies

Describes the specified policies or all policies used for SSL negotiation.

```http
POST / HTTP/1.1

Action=DescribeSSLPolicies
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeSSLPoliciesResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeSSLPoliciesResult>
    <SslPolicies>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <SslProtocols>
          <member>TLSv1</member>
          <member>TLSv1.1</member>
          <member>TLSv1.2</member>
        </SslProtocols>
        <Ciphers>
          <member>
            <Name>ECDHE-ECDSA-AES128-GCM-SHA256</Name>
            <Priority>1</Priority>
          </member>
          <member>
            <Name>ECDHE-RSA-AES128-GCM-SHA256</Name>
            <Priority>2</Priority>
          </member>
          <member>
            <Name>ECDHE-ECDSA-AES128-SHA256</Name>
… (2903 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTags

Describes the tags for the specified Elastic Load Balancing resources.

```http
POST / HTTP/1.1

Action=DescribeTags
&Version=2015-12-01
&ResourceArns.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>'example' in ResourceArns is not the ARN of an Elastic Load Balancing resource</Message>
  </Error>
  <RequestId>1f722e78-1e7f-41f0-a0ca-7ac0375557bb</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeTargetGroupAttributes

Describes the attributes for the specified target group.

```http
POST / HTTP/1.1

Action=DescribeTargetGroupAttributes
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TargetGroupNotFound</Code>
    <Message>Target groups 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>f2389318-ffa7-4d8a-96ae-2bd21fb4056c</RequestId>
</ErrorResponse>
```

`400` `TargetGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeTargetGroups

Describes the specified target groups or all of your target groups.

```http
POST / HTTP/1.1

Action=DescribeTargetGroups
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeTargetGroupsResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeTargetGroupsResult>
    <TargetGroups>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <TargetGroupArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:targetgroup/example/a6acbd5f4ced3490</TargetGroupArn>
        <TargetGroupName>example</TargetGroupName>
        <HealthCheckProtocol>TCP</HealthCheckProtocol>
        <HealthCheckPort>traffic-port</HealthCheckPort>
        <HealthCheckEnabled>true</HealthCheckEnabled>
        <HealthCheckIntervalSeconds>30</HealthCheckIntervalSeconds>
        <HealthCheckTimeoutSeconds>10</HealthCheckTimeoutSeconds>
        <HealthyThresholdCount>5</HealthyThresholdCount>
        <UnhealthyThresholdCount>2</UnhealthyThresholdCount>
        <LoadBalancerArns/>
        <TargetType>instance</TargetType>
        <IpAddressType>ipv4</IpAddressType>
      </member>
    </TargetGroups>
  </DescribeTargetGroupsResult>
  <ResponseMetadata>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTargetHealth

Describes the health of the specified targets or all of your targets.

```http
POST / HTTP/1.1

Action=DescribeTargetHealth
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TargetGroupNotFound</Code>
    <Message>Target groups 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>bfe62509-77c7-4f25-aca1-a46871f3f79c</RequestId>
</ErrorResponse>
```

`400` `TargetGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeTrustStoreAssociations

Describes all resources associated with the specified trust store.

```http
POST / HTTP/1.1

Action=DescribeTrustStoreAssociations
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>21357f99-2a59-49a6-98de-6a6c00de4ca5</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeTrustStoreRevocations

Describes the revocation files in use by the specified trust store or revocation files.

```http
POST / HTTP/1.1

Action=DescribeTrustStoreRevocations
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeTrustStoreRevocations is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>900fc3b8-e045-4f2d-b238-1407e77a2332</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeTrustStores

Describes all trust stores for the specified account.

```http
POST / HTTP/1.1

Action=DescribeTrustStores
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeTrustStoresResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeTrustStoresResult>
    <TrustStores/>
  </DescribeTrustStoresResult>
  <ResponseMetadata>
    <RequestId>5287d6d5-c4d5-4e8d-88b2-27567c235da3</RequestId>
  </ResponseMetadata>
</DescribeTrustStoresResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetResourcePolicy

Retrieves the resource policy for a specified resource.

```http
POST / HTTP/1.1

Action=GetResourcePolicy
&Version=2015-12-01
&ResourceArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>The specified resource type is not supported</Message>
  </Error>
  <RequestId>c11c21f9-ae1b-4f4b-b74f-8151c6e64b07</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetTrustStoreCaCertificatesBundle

Retrieves the ca certificate bundle.

```http
POST / HTTP/1.1

Action=GetTrustStoreCaCertificatesBundle
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation GetTrustStoreCaCertificatesBundle is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>9c157dba-c16c-4f9e-945d-0cff1b39a865</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetTrustStoreRevocationContent

Retrieves the specified revocation file.

```http
POST / HTTP/1.1

Action=GetTrustStoreRevocationContent
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&RevocationId=1
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation GetTrustStoreRevocationContent is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>17206e2b-2da0-4cff-b08e-1c706abc7642</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyCapacityReservation

Modifies the capacity reservation of the specified load balancer.

```http
POST / HTTP/1.1

Action=ModifyCapacityReservation
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidConfigurationRequest</Code>
    <Message>You must specify either MinimumLoadBalancerCapacity or ResetCapacityReservation</Message>
  </Error>
  <RequestId>32ba4a18-60ad-42fa-90a8-e5bc4c214447</RequestId>
</ErrorResponse>
```

`400` `InvalidConfigurationRequest`.

### ModifyIpPools

[Application Load Balancers] Modify the IP pool associated to a load balancer.

```http
POST / HTTP/1.1

Action=ModifyIpPools
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>Load balancer 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>73c4844d-969a-4666-8b65-4f09c336ea36</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyListener

Replaces the specified properties of the specified listener.

```http
POST / HTTP/1.1

Action=ModifyListener
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' is not a valid listener ARN</Message>
  </Error>
  <RequestId>d5ec765b-ace3-4e7b-a616-582a561f27d5</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyListenerAttributes

Modifies the specified attributes of the specified listener.

```http
POST / HTTP/1.1

Action=ModifyListenerAttributes
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at 'attributes' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>2a0bd339-7000-4f75-bab5-ffea4ac2ddbb</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyLoadBalancerAttributes

Modifies the specified attributes of the specified Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1

Action=ModifyLoadBalancerAttributes
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at 'attributes' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>c4f51d8d-2435-4947-8d42-93369a7db181</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyRule

Replaces the specified properties of the specified rule.

```http
POST / HTTP/1.1

Action=ModifyRule
&Version=2015-12-01
&RuleArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>RuleNotFound</Code>
    <Message>Rule 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>971364b6-5ff8-45ba-84f9-088005dab88e</RequestId>
</ErrorResponse>
```

`400` `RuleNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyTargetGroup

Modifies the health checks used when evaluating the health state of the targets in the specified target group.

```http
POST / HTTP/1.1

Action=ModifyTargetGroup
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TargetGroupNotFound</Code>
    <Message>Target groups 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>09c761e7-4671-4e6c-8f7d-963567ce96e0</RequestId>
</ErrorResponse>
```

`400` `TargetGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyTargetGroupAttributes

Modifies the specified attributes of the specified target group.

```http
POST / HTTP/1.1

Action=ModifyTargetGroupAttributes
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at 'attributes' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>ff51c7f1-b230-495c-96b8-cc49d13c64cc</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyTrustStore

Update the ca certificate bundle for the specified trust store.

```http
POST / HTTP/1.1

Action=ModifyTrustStore
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&CaCertificatesBundleS3Bucket=example
&CaCertificatesBundleS3Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>70d65843-6c27-48b2-b53f-809a7f0fc1d0</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RegisterTargets

Registers the specified targets with the specified target group.

```http
POST / HTTP/1.1

Action=RegisterTargets
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Targets.member.1.Id=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TargetGroupNotFound</Code>
    <Message>Target groups 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>2147ff6f-8ab5-47bf-b6b4-32581a38990f</RequestId>
</ErrorResponse>
```

`400` `TargetGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveListenerCertificates

Removes the specified certificate from the certificate list for the specified HTTPS or TLS listener.

```http
POST / HTTP/1.1

Action=RemoveListenerCertificates
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at 'certificates' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>17e6bf0f-e4da-4d32-83bd-fbca8ac8596b</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### RemoveTags

Removes the specified tags from the specified Elastic Load Balancing resources.

```http
POST / HTTP/1.1

Action=RemoveTags
&Version=2015-12-01
&ResourceArns.member.1=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>'example' in ResourceArns is not the ARN of an Elastic Load Balancing resource</Message>
  </Error>
  <RequestId>d51a400a-c4a9-425b-97c2-19f4a26a807d</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### RemoveTrustStoreRevocations

Removes the specified revocation file from the specified trust store.

```http
POST / HTTP/1.1

Action=RemoveTrustStoreRevocations
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&RevocationIds.member.1=1
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation RemoveTrustStoreRevocations is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>39fb585c-ef1b-4ee3-be7f-f40491320f05</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### SetIpAddressType

Sets the type of IP addresses used by the subnets of the specified load balancer.

```http
POST / HTTP/1.1

Action=SetIpAddressType
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&IpAddressType=ipv4
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>Load balancer 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>771197f0-7b05-4f86-913c-d1c5ac6d29f8</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SetRulePriorities

Sets the priorities of the specified rules.

```http
POST / HTTP/1.1

Action=SetRulePriorities
&Version=2015-12-01
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at 'rulePriorities' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>ee3a742f-41ea-44c3-b704-db0d9e8c8583</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SetSecurityGroups

Associates the specified security groups with the specified Application Load Balancer or Network Load Balancer.

```http
POST / HTTP/1.1

Action=SetSecurityGroups
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&SecurityGroups.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>LoadBalancerNotFound</Code>
    <Message>Load balancer 'arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example' not found</Message>
  </Error>
  <RequestId>14b863c3-eb99-48c1-b04d-daf633cb5191</RequestId>
</ErrorResponse>
```

`400` `LoadBalancerNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SetSubnets

Enables the Availability Zones for the specified public subnets for the specified Application Load Balancer, Network Load Balancer or Gateway Load Balancer.

```http
POST / HTTP/1.1

Action=SetSubnets
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidConfigurationRequest</Code>
    <Message>You must specify either Subnets or SubnetMappings</Message>
  </Error>
  <RequestId>860a5fe7-98a2-4a6a-aa36-2af1ec7302f3</RequestId>
</ErrorResponse>
```

`400` `InvalidConfigurationRequest`.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/elasticloadbalancing/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with a form-encoded body: `Action` names the operation, `Version` the API version, and every input member is a flattened key such as `Tags.member.1.Key`.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Elastic Load Balancing v2 (Application, Network and Gateway) answers in AWS: XML wrapped in `<OperationResponse><OperationResult>`. Errors captured on this page are an XML document rooted at `<ErrorResponse>`.

### Local endpoints

Where AWS returns a regional hostname, the emulator returns a local address: the load balancer's DNS name is `127.0.0.1:<port>`, a port the emulator opened for that resource. Those addresses are local to the emulator; the [Docker setup](README.md#running-the-emulator-in-docker) publishes only port 9297, so from your machine only the API endpoint is reachable.
