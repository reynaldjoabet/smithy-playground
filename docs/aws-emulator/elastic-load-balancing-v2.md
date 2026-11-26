# Elastic Load Balancing v2 (Application, Network and Gateway)

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `Elastic Load Balancing v2`
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
X-Amz-Date: 20261126T183321Z
Content-Length: 187
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Thu, 26 Nov 2026 18:33:21 GMT

<CreateLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <CreateLoadBalancerResult>
    <LoadBalancers>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <LoadBalancerArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:loadbalancer/app/docs-demo-alb/a3fa9733f0c6283b</LoadBalancerArn>
        <DNSName>127.0.0.1:34101</DNSName>
        <CanonicalHostedZoneId>Z35SXDOTRQ7X7K</CanonicalHostedZoneId>
        <CreatedTime>2026-11-26T18:33:21.918857Z</CreatedTime>
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
            <LoadBalancerAddresses></LoadBalancerAddresses>
          </member>
          <member>
            <ZoneName>us-east-1b</ZoneName>
            <SubnetId>subnet-00000000000000009</SubnetId>
            <LoadBalancerAddresses></LoadBalancerAddresses>
          </member>
        </AvailabilityZones>
        <SecurityGroups>
          <member>sg-00000000000000013</member>
        </SecurityGroups>
        <IpAddressType>ipv4</IpAddressType>
        <EnablePrefixForIpv6SourceNat>off</EnablePrefixForIpv6SourceNat>
      </member>
    </LoadBalancers>
  </CreateLoadBalancerResult>
  <ResponseMetadata>
    <RequestId>5fec2c36-81e7-48af-b103-2e1c21c7ed26</RequestId>
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
X-Amz-Date: 20261126T183321Z
Content-Length: 151
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Thu, 26 Nov 2026 18:33:21 GMT

<CreateTargetGroupResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <CreateTargetGroupResult>
    <TargetGroups>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <TargetGroupArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:targetgroup/docs-demo-tg/ce012da1bf2575f3</TargetGroupArn>
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
        <LoadBalancerArns></LoadBalancerArns>
        <TargetType>ip</TargetType>
        <ProtocolVersion>HTTP1</ProtocolVersion>
        <IpAddressType>ipv4</IpAddressType>
      </member>
    </TargetGroups>
  </CreateTargetGroupResult>
  <ResponseMetadata>
    <RequestId>0aea837f-4c33-4b37-afcb-c9750ebfda13</RequestId>
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
X-Amz-Date: 20261126T183321Z
Content-Length: 378
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateListener
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Aloadbalancer%2Fapp%2Fdocs-demo-alb%2Fa3fa9733f0c6283b
&Protocol=HTTP
&Port=80
&DefaultActions.member.1.Type=forward
&DefaultActions.member.1.TargetGroupArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Atargetgroup%2Fdocs-demo-tg%2Fce012da1bf2575f3
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1204
date: Thu, 26 Nov 2026 18:33:21 GMT

<CreateListenerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <CreateListenerResult>
    <Listeners>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <ListenerArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:listener/app/docs-demo-alb/a3fa9733f0c6283b/6d7b86cc11152c4f</ListenerArn>
        <LoadBalancerArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:loadbalancer/app/docs-demo-alb/a3fa9733f0c6283b</LoadBalancerArn>
        <Port>80</Port>
        <Protocol>HTTP</Protocol>
        <Certificates></Certificates>
        <DefaultActions>
          <member>
            <Type>forward</Type>
            <TargetGroupArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:targetgroup/docs-demo-tg/ce012da1bf2575f3</TargetGroupArn>
            <ForwardConfig>
              <TargetGroups>
                <member>
                  <TargetGroupArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:targetgroup/docs-demo-tg/ce012da1bf2575f3</TargetGroupArn>
                  <Weight>1</Weight>
                </member>
              </TargetGroups>
              <TargetGroupStickinessConfig>
                <Enabled>false</Enabled>
              </TargetGroupStickinessConfig>
            </ForwardConfig>
          </member>
        </DefaultActions>
      </member>
    </Listeners>
  </CreateListenerResult>
  <ResponseMetadata>
    <RequestId>32ad681e-7c5a-4ab9-8454-1bf0f9c076f3</RequestId>
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
X-Amz-Date: 20261126T183322Z
Content-Length: 189
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancers
&Version=2015-12-01
&LoadBalancerArns.member.1=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Aloadbalancer%2Fapp%2Fdocs-demo-alb%2Fa3fa9733f0c6283b
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1345
date: Thu, 26 Nov 2026 18:33:22 GMT

<DescribeLoadBalancersResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeLoadBalancersResult>
    <LoadBalancers>
      <member xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
        <LoadBalancerArn>arn:aws:elasticloadbalancing:us-east-1:000000000000:loadbalancer/app/docs-demo-alb/a3fa9733f0c6283b</LoadBalancerArn>
        <DNSName>127.0.0.1:34101</DNSName>
        <CanonicalHostedZoneId>Z35SXDOTRQ7X7K</CanonicalHostedZoneId>
        <CreatedTime>2026-11-26T18:33:21.918857Z</CreatedTime>
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
            <LoadBalancerAddresses></LoadBalancerAddresses>
          </member>
          <member>
            <ZoneName>us-east-1b</ZoneName>
            <SubnetId>subnet-00000000000000009</SubnetId>
            <LoadBalancerAddresses></LoadBalancerAddresses>
          </member>
        </AvailabilityZones>
        <SecurityGroups>
          <member>sg-00000000000000013</member>
        </SecurityGroups>
        <IpAddressType>ipv4</IpAddressType>
        <EnablePrefixForIpv6SourceNat>off</EnablePrefixForIpv6SourceNat>
      </member>
    </LoadBalancers>
  </DescribeLoadBalancersResult>
  <ResponseMetadata>
    <RequestId>3f5c0069-fc62-4a85-a84d-970a5749d07d</RequestId>
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
X-Amz-Date: 20261126T183322Z
Content-Length: 166
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTargetGroup
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Atargetgroup%2Fdocs-demo-tg%2Fce012da1bf2575f3
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 5f017b16-39b3-4f1e-96f8-277f477a60a2
content-length: 397
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ResourceInUse</Code>
    <Message>Target group &apos;arn:aws:elasticloadbalancing:us-east-1:000000000000:targetgroup/docs-demo-tg/ce012da1bf2575f3&apos; is currently in use by a listener or a rule</Message>
  </Error>
  <RequestId>5f017b16-39b3-4f1e-96f8-277f477a60a2</RequestId>
