# Auto Scaling

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `Auto Scaling`
- **sigv4 signing name:** `autoscaling`
- **Endpoint prefix:** `autoscaling`
- **ARN namespace:** `autoscaling`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `autoscaling`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

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
X-Amz-Date: 20261116T120917Z
Content-Length: 140
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
x-amzn-requestid: 75bf7700-1ea0-4282-bc95-28c928532efb
content-length: 395
date: Mon, 16 Nov 2026 12:09:17 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>UnsupportedOperation</Code>
    <Message>The Launch Configuration creation operation is not available in your account. Use launch templates to create configuration templates for your Auto Scaling groups.</Message>
  </Error>
  <RequestId>75bf7700-1ea0-4282-bc95-28c928532efb</RequestId>
</ErrorResponse>
```

### 2. Amazon Elastic Compute Cloud: CreateLaunchTemplate

The launch template lives in EC2.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120917Z
Content-Length: 217
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
content-length: 616
date: Mon, 16 Nov 2026 12:09:17 GMT

<CreateLaunchTemplateResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <launchTemplate xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <launchTemplateId>lt-000000000000000a9</launchTemplateId>
    <launchTemplateName>docs-demo-lt</launchTemplateName>
    <createTime>2026-11-16T12:09:17.312683Z</createTime>
    <createdBy>arn:aws:iam::000000000000:root</createdBy>
    <defaultVersionNumber>1</defaultVersionNumber>
    <latestVersionNumber>1</latestVersionNumber>
    <tagSet/>
    <operator>
      <managed>false</managed>
    </operator>
  </launchTemplate>
  <requestId>196c5ce6-e618-449b-b1b6-bbb77b468a9b</requestId>
</CreateLaunchTemplateResponse>
```

### 3. CreateAutoScalingGroup

Subnets are passed as one comma-separated string; success is an empty result.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120917Z
Content-Length: 277
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=docs-demo-asg
&LaunchTemplate.LaunchTemplateId=lt-000000000000000a9
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
date: Mon, 16 Nov 2026 12:09:17 GMT

<CreateAutoScalingGroupResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <CreateAutoScalingGroupResult/>
  <ResponseMetadata>
    <RequestId>0d05c457-1aab-4ea7-998a-a9b4a7777c76</RequestId>
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
X-Amz-Date: 20261116T120917Z
Content-Length: 97
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:09:17 GMT

<SetDesiredCapacityResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <SetDesiredCapacityResult/>
  <ResponseMetadata>
    <RequestId>f5f4622a-3fdb-43eb-b4a2-f0748cb1ddc7</RequestId>
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
X-Amz-Date: 20261116T120917Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAutoScalingGroups
&Version=2011-01-01
&AutoScalingGroupNames.member.1=docs-demo-asg
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 2080
date: Mon, 16 Nov 2026 12:09:17 GMT

<DescribeAutoScalingGroupsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAutoScalingGroupsResult>
    <AutoScalingGroups>
      <member xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
        <AutoScalingGroupName>docs-demo-asg</AutoScalingGroupName>
        <AutoScalingGroupARN>arn:aws:autoscaling:us-east-1:000000000000:autoScalingGroup:00000001-0000-4000-8000-000000000001:autoScalingGroupName/docs-demo-asg</AutoScalingGroupARN>
        <LaunchTemplate>
          <LaunchTemplateId>lt-000000000000000a9</LaunchTemplateId>
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
        <LoadBalancerNames/>
        <TargetGroupARNs/>
        <HealthCheckType>EC2</HealthCheckType>
        <HealthCheckGracePeriod>0</HealthCheckGracePeriod>
        <Instances>
          <member>
            <InstanceId>i-00000000000000002</InstanceId>
            <InstanceType>t3.micro</InstanceType>
            <AvailabilityZone>us-east-1a</AvailabilityZone>
            <LifecycleState>Pending</LifecycleState>
            <HealthStatus>Healthy</HealthStatus>
            <LaunchTemplate>
              <LaunchTemplateId>lt-000000000000000a9</LaunchTemplateId>
              <LaunchTemplateName>docs-demo-lt</LaunchTemplateName>
              <Version>$Latest</Version>
            </LaunchTemplate>
            <ImageId>ami-0000000000000003c</ImageId>
            <ProtectedFromScaleIn>false</ProtectedFromScaleIn>
          </member>
        </Instances>
        <CreatedTime>2026-11-16T12:09:17.331094Z</CreatedTime>
        <SuspendedProcesses/>
        <VPCZoneIdentifier>subnet-00000000000000007,subnet-00000000000000009</VPCZoneIdentifier>
        <EnabledMetrics/>
        <Tags/>
        <TerminationPolicies>
          <member>Default</member>
        </TerminationPolicies>
        <NewInstancesProtectedFromScaleIn>false</NewInstancesProtectedFromScaleIn>
        <TrafficSources/>
      </member>
    </AutoScalingGroups>
  </DescribeAutoScalingGroupsResult>
  <ResponseMetadata>
    <RequestId>b7068fa3-1cd2-4afe-9df1-e111abc72e67</RequestId>
  </ResponseMetadata>
