# Auto Scaling

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `Auto Scaling`
- **sigv4 signing name:** `autoscaling`
- **Endpoint prefix:** `autoscaling`
- **ARN namespace:** `autoscaling`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `autoscaling`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `DescribeAutoScalingGroups`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:autoscaling" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded; charset=utf-8' \
  --data-binary 'Action=DescribeAutoScalingGroups&Version=2011-01-01' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create an EC2 launch template and an Auto Scaling group from it, scale the group, then delete both. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateLaunchConfiguration

Launch configurations are refused, as they are for new AWS accounts: Auto Scaling groups are built from EC2 launch templates instead.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 140
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLaunchConfiguration
&Version=2011-01-01
&LaunchConfigurationName=docs-demo-lc
&ImageId=ami-0000000000000003c
&InstanceType=t3.micro
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 199918dd-e6b7-4d76-909b-635f36def8d0
content-length: 395
date: Thu, 26 Nov 2026 18:27:32 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>UnsupportedOperation</Code>
    <Message>The Launch Configuration creation operation is not available in your account. Use launch templates to create configuration templates for your Auto Scaling groups.</Message>
  </Error>
  <RequestId>199918dd-e6b7-4d76-909b-635f36def8d0</RequestId>
</ErrorResponse>
```

### 2. Amazon Elastic Compute Cloud: CreateLaunchTemplate

The launch template lives in EC2.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 217
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLaunchTemplate
&Version=2016-11-15
&ClientToken=7e8bd66e-2213-4817-9cdb-464cd474f9f2
&LaunchTemplateName=docs-demo-lt
&LaunchTemplateData.ImageId=ami-0000000000000003c
&LaunchTemplateData.InstanceType=t3.micro
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 625
date: Thu, 26 Nov 2026 18:27:32 GMT

<CreateLaunchTemplateResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <launchTemplate xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <launchTemplateId>lt-000000000000000a7</launchTemplateId>
    <launchTemplateName>docs-demo-lt</launchTemplateName>
    <createTime>2026-11-26T18:27:32.982524Z</createTime>
    <createdBy>arn:aws:iam::000000000000:user/emulator</createdBy>
    <defaultVersionNumber>1</defaultVersionNumber>
    <latestVersionNumber>1</latestVersionNumber>
    <tagSet></tagSet>
    <operator>
      <managed>false</managed>
    </operator>
  </launchTemplate>
  <requestId>d16e9592-3b37-4355-8e89-260a639bbedf</requestId>
</CreateLaunchTemplateResponse>
```

### 3. CreateAutoScalingGroup

Subnets are passed as one comma-separated string; success is an empty result.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 277
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=docs-demo-asg
&LaunchTemplate.LaunchTemplateId=lt-000000000000000a7
&LaunchTemplate.Version=%24Latest
&MinSize=0
&MaxSize=2
&DesiredCapacity=0
&VPCZoneIdentifier=subnet-00000000000000007%2Csubnet-00000000000000009
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 279
date: Thu, 26 Nov 2026 18:27:32 GMT

<CreateAutoScalingGroupResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <CreateAutoScalingGroupResult></CreateAutoScalingGroupResult>
  <ResponseMetadata>
    <RequestId>e1639565-3102-4414-a49e-0a0deaa6e367</RequestId>
  </ResponseMetadata>
</CreateAutoScalingGroupResponse>
```

### 4. SetDesiredCapacity

The group launches instances to reach the desired capacity.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 97
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetDesiredCapacity
&Version=2011-01-01
&AutoScalingGroupName=docs-demo-asg
&DesiredCapacity=1
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 263
date: Thu, 26 Nov 2026 18:27:32 GMT

<SetDesiredCapacityResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <SetDesiredCapacityResult></SetDesiredCapacityResult>
  <ResponseMetadata>
    <RequestId>26cc2823-ea25-4b6e-8df1-62c80b309c60</RequestId>
  </ResponseMetadata>
</SetDesiredCapacityResponse>
```

### 5. DescribeAutoScalingGroups

The group's sizes and the instances it holds.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAutoScalingGroups
&Version=2011-01-01
&AutoScalingGroupNames.member.1=docs-demo-asg
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 2788
date: Thu, 26 Nov 2026 18:27:32 GMT

<DescribeAutoScalingGroupsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAutoScalingGroupsResult>
    <AutoScalingGroups>
      <member xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
        <AutoScalingGroupName>docs-demo-asg</AutoScalingGroupName>
        <AutoScalingGroupARN>arn:aws:autoscaling:us-east-1:000000000000:autoScalingGroup:00000001-0000-4000-8000-000000000001:autoScalingGroupName/docs-demo-asg</AutoScalingGroupARN>
        <LaunchTemplate>
          <LaunchTemplateId>lt-000000000000000a7</LaunchTemplateId>
          <LaunchTemplateName>docs-demo-lt</LaunchTemplateName>
          <Version>$Latest</Version>
        </LaunchTemplate>
        <MinSize>0</MinSize>
        <MaxSize>2</MaxSize>
        <DesiredCapacity>1</DesiredCapacity>
        <DefaultCooldown>300</DefaultCooldown>
        <AvailabilityZones>
          <member>us-east-1a</member>
          <member>us-east-1b</member>
        </AvailabilityZones>
        <AvailabilityZoneIds>
          <member>use1-az1</member>
          <member>use1-az2</member>
        </AvailabilityZoneIds>
        <LoadBalancerNames></LoadBalancerNames>
        <TargetGroupARNs></TargetGroupARNs>
        <HealthCheckType>EC2</HealthCheckType>
        <HealthCheckGracePeriod>0</HealthCheckGracePeriod>
        <Instances>
          <member>
            <InstanceId>i-037b3ad409997f83d</InstanceId>
            <InstanceType>t3.micro</InstanceType>
            <AvailabilityZone>us-east-1a</AvailabilityZone>
            <AvailabilityZoneId>use1-az1</AvailabilityZoneId>
            <LifecycleState>Pending</LifecycleState>
            <HealthStatus>Healthy</HealthStatus>
            <LaunchTemplate>
              <LaunchTemplateId>lt-000000000000000a7</LaunchTemplateId>
              <LaunchTemplateName>docs-demo-lt</LaunchTemplateName>
              <Version>1</Version>
            </LaunchTemplate>
            <ImageId>ami-0000000000000003c</ImageId>
            <ProtectedFromScaleIn>false</ProtectedFromScaleIn>
          </member>
        </Instances>
        <CreatedTime>2026-11-26T18:27:32.984618Z</CreatedTime>
        <SuspendedProcesses></SuspendedProcesses>
        <VPCZoneIdentifier>subnet-00000000000000007,subnet-00000000000000009</VPCZoneIdentifier>
        <EnabledMetrics></EnabledMetrics>
        <Tags></Tags>
        <TerminationPolicies>
          <member>Default</member>
        </TerminationPolicies>
        <NewInstancesProtectedFromScaleIn>false</NewInstancesProtectedFromScaleIn>
        <ServiceLinkedRoleARN>arn:aws:iam::000000000000:role/aws-service-role/autoscaling.amazonaws.com/AWSServiceRoleForAutoScaling</ServiceLinkedRoleARN>
        <TrafficSources></TrafficSources>
        <AvailabilityZoneDistribution>
          <CapacityDistributionStrategy>balanced-best-effort</CapacityDistributionStrategy>
        </AvailabilityZoneDistribution>
        <CapacityReservationSpecification>
          <CapacityReservationPreference>default</CapacityReservationPreference>
… (13 more lines)
```

### 6. DeleteAutoScalingGroup

A group that still has instances is refused unless the request forces it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=docs-demo-asg
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b91f76d8-be2d-4f6c-b176-d86c1c0972e0
content-length: 345
date: Thu, 26 Nov 2026 18:27:32 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ResourceInUse</Code>
    <Message>You cannot delete an AutoScalingGroup while there are instances or pending Spot instance request(s) still in the group.</Message>
  </Error>
  <RequestId>b91f76d8-be2d-4f6c-b176-d86c1c0972e0</RequestId>
</ErrorResponse>
```

### 7. DeleteAutoScalingGroup

`ForceDelete` terminates the group's instances with it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 100
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=docs-demo-asg
&ForceDelete=true
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 279
date: Thu, 26 Nov 2026 18:27:32 GMT

<DeleteAutoScalingGroupResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DeleteAutoScalingGroupResult></DeleteAutoScalingGroupResult>
  <ResponseMetadata>
    <RequestId>d06ebb2f-35bb-402b-8a8c-b16579756543</RequestId>
  </ResponseMetadata>
</DeleteAutoScalingGroupResponse>
```

### 8. DescribeAutoScalingGroups

Poll until the group is gone.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAutoScalingGroups
&Version=2011-01-01
&AutoScalingGroupNames.member.1=docs-demo-asg
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 330
date: Thu, 26 Nov 2026 18:27:32 GMT

<DescribeAutoScalingGroupsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAutoScalingGroupsResult>
    <AutoScalingGroups></AutoScalingGroups>
  </DescribeAutoScalingGroupsResult>
  <ResponseMetadata>
    <RequestId>2b370994-39fe-428b-bb5a-6a616ef2c8c7</RequestId>
  </ResponseMetadata>
</DescribeAutoScalingGroupsResponse>
```

### 9. Amazon Elastic Compute Cloud: DeleteLaunchTemplate

Clean up the launch template.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLaunchTemplate
&Version=2016-11-15
&LaunchTemplateId=lt-000000000000000a7
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 625
date: Thu, 26 Nov 2026 18:27:32 GMT

<DeleteLaunchTemplateResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <launchTemplate xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <launchTemplateId>lt-000000000000000a7</launchTemplateId>
    <launchTemplateName>docs-demo-lt</launchTemplateName>
    <createTime>2026-11-26T18:27:32.982524Z</createTime>
    <createdBy>arn:aws:iam::000000000000:user/emulator</createdBy>
    <defaultVersionNumber>1</defaultVersionNumber>
    <latestVersionNumber>1</latestVersionNumber>
    <tagSet></tagSet>
    <operator>
      <managed>false</managed>
    </operator>
  </launchTemplate>
  <requestId>1ea168f7-01d3-4a19-b387-cfa77921c3e1</requestId>
</DeleteLaunchTemplateResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 68 operations, 16 accepted the placeholder call, 2 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AttachInstances