</ErrorResponse>
```

### 6. DeleteListener

Remove the listener ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 183
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteListener
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Alistener%2Fapp%2Fdocs-demo-alb%2Fa3fa9733f0c6283b%2F6d7b86cc11152c4f
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 256
date: Thu, 26 Nov 2026 18:33:22 GMT

<DeleteListenerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DeleteListenerResult></DeleteListenerResult>
  <ResponseMetadata>
    <RequestId>fdff6d80-fab0-4c63-83f1-b0887e313295</RequestId>
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
X-Amz-Date: 20261126T183322Z
Content-Length: 166
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTargetGroup
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Atargetgroup%2Fdocs-demo-tg%2Fce012da1bf2575f3
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 268
date: Thu, 26 Nov 2026 18:33:22 GMT

<DeleteTargetGroupResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DeleteTargetGroupResult></DeleteTargetGroupResult>
  <ResponseMetadata>
    <RequestId>728cca51-2421-431a-9486-f95336cd2d22</RequestId>
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
X-Amz-Date: 20261126T183322Z
Content-Length: 176
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLoadBalancer
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancing%3Aus-east-1%3A000000000000%3Aloadbalancer%2Fapp%2Fdocs-demo-alb%2Fa3fa9733f0c6283b
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 272
date: Thu, 26 Nov 2026 18:33:22 GMT

<DeleteLoadBalancerResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DeleteLoadBalancerResult></DeleteLoadBalancerResult>
  <ResponseMetadata>
    <RequestId>02c7eb82-030d-4807-920d-7ad98bda266b</RequestId>
  </ResponseMetadata>
</DeleteLoadBalancerResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 51 operations, 6 accepted the placeholder call, 0 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.629* answered `501` in 1.0.595. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AddListenerCertificates

Adds the specified SSL server certificate to the certificate list for the specified HTTPS or TLS listener.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 143
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddListenerCertificates
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 5e07c7b4-55b2-42d9-ade8-a4d1b017f86e
content-length: 313
date: Thu, 26 Nov 2026 18:33:22 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at &apos;certificates&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>5e07c7b4-55b2-42d9-ade8-a4d1b017f86e</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AddTags

Adds the specified tags to the specified Elastic Load Balancing resource.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 89
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddTags
&Version=2015-12-01
&ResourceArns.member.1=example
&Tags.member.1.Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 868fef09-031a-4fe3-b440-f12208e383af
content-length: 325
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;example&apos; in ResourceArns is not the ARN of an Elastic Load Balancing resource</Message>
  </Error>
  <RequestId>868fef09-031a-4fe3-b440-f12208e383af</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AddTrustStoreRevocations

Adds the specified revocation file to the specified trust store.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 146
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddTrustStoreRevocations
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 3cb8ed5f-2e83-4508-917c-8bacba28c785
content-length: 347
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>3cb8ed5f-2e83-4508-917c-8bacba28c785</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateListener

Creates a listener for the specified Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 175
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateListener
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&DefaultActions.member.1.Type=forward
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2a0d0bb2-ed96-4ead-8069-544985a0ded2
content-length: 351
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid load balancer ARN</Message>
  </Error>
  <RequestId>2a0d0bb2-ed96-4ead-8069-544985a0ded2</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-createlistener) for a successful call.

### CreateLoadBalancer

Creates an Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 57
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLoadBalancer
&Version=2015-12-01
&Name=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 70792bfc-8e7e-4f8a-885e-8d6eba98820c
content-length: 298
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidConfigurationRequest</Code>
    <Message>You must specify either Subnets or SubnetMappings</Message>
  </Error>
  <RequestId>70792bfc-8e7e-4f8a-885e-8d6eba98820c</RequestId>
</ErrorResponse>
```

`400` `InvalidConfigurationRequest`. See [the walkthrough](#1-createloadbalancer) for a successful call.

### CreateRule

Creates a rule for the specified listener.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 171
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateRule
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Priority=1
&Actions.member.1.Type=forward
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: fb796376-c11b-4514-aae0-384d9a57d580
content-length: 311
date: Thu, 26 Nov 2026 18:33:22 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at &apos;conditions&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>fb796376-c11b-4514-aae0-384d9a57d580</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateTargetGroup

Creates a target group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 56
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateTargetGroup
&Version=2015-12-01
&Name=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 553b8f72-ac15-4c36-b847-8f86d0cd7a05
content-length: 263
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>A VPC ID must be specified</Message>
  </Error>
  <RequestId>553b8f72-ac15-4c36-b847-8f86d0cd7a05</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createtargetgroup) for a successful call.

### CreateTrustStore

Creates a trust store.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 126
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateTrustStore
&Version=2015-12-01
&Name=example
&CaCertificatesBundleS3Bucket=example
&CaCertificatesBundleS3Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 6e2e5f5a-d815-4fb7-b8c2-dba31f95cca7
content-length: 294
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>CaCertificatesBundleNotFound</Code>
    <Message>Provided CA certificate bundle was not found</Message>
  </Error>
  <RequestId>6e2e5f5a-d815-4fb7-b8c2-dba31f95cca7</RequestId>
</ErrorResponse>
```

`400` `CaCertificatesBundleNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteListener

Deletes the specified listener.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 134
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteListener
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e4a5a600-f604-4c8f-981d-6a4e9dc5f13f
content-length: 346
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid listener ARN</Message>
  </Error>
  <RequestId>e4a5a600-f604-4c8f-981d-6a4e9dc5f13f</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#6-deletelistener) for a successful call.

### DeleteLoadBalancer

Deletes the specified Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 142
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLoadBalancer
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 789c30d2-c5d9-4112-91b6-ebe340eed6f8
content-length: 351
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid load balancer ARN</Message>
  </Error>
  <RequestId>789c30d2-c5d9-4112-91b6-ebe340eed6f8</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#8-deleteloadbalancer) for a successful call.

### DeleteRule

Deletes the specified rule.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 126
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteRule
&Version=2015-12-01
&RuleArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 404a61c4-5083-4560-bd77-82163e2cb9dc
content-length: 330
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>RuleNotFound</Code>
    <Message>Rule &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>404a61c4-5083-4560-bd77-82163e2cb9dc</RequestId>
</ErrorResponse>
```

`400` `RuleNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSharedTrustStoreAssociation

Deletes a shared trust store association.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 249
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteSharedTrustStoreAssociation
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ResourceArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 260e0bbf-63d2-498e-b3a9-7f502c4d0001
content-length: 347
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>260e0bbf-63d2-498e-b3a9-7f502c4d0001</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTargetGroup

Deletes the specified target group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 140
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTargetGroup
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 83641e82-676a-446c-a55e-60e52468edf4
content-length: 350
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid target group ARN</Message>
  </Error>
  <RequestId>83641e82-676a-446c-a55e-60e52468edf4</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#7-deletetargetgroup) for a successful call.

### DeleteTrustStore

Deletes a trust store.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 138
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTrustStore
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9a3551f7-1c8a-4943-b26d-4c2adbabdb01
content-length: 347
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>9a3551f7-1c8a-4943-b26d-4c2adbabdb01</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeregisterTargets

Deregisters the specified targets from the specified target group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 168
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeregisterTargets
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Targets.member.1.Id=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 827f712f-6b25-49cd-ba5a-232d65847e66
content-length: 350
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid target group ARN</Message>
  </Error>
  <RequestId>827f712f-6b25-49cd-ba5a-232d65847e66</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeAccountLimits

Describes the current Elastic Load Balancing resource limits for your Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAccountLimits
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 2966
date: Thu, 26 Nov 2026 18:33:22 GMT

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
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 151
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCapacityReservation
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 761d4cc6-743a-4a78-9cea-9e0125a79c47
content-length: 351
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid load balancer ARN</Message>
  </Error>
  <RequestId>761d4cc6-743a-4a78-9cea-9e0125a79c47</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeListenerAttributes

Describes the attributes for the specified listener.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 146
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeListenerAttributes
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 84f5581b-11fa-4928-8fc7-8892dbd560e7
content-length: 346
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid listener ARN</Message>
  </Error>
  <RequestId>84f5581b-11fa-4928-8fc7-8892dbd560e7</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeListenerCertificates

Describes the default certificate and the certificate list for the specified HTTPS or TLS listener.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 148
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeListenerCertificates
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 761f96c5-9b1b-417b-8f2f-caea236ae65c
content-length: 346
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid listener ARN</Message>
  </Error>
  <RequestId>761f96c5-9b1b-417b-8f2f-caea236ae65c</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeListeners

Describes the specified listeners or the listeners for the specified Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeListeners
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 291
date: Thu, 26 Nov 2026 18:33:22 GMT

<DescribeListenersResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeListenersResult>
    <Listeners></Listeners>
  </DescribeListenersResult>
  <ResponseMetadata>
    <RequestId>10ed596c-902d-491e-859c-238bf0ae4eb8</RequestId>
  </ResponseMetadata>
</DescribeListenersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLoadBalancerAttributes

Describes the attributes for the specified Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 154
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancerAttributes
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 55f62739-9173-4039-a44e-1182fe8406ec
content-length: 340
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; must be in ARN format</Message>
  </Error>
  <RequestId>55f62739-9173-4039-a44e-1182fe8406ec</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeLoadBalancers

Describes the specified load balancers or all of your load balancers.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancers
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 315
date: Thu, 26 Nov 2026 18:33:22 GMT

<DescribeLoadBalancersResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeLoadBalancersResult>
    <LoadBalancers></LoadBalancers>
  </DescribeLoadBalancersResult>
  <ResponseMetadata>
    <RequestId>a3a05add-7880-4227-a0f1-fbbc254d0d89</RequestId>
  </ResponseMetadata>
</DescribeLoadBalancersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-describeloadbalancers) for a successful call.

### DescribeRules

Describes the specified rules or the rules for the specified listener.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 39
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeRules
&Version=2015-12-01
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 0b1e281c-e2c1-4f14-828a-6fa6933b7aae
content-length: 297
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>You must specify either listener rule ARNs or a listener ARN</Message>
  </Error>
  <RequestId>0b1e281c-e2c1-4f14-828a-6fa6933b7aae</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeSSLPolicies

Describes the specified policies or all policies used for SSL negotiation.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeSSLPolicies
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 56373
date: Thu, 26 Nov 2026 18:33:22 GMT

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
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTags
&Version=2015-12-01
&ResourceArns.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 60dd4289-f8d6-412e-b831-dfb78b184e9c
content-length: 325
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;example&apos; in ResourceArns is not the ARN of an Elastic Load Balancing resource</Message>
  </Error>
  <RequestId>60dd4289-f8d6-412e-b831-dfb78b184e9c</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeTargetGroupAttributes

Describes the attributes for the specified target group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 152
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTargetGroupAttributes
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 5c01382d-3de7-4bac-805f-f6f992eaa6b9
content-length: 340
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; must be in ARN format</Message>
  </Error>
  <RequestId>5c01382d-3de7-4bac-805f-f6f992eaa6b9</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeTargetGroups

Describes the specified target groups or all of your target groups.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTargetGroups
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 309
date: Thu, 26 Nov 2026 18:33:22 GMT

<DescribeTargetGroupsResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeTargetGroupsResult>
    <TargetGroups></TargetGroups>
  </DescribeTargetGroupsResult>
  <ResponseMetadata>
    <RequestId>f0423b5d-552f-4206-bedc-04aa86640fea</RequestId>
  </ResponseMetadata>
</DescribeTargetGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTargetHealth

Describes the health of the specified targets or all of your targets.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 143
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTargetHealth
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 20284131-c0f0-40cc-9991-975c8f5c7d00
content-length: 350
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid target group ARN</Message>
  </Error>
  <RequestId>20284131-c0f0-40cc-9991-975c8f5c7d00</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeTrustStoreAssociations

Describes all resources associated with the specified trust store.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 152
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTrustStoreAssociations
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 34858f10-41b4-49ee-a36e-05ec7df07925
content-length: 347
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>34858f10-41b4-49ee-a36e-05ec7df07925</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeTrustStoreRevocations

Describes the revocation files in use by the specified trust store or revocation files.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 151
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTrustStoreRevocations
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 44932e8e-e0de-49d4-9d65-019046ec93e9
content-length: 347
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>44932e8e-e0de-49d4-9d65-019046ec93e9</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeTrustStores

Describes all trust stores for the specified account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTrustStores
&Version=2015-12-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 303
date: Thu, 26 Nov 2026 18:33:22 GMT

<DescribeTrustStoresResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <DescribeTrustStoresResult>
    <TrustStores></TrustStores>
  </DescribeTrustStoresResult>
  <ResponseMetadata>
    <RequestId>79c734a5-923b-4b0a-96c2-d924931721ac</RequestId>
  </ResponseMetadata>
</DescribeTrustStoresResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetResourcePolicy

Retrieves the resource policy for a specified resource.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 137
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetResourcePolicy
&Version=2015-12-01
&ResourceArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9bf6fbec-c3cd-491a-af19-ba01f143cd8f
content-length: 281
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>The specified resource type is not supported</Message>
  </Error>
  <RequestId>9bf6fbec-c3cd-491a-af19-ba01f143cd8f</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetTrustStoreCaCertificatesBundle

Retrieves the ca certificate bundle.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 155
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetTrustStoreCaCertificatesBundle
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2a800675-386b-4732-b85f-8e0f95478093
content-length: 347
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>2a800675-386b-4732-b85f-8e0f95478093</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTrustStoreRevocationContent

Retrieves the specified revocation file.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 167
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetTrustStoreRevocationContent
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&RevocationId=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 373dc517-310b-4481-90da-d4ae4d8eca8a
content-length: 347
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>373dc517-310b-4481-90da-d4ae4d8eca8a</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyCapacityReservation

Modifies the capacity reservation of the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 149
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyCapacityReservation
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 8010901e-74d7-4a63-9deb-a779ffbc3e68
content-length: 351
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid load balancer ARN</Message>
  </Error>
  <RequestId>8010901e-74d7-4a63-9deb-a779ffbc3e68</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyIpPools

[Application Load Balancers] Modify the IP pool associated to a load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 137
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyIpPools
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: bb2f6167-0053-4a61-8a71-bb16aa048df0
content-length: 351
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid load balancer ARN</Message>
  </Error>
  <RequestId>bb2f6167-0053-4a61-8a71-bb16aa048df0</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyListener

Replaces the specified properties of the specified listener.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 134
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyListener
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 114785f0-e845-468d-81e9-362027b62df1
content-length: 346
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid listener ARN</Message>
  </Error>
  <RequestId>114785f0-e845-468d-81e9-362027b62df1</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyListenerAttributes

Modifies the specified attributes of the specified listener.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 144
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyListenerAttributes
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c3a05463-4831-47e7-b0db-8358618669bb
content-length: 311
date: Thu, 26 Nov 2026 18:33:22 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at &apos;attributes&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>c3a05463-4831-47e7-b0db-8358618669bb</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyLoadBalancerAttributes

Modifies the specified attributes of the specified Application Load Balancer, Network Load Balancer, or Gateway Load Balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 152
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyLoadBalancerAttributes
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: fad209b9-0be1-4826-81c2-0ebd3262d819
content-length: 311
date: Thu, 26 Nov 2026 18:33:22 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at &apos;attributes&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>fad209b9-0be1-4826-81c2-0ebd3262d819</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyRule

Replaces the specified properties of the specified rule.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 126
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyRule
&Version=2015-12-01
&RuleArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 5e14d044-f059-41f2-a3dc-4c7ed3562856
content-length: 330
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>RuleNotFound</Code>
    <Message>Rule &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>5e14d044-f059-41f2-a3dc-4c7ed3562856</RequestId>
</ErrorResponse>
```

`400` `RuleNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyTargetGroup

Modifies the health checks used when evaluating the health state of the targets in the specified target group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 140
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyTargetGroup
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f5c69281-8c31-4e64-9023-55b27da65850
content-length: 350
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid target group ARN</Message>
  </Error>
  <RequestId>f5c69281-8c31-4e64-9023-55b27da65850</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyTargetGroupAttributes

Modifies the specified attributes of the specified target group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 150
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyTargetGroupAttributes
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 707d48eb-4a51-4329-8d34-995389a8d7e3
content-length: 311
date: Thu, 26 Nov 2026 18:33:22 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at &apos;attributes&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>707d48eb-4a51-4329-8d34-995389a8d7e3</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ModifyTrustStore

Update the ca certificate bundle for the specified trust store.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 209
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyTrustStore
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&CaCertificatesBundleS3Bucket=example
&CaCertificatesBundleS3Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 6658cce9-f34c-4d60-8865-b91dbdf4aae0
content-length: 347
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>6658cce9-f34c-4d60-8865-b91dbdf4aae0</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RegisterTargets