</DescribeAutoScalingGroupsResponse>
```

### 6. DeleteAutoScalingGroup

A group that still has instances is refused unless the request forces it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120917Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=docs-demo-asg
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 83cbd18d-ed52-4f65-91b6-2c7700c00fd0
content-length: 345
date: Mon, 16 Nov 2026 12:09:17 GMT

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ResourceInUse</Code>
    <Message>You cannot delete an AutoScalingGroup while there are instances or pending Spot instance request(s) still in the group.</Message>
  </Error>
  <RequestId>83cbd18d-ed52-4f65-91b6-2c7700c00fd0</RequestId>
</ErrorResponse>
```

### 7. DeleteAutoScalingGroup

`ForceDelete` terminates the group's instances with it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120917Z
Content-Length: 100
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Mon, 16 Nov 2026 12:09:17 GMT

<DeleteAutoScalingGroupResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DeleteAutoScalingGroupResult/>
  <ResponseMetadata>
    <RequestId>ce83c3f6-d3f0-4ced-81ef-98bb1789d530</RequestId>
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
X-Amz-Date: 20261116T120917Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/autoscaling/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAutoScalingGroups
&Version=2011-01-01
&AutoScalingGroupNames.member.1=docs-demo-asg
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 330
date: Mon, 16 Nov 2026 12:09:17 GMT

<DescribeAutoScalingGroupsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAutoScalingGroupsResult>
    <AutoScalingGroups/>
  </DescribeAutoScalingGroupsResult>
  <ResponseMetadata>
    <RequestId>42bf3e2b-83d3-4e48-a03b-af6fc772c0f0</RequestId>
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
X-Amz-Date: 20261116T120917Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteLaunchTemplate
&Version=2016-11-15
&LaunchTemplateId=lt-000000000000000a9
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 616
date: Mon, 16 Nov 2026 12:09:17 GMT

<DeleteLaunchTemplateResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <launchTemplate xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <launchTemplateId>lt-000000000000000a9</launchTemplateId>
    <launchTemplateName>docs-demo-lt</launchTemplateName>
    <createTime>2026-11-16T12:09:17.312683Z</createTime>
    <createdBy>arn:aws:iam::000000000000:root</createdBy>
    <defaultVersionNumber>1</defaultVersionNumber>
    <latestVersionNumber>1</latestVersionNumber>
    <tagSet/>
    <operator>
      <managed>false</managed>
    </operator>
  </launchTemplate>
  <requestId>179e057f-1c75-4a4a-af0d-3aef0c49b361</requestId>
</DeleteLaunchTemplateResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 68 operations, 15 accepted the placeholder call, 2 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AttachInstances

Attaches one or more EC2 instances to the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=AttachInstances
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup: example not found</Message>
  </Error>
  <RequestId>c5ebd126-b0f8-46da-a375-b51e2e2579f7</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AttachLoadBalancerTargetGroups

This API operation is superseded by AttachTrafficSources , which can attach multiple traffic sources types.