Attaches one or more EC2 instances to the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AttachInstances
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: af0f5a6a-e613-4497-b38e-0ad397dae300
content-length: 297
date: Thu, 26 Nov 2026 18:27:32 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup: example not found</Message>
  </Error>
  <RequestId>af0f5a6a-e613-4497-b38e-0ad397dae300</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AttachLoadBalancerTargetGroups

This API operation is superseded by AttachTrafficSources , which can attach multiple traffic sources types.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 118
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AttachLoadBalancerTargetGroups
&Version=2011-01-01
&AutoScalingGroupName=example
&TargetGroupARNs.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 32bd5666-caea-4f1e-8382-53aff0b8b863
content-length: 266
date: Thu, 26 Nov 2026 18:27:32 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>32bd5666-caea-4f1e-8382-53aff0b8b863</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AttachLoadBalancers

This API operation is superseded by AttachTrafficSources , which can attach multiple traffic sources types.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 109
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AttachLoadBalancers
&Version=2011-01-01
&AutoScalingGroupName=example
&LoadBalancerNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 38d30d7f-8fcf-4976-b455-14b1fcde2770
content-length: 266
date: Thu, 26 Nov 2026 18:27:32 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>38d30d7f-8fcf-4976-b455-14b1fcde2770</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AttachTrafficSources

Attaches one or more traffic sources to the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 118
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AttachTrafficSources
&Version=2011-01-01
&AutoScalingGroupName=example
&TrafficSources.member.1.Identifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9f44084e-9676-4419-928b-4d8c2fd08533
content-length: 266
date: Thu, 26 Nov 2026 18:27:32 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>9f44084e-9676-4419-928b-4d8c2fd08533</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### BatchDeleteScheduledAction

Deletes one or more scheduled actions for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 119
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=BatchDeleteScheduledAction
&Version=2011-01-01
&AutoScalingGroupName=example
&ScheduledActionNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 6bbd936c-3335-445d-88e2-1de1407d8c35
content-length: 296
date: Thu, 26 Nov 2026 18:27:32 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>6bbd936c-3335-445d-88e2-1de1407d8c35</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### BatchPutScheduledUpdateGroupAction

Creates or updates one or more scheduled scaling actions for an Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182732Z
Content-Length: 154
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=BatchPutScheduledUpdateGroupAction
&Version=2011-01-01
&AutoScalingGroupName=example
&ScheduledUpdateGroupActions.member.1.ScheduledActionName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 01f2950f-c8d9-4c36-956f-ebeac74988dc
content-length: 266
date: Thu, 26 Nov 2026 18:27:32 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>01f2950f-c8d9-4c36-956f-ebeac74988dc</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CancelInstanceRefresh

Cancels an instance refresh or rollback that is in progress.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CancelInstanceRefresh
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 7b640d4f-5345-4aa3-9b32-165b29a25616
content-length: 308
date: Thu, 26 Nov 2026 18:27:32 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - Auto Scaling group with name example not found</Message>
  </Error>
  <RequestId>7b640d4f-5345-4aa3-9b32-165b29a25616</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CompleteLifecycleAction

Completes the lifecycle action for the specified token or instance with the specified result.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 134
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CompleteLifecycleAction
&Version=2011-01-01
&LifecycleHookName=example
&AutoScalingGroupName=example
&LifecycleActionResult=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 93e332a8-b64d-43db-a4ab-05b8b2459f9e
content-length: 295
date: Thu, 26 Nov 2026 18:27:32 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>&apos;LifecycleActionResult&apos; must be one of: CONTINUE, ABANDON</Message>
  </Error>
  <RequestId>93e332a8-b64d-43db-a4ab-05b8b2459f9e</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateAutoScalingGroup

We strongly recommend using a launch template when calling this operation to ensure full functionality for Amazon EC2 Auto Scaling and Amazon EC2.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 97
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=example
&MinSize=1
&MaxSize=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 09c95f6c-4659-4445-862b-90a9419bae44
content-length: 349
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Valid requests must contain either LaunchTemplate, LaunchConfigurationName, InstanceId or MixedInstancesPolicy parameter.</Message>
  </Error>
  <RequestId>09c95f6c-4659-4445-862b-90a9419bae44</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-createautoscalinggroup) for a successful call.

### CreateLaunchConfiguration

Creates a launch configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateLaunchConfiguration
&Version=2011-01-01
&LaunchConfigurationName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 0a8b69c7-c0e6-4ec6-9c71-7895d600d9fe
content-length: 336
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Valid requests must contain either the InstanceID parameter or both the ImageId and InstanceType parameters.</Message>
  </Error>
  <RequestId>0a8b69c7-c0e6-4ec6-9c71-7895d600d9fe</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateOrUpdateTags

Creates or updates tags for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateOrUpdateTags
&Version=2011-01-01
&Tags.member.1.Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c35015e6-8ecd-4ae7-a7a0-bdd967967870
content-length: 266
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>c35015e6-8ecd-4ae7-a7a0-bdd967967870</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteAutoScalingGroup

Deletes the specified Auto Scaling group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: edd68fa2-491c-4ebb-844f-83ef217e7a28
content-length: 308
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup &apos;example&apos; not found</Message>
  </Error>
  <RequestId>edd68fa2-491c-4ebb-844f-83ef217e7a28</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#7-deleteautoscalinggroup) for a successful call.

### DeleteLaunchConfiguration

Deletes the specified launch configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLaunchConfiguration
&Version=2011-01-01
&LaunchConfigurationName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 083af9ea-2cbe-4b97-b10d-b6286ba4a419
content-length: 304
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Launch configuration name not found - Launch configuration example not found</Message>
  </Error>
  <RequestId>083af9ea-2cbe-4b97-b10d-b6286ba4a419</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteLifecycleHook

Deletes the specified lifecycle hook.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 100
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLifecycleHook
&Version=2011-01-01
&LifecycleHookName=example
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c56d6c71-eb2a-4dc2-ad35-099c8f8f7613
content-length: 311
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>No Lifecycle Hook found with name &apos;example&apos; for group &apos;example&apos;</Message>
  </Error>
  <RequestId>c56d6c71-eb2a-4dc2-ad35-099c8f8f7613</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteNotificationConfiguration

Deletes the specified notification.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 166
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteNotificationConfiguration
&Version=2011-01-01
&AutoScalingGroupName=example
&TopicARN=arn%3Aaws%3Aautoscaling%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 469aa839-a6f9-44fc-896a-ece6f02cb54e
content-length: 269
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - example</Message>
  </Error>
  <RequestId>469aa839-a6f9-44fc-896a-ece6f02cb54e</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeletePolicy

Deletes the specified scaling policy.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 57
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeletePolicy
&Version=2011-01-01
&PolicyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b2a8d8c5-f8fa-43d0-8707-ddebcc559d49
content-length: 303
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>The AutoScaling Group name is required when specifying a policy by its name</Message>
  </Error>
  <RequestId>b2a8d8c5-f8fa-43d0-8707-ddebcc559d49</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteScheduledAction

Deletes the specified scheduled action.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteScheduledAction
&Version=2011-01-01
&AutoScalingGroupName=example
&ScheduledActionName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 3b098a01-20a6-4764-a627-a223598bdd20
content-length: 296
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>3b098a01-20a6-4764-a627-a223598bdd20</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteTags

Deletes the specified tags.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTags
&Version=2011-01-01
&Tags.member.1.Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: fe257251-21ef-4177-92bf-04b3ff09a76a
content-length: 266
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>fe257251-21ef-4177-92bf-04b3ff09a76a</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteWarmPool

Deletes the warm pool for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteWarmPool
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d940aa50-0910-4712-a7f0-942b57e93818
content-length: 297
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - No such Auto Scaling group: example</Message>
  </Error>
  <RequestId>d940aa50-0910-4712-a7f0-942b57e93818</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeAccountLimits

Describes the current Amazon EC2 Auto Scaling resource quotas for your account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAccountLimits
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 717
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeAccountLimitsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAccountLimitsResult>
    <MaxNumberOfAutoScalingGroups>500</MaxNumberOfAutoScalingGroups>
    <MaxNumberOfLaunchConfigurations>200</MaxNumberOfLaunchConfigurations>
    <NumberOfAutoScalingGroups>0</NumberOfAutoScalingGroups>
    <NumberOfLaunchConfigurations>0</NumberOfLaunchConfigurations>
    <ThrottlingLimits xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
      <AccountMaxCallRate>
        <RefillRate>5.0</RefillRate>
        <BucketSize>30</BucketSize>
      </AccountMaxCallRate>
    </ThrottlingLimits>
  </DescribeAccountLimitsResult>
  <ResponseMetadata>
    <RequestId>5650a32c-9bc5-4952-890b-3cc230063d92</RequestId>
  </ResponseMetadata>
</DescribeAccountLimitsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAccountSettings

Describes the current Amazon EC2 Auto Scaling account settings for your account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAccountSettings
&Version=2011-01-01
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: a7413685-68ef-4e0a-9656-0a33b9b971e7
content-length: 313
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeAccountSettings is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>a7413685-68ef-4e0a-9656-0a33b9b971e7</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeAdjustmentTypes

Describes the available adjustment types for step scaling and simple scaling policies.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAdjustmentTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 577
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeAdjustmentTypesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAdjustmentTypesResult>
    <AdjustmentTypes>
      <member xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
        <AdjustmentType>ChangeInCapacity</AdjustmentType>
      </member>
      <member>
        <AdjustmentType>ExactCapacity</AdjustmentType>
      </member>
      <member>
        <AdjustmentType>PercentChangeInCapacity</AdjustmentType>
      </member>
    </AdjustmentTypes>
  </DescribeAdjustmentTypesResult>
  <ResponseMetadata>
    <RequestId>3e2d8d16-7fa8-4b99-8054-b8d3ecdae06a</RequestId>
  </ResponseMetadata>
</DescribeAdjustmentTypesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAutoScalingGroups

Gets information about the Auto Scaling groups in the account and Region.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAutoScalingGroups
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 330
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeAutoScalingGroupsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAutoScalingGroupsResult>
    <AutoScalingGroups></AutoScalingGroups>
  </DescribeAutoScalingGroupsResult>
  <ResponseMetadata>
    <RequestId>0420dc89-7e8e-4764-acba-c9df944f6d5d</RequestId>
  </ResponseMetadata>
</DescribeAutoScalingGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#5-describeautoscalinggroups) for a successful call.

### DescribeAutoScalingInstances

Gets information about the Auto Scaling instances in the account and Region.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAutoScalingInstances
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 348
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeAutoScalingInstancesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAutoScalingInstancesResult>
    <AutoScalingInstances></AutoScalingInstances>
  </DescribeAutoScalingInstancesResult>
  <ResponseMetadata>
    <RequestId>c4adf887-8c09-47e2-8cd0-2964411e6bbf</RequestId>
  </ResponseMetadata>
</DescribeAutoScalingInstancesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAutoScalingNotificationTypes

Describes the notification types that are supported by Amazon EC2 Auto Scaling.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAutoScalingNotificationTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 770
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeAutoScalingNotificationTypesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAutoScalingNotificationTypesResult>
    <AutoScalingNotificationTypes>
      <member>autoscaling:EC2_INSTANCE_LAUNCH</member>
      <member>autoscaling:EC2_INSTANCE_LAUNCH_ERROR</member>
      <member>autoscaling:EC2_INSTANCE_TERMINATE</member>
      <member>autoscaling:EC2_INSTANCE_TERMINATE_ERROR</member>
      <member>autoscaling:EC2_ROOT_VOLUME_REPLACEMENT</member>
      <member>autoscaling:EC2_ROOT_VOLUME_REPLACEMENT_ERROR</member>
      <member>autoscaling:TEST_NOTIFICATION</member>
    </AutoScalingNotificationTypes>
  </DescribeAutoScalingNotificationTypesResult>
  <ResponseMetadata>
    <RequestId>4e2eece7-8699-485b-97a9-b9b9288d6d49</RequestId>
  </ResponseMetadata>
</DescribeAutoScalingNotificationTypesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstanceRefreshes

Gets information about the instance refreshes for the specified Auto Scaling group from the previous six weeks.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 80
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeInstanceRefreshes
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 5f416e8f-27ff-4f76-9ef7-f36a2ec30fd6
content-length: 296
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>5f416e8f-27ff-4f76-9ef7-f36a2ec30fd6</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeLaunchConfigurations

Gets information about the launch configurations in the account and Region.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLaunchConfigurations
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 348
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeLaunchConfigurationsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeLaunchConfigurationsResult>
    <LaunchConfigurations></LaunchConfigurations>
  </DescribeLaunchConfigurationsResult>
  <ResponseMetadata>
    <RequestId>5a025338-3b54-45fc-8a4c-da8a5e7c7661</RequestId>
  </ResponseMetadata>
</DescribeLaunchConfigurationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLifecycleHookTypes

Describes the available types of lifecycle hooks.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLifecycleHookTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 440
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeLifecycleHookTypesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeLifecycleHookTypesResult>
    <LifecycleHookTypes>
      <member>autoscaling:EC2_INSTANCE_LAUNCHING</member>
      <member>autoscaling:EC2_INSTANCE_TERMINATING</member>
    </LifecycleHookTypes>
  </DescribeLifecycleHookTypesResult>
  <ResponseMetadata>
    <RequestId>5cb7dde8-0293-4f35-8081-afb39ae3bdd1</RequestId>
  </ResponseMetadata>
</DescribeLifecycleHookTypesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLifecycleHooks

Gets information about the lifecycle hooks for the specified Auto Scaling group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLifecycleHooks
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 312
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeLifecycleHooksResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeLifecycleHooksResult>
    <LifecycleHooks></LifecycleHooks>
  </DescribeLifecycleHooksResult>
  <ResponseMetadata>
    <RequestId>dfcd7f22-fecc-4c2f-9781-2faee76e2a95</RequestId>
  </ResponseMetadata>
</DescribeLifecycleHooksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLoadBalancerTargetGroups

This API operation is superseded by DescribeTrafficSources , which can describe multiple traffic sources types.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 87
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancerTargetGroups
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e801911c-58a2-4a3c-b84a-5e5b7eed8285
content-length: 321
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - Could not find Auto Scaling group named &apos;example&apos;</Message>
  </Error>
  <RequestId>e801911c-58a2-4a3c-b84a-5e5b7eed8285</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeLoadBalancers

This API operation is superseded by DescribeTrafficSources , which can describe multiple traffic sources types.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeLoadBalancers
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 4558a4e8-e4f8-4e23-b73c-45d4b1919f5b
content-length: 321
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - Could not find Auto Scaling group named &apos;example&apos;</Message>
  </Error>
  <RequestId>4558a4e8-e4f8-4e23-b73c-45d4b1919f5b</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeMetricCollectionTypes

Describes the available CloudWatch metrics for Amazon EC2 Auto Scaling.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeMetricCollectionTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1894
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeMetricCollectionTypesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeMetricCollectionTypesResult>
    <Metrics>
      <member xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
        <Metric>GroupMinSize</Metric>
      </member>
      <member>
        <Metric>GroupMaxSize</Metric>
      </member>
      <member>
        <Metric>GroupDesiredCapacity</Metric>
      </member>
      <member>
        <Metric>GroupInServiceInstances</Metric>
      </member>
      <member>
        <Metric>GroupInServiceCapacity</Metric>
      </member>
      <member>
        <Metric>GroupPendingInstances</Metric>