Registers the specified targets with the specified target group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 166
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RegisterTargets
&Version=2015-12-01
&TargetGroupArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Targets.member.1.Id=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 978aa09f-4721-4af4-abc6-5374f41ecc27
content-length: 350
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid target group ARN</Message>
  </Error>
  <RequestId>978aa09f-4721-4af4-abc6-5374f41ecc27</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### RemoveListenerCertificates

Removes the specified certificate from the certificate list for the specified HTTPS or TLS listener.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 146
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveListenerCertificates
&Version=2015-12-01
&ListenerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9ba54d00-5c7f-466f-9644-89e421a941b3
content-length: 313
date: Thu, 26 Nov 2026 18:33:22 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at &apos;certificates&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>9ba54d00-5c7f-466f-9644-89e421a941b3</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### RemoveTags

Removes the specified tags from the specified Elastic Load Balancing resources.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveTags
&Version=2015-12-01
&ResourceArns.member.1=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2e6dcd50-523e-4a80-8291-939b1c80a098
content-length: 325
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;example&apos; in ResourceArns is not the ARN of an Elastic Load Balancing resource</Message>
  </Error>
  <RequestId>2e6dcd50-523e-4a80-8291-939b1c80a098</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### RemoveTrustStoreRevocations

Removes the specified revocation file from the specified trust store.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 174
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveTrustStoreRevocations
&Version=2015-12-01
&TrustStoreArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&RevocationIds.member.1=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: af8a5ffc-f1d6-43ab-a60a-2db064d341f7
content-length: 347
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>TrustStoreNotFound</Code>
    <Message>Trust store ARN &apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; not found</Message>
  </Error>
  <RequestId>af8a5ffc-f1d6-43ab-a60a-2db064d341f7</RequestId>
</ErrorResponse>
```

`400` `TrustStoreNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SetIpAddressType

Sets the type of IP addresses used by the subnets of the specified load balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 159
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetIpAddressType
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&IpAddressType=ipv4
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: bba6c584-0a79-4872-a00f-4f3b2badd1ca
content-length: 351
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid load balancer ARN</Message>
  </Error>
  <RequestId>bba6c584-0a79-4872-a00f-4f3b2badd1ca</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SetRulePriorities

Sets the priorities of the specified rules.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetRulePriorities
&Version=2015-12-01
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c9c44e71-829d-488c-9f32-9000d8fd2c45
content-length: 315
date: Thu, 26 Nov 2026 18:33:22 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>1 validation error detected: Value null at &apos;rulePriorities&apos; failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>c9c44e71-829d-488c-9f32-9000d8fd2c45</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SetSecurityGroups

Associates the specified security groups with the specified Application Load Balancer or Network Load Balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 173
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetSecurityGroups
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&SecurityGroups.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 03e3b531-cf8c-4390-a09d-b70917dc52ab
content-length: 351
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid load balancer ARN</Message>
  </Error>
  <RequestId>03e3b531-cf8c-4390-a09d-b70917dc52ab</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SetSubnets

Enables the Availability Zones for the specified public subnets for the specified Application Load Balancer, Network Load Balancer or Gateway Load Balancer.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183322Z
Content-Length: 134
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticloadbalancing/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetSubnets
&Version=2015-12-01
&LoadBalancerArn=arn%3Aaws%3Aelasticloadbalancingv2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 5ff86b7b-f873-4c3e-9c83-f953d461ec1f
content-length: 351
date: Thu, 26 Nov 2026 18:33:22 GMT

<ErrorResponse xmlns="http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;arn:aws:elasticloadbalancingv2:us-east-1:000000000000:example/example&apos; is not a valid load balancer ARN</Message>
  </Error>
  <RequestId>5ff86b7b-f873-4c3e-9c83-f953d461ec1f</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

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