```http
POST / HTTP/1.1

Action=AttachLoadBalancerTargetGroups
&Version=2011-01-01
&AutoScalingGroupName=example
&TargetGroupARNs.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>55a103f5-d42a-41ae-989f-b16a7b75d9ef</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AttachLoadBalancers

This API operation is superseded by AttachTrafficSources , which can attach multiple traffic sources types.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=AttachLoadBalancers
&Version=2011-01-01
&AutoScalingGroupName=example
&LoadBalancerNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>f9fb7d57-2684-44d3-a691-e8abd77e7763</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### AttachTrafficSources

Attaches one or more traffic sources to the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=AttachTrafficSources
&Version=2011-01-01
&AutoScalingGroupName=example
&TrafficSources.member.1.Identifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>e28ec8a2-b45f-4ca8-8483-c995e5823333</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### BatchDeleteScheduledAction

Deletes one or more scheduled actions for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=BatchDeleteScheduledAction
&Version=2011-01-01
&AutoScalingGroupName=example
&ScheduledActionNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>104bb305-fdfd-486a-a0fc-1bd757477bf4</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### BatchPutScheduledUpdateGroupAction

Creates or updates one or more scheduled scaling actions for an Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=BatchPutScheduledUpdateGroupAction
&Version=2011-01-01
&AutoScalingGroupName=example
&ScheduledUpdateGroupActions.member.1.ScheduledActionName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>906409b6-713a-40c0-abaf-d3cc61b36c4c</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CancelInstanceRefresh

Cancels an instance refresh or rollback that is in progress.

```http
POST / HTTP/1.1

Action=CancelInstanceRefresh
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - Auto Scaling group with name example not found</Message>
  </Error>
  <RequestId>5ba6280e-657e-420b-94eb-e0c27cc7a5ec</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CompleteLifecycleAction

Completes the lifecycle action for the specified token or instance with the specified result.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=CompleteLifecycleAction
&Version=2011-01-01
&LifecycleHookName=example
&AutoScalingGroupName=example
&LifecycleActionResult=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>'LifecycleActionResult' must be one of: CONTINUE, ABANDON</Message>
  </Error>
  <RequestId>4fb9c51a-03b9-4aad-a02f-3304d87bdd91</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### CreateAutoScalingGroup

We strongly recommend using a launch template when calling this operation to ensure full functionality for Amazon EC2 Auto Scaling and Amazon EC2.

```http
POST / HTTP/1.1

Action=CreateAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=example
&MinSize=1
&MaxSize=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Valid requests must contain either LaunchTemplate, LaunchConfigurationName, InstanceId or MixedInstancesPolicy parameter.</Message>
  </Error>
  <RequestId>51626fc8-7d17-404f-9c6f-b49fffeb1553</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#3-createautoscalinggroup) for a successful call.

### CreateLaunchConfiguration

Creates a launch configuration.

```http
POST / HTTP/1.1

Action=CreateLaunchConfiguration
&Version=2011-01-01
&LaunchConfigurationName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>UnsupportedOperation</Code>
    <Message>The Launch Configuration creation operation is not available in your account. Use launch templates to create configuration templates for your Auto Scaling groups.</Message>
  </Error>
  <RequestId>3b5c71f1-3e01-45a5-86b2-728ce976b890</RequestId>
</ErrorResponse>
```

`400` `UnsupportedOperation`.

### CreateOrUpdateTags

Creates or updates tags for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=CreateOrUpdateTags
&Version=2011-01-01
&Tags.member.1.Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>ddb882a0-df57-4ab1-8f37-d1426ec449f5</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteAutoScalingGroup

Deletes the specified Auto Scaling group.

```http
POST / HTTP/1.1

Action=DeleteAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup 'example' not found</Message>
  </Error>
  <RequestId>2a26a680-8120-44d0-8440-b04e282eeee9</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#7-deleteautoscalinggroup) for a successful call.

### DeleteLaunchConfiguration

Deletes the specified launch configuration.

```http
POST / HTTP/1.1

Action=DeleteLaunchConfiguration
&Version=2011-01-01
&LaunchConfigurationName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Launch configuration name not found - Launch configuration example not found</Message>
  </Error>
  <RequestId>bf388b9b-6392-494c-b3e5-d93cae825135</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteLifecycleHook

Deletes the specified lifecycle hook.

```http
POST / HTTP/1.1

Action=DeleteLifecycleHook
&Version=2011-01-01
&LifecycleHookName=example
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>adaac854-6181-44cf-b09b-05cf05f92071</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteNotificationConfiguration

Deletes the specified notification.