… (69 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeNotificationConfigurations

Gets information about the Amazon SNS notifications that are configured for one or more Auto Scaling groups.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeNotificationConfigurations
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 384
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeNotificationConfigurationsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeNotificationConfigurationsResult>
    <NotificationConfigurations></NotificationConfigurations>
  </DescribeNotificationConfigurationsResult>
  <ResponseMetadata>
    <RequestId>8ce0a6bc-f67b-479d-a579-54fdbd9da225</RequestId>
  </ResponseMetadata>
</DescribeNotificationConfigurationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribePolicies

Gets information about the scaling policies in the account and Region.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 42
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribePolicies
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 290
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribePoliciesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribePoliciesResult>
    <ScalingPolicies></ScalingPolicies>
  </DescribePoliciesResult>
  <ResponseMetadata>
    <RequestId>e7b256d9-0c98-492e-b928-5786459982f9</RequestId>
  </ResponseMetadata>
</DescribePoliciesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeScalingActivities

Gets information about the scaling activities in the account and Region.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeScalingActivities
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 316
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeScalingActivitiesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeScalingActivitiesResult>
    <Activities></Activities>
  </DescribeScalingActivitiesResult>
  <ResponseMetadata>
    <RequestId>5442dde8-6fcc-49bc-902e-b8dfaa458ae2</RequestId>
  </ResponseMetadata>
</DescribeScalingActivitiesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeScalingProcessTypes

Describes the scaling process types for use with the ResumeProcesses and SuspendProcesses APIs.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeScalingProcessTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 893
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeScalingProcessTypesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeScalingProcessTypesResult>
    <Processes>
      <member xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
        <ProcessName>AZRebalance</ProcessName>
      </member>
      <member>
        <ProcessName>AddToLoadBalancer</ProcessName>
      </member>
      <member>
        <ProcessName>AlarmNotification</ProcessName>
      </member>
      <member>
        <ProcessName>HealthCheck</ProcessName>
      </member>
      <member>
        <ProcessName>InstanceRefresh</ProcessName>
      </member>
      <member>
        <ProcessName>Launch</ProcessName>
… (16 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeScheduledActions

Gets information about the scheduled actions that haven't run or that have not reached their end time.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeScheduledActions
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 346
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeScheduledActionsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeScheduledActionsResult>
    <ScheduledUpdateGroupActions></ScheduledUpdateGroupActions>
  </DescribeScheduledActionsResult>
  <ResponseMetadata>
    <RequestId>4fc4e7b6-9cba-4fb9-b5f9-29e5fe47a310</RequestId>
  </ResponseMetadata>
</DescribeScheduledActionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTags

Describes the specified tags.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 38
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTags
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 252
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeTagsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeTagsResult>
    <Tags></Tags>
  </DescribeTagsResult>
  <ResponseMetadata>
    <RequestId>5a329165-e5d6-45f0-a91d-dee22bdf13b5</RequestId>
  </ResponseMetadata>
</DescribeTagsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTerminationPolicyTypes

Describes the termination policies supported by Amazon EC2 Auto Scaling.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 56
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTerminationPolicyTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 625
date: Thu, 26 Nov 2026 18:27:33 GMT

<DescribeTerminationPolicyTypesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeTerminationPolicyTypesResult>
    <TerminationPolicyTypes>
      <member>AllocationStrategy</member>
      <member>ClosestToNextInstanceHour</member>
      <member>Default</member>
      <member>Lambda</member>
      <member>NewestInstance</member>
      <member>OldestInstance</member>
      <member>OldestLaunchConfiguration</member>
      <member>OldestLaunchTemplate</member>
    </TerminationPolicyTypes>
  </DescribeTerminationPolicyTypesResult>
  <ResponseMetadata>
    <RequestId>077ac5be-d1c2-4a04-90b0-ef2d93005839</RequestId>
  </ResponseMetadata>
</DescribeTerminationPolicyTypesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTrafficSources

Gets information about the traffic sources for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTrafficSources
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: efa7fcc4-34d5-4a8a-9aaf-1ad2b88d43af
content-length: 321
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - Could not find Auto Scaling group named &apos;example&apos;</Message>
  </Error>
  <RequestId>efa7fcc4-34d5-4a8a-9aaf-1ad2b88d43af</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeWarmPool

Gets information about a warm pool and its instances.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeWarmPool
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 6c088fb7-3dc6-4ffc-80bb-92e04bbe616f
content-length: 297
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - No such Auto Scaling group: example</Message>
  </Error>
  <RequestId>6c088fb7-3dc6-4ffc-80bb-92e04bbe616f</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DetachInstances

Removes one or more instances from the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 107
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DetachInstances
&Version=2011-01-01
&AutoScalingGroupName=example
&ShouldDecrementDesiredCapacity=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: ade3df0e-e5fc-4187-9a3b-22a192200c46
content-length: 296
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>ade3df0e-e5fc-4187-9a3b-22a192200c46</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DetachLoadBalancerTargetGroups

This API operation is superseded by DetachTrafficSources , which can detach multiple traffic sources types.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 118
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DetachLoadBalancerTargetGroups
&Version=2011-01-01
&AutoScalingGroupName=example
&TargetGroupARNs.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a0fd21ac-4f10-4570-a9bc-e69b34d4b1fd
content-length: 266
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>a0fd21ac-4f10-4570-a9bc-e69b34d4b1fd</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DetachLoadBalancers

This API operation is superseded by DetachTrafficSources , which can detach multiple traffic sources types.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 109
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DetachLoadBalancers
&Version=2011-01-01
&AutoScalingGroupName=example
&LoadBalancerNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 73afd076-23b9-442c-b9e2-9e28b3967739
content-length: 266
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>73afd076-23b9-442c-b9e2-9e28b3967739</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DetachTrafficSources

Detaches one or more traffic sources from the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 118
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DetachTrafficSources
&Version=2011-01-01
&AutoScalingGroupName=example
&TrafficSources.member.1.Identifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e0d5bca2-66b1-4a51-a59d-52feb296e38d
content-length: 266
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>e0d5bca2-66b1-4a51-a59d-52feb296e38d</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DisableMetricsCollection

Disables group metrics collection for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 79
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DisableMetricsCollection
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e97323bb-283d-4405-8bcc-30386e77136a
content-length: 251
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Group example not found</Message>
  </Error>
  <RequestId>e97323bb-283d-4405-8bcc-30386e77136a</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### EnableMetricsCollection

Enables group metrics collection for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=EnableMetricsCollection
&Version=2011-01-01
&AutoScalingGroupName=example
&Granularity=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 64cb4e40-6c98-43bb-b119-497b110b3de9
content-length: 251
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Group example not found</Message>
  </Error>
  <RequestId>64cb4e40-6c98-43bb-b119-497b110b3de9</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### EnterStandby

Moves the specified instances into the standby state.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=EnterStandby
&Version=2011-01-01
&AutoScalingGroupName=example
&ShouldDecrementDesiredCapacity=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b15a2918-bf9f-406b-a830-27ea63757d10
content-length: 296
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>b15a2918-bf9f-406b-a830-27ea63757d10</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ExecutePolicy

Executes the specified policy.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ExecutePolicy
&Version=2011-01-01
&PolicyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 74560897-cf0c-433b-afe1-aeae16898aac
content-length: 303
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>The AutoScaling Group name is required when specifying a policy by its name</Message>
  </Error>
  <RequestId>74560897-cf0c-433b-afe1-aeae16898aac</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ExitStandby

Moves the specified instances out of the standby state.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ExitStandby
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 881f4c65-e8de-4091-bc0c-104f6484d278
content-length: 296
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>881f4c65-e8de-4091-bc0c-104f6484d278</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetPredictiveScalingForecast

Retrieves the forecast data for a predictive scaling policy.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 170
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=GetPredictiveScalingForecast
&Version=2011-01-01
&AutoScalingGroupName=example
&PolicyName=example
&StartTime=2026-11-24T01%3A44%3A41Z
&EndTime=2026-11-24T01%3A44%3A41Z
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f2593334-fd68-4ff5-932a-c620f47d9fd3
content-length: 267
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Given end time must be after start time</Message>
  </Error>
  <RequestId>f2593334-fd68-4ff5-932a-c620f47d9fd3</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### LaunchInstances

Launches a specified number of instances in an Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 110
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=LaunchInstances
&Version=2011-01-01
&AutoScalingGroupName=example
&RequestedCapacity=1
&ClientToken=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f0024949-aa26-4a5e-b142-ab39fd56dd78
content-length: 292
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup not found - No such Auto Scaling group: example</Message>
  </Error>
  <RequestId>f0024949-aa26-4a5e-b142-ab39fd56dd78</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutAccountSetting

Modifies an account setting for your account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutAccountSetting
&Version=2011-01-01
&Name=example
&Value=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: 4bde3fbb-6f32-49dd-a5a1-8916c4736e8a
content-length: 307
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation PutAccountSetting is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>4bde3fbb-6f32-49dd-a5a1-8916c4736e8a</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### PutLifecycleHook

Creates or updates a lifecycle hook for the specified Auto Scaling group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 97
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutLifecycleHook
&Version=2011-01-01
&LifecycleHookName=example
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 7b32f049-b951-48fe-9a6a-e30c2fccf327
content-length: 297
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - No such Auto Scaling group: example</Message>
  </Error>
  <RequestId>7b32f049-b951-48fe-9a6a-e30c2fccf327</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutNotificationConfiguration

Configures an Auto Scaling group to send notifications when specified events take place.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 198
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutNotificationConfiguration
&Version=2011-01-01
&AutoScalingGroupName=example
&TopicARN=arn%3Aaws%3Aautoscaling%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&NotificationTypes.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a1710d26-f246-4140-bb5a-ecd161aa467a
content-length: 269
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - example</Message>
  </Error>
  <RequestId>a1710d26-f246-4140-bb5a-ecd161aa467a</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutScalingPolicy

Creates or updates a scaling policy for an Auto Scaling group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutScalingPolicy
&Version=2011-01-01
&AutoScalingGroupName=example
&PolicyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 965c8e2d-20dd-47b8-ae27-f93ceaecce60
content-length: 251
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Group example not found</Message>
  </Error>
  <RequestId>965c8e2d-20dd-47b8-ae27-f93ceaecce60</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutScheduledUpdateGroupAction

Creates or updates a scheduled scaling action for an Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 112
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutScheduledUpdateGroupAction
&Version=2011-01-01
&AutoScalingGroupName=example
&ScheduledActionName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b9aae381-ed2f-489a-a1c0-dacaeea94e77
content-length: 266
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>b9aae381-ed2f-489a-a1c0-dacaeea94e77</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutWarmPool

Creates or updates a warm pool for the specified Auto Scaling group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PutWarmPool
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 0505617e-c91a-4c6d-8372-25f980740a37
content-length: 297
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - No such Auto Scaling group: example</Message>
  </Error>
  <RequestId>0505617e-c91a-4c6d-8372-25f980740a37</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### RecordLifecycleActionHeartbeat

Records a heartbeat for the lifecycle action associated with the specified token or instance.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 111
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RecordLifecycleActionHeartbeat
&Version=2011-01-01
&LifecycleHookName=example
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b61dd3da-524a-4b4e-8eae-b3015001d447
content-length: 297
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>No Lifecycle Action found because no token or instance ID is provided</Message>
  </Error>
  <RequestId>b61dd3da-524a-4b4e-8eae-b3015001d447</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ResumeProcesses

Resumes the specified suspended auto scaling processes, or all suspended process, for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ResumeProcesses
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: daaee39d-5f96-4d49-8bac-b94f4444fb15
content-length: 284
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - no such group: example</Message>
  </Error>
  <RequestId>daaee39d-5f96-4d49-8bac-b94f4444fb15</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### RollbackInstanceRefresh

Cancels an instance refresh that is in progress and rolls back any changes that it made.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RollbackInstanceRefresh
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 36b4402d-ebec-4ad2-a4e9-0120fef6f1af
content-length: 308
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - Auto Scaling group with name example not found</Message>
  </Error>
  <RequestId>36b4402d-ebec-4ad2-a4e9-0120fef6f1af</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SetDesiredCapacity

Sets the size of the specified Auto Scaling group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetDesiredCapacity
&Version=2011-01-01
&AutoScalingGroupName=example
&DesiredCapacity=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2e44793c-0786-4698-80ba-963998cd0a00
content-length: 266
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>2e44793c-0786-4698-80ba-963998cd0a00</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-setdesiredcapacity) for a successful call.

### SetInstanceHealth

Sets the health status of the specified instance.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetInstanceHealth
&Version=2011-01-01
&InstanceId=example
&HealthStatus=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 7bcebd80-da38-440f-ae49-964095c1a300
content-length: 283
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Valid instance health states are: [Healthy, Unhealthy].</Message>
  </Error>
  <RequestId>7bcebd80-da38-440f-ae49-964095c1a300</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SetInstanceProtection

Updates the instance protection settings of the specified instances.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 132
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SetInstanceProtection
&Version=2011-01-01
&InstanceIds.member.1=example
&AutoScalingGroupName=example
&ProtectedFromScaleIn=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 0157fd44-3d6a-4a9a-b9e9-86e81236353a
content-length: 296
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>0157fd44-3d6a-4a9a-b9e9-86e81236353a</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### StartInstanceRefresh

Starts an instance refresh.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StartInstanceRefresh
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 62916ac8-3278-4387-9b2a-068f47ec071d
content-length: 266
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>62916ac8-3278-4387-9b2a-068f47ec071d</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SuspendProcesses

Suspends the specified auto scaling processes, or all processes, for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SuspendProcesses
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 0ace11d7-c11c-414b-af14-d117e1b91ded
content-length: 284
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - no such group: example</Message>
  </Error>
  <RequestId>0ace11d7-c11c-414b-af14-d117e1b91ded</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### TerminateInstanceInAutoScalingGroup

Terminates the specified instance and optionally adjusts the desired group size.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TerminateInstanceInAutoScalingGroup
&Version=2011-01-01
&ShouldDecrementDesiredCapacity=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 33b6ff9a-b597-4038-b0c5-85fe4a6ade60
content-length: 307
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Valid requests must contain either the InstanceId or the InstanceIds parameter.</Message>
  </Error>
  <RequestId>33b6ff9a-b597-4038-b0c5-85fe4a6ade60</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### UpdateAutoScalingGroup

We strongly recommend that all Auto Scaling groups use launch templates to ensure full functionality for Amazon EC2 Auto Scaling and Amazon EC2.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T182733Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=UpdateAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 446440e7-51e9-4330-9c9f-0896ce8b36f4
content-length: 266
date: Thu, 26 Nov 2026 18:27:33 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>446440e7-51e9-4330-9c9f-0896ce8b36f4</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/autoscaling/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with a form-encoded body: `Action` names the operation, `Version` the API version, and every input member is a flattened key such as `Tags.member.1.Key`.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Auto Scaling answers in AWS: XML wrapped in `<OperationResponse><OperationResult>`. Errors captured on this page are an XML document rooted at `<ErrorResponse>`.

### Other services involved

The walkthrough also called [Amazon Elastic Compute Cloud](ec2.md). They are separate services behind the same endpoint, sharing one account and one in-memory engine, so a resource created through one is immediately visible to the other.