```http
POST / HTTP/1.1

Action=DeleteNotificationConfiguration
&Version=2011-01-01
&AutoScalingGroupName=example
&TopicARN=arn%3Aaws%3Aautoscaling%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>348e1df4-ed1a-4bb4-86dc-d8cc292c2bd5</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeletePolicy

Deletes the specified scaling policy.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DeletePolicy
&Version=2011-01-01
&PolicyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>The AutoScaling Group name is required when specifying a policy by its name</Message>
  </Error>
  <RequestId>e064ab7f-52c2-4df3-a97e-255c60ecea07</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteScheduledAction

Deletes the specified scheduled action.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DeleteScheduledAction
&Version=2011-01-01
&AutoScalingGroupName=example
&ScheduledActionName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>1233f1da-ef86-4ec6-b8b7-5ecd6c2dd3ce</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteTags

Deletes the specified tags.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DeleteTags
&Version=2011-01-01
&Tags.member.1.Key=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>9d5df158-f93c-4b1f-aa1b-33c22184bb9f</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DeleteWarmPool

Deletes the warm pool for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DeleteWarmPool
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - No such Auto Scaling group: example</Message>
  </Error>
  <RequestId>73ed09ce-ab09-4f6c-b5be-3af2fd67419e</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeAccountLimits

Describes the current Amazon EC2 Auto Scaling resource quotas for your account.

```http
POST / HTTP/1.1

Action=DescribeAccountLimits
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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
    <RequestId>c2af7ebd-15f2-4f01-bfe9-dd9079f64eb1</RequestId>
  </ResponseMetadata>
</DescribeAccountLimitsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAccountSettings

Describes the current Amazon EC2 Auto Scaling account settings for your account.

```http
POST / HTTP/1.1

Action=DescribeAccountSettings
&Version=2011-01-01
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeAccountSettings is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>ec0452da-161c-4c8c-9d9b-85e0675e60b4</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeAdjustmentTypes

Describes the available adjustment types for step scaling and simple scaling policies.

```http
POST / HTTP/1.1

Action=DescribeAdjustmentTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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
    <RequestId>eca42808-dedb-4d21-882e-bb90e1c3bb3d</RequestId>
  </ResponseMetadata>
</DescribeAdjustmentTypesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAutoScalingGroups

Gets information about the Auto Scaling groups in the account and Region.

```http
POST / HTTP/1.1

Action=DescribeAutoScalingGroups
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeAutoScalingGroupsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAutoScalingGroupsResult>
    <AutoScalingGroups/>
  </DescribeAutoScalingGroupsResult>
  <ResponseMetadata>
    <RequestId>3bdbbec4-a470-4c11-bd21-8841e4a64117</RequestId>
  </ResponseMetadata>
</DescribeAutoScalingGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#5-describeautoscalinggroups) for a successful call.

### DescribeAutoScalingInstances

Gets information about the Auto Scaling instances in the account and Region.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DescribeAutoScalingInstances
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeAutoScalingInstancesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeAutoScalingInstancesResult>
    <AutoScalingInstances/>
  </DescribeAutoScalingInstancesResult>
  <ResponseMetadata>
    <RequestId>95250a84-48a3-40bd-a18c-4c64027d96f4</RequestId>
  </ResponseMetadata>
</DescribeAutoScalingInstancesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAutoScalingNotificationTypes

Describes the notification types that are supported by Amazon EC2 Auto Scaling.

```http
POST / HTTP/1.1

Action=DescribeAutoScalingNotificationTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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
    <RequestId>8d9bcdb5-4b5a-48be-84f1-cb23b22da2bb</RequestId>
  </ResponseMetadata>
</DescribeAutoScalingNotificationTypesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstanceRefreshes

Gets information about the instance refreshes for the specified Auto Scaling group from the previous six weeks.

```http
POST / HTTP/1.1

Action=DescribeInstanceRefreshes
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>3e8a8b2a-6fd4-4265-be99-6d1545218ad4</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeLaunchConfigurations

Gets information about the launch configurations in the account and Region.

```http
POST / HTTP/1.1

Action=DescribeLaunchConfigurations
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeLaunchConfigurationsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeLaunchConfigurationsResult>
    <LaunchConfigurations/>
  </DescribeLaunchConfigurationsResult>
  <ResponseMetadata>
    <RequestId>bc84a6fd-504e-4145-b991-5ec3d7644db0</RequestId>
  </ResponseMetadata>
</DescribeLaunchConfigurationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLifecycleHookTypes

Describes the available types of lifecycle hooks.

```http
POST / HTTP/1.1

Action=DescribeLifecycleHookTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeLifecycleHookTypesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeLifecycleHookTypesResult>
    <LifecycleHookTypes>
      <member>autoscaling:EC2_INSTANCE_LAUNCHING</member>
      <member>autoscaling:EC2_INSTANCE_TERMINATING</member>
    </LifecycleHookTypes>
  </DescribeLifecycleHookTypesResult>
  <ResponseMetadata>
    <RequestId>8c347729-a230-4c58-b75c-5f08f16866db</RequestId>
  </ResponseMetadata>
</DescribeLifecycleHookTypesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLifecycleHooks

Gets information about the lifecycle hooks for the specified Auto Scaling group.

```http
POST / HTTP/1.1

Action=DescribeLifecycleHooks
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>d1d73352-3c09-46ba-a17e-310abec7b034</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeLoadBalancerTargetGroups

This API operation is superseded by DescribeTrafficSources , which can describe multiple traffic sources types.

```http
POST / HTTP/1.1

Action=DescribeLoadBalancerTargetGroups
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>2f3f0355-3c38-4085-8b16-ee6f1407ff28</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeLoadBalancers

This API operation is superseded by DescribeTrafficSources , which can describe multiple traffic sources types.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DescribeLoadBalancers
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - Could not find Auto Scaling group named 'example'</Message>
  </Error>
  <RequestId>b589068b-4469-494c-9e14-a995d4ca0974</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeMetricCollectionTypes

Describes the available CloudWatch metrics for Amazon EC2 Auto Scaling.

```http
POST / HTTP/1.1

Action=DescribeMetricCollectionTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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
        <Metric>GroupPendingInstances</Metric>
      </member>
      <member>
        <Metric>GroupStandbyInstances</Metric>
… (69 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeNotificationConfigurations

Gets information about the Amazon SNS notifications that are configured for one or more Auto Scaling groups.

```http
POST / HTTP/1.1

Action=DescribeNotificationConfigurations
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeNotificationConfigurationsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeNotificationConfigurationsResult>
    <NotificationConfigurations/>
  </DescribeNotificationConfigurationsResult>
  <ResponseMetadata>
    <RequestId>74446ca0-20bd-419e-bdd2-20491b45bb93</RequestId>
  </ResponseMetadata>
</DescribeNotificationConfigurationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribePolicies

Gets information about the scaling policies in the account and Region.

```http
POST / HTTP/1.1

Action=DescribePolicies
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribePoliciesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribePoliciesResult>
    <ScalingPolicies/>
  </DescribePoliciesResult>
  <ResponseMetadata>
    <RequestId>58d7bc7e-1066-4fde-9f0b-57fb28a26133</RequestId>
  </ResponseMetadata>
</DescribePoliciesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeScalingActivities

Gets information about the scaling activities in the account and Region.

```http
POST / HTTP/1.1

Action=DescribeScalingActivities
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeScalingActivitiesResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeScalingActivitiesResult>
    <Activities/>
  </DescribeScalingActivitiesResult>
  <ResponseMetadata>
    <RequestId>38bbbb20-3508-45c3-aa8c-4e088b6c003b</RequestId>
  </ResponseMetadata>
</DescribeScalingActivitiesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeScalingProcessTypes

Describes the scaling process types for use with the ResumeProcesses and SuspendProcesses APIs.

```http
POST / HTTP/1.1

Action=DescribeScalingProcessTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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

Action=DescribeScheduledActions
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeScheduledActionsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeScheduledActionsResult>
    <ScheduledUpdateGroupActions/>
  </DescribeScheduledActionsResult>
  <ResponseMetadata>
    <RequestId>8410a9a6-8224-44dc-9fd0-6770e3ac85b2</RequestId>
  </ResponseMetadata>
</DescribeScheduledActionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTags

Describes the specified tags.

```http
POST / HTTP/1.1

Action=DescribeTags
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeTagsResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <DescribeTagsResult>
    <Tags/>
  </DescribeTagsResult>
  <ResponseMetadata>
    <RequestId>7d84ff59-f365-432d-8063-7a94dd2aaffa</RequestId>
  </ResponseMetadata>
</DescribeTagsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTerminationPolicyTypes

Describes the termination policies supported by Amazon EC2 Auto Scaling.

```http
POST / HTTP/1.1

Action=DescribeTerminationPolicyTypes
&Version=2011-01-01
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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
    <RequestId>7becbc80-81b7-4089-8589-31cd12b7e2ce</RequestId>
  </ResponseMetadata>
</DescribeTerminationPolicyTypesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTrafficSources

Gets information about the traffic sources for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DescribeTrafficSources
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - Could not find Auto Scaling group named 'example'</Message>
  </Error>
  <RequestId>6837bdb9-d576-4cb1-be56-90f8592fc7b8</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DescribeWarmPool

Gets information about a warm pool and its instances.

```http
POST / HTTP/1.1

Action=DescribeWarmPool
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - No such Auto Scaling group: example</Message>
  </Error>
  <RequestId>c1650722-11de-4d86-bf44-494084b5338c</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DetachInstances

Removes one or more instances from the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DetachInstances
&Version=2011-01-01
&AutoScalingGroupName=example
&ShouldDecrementDesiredCapacity=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>5bf2d0e1-c826-4f40-8da2-57ee7d14d2ca</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DetachLoadBalancerTargetGroups

This API operation is superseded by DetachTrafficSources , which can detach multiple traffic sources types.

```http
POST / HTTP/1.1

Action=DetachLoadBalancerTargetGroups
&Version=2011-01-01
&AutoScalingGroupName=example
&TargetGroupARNs.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>c89645b9-5566-48e4-9d62-c26594a9a79d</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DetachLoadBalancers

This API operation is superseded by DetachTrafficSources , which can detach multiple traffic sources types.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DetachLoadBalancers
&Version=2011-01-01
&AutoScalingGroupName=example
&LoadBalancerNames.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>9b4e370f-67ab-491c-89ab-9eed7f56fccd</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DetachTrafficSources

Detaches one or more traffic sources from the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DetachTrafficSources
&Version=2011-01-01
&AutoScalingGroupName=example
&TrafficSources.member.1.Identifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>17aaee95-fc77-4615-b6c2-23303fb63b62</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### DisableMetricsCollection

Disables group metrics collection for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DisableMetricsCollection
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Group example not found</Message>
  </Error>
  <RequestId>9550510e-9498-431e-8345-e0f173c0e39c</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### EnableMetricsCollection

Enables group metrics collection for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=EnableMetricsCollection
&Version=2011-01-01
&AutoScalingGroupName=example
&Granularity=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Group example not found</Message>
  </Error>
  <RequestId>0d588393-6707-4886-9f01-b942d65e429b</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### EnterStandby

Moves the specified instances into the standby state.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=EnterStandby
&Version=2011-01-01
&AutoScalingGroupName=example
&ShouldDecrementDesiredCapacity=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>17432d14-d938-4aeb-8d7e-a5a60c9422cd</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ExecutePolicy

Executes the specified policy.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=ExecutePolicy
&Version=2011-01-01
&PolicyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>The AutoScaling Group name is required when specifying a policy by its name</Message>
  </Error>
  <RequestId>9d31d319-3e9d-4087-aa25-9020676987b4</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ExitStandby

Moves the specified instances out of the standby state.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=ExitStandby
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - AutoScalingGroup example not found</Message>
  </Error>
  <RequestId>7c07fc73-9db8-404a-872a-892b819dcc7d</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### GetPredictiveScalingForecast

Retrieves the forecast data for a predictive scaling policy.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

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

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Given end time must be after start time</Message>
  </Error>
  <RequestId>f263da02-ac67-4361-807d-d1cd7f9ea1c9</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### LaunchInstances

Launches a specified number of instances in an Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=LaunchInstances
&Version=2011-01-01
&AutoScalingGroupName=example
&RequestedCapacity=1
&ClientToken=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup not found - No such Auto Scaling group: example</Message>
  </Error>
  <RequestId>baaf2b2f-87b9-45ad-8a1a-fe3a1eb9cb30</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutAccountSetting

Modifies an account setting for your account.

```http
POST / HTTP/1.1

Action=PutAccountSetting
&Version=2011-01-01
&Name=example
&Value=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation PutAccountSetting is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>1a29a5ad-7da5-454b-b34f-3e2b148e32be</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### PutLifecycleHook

Creates or updates a lifecycle hook for the specified Auto Scaling group.

```http
POST / HTTP/1.1

Action=PutLifecycleHook
&Version=2011-01-01
&LifecycleHookName=example
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>f45f3b28-1a9d-4ee6-9116-5736cc9129dc</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutNotificationConfiguration

Configures an Auto Scaling group to send notifications when specified events take place.

```http
POST / HTTP/1.1

Action=PutNotificationConfiguration
&Version=2011-01-01
&AutoScalingGroupName=example
&TopicARN=arn%3Aaws%3Aautoscaling%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&NotificationTypes.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>00fdee90-ee0a-479a-85b7-100b3ab7d0bc</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutScalingPolicy

Creates or updates a scaling policy for an Auto Scaling group.

```http
POST / HTTP/1.1

Action=PutScalingPolicy
&Version=2011-01-01
&AutoScalingGroupName=example
&PolicyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>c1263e1e-1484-480c-8684-586a77d3f589</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutScheduledUpdateGroupAction

Creates or updates a scheduled scaling action for an Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=PutScheduledUpdateGroupAction
&Version=2011-01-01
&AutoScalingGroupName=example
&ScheduledActionName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>6de6feb5-c649-4ffd-af05-e674531e338b</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### PutWarmPool

Creates or updates a warm pool for the specified Auto Scaling group.

```http
POST / HTTP/1.1

Action=PutWarmPool
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>6eb91c6d-01a7-4148-b6de-fa9c143b8ecb</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### RecordLifecycleActionHeartbeat

Records a heartbeat for the lifecycle action associated with the specified token or instance.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=RecordLifecycleActionHeartbeat
&Version=2011-01-01
&LifecycleHookName=example
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>No Lifecycle Action found because no token or instance ID is provided</Message>
  </Error>
  <RequestId>10bdaf94-341b-4034-bb86-f2c31c83b4df</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### ResumeProcesses

Resumes the specified suspended auto scaling processes, or all suspended process, for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=ResumeProcesses
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - no such group: example</Message>
  </Error>
  <RequestId>fff5fec1-b83f-4c40-9f17-ea7ca514dcb7</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### RollbackInstanceRefresh

Cancels an instance refresh that is in progress and rolls back any changes that it made.

```http
POST / HTTP/1.1

Action=RollbackInstanceRefresh
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - Auto Scaling group with name example not found</Message>
  </Error>
  <RequestId>4c2eb3e7-2797-43b5-9bf3-10c2a6614181</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SetDesiredCapacity

Sets the size of the specified Auto Scaling group.

```http
POST / HTTP/1.1

Action=SetDesiredCapacity
&Version=2011-01-01
&AutoScalingGroupName=example
&DesiredCapacity=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>06f48a7f-8a82-4002-bb45-e207ee2014a4</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-setdesiredcapacity) for a successful call.

### SetInstanceHealth

Sets the health status of the specified instance.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=SetInstanceHealth
&Version=2011-01-01
&InstanceId=example
&HealthStatus=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Valid instance health states are: [Healthy, Unhealthy].</Message>
  </Error>
  <RequestId>96529a59-b267-4558-8b1a-64fac8b6e714</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SetInstanceProtection

Updates the instance protection settings of the specified instances.

```http
POST / HTTP/1.1

Action=SetInstanceProtection
&Version=2011-01-01
&InstanceIds.member.1=example
&AutoScalingGroupName=example
&ProtectedFromScaleIn=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>cdcdf606-275d-4c6a-a3f3-b6ae484bb3c8</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### StartInstanceRefresh

Starts an instance refresh.

```http
POST / HTTP/1.1

Action=StartInstanceRefresh
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>7efd7a47-618b-423d-bec6-1e2f128e2098</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### SuspendProcesses

Suspends the specified auto scaling processes, or all processes, for the specified Auto Scaling group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=SuspendProcesses
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - no such group: example</Message>
  </Error>
  <RequestId>11b998e9-0a1a-4c04-b5e1-3d20932b60ba</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### TerminateInstanceInAutoScalingGroup

Terminates the specified instance and optionally adjusts the desired group size.

```http
POST / HTTP/1.1

Action=TerminateInstanceInAutoScalingGroup
&Version=2011-01-01
&ShouldDecrementDesiredCapacity=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>Valid requests must contain either the InstanceId or the InstanceIds parameter.</Message>
  </Error>
  <RequestId>4882c1ed-f835-43a1-b5d9-a83a88a4244d</RequestId>
</ErrorResponse>
```

`400` `ValidationError`: the placeholder values failed the operation's input validation.

### UpdateAutoScalingGroup

We strongly recommend that all Auto Scaling groups use launch templates to ensure full functionality for Amazon EC2 Auto Scaling and Amazon EC2.

```http
POST / HTTP/1.1

Action=UpdateAutoScalingGroup
&Version=2011-01-01
&AutoScalingGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://autoscaling.amazonaws.com/doc/2011-01-01/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationError</Code>
    <Message>AutoScalingGroup name not found - null</Message>
  </Error>
  <RequestId>d4f5a4ee-0ed7-4fc3-b2e7-ce3b7fd49414</RequestId>
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
