# Amazon Elastic Compute Cloud

## Wire protocol

- **Protocol:** `ec2Query`
- **SDK id:** `EC2`
- **sigv4 signing name:** `ec2`
- **Endpoint prefix:** `ec2`
- **ARN namespace:** `ec2`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `ec2`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `DescribeVpcs`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:ec2" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded; charset=utf-8' \
  --data-binary 'Action=DescribeVpcs&Version=2016-11-15' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Build a VPC with a subnet and a security group, launch an instance in it, then tear it all down. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateVpc

EC2 answers in XML (the `ec2Query` protocol); the VPC id is used by every later step.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120528Z
Content-Length: 171
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateVpc
&Version=2016-11-15
&CidrBlock=10.20.0.0%2F16
&TagSpecification.1.ResourceType=vpc
&TagSpecification.1.Tag.1.Key=Name
&TagSpecification.1.Tag.1.Value=docs-demo
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 949
date: Mon, 16 Nov 2026 12:05:28 GMT

<CreateVpcResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpc xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <ownerId>000000000000</ownerId>
    <instanceTenancy>default</instanceTenancy>
    <ipv6CidrBlockAssociationSet/>
    <cidrBlockAssociationSet>
      <item>
        <associationId>vpc-cidr-assoc-00000000000000095</associationId>
        <cidrBlock>10.20.0.0/16</cidrBlock>
        <cidrBlockState>
          <state>associated</state>
        </cidrBlockState>
      </item>
    </cidrBlockAssociationSet>
    <isDefault>false</isDefault>
    <tagSet>
      <item>
        <key>Name</key>
        <value>docs-demo</value>
      </item>
    </tagSet>
    <blockPublicAccessStates>
      <internetGatewayBlockMode>off</internetGatewayBlockMode>
    </blockPublicAccessStates>
    <vpcId>vpc-00000000000000094</vpcId>
    <state>pending</state>
    <cidrBlock>10.20.0.0/16</cidrBlock>
    <dhcpOptionsId>dopt-00000000000000003</dhcpOptionsId>
    <dhcpOptionsSource>vpc</dhcpOptionsSource>
  </vpc>
  <requestId>83413c22-3142-47fc-9221-74fa9186a4b9</requestId>
</CreateVpcResponse>
```

### 2. CreateSubnet

The subnet's CIDR must sit inside the VPC's.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120528Z
Content-Length: 119
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateSubnet
&Version=2016-11-15
&AvailabilityZone=us-east-1a
&CidrBlock=10.20.1.0%2F24
&VpcId=vpc-00000000000000094
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 1317
date: Mon, 16 Nov 2026 12:05:28 GMT

<CreateSubnetResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <subnet xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <availabilityZoneId>use1-az1</availabilityZoneId>
    <mapCustomerOwnedIpOnLaunch>false</mapCustomerOwnedIpOnLaunch>
    <ownerId>000000000000</ownerId>
    <assignIpv6AddressOnCreation>false</assignIpv6AddressOnCreation>
    <ipv6CidrBlockAssociationSet/>
    <tagSet/>
    <subnetArn>arn:aws:ec2:us-east-1:000000000000:subnet/subnet-0000000000000009c</subnetArn>
    <enableDns64>false</enableDns64>
    <ipv6Native>false</ipv6Native>
    <privateDnsNameOptionsOnLaunch>
      <hostnameType>ip-name</hostnameType>
      <enableResourceNameDnsARecord>false</enableResourceNameDnsARecord>
      <enableResourceNameDnsAAAARecord>false</enableResourceNameDnsAAAARecord>
    </privateDnsNameOptionsOnLaunch>
    <blockPublicAccessStates>
      <internetGatewayBlockMode>off</internetGatewayBlockMode>
    </blockPublicAccessStates>
    <subnetId>subnet-0000000000000009c</subnetId>
    <state>available</state>
    <vpcId>vpc-00000000000000094</vpcId>
    <cidrBlock>10.20.1.0/24</cidrBlock>
    <availableIpAddressCount>251</availableIpAddressCount>
    <availabilityZone>us-east-1a</availabilityZone>
    <defaultForAz>false</defaultForAz>
    <mapPublicIpOnLaunch>false</mapPublicIpOnLaunch>
  </subnet>
  <requestId>561aae7f-8550-4341-ae95-30b69173b352</requestId>
</CreateSubnetResponse>
```

### 3. CreateSecurityGroup

A security group belongs to one VPC.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120528Z
Content-Length: 128
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateSecurityGroup
&Version=2016-11-15
&GroupDescription=web%20traffic
&GroupName=docs-demo-web
&VpcId=vpc-00000000000000094
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 333
date: Mon, 16 Nov 2026 12:05:28 GMT

<CreateSecurityGroupResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <groupId>sg-0000000000000009e</groupId>
  <securityGroupArn>arn:aws:ec2:us-east-1:000000000000:security-group/sg-0000000000000009e</securityGroupArn>
  <return>true</return>
  <requestId>54fbb016-17e7-455b-a2c4-e9698f278cdb</requestId>
</CreateSecurityGroupResponse>
```

### 4. AuthorizeSecurityGroupIngress

Each rule comes back with its own `securityGroupRuleId`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120528Z
Content-Length: 262
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AuthorizeSecurityGroupIngress
&Version=2016-11-15
&GroupId=sg-0000000000000009e
&IpPermissions.1.IpProtocol=tcp
&IpPermissions.1.FromPort=443
&IpPermissions.1.ToPort=443
&IpPermissions.1.IpRanges.1.Description=https
&IpPermissions.1.IpRanges.1.CidrIp=0.0.0.0%2F0
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 755
date: Mon, 16 Nov 2026 12:05:28 GMT

<AuthorizeSecurityGroupIngressResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <return>true</return>
  <securityGroupRuleSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <securityGroupRuleId>sgr-000000000000000a0</securityGroupRuleId>
      <groupId>sg-0000000000000009e</groupId>
      <groupOwnerId>000000000000</groupOwnerId>
      <isEgress>false</isEgress>
      <ipProtocol>tcp</ipProtocol>
      <fromPort>443</fromPort>
      <toPort>443</toPort>
      <cidrIpv4>0.0.0.0/0</cidrIpv4>
      <description>https</description>
      <tagSet/>
      <securityGroupRuleArn>arn:aws:ec2:us-east-1:000000000000:security-group-rule/sgr-000000000000000a0</securityGroupRuleArn>
    </item>
  </securityGroupRuleSet>
  <requestId>92e1ed27-fac2-4326-9707-e9cd0497885c</requestId>
</AuthorizeSecurityGroupIngressResponse>
```

### 5. RunInstances

The instance starts in `pending` with a private IP from the subnet.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120529Z
Content-Length: 234
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RunInstances
&Version=2016-11-15
&ImageId=ami-0000000000000003c
&InstanceType=t3.micro
&MaxCount=1
&MinCount=1
&SecurityGroupId.1=sg-0000000000000009e
&SubnetId=subnet-0000000000000009c
&ClientToken=bc482057-70e6-46d5-a32e-381f6a0cbca3
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 4425
date: Mon, 16 Nov 2026 12:05:29 GMT

<RunInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <reservationId>r-00000000000000001</reservationId>
  <ownerId>000000000000</ownerId>
  <groupSet/>
  <instancesSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <architecture>x86_64</architecture>
      <blockDeviceMapping/>
      <clientToken>bc482057-70e6-46d5-a32e-381f6a0cbca3</clientToken>
      <ebsOptimized>false</ebsOptimized>
      <enaSupport>true</enaSupport>
      <hypervisor>xen</hypervisor>
      <networkInterfaceSet>
        <item>
          <attachment>
            <attachTime>2026-11-16T12:05:29.041542Z</attachTime>
            <attachmentId>eni-attach-000000000000000a2</attachmentId>
            <deleteOnTermination>true</deleteOnTermination>
            <deviceIndex>0</deviceIndex>
            <status>attaching</status>
            <networkCardIndex>0</networkCardIndex>
          </attachment>
          <description/>
          <groupSet>
            <item>
              <groupId>sg-0000000000000009e</groupId>
              <groupName>docs-demo-web</groupName>
            </item>
          </groupSet>
          <ipv6AddressesSet/>
          <macAddress>02:00:00:00:00:a3</macAddress>
          <networkInterfaceId>eni-000000000000000a3</networkInterfaceId>
          <ownerId>000000000000</ownerId>
          <privateDnsName>ip-10-20-1-4.ec2.internal</privateDnsName>
          <privateIpAddress>10.20.1.4</privateIpAddress>
          <privateIpAddressesSet>
            <item>
              <primary>true</primary>
              <privateDnsName>ip-10-20-1-4.ec2.internal</privateDnsName>
              <privateIpAddress>10.20.1.4</privateIpAddress>
            </item>
          </privateIpAddressesSet>
          <sourceDestCheck>true</sourceDestCheck>
          <status>in-use</status>
          <subnetId>subnet-0000000000000009c</subnetId>
          <vpcId>vpc-00000000000000094</vpcId>
          <interfaceType>interface</interfaceType>
          <ipv4PrefixSet/>
          <ipv6PrefixSet/>
          <operator>
            <managed>false</managed>
            <hiddenByDefault>false</hiddenByDefault>
          </operator>
        </item>
      </networkInterfaceSet>
      <rootDeviceName>/dev/xvda</rootDeviceName>
      <rootDeviceType>ebs</rootDeviceType>
      <groupSet>
        <item>
          <groupId>sg-0000000000000009e</groupId>
… (83 more lines)
```

### 6. DescribeInstances

Poll until the instance reports `running`.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120531Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeInstances
&Version=2016-11-15
&InstanceId.1=i-00000000000000001
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 4747
date: Mon, 16 Nov 2026 12:05:31 GMT

<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <reservationSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <reservationId>r-00000000000000001</reservationId>
      <ownerId>000000000000</ownerId>
      <groupSet/>
      <instancesSet>
        <item>
          <architecture>x86_64</architecture>
          <blockDeviceMapping>
            <item>
              <deviceName>/dev/xvda</deviceName>
              <ebs>
                <attachTime>2026-11-16T12:05:29.040767Z</attachTime>
                <deleteOnTermination>true</deleteOnTermination>
                <status>attached</status>
                <volumeId>vol-000000000000000a1</volumeId>
                <operator>
                  <managed>false</managed>
                  <hiddenByDefault>false</hiddenByDefault>
                </operator>
                <ebsCardIndex>0</ebsCardIndex>
              </ebs>
            </item>
          </blockDeviceMapping>
          <clientToken>bc482057-70e6-46d5-a32e-381f6a0cbca3</clientToken>
          <ebsOptimized>false</ebsOptimized>
          <enaSupport>true</enaSupport>
          <hypervisor>xen</hypervisor>
          <networkInterfaceSet>
            <item>
              <attachment>
                <attachTime>2026-11-16T12:05:29.041542Z</attachTime>
                <attachmentId>eni-attach-000000000000000a2</attachmentId>
                <deleteOnTermination>true</deleteOnTermination>
                <deviceIndex>0</deviceIndex>
                <status>attached</status>
                <networkCardIndex>0</networkCardIndex>
              </attachment>
              <description/>
              <groupSet>
                <item>
                  <groupId>sg-0000000000000009e</groupId>
                  <groupName>docs-demo-web</groupName>
                </item>
              </groupSet>
              <ipv6AddressesSet/>
              <macAddress>02:00:00:00:00:a3</macAddress>
              <networkInterfaceId>eni-000000000000000a3</networkInterfaceId>
              <ownerId>000000000000</ownerId>
              <privateDnsName>ip-10-20-1-4.ec2.internal</privateDnsName>
              <privateIpAddress>10.20.1.4</privateIpAddress>
              <privateIpAddressesSet>
                <item>
                  <primary>true</primary>
                  <privateDnsName>ip-10-20-1-4.ec2.internal</privateDnsName>
                  <privateIpAddress>10.20.1.4</privateIpAddress>
                </item>
              </privateIpAddressesSet>
              <sourceDestCheck>true</sourceDestCheck>
… (98 more lines)
```

### 7. TerminateInstances

Termination reports the state change from `running` to `shutting-down`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120531Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TerminateInstances
&Version=2016-11-15
&InstanceId.1=i-00000000000000001
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 434
date: Mon, 16 Nov 2026 12:05:31 GMT

<TerminateInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instancesSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <instanceId>i-00000000000000001</instanceId>
      <currentState>
        <code>32</code>
        <name>shutting-down</name>
      </currentState>
      <previousState>
        <code>16</code>
        <name>running</name>
      </previousState>
    </item>
  </instancesSet>
  <requestId>924fcea9-ef20-46a9-aa1a-c556cb4c5de3</requestId>
</TerminateInstancesResponse>
```

### 8. DescribeInstances

Poll until the instance is `terminated`, which frees the subnet.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120533Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeInstances
&Version=2016-11-15
&InstanceId.1=i-00000000000000001
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 3447
date: Mon, 16 Nov 2026 12:05:33 GMT

<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <reservationSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <reservationId>r-00000000000000001</reservationId>
      <ownerId>000000000000</ownerId>
      <groupSet/>
      <instancesSet>
        <item>
          <architecture>x86_64</architecture>
          <blockDeviceMapping>
            <item>
              <deviceName>/dev/xvda</deviceName>
              <ebs>
                <attachTime>2026-11-16T12:05:29.040767Z</attachTime>
                <deleteOnTermination>true</deleteOnTermination>
                <status>attached</status>
                <volumeId>vol-000000000000000a1</volumeId>
                <operator>
                  <managed>false</managed>
                  <hiddenByDefault>false</hiddenByDefault>
                </operator>
                <ebsCardIndex>0</ebsCardIndex>
              </ebs>
            </item>
          </blockDeviceMapping>
          <clientToken>bc482057-70e6-46d5-a32e-381f6a0cbca3</clientToken>
          <ebsOptimized>false</ebsOptimized>
          <enaSupport>true</enaSupport>
          <hypervisor>xen</hypervisor>
          <networkInterfaceSet/>
          <rootDeviceName>/dev/xvda</rootDeviceName>
          <rootDeviceType>ebs</rootDeviceType>
          <groupSet/>
          <sourceDestCheck>true</sourceDestCheck>
          <stateReason>
            <code>Client.UserInitiatedShutdown</code>
            <message>Client.UserInitiatedShutdown: User initiated shutdown</message>
          </stateReason>
          <tagSet/>
          <virtualizationType>hvm</virtualizationType>
          <cpuOptions>
            <coreCount>1</coreCount>
            <threadsPerCore>2</threadsPerCore>
          </cpuOptions>
          <capacityReservationSpecification>
            <capacityReservationPreference>open</capacityReservationPreference>
          </capacityReservationSpecification>
          <hibernationOptions>
            <configured>false</configured>
          </hibernationOptions>
          <metadataOptions>
            <state>applied</state>
            <httpTokens>required</httpTokens>
            <httpPutResponseHopLimit>2</httpPutResponseHopLimit>
            <httpEndpoint>enabled</httpEndpoint>
            <httpProtocolIpv6>disabled</httpProtocolIpv6>
            <instanceMetadataTags>disabled</instanceMetadataTags>
            <httpProtocolIpv4>enabled</httpProtocolIpv4>
          </metadataOptions>
          <enclaveOptions>
… (51 more lines)
```

### 9. DeleteSubnet

With the instance gone the subnet can be removed.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120533Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteSubnet
&Version=2016-11-15
&SubnetId=subnet-0000000000000009c
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 173
date: Mon, 16 Nov 2026 12:05:33 GMT

<DeleteSubnetResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <return>true</return>
  <requestId>8b4a8e87-089c-4f86-bc01-75cfa71749a4</requestId>
</DeleteSubnetResponse>
```

### 10. DeleteVpc

Deleting a VPC that still holds a security group other than the default fails with `DependencyViolation`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120533Z
Content-Length: 63
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteVpc
&Version=2016-11-15
&VpcId=vpc-00000000000000094
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8
content-length: 292
date: Mon, 16 Nov 2026 12:05:33 GMT

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>DependencyViolation</Code>
      <Message>The vpc 'vpc-00000000000000094' has dependencies and cannot be deleted.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000001</RequestID>
</Response>
```

### 11. DeleteSecurityGroup

Remove the group ...

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120533Z
Content-Length: 74
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteSecurityGroup
&Version=2016-11-15
&GroupId=sg-0000000000000009e
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 226
date: Mon, 16 Nov 2026 12:05:33 GMT

<DeleteSecurityGroupResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <return>true</return>
  <groupId>sg-0000000000000009e</groupId>
  <requestId>2b305faf-04b3-43ee-a5d7-e1c1e3bd0a30</requestId>
</DeleteSecurityGroupResponse>
```

### 12. DeleteVpc

... and the VPC deletes cleanly.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120533Z
Content-Length: 63
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/ec2/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteVpc
&Version=2016-11-15
&VpcId=vpc-00000000000000094
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8
content-length: 167
date: Mon, 16 Nov 2026 12:05:33 GMT

<DeleteVpcResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <return>true</return>
  <requestId>ffd6f896-23c9-499b-9b2f-9055e87f8e7c</requestId>
</DeleteVpcResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 807 operations, 199 accepted the placeholder call, 216 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AcceptAddressTransfer

Accepts an Elastic IP address transfer.

```http
POST / HTTP/1.1

Action=AcceptAddressTransfer
&Version=2016-11-15
&Address=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value 'example' for address. It must be an IPv4 address</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000002</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AcceptCapacityReservationBillingOwnership

Accepts a request to assign billing of the available capacity of a shared Capacity Reservation to your account.

```http
POST / HTTP/1.1

Action=AcceptCapacityReservationBillingOwnership
&Version=2016-11-15
&CapacityReservationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000003</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### AcceptReservedInstancesExchangeQuote

Accepts the Convertible Reserved Instance exchange quote described in the GetReservedInstancesExchangeQuote call.

```http
POST / HTTP/1.1

Action=AcceptReservedInstancesExchangeQuote
&Version=2016-11-15
&ReservedInstanceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidReservedInstancesID.NotFound</Code>
      <Message>The reserved-instances ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000004</RequestID>
</Response>
```

`400` `InvalidReservedInstancesID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AcceptTransitGatewayClientVpnAttachment

Accepts a Transit Gateway attachment request for a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=AcceptTransitGatewayClientVpnAttachment
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayAttachmentID.Malformed</Code>
      <Message>Invalid Transit Gateway Attachment id.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000005</RequestID>
</Response>
```

`400` `InvalidTransitGatewayAttachmentID.Malformed`: the placeholder values failed the operation's input validation.

### AcceptTransitGatewayMulticastDomainAssociations

Accepts a request to associate subnets with a transit gateway multicast domain.

```http
POST / HTTP/1.1

Action=AcceptTransitGatewayMulticastDomainAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter TransitGatewayMulticastDomainId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000006</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### AcceptTransitGatewayPeeringAttachment

Accepts a transit gateway peering attachment request.

```http
POST / HTTP/1.1

Action=AcceptTransitGatewayPeeringAttachment
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayAttachmentID.Malformed</Code>
      <Message>Invalid Transit Gateway Attachment id.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000007</RequestID>
</Response>
```

`400` `InvalidTransitGatewayAttachmentID.Malformed`: the placeholder values failed the operation's input validation.

### AcceptTransitGatewayVpcAttachment

Accepts a request to attach a VPC to a transit gateway.

```http
POST / HTTP/1.1

Action=AcceptTransitGatewayVpcAttachment
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayAttachmentID.Malformed</Code>
      <Message>Invalid Transit Gateway Attachment id.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000008</RequestID>
</Response>
```

`400` `InvalidTransitGatewayAttachmentID.Malformed`: the placeholder values failed the operation's input validation.

### AcceptVpcEndpointConnections

Accepts connection requests to your VPC endpoint service.

```http
POST / HTTP/1.1

Action=AcceptVpcEndpointConnections
&Version=2016-11-15
&ServiceId=example
&VpcEndpointId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcEndpointServiceId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000009</RequestID>
</Response>
```

`400` `InvalidVpcEndpointServiceId.Malformed`: the placeholder values failed the operation's input validation.

### AcceptVpcPeeringConnection

Accept a VPC peering connection request.

```http
POST / HTTP/1.1

Action=AcceptVpcPeeringConnection
&Version=2016-11-15
&VpcPeeringConnectionId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation AcceptVpcPeeringConnection is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000000a</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### AdvertiseByoipCidr

Advertises an IPv4 or IPv6 address range that is provisioned for use with your Amazon Web Services resources through bring your own IP addresses (BYOIP).

```http
POST / HTTP/1.1

Action=AdvertiseByoipCidr
&Version=2016-11-15
&Cidr=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>The CIDR (example) is not valid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000000b</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AllocateAddress

Allocates an Elastic IP address to your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=AllocateAddress
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<AllocateAddressResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <allocationId>eipalloc-000000000000000ad</allocationId>
  <publicIpv4Pool>amazon</publicIpv4Pool>
  <networkBorderGroup>us-east-1</networkBorderGroup>
  <domain>vpc</domain>
  <publicIp>54.0.0.3</publicIp>
  <requestId>cf8053fd-fc70-42c0-8b96-357638a3765e</requestId>
</AllocateAddressResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### AllocateHosts

Allocates a Dedicated Host to your account.

```http
POST / HTTP/1.1

Action=AllocateHosts
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>Either availabilityZone or availabilityZoneId must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000000c</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### AllocateIpamPoolCidr

Allocate a CIDR from an IPAM pool.

```http
POST / HTTP/1.1

Action=AllocateIpamPoolCidr
&Version=2016-11-15
&IpamPoolId=example
&ClientToken=fb56d414-6ad9-447b-8d41-47902ba0320d
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation AllocateIpamPoolCidr is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000000d</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ApplySecurityGroupsToClientVpnTargetNetwork

Applies a security group to the association between the target network and the Client VPN endpoint.

```http
POST / HTTP/1.1

Action=ApplySecurityGroupsToClientVpnTargetNetwork
&Version=2016-11-15
&ClientVpnEndpointId=example
&VpcId=example
&SecurityGroupId.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ApplySecurityGroupsToClientVpnTargetNetwork is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000000e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### AssignIpv6Addresses

Assigns the specified IPv6 addresses to the specified network interface.

```http
POST / HTTP/1.1

Action=AssignIpv6Addresses
&Version=2016-11-15
&NetworkInterfaceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkInterfaceId.Malformed</Code>
      <Message>The interface ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000000f</RequestID>
</Response>
```

`400` `InvalidNetworkInterfaceId.Malformed`: the placeholder values failed the operation's input validation.

### AssignPrivateIpAddresses

Assigns the specified secondary private IP addresses to the specified network interface.

```http
POST / HTTP/1.1

Action=AssignPrivateIpAddresses
&Version=2016-11-15
&NetworkInterfaceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation AssignPrivateIpAddresses is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000010</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### AssignPrivateNatGatewayAddress

Assigns private IPv4 addresses to a private NAT gateway.

```http
POST / HTTP/1.1

Action=AssignPrivateNatGatewayAddress
&Version=2016-11-15
&NatGatewayId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation AssignPrivateNatGatewayAddress is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000011</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### AssociateAddress

Associates an Elastic IP address, or carrier IP address (for instances that are in subnets in Wavelength Zones) with an instance or a network interface.

```http
POST / HTTP/1.1

Action=AssociateAddress
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter allocationId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000012</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### AssociateApplicationStatusCheck

Associates an application status check with instances or tags .

```http
POST / HTTP/1.1

Action=AssociateApplicationStatusCheck
&Version=2016-11-15
&ApplicationStatusCheckId=example
&ClientToken=8f988866-383c-46c1-b945-ff99f7cb8e08
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidApplicationStatusCheckId.Malformed</Code>
      <Message>The application-status-check ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000013</RequestID>
</Response>
```

`400` `InvalidApplicationStatusCheckId.Malformed`: the placeholder values failed the operation's input validation.

### AssociateCapacityReservationBillingOwner

Initiates a request to assign billing of the unused capacity of a shared Capacity Reservation to a consumer account that is consolidated under the same Amazon Web Services organizations payer account.

```http
POST / HTTP/1.1

Action=AssociateCapacityReservationBillingOwner
&Version=2016-11-15
&CapacityReservationId=example
&UnusedReservationBillingOwnerId=100000000000
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation AssociateCapacityReservationBillingOwner is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000014</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### AssociateClientVpnTargetNetwork

Associates a target network with a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=AssociateClientVpnTargetNetwork
&Version=2016-11-15
&ClientVpnEndpointId=example
&ClientToken=c4c13635-0ded-4fea-b8bc-adbcdfa56ee3
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidClientVpnEndpointId.NotFound</Code>
      <Message>Endpoint example does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000015</RequestID>
</Response>
```

`400` `InvalidClientVpnEndpointId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AssociateDhcpOptions

Associates a set of DHCP options (that you've previously created) with the specified VPC, or associates no DHCP options with the VPC.

```http
POST / HTTP/1.1

Action=AssociateDhcpOptions
&Version=2016-11-15
&DhcpOptionsId=example
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidDhcpOptionsId.Malformed</Code>
      <Message>The dhcp-options ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000016</RequestID>
</Response>
```

`400` `InvalidDhcpOptionsId.Malformed`: the placeholder values failed the operation's input validation.

### AssociateEnclaveCertificateIamRole

Associates an Identity and Access Management (IAM) role with an Certificate Manager (ACM) certificate.

```http
POST / HTTP/1.1

Action=AssociateEnclaveCertificateIamRole
&Version=2016-11-15
&CertificateArn=arn%3Aaws%3Aec2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&RoleArn=arn%3Aaws%3Aec2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCertificateArn.Malformed</Code>
      <Message>The request must contain a valid certificate arn</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000017</RequestID>
</Response>
```

`400` `InvalidCertificateArn.Malformed`: the placeholder values failed the operation's input validation.

### AssociateIamInstanceProfile

Associates an IAM instance profile with a running or stopped instance.

```http
POST / HTTP/1.1

Action=AssociateIamInstanceProfile
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'iamInstanceProfile' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000018</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### AssociateInstanceEventWindow

Associates one or more targets with an event window.

```http
POST / HTTP/1.1

Action=AssociateInstanceEventWindow
&Version=2016-11-15
&InstanceEventWindowId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'associationTarget' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000019</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### AssociateIpamByoasn

Associates your Autonomous System Number (ASN) with a BYOIP CIDR that you own in the same Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=AssociateIpamByoasn
&Version=2016-11-15
&Asn=example
&Cidr=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value 'example' for Asn. It must be a public 2-byte or 4-byte ASN</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000001a</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AssociateIpamResourceDiscovery

Associates an IPAM resource discovery with an Amazon VPC IPAM.

```http
POST / HTTP/1.1

Action=AssociateIpamResourceDiscovery
&Version=2016-11-15
&IpamId=example
&IpamResourceDiscoveryId=example
&ClientToken=42ba3ecf-f37c-477b-b809-1577ebbee828
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamResourceDiscoveryId.Malformed</Code>
      <Message>The ipam-resource-discovery ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000001b</RequestID>
</Response>
```

`400` `InvalidIpamResourceDiscoveryId.Malformed`: the placeholder values failed the operation's input validation.

### AssociateNatGatewayAddress

Associates Elastic IP addresses (EIPs) and private IPv4 addresses with a public NAT gateway.

```http
POST / HTTP/1.1

Action=AssociateNatGatewayAddress
&Version=2016-11-15
&NatGatewayId=example
&AllocationId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NatGatewayMalformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000001c</RequestID>
</Response>
```

`400` `NatGatewayMalformed`: the placeholder values failed the operation's input validation.

### AssociateRouteServer

Associates a route server with a VPC to enable dynamic route updates.

```http
POST / HTTP/1.1

Action=AssociateRouteServer
&Version=2016-11-15
&RouteServerId=example
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000001d</RequestID>
</Response>
```

`400` `InvalidRouteServerId.Malformed`: the placeholder values failed the operation's input validation.

### AssociateRouteTable

Associates a subnet in your VPC or an internet gateway or virtual private gateway attached to your VPC with a route table in your VPC.

```http
POST / HTTP/1.1

Action=AssociateRouteTable
&Version=2016-11-15
&RouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either the parameter subnetId or gatewayId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000001e</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### AssociateSecurityGroupVpc

Associates a security group with another VPC in the same Region.

```http
POST / HTTP/1.1

Action=AssociateSecurityGroupVpc
&Version=2016-11-15
&GroupId=example
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000001f</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AssociateSubnetCidrBlock

Associates a CIDR block with your subnet.

```http
POST / HTTP/1.1

Action=AssociateSubnetCidrBlock
&Version=2016-11-15
&SubnetId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSubnetID.NotFound</Code>
      <Message>The subnet ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000020</RequestID>
</Response>
```

`400` `InvalidSubnetID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AssociateTransitGatewayMulticastDomain

Associates the specified subnets and transit gateway attachments with the specified transit gateway multicast domain.

```http
POST / HTTP/1.1

Action=AssociateTransitGatewayMulticastDomain
&Version=2016-11-15
&TransitGatewayMulticastDomainId=example
&TransitGatewayAttachmentId=example
&SubnetIds.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayMulticastDomainId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000021</RequestID>
</Response>
```

`400` `InvalidTransitGatewayMulticastDomainId.Malformed`: the placeholder values failed the operation's input validation.

### AssociateTransitGatewayPolicyTable

Associates the specified transit gateway attachment with a transit gateway policy table.

```http
POST / HTTP/1.1

Action=AssociateTransitGatewayPolicyTable
&Version=2016-11-15
&TransitGatewayPolicyTableId=example
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayPolicyTableId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000022</RequestID>
</Response>
```

`400` `InvalidTransitGatewayPolicyTableId.Malformed`: the placeholder values failed the operation's input validation.

### AssociateTransitGatewayRouteTable

Associates the specified attachment with the specified transit gateway route table.

```http
POST / HTTP/1.1

Action=AssociateTransitGatewayRouteTable
&Version=2016-11-15
&TransitGatewayRouteTableId=example
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableId.Malformed</Code>
      <Message>Invalid Transit Gateway Route Table id example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000023</RequestID>
</Response>
```

`400` `InvalidRouteTableId.Malformed`: the placeholder values failed the operation's input validation.

### AssociateTrunkInterface

Associates a branch network interface with a trunk network interface.

```http
POST / HTTP/1.1

Action=AssociateTrunkInterface
&Version=2016-11-15
&BranchInterfaceId=example
&TrunkInterfaceId=example
&ClientToken=d18c0a3a-109f-4b63-9983-b20fa1b26b3a
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>OperationNotPermitted</Code>
      <Message>User 000000000000 is not permitted to perform this operation</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000024</RequestID>
</Response>
```

`400` `OperationNotPermitted`.

### AssociateVpcCidrBlock

Associates a CIDR block with your VPC.

```http
POST / HTTP/1.1

Action=AssociateVpcCidrBlock
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter cidrBlock, ipv6CidrBlock, amazonProvidedIpv6CidrBlock, ipv4IpamPoolId or ipv6IpamPoolId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000025</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### AttachClassicLinkVpc

This action is deprecated.

```http
POST / HTTP/1.1

Action=AttachClassicLinkVpc
&Version=2016-11-15
&InstanceId=example
&VpcId=example
&SecurityGroupId.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation AttachClassicLinkVpc is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000026</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### AttachImageWatermark

Attaches a watermark to a non-public AMI.

```http
POST / HTTP/1.1

Action=AttachImageWatermark
&Version=2016-11-15
&ImageId=example
&WatermarkName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAMIID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000027</RequestID>
</Response>
```

`400` `InvalidAMIID.Malformed`: the placeholder values failed the operation's input validation.

### AttachInternetGateway

Attaches an internet gateway or a virtual private gateway to a VPC, enabling connectivity between the internet and the VPC.

```http
POST / HTTP/1.1

Action=AttachInternetGateway
&Version=2016-11-15
&InternetGatewayId=example
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInternetGatewayID.NotFound</Code>
      <Message>The internetGateway ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000028</RequestID>
</Response>
```

`400` `InvalidInternetGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AttachNetworkInterface

Attaches a network interface to an instance.

```http
POST / HTTP/1.1

Action=AttachNetworkInterface
&Version=2016-11-15
&NetworkInterfaceId=example
&InstanceId=example
&DeviceIndex=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkInterfaceId.Malformed</Code>
      <Message>The interface ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000029</RequestID>
</Response>
```

`400` `InvalidNetworkInterfaceId.Malformed`: the placeholder values failed the operation's input validation.

### AttachVerifiedAccessTrustProvider

Attaches the specified Amazon Web Services Verified Access trust provider to the specified Amazon Web Services Verified Access instance.

```http
POST / HTTP/1.1

Action=AttachVerifiedAccessTrustProvider
&Version=2016-11-15
&VerifiedAccessInstanceId=example
&VerifiedAccessTrustProviderId=example
&ClientToken=0e0a68b7-23ac-48fe-bd08-24a27b456809
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVerifiedAccessInstanceId.NotFound</Code>
      <Message>The verified-access-instance ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000002a</RequestID>
</Response>
```

`400` `InvalidVerifiedAccessInstanceId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AttachVolume

Attaches an Amazon EBS volume to a running or stopped instance, and exposes it to the instance with the specified device name.

```http
POST / HTTP/1.1

Action=AttachVolume
&Version=2016-11-15
&Device=example
&InstanceId=example
&VolumeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVolumeID.Malformed</Code>
      <Message>The volume ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000002b</RequestID>
</Response>
```

`400` `InvalidVolumeID.Malformed`: the placeholder values failed the operation's input validation.

### AttachVpnGateway

Attaches an available virtual private gateway to a VPC.

```http
POST / HTTP/1.1

Action=AttachVpnGateway
&Version=2016-11-15
&VpcId=example
&VpnGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpnGatewayID.NotFound</Code>
      <Message>The vpnGateway ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000002c</RequestID>
</Response>
```

`400` `InvalidVpnGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AuthorizeClientVpnIngress

Adds an ingress authorization rule to a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=AuthorizeClientVpnIngress
&Version=2016-11-15
&ClientVpnEndpointId=example
&TargetNetworkCidr=example
&ClientToken=4e2843d0-e68e-4977-8057-9378e69a5fcf
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation AuthorizeClientVpnIngress is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000002d</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### AuthorizeSecurityGroupEgress

Adds the specified outbound (egress) rules to a security group.

```http
POST / HTTP/1.1

Action=AuthorizeSecurityGroupEgress
&Version=2016-11-15
&GroupId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter ipPermissions</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000002e</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### AuthorizeSecurityGroupIngress

Adds the specified inbound (ingress) rules to a security group.

```http
POST / HTTP/1.1

Action=AuthorizeSecurityGroupIngress
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter ipPermissions</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000002f</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation. See [the walkthrough](#4-authorizesecuritygroupingress) for a successful call.

### BatchModifyIpamRoutingPolicyRegistrations

Modifies multiple routing policy registrations in a single operation.

```http
POST / HTTP/1.1

Action=BatchModifyIpamRoutingPolicyRegistrations
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
&DeltaJson=example
&ClientToken=a2ebe748-ccb8-4c63-b723-c6888e5455b8
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation BatchModifyIpamRoutingPolicyRegistrations is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000030</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### BundleInstance

Bundles an Amazon instance store-backed Windows instance.

```http
POST / HTTP/1.1

Action=BundleInstance
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'storage' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000031</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CancelBundleTask

Cancels a bundling operation for an instance store-backed Windows instance.

```http
POST / HTTP/1.1

Action=CancelBundleTask
&Version=2016-11-15
&BundleId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>UnsupportedOperation</Code>
      <Message>CancelBundleTask API is not supported.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000032</RequestID>
</Response>
```

`400` `UnsupportedOperation`.

### CancelCapacityReservation

Cancels the specified Capacity Reservation, releases the reserved capacity, and changes the Capacity Reservation's state to cancelled .

```http
POST / HTTP/1.1

Action=CancelCapacityReservation
&Version=2016-11-15
&CapacityReservationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000033</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### CancelCapacityReservationFleets

Cancels one or more Capacity Reservation Fleets.

```http
POST / HTTP/1.1

Action=CancelCapacityReservationFleets
&Version=2016-11-15
&CapacityReservationFleetId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationFleetId.Malformed</Code>
      <Message>Capacity Reservation Fleet ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000034</RequestID>
</Response>
```

`400` `InvalidCapacityReservationFleetId.Malformed`: the placeholder values failed the operation's input validation.

### CancelConversionTask

Cancels an active conversion task.

```http
POST / HTTP/1.1

Action=CancelConversionTask
&Version=2016-11-15
&ConversionTaskId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidConversionTaskId</Code>
      <Message>Parameter conversion-task-id (example) has an invalid format.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000035</RequestID>
</Response>
```

`400` `InvalidConversionTaskId`.

### CancelDeclarativePoliciesReport

Cancels the generation of an account status report.

```http
POST / HTTP/1.1

Action=CancelDeclarativePoliciesReport
&Version=2016-11-15
&ReportId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidDeclarativePoliciesReportId.Malformed</Code>
      <Message>The declarative-policies-report ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000036</RequestID>
</Response>
```

`400` `InvalidDeclarativePoliciesReportId.Malformed`: the placeholder values failed the operation's input validation.

### CancelExportTask

Cancels an active export task.

```http
POST / HTTP/1.1

Action=CancelExportTask
&Version=2016-11-15
&ExportTaskId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidID</Code>
      <Message>The ID 'example' is not valid</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000037</RequestID>
</Response>
```

`400` `InvalidID`.

### CancelImageLaunchPermission

Removes your Amazon Web Services account from the launch permissions for the specified AMI.

```http
POST / HTTP/1.1

Action=CancelImageLaunchPermission
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CancelImageLaunchPermission is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000038</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CancelImportTask

Cancels an in-process import virtual machine or import snapshot task.

```http
POST / HTTP/1.1

Action=CancelImportTask
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter importTaskId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000039</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CancelReservedInstancesListing

Cancels the specified Reserved Instance listing in the Reserved Instance Marketplace.

```http
POST / HTTP/1.1

Action=CancelReservedInstancesListing
&Version=2016-11-15
&ReservedInstancesListingId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInput</Code>
      <Message> Invalid value for 'listingId'</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000003a</RequestID>
</Response>
```

`400` `InvalidInput`: the placeholder values failed the operation's input validation.

### CancelSpotFleetRequests

Cancels the specified Spot Fleet requests.

```http
POST / HTTP/1.1

Action=CancelSpotFleetRequests
&Version=2016-11-15
&SpotFleetRequestId.1=example
&TerminateInstances=false
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CancelSpotFleetRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <successfulFleetRequestSet/>
  <unsuccessfulFleetRequestSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <error>
        <code>fleetRequestIdMalformed</code>
        <message>Invalid id: &quot;example&quot;</message>
      </error>
      <spotFleetRequestId>example</spotFleetRequestId>
    </item>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CancelSpotInstanceRequests

Cancels one or more Spot Instance requests.

```http
POST / HTTP/1.1

Action=CancelSpotInstanceRequests
&Version=2016-11-15
&SpotInstanceRequestId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSpotInstanceRequestID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000003b</RequestID>
</Response>
```

`400` `InvalidSpotInstanceRequestID.Malformed`: the placeholder values failed the operation's input validation.

### ConfirmProductInstance

Determines whether a product code is associated with an instance.

```http
POST / HTTP/1.1

Action=ConfirmProductInstance
&Version=2016-11-15
&InstanceId=example
&ProductCode=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>AuthFailure</Code>
      <Message>Not authorised for product code example</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000003c</RequestID>
</Response>
```

`400` `AuthFailure`.

### CopyFpgaImage

Copies the specified Amazon FPGA Image (AFI) to the current Region.

```http
POST / HTTP/1.1

Action=CopyFpgaImage
&Version=2016-11-15
&SourceFpgaImageId=example
&SourceRegion=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidFpgaImageID.Malformed</Code>
      <Message>Image ID 'example' is invalid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000003d</RequestID>
</Response>
```

`400` `InvalidFpgaImageID.Malformed`: the placeholder values failed the operation's input validation.

### CopyImage

Initiates an AMI copy operation.

```http
POST / HTTP/1.1

Action=CopyImage
&Version=2016-11-15
&ClientToken=18f52fca-6c58-4067-b85b-1563441b06ac
&Name=example
&SourceImageId=example
&SourceRegion=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CopyImage is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000003e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CopySnapshot

Creates an exact copy of an Amazon EBS snapshot.

```http
POST / HTTP/1.1

Action=CopySnapshot
&Version=2016-11-15
&SourceRegion=example
&SourceSnapshotId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>The specified snapshotId is invalid</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000003f</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CopyVolumes

Creates a crash-consistent, point-in-time copy of an existing Amazon EBS volume within the same Availability Zone.

```http
POST / HTTP/1.1

Action=CopyVolumes
&Version=2016-11-15
&SourceVolumeId=example
&ClientToken=e3025b6f-e027-46b6-91c4-2607d6ad2c89
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVolumeID.Malformed</Code>
      <Message>The volume ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000040</RequestID>
</Response>
```

`400` `InvalidVolumeID.Malformed`: the placeholder values failed the operation's input validation.

### CreateApplicationStatusCheck

Creates an application status check for monitoring the health of applications running on your instances.

```http
POST / HTTP/1.1

Action=CreateApplicationStatusCheck
&Version=2016-11-15
&Protocol=http
&Port=1
&ClientToken=8fdeeff3-edd4-470e-bb4d-e1be849aba3c
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateApplicationStatusCheckResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <applicationStatusCheck xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <applicationStatusCheckId>asc-000000000000000ae</applicationStatusCheckId>
    <aggregation>included</aggregation>
    <healthCheckPathSet/>
    <protocol>http</protocol>
    <port>1</port>
    <path>/</path>
    <deviceIndex>0</deviceIndex>
    <ipVersion>ipv4</ipVersion>
… (13 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateCapacityManagerDataExport

Creates a new data export configuration for EC2 Capacity Manager.

```http
POST / HTTP/1.1

Action=CreateCapacityManagerDataExport
&Version=2016-11-15
&S3BucketName=example
&Schedule=hourly
&OutputFormat=csv
&ClientToken=4be9604b-8e20-4fbf-9cef-72da29f1b172
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateCapacityManagerDataExportResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityManagerDataExportId>cmde-000000000000000af</capacityManagerDataExportId>
  <requestId>c8412ee5-fef9-437f-ac6b-eb7ffde6a3ed</requestId>
</CreateCapacityManagerDataExportResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateCapacityReservation

Creates a new Capacity Reservation with the specified attributes.

```http
POST / HTTP/1.1

Action=CreateCapacityReservation
&Version=2016-11-15
&InstanceType=example
&InstancePlatform=Linux%2FUNIX
&InstanceCount=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>Either availabilityZone or availabilityZoneId must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000041</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateCapacityReservationBySplitting

Create a new Capacity Reservation by splitting the capacity of the source Capacity Reservation.

```http
POST / HTTP/1.1

Action=CreateCapacityReservationBySplitting
&Version=2016-11-15
&ClientToken=7fdf9613-af51-4379-807a-8763f4dd2f65
&SourceCapacityReservationId=example
&InstanceCount=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000042</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### CreateCapacityReservationCancellationQuote

Generates a cancellation quote for a future-dated Capacity Reservation that is within its commitment duration.

```http
POST / HTTP/1.1

Action=CreateCapacityReservationCancellationQuote
&Version=2016-11-15
&CapacityReservationId=example
&ClientToken=d72db02b-252a-4482-a4cd-ab0bf99fca7f
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000043</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### CreateCapacityReservationDateChangeQuote

Generates a quote for changing the start date of a future-dated Capacity Reservation that has not yet been delivered.

```http
POST / HTTP/1.1

Action=CreateCapacityReservationDateChangeQuote
&Version=2016-11-15
&CapacityReservationId=example
&NewStartDate=2026-11-16T12%3A11%3A31Z
&ClientToken=9bd5c620-7699-411e-8f19-ee32a5ee69b4
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateCapacityReservationDateChangeQuote is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000044</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateCapacityReservationFleet

Creates a Capacity Reservation Fleet.

```http
POST / HTTP/1.1

Action=CreateCapacityReservationFleet
&Version=2016-11-15
&ClientToken=450fe4f0-789f-4b43-8d8b-b34863c96f2c
&TotalTargetCapacity=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>instanceTypeSpecifications must name at least one instance type for which to reserve capacity</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000045</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateCarrierGateway

Creates a carrier gateway.

```http
POST / HTTP/1.1

Action=CreateCarrierGateway
&Version=2016-11-15
&VpcId=example
&ClientToken=9e240fab-508c-441d-a7d4-5b48e2efeeda
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcId.Malformed</Code>
      <Message>The vpc ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000046</RequestID>
</Response>
```

`400` `InvalidVpcId.Malformed`: the placeholder values failed the operation's input validation.

### CreateClientVpnEndpoint

Creates a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=CreateClientVpnEndpoint
&Version=2016-11-15
&ServerCertificateArn=arn%3Aaws%3Aec2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ClientToken=15d87494-9aa5-4de7-893c-fe01347fbdca
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'authenticationOptions' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000047</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CreateClientVpnRoute

Adds a route to a network to a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=CreateClientVpnRoute
&Version=2016-11-15
&ClientVpnEndpointId=example
&DestinationCidrBlock=example
&ClientToken=f1c20c19-1101-453e-b762-d79fd120e102
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter destinationCidrBlock is invalid. This is not a valid CIDR block.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000048</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateCoipCidr

Creates a range of customer-owned IP addresses.

```http
POST / HTTP/1.1

Action=CreateCoipCidr
&Version=2016-11-15
&Cidr=example
&CoipPoolId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPoolID.Malformed</Code>
      <Message>The pool ID 'example' is invalid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000049</RequestID>
</Response>
```

`400` `InvalidPoolID.Malformed`: the placeholder values failed the operation's input validation.

### CreateCoipPool

Creates a pool of customer-owned IP (CoIP) addresses.

```http
POST / HTTP/1.1

Action=CreateCoipPool
&Version=2016-11-15
&LocalGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayRouteTableID.Malformed</Code>
      <Message>The local-gateway-route-table ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000004a</RequestID>
</Response>
```

`400` `InvalidLocalGatewayRouteTableID.Malformed`: the placeholder values failed the operation's input validation.

### CreateCustomerGateway

Provides information to Amazon Web Services about your customer gateway device.

```http
POST / HTTP/1.1

Action=CreateCustomerGateway
&Version=2016-11-15
&Type=ipsec.1
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateCustomerGatewayResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <customerGateway xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <tagSet/>
    <customerGatewayId>cgw-000000000000000b0</customerGatewayId>
    <state>available</state>
    <type>ipsec.1</type>
    <bgpAsn>65000</bgpAsn>
  </customerGateway>
  <requestId>1ee5c1d2-9f2c-4f35-9e27-bd7a5e843e47</requestId>
</CreateCustomerGatewayResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateDefaultSubnet

Creates a default subnet with a size /20 IPv4 CIDR block in the specified Availability Zone in your default VPC.

```http
POST / HTTP/1.1

Action=CreateDefaultSubnet
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateDefaultSubnet is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000004b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateDefaultVpc

Creates a default VPC with a size /16 IPv4 CIDR block and a default subnet in each Availability Zone.

```http
POST / HTTP/1.1

Action=CreateDefaultVpc
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>DefaultVpcAlreadyExists</Code>
      <Message>A Default VPC already exists for this account in this region.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000004c</RequestID>
</Response>
```

`400` `DefaultVpcAlreadyExists`.

### CreateDelegateMacVolumeOwnershipTask

Delegates ownership of the Amazon EBS root volume for an Apple silicon Mac instance to an administrative user.

```http
POST / HTTP/1.1

Action=CreateDelegateMacVolumeOwnershipTask
&Version=2016-11-15
&ClientToken=286e6223-4617-46e1-80ec-f9a5ddb8f6cb
&InstanceId=example
&MacCredentials=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>The instance ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000004d</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### CreateDhcpOptions

Creates a custom set of DHCP options.

```http
POST / HTTP/1.1

Action=CreateDhcpOptions
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'dhcpConfigurations' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000004e</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CreateEgressOnlyInternetGateway

[IPv6 only] Creates an egress-only internet gateway for your VPC.

```http
POST / HTTP/1.1

Action=CreateEgressOnlyInternetGateway
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateEgressOnlyInternetGateway is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000004f</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateFleet

Creates an EC2 Fleet that contains the configuration information for On-Demand Instances and Spot Instances.

```http
POST / HTTP/1.1

Action=CreateFleet
&Version=2016-11-15
&ClientToken=12a14828-e04d-41ec-afa3-51d7a43e0a4c
&TargetCapacitySpecification.TotalTargetCapacity=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'launchTemplateConfigs' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000050</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CreateFlowLogs

Creates one or more flow logs to capture information about IP traffic for a specific network interface, subnet, or VPC.

```http
POST / HTTP/1.1

Action=CreateFlowLogs
&Version=2016-11-15
&ResourceId.1=example
&ResourceType=VPC
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter TrafficType</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000051</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateFpgaImage

Creates an Amazon FPGA Image (AFI) from the specified design checkpoint (DCP).

```http
POST / HTTP/1.1

Action=CreateFpgaImage
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'inputStorageLocation' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000052</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CreateImage

Creates an Amazon EBS-backed AMI from an Amazon EBS-backed instance that is either running or stopped.

```http
POST / HTTP/1.1

Action=CreateImage
&Version=2016-11-15
&InstanceId=example
&Name=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateImage is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000053</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateImageUsageReport

Creates a report that shows how your image is used across other Amazon Web Services accounts.

```http
POST / HTTP/1.1

Action=CreateImageUsageReport
&Version=2016-11-15
&ImageId=example
&ClientToken=c7b10a9b-46a6-4338-95d0-ecf98653a79e
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'resourceTypes' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000054</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CreateInstanceConnectEndpoint

Creates an EC2 Instance Connect Endpoint.

```http
POST / HTTP/1.1

Action=CreateInstanceConnectEndpoint
&Version=2016-11-15
&SubnetId=example
&ClientToken=e243e1b4-c1d2-4871-9e51-11522b2fbfe5
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSubnetID.NotFound</Code>
      <Message>The subnet ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000055</RequestID>
</Response>
```

`400` `InvalidSubnetID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateInstanceEventWindow

Creates an event window in which scheduled events for the associated Amazon EC2 instances can run.

```http
POST / HTTP/1.1

Action=CreateInstanceEventWindow
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>Either cronExpression or timeRanges must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000056</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateInstanceExportTask

Exports a running or stopped instance to an Amazon S3 bucket.

```http
POST / HTTP/1.1

Action=CreateInstanceExportTask
&Version=2016-11-15
&InstanceId=example
&TargetEnvironment=citrix
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'exportToS3Task' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000057</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CreateInternetGateway

Creates an internet gateway for use with a VPC.

```http
POST / HTTP/1.1

Action=CreateInternetGateway
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateInternetGatewayResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <internetGateway xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <attachmentSet/>
    <internetGatewayId>igw-000000000000000b1</internetGatewayId>
    <ownerId>000000000000</ownerId>
    <tagSet/>
  </internetGateway>
  <requestId>b70334a4-45b5-4d1d-beaf-ad2c11ccf931</requestId>
</CreateInternetGatewayResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateInterruptibleCapacityReservationAllocation

Creates an interruptible Capacity Reservation by specifying the number of unused instances you want to allocate from your source reservation.

```http
POST / HTTP/1.1

Action=CreateInterruptibleCapacityReservationAllocation
&Version=2016-11-15
&CapacityReservationId=example
&InstanceCount=1
&ClientToken=76128eca-ab3a-41fe-9daa-0c68a6440f00
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000058</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### CreateIpam

Create an IPAM.

```http
POST / HTTP/1.1

Action=CreateIpam
&Version=2016-11-15
&ClientToken=34053775-8108-4470-9db2-5827a503b70f
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateIpamResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipam xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <ownerId>000000000000</ownerId>
    <ipamId>ipam-000000000000000b2</ipamId>
    <ipamArn>arn:aws:ec2::000000000000:ipam/ipam-000000000000000b2</ipamArn>
    <ipamRegion>us-east-1</ipamRegion>
    <publicDefaultScopeId>ipam-scope-000000000000000b3</publicDefaultScopeId>
    <privateDefaultScopeId>ipam-scope-000000000000000b4</privateDefaultScopeId>
    <scopeCount>2</scopeCount>
    <operatingRegionSet>
… (15 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateIpamExternalResourceVerificationToken

Create a verification token.

```http
POST / HTTP/1.1

Action=CreateIpamExternalResourceVerificationToken
&Version=2016-11-15
&IpamId=example
&ClientToken=712e7a21-3b52-4081-b1a5-8e99a6d01b6f
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateIpamExternalResourceVerificationToken is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000059</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateIpamInternetRegistryAssociation

Creates an association between an IPAM and a Regional Internet Registry (RIR) for Resource Public Key Infrastructure (RPKI) management.

```http
POST / HTTP/1.1

Action=CreateIpamInternetRegistryAssociation
&Version=2016-11-15
&IpamId=example
&Rir=ripe
&OrganizationHandle=example
&ClientToken=b3e0133d-e79f-4d49-9e62-1af929871e41
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000005a</RequestID>
</Response>
```

`400` `InvalidIpamId.Malformed`: the placeholder values failed the operation's input validation.

### CreateIpamPolicy

Creates an IPAM policy.

```http
POST / HTTP/1.1

Action=CreateIpamPolicy
&Version=2016-11-15
&ClientToken=098d66ae-4427-4e25-8592-b23323e26d2d
&IpamId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateIpamPolicy is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000005b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateIpamPool

Create an IP address pool for Amazon VPC IP Address Manager (IPAM).

```http
POST / HTTP/1.1

Action=CreateIpamPool
&Version=2016-11-15
&IpamScopeId=example
&AddressFamily=ipv4
&ClientToken=2cad27f9-497b-46cd-b811-1bc97b88bf24
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamScopeId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000005c</RequestID>
</Response>
```

`400` `InvalidIpamScopeId.Malformed`: the placeholder values failed the operation's input validation.

### CreateIpamPrefixListResolver

Creates an IPAM prefix list resolver.

```http
POST / HTTP/1.1

Action=CreateIpamPrefixListResolver
&Version=2016-11-15
&IpamId=example
&AddressFamily=ipv4
&ClientToken=c10e14f9-4fbe-45b0-8e99-92d6e34b1d30
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000005d</RequestID>
</Response>
```

`400` `InvalidIpamId.Malformed`: the placeholder values failed the operation's input validation.

### CreateIpamPrefixListResolverTarget

Creates an IPAM prefix list resolver target.

```http
POST / HTTP/1.1

Action=CreateIpamPrefixListResolverTarget
&Version=2016-11-15
&IpamPrefixListResolverId=example
&PrefixListId=example
&PrefixListRegion=example
&TrackLatestVersion=false
&ClientToken=24ba8407-ede7-459a-a785-d33530f63462
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPrefixListId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000005e</RequestID>
</Response>
```

`400` `InvalidPrefixListId.Malformed`: the placeholder values failed the operation's input validation.

### CreateIpamResourceDiscovery

Creates an IPAM resource discovery.

```http
POST / HTTP/1.1

Action=CreateIpamResourceDiscovery
&Version=2016-11-15
&ClientToken=63185666-4428-4f6d-86ea-15ceaa138366
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateIpamResourceDiscovery is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000005f</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateIpamRoutingPolicyRegistration

Creates a routing policy registration and publishes Route Origin Authorizations (ROAs) to the RPKI for the specified CIDR prefix and ASNs.

```http
POST / HTTP/1.1

Action=CreateIpamRoutingPolicyRegistration
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
&Cidr=example
&Asn.1=example
&ClientToken=bb84b0a8-a8d0-4bd5-9efd-48d3b58f6ec0
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateIpamRoutingPolicyRegistration is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000060</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateIpamScope

Create an IPAM scope.

```http
POST / HTTP/1.1

Action=CreateIpamScope
&Version=2016-11-15
&IpamId=example
&ClientToken=f7bdd839-4dcb-41bd-9700-73b345a9c797
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000061</RequestID>
</Response>
```

`400` `InvalidIpamId.Malformed`: the placeholder values failed the operation's input validation.

### CreateKeyPair

Creates an ED25519 or 2048-bit RSA key pair with the specified name and in the specified format.

```http
POST / HTTP/1.1

Action=CreateKeyPair
&Version=2016-11-15
&KeyName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateKeyPairResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <keyPairId>key-000000000000000b7</keyPairId>
  <tagSet/>
  <keyName>example</keyName>
  <keyFingerprint>41:33:3a:af:ab:b9:0e:20:c8:e4:c5:b0:ae:88:bd:b0:c7:18:1c:bd</keyFingerprint>
  <keyMaterial>-----BEGIN RSA PRIVATE KEY-----
MIIEoQIBAAKCAQEAztFPB3bVG0zJBHtEy8NFzhbWSr7pM43Zh7Lm9Q5RhvxSGKLm
jT+IBOuapf4QdE448phhpj8HNakfffyK9ml/p/dmN7/iHz6ETXVMafcIuOHFZc03
aSoEM/dO3Edem5AeH5AqKByp5MUjNVmNd0+4w6OBCy+vsJmxSNGzYkxkyIB+LwR6
f9b2VhAv87SEOXefg7/E/dWf+DHr4s91eCyviO/tNiPsf6JEpqIbMLHgI5nv6TS2
… (25 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateLaunchTemplate

Creates a launch template.

```http
POST / HTTP/1.1

Action=CreateLaunchTemplate
&Version=2016-11-15
&ClientToken=00be74f7-af5d-45f2-aaaf-302e8a8b38d6
&LaunchTemplateName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'launchTemplateData' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000062</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CreateLaunchTemplateVersion

Creates a new version of a launch template.

```http
POST / HTTP/1.1

Action=CreateLaunchTemplateVersion
&Version=2016-11-15
&ClientToken=a2cd5de2-1ab4-4507-9050-d553596f4add
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'launchTemplateData' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000063</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CreateLocalGatewayRoute

Creates a static route for the specified local gateway route table.

```http
POST / HTTP/1.1

Action=CreateLocalGatewayRoute
&Version=2016-11-15
&LocalGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayRouteTableID.Malformed</Code>
      <Message>The local-gateway-route-table ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000064</RequestID>
</Response>
```

`400` `InvalidLocalGatewayRouteTableID.Malformed`: the placeholder values failed the operation's input validation.

### CreateLocalGatewayRouteTable

Creates a local gateway route table.

```http
POST / HTTP/1.1

Action=CreateLocalGatewayRouteTable
&Version=2016-11-15
&LocalGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayID.Malformed</Code>
      <Message>The local-gateway ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000065</RequestID>
</Response>
```

`400` `InvalidLocalGatewayID.Malformed`: the placeholder values failed the operation's input validation.

### CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociation

Creates a local gateway route table virtual interface group association.

```http
POST / HTTP/1.1

Action=CreateLocalGatewayRouteTableVirtualInterfaceGroupAssociation
&Version=2016-11-15
&LocalGatewayRouteTableId=example
&LocalGatewayVirtualInterfaceGroupId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayRouteTableID.Malformed</Code>
      <Message>The local-gateway-route-table ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000066</RequestID>
</Response>
```

`400` `InvalidLocalGatewayRouteTableID.Malformed`: the placeholder values failed the operation's input validation.

### CreateLocalGatewayRouteTableVpcAssociation

Associates the specified VPC with the specified local gateway route table.

```http
POST / HTTP/1.1

Action=CreateLocalGatewayRouteTableVpcAssociation
&Version=2016-11-15
&LocalGatewayRouteTableId=example
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000067</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateLocalGatewayVirtualInterface

Create a virtual interface for a local gateway.

```http
POST / HTTP/1.1

Action=CreateLocalGatewayVirtualInterface
&Version=2016-11-15
&LocalGatewayVirtualInterfaceGroupId=example
&OutpostLagId=example
&Vlan=1
&LocalAddress=example
&PeerAddress=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayVirtualInterfaceGroupID.Malformed</Code>
      <Message>The local-gateway-virtual-interface-group ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000068</RequestID>
</Response>
```

`400` `InvalidLocalGatewayVirtualInterfaceGroupID.Malformed`: the placeholder values failed the operation's input validation.

### CreateLocalGatewayVirtualInterfaceGroup

Create a local gateway virtual interface group.

```http
POST / HTTP/1.1

Action=CreateLocalGatewayVirtualInterfaceGroup
&Version=2016-11-15
&LocalGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayID.Malformed</Code>
      <Message>The local-gateway ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000069</RequestID>
</Response>
```

`400` `InvalidLocalGatewayID.Malformed`: the placeholder values failed the operation's input validation.

### CreateMacSystemIntegrityProtectionModificationTask

Creates a System Integrity Protection (SIP) modification task to configure the SIP settings for an x86 Mac instance or Apple silicon Mac instance.

```http
POST / HTTP/1.1

Action=CreateMacSystemIntegrityProtectionModificationTask
&Version=2016-11-15
&ClientToken=ad1434a1-01b9-4c70-afde-a0ec15b4b3bc
&InstanceId=example
&MacSystemIntegrityProtectionStatus=enabled
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>The instance ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000006a</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### CreateManagedPrefixList

Creates a managed prefix list.

```http
POST / HTTP/1.1

Action=CreateManagedPrefixList
&Version=2016-11-15
&PrefixListName=example
&MaxEntries=1
&AddressFamily=example
&ClientToken=98def155-a9fd-43fb-b462-eac1dc4c551c
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value 'example' for addressFamily. Valid values: IPv4 | IPv6</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000006b</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateNatGateway

Creates a NAT gateway in the specified subnet.

```http
POST / HTTP/1.1

Action=CreateNatGateway
&Version=2016-11-15
&ClientToken=5b4e8f16-6070-404d-9994-b4db19b99cdb
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter subnetId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000006c</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateNetworkAcl

Creates a network ACL in a VPC.

```http
POST / HTTP/1.1

Action=CreateNetworkAcl
&Version=2016-11-15
&ClientToken=084dbb90-8bf2-4792-84d8-0dc74f1f6b2f
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000006d</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateNetworkAclEntry

Creates an entry (a rule) in a network ACL with the specified rule number.

```http
POST / HTTP/1.1

Action=CreateNetworkAclEntry
&Version=2016-11-15
&NetworkAclId=example
&RuleNumber=1
&Protocol=example
&RuleAction=allow
&Egress=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkAclID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000006e</RequestID>
</Response>
```

`400` `InvalidNetworkAclID.Malformed`: the placeholder values failed the operation's input validation.

### CreateNetworkInsightsAccessScope

Creates a Network Access Scope.

```http
POST / HTTP/1.1

Action=CreateNetworkInsightsAccessScope
&Version=2016-11-15
&ClientToken=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateNetworkInsightsAccessScopeResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <networkInsightsAccessScope xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <networkInsightsAccessScopeId>nis-000000000000000b8</networkInsightsAccessScopeId>
    <networkInsightsAccessScopeArn>arn:aws:ec2:us-east-1:000000000000:network-insights-access-scope/nis-000000000000000b8</networkInsightsAccessScopeArn>
    <createdDate>2026-11-16T12:11:31.379122Z</createdDate>
    <updatedDate>2026-11-16T12:11:31.379122Z</updatedDate>
    <tagSet/>
  </networkInsightsAccessScope>
  <networkInsightsAccessScopeContent>
    <networkInsightsAccessScopeId>nis-000000000000000b8</networkInsightsAccessScopeId>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateNetworkInsightsPath

Creates a path to analyze for reachability.

```http
POST / HTTP/1.1

Action=CreateNetworkInsightsPath
&Version=2016-11-15
&Source=example
&Protocol=tcp
&ClientToken=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>example is not a supported Source type. Supported Source types include: [instance, network-interface, internet-gateway, vpn-gateway, vpc-endpoint, vpc-endpoint-service, vpc-peering-connection, transit-gateway, transit-gateway-attachment]</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000006f</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateNetworkInterface

Creates a network interface in the specified subnet.

```http
POST / HTTP/1.1

Action=CreateNetworkInterface
&Version=2016-11-15
&ClientToken=25a0166a-e123-4ee2-8811-088566c0e5eb
&SubnetId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSubnetID.NotFound</Code>
      <Message>The subnet ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000070</RequestID>
</Response>
```

`400` `InvalidSubnetID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateNetworkInterfacePermission

Grants an Amazon Web Services-authorized account permission to attach the specified network interface to an instance in their account.

```http
POST / HTTP/1.1

Action=CreateNetworkInterfacePermission
&Version=2016-11-15
&NetworkInterfaceId=example
&Permission=INSTANCE-ATTACH
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter awsAccountId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000071</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreatePlacementGroup

Creates a placement group in which to launch instances.

```http
POST / HTTP/1.1

Action=CreatePlacementGroup
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter GroupName</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000072</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreatePublicIpv4Pool

Creates a public IPv4 address pool.

```http
POST / HTTP/1.1

Action=CreatePublicIpv4Pool
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreatePublicIpv4PoolResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <poolId>ipv4pool-ec2-000000000000000b9</poolId>
  <requestId>32e70042-ce5a-4e19-835d-e2585ecca6ed</requestId>
</CreatePublicIpv4PoolResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateReplaceRootVolumeTask

Replaces the EBS-backed root volume for a running instance with a new volume that is restored to the original root volume's launch state, that is restored to a specific snapshot taken from the original root volume, that is restored from an AMI that has the same key characteristics as that of the instance, or that is replaced by a specified volume.

```http
POST / HTTP/1.1

Action=CreateReplaceRootVolumeTask
&Version=2016-11-15
&InstanceId=example
&ClientToken=c9c16ad7-3783-45c8-be40-b239c003020f
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>The instance ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000073</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### CreateReservedInstancesListing

Creates a listing for Amazon EC2 Standard Reserved Instances to be sold in the Reserved Instance Marketplace.

```http
POST / HTTP/1.1

Action=CreateReservedInstancesListing
&Version=2016-11-15
&ReservedInstancesId=example
&InstanceCount=1
&ClientToken=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'priceSchedules' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000074</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CreateRestoreImageTask

Starts a task that restores an AMI from an Amazon S3 object that was previously created by using CreateStoreImageTask .

```http
POST / HTTP/1.1

Action=CreateRestoreImageTask
&Version=2016-11-15
&Bucket=example
&ObjectKey=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InaccessibleStorageLocation</Code>
      <Message>Error accessing resource in S3. Check permissions, naming, and ensure the bucket is within the same region as the API endpoint. Bucket: example, Key: example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000075</RequestID>
</Response>
```

`400` `InaccessibleStorageLocation`.

### CreateRoute

Creates a route in a route table within a VPC.

```http
POST / HTTP/1.1

Action=CreateRoute
&Version=2016-11-15
&RouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either the parameter destinationCidrBlock, destinationIpv6CidrBlock or destinationPrefixListId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000076</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateRouteServer

Creates a new route server to manage dynamic routing in a VPC.

```http
POST / HTTP/1.1

Action=CreateRouteServer
&Version=2016-11-15
&AmazonSideAsn=1
&ClientToken=2ea689ea-7960-4f66-9c10-a9a75fe1b3bf
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateRouteServerResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <routeServer xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <routeServerId>rs-000000000000000ba</routeServerId>
    <amazonSideAsn>1</amazonSideAsn>
    <state>pending</state>
    <tagSet/>
    <persistRoutesState>disabled</persistRoutesState>
    <snsNotificationsEnabled>false</snsNotificationsEnabled>
  </routeServer>
  <requestId>ec1bb7ff-060d-4209-be4d-7e979de99622</requestId>
… (1 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateRouteServerEndpoint

Creates a new endpoint for a route server in a specified subnet.

```http
POST / HTTP/1.1

Action=CreateRouteServerEndpoint
&Version=2016-11-15
&RouteServerId=example
&SubnetId=example
&ClientToken=b7e5064e-a431-4478-9424-682bcd089cfe
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000077</RequestID>
</Response>
```

`400` `InvalidRouteServerId.Malformed`: the placeholder values failed the operation's input validation.

### CreateRouteServerPeer

Creates a new BGP peer for a specified route server endpoint.

```http
POST / HTTP/1.1

Action=CreateRouteServerPeer
&Version=2016-11-15
&RouteServerEndpointId=example
&PeerAddress=example
&BgpOptions.PeerAsn=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerEndpointId.NotFound</Code>
      <Message>The route-server-endpoint ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000078</RequestID>
</Response>
```

`400` `InvalidRouteServerEndpointId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateRouteTable

Creates a route table for the specified VPC.

```http
POST / HTTP/1.1

Action=CreateRouteTable
&Version=2016-11-15
&ClientToken=a770569d-c943-4a02-9aa3-310c2d9ec85c
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000079</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateSecondaryNetwork

Creates a secondary network.

```http
POST / HTTP/1.1

Action=CreateSecondaryNetwork
&Version=2016-11-15
&ClientToken=c04533bf-5305-4296-8fdb-6a20e80a89a5
&Ipv4CidrBlock=example
&NetworkType=rdma
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter ipv4CidrBlock is invalid. This is not a valid CIDR block.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000007a</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateSecondarySubnet

Creates a secondary subnet in a secondary network.

```http
POST / HTTP/1.1

Action=CreateSecondarySubnet
&Version=2016-11-15
&ClientToken=b41ae075-4371-4ad2-b1d8-54ef2ff1958f
&Ipv4CidrBlock=example
&SecondaryNetworkId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateSecondarySubnet is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000007b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateSecurityGroup

Creates a security group.

```http
POST / HTTP/1.1

Action=CreateSecurityGroup
&Version=2016-11-15
&GroupDescription=example
&GroupName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateSecurityGroupResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <groupId>sg-000000000000000bb</groupId>
  <securityGroupArn>arn:aws:ec2:us-east-1:000000000000:security-group/sg-000000000000000bb</securityGroupArn>
  <return>true</return>
  <requestId>d4a55df9-e3de-4bbd-b9c4-c3894d290964</requestId>
</CreateSecurityGroupResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#3-createsecuritygroup) for a successful call.

### CreateSnapshot

Creates a snapshot of an EBS volume and stores it in Amazon S3.

```http
POST / HTTP/1.1

Action=CreateSnapshot
&Version=2016-11-15
&VolumeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVolumeID.Malformed</Code>
      <Message>Value ( example ) for parameter VOLUME is invalid. </Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000007c</RequestID>
</Response>
```

`400` `InvalidVolumeID.Malformed`: the placeholder values failed the operation's input validation.

### CreateSnapshots

Creates crash-consistent snapshots of multiple EBS volumes attached to an Amazon EC2 instance.

```http
POST / HTTP/1.1

Action=CreateSnapshots
&Version=2016-11-15
&InstanceSpecification.InstanceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>Unable to unobfuscate ID.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000007d</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### CreateSpotDatafeedSubscription

Creates a data feed for Spot Instances, enabling you to view Spot Instance usage logs.

```http
POST / HTTP/1.1

Action=CreateSpotDatafeedSubscription
&Version=2016-11-15
&Bucket=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateSpotDatafeedSubscriptionResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <spotDatafeedSubscription xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <bucket>example</bucket>
    <ownerId>000000000000</ownerId>
    <state>Active</state>
  </spotDatafeedSubscription>
  <requestId>5817aca3-9c19-4f04-9084-c58c54199ecd</requestId>
</CreateSpotDatafeedSubscriptionResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateStoreImageTask

Stores an AMI as a single object in an Amazon S3 bucket.

```http
POST / HTTP/1.1

Action=CreateStoreImageTask
&Version=2016-11-15
&ImageId=example
&Bucket=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAMIID.Malformed</Code>
      <Message>The image ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000007e</RequestID>
</Response>
```

`400` `InvalidAMIID.Malformed`: the placeholder values failed the operation's input validation.

### CreateSubnet

Creates a subnet in the specified VPC.

```http
POST / HTTP/1.1

Action=CreateSubnet
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>Either 'cidrBlock' or 'ipv6CidrBlock' must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000007f</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation. See [the walkthrough](#2-createsubnet) for a successful call.

### CreateSubnetCidrReservation

Creates a subnet CIDR reservation.

```http
POST / HTTP/1.1

Action=CreateSubnetCidrReservation
&Version=2016-11-15
&SubnetId=example
&Cidr=example
&ReservationType=prefix
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter cidr is invalid. This is not a valid CIDR block.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000080</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateTags

Adds or overwrites only the specified tags for the specified Amazon EC2 resource or resources.

```http
POST / HTTP/1.1

Action=CreateTags
&Version=2016-11-15
&ResourceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'tags' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000081</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### CreateTrafficMirrorFilter

Creates a Traffic Mirror filter.

```http
POST / HTTP/1.1

Action=CreateTrafficMirrorFilter
&Version=2016-11-15
&ClientToken=8e575ea0-f829-4f32-a7c3-a50f04fe6208
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateTrafficMirrorFilterResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <trafficMirrorFilter xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <trafficMirrorFilterId>tmf-000000000000000bd</trafficMirrorFilterId>
    <ingressFilterRuleSet/>
    <egressFilterRuleSet/>
    <networkServiceSet/>
    <tagSet/>
  </trafficMirrorFilter>
  <clientToken>8e575ea0-f829-4f32-a7c3-a50f04fe6208</clientToken>
  <requestId>520390ed-d116-4fae-80b0-f3b52c6314f2</requestId>
… (1 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateTrafficMirrorFilterRule

Creates a Traffic Mirror filter rule.

```http
POST / HTTP/1.1

Action=CreateTrafficMirrorFilterRule
&Version=2016-11-15
&TrafficMirrorFilterId=example
&TrafficDirection=ingress
&RuleNumber=1
&RuleAction=accept
&DestinationCidrBlock=example
&SourceCidrBlock=example
&ClientToken=d64f73eb-7a3f-4d22-bdff-7f86d976edc4
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateTrafficMirrorFilterRule is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000082</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateTrafficMirrorSession

Creates a Traffic Mirror session.

```http
POST / HTTP/1.1

Action=CreateTrafficMirrorSession
&Version=2016-11-15
&NetworkInterfaceId=example
&TrafficMirrorTargetId=example
&TrafficMirrorFilterId=example
&SessionNumber=1
&ClientToken=85873d10-ecb1-4103-8866-39b1e99864df
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation CreateTrafficMirrorSession is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000083</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateTrafficMirrorTarget

Creates a target for your Traffic Mirror session.

```http
POST / HTTP/1.1

Action=CreateTrafficMirrorTarget
&Version=2016-11-15
&ClientToken=f8dc7bc7-8131-42f6-b97e-111d507f9784
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameter</Code>
      <Message>Either networkInterfaceId, networkLoadBalancerArn or gatewayLoadBalancerEndpointId must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000084</RequestID>
</Response>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### CreateTransitGateway

Creates a transit gateway.

```http
POST / HTTP/1.1

Action=CreateTransitGateway
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateTransitGatewayResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGateway xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <transitGatewayId>tgw-000000000000000be</transitGatewayId>
    <transitGatewayArn>arn:aws:ec2:us-east-1:000000000000:transit-gateway/tgw-000000000000000be</transitGatewayArn>
    <state>pending</state>
    <ownerId>000000000000</ownerId>
    <creationTime>2026-11-16T12:11:31.481156Z</creationTime>
    <options>
      <amazonSideAsn>64512</amazonSideAsn>
      <autoAcceptSharedAttachments>disable</autoAcceptSharedAttachments>
… (13 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateTransitGatewayConnect

Creates a Connect attachment from a specified transit gateway attachment.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayConnect
&Version=2016-11-15
&TransportTransitGatewayAttachmentId=example
&Options.Protocol=gre
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid Transit Gateway Attachment id.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000085</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateTransitGatewayConnectPeer

Creates a Connect peer for a specified transit gateway Connect attachment between a transit gateway and an appliance.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayConnectPeer
&Version=2016-11-15
&TransitGatewayAttachmentId=example
&PeerAddress=example
&InsideCidrBlocks.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayAttachmentID.Malformed</Code>
      <Message>Invalid Transit Gateway Attachment id.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000086</RequestID>
</Response>
```

`400` `InvalidTransitGatewayAttachmentID.Malformed`: the placeholder values failed the operation's input validation.

### CreateTransitGatewayMeteringPolicy

Creates a metering policy for a transit gateway to track and measure network traffic.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayMeteringPolicy
&Version=2016-11-15
&TransitGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayID.NotFound</Code>
      <Message>Transit Gateway example was deleted or does not exist.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000087</RequestID>
</Response>
```

`400` `InvalidTransitGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateTransitGatewayMeteringPolicyEntry

Creates an entry in a transit gateway metering policy to define traffic measurement rules.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayMeteringPolicyEntry
&Version=2016-11-15
&TransitGatewayMeteringPolicyId=example
&PolicyRuleNumber=1
&MeteredAccount=source-attachment-owner
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayMeteringPolicyIdMalformedException</Code>
      <Message>The transit-gateway-metering-policy ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000088</RequestID>
</Response>
```

`400` `InvalidTransitGatewayMeteringPolicyIdMalformedException`: the placeholder values failed the operation's input validation.

### CreateTransitGatewayMulticastDomain

Creates a multicast domain using the specified transit gateway.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayMulticastDomain
&Version=2016-11-15
&TransitGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayID.NotFound</Code>
      <Message>Transit Gateway example was deleted or does not exist.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000089</RequestID>
</Response>
```

`400` `InvalidTransitGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateTransitGatewayPeeringAttachment

Requests a transit gateway peering attachment between the specified transit gateway (requester) and a peer transit gateway (accepter).

```http
POST / HTTP/1.1

Action=CreateTransitGatewayPeeringAttachment
&Version=2016-11-15
&TransitGatewayId=example
&PeerTransitGatewayId=example
&PeerAccountId=example
&PeerRegion=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value example for PeerTransitGatewayId. The value of PeerTransitGatewayId needs to be different to TransitGatewayId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000008a</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateTransitGatewayPolicyTable

Creates a transit gateway policy table.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayPolicyTable
&Version=2016-11-15
&TransitGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayID.NotFound</Code>
      <Message>Transit Gateway example was deleted or does not exist.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000008b</RequestID>
</Response>
```

`400` `InvalidTransitGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateTransitGatewayPolicyTableEntry

Creates an entry in a transit gateway policy table to route matching traffic to a specified route table.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayPolicyTableEntry
&Version=2016-11-15
&TransitGatewayPolicyTableId=example
&PolicyRuleNumber=example
&TargetRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayPolicyTableId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000008c</RequestID>
</Response>
```

`400` `InvalidTransitGatewayPolicyTableId.Malformed`: the placeholder values failed the operation's input validation.

### CreateTransitGatewayPrefixListReference

Creates a reference (route) to a prefix list in a specified transit gateway route table.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayPrefixListReference
&Version=2016-11-15
&TransitGatewayRouteTableId=example
&PrefixListId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableId.Malformed</Code>
      <Message>Invalid Transit Gateway Route Table id example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000008d</RequestID>
</Response>
```

`400` `InvalidRouteTableId.Malformed`: the placeholder values failed the operation's input validation.

### CreateTransitGatewayRoute

Creates a static route for the specified transit gateway route table.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayRoute
&Version=2016-11-15
&DestinationCidrBlock=example
&TransitGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value 'example' for destinationCidrBlock. A destination is a CIDR block</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000008e</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateTransitGatewayRouteTable

Creates a route table for the specified transit gateway.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayRouteTable
&Version=2016-11-15
&TransitGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayID.NotFound</Code>
      <Message>Transit Gateway example was deleted or does not exist.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000008f</RequestID>
</Response>
```

`400` `InvalidTransitGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateTransitGatewayRouteTableAnnouncement

Advertises a new transit gateway route table.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayRouteTableAnnouncement
&Version=2016-11-15
&TransitGatewayRouteTableId=example
&PeeringAttachmentId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableId.Malformed</Code>
      <Message>Invalid Transit Gateway Route Table id example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000090</RequestID>
</Response>
```

`400` `InvalidRouteTableId.Malformed`: the placeholder values failed the operation's input validation.

### CreateTransitGatewayVpcAttachment

Attaches the specified VPC to the specified transit gateway.

```http
POST / HTTP/1.1

Action=CreateTransitGatewayVpcAttachment
&Version=2016-11-15
&TransitGatewayId=example
&VpcId=example
&SubnetIds.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSubnetID.Malformed</Code>
      <Message>The subnet ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000091</RequestID>
</Response>
```

`400` `InvalidSubnetID.Malformed`: the placeholder values failed the operation's input validation.

### CreateVerifiedAccessEndpoint

An Amazon Web Services Verified Access endpoint is where you define your application along with an optional endpoint-level access policy.

```http
POST / HTTP/1.1

Action=CreateVerifiedAccessEndpoint
&Version=2016-11-15
&VerifiedAccessGroupId=example
&EndpointType=load-balancer
&AttachmentType=vpc
&ClientToken=86802f94-ef12-4775-9f97-84e5ca874fc7
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>loadBalancerOptions is required when endpointType is load-balancer</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000092</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateVerifiedAccessGroup

An Amazon Web Services Verified Access group is a collection of Amazon Web Services Verified Access endpoints who's associated applications have similar security requirements.

```http
POST / HTTP/1.1

Action=CreateVerifiedAccessGroup
&Version=2016-11-15
&VerifiedAccessInstanceId=example
&ClientToken=93ef0444-e960-4f90-9180-93ba9db799af
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVerifiedAccessInstanceId.NotFound</Code>
      <Message>The verified-access-instance ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000093</RequestID>
</Response>
```

`400` `InvalidVerifiedAccessInstanceId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateVerifiedAccessInstance

An Amazon Web Services Verified Access instance is a regional entity that evaluates application requests and grants access only when your security requirements are met.

```http
POST / HTTP/1.1

Action=CreateVerifiedAccessInstance
&Version=2016-11-15
&ClientToken=888bd172-28b3-4bcb-b923-f6d241eef6a9
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateVerifiedAccessInstanceResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <verifiedAccessInstance xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <verifiedAccessInstanceId>vai-000000000000000c0</verifiedAccessInstanceId>
    <verifiedAccessTrustProviderSet/>
    <creationTime>2026-11-16T12:11:31.558696Z</creationTime>
    <lastUpdatedTime>2026-11-16T12:11:31.558696Z</lastUpdatedTime>
    <tagSet/>
    <fipsEnabled>false</fipsEnabled>
    <verifiedAccessInstanceTransitGatewayAttachmentSet/>
  </verifiedAccessInstance>
… (2 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateVerifiedAccessTrustProvider

A trust provider is a third-party entity that creates, maintains, and manages identity information for users and devices.

```http
POST / HTTP/1.1

Action=CreateVerifiedAccessTrustProvider
&Version=2016-11-15
&TrustProviderType=user
&PolicyReferenceName=example
&ClientToken=11d62cef-02cf-4874-b6fd-8078421fcafb
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>userTrustProviderType is required when trustProviderType is user</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000094</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateVolume

Creates an EBS volume that can be attached to an instance in the same Availability Zone.

```http
POST / HTTP/1.1

Action=CreateVolume
&Version=2016-11-15
&ClientToken=57dae450-7890-4db0-8951-2f46cb1e9979
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>Either availabilityZone or availabilityZoneId must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000095</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateVpc

Creates a VPC with the specified CIDR blocks.

```http
POST / HTTP/1.1

Action=CreateVpc
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>Either 'cidrBlock' or 'ipv4IpamPoolId' must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000096</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createvpc) for a successful call.

### CreateVpcBlockPublicAccessExclusion

Create a VPC Block Public Access (BPA) exclusion.

```http
POST / HTTP/1.1

Action=CreateVpcBlockPublicAccessExclusion
&Version=2016-11-15
&InternetGatewayExclusionMode=allow-bidirectional
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>One of the following parameters must be provided: VpcId, SubnetId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000097</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateVpcEncryptionControl

Creates a VPC Encryption Control configuration for a specified VPC.

```http
POST / HTTP/1.1

Action=CreateVpcEncryptionControl
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000098</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateVpcEndpoint

Creates a VPC endpoint.

```http
POST / HTTP/1.1

Action=CreateVpcEndpoint
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter ServiceName</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000099</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateVpcEndpointConnectionNotification

Creates a connection notification for a specified VPC endpoint or VPC endpoint service.

```http
POST / HTTP/1.1

Action=CreateVpcEndpointConnectionNotification
&Version=2016-11-15
&ConnectionNotificationArn=arn%3Aaws%3Aec2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&ConnectionEvents.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameter</Code>
      <Message>Either serviceId or vpcEndpointId must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000009a</RequestID>
</Response>
```

`400` `InvalidParameter`: the placeholder values failed the operation's input validation.

### CreateVpcEndpointServiceConfiguration

Creates a VPC endpoint service to which service consumers (Amazon Web Services accounts, users, and IAM roles) can connect.

```http
POST / HTTP/1.1

Action=CreateVpcEndpointServiceConfiguration
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>You must specify either NetworkLoadBalancerArn or GatewayLoadBalancerArn</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000009b</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateVpcPeeringConnection

Requests a VPC peering connection between two VPCs: a requester VPC that you own and an accepter VPC with which to create the connection.

```http
POST / HTTP/1.1

Action=CreateVpcPeeringConnection
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter peerVpcId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000009c</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateVpnConcentrator

Creates a VPN concentrator that aggregates multiple VPN connections to a transit gateway.

```http
POST / HTTP/1.1

Action=CreateVpnConcentrator
&Version=2016-11-15
&Type=ipsec.1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter transitGatewayId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000009d</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### CreateVpnConnection

Creates a VPN connection between an existing virtual private gateway or transit gateway and a customer gateway.

```http
POST / HTTP/1.1

Action=CreateVpnConnection
&Version=2016-11-15
&CustomerGatewayId=example
&Type=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value 'example' for type. The only supported type is 'ipsec.1'</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000009e</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateVpnConnectionRoute

Creates a static route associated with a VPN connection between an existing virtual private gateway and a VPN customer gateway.

```http
POST / HTTP/1.1

Action=CreateVpnConnectionRoute
&Version=2016-11-15
&DestinationCidrBlock=example
&VpnConnectionId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter destinationCidrBlock is invalid. This is not a valid CIDR block.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000009f</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateVpnGateway

Creates a virtual private gateway.

```http
POST / HTTP/1.1

Action=CreateVpnGateway
&Version=2016-11-15
&Type=ipsec.1
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<CreateVpnGatewayResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpnGateway xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <amazonSideAsn>64512</amazonSideAsn>
    <tagSet/>
    <vpnGatewayId>vgw-000000000000000c1</vpnGatewayId>
    <state>pending</state>
    <type>ipsec.1</type>
    <attachments/>
  </vpnGateway>
  <requestId>b2317a02-5929-4e0f-a17c-5343183700f4</requestId>
… (1 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteApplicationStatusCheck

Deletes an application status check.

```http
POST / HTTP/1.1

Action=DeleteApplicationStatusCheck
&Version=2016-11-15
&ApplicationStatusCheckId=example
&ClientToken=8aa1d893-96eb-4849-8bbe-3ffdeabd8ed5
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidApplicationStatusCheckId.Malformed</Code>
      <Message>The application-status-check ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000a0</RequestID>
</Response>
```

`400` `InvalidApplicationStatusCheckId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteCapacityManagerDataExport

Deletes an existing Capacity Manager data export configuration.

```http
POST / HTTP/1.1

Action=DeleteCapacityManagerDataExport
&Version=2016-11-15
&CapacityManagerDataExportId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityManagerDataExportId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000a1</RequestID>
</Response>
```

`400` `InvalidCapacityManagerDataExportId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteCarrierGateway

Deletes a carrier gateway.

```http
POST / HTTP/1.1

Action=DeleteCarrierGateway
&Version=2016-11-15
&CarrierGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCarrierGatewayID.NotFound</Code>
      <Message>The CarrierGateway ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000a2</RequestID>
</Response>
```

`400` `InvalidCarrierGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteClientVpnEndpoint

Deletes the specified Client VPN endpoint.

```http
POST / HTTP/1.1

Action=DeleteClientVpnEndpoint
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidClientVpnEndpointId.NotFound</Code>
      <Message>Endpoint example does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000a3</RequestID>
</Response>
```

`400` `InvalidClientVpnEndpointId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteClientVpnEndpointAuthorizationPolicy

Deletes the authorization policy for a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=DeleteClientVpnEndpointAuthorizationPolicy
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteClientVpnEndpointAuthorizationPolicy is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000a4</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteClientVpnRoute

Deletes a route from a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=DeleteClientVpnRoute
&Version=2016-11-15
&ClientVpnEndpointId=example
&DestinationCidrBlock=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter destinationCidrBlock is invalid. This is not a valid CIDR block.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000a5</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteCoipCidr

Deletes a range of customer-owned IP addresses.

```http
POST / HTTP/1.1

Action=DeleteCoipCidr
&Version=2016-11-15
&Cidr=example
&CoipPoolId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPoolID.Malformed</Code>
      <Message>The pool ID 'example' is invalid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000a6</RequestID>
</Response>
```

`400` `InvalidPoolID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteCoipPool

Deletes a pool of customer-owned IP (CoIP) addresses.

```http
POST / HTTP/1.1

Action=DeleteCoipPool
&Version=2016-11-15
&CoipPoolId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPoolID.Malformed</Code>
      <Message>The pool ID 'example' is invalid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000a7</RequestID>
</Response>
```

`400` `InvalidPoolID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteCustomerGateway

Deletes the specified customer gateway.

```http
POST / HTTP/1.1

Action=DeleteCustomerGateway
&Version=2016-11-15
&CustomerGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCustomerGatewayID.NotFound</Code>
      <Message>The customerGateway ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000a8</RequestID>
</Response>
```

`400` `InvalidCustomerGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDhcpOptions

Deletes the specified set of DHCP options.

```http
POST / HTTP/1.1

Action=DeleteDhcpOptions
&Version=2016-11-15
&DhcpOptionsId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidDhcpOptionsId.Malformed</Code>
      <Message>The dhcp-options ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000a9</RequestID>
</Response>
```

`400` `InvalidDhcpOptionsId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteEgressOnlyInternetGateway

Deletes an egress-only internet gateway.

```http
POST / HTTP/1.1

Action=DeleteEgressOnlyInternetGateway
&Version=2016-11-15
&EgressOnlyInternetGatewayId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteEgressOnlyInternetGateway is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000aa</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteFleets

Deletes the specified EC2 Fleet request.

```http
POST / HTTP/1.1

Action=DeleteFleets
&Version=2016-11-15
&FleetId.1=example
&TerminateInstances=false
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DeleteFleetsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <successfulFleetDeletionSet/>
  <unsuccessfulFleetDeletionSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <error>
        <code>fleetIdMalformed</code>
        <message>Invalid id: &quot;example&quot;</message>
      </error>
      <fleetId>example</fleetId>
    </item>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteFlowLogs

Deletes one or more flow logs.

```http
POST / HTTP/1.1

Action=DeleteFlowLogs
&Version=2016-11-15
&FlowLogId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidFlowLogId.NotFound</Code>
      <Message>These flow log ids in the input list are not found: [TotalCount: 1] example</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ab</RequestID>
</Response>
```

`400` `InvalidFlowLogId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteFpgaImage

Deletes the specified Amazon FPGA Image (AFI).

```http
POST / HTTP/1.1

Action=DeleteFpgaImage
&Version=2016-11-15
&FpgaImageId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidFpgaImageID.Malformed</Code>
      <Message>Image ID 'example' is invalid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ac</RequestID>
</Response>
```

`400` `InvalidFpgaImageID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteImageUsageReport

Deletes the specified image usage report.

```http
POST / HTTP/1.1

Action=DeleteImageUsageReport
&Version=2016-11-15
&ReportId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidImageUsageReportId.Malformed</Code>
      <Message>The image-usage-report ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ad</RequestID>
</Response>
```

`400` `InvalidImageUsageReportId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteInstanceConnectEndpoint

Deletes the specified EC2 Instance Connect Endpoint.

```http
POST / HTTP/1.1

Action=DeleteInstanceConnectEndpoint
&Version=2016-11-15
&InstanceConnectEndpointId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceConnectEndpointId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ae</RequestID>
</Response>
```

`400` `InvalidInstanceConnectEndpointId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteInstanceEventWindow

Deletes the specified event window.

```http
POST / HTTP/1.1

Action=DeleteInstanceEventWindow
&Version=2016-11-15
&InstanceEventWindowId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceEventWindowId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000af</RequestID>
</Response>
```

`400` `InvalidInstanceEventWindowId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteInternetGateway

Deletes the specified internet gateway.

```http
POST / HTTP/1.1

Action=DeleteInternetGateway
&Version=2016-11-15
&InternetGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInternetGatewayID.NotFound</Code>
      <Message>The internetGateway ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000b0</RequestID>
</Response>
```

`400` `InvalidInternetGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteIpam

Delete an IPAM.

```http
POST / HTTP/1.1

Action=DeleteIpam
&Version=2016-11-15
&IpamId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000b1</RequestID>
</Response>
```

`400` `InvalidIpamId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteIpamExternalResourceVerificationToken

Delete a verification token.

```http
POST / HTTP/1.1

Action=DeleteIpamExternalResourceVerificationToken
&Version=2016-11-15
&IpamExternalResourceVerificationTokenId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteIpamExternalResourceVerificationToken is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000b2</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteIpamInternetRegistryAssociation

Deletes an IPAM internet registry association.

```http
POST / HTTP/1.1

Action=DeleteIpamInternetRegistryAssociation
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamInternetRegistryAssociationId.Malformed</Code>
      <Message>The ipam-internet-registry-association ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000b3</RequestID>
</Response>
```

`400` `InvalidIpamInternetRegistryAssociationId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteIpamPolicy

Deletes an IPAM policy.

```http
POST / HTTP/1.1

Action=DeleteIpamPolicy
&Version=2016-11-15
&IpamPolicyId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamPolicyId.Malformed</Code>
      <Message>The ipam-policy ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000b4</RequestID>
</Response>
```

`400` `InvalidIpamPolicyId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteIpamPool

Delete an IPAM pool.

```http
POST / HTTP/1.1

Action=DeleteIpamPool
&Version=2016-11-15
&IpamPoolId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamPoolId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000b5</RequestID>
</Response>
```

`400` `InvalidIpamPoolId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteIpamPrefixListResolver

Deletes an IPAM prefix list resolver.

```http
POST / HTTP/1.1

Action=DeleteIpamPrefixListResolver
&Version=2016-11-15
&IpamPrefixListResolverId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamPrefixListResolverId.NotFound</Code>
      <Message>The specified IPAM Prefix List Resolver does not exist.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000b6</RequestID>
</Response>
```

`400` `InvalidIpamPrefixListResolverId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteIpamPrefixListResolverTarget

Deletes an IPAM prefix list resolver target.

```http
POST / HTTP/1.1

Action=DeleteIpamPrefixListResolverTarget
&Version=2016-11-15
&IpamPrefixListResolverTargetId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteIpamPrefixListResolverTarget is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000b7</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteIpamResourceDiscovery

Deletes an IPAM resource discovery.

```http
POST / HTTP/1.1

Action=DeleteIpamResourceDiscovery
&Version=2016-11-15
&IpamResourceDiscoveryId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteIpamResourceDiscovery is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000b8</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteIpamRoutingPolicyRegistration

Deletes a routing policy registration for a specified CIDR prefix.

```http
POST / HTTP/1.1

Action=DeleteIpamRoutingPolicyRegistration
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
&Cidr=example
&ClientToken=24b7ec78-59a6-4e8f-a156-6e17bfccd420
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamInternetRegistryAssociationId.Malformed</Code>
      <Message>The ipam-internet-registry-association ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000b9</RequestID>
</Response>
```

`400` `InvalidIpamInternetRegistryAssociationId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteIpamScope

Delete the scope for an IPAM.

```http
POST / HTTP/1.1

Action=DeleteIpamScope
&Version=2016-11-15
&IpamScopeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamScopeId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ba</RequestID>
</Response>
```

`400` `InvalidIpamScopeId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteKeyPair

Deletes the specified key pair, by removing the public key from Amazon EC2.

```http
POST / HTTP/1.1

Action=DeleteKeyPair
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either the parameter KeyName or KeyPairId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000bb</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### DeleteLaunchTemplate

Deletes a launch template.

```http
POST / HTTP/1.1

Action=DeleteLaunchTemplate
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either the parameter LaunchTemplateId or the parameter LaunchTemplateName</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000bc</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### DeleteLaunchTemplateVersions

Deletes one or more versions of a launch template.

```http
POST / HTTP/1.1

Action=DeleteLaunchTemplateVersions
&Version=2016-11-15
&LaunchTemplateVersion.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value 'example' for version. Valid values are $Latest, $Default, and version numbers</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000bd</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteLocalGatewayRoute

Deletes the specified route from the specified local gateway route table.

```http
POST / HTTP/1.1

Action=DeleteLocalGatewayRoute
&Version=2016-11-15
&LocalGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayRouteTableID.Malformed</Code>
      <Message>The local-gateway-route-table ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000be</RequestID>
</Response>
```

`400` `InvalidLocalGatewayRouteTableID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteLocalGatewayRouteTable

Deletes a local gateway route table.

```http
POST / HTTP/1.1

Action=DeleteLocalGatewayRouteTable
&Version=2016-11-15
&LocalGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayRouteTableID.Malformed</Code>
      <Message>The local-gateway-route-table ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000bf</RequestID>
</Response>
```

`400` `InvalidLocalGatewayRouteTableID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociation

Deletes a local gateway route table virtual interface group association.

```http
POST / HTTP/1.1

Action=DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociation
&Version=2016-11-15
&LocalGatewayRouteTableVirtualInterfaceGroupAssociationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteLocalGatewayRouteTableVirtualInterfaceGroupAssociation is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000c0</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteLocalGatewayRouteTableVpcAssociation

Deletes the specified association between a VPC and local gateway route table.

```http
POST / HTTP/1.1

Action=DeleteLocalGatewayRouteTableVpcAssociation
&Version=2016-11-15
&LocalGatewayRouteTableVpcAssociationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteLocalGatewayRouteTableVpcAssociation is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000c1</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteLocalGatewayVirtualInterface

Deletes the specified local gateway virtual interface.

```http
POST / HTTP/1.1

Action=DeleteLocalGatewayVirtualInterface
&Version=2016-11-15
&LocalGatewayVirtualInterfaceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayVirtualInterfaceID.Malformed</Code>
      <Message>The local-gateway-virtual-interface ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000c2</RequestID>
</Response>
```

`400` `InvalidLocalGatewayVirtualInterfaceID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteLocalGatewayVirtualInterfaceGroup

Delete the specified local gateway interface group.

```http
POST / HTTP/1.1

Action=DeleteLocalGatewayVirtualInterfaceGroup
&Version=2016-11-15
&LocalGatewayVirtualInterfaceGroupId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayVirtualInterfaceGroupID.Malformed</Code>
      <Message>The local-gateway-virtual-interface-group ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000c3</RequestID>
</Response>
```

`400` `InvalidLocalGatewayVirtualInterfaceGroupID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteManagedPrefixList

Deletes the specified managed prefix list.

```http
POST / HTTP/1.1

Action=DeleteManagedPrefixList
&Version=2016-11-15
&PrefixListId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPrefixListId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000c4</RequestID>
</Response>
```

`400` `InvalidPrefixListId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteNatGateway

Deletes the specified NAT gateway.

```http
POST / HTTP/1.1

Action=DeleteNatGateway
&Version=2016-11-15
&NatGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NatGatewayMalformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000c5</RequestID>
</Response>
```

`400` `NatGatewayMalformed`: the placeholder values failed the operation's input validation.

### DeleteNetworkAcl

Deletes the specified network ACL.

```http
POST / HTTP/1.1

Action=DeleteNetworkAcl
&Version=2016-11-15
&NetworkAclId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkAclID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000c6</RequestID>
</Response>
```

`400` `InvalidNetworkAclID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteNetworkAclEntry

Deletes the specified ingress or egress entry (rule) from the specified network ACL.

```http
POST / HTTP/1.1

Action=DeleteNetworkAclEntry
&Version=2016-11-15
&NetworkAclId=example
&RuleNumber=1
&Egress=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkAclID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000c7</RequestID>
</Response>
```

`400` `InvalidNetworkAclID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteNetworkInsightsAccessScope

Deletes the specified Network Access Scope.

```http
POST / HTTP/1.1

Action=DeleteNetworkInsightsAccessScope
&Version=2016-11-15
&NetworkInsightsAccessScopeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>NetworkInsightsAccessScopeId (example) is not valid</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000c8</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteNetworkInsightsAccessScopeAnalysis

Deletes the specified Network Access Scope analysis.

```http
POST / HTTP/1.1

Action=DeleteNetworkInsightsAccessScopeAnalysis
&Version=2016-11-15
&NetworkInsightsAccessScopeAnalysisId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>NetworkInsightsAccessScopeAnalysisId (example) is not valid</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000c9</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteNetworkInsightsAnalysis

Deletes the specified network insights analysis.

```http
POST / HTTP/1.1

Action=DeleteNetworkInsightsAnalysis
&Version=2016-11-15
&NetworkInsightsAnalysisId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteNetworkInsightsAnalysis is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ca</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteNetworkInsightsPath

Deletes the specified path.

```http
POST / HTTP/1.1

Action=DeleteNetworkInsightsPath
&Version=2016-11-15
&NetworkInsightsPathId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>NetworkInsightsPathId (example) is not valid</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000cb</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteNetworkInterface

Deletes the specified network interface.

```http
POST / HTTP/1.1

Action=DeleteNetworkInterface
&Version=2016-11-15
&NetworkInterfaceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkInterfaceId.Malformed</Code>
      <Message>The interface ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000cc</RequestID>
</Response>
```

`400` `InvalidNetworkInterfaceId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteNetworkInterfacePermission

Deletes a permission for a network interface.

```http
POST / HTTP/1.1

Action=DeleteNetworkInterfacePermission
&Version=2016-11-15
&NetworkInterfacePermissionId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteNetworkInterfacePermission is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000cd</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeletePlacementGroup

Deletes the specified placement group.

```http
POST / HTTP/1.1

Action=DeletePlacementGroup
&Version=2016-11-15
&GroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPlacementGroup.Unknown</Code>
      <Message>The placement group 'example' is unknown.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ce</RequestID>
</Response>
```

`400` `InvalidPlacementGroup.Unknown`.

### DeletePublicIpv4Pool

Delete a public IPv4 pool.

```http
POST / HTTP/1.1

Action=DeletePublicIpv4Pool
&Version=2016-11-15
&PoolId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPublicIpv4Pool.NotFound</Code>
      <Message>The pool ID 'example' does not exist.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000cf</RequestID>
</Response>
```

`400` `InvalidPublicIpv4Pool.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteQueuedReservedInstances

Deletes the queued purchases for the specified Reserved Instances.

```http
POST / HTTP/1.1

Action=DeleteQueuedReservedInstances
&Version=2016-11-15
&ReservedInstancesId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value 'example' for reservedInstancesId. A Reserved Instances ID is a UUID</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000d0</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteRoute

Deletes the specified route from the specified route table.

```http
POST / HTTP/1.1

Action=DeleteRoute
&Version=2016-11-15
&RouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either the parameter destinationCidrBlock, destinationIpv6CidrBlock or destinationPrefixListId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000d1</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### DeleteRouteServer

Deletes the specified route server.

```http
POST / HTTP/1.1

Action=DeleteRouteServer
&Version=2016-11-15
&RouteServerId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000d2</RequestID>
</Response>
```

`400` `InvalidRouteServerId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteRouteServerEndpoint

Deletes the specified route server endpoint.

```http
POST / HTTP/1.1

Action=DeleteRouteServerEndpoint
&Version=2016-11-15
&RouteServerEndpointId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerEndpointId.NotFound</Code>
      <Message>The route-server-endpoint ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000d3</RequestID>
</Response>
```

`400` `InvalidRouteServerEndpointId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRouteServerPeer

Deletes the specified BGP peer from a route server.

```http
POST / HTTP/1.1

Action=DeleteRouteServerPeer
&Version=2016-11-15
&RouteServerPeerId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerPeerId.NotFound</Code>
      <Message>The route-server-peer ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000d4</RequestID>
</Response>
```

`400` `InvalidRouteServerPeerId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteRouteTable

Deletes the specified route table.

```http
POST / HTTP/1.1

Action=DeleteRouteTable
&Version=2016-11-15
&RouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableID.NotFound</Code>
      <Message>The routeTable ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000d5</RequestID>
</Response>
```

`400` `InvalidRouteTableID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSecondaryNetwork

Deletes a secondary network.

```http
POST / HTTP/1.1

Action=DeleteSecondaryNetwork
&Version=2016-11-15
&ClientToken=0cecf137-08e1-4caa-8fe5-dd3d2e68b7ff
&SecondaryNetworkId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSecondaryNetworkId.Malformed</Code>
      <Message>The secondary-network ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000d6</RequestID>
</Response>
```

`400` `InvalidSecondaryNetworkId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteSecondarySubnet

Deletes a secondary subnet.

```http
POST / HTTP/1.1

Action=DeleteSecondarySubnet
&Version=2016-11-15
&ClientToken=85ca307e-dc62-4dd6-b401-356ff57d678c
&SecondarySubnetId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSecondarySubnetId.Malformed</Code>
      <Message>The secondary-subnet ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000d7</RequestID>
</Response>
```

`400` `InvalidSecondarySubnetId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteSecurityGroup

Deletes a security group.

```http
POST / HTTP/1.1

Action=DeleteSecurityGroup
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either the parameter GroupId or GroupName</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000d8</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation. See [the walkthrough](#11-deletesecuritygroup) for a successful call.

### DeleteSnapshot

Deletes the specified snapshot.

```http
POST / HTTP/1.1

Action=DeleteSnapshot
&Version=2016-11-15
&SnapshotId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSnapshotID.Malformed</Code>
      <Message>Value (example) for snapshot ID is invalid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000d9</RequestID>
</Response>
```

`400` `InvalidSnapshotID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteSpotDatafeedSubscription

Deletes the data feed for Spot Instances.

```http
POST / HTTP/1.1

Action=DeleteSpotDatafeedSubscription
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DeleteSpotDatafeedSubscriptionResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <return>true</return>
  <requestId>7c95e5b4-0060-44bd-beee-2051fde1f6d8</requestId>
</DeleteSpotDatafeedSubscriptionResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteSubnet

Deletes the specified subnet.

```http
POST / HTTP/1.1

Action=DeleteSubnet
&Version=2016-11-15
&SubnetId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSubnetID.NotFound</Code>
      <Message>The subnet ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000da</RequestID>
</Response>
```

`400` `InvalidSubnetID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#9-deletesubnet) for a successful call.

### DeleteSubnetCidrReservation

Deletes a subnet CIDR reservation.

```http
POST / HTTP/1.1

Action=DeleteSubnetCidrReservation
&Version=2016-11-15
&SubnetCidrReservationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSubnetCidrReservationID.NotFound</Code>
      <Message>The subnet-cidr-reservation ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000db</RequestID>
</Response>
```

`400` `InvalidSubnetCidrReservationID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTags

Deletes the specified set of tags from the specified set of resources.

```http
POST / HTTP/1.1

Action=DeleteTags
&Version=2016-11-15
&ResourceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidID</Code>
      <Message>The ID 'example' is not valid</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000dc</RequestID>
</Response>
```

`400` `InvalidID`.

### DeleteTrafficMirrorFilter

Deletes the specified Traffic Mirror filter.

```http
POST / HTTP/1.1

Action=DeleteTrafficMirrorFilter
&Version=2016-11-15
&TrafficMirrorFilterId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTrafficMirrorFilterId.NotFound</Code>
      <Message>example does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000dd</RequestID>
</Response>
```

`400` `InvalidTrafficMirrorFilterId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTrafficMirrorFilterRule

Deletes the specified Traffic Mirror rule.

```http
POST / HTTP/1.1

Action=DeleteTrafficMirrorFilterRule
&Version=2016-11-15
&TrafficMirrorFilterRuleId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteTrafficMirrorFilterRule is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000de</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteTrafficMirrorSession

Deletes the specified Traffic Mirror session.

```http
POST / HTTP/1.1

Action=DeleteTrafficMirrorSession
&Version=2016-11-15
&TrafficMirrorSessionId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTrafficMirrorSessionId.NotFound</Code>
      <Message>example does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000df</RequestID>
</Response>
```

`400` `InvalidTrafficMirrorSessionId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTrafficMirrorTarget

Deletes the specified Traffic Mirror target.

```http
POST / HTTP/1.1

Action=DeleteTrafficMirrorTarget
&Version=2016-11-15
&TrafficMirrorTargetId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTrafficMirrorTargetId.NotFound</Code>
      <Message>example does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000e0</RequestID>
</Response>
```

`400` `InvalidTrafficMirrorTargetId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTransitGateway

Deletes the specified transit gateway.

```http
POST / HTTP/1.1

Action=DeleteTransitGateway
&Version=2016-11-15
&TransitGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayID.NotFound</Code>
      <Message>Transit Gateway example was deleted or does not exist.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000e1</RequestID>
</Response>
```

`400` `InvalidTransitGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTransitGatewayClientVpnAttachment

Deletes a Transit Gateway attachment for a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayClientVpnAttachment
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteTransitGatewayClientVpnAttachment is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000e2</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteTransitGatewayConnect

Deletes the specified Connect attachment.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayConnect
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteTransitGatewayConnect is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000e3</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteTransitGatewayConnectPeer

Deletes the specified Connect peer.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayConnectPeer
&Version=2016-11-15
&TransitGatewayConnectPeerId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayConnectPeerID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000e4</RequestID>
</Response>
```

`400` `InvalidTransitGatewayConnectPeerID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteTransitGatewayMeteringPolicy

Deletes a transit gateway metering policy.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayMeteringPolicy
&Version=2016-11-15
&TransitGatewayMeteringPolicyId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayMeteringPolicyIdMalformedException</Code>
      <Message>The transit-gateway-metering-policy ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000e5</RequestID>
</Response>
```

`400` `InvalidTransitGatewayMeteringPolicyIdMalformedException`: the placeholder values failed the operation's input validation.

### DeleteTransitGatewayMeteringPolicyEntry

Deletes an entry from a transit gateway metering policy.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayMeteringPolicyEntry
&Version=2016-11-15
&TransitGatewayMeteringPolicyId=example
&PolicyRuleNumber=1
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteTransitGatewayMeteringPolicyEntry is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000e6</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteTransitGatewayMulticastDomain

Deletes the specified transit gateway multicast domain.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayMulticastDomain
&Version=2016-11-15
&TransitGatewayMulticastDomainId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteTransitGatewayMulticastDomain is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000e7</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteTransitGatewayPeeringAttachment

Deletes a transit gateway peering attachment.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayPeeringAttachment
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteTransitGatewayPeeringAttachment is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000e8</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteTransitGatewayPolicyTable

Deletes the specified transit gateway policy table.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayPolicyTable
&Version=2016-11-15
&TransitGatewayPolicyTableId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteTransitGatewayPolicyTable is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000e9</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteTransitGatewayPolicyTableEntry

Deletes the specified transit gateway policy table entry.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayPolicyTableEntry
&Version=2016-11-15
&TransitGatewayPolicyTableId=example
&PolicyRuleNumber=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayPolicyTableId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ea</RequestID>
</Response>
```

`400` `InvalidTransitGatewayPolicyTableId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteTransitGatewayPrefixListReference

Deletes a reference (route) to a prefix list in a specified transit gateway route table.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayPrefixListReference
&Version=2016-11-15
&TransitGatewayRouteTableId=example
&PrefixListId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableId.Malformed</Code>
      <Message>Invalid Transit Gateway Route Table id example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000eb</RequestID>
</Response>
```

`400` `InvalidRouteTableId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteTransitGatewayRoute

Deletes the specified route from the specified transit gateway route table.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayRoute
&Version=2016-11-15
&TransitGatewayRouteTableId=example
&DestinationCidrBlock=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteTransitGatewayRoute is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ec</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteTransitGatewayRouteTable

Deletes the specified transit gateway route table.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayRouteTable
&Version=2016-11-15
&TransitGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableId.Malformed</Code>
      <Message>Invalid Transit Gateway Route Table id example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ed</RequestID>
</Response>
```

`400` `InvalidRouteTableId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteTransitGatewayRouteTableAnnouncement

Advertises to the transit gateway that a transit gateway route table is deleted.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayRouteTableAnnouncement
&Version=2016-11-15
&TransitGatewayRouteTableAnnouncementId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayRouteTableAnnouncementId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ee</RequestID>
</Response>
```

`400` `InvalidTransitGatewayRouteTableAnnouncementId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteTransitGatewayVpcAttachment

Deletes the specified VPC attachment.

```http
POST / HTTP/1.1

Action=DeleteTransitGatewayVpcAttachment
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayAttachmentID.Malformed</Code>
      <Message>Invalid Transit Gateway Attachment id.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ef</RequestID>
</Response>
```

`400` `InvalidTransitGatewayAttachmentID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteVerifiedAccessEndpoint

Delete an Amazon Web Services Verified Access endpoint.

```http
POST / HTTP/1.1

Action=DeleteVerifiedAccessEndpoint
&Version=2016-11-15
&VerifiedAccessEndpointId=example
&ClientToken=d46f766f-2902-4438-b496-b2559a38750c
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVerifiedAccessEndpointId.NotFound</Code>
      <Message>The verified-access-endpoint ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000f0</RequestID>
</Response>
```

`400` `InvalidVerifiedAccessEndpointId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVerifiedAccessGroup

Delete an Amazon Web Services Verified Access group.

```http
POST / HTTP/1.1

Action=DeleteVerifiedAccessGroup
&Version=2016-11-15
&VerifiedAccessGroupId=example
&ClientToken=902a4413-6bd3-4a67-8bd4-dd37fde10943
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVerifiedAccessGroupId.NotFound</Code>
      <Message>The verified-access-group ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000f1</RequestID>
</Response>
```

`400` `InvalidVerifiedAccessGroupId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVerifiedAccessInstance

Delete an Amazon Web Services Verified Access instance.

```http
POST / HTTP/1.1

Action=DeleteVerifiedAccessInstance
&Version=2016-11-15
&VerifiedAccessInstanceId=example
&ClientToken=700dd373-c29b-4723-8819-c04a1490101e
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVerifiedAccessInstanceId.NotFound</Code>
      <Message>The verified-access-instance ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000f2</RequestID>
</Response>
```

`400` `InvalidVerifiedAccessInstanceId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVerifiedAccessTrustProvider

Delete an Amazon Web Services Verified Access trust provider.

```http
POST / HTTP/1.1

Action=DeleteVerifiedAccessTrustProvider
&Version=2016-11-15
&VerifiedAccessTrustProviderId=example
&ClientToken=9f743196-689f-4c1b-af52-b2e844b65087
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVerifiedAccessTrustProviderId.NotFound</Code>
      <Message>VerifiedAccessTrustProvider example does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000f3</RequestID>
</Response>
```

`400` `InvalidVerifiedAccessTrustProviderId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVolume

Deletes the specified EBS volume.

```http
POST / HTTP/1.1

Action=DeleteVolume
&Version=2016-11-15
&VolumeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVolumeID.Malformed</Code>
      <Message>The volume ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000f4</RequestID>
</Response>
```

`400` `InvalidVolumeID.Malformed`: the placeholder values failed the operation's input validation.

### DeleteVpc

Deletes the specified VPC.

```http
POST / HTTP/1.1

Action=DeleteVpc
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000f5</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#12-deletevpc) for a successful call.

### DeleteVpcBlockPublicAccessExclusion

Delete a VPC Block Public Access (BPA) exclusion.

```http
POST / HTTP/1.1

Action=DeleteVpcBlockPublicAccessExclusion
&Version=2016-11-15
&ExclusionId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteVpcBlockPublicAccessExclusion is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000f6</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteVpcEncryptionControl

Deletes a VPC Encryption Control configuration.

```http
POST / HTTP/1.1

Action=DeleteVpcEncryptionControl
&Version=2016-11-15
&VpcEncryptionControlId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcEncryptionControlId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000f7</RequestID>
</Response>
```

`400` `InvalidVpcEncryptionControlId.Malformed`: the placeholder values failed the operation's input validation.

### DeleteVpcEndpointConnectionNotifications

Deletes the specified VPC endpoint connection notifications.

```http
POST / HTTP/1.1

Action=DeleteVpcEndpointConnectionNotifications
&Version=2016-11-15
&ConnectionNotificationId.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteVpcEndpointConnectionNotifications is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000f8</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteVpcEndpointServiceConfigurations

Deletes the specified VPC endpoint service configurations.

```http
POST / HTTP/1.1

Action=DeleteVpcEndpointServiceConfigurations
&Version=2016-11-15
&ServiceId.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteVpcEndpointServiceConfigurations is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000f9</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteVpcEndpoints

Deletes the specified VPC endpoints.

```http
POST / HTTP/1.1

Action=DeleteVpcEndpoints
&Version=2016-11-15
&VpcEndpointId.1=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DeleteVpcEndpointsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <unsuccessful>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <error>
        <code>InvalidVpcEndpointId.Malformed</code>
        <message>Invalid id: &quot;example&quot;</message>
      </error>
      <resourceId>example</resourceId>
    </item>
  </unsuccessful>
… (2 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteVpcPeeringConnection

Deletes a VPC peering connection.

```http
POST / HTTP/1.1

Action=DeleteVpcPeeringConnection
&Version=2016-11-15
&VpcPeeringConnectionId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeleteVpcPeeringConnection is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000fa</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteVpnConcentrator

Deletes the specified VPN concentrator.

```http
POST / HTTP/1.1

Action=DeleteVpnConcentrator
&Version=2016-11-15
&VpnConcentratorId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpnConcentratorID.NotFound</Code>
      <Message>The vpn-concentrator ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000fb</RequestID>
</Response>
```

`400` `InvalidVpnConcentratorID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVpnConnection

Deletes the specified VPN connection.

```http
POST / HTTP/1.1

Action=DeleteVpnConnection
&Version=2016-11-15
&VpnConnectionId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpnConnectionID.NotFound</Code>
      <Message>The vpnConnection ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000fc</RequestID>
</Response>
```

`400` `InvalidVpnConnectionID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteVpnConnectionRoute

Deletes the specified static route associated with a VPN connection between an existing virtual private gateway and a VPN customer gateway.

```http
POST / HTTP/1.1

Action=DeleteVpnConnectionRoute
&Version=2016-11-15
&DestinationCidrBlock=example
&VpnConnectionId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter destinationCidrBlock is invalid. This is not a valid CIDR block.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000fd</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteVpnGateway

Deletes the specified virtual private gateway.

```http
POST / HTTP/1.1

Action=DeleteVpnGateway
&Version=2016-11-15
&VpnGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpnGatewayID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000fe</RequestID>
</Response>
```

`400` `InvalidVpnGatewayID.Malformed`: the placeholder values failed the operation's input validation.

### DeprovisionByoipCidr

Releases the specified address range that you provisioned for use with your Amazon Web Services resources through bring your own IP addresses (BYOIP) and deletes the corresponding address pool.

```http
POST / HTTP/1.1

Action=DeprovisionByoipCidr
&Version=2016-11-15
&Cidr=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>The CIDR (example) is not valid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000000ff</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeprovisionIpamByoasn

Deprovisions your Autonomous System Number (ASN) from your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=DeprovisionIpamByoasn
&Version=2016-11-15
&IpamId=example
&Asn=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000100</RequestID>
</Response>
```

`400` `InvalidIpamId.Malformed`: the placeholder values failed the operation's input validation.

### DeprovisionIpamPoolCidr

Deprovision a CIDR provisioned from an IPAM pool.

```http
POST / HTTP/1.1

Action=DeprovisionIpamPoolCidr
&Version=2016-11-15
&IpamPoolId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamPoolId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000101</RequestID>
</Response>
```

`400` `InvalidIpamPoolId.Malformed`: the placeholder values failed the operation's input validation.

### DeprovisionPublicIpv4PoolCidr

Deprovision a CIDR from a public IPv4 pool.

```http
POST / HTTP/1.1

Action=DeprovisionPublicIpv4PoolCidr
&Version=2016-11-15
&PoolId=example
&Cidr=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter cidr is invalid. This is not a valid IPv4 CIDR block.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000102</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeregisterImage

Deregisters the specified AMI.

```http
POST / HTTP/1.1

Action=DeregisterImage
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAMIID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000103</RequestID>
</Response>
```

`400` `InvalidAMIID.Malformed`: the placeholder values failed the operation's input validation.

### DeregisterInstanceEventNotificationAttributes

Deregisters tag keys to prevent tags that have the specified tag keys from being included in scheduled event notifications for resources in the Region.

```http
POST / HTTP/1.1

Action=DeregisterInstanceEventNotificationAttributes
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'instanceTagAttribute' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000104</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### DeregisterTransitGatewayMulticastGroupMembers

Deregisters the specified members (network interfaces) from the transit gateway multicast group.

```http
POST / HTTP/1.1

Action=DeregisterTransitGatewayMulticastGroupMembers
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeregisterTransitGatewayMulticastGroupMembers is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000105</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeregisterTransitGatewayMulticastGroupSources

Deregisters the specified sources (network interfaces) from the transit gateway multicast group.

```http
POST / HTTP/1.1

Action=DeregisterTransitGatewayMulticastGroupSources
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DeregisterTransitGatewayMulticastGroupSources is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000106</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeAccountAttributes

Describes attributes of your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=DescribeAccountAttributes
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeAccountAttributesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <accountAttributeSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <attributeName>default-vpc</attributeName>
      <attributeValueSet>
        <item>
          <attributeValue>vpc-00000000000000001</attributeValue>
        </item>
      </attributeValueSet>
    </item>
… (43 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAccountVpcEncryptionControl

Describes the account-level VPC Encryption Control configuration for your account.

```http
POST / HTTP/1.1

Action=DescribeAccountVpcEncryptionControl
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeAccountVpcEncryptionControlResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <accountVpcEncryptionControl xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <state>default-state</state>
    <mode>unmanaged</mode>
    <exclusions>
      <internetGateway>disabled</internetGateway>
      <egressOnlyInternetGateway>disabled</egressOnlyInternetGateway>
      <natGateway>disabled</natGateway>
      <virtualPrivateGateway>disabled</virtualPrivateGateway>
      <vpcPeering>disabled</vpcPeering>
… (8 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAddressTransfers

Describes an Elastic IP address transfer.

```http
POST / HTTP/1.1

Action=DescribeAddressTransfers
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeAddressTransfersResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <addressTransferSet/>
  <requestId>5970939e-f1ea-4f36-9e5d-45291133a803</requestId>
</DescribeAddressTransfersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAddresses

Describes the specified Elastic IP addresses or all of your Elastic IP addresses.

```http
POST / HTTP/1.1

Action=DescribeAddresses
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeAddressesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <addressesSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <allocationId>eipalloc-000000000000000ad</allocationId>
      <domain>vpc</domain>
      <tagSet/>
      <publicIpv4Pool>amazon</publicIpv4Pool>
      <networkBorderGroup>us-east-1</networkBorderGroup>
      <publicIp>54.0.0.3</publicIp>
    </item>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAddressesAttribute

Describes the attributes of the specified Elastic IP addresses.

```http
POST / HTTP/1.1

Action=DescribeAddressesAttribute
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeAddressesAttributeResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <addressSet/>
  <requestId>f01e8f69-ab01-4cdd-b071-4139bfec37d0</requestId>
</DescribeAddressesAttributeResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAggregateIdFormat

Describes the longer ID format settings for all resource types in a specific Region.

```http
POST / HTTP/1.1

Action=DescribeAggregateIdFormat
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeAggregateIdFormatResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <useLongIdsAggregated>true</useLongIdsAggregated>
  <statusSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <resource>bundle</resource>
      <useLongIds>true</useLongIds>
    </item>
    <item>
      <resource>conversion-task</resource>
      <useLongIds>true</useLongIds>
… (120 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeApplicationStatus

Describes the aggregated application health status for the specified instances.

```http
POST / HTTP/1.1

Action=DescribeApplicationStatus
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeApplicationStatusResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <applicationStatusesResponseType xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <instanceSet>
      <item>
        <instanceId>i-00000000000000001</instanceId>
        <availabilityZone>us-east-1a</availabilityZone>
        <availabilityZoneId>use1-az1</availabilityZoneId>
        <applicationStatus>
          <status>not-applicable</status>
          <statusTimeStamp>2026-11-16T12:05:29.040129Z</statusTimeStamp>
… (26 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeApplicationStatusCheckAssociations

Describes the associations for one or more application status checks.

```http
POST / HTTP/1.1

Action=DescribeApplicationStatusCheckAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeApplicationStatusCheckAssociations is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000107</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeApplicationStatusChecks

Describes application status checks, including configuration details such as protocol, port, path, thresholds, and associations.

```http
POST / HTTP/1.1

Action=DescribeApplicationStatusChecks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeApplicationStatusChecksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <applicationStatusCheckSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <applicationStatusCheckId>asc-000000000000000ae</applicationStatusCheckId>
      <aggregation>included</aggregation>
      <healthCheckPathSet/>
      <protocol>http</protocol>
      <port>1</port>
      <path>/</path>
      <deviceIndex>0</deviceIndex>
… (15 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAvailabilityZones

Describes the Availability Zones, Local Zones, and Wavelength Zones that are available to you.

```http
POST / HTTP/1.1

Action=DescribeAvailabilityZones
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeAvailabilityZonesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <availabilityZoneInfo>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <optInStatus>opt-in-not-required</optInStatus>
      <messageSet/>
      <regionName>us-east-1</regionName>
      <zoneName>us-east-1a</zoneName>
      <zoneId>use1-az1</zoneId>
      <groupName>us-east-1-zg-1</groupName>
      <networkBorderGroup>us-east-1</networkBorderGroup>
… (61 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeAwsNetworkPerformanceMetricSubscriptions

Describes the current Infrastructure Performance metric subscriptions.

```http
POST / HTTP/1.1

Action=DescribeAwsNetworkPerformanceMetricSubscriptions
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeAwsNetworkPerformanceMetricSubscriptionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <subscriptionSet/>
  <requestId>5d5dd88e-6dfb-4bf2-a63e-bcd321bc66e7</requestId>
</DescribeAwsNetworkPerformanceMetricSubscriptionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeBundleTasks

Describes the specified bundle tasks or all of your bundle tasks.

```http
POST / HTTP/1.1

Action=DescribeBundleTasks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeBundleTasksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <bundleInstanceTasksSet/>
  <requestId>240facd4-fb7b-4b7f-9730-6dc1cebbda80</requestId>
</DescribeBundleTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeByoipCidrs

Describes the IP address ranges that were provisioned for use with Amazon Web Services resources through through bring your own IP addresses (BYOIP).

```http
POST / HTTP/1.1

Action=DescribeByoipCidrs
&Version=2016-11-15
&MaxResults=1
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeByoipCidrsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <byoipCidrSet/>
  <requestId>cb88963f-533b-45c6-bdbb-161d61ac33d0</requestId>
</DescribeByoipCidrsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCapacityBlockExtensionHistory

Describes the events for the specified Capacity Block extension during the specified time.

```http
POST / HTTP/1.1

Action=DescribeCapacityBlockExtensionHistory
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeCapacityBlockExtensionHistory is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000108</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeCapacityBlockExtensionOfferings

Describes Capacity Block extension offerings available for purchase in the Amazon Web Services Region that you're currently using.

```http
POST / HTTP/1.1

Action=DescribeCapacityBlockExtensionOfferings
&Version=2016-11-15
&CapacityBlockExtensionDurationHours=1
&CapacityReservationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000109</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### DescribeCapacityBlockOfferings

Describes Capacity Block offerings available for purchase in the Amazon Web Services Region that you're currently using.

```http
POST / HTTP/1.1

Action=DescribeCapacityBlockOfferings
&Version=2016-11-15
&CapacityDurationHours=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>CapacityDurationHours must be a multiple of 24 up to 336 hours (14 days), or a multiple of 168 up to 4368 hours (182 days); got 1</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000010a</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribeCapacityBlockStatus

Describes the availability of capacity for the specified Capacity blocks, or all of your Capacity Blocks.

```http
POST / HTTP/1.1

Action=DescribeCapacityBlockStatus
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCapacityBlockStatusResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityBlockStatusSet/>
  <requestId>a6452533-b4a5-4b2a-82f9-92e87c33bd71</requestId>
</DescribeCapacityBlockStatusResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCapacityBlocks

Describes details about Capacity Blocks in the Amazon Web Services Region that you're currently using.

```http
POST / HTTP/1.1

Action=DescribeCapacityBlocks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCapacityBlocksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityBlockSet/>
  <requestId>edc83a57-5f52-4408-ac51-8fe1f5de6091</requestId>
</DescribeCapacityBlocksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCapacityManagerDataExports

Describes one or more Capacity Manager data export configurations.

```http
POST / HTTP/1.1

Action=DescribeCapacityManagerDataExports
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCapacityManagerDataExportsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityManagerDataExportSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <capacityManagerDataExportId>cmde-000000000000000af</capacityManagerDataExportId>
      <s3BucketName>example</s3BucketName>
      <schedule>hourly</schedule>
      <outputFormat>csv</outputFormat>
      <createTime>2026-11-16T12:11:31.044165Z</createTime>
      <tagSet/>
    </item>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCapacityReservationBillingRequests

Describes a request to assign the billing of the unused capacity of a Capacity Reservation.

```http
POST / HTTP/1.1

Action=DescribeCapacityReservationBillingRequests
&Version=2016-11-15
&Role=odcr-owner
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCapacityReservationBillingRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityReservationBillingRequestSet/>
  <requestId>724554da-d4e6-4e54-91e8-3805799bdf1c</requestId>
</DescribeCapacityReservationBillingRequestsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCapacityReservationCancellationQuotes

Describes one or more Capacity Reservation cancellation quotes.

```http
POST / HTTP/1.1

Action=DescribeCapacityReservationCancellationQuotes
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCapacityReservationCancellationQuotesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityReservationCancellationQuoteSet/>
  <requestId>c646a2b9-2dba-4e23-8f09-0fb0fefe1d48</requestId>
</DescribeCapacityReservationCancellationQuotesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCapacityReservationDateChangeQuotes

Describes one or more Capacity Reservation date change quotes that you generated by using the CreateCapacityReservationDateChangeQuote operation.

```http
POST / HTTP/1.1

Action=DescribeCapacityReservationDateChangeQuotes
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeCapacityReservationDateChangeQuotes is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000010b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeCapacityReservationFleets

Describes one or more Capacity Reservation Fleets.

```http
POST / HTTP/1.1

Action=DescribeCapacityReservationFleets
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCapacityReservationFleetsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityReservationFleetSet/>
  <requestId>aafa1e91-168e-4de1-830b-50a9740f4b9a</requestId>
</DescribeCapacityReservationFleetsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCapacityReservationTopology

Describes a tree-based hierarchy that represents the physical host placement of your pending or active Capacity Reservations within an Availability Zone or Local Zone.

```http
POST / HTTP/1.1

Action=DescribeCapacityReservationTopology
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCapacityReservationTopologyResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityReservationSet/>
  <requestId>2da934fe-c0d4-4556-a75d-4f84e29e2e6d</requestId>
</DescribeCapacityReservationTopologyResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCapacityReservations

Describes one or more of your Capacity Reservations.

```http
POST / HTTP/1.1

Action=DescribeCapacityReservations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCapacityReservationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityReservationSet/>
  <requestId>bffe9d69-0921-444a-9b4a-8f32daa801a1</requestId>
</DescribeCapacityReservationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCarrierGateways

Describes one or more of your carrier gateways.

```http
POST / HTTP/1.1

Action=DescribeCarrierGateways
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCarrierGatewaysResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <carrierGatewaySet/>
  <requestId>59bf8307-6848-4161-a878-9d67919daa6e</requestId>
</DescribeCarrierGatewaysResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeClassicLinkInstances

This action is deprecated.

```http
POST / HTTP/1.1

Action=DescribeClassicLinkInstances
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeClassicLinkInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instancesSet/>
  <requestId>5794b068-7dae-4c70-8a89-448630383b60</requestId>
</DescribeClassicLinkInstancesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeClientVpnAuthorizationRules

Describes the authorization rules for a specified Client VPN endpoint.

```http
POST / HTTP/1.1

Action=DescribeClientVpnAuthorizationRules
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeClientVpnAuthorizationRules is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000010c</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeClientVpnConnections

Describes active client connections and connections that have been terminated within the last 60 minutes for the specified Client VPN endpoint.

```http
POST / HTTP/1.1

Action=DescribeClientVpnConnections
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeClientVpnConnections is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000010d</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeClientVpnEndpoints

Describes one or more Client VPN endpoints in the account.

```http
POST / HTTP/1.1

Action=DescribeClientVpnEndpoints
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeClientVpnEndpointsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <clientVpnEndpoint/>
  <requestId>a39fdcfd-863a-41c6-965c-b0272cbe85cb</requestId>
</DescribeClientVpnEndpointsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeClientVpnRoutes

Describes the routes for the specified Client VPN endpoint.

```http
POST / HTTP/1.1

Action=DescribeClientVpnRoutes
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidClientVpnEndpointId.NotFound</Code>
      <Message>Endpoint example does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000010e</RequestID>
</Response>
```

`400` `InvalidClientVpnEndpointId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeClientVpnTargetNetworks

Describes the target networks associated with the specified Client VPN endpoint.

```http
POST / HTTP/1.1

Action=DescribeClientVpnTargetNetworks
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidClientVpnEndpointId.NotFound</Code>
      <Message>Endpoint example does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000010f</RequestID>
</Response>
```

`400` `InvalidClientVpnEndpointId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeCoipPools

Describes the specified customer-owned address pools or all of your customer-owned address pools.

```http
POST / HTTP/1.1

Action=DescribeCoipPools
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCoipPoolsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <coipPoolSet/>
  <requestId>7dda0af0-5ae6-4a9b-8808-3cf6d114097c</requestId>
</DescribeCoipPoolsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeConversionTasks

Describes the specified conversion tasks or all your conversion tasks.

```http
POST / HTTP/1.1

Action=DescribeConversionTasks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeConversionTasksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <conversionTasks/>
  <requestId>64b239ee-87c1-43d6-9817-f3aedf711e9d</requestId>
</DescribeConversionTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCustomerGateways

Describes one or more of your VPN customer gateways.

```http
POST / HTTP/1.1

Action=DescribeCustomerGateways
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeCustomerGatewaysResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <customerGatewaySet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <tagSet/>
      <customerGatewayId>cgw-000000000000000b0</customerGatewayId>
      <state>available</state>
      <type>ipsec.1</type>
      <bgpAsn>65000</bgpAsn>
    </item>
  </customerGatewaySet>
… (2 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDeclarativePoliciesReports

Describes the metadata of an account status report, including the status of the report.

```http
POST / HTTP/1.1

Action=DescribeDeclarativePoliciesReports
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeDeclarativePoliciesReportsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <reportSet/>
  <requestId>8b881dff-44a6-426d-ae05-a4c92c4da393</requestId>
</DescribeDeclarativePoliciesReportsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDhcpOptions

Describes your DHCP option sets.

```http
POST / HTTP/1.1

Action=DescribeDhcpOptions
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeDhcpOptionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <dhcpOptionsSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <ownerId>000000000000</ownerId>
      <tagSet/>
      <dhcpOptionsId>dopt-00000000000000003</dhcpOptionsId>
      <dhcpConfigurationSet>
        <item>
          <key>domain-name</key>
          <valueSet>
… (18 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeEgressOnlyInternetGateways

Describes your egress-only internet gateways.

```http
POST / HTTP/1.1

Action=DescribeEgressOnlyInternetGateways
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeEgressOnlyInternetGateways is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000110</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeElasticGpus

Amazon Elastic Graphics reached end of life on January 8, 2024.

```http
POST / HTTP/1.1

Action=DescribeElasticGpus
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>UnsupportedOperation</Code>
      <Message>The functionality you requested is not available in this region.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000111</RequestID>
</Response>
```

`400` `UnsupportedOperation`.

### DescribeExportImageTasks

Describes the specified export image tasks or all of your export image tasks.

```http
POST / HTTP/1.1

Action=DescribeExportImageTasks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeExportImageTasksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <exportImageTaskSet/>
  <requestId>a59223a0-4fbf-4d56-8d18-2ba2cb2a5bf6</requestId>
</DescribeExportImageTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeExportTasks

Describes the specified export instance tasks or all of your export instance tasks.

```http
POST / HTTP/1.1

Action=DescribeExportTasks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeExportTasksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <exportTaskSet/>
  <requestId>c19484d1-dfc6-466e-bcba-051b00e68732</requestId>
</DescribeExportTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeFastLaunchImages

Describe details for Windows AMIs that are configured for Windows fast launch.

```http
POST / HTTP/1.1

Action=DescribeFastLaunchImages
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeFastLaunchImagesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <fastLaunchImageSet/>
  <requestId>f771a55b-2257-45b8-aa27-bdb30e5abe48</requestId>
</DescribeFastLaunchImagesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeFastSnapshotRestores

Describes the state of fast snapshot restores for your snapshots.

```http
POST / HTTP/1.1

Action=DescribeFastSnapshotRestores
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeFastSnapshotRestoresResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <fastSnapshotRestoreSet/>
  <requestId>0532f287-1b64-49ed-9ae0-a17f14c00714</requestId>
</DescribeFastSnapshotRestoresResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeFleetHistory

Describes the events for the specified EC2 Fleet during the specified time.

```http
POST / HTTP/1.1

Action=DescribeFleetHistory
&Version=2016-11-15
&FleetId=example
&StartTime=2026-11-16T12%3A11%3A32Z
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeFleetHistory is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000112</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeFleetInstances

Describes the running instances for the specified EC2 Fleet.

```http
POST / HTTP/1.1

Action=DescribeFleetInstances
&Version=2016-11-15
&FleetId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidFleetId.Malformed</Code>
      <Message>The fleet ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000113</RequestID>
</Response>
```

`400` `InvalidFleetId.Malformed`: the placeholder values failed the operation's input validation.

### DescribeFleets

Describes the specified EC2 Fleet or all of your EC2 Fleets.

```http
POST / HTTP/1.1

Action=DescribeFleets
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeFleetsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <fleetSet/>
  <requestId>b5bafff4-48d5-41f5-bf22-de42cd99563d</requestId>
</DescribeFleetsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeFlowLogs

Describes one or more flow logs.

```http
POST / HTTP/1.1

Action=DescribeFlowLogs
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeFlowLogsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <flowLogSet/>
  <requestId>a0c5a517-e151-4959-8224-e5330b4ef85b</requestId>
</DescribeFlowLogsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeFpgaImageAttribute

Describes the specified attribute of the specified Amazon FPGA Image (AFI).

```http
POST / HTTP/1.1

Action=DescribeFpgaImageAttribute
&Version=2016-11-15
&FpgaImageId=example
&Attribute=description
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidFpgaImageID.Malformed</Code>
      <Message>Image ID 'example' is invalid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000114</RequestID>
</Response>
```

`400` `InvalidFpgaImageID.Malformed`: the placeholder values failed the operation's input validation.

### DescribeFpgaImages

Describes the Amazon FPGA Images (AFIs) available to you.

```http
POST / HTTP/1.1

Action=DescribeFpgaImages
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeFpgaImagesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <fpgaImageSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <fpgaImageId>afi-0c2b7c9d4e5f6a8b1</fpgaImageId>
      <fpgaImageGlobalId>agfi-0d4f3a2b1c9e8d7f6</fpgaImageGlobalId>
      <name>cl_hello_world</name>
      <description>AWS FPGA Hardware Development Kit example: hello world custom logic</description>
      <shellVersion>0x04261818</shellVersion>
      <pciId>
        <DeviceId>0xf000</DeviceId>
… (53 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeHostReservationOfferings

Describes the Dedicated Host reservations that are available to purchase.

```http
POST / HTTP/1.1

Action=DescribeHostReservationOfferings
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeHostReservationOfferingsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <nextToken>6563322d313a383732623436356261306433633437313a6d332030303331353336303030205061727469616c557066726f6e74</nextToken>
  <offeringSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <currencyCode>USD</currencyCode>
      <duration>31536000</duration>
      <hourlyPrice>0.000</hourlyPrice>
      <instanceFamily>a1</instanceFamily>
      <offeringId>hro-00000000000000001</offeringId>
      <paymentOption>AllUpfront</paymentOption>
… (4496 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeHostReservations

Describes reservations that are associated with Dedicated Hosts in your account.

```http
POST / HTTP/1.1

Action=DescribeHostReservations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeHostReservationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <hostReservationSet/>
  <requestId>3484385f-913e-4278-9f63-0ecf1cb225c6</requestId>
</DescribeHostReservationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeHosts

Describes the specified Dedicated Hosts or all your Dedicated Hosts.

```http
POST / HTTP/1.1

Action=DescribeHosts
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeHostsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <hostSet/>
  <requestId>a1345a7b-c561-4ac5-ad4c-99fa98b768da</requestId>
</DescribeHostsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIamInstanceProfileAssociations

Describes your IAM instance profile associations.

```http
POST / HTTP/1.1

Action=DescribeIamInstanceProfileAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIamInstanceProfileAssociationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <iamInstanceProfileAssociationSet/>
  <requestId>67f47b8e-6248-4e70-9320-dfc50b9a3735</requestId>
</DescribeIamInstanceProfileAssociationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIdFormat

Describes the ID format settings for your resources on a per-Region basis, for example, to view which resource types are enabled for longer IDs.

```http
POST / HTTP/1.1

Action=DescribeIdFormat
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeIdFormat is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000115</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeIdentityIdFormat

Describes the ID format settings for resources for the specified IAM user, IAM role, or root user.

```http
POST / HTTP/1.1

Action=DescribeIdentityIdFormat
&Version=2016-11-15
&PrincipalArn=arn%3Aaws%3Aec2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTargetArn.Unknown</Code>
      <Message>Invalid TargetArn: arn:aws:ec2:us-east-1:000000000000:example/example</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000116</RequestID>
</Response>
```

`400` `InvalidTargetArn.Unknown`.

### DescribeImageAttribute

Describes the specified attribute of the specified AMI.

```http
POST / HTTP/1.1

Action=DescribeImageAttribute
&Version=2016-11-15
&Attribute=description
&ImageId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAMIID.Malformed</Code>
      <Message>The image ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000117</RequestID>
</Response>
```

`400` `InvalidAMIID.Malformed`: the placeholder values failed the operation's input validation.

### DescribeImageReferences

Describes your Amazon Web Services resources that are referencing the specified images.

```http
POST / HTTP/1.1

Action=DescribeImageReferences
&Version=2016-11-15
&ImageId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>Either IncludeAllResourceTypes or ResourceTypes must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000118</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### DescribeImageUsageReportEntries

Describes the entries in image usage reports, showing how your images are used across other Amazon Web Services accounts.

```http
POST / HTTP/1.1

Action=DescribeImageUsageReportEntries
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeImageUsageReportEntries is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000119</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeImageUsageReports

Describes the configuration and status of image usage reports, filtered by report IDs or image IDs.

```http
POST / HTTP/1.1

Action=DescribeImageUsageReports
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeImageUsageReportsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <imageUsageReportSet/>
  <requestId>72c9806b-03e0-456f-9d77-cd00892211ce</requestId>
</DescribeImageUsageReportsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeImages

Describes the specified images (AMIs, AKIs, and ARIs) available to you or all of the images available to you.

```http
POST / HTTP/1.1

Action=DescribeImages
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeImagesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <imagesSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <platformDetails>Linux/UNIX</platformDetails>
      <usageOperation>RunInstances</usageOperation>
      <blockDeviceMapping>
        <item>
          <ebs>
            <deleteOnTermination>true</deleteOnTermination>
            <snapshotId>snap-00000000000000011</snapshotId>
… (2518 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeImportImageTasks

Displays details about an import virtual machine or import snapshot tasks that are already created.

```http
POST / HTTP/1.1

Action=DescribeImportImageTasks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeImportImageTasksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <importImageTaskSet/>
  <requestId>b5c2c6ca-0634-44ca-9eef-0062f249485e</requestId>
</DescribeImportImageTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeImportSnapshotTasks

Describes your import snapshot tasks.

```http
POST / HTTP/1.1

Action=DescribeImportSnapshotTasks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeImportSnapshotTasksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <importSnapshotTaskSet/>
  <requestId>128d02e9-941a-40c2-8ed2-64d2eaef447a</requestId>
</DescribeImportSnapshotTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstanceAttribute

Describes the specified attribute of the specified instance.

```http
POST / HTTP/1.1

Action=DescribeInstanceAttribute
&Version=2016-11-15
&InstanceId=example
&Attribute=instanceType
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter instanceId is invalid. Expected: 'i-...'.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000011a</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribeInstanceConnectEndpoints

Describes the specified EC2 Instance Connect Endpoints or all EC2 Instance Connect Endpoints.

```http
POST / HTTP/1.1

Action=DescribeInstanceConnectEndpoints
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeInstanceConnectEndpointsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instanceConnectEndpointSet/>
  <requestId>3bb63372-6e50-4c5b-aed4-41628a7b40bb</requestId>
</DescribeInstanceConnectEndpointsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstanceCreditSpecifications

Describes the credit option for CPU usage of the specified burstable performance instances.

```http
POST / HTTP/1.1

Action=DescribeInstanceCreditSpecifications
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeInstanceCreditSpecifications is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000011b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeInstanceEventNotificationAttributes

Describes the tag keys that are registered to appear in scheduled event notifications for resources in the current Region.

```http
POST / HTTP/1.1

Action=DescribeInstanceEventNotificationAttributes
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeInstanceEventNotificationAttributesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instanceTagAttribute xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <instanceTagKeySet/>
    <includeAllTagsOfInstance>false</includeAllTagsOfInstance>
  </instanceTagAttribute>
  <requestId>059f32be-048a-4244-83be-8b7eeba47ef0</requestId>
</DescribeInstanceEventNotificationAttributesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstanceEventWindows

Describes the specified event windows or all event windows.

```http
POST / HTTP/1.1

Action=DescribeInstanceEventWindows
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeInstanceEventWindowsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instanceEventWindowSet/>
  <requestId>f1e7014f-fd9e-4acc-ac48-73de7d8fa660</requestId>
</DescribeInstanceEventWindowsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstanceImageMetadata

Describes the AMI that was used to launch an instance, even if the AMI is deprecated, deregistered, made private (no longer public or shared with your account), or not allowed.

```http
POST / HTTP/1.1

Action=DescribeInstanceImageMetadata
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeInstanceImageMetadata is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000011c</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeInstanceSqlHaHistoryStates

Describes the historical SQL Server High Availability states for Amazon EC2 instances that are enabled for Amazon EC2 High Availability for SQL Server monitoring.

```http
POST / HTTP/1.1

Action=DescribeInstanceSqlHaHistoryStates
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeInstanceSqlHaHistoryStatesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instanceSet/>
  <requestId>153cdccf-d8a5-4434-ac80-25f3b42261a7</requestId>
</DescribeInstanceSqlHaHistoryStatesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstanceSqlHaStates

Describes the SQL Server High Availability states for Amazon EC2 instances that are enabled for Amazon EC2 High Availability for SQL Server monitoring.

```http
POST / HTTP/1.1

Action=DescribeInstanceSqlHaStates
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeInstanceSqlHaStatesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instanceSet/>
  <requestId>cc8ea299-c676-427e-a5dc-29c00fe3ae88</requestId>
</DescribeInstanceSqlHaStatesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstanceStatus

Describes the status of the specified instances or all of your instances.

```http
POST / HTTP/1.1

Action=DescribeInstanceStatus
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeInstanceStatusResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instanceStatusSet/>
  <requestId>46f4b86f-2b9e-4e42-9bf9-b278a0d1d68c</requestId>
</DescribeInstanceStatusResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstanceTopology

Describes a tree-based hierarchy that represents the physical host placement of your EC2 instances within an Availability Zone or Local Zone.

```http
POST / HTTP/1.1

Action=DescribeInstanceTopology
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeInstanceTopology is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000011d</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeInstanceTypeOfferings

Lists the instance types that are offered for the specified location.

```http
POST / HTTP/1.1

Action=DescribeInstanceTypeOfferings
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeInstanceTypeOfferingsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instanceTypeOfferingSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <instanceType>a1.2xlarge</instanceType>
      <locationType>region</locationType>
      <location>us-east-1</location>
    </item>
    <item>
      <instanceType>a1.4xlarge</instanceType>
      <locationType>region</locationType>
… (4996 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstanceTypes

Describes the specified instance types.

```http
POST / HTTP/1.1

Action=DescribeInstanceTypes
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeInstanceTypesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instanceTypeSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <instanceType>a1.2xlarge</instanceType>
      <currentGeneration>false</currentGeneration>
      <freeTierEligible>false</freeTierEligible>
      <supportedUsageClasses>
        <item>on-demand</item>
        <item>spot</item>
      </supportedUsageClasses>
… (228124 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeInstances

Describes the specified instances or all instances.

```http
POST / HTTP/1.1

Action=DescribeInstances
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <reservationSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <reservationId>r-00000000000000001</reservationId>
      <ownerId>000000000000</ownerId>
      <groupSet/>
      <instancesSet>
        <item>
          <architecture>x86_64</architecture>
          <blockDeviceMapping>
… (212 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#6-describeinstances) for a successful call.

### DescribeInternetGateways

Describes your internet gateways.

```http
POST / HTTP/1.1

Action=DescribeInternetGateways
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeInternetGatewaysResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <internetGatewaySet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <attachmentSet/>
      <internetGatewayId>igw-000000000000000b1</internetGatewayId>
      <ownerId>000000000000</ownerId>
      <tagSet/>
    </item>
  </internetGatewaySet>
  <requestId>e5cde466-84df-4f88-bbd0-58af2c2c8697</requestId>
… (1 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIpamByoasn

Describes your Autonomous System Numbers (ASNs), their provisioning statuses, and the BYOIP CIDRs with which they are associated.

```http
POST / HTTP/1.1

Action=DescribeIpamByoasn
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIpamByoasnResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <byoasnSet/>
  <requestId>e53fbb8c-8c7a-479b-b3af-1fe68f99f992</requestId>
</DescribeIpamByoasnResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIpamExternalResourceVerificationTokens

Describe verification tokens.

```http
POST / HTTP/1.1

Action=DescribeIpamExternalResourceVerificationTokens
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeIpamExternalResourceVerificationTokens is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000011e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeIpamInternetRegistryAssociations

Describes one or more IPAM internet registry associations.

```http
POST / HTTP/1.1

Action=DescribeIpamInternetRegistryAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIpamInternetRegistryAssociationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipamInternetRegistryAssociationSet/>
  <requestId>7f80183e-6b79-4630-83d6-19a17773b941</requestId>
</DescribeIpamInternetRegistryAssociationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIpamPolicies

Describes one or more IPAM policies.

```http
POST / HTTP/1.1

Action=DescribeIpamPolicies
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeIpamPolicies is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000011f</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeIpamPoolAllocations

Describes IPAM pool allocations.

```http
POST / HTTP/1.1

Action=DescribeIpamPoolAllocations
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeIpamPoolAllocations is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000120</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeIpamPools

Get information about your IPAM pools.

```http
POST / HTTP/1.1

Action=DescribeIpamPools
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIpamPoolsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipamPoolSet/>
  <requestId>ab93eaa8-3b8a-4aa5-b6a9-392d3fc63134</requestId>
</DescribeIpamPoolsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIpamPrefixListResolverTargets

Describes one or more IPAM prefix list resolver Targets.

```http
POST / HTTP/1.1

Action=DescribeIpamPrefixListResolverTargets
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIpamPrefixListResolverTargetsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipamPrefixListResolverTargetSet/>
  <requestId>33b1cddd-87c6-4f75-8d46-a197e8c42b6f</requestId>
</DescribeIpamPrefixListResolverTargetsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIpamPrefixListResolvers

Describes one or more IPAM prefix list resolvers.

```http
POST / HTTP/1.1

Action=DescribeIpamPrefixListResolvers
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIpamPrefixListResolversResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipamPrefixListResolverSet/>
  <requestId>82f606d5-5f5c-4608-9e8e-262e30a6699f</requestId>
</DescribeIpamPrefixListResolversResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIpamResourceDiscoveries

Describes IPAM resource discoveries.

```http
POST / HTTP/1.1

Action=DescribeIpamResourceDiscoveries
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIpamResourceDiscoveriesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipamResourceDiscoverySet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <ownerId>000000000000</ownerId>
      <ipamResourceDiscoveryId>ipam-res-disco-000000000000000b5</ipamResourceDiscoveryId>
      <ipamResourceDiscoveryArn>arn:aws:ec2::000000000000:ipam-resource-discovery/ipam-res-disco-000000000000000b5</ipamResourceDiscoveryArn>
      <ipamResourceDiscoveryRegion>us-east-1</ipamResourceDiscoveryRegion>
      <operatingRegionSet>
        <item>
          <regionName>us-east-1</regionName>
… (10 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIpamResourceDiscoveryAssociations

Describes resource discovery association with an Amazon VPC IPAM.

```http
POST / HTTP/1.1

Action=DescribeIpamResourceDiscoveryAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIpamResourceDiscoveryAssociationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipamResourceDiscoveryAssociationSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <ownerId>000000000000</ownerId>
      <ipamResourceDiscoveryAssociationId>ipam-res-disco-assoc-000000000000000b6</ipamResourceDiscoveryAssociationId>
      <ipamResourceDiscoveryAssociationArn>arn:aws:ec2::000000000000:ipam-resource-discovery-association/ipam-res-disco-assoc-000000000000000b6</ipamResourceDiscoveryAssociationArn>
      <ipamResourceDiscoveryId>ipam-res-disco-000000000000000b5</ipamResourceDiscoveryId>
      <ipamId>ipam-000000000000000b2</ipamId>
      <ipamArn>arn:aws:ec2::000000000000:ipam/ipam-000000000000000b2</ipamArn>
      <ipamRegion>us-east-1</ipamRegion>
… (8 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIpamScopes

Get information about your IPAM scopes.

```http
POST / HTTP/1.1

Action=DescribeIpamScopes
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIpamScopesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipamScopeSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <ownerId>000000000000</ownerId>
      <ipamScopeId>ipam-scope-000000000000000b3</ipamScopeId>
      <ipamScopeArn>arn:aws:ec2::000000000000:ipam-scope/ipam-scope-000000000000000b3</ipamScopeArn>
      <ipamArn>arn:aws:ec2::000000000000:ipam/ipam-000000000000000b2</ipamArn>
      <ipamRegion>us-east-1</ipamRegion>
      <ipamScopeType>public</ipamScopeType>
      <isDefault>true</isDefault>
… (19 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIpams

Get information about your IPAM pools.

```http
POST / HTTP/1.1

Action=DescribeIpams
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIpamsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipamSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <ownerId>000000000000</ownerId>
      <ipamId>ipam-000000000000000b2</ipamId>
      <ipamArn>arn:aws:ec2::000000000000:ipam/ipam-000000000000000b2</ipamArn>
      <ipamRegion>us-east-1</ipamRegion>
      <publicDefaultScopeId>ipam-scope-000000000000000b3</publicDefaultScopeId>
      <privateDefaultScopeId>ipam-scope-000000000000000b4</privateDefaultScopeId>
      <scopeCount>2</scopeCount>
… (17 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIpv6Pools

Describes your IPv6 address pools.

```http
POST / HTTP/1.1

Action=DescribeIpv6Pools
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeIpv6PoolsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipv6PoolSet/>
  <requestId>733de873-1bdf-4c8c-b20a-64c669e4b79e</requestId>
</DescribeIpv6PoolsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeKeyPairs

Describes the specified key pairs or all of your key pairs.

```http
POST / HTTP/1.1

Action=DescribeKeyPairs
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeKeyPairsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <keySet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <keyPairId>key-000000000000000b7</keyPairId>
      <keyType>rsa</keyType>
      <tagSet/>
      <createTime>2026-11-16T12:11:31.323986Z</createTime>
      <keyName>example</keyName>
      <keyFingerprint>41:33:3a:af:ab:b9:0e:20:c8:e4:c5:b0:ae:88:bd:b0:c7:18:1c:bd</keyFingerprint>
    </item>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLaunchTemplateVersions

Describes one or more versions of a specified launch template.

```http
POST / HTTP/1.1

Action=DescribeLaunchTemplateVersions
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either the parameter LaunchTemplateId or the parameter LaunchTemplateName, or name the version $Latest or $Default</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000121</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### DescribeLaunchTemplates

Describes one or more launch templates.

```http
POST / HTTP/1.1

Action=DescribeLaunchTemplates
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeLaunchTemplatesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <launchTemplates/>
  <requestId>985c8f59-7064-45b7-b5fa-9156a763a2dd</requestId>
</DescribeLaunchTemplatesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociations

Describes the associations between virtual interface groups and local gateway route tables.

```http
POST / HTTP/1.1

Action=DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <localGatewayRouteTableVirtualInterfaceGroupAssociationSet/>
  <requestId>882f3bbf-a1c0-4bdb-9997-e4c983a921a2</requestId>
</DescribeLocalGatewayRouteTableVirtualInterfaceGroupAssociationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLocalGatewayRouteTableVpcAssociations

Describes the specified associations between VPCs and local gateway route tables.

```http
POST / HTTP/1.1

Action=DescribeLocalGatewayRouteTableVpcAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeLocalGatewayRouteTableVpcAssociationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <localGatewayRouteTableVpcAssociationSet/>
  <requestId>938c1c61-7619-4eca-889b-3126cec9467f</requestId>
</DescribeLocalGatewayRouteTableVpcAssociationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLocalGatewayRouteTables

Describes one or more local gateway route tables.

```http
POST / HTTP/1.1

Action=DescribeLocalGatewayRouteTables
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeLocalGatewayRouteTablesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <localGatewayRouteTableSet/>
  <requestId>9983dc47-0e06-49f0-8b9b-b4e7edd8ef3c</requestId>
</DescribeLocalGatewayRouteTablesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLocalGatewayVirtualInterfaceGroups

Describes the specified local gateway virtual interface groups.

```http
POST / HTTP/1.1

Action=DescribeLocalGatewayVirtualInterfaceGroups
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeLocalGatewayVirtualInterfaceGroupsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <localGatewayVirtualInterfaceGroupSet/>
  <requestId>526bbcda-a99d-4b7c-b8ac-fcafd719c282</requestId>
</DescribeLocalGatewayVirtualInterfaceGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLocalGatewayVirtualInterfaces

Describes the specified local gateway virtual interfaces.

```http
POST / HTTP/1.1

Action=DescribeLocalGatewayVirtualInterfaces
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeLocalGatewayVirtualInterfacesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <localGatewayVirtualInterfaceSet/>
  <requestId>b19b8f68-dabf-44da-b284-101f3e029230</requestId>
</DescribeLocalGatewayVirtualInterfacesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLocalGateways

Describes one or more local gateways.

```http
POST / HTTP/1.1

Action=DescribeLocalGateways
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeLocalGatewaysResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <localGatewaySet/>
  <requestId>43fda2a8-bb3b-4e45-b74b-85b2fbbbb83f</requestId>
</DescribeLocalGatewaysResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeLockedSnapshots

Describes the lock status for a snapshot.

```http
POST / HTTP/1.1

Action=DescribeLockedSnapshots
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeLockedSnapshots is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000122</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeMacHosts

Describes the specified EC2 Mac Dedicated Host or all of your EC2 Mac Dedicated Hosts.

```http
POST / HTTP/1.1

Action=DescribeMacHosts
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeMacHostsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <macHostSet/>
  <requestId>55d68fd8-7a74-4c98-941c-a06ef90e84e8</requestId>
</DescribeMacHostsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeMacModificationTasks

Describes a System Integrity Protection (SIP) modification task or volume ownership delegation task for an Amazon EC2 Mac instance.

```http
POST / HTTP/1.1

Action=DescribeMacModificationTasks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeMacModificationTasksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <macModificationTaskSet/>
  <requestId>c42d4ad1-1d3d-4c26-8082-81a29d594804</requestId>
</DescribeMacModificationTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeManagedPrefixLists

Describes your managed prefix lists and any Amazon Web Services-managed prefix lists.

```http
POST / HTTP/1.1

Action=DescribeManagedPrefixLists
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeManagedPrefixListsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <prefixListSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <prefixListId>pl-0b11f84d</prefixListId>
      <addressFamily>IPv6</addressFamily>
      <state>create-complete</state>
      <prefixListArn>arn:aws:ec2:us-east-1:aws:prefix-list/pl-0b11f84d</prefixListArn>
      <prefixListName>com.amazonaws.us-east-1.ipv6.cloudfront.origin-facing</prefixListName>
      <tagSet/>
      <ownerId>AWS</ownerId>
… (130 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeMovingAddresses

This action is deprecated.

```http
POST / HTTP/1.1

Action=DescribeMovingAddresses
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeMovingAddresses is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000123</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeNatGateways

Describes your NAT gateways.

```http
POST / HTTP/1.1

Action=DescribeNatGateways
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeNatGatewaysResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <natGatewaySet/>
  <requestId>824e065f-98f6-499a-864d-9ec6da5e9c6b</requestId>
</DescribeNatGatewaysResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeNetworkAcls

Describes your network ACLs.

```http
POST / HTTP/1.1

Action=DescribeNetworkAcls
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeNetworkAclsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <networkAclSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <associationSet>
        <item>
          <networkAclAssociationId>aclassoc-00000000000000008</networkAclAssociationId>
          <networkAclId>acl-00000000000000004</networkAclId>
          <subnetId>subnet-00000000000000007</subnetId>
        </item>
        <item>
… (57 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeNetworkInsightsAccessScopeAnalyses

Describes the specified Network Access Scope analyses.

```http
POST / HTTP/1.1

Action=DescribeNetworkInsightsAccessScopeAnalyses
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeNetworkInsightsAccessScopeAnalysesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <networkInsightsAccessScopeAnalysisSet/>
  <requestId>f292683c-a4aa-4bdd-a9f6-3b9ebe50748c</requestId>
</DescribeNetworkInsightsAccessScopeAnalysesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeNetworkInsightsAccessScopes

Describes the specified Network Access Scopes.

```http
POST / HTTP/1.1

Action=DescribeNetworkInsightsAccessScopes
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeNetworkInsightsAccessScopesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <networkInsightsAccessScopeSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <networkInsightsAccessScopeId>nis-000000000000000b8</networkInsightsAccessScopeId>
      <networkInsightsAccessScopeArn>arn:aws:ec2:us-east-1:000000000000:network-insights-access-scope/nis-000000000000000b8</networkInsightsAccessScopeArn>
      <createdDate>2026-11-16T12:11:31.379122Z</createdDate>
      <updatedDate>2026-11-16T12:11:31.379122Z</updatedDate>
      <tagSet/>
    </item>
  </networkInsightsAccessScopeSet>
… (2 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeNetworkInsightsAnalyses

Describes one or more of your network insights analyses.

```http
POST / HTTP/1.1

Action=DescribeNetworkInsightsAnalyses
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeNetworkInsightsAnalysesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <networkInsightsAnalysisSet/>
  <requestId>5fd2b7a8-6566-44a3-859e-ef3d996669e6</requestId>
</DescribeNetworkInsightsAnalysesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeNetworkInsightsPaths

Describes one or more of your paths.

```http
POST / HTTP/1.1

Action=DescribeNetworkInsightsPaths
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeNetworkInsightsPathsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <networkInsightsPathSet/>
  <requestId>fc9eb877-80d7-4ca3-9d2d-c5ff6fcb141c</requestId>
</DescribeNetworkInsightsPathsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeNetworkInterfaceAttribute

Describes a network interface attribute.

```http
POST / HTTP/1.1

Action=DescribeNetworkInterfaceAttribute
&Version=2016-11-15
&NetworkInterfaceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter attribute</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000124</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### DescribeNetworkInterfacePermissions

Describes the permissions for your network interfaces.

```http
POST / HTTP/1.1

Action=DescribeNetworkInterfacePermissions
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeNetworkInterfacePermissionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <networkInterfacePermissions/>
  <requestId>179f9695-f027-4a91-8d13-f76049d60fa0</requestId>
</DescribeNetworkInterfacePermissionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeNetworkInterfaces

Describes the specified network interfaces or all your network interfaces.

```http
POST / HTTP/1.1

Action=DescribeNetworkInterfaces
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeNetworkInterfacesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <networkInterfaceSet/>
  <requestId>5db7ef09-2c06-4883-8d5d-5c38c9142853</requestId>
</DescribeNetworkInterfacesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeOutpostLags

Describes the Outposts link aggregation groups (LAGs).

```http
POST / HTTP/1.1

Action=DescribeOutpostLags
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeOutpostLagsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <outpostLagSet/>
  <requestId>559c13fd-f279-441a-87a7-449c33d9963a</requestId>
</DescribeOutpostLagsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribePlacementGroups

Describes the specified placement groups or all of your placement groups.

```http
POST / HTTP/1.1

Action=DescribePlacementGroups
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribePlacementGroupsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <placementGroupSet/>
  <requestId>190ff5cb-ca10-4c9a-a28c-1fe351c2dbac</requestId>
</DescribePlacementGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribePrefixLists

Describes available Amazon Web Services services in a prefix list format, which includes the prefix list name and prefix list ID of the service and the IP address range for the service.

```http
POST / HTTP/1.1

Action=DescribePrefixLists
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribePrefixListsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <prefixListSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <cidrSet>
        <item>198.18.8.0/21</item>
      </cidrSet>
      <prefixListId>pl-55a00f4c</prefixListId>
      <prefixListName>com.amazonaws.us-east-1.dynamodb</prefixListName>
    </item>
    <item>
… (37 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribePrincipalIdFormat

Describes the ID format settings for the root user and all IAM roles and IAM users that have explicitly specified a longer ID (17-character ID) preference.

```http
POST / HTTP/1.1

Action=DescribePrincipalIdFormat
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribePrincipalIdFormatResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <principalSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <arn>arn:aws:iam::000000000000:root</arn>
      <statusSet>
        <item>
          <resource>bundle</resource>
          <useLongIds>true</useLongIds>
        </item>
        <item>
… (124 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribePublicIpv4Pools

Describes the specified IPv4 address pools.

```http
POST / HTTP/1.1

Action=DescribePublicIpv4Pools
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribePublicIpv4PoolsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <publicIpv4PoolSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <poolId>ipv4pool-ec2-000000000000000b9</poolId>
      <description/>
      <poolAddressRangeSet/>
      <totalAddressCount>0</totalAddressCount>
      <totalAvailableAddressCount>0</totalAvailableAddressCount>
      <networkBorderGroup>us-east-1</networkBorderGroup>
      <tagSet/>
… (4 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeRegions

Describes the Regions that are enabled for your account, or all Regions.

```http
POST / HTTP/1.1

Action=DescribeRegions
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeRegionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <regionInfo>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <optInStatus>opt-in-not-required</optInStatus>
      <geographySet>
        <item>
          <name>Japan</name>
        </item>
      </geographySet>
      <regionName>ap-northeast-1</regionName>
… (165 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReplaceRootVolumeTasks

Describes a root volume replacement task.

```http
POST / HTTP/1.1

Action=DescribeReplaceRootVolumeTasks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeReplaceRootVolumeTasksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <replaceRootVolumeTaskSet/>
  <requestId>9a07c898-059a-4b5c-9962-fa67da0c26f1</requestId>
</DescribeReplaceRootVolumeTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReservedInstances

Describes one or more of the Reserved Instances that you purchased.

```http
POST / HTTP/1.1

Action=DescribeReservedInstances
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeReservedInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <reservedInstancesSet/>
  <requestId>8c524065-763e-4dff-9223-e6a7f74222ae</requestId>
</DescribeReservedInstancesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReservedInstancesListings

Describes your account's Reserved Instance listings in the Reserved Instance Marketplace.

```http
POST / HTTP/1.1

Action=DescribeReservedInstancesListings
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeReservedInstancesListingsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <reservedInstancesListingsSet/>
  <requestId>4c718772-f4c8-44e3-8319-f4a15422a935</requestId>
</DescribeReservedInstancesListingsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReservedInstancesModifications

Describes the modifications made to your Reserved Instances.

```http
POST / HTTP/1.1

Action=DescribeReservedInstancesModifications
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeReservedInstancesModifications is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000125</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeReservedInstancesOfferings

Describes Reserved Instance offerings that are available for purchase.

```http
POST / HTTP/1.1

Action=DescribeReservedInstancesOfferings
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeReservedInstancesOfferingsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <nextToken>6563322d313a316364343336636162376161663636663a3365393437323766643537353166376623313030</nextToken>
  <reservedInstancesOfferingsSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <currencyCode>USD</currencyCode>
      <instanceTenancy>default</instanceTenancy>
      <marketplace>false</marketplace>
      <offeringClass>convertible</offeringClass>
      <offeringType>All Upfront</offeringType>
      <pricingDetailsSet/>
… (1596 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeRouteServerEndpoints

Describes one or more route server endpoints.

```http
POST / HTTP/1.1

Action=DescribeRouteServerEndpoints
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeRouteServerEndpointsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <routeServerEndpointSet/>
  <requestId>24cf18f8-a242-4939-aa4f-d5a114dd3c15</requestId>
</DescribeRouteServerEndpointsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeRouteServerPeers

Describes one or more route server peers.

```http
POST / HTTP/1.1

Action=DescribeRouteServerPeers
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeRouteServerPeersResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <routeServerPeerSet/>
  <requestId>1ff52f2a-f164-45db-a97c-b3cabeec1b02</requestId>
</DescribeRouteServerPeersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeRouteServers

Describes one or more route servers.

```http
POST / HTTP/1.1

Action=DescribeRouteServers
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeRouteServersResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <routeServerSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <routeServerId>rs-000000000000000ba</routeServerId>
      <amazonSideAsn>1</amazonSideAsn>
      <state>available</state>
      <tagSet/>
      <persistRoutesState>disabled</persistRoutesState>
      <snsNotificationsEnabled>false</snsNotificationsEnabled>
    </item>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeRouteTables

Describes your route tables.

```http
POST / HTTP/1.1

Action=DescribeRouteTables
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeRouteTablesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <routeTableSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <associationSet>
        <item>
          <main>true</main>
          <routeTableAssociationId>rtbassoc-00000000000000006</routeTableAssociationId>
          <routeTableId>rtb-00000000000000005</routeTableId>
          <associationState>
            <state>associated</state>
… (20 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeScheduledInstanceAvailability

Finds available schedules that meet the specified criteria.

```http
POST / HTTP/1.1

Action=DescribeScheduledInstanceAvailability
&Version=2016-11-15
&FirstSlotStartTimeRange.EarliestTime=2026-11-16T12%3A11%3A33Z
&FirstSlotStartTimeRange.LatestTime=2026-11-16T12%3A11%3A33Z
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'recurrence' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000126</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### DescribeScheduledInstances

Describes the specified Scheduled Instances or all your Scheduled Instances.

```http
POST / HTTP/1.1

Action=DescribeScheduledInstances
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeScheduledInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <scheduledInstanceSet/>
  <requestId>2411dcf1-f9a3-47bf-a57b-ab7ba1ad85d2</requestId>
</DescribeScheduledInstancesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSecondaryInterfaces

Describes one or more of your secondary interfaces.

```http
POST / HTTP/1.1

Action=DescribeSecondaryInterfaces
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSecondaryInterfacesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <secondaryInterfaceSet/>
  <requestId>ea8712e5-9f57-47b0-b85d-2a992bc67fb9</requestId>
</DescribeSecondaryInterfacesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSecondaryNetworks

Describes one or more secondary networks.

```http
POST / HTTP/1.1

Action=DescribeSecondaryNetworks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSecondaryNetworksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <secondaryNetworkSet/>
  <requestId>d63a420c-d0ab-4c59-9858-420a35c4a5b6</requestId>
</DescribeSecondaryNetworksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSecondarySubnets

Describes one or more of your secondary subnets.

```http
POST / HTTP/1.1

Action=DescribeSecondarySubnets
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSecondarySubnetsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <secondarySubnetSet/>
  <requestId>b7553a96-78a5-4c87-b389-3eaa366cfec1</requestId>
</DescribeSecondarySubnetsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSecurityGroupReferences

Describes the VPCs on the other side of a VPC peering or Transit Gateway connection that are referencing the security groups you've specified in this request.

```http
POST / HTTP/1.1

Action=DescribeSecurityGroupReferences
&Version=2016-11-15
&GroupId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidGroupId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000127</RequestID>
</Response>
```

`400` `InvalidGroupId.Malformed`: the placeholder values failed the operation's input validation.

### DescribeSecurityGroupRules

Describes one or more of your security group rules.

```http
POST / HTTP/1.1

Action=DescribeSecurityGroupRules
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSecurityGroupRulesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <securityGroupRuleSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <securityGroupRuleId>sgr-0000000000000000e</securityGroupRuleId>
      <groupId>sg-0000000000000000d</groupId>
      <groupOwnerId>000000000000</groupOwnerId>
      <isEgress>true</isEgress>
      <ipProtocol>-1</ipProtocol>
      <cidrIpv4>0.0.0.0/0</cidrIpv4>
      <tagSet/>
… (28 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSecurityGroupVpcAssociations

Describes security group VPC associations made with AssociateSecurityGroupVpc .

```http
POST / HTTP/1.1

Action=DescribeSecurityGroupVpcAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSecurityGroupVpcAssociationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <securityGroupVpcAssociationSet/>
  <requestId>a1b1038a-6ddc-44c3-b039-58f393cdba7f</requestId>
</DescribeSecurityGroupVpcAssociationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSecurityGroups

Describes the specified security groups or all of your security groups.

```http
POST / HTTP/1.1

Action=DescribeSecurityGroups
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSecurityGroupsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <securityGroupInfo>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <groupId>sg-0000000000000000d</groupId>
      <ipPermissionsEgress>
        <item>
          <ipProtocol>-1</ipProtocol>
          <groups/>
          <ipRanges>
            <item>
… (54 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeServiceLinkVirtualInterfaces

Describes the Outpost service link virtual interfaces.

```http
POST / HTTP/1.1

Action=DescribeServiceLinkVirtualInterfaces
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeServiceLinkVirtualInterfacesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <serviceLinkVirtualInterfaceSet/>
  <requestId>7a08c60d-8816-46ec-9156-4e51d60c8865</requestId>
</DescribeServiceLinkVirtualInterfacesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSnapshotAttribute

Describes the specified attribute of the specified snapshot.

```http
POST / HTTP/1.1

Action=DescribeSnapshotAttribute
&Version=2016-11-15
&Attribute=productCodes
&SnapshotId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeSnapshotAttribute is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000128</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeSnapshotTierStatus

Describes the storage tier status of one or more Amazon EBS snapshots.

```http
POST / HTTP/1.1

Action=DescribeSnapshotTierStatus
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeSnapshotTierStatus is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000129</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeSnapshots

Describes the specified EBS snapshots available to you or all of the EBS snapshots available to you.

```http
POST / HTTP/1.1

Action=DescribeSnapshots
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSnapshotsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <snapshotSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <ownerAlias>amazon</ownerAlias>
      <tagSet/>
      <storageTier>standard</storageTier>
      <transferType>standard</transferType>
      <completionTime>2023-07-19T18:12:21Z</completionTime>
      <fullSnapshotSizeInBytes>8589934592</fullSnapshotSizeInBytes>
      <snapshotId>snap-00000000000000011</snapshotId>
… (1015 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSpotDatafeedSubscription

Describes the data feed for Spot Instances.

```http
POST / HTTP/1.1

Action=DescribeSpotDatafeedSubscription
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSpotDatafeed.NotFound</Code>
      <Message>Spot datafeed subscription does not exist.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000012a</RequestID>
</Response>
```

`400` `InvalidSpotDatafeed.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeSpotFleetInstances

Describes the running instances for the specified Spot Fleet.

```http
POST / HTTP/1.1

Action=DescribeSpotFleetInstances
&Version=2016-11-15
&SpotFleetRequestId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSpotFleetRequestId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000012b</RequestID>
</Response>
```

`400` `InvalidSpotFleetRequestId.Malformed`: the placeholder values failed the operation's input validation.

### DescribeSpotFleetRequestHistory

Describes the events for the specified Spot Fleet request during the specified time.

```http
POST / HTTP/1.1

Action=DescribeSpotFleetRequestHistory
&Version=2016-11-15
&SpotFleetRequestId=example
&StartTime=2026-11-16T12%3A11%3A33Z
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeSpotFleetRequestHistory is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000012c</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeSpotFleetRequests

Describes your Spot Fleet requests.

```http
POST / HTTP/1.1

Action=DescribeSpotFleetRequests
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSpotFleetRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <spotFleetRequestConfigSet/>
  <requestId>2a0ba402-703a-41cc-b41c-3b6df7442831</requestId>
</DescribeSpotFleetRequestsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSpotInstanceRequests

Describes the specified Spot Instance requests.

```http
POST / HTTP/1.1

Action=DescribeSpotInstanceRequests
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSpotInstanceRequestsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <spotInstanceRequestSet/>
  <requestId>953b8399-6a69-42f5-90fa-6f9b101a9ba1</requestId>
</DescribeSpotInstanceRequestsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSpotPriceHistory

Describes the Spot price history.

```http
POST / HTTP/1.1

Action=DescribeSpotPriceHistory
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSpotPriceHistoryResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <nextToken>6563322d313a376436666135613136303565356535303a6563322d7370682d317c623835626564323631343333396233647c32303737337c32303737337c31303030</nextToken>
  <spotPriceHistorySet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <availabilityZone>us-east-1a</availabilityZone>
      <availabilityZoneId>use1-az1</availabilityZoneId>
      <instanceType>a1.2xlarge</instanceType>
      <productDescription>Linux/UNIX</productDescription>
      <spotPrice>0.136400</spotPrice>
      <timestamp>2026-11-16T00:00:00Z</timestamp>
… (7996 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeStaleSecurityGroups

Describes the stale security group rules for security groups referenced across a VPC peering connection, transit gateway connection, or with a security group VPC association.

```http
POST / HTTP/1.1

Action=DescribeStaleSecurityGroups
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000012d</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeStoreImageTasks

Describes the progress of the AMI store tasks.

```http
POST / HTTP/1.1

Action=DescribeStoreImageTasks
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeStoreImageTasksResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <storeImageTaskResultSet/>
  <requestId>2e775aa5-a193-4237-b751-b8b9c63485e1</requestId>
</DescribeStoreImageTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSubnets

Describes your subnets.

```http
POST / HTTP/1.1

Action=DescribeSubnets
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeSubnetsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <subnetSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <availabilityZoneId>use1-az1</availabilityZoneId>
      <mapCustomerOwnedIpOnLaunch>false</mapCustomerOwnedIpOnLaunch>
      <ownerId>000000000000</ownerId>
      <assignIpv6AddressOnCreation>false</assignIpv6AddressOnCreation>
      <ipv6CidrBlockAssociationSet/>
      <tagSet/>
      <subnetArn>arn:aws:ec2:us-east-1:000000000000:subnet/subnet-00000000000000007</subnetArn>
… (76 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTags

Describes the specified tags for your EC2 resources.

```http
POST / HTTP/1.1

Action=DescribeTags
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTagsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <tagSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <key>aws:autoscaling:groupName</key>
      <resourceId>i-00000000000000002</resourceId>
      <resourceType>instance</resourceType>
      <value>docs-demo-asg</value>
    </item>
  </tagSet>
  <requestId>eee67dc7-5f8f-4b4c-aff5-693644f816a5</requestId>
… (1 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTrafficMirrorFilterRules

Describe traffic mirror filters that determine the traffic that is mirrored.

```http
POST / HTTP/1.1

Action=DescribeTrafficMirrorFilterRules
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeTrafficMirrorFilterRules is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000012e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeTrafficMirrorFilters

Describes one or more Traffic Mirror filters.

```http
POST / HTTP/1.1

Action=DescribeTrafficMirrorFilters
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTrafficMirrorFiltersResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <trafficMirrorFilterSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <trafficMirrorFilterId>tmf-000000000000000bd</trafficMirrorFilterId>
      <ingressFilterRuleSet/>
      <egressFilterRuleSet/>
      <networkServiceSet/>
      <tagSet/>
    </item>
  </trafficMirrorFilterSet>
… (2 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTrafficMirrorSessions

Describes one or more Traffic Mirror sessions.

```http
POST / HTTP/1.1

Action=DescribeTrafficMirrorSessions
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTrafficMirrorSessionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <trafficMirrorSessionSet/>
  <requestId>70a3d762-828f-450c-b695-11a53970e36e</requestId>
</DescribeTrafficMirrorSessionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTrafficMirrorTargets

Information about one or more Traffic Mirror targets.

```http
POST / HTTP/1.1

Action=DescribeTrafficMirrorTargets
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTrafficMirrorTargetsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <trafficMirrorTargetSet/>
  <requestId>5a939417-4f82-4793-ae93-a8053581f5dd</requestId>
</DescribeTrafficMirrorTargetsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGatewayAttachments

Describes one or more attachments between resources and transit gateways.

```http
POST / HTTP/1.1

Action=DescribeTransitGatewayAttachments
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewayAttachmentsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewayAttachments/>
  <requestId>f7fdb878-b34f-4801-afe1-1f44f643d24c</requestId>
</DescribeTransitGatewayAttachmentsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGatewayConnectPeers

Describes one or more Connect peers.

```http
POST / HTTP/1.1

Action=DescribeTransitGatewayConnectPeers
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewayConnectPeersResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewayConnectPeerSet/>
  <requestId>7a758c96-219c-40f0-99dc-e74a4083b98d</requestId>
</DescribeTransitGatewayConnectPeersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGatewayConnects

Describes one or more Connect attachments.

```http
POST / HTTP/1.1

Action=DescribeTransitGatewayConnects
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewayConnectsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewayConnectSet/>
  <requestId>c35ef0bf-73e9-472a-a951-185806b4837b</requestId>
</DescribeTransitGatewayConnectsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGatewayMeteringPolicies

Describes one or more transit gateway metering policies.

```http
POST / HTTP/1.1

Action=DescribeTransitGatewayMeteringPolicies
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewayMeteringPoliciesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewayMeteringPolicies/>
  <requestId>57d7e638-799f-4ac3-bb3a-72eb048725f8</requestId>
</DescribeTransitGatewayMeteringPoliciesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGatewayMulticastDomains

Describes one or more transit gateway multicast domains.

```http
POST / HTTP/1.1

Action=DescribeTransitGatewayMulticastDomains
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewayMulticastDomainsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewayMulticastDomains/>
  <requestId>0a02634f-a125-4104-9551-18328750850c</requestId>
</DescribeTransitGatewayMulticastDomainsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGatewayPeeringAttachments

Describes your transit gateway peering attachments.

```http
POST / HTTP/1.1

Action=DescribeTransitGatewayPeeringAttachments
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewayPeeringAttachmentsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewayPeeringAttachments/>
  <requestId>1784219d-1a03-49df-abf8-0ec52c8e40b5</requestId>
</DescribeTransitGatewayPeeringAttachmentsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGatewayPolicyTables

Describes one or more transit gateway route policy tables.

```http
POST / HTTP/1.1

Action=DescribeTransitGatewayPolicyTables
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewayPolicyTablesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewayPolicyTables/>
  <requestId>dc48f116-934e-4f1d-9d1a-f639894397e8</requestId>
</DescribeTransitGatewayPolicyTablesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGatewayRouteTableAnnouncements

Describes one or more transit gateway route table advertisements.

```http
POST / HTTP/1.1

Action=DescribeTransitGatewayRouteTableAnnouncements
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewayRouteTableAnnouncementsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewayRouteTableAnnouncements/>
  <requestId>54ac725f-8b23-44a5-a377-68ec65538a5c</requestId>
</DescribeTransitGatewayRouteTableAnnouncementsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGatewayRouteTables

Describes one or more transit gateway route tables.

```http
POST / HTTP/1.1

Action=DescribeTransitGatewayRouteTables
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewayRouteTablesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewayRouteTables>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <transitGatewayRouteTableId>tgw-rtb-000000000000000bf</transitGatewayRouteTableId>
      <transitGatewayId>tgw-000000000000000be</transitGatewayId>
      <state>available</state>
      <defaultAssociationRouteTable>true</defaultAssociationRouteTable>
      <defaultPropagationRouteTable>true</defaultPropagationRouteTable>
      <creationTime>2026-11-16T12:11:31.481156Z</creationTime>
      <tagSet/>
… (4 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGatewayVpcAttachments

Describes one or more VPC attachments.

```http
POST / HTTP/1.1

Action=DescribeTransitGatewayVpcAttachments
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewayVpcAttachmentsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewayVpcAttachments/>
  <requestId>227508c7-dad3-4b47-b110-f164da08700d</requestId>
</DescribeTransitGatewayVpcAttachmentsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTransitGateways

Describes one or more transit gateways.

```http
POST / HTTP/1.1

Action=DescribeTransitGateways
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeTransitGatewaysResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <transitGatewaySet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <transitGatewayId>tgw-000000000000000be</transitGatewayId>
      <transitGatewayArn>arn:aws:ec2:us-east-1:000000000000:transit-gateway/tgw-000000000000000be</transitGatewayArn>
      <state>available</state>
      <ownerId>000000000000</ownerId>
      <creationTime>2026-11-16T12:11:31.481156Z</creationTime>
      <options>
        <amazonSideAsn>64512</amazonSideAsn>
… (15 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeTrunkInterfaceAssociations

Describes one or more network interface trunk associations.

```http
POST / HTTP/1.1

Action=DescribeTrunkInterfaceAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>OperationNotPermitted</Code>
      <Message>User 000000000000 is not permitted to perform this operation</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000012f</RequestID>
</Response>
```

`400` `OperationNotPermitted`.

### DescribeVerifiedAccessEndpoints

Describes the specified Amazon Web Services Verified Access endpoints.

```http
POST / HTTP/1.1

Action=DescribeVerifiedAccessEndpoints
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVerifiedAccessEndpointsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <verifiedAccessEndpointSet/>
  <requestId>2857da08-bc78-4317-a3d0-84d8d11eaed0</requestId>
</DescribeVerifiedAccessEndpointsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVerifiedAccessGroups

Describes the specified Verified Access groups.

```http
POST / HTTP/1.1

Action=DescribeVerifiedAccessGroups
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeVerifiedAccessGroups is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000130</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeVerifiedAccessInstanceLoggingConfigurations

Describes the specified Amazon Web Services Verified Access instances.

```http
POST / HTTP/1.1

Action=DescribeVerifiedAccessInstanceLoggingConfigurations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVerifiedAccessInstanceLoggingConfigurationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <loggingConfigurationSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <verifiedAccessInstanceId>vai-000000000000000c0</verifiedAccessInstanceId>
      <accessLogs>
        <s3>
          <enabled>false</enabled>
        </s3>
        <cloudWatchLogs>
          <enabled>false</enabled>
… (10 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVerifiedAccessInstances

Describes the specified Amazon Web Services Verified Access instances.

```http
POST / HTTP/1.1

Action=DescribeVerifiedAccessInstances
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVerifiedAccessInstancesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <verifiedAccessInstanceSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <verifiedAccessInstanceId>vai-000000000000000c0</verifiedAccessInstanceId>
      <verifiedAccessTrustProviderSet/>
      <creationTime>2026-11-16T12:11:31.558696Z</creationTime>
      <lastUpdatedTime>2026-11-16T12:11:31.558696Z</lastUpdatedTime>
      <tagSet/>
      <fipsEnabled>false</fipsEnabled>
      <verifiedAccessInstanceTransitGatewayAttachmentSet/>
… (4 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVerifiedAccessTrustProviders

Describes the specified Amazon Web Services Verified Access trust providers.

```http
POST / HTTP/1.1

Action=DescribeVerifiedAccessTrustProviders
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVerifiedAccessTrustProvidersResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <verifiedAccessTrustProviderSet/>
  <requestId>25ff3b9f-0280-4d84-afa8-4151843ea7bf</requestId>
</DescribeVerifiedAccessTrustProvidersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVolumeAttribute

Describes the specified attribute of the specified volume.

```http
POST / HTTP/1.1

Action=DescribeVolumeAttribute
&Version=2016-11-15
&Attribute=autoEnableIO
&VolumeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVolumeID.Malformed</Code>
      <Message>The volume ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000131</RequestID>
</Response>
```

`400` `InvalidVolumeID.Malformed`: the placeholder values failed the operation's input validation.

### DescribeVolumeStatus

Describes the status of the specified volumes.

```http
POST / HTTP/1.1

Action=DescribeVolumeStatus
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVolumeStatusResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <volumeStatusSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <actionsSet/>
      <availabilityZone>us-east-1a</availabilityZone>
      <eventsSet/>
      <volumeId>vol-000000000000000a1</volumeId>
      <volumeStatus>
        <details>
          <item>
… (47 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVolumes

Describes the specified EBS volumes or all of your EBS volumes.

```http
POST / HTTP/1.1

Action=DescribeVolumes
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVolumesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <volumeSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <availabilityZoneId>use1-az1</availabilityZoneId>
      <iops>3000</iops>
      <tagSet/>
      <volumeType>gp3</volumeType>
      <fastRestored>false</fastRestored>
      <multiAttachEnabled>false</multiAttachEnabled>
      <throughput>125</throughput>
… (65 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVolumesModifications

Describes the most recent volume modification request for the specified EBS volumes.

```http
POST / HTTP/1.1

Action=DescribeVolumesModifications
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVolumesModificationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <volumeModificationSet/>
  <requestId>f0aa3938-7a12-405e-a10a-70e5a15aab14</requestId>
</DescribeVolumesModificationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcAttribute

Describes the specified attribute of the specified VPC.

```http
POST / HTTP/1.1

Action=DescribeVpcAttribute
&Version=2016-11-15
&Attribute=enableDnsSupport
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000132</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeVpcBlockPublicAccessExclusions

Describe VPC Block Public Access (BPA) exclusions.

```http
POST / HTTP/1.1

Action=DescribeVpcBlockPublicAccessExclusions
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcBlockPublicAccessExclusionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcBlockPublicAccessExclusionSet/>
  <requestId>0d73f38c-6d40-439f-bbc3-496b8dda689b</requestId>
</DescribeVpcBlockPublicAccessExclusionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcBlockPublicAccessOptions

Describe VPC Block Public Access (BPA) options.

```http
POST / HTTP/1.1

Action=DescribeVpcBlockPublicAccessOptions
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcBlockPublicAccessOptionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcBlockPublicAccessOptions xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <awsAccountId>000000000000</awsAccountId>
    <awsRegion>us-east-1</awsRegion>
    <state>default-state</state>
    <internetGatewayBlockMode>off</internetGatewayBlockMode>
    <managedBy>account</managedBy>
    <exclusionsAllowed>allowed</exclusionsAllowed>
  </vpcBlockPublicAccessOptions>
  <requestId>3c5fdeba-f1e3-4951-ac8e-73d8fbff6b57</requestId>
… (1 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcClassicLink

This action is deprecated.

```http
POST / HTTP/1.1

Action=DescribeVpcClassicLink
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcClassicLinkResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <classicLinkEnabled>false</classicLinkEnabled>
      <tagSet/>
      <vpcId>vpc-00000000000000001</vpcId>
    </item>
  </vpcSet>
  <requestId>cbfc0687-7b9b-483c-bdbd-0da609f987df</requestId>
</DescribeVpcClassicLinkResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcClassicLinkDnsSupport

This action is deprecated.

```http
POST / HTTP/1.1

Action=DescribeVpcClassicLinkDnsSupport
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcClassicLinkDnsSupportResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcs>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <classicLinkDnsSupported>false</classicLinkDnsSupported>
      <vpcId>vpc-00000000000000001</vpcId>
    </item>
  </vpcs>
  <requestId>a8c1c918-68af-4458-bc4e-e9ef83a6bb41</requestId>
</DescribeVpcClassicLinkDnsSupportResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcEncryptionControls

Describes one or more VPC Encryption Control configurations.

```http
POST / HTTP/1.1

Action=DescribeVpcEncryptionControls
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcEncryptionControlsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcEncryptionControlSet/>
  <requestId>df8135cd-2919-450b-8a3e-a1a0c9623d7b</requestId>
</DescribeVpcEncryptionControlsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcEndpointAssociations

Describes the VPC resources, VPC endpoint services, Amazon Lattice services, or service networks associated with the VPC endpoint.

```http
POST / HTTP/1.1

Action=DescribeVpcEndpointAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcEndpointAssociationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcEndpointAssociationSet/>
  <requestId>b4c18ab3-7dc4-4df1-89ed-45a6d29f5163</requestId>
</DescribeVpcEndpointAssociationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcEndpointConnectionNotifications

Describes the connection notifications for VPC endpoints and VPC endpoint services.

```http
POST / HTTP/1.1

Action=DescribeVpcEndpointConnectionNotifications
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcEndpointConnectionNotificationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <connectionNotificationSet/>
  <requestId>13d9e7b6-3d14-4926-acbd-01c137a47566</requestId>
</DescribeVpcEndpointConnectionNotificationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcEndpointConnections

Describes the VPC endpoint connections to your VPC endpoint services, including any endpoints that are pending your acceptance.

```http
POST / HTTP/1.1

Action=DescribeVpcEndpointConnections
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcEndpointConnectionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcEndpointConnectionSet/>
  <requestId>83d07152-84ea-4363-a5bb-98086834f1f2</requestId>
</DescribeVpcEndpointConnectionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcEndpointServiceConfigurations

Describes the VPC endpoint service configurations in your account (your services).

```http
POST / HTTP/1.1

Action=DescribeVpcEndpointServiceConfigurations
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcEndpointServiceConfigurationsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <serviceConfigurationSet/>
  <requestId>8177d36c-7dde-4b97-8d1a-dfaad3c1b61f</requestId>
</DescribeVpcEndpointServiceConfigurationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcEndpointServicePermissions

Describes the principals (service consumers) that are permitted to discover your VPC endpoint service.

```http
POST / HTTP/1.1

Action=DescribeVpcEndpointServicePermissions
&Version=2016-11-15
&ServiceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeVpcEndpointServicePermissions is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000133</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeVpcEndpointServices

Describes available services to which you can create a VPC endpoint.

```http
POST / HTTP/1.1

Action=DescribeVpcEndpointServices
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcEndpointServicesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <serviceNameSet>
    <item>api.aws.us-east-1.cassandra-streams</item>
    <item>aws.api.us-east-1.emr-service-cell01</item>
    <item>com.amazonaws.us-east-1.access-analyzer</item>
    <item>com.amazonaws.us-east-1.account</item>
    <item>com.amazonaws.us-east-1.acm-pca</item>
    <item>com.amazonaws.us-east-1.airflow.api</item>
    <item>com.amazonaws.us-east-1.airflow.env</item>
    <item>com.amazonaws.us-east-1.airflow.ops</item>
… (6061 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcEndpoints

Describes your VPC endpoints.

```http
POST / HTTP/1.1

Action=DescribeVpcEndpoints
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcEndpointsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcEndpointSet/>
  <requestId>a9f7a7e2-176d-49bf-aa0d-da4e90561b0d</requestId>
</DescribeVpcEndpointsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcPeeringConnections

Describes your VPC peering connections.

```http
POST / HTTP/1.1

Action=DescribeVpcPeeringConnections
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcPeeringConnectionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcPeeringConnectionSet/>
  <requestId>4685c4de-d80e-4d00-873d-e9ba152969e8</requestId>
</DescribeVpcPeeringConnectionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpcs

Describes your VPCs.

```http
POST / HTTP/1.1

Action=DescribeVpcs
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpcsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <ownerId>000000000000</ownerId>
      <instanceTenancy>default</instanceTenancy>
      <cidrBlockAssociationSet>
        <item>
          <associationId>vpc-cidr-assoc-00000000000000002</associationId>
          <cidrBlock>172.31.0.0/16</cidrBlock>
          <cidrBlockState>
… (18 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpnConcentrators

Describes one or more of your VPN concentrators.

```http
POST / HTTP/1.1

Action=DescribeVpnConcentrators
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DescribeVpnConcentrators is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000134</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeVpnConnections

Describes one or more of your VPN connections.

```http
POST / HTTP/1.1

Action=DescribeVpnConnections
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpnConnectionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpnConnectionSet/>
  <requestId>545536bc-e2a4-4ff0-a463-5305d4c3f05f</requestId>
</DescribeVpnConnectionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeVpnGateways

Describes one or more of your virtual private gateways.

```http
POST / HTTP/1.1

Action=DescribeVpnGateways
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DescribeVpnGatewaysResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpnGatewaySet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <amazonSideAsn>64512</amazonSideAsn>
      <tagSet/>
      <vpnGatewayId>vgw-000000000000000c1</vpnGatewayId>
      <state>available</state>
      <type>ipsec.1</type>
      <attachments/>
    </item>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DetachClassicLinkVpc

This action is deprecated.

```http
POST / HTTP/1.1

Action=DetachClassicLinkVpc
&Version=2016-11-15
&InstanceId=example
&VpcId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DetachClassicLinkVpc is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000135</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DetachImageWatermark

Removes a watermark from the specified AMI.

```http
POST / HTTP/1.1

Action=DetachImageWatermark
&Version=2016-11-15
&ImageId=example
&WatermarkKey=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DetachImageWatermark is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000136</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DetachInternetGateway

Detaches an internet gateway from a VPC, disabling connectivity between the internet and the VPC.

```http
POST / HTTP/1.1

Action=DetachInternetGateway
&Version=2016-11-15
&InternetGatewayId=example
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInternetGatewayID.NotFound</Code>
      <Message>The internetGateway ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000137</RequestID>
</Response>
```

`400` `InvalidInternetGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DetachNetworkInterface

Detaches a network interface from an instance.

```http
POST / HTTP/1.1

Action=DetachNetworkInterface
&Version=2016-11-15
&AttachmentId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkInterfaceAttachmentId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000138</RequestID>
</Response>
```

`400` `InvalidNetworkInterfaceAttachmentId.Malformed`: the placeholder values failed the operation's input validation.

### DetachVerifiedAccessTrustProvider

Detaches the specified Amazon Web Services Verified Access trust provider from the specified Amazon Web Services Verified Access instance.

```http
POST / HTTP/1.1

Action=DetachVerifiedAccessTrustProvider
&Version=2016-11-15
&VerifiedAccessInstanceId=example
&VerifiedAccessTrustProviderId=example
&ClientToken=418f71b6-ab79-414c-aec2-3a8e18c640b9
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVerifiedAccessInstanceId.NotFound</Code>
      <Message>The verified-access-instance ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000139</RequestID>
</Response>
```

`400` `InvalidVerifiedAccessInstanceId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DetachVolume

Detaches an EBS volume from an instance.

```http
POST / HTTP/1.1

Action=DetachVolume
&Version=2016-11-15
&VolumeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVolumeID.Malformed</Code>
      <Message>The volume ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000013a</RequestID>
</Response>
```

`400` `InvalidVolumeID.Malformed`: the placeholder values failed the operation's input validation.

### DetachVpnGateway

Detaches a virtual private gateway from a VPC.

```http
POST / HTTP/1.1

Action=DetachVpnGateway
&Version=2016-11-15
&VpcId=example
&VpnGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpnGatewayID.NotFound</Code>
      <Message>The vpnGateway ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000013b</RequestID>
</Response>
```

`400` `InvalidVpnGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisableAddressTransfer

Disables Elastic IP address transfer.

```http
POST / HTTP/1.1

Action=DisableAddressTransfer
&Version=2016-11-15
&AllocationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAllocationID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000013c</RequestID>
</Response>
```

`400` `InvalidAllocationID.Malformed`: the placeholder values failed the operation's input validation.

### DisableAllowedImagesSettings

Disables Allowed AMIs for your account in the specified Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=DisableAllowedImagesSettings
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DisableAllowedImagesSettingsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <allowedImagesSettingsState>disabled</allowedImagesSettingsState>
  <requestId>0e206525-e6e9-490a-a374-6a2929c8def0</requestId>
</DisableAllowedImagesSettingsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DisableApplicationStatusCheckSuppression

Disables suppression of application status checks for the specified instances.

```http
POST / HTTP/1.1

Action=DisableApplicationStatusCheckSuppression
&Version=2016-11-15
&ClientToken=59563157-a29a-4778-8bab-b9ed62a22d77
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter InstanceId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000013d</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### DisableAwsNetworkPerformanceMetricSubscription

Disables Infrastructure Performance metric subscriptions.

```http
POST / HTTP/1.1

Action=DisableAwsNetworkPerformanceMetricSubscription
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter Source</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000013e</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### DisableCapacityManager

Disables EC2 Capacity Manager for your account.

```http
POST / HTTP/1.1

Action=DisableCapacityManager
&Version=2016-11-15
&ClientToken=75726804-3d13-4b22-8ae7-c4555da66b39
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>CapacityManager.Disabled</Code>
      <Message>Capacity Manager isn't enabled for this account.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000013f</RequestID>
</Response>
```

`400` `CapacityManager.Disabled`.

### DisableEbsEncryptionByDefault

Disables EBS encryption by default for your account in the current Region.

```http
POST / HTTP/1.1

Action=DisableEbsEncryptionByDefault
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DisableEbsEncryptionByDefaultResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ebsEncryptionByDefault>false</ebsEncryptionByDefault>
  <requestId>b9391411-90cb-41d4-9c4d-5bc0c970e611</requestId>
</DisableEbsEncryptionByDefaultResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DisableFastLaunch

Discontinue Windows fast launch for a Windows AMI, and clean up existing pre-provisioned snapshots.

```http
POST / HTTP/1.1

Action=DisableFastLaunch
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAMIID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000140</RequestID>
</Response>
```

`400` `InvalidAMIID.Malformed`: the placeholder values failed the operation's input validation.

### DisableFastSnapshotRestores

Disables fast snapshot restores for the specified snapshots in the specified Availability Zones.

```http
POST / HTTP/1.1

Action=DisableFastSnapshotRestores
&Version=2016-11-15
&SourceSnapshotId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>Either availabilityZone or availabilityZoneId must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000141</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### DisableImage

Sets the AMI state to disabled and removes all launch permissions from the AMI.

```http
POST / HTTP/1.1

Action=DisableImage
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisableImage is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000142</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisableImageBlockPublicAccess

Disables block public access for AMIs at the account level in the specified Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=DisableImageBlockPublicAccess
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DisableImageBlockPublicAccessResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <imageBlockPublicAccessState>unblocked</imageBlockPublicAccessState>
  <requestId>f1aa8bf1-24be-4ae7-b432-7a0eab91cb23</requestId>
</DisableImageBlockPublicAccessResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DisableImageDeprecation

Cancels the deprecation of the specified AMI.

```http
POST / HTTP/1.1

Action=DisableImageDeprecation
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisableImageDeprecation is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000143</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisableImageDeregistrationProtection

Disables deregistration protection for an AMI.

```http
POST / HTTP/1.1

Action=DisableImageDeregistrationProtection
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisableImageDeregistrationProtection is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000144</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisableInstanceSqlHaStandbyDetections

Disable Amazon EC2 instances running in an SQL Server High Availability cluster from SQL Server High Availability instance standby detection monitoring.

```http
POST / HTTP/1.1

Action=DisableInstanceSqlHaStandbyDetections
&Version=2016-11-15
&InstanceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>The instance ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000145</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### DisableIpamOrganizationAdminAccount

Disable the IPAM account.

```http
POST / HTTP/1.1

Action=DisableIpamOrganizationAdminAccount
&Version=2016-11-15
&DelegatedAdminAccountId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisableIpamOrganizationAdminAccount is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000146</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisableIpamPolicy

Disables an IPAM policy.

```http
POST / HTTP/1.1

Action=DisableIpamPolicy
&Version=2016-11-15
&IpamPolicyId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamPolicyId.Malformed</Code>
      <Message>The ipam-policy ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000147</RequestID>
</Response>
```

`400` `InvalidIpamPolicyId.Malformed`: the placeholder values failed the operation's input validation.

### DisableRouteServerPropagation

Disables route propagation from a route server to a specified route table.

```http
POST / HTTP/1.1

Action=DisableRouteServerPropagation
&Version=2016-11-15
&RouteServerId=example
&RouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000148</RequestID>
</Response>
```

`400` `InvalidRouteServerId.Malformed`: the placeholder values failed the operation's input validation.

### DisableSerialConsoleAccess

Disables access to the EC2 serial console of all instances for your account.

```http
POST / HTTP/1.1

Action=DisableSerialConsoleAccess
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DisableSerialConsoleAccessResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <serialConsoleAccessEnabled>false</serialConsoleAccessEnabled>
  <requestId>fa410eb3-6b94-46d4-bdea-b773426d3d71</requestId>
</DisableSerialConsoleAccessResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DisableSnapshotBlockPublicAccess

Disables the block public access for snapshots setting at the account level for the specified Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=DisableSnapshotBlockPublicAccess
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<DisableSnapshotBlockPublicAccessResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <state>unblocked</state>
  <requestId>6167f10e-4742-4456-a0fc-3fc26f309d0f</requestId>
</DisableSnapshotBlockPublicAccessResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DisableTransitGatewayRouteTablePropagation

Disables the specified resource attachment from propagating routes to the specified propagation route table.

```http
POST / HTTP/1.1

Action=DisableTransitGatewayRouteTablePropagation
&Version=2016-11-15
&TransitGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableId.Malformed</Code>
      <Message>Invalid Transit Gateway Route Table id example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000149</RequestID>
</Response>
```

`400` `InvalidRouteTableId.Malformed`: the placeholder values failed the operation's input validation.

### DisableVgwRoutePropagation

Disables a virtual private gateway (VGW) from propagating routes to a specified route table of a VPC.

```http
POST / HTTP/1.1

Action=DisableVgwRoutePropagation
&Version=2016-11-15
&GatewayId=example
&RouteTableId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisableVgwRoutePropagation is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000014a</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisableVpcClassicLink

This action is deprecated.

```http
POST / HTTP/1.1

Action=DisableVpcClassicLink
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000014b</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisableVpcClassicLinkDnsSupport

This action is deprecated.

```http
POST / HTTP/1.1

Action=DisableVpcClassicLinkDnsSupport
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisableVpcClassicLinkDnsSupport is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000014c</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateAddress

Disassociates an Elastic IP address from the instance or network interface it's associated with.

```http
POST / HTTP/1.1

Action=DisassociateAddress
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter associationId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000014d</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### DisassociateApplicationStatusCheck

Disassociates an application status check from instances or tags .

```http
POST / HTTP/1.1

Action=DisassociateApplicationStatusCheck
&Version=2016-11-15
&ApplicationStatusCheckId=example
&ClientToken=f577f25d-85ff-46e8-93c0-2108b863dc6f
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisassociateApplicationStatusCheck is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000014e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateCapacityReservationBillingOwner

Cancels a pending request to assign billing of the unused capacity of a Capacity Reservation to a consumer account, or revokes a request that has already been accepted.

```http
POST / HTTP/1.1

Action=DisassociateCapacityReservationBillingOwner
&Version=2016-11-15
&CapacityReservationId=example
&UnusedReservationBillingOwnerId=100000000000
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisassociateCapacityReservationBillingOwner is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000014f</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateClientVpnTargetNetwork

Disassociates a target network from the specified Client VPN endpoint.

```http
POST / HTTP/1.1

Action=DisassociateClientVpnTargetNetwork
&Version=2016-11-15
&ClientVpnEndpointId=example
&AssociationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisassociateClientVpnTargetNetwork is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000150</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateEnclaveCertificateIamRole

Disassociates an IAM role from an Certificate Manager (ACM) certificate.

```http
POST / HTTP/1.1

Action=DisassociateEnclaveCertificateIamRole
&Version=2016-11-15
&CertificateArn=arn%3Aaws%3Aec2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&RoleArn=arn%3Aaws%3Aec2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCertificateArn.Malformed</Code>
      <Message>The request must contain a valid certificate arn</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000151</RequestID>
</Response>
```

`400` `InvalidCertificateArn.Malformed`: the placeholder values failed the operation's input validation.

### DisassociateIamInstanceProfile

Disassociates an IAM instance profile from a running or stopped instance.

```http
POST / HTTP/1.1

Action=DisassociateIamInstanceProfile
&Version=2016-11-15
&AssociationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid Id: example</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000152</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DisassociateInstanceEventWindow

Disassociates one or more targets from an event window.

```http
POST / HTTP/1.1

Action=DisassociateInstanceEventWindow
&Version=2016-11-15
&InstanceEventWindowId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'associationTarget' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000153</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### DisassociateIpamByoasn

Remove the association between your Autonomous System Number (ASN) and your BYOIP CIDR.

```http
POST / HTTP/1.1

Action=DisassociateIpamByoasn
&Version=2016-11-15
&Asn=example
&Cidr=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisassociateIpamByoasn is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000154</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateIpamResourceDiscovery

Disassociates a resource discovery from an Amazon VPC IPAM.

```http
POST / HTTP/1.1

Action=DisassociateIpamResourceDiscovery
&Version=2016-11-15
&IpamResourceDiscoveryAssociationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisassociateIpamResourceDiscovery is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000155</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateNatGatewayAddress

Disassociates secondary Elastic IP addresses (EIPs) from a public NAT gateway.

```http
POST / HTTP/1.1

Action=DisassociateNatGatewayAddress
&Version=2016-11-15
&NatGatewayId=example
&AssociationId.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisassociateNatGatewayAddress is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000156</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateRouteServer

Disassociates a route server from a VPC.

```http
POST / HTTP/1.1

Action=DisassociateRouteServer
&Version=2016-11-15
&RouteServerId=example
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000157</RequestID>
</Response>
```

`400` `InvalidRouteServerId.Malformed`: the placeholder values failed the operation's input validation.

### DisassociateRouteTable

Disassociates a subnet or gateway from a route table.

```http
POST / HTTP/1.1

Action=DisassociateRouteTable
&Version=2016-11-15
&AssociationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAssociationID.NotFound</Code>
      <Message>The association ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000158</RequestID>
</Response>
```

`400` `InvalidAssociationID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisassociateSecurityGroupVpc

Disassociates a security group from a VPC.

```http
POST / HTTP/1.1

Action=DisassociateSecurityGroupVpc
&Version=2016-11-15
&GroupId=example
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000159</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisassociateSubnetCidrBlock

Disassociates a CIDR block from a subnet.

```http
POST / HTTP/1.1

Action=DisassociateSubnetCidrBlock
&Version=2016-11-15
&AssociationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSubnetCidrBlockAssociationID.NotFound</Code>
      <Message>The subnet CIDR block with association ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000015a</RequestID>
</Response>
```

`400` `InvalidSubnetCidrBlockAssociationID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisassociateTransitGatewayMulticastDomain

Disassociates the specified subnets from the transit gateway multicast domain.

```http
POST / HTTP/1.1

Action=DisassociateTransitGatewayMulticastDomain
&Version=2016-11-15
&TransitGatewayMulticastDomainId=example
&TransitGatewayAttachmentId=example
&SubnetIds.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisassociateTransitGatewayMulticastDomain is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000015b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateTransitGatewayPolicyTable

Removes the association between an an attachment and a policy table.

```http
POST / HTTP/1.1

Action=DisassociateTransitGatewayPolicyTable
&Version=2016-11-15
&TransitGatewayPolicyTableId=example
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisassociateTransitGatewayPolicyTable is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000015c</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateTransitGatewayRouteTable

Disassociates a resource attachment from a transit gateway route table.

```http
POST / HTTP/1.1

Action=DisassociateTransitGatewayRouteTable
&Version=2016-11-15
&TransitGatewayRouteTableId=example
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisassociateTransitGatewayRouteTable is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000015d</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DisassociateTrunkInterface

Removes an association between a branch network interface with a trunk network interface.

```http
POST / HTTP/1.1

Action=DisassociateTrunkInterface
&Version=2016-11-15
&AssociationId=example
&ClientToken=d4249906-ac1f-451d-b2e3-65d06da133ed
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>OperationNotPermitted</Code>
      <Message>User 000000000000 is not permitted to perform this operation</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000015e</RequestID>
</Response>
```

`400` `OperationNotPermitted`.

### DisassociateVpcCidrBlock

Disassociates a CIDR block from a VPC.

```http
POST / HTTP/1.1

Action=DisassociateVpcCidrBlock
&Version=2016-11-15
&AssociationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation DisassociateVpcCidrBlock is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000015f</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableAddressTransfer

Enables Elastic IP address transfer.

```http
POST / HTTP/1.1

Action=EnableAddressTransfer
&Version=2016-11-15
&AllocationId=example
&TransferAccountId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value 'example' for transferAccountId. It must be an Amazon Web Services account ID of 12 digits</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000160</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### EnableAllowedImagesSettings

Enables Allowed AMIs for your account in the specified Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=EnableAllowedImagesSettings
&Version=2016-11-15
&AllowedImagesSettingsState=enabled
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<EnableAllowedImagesSettingsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <allowedImagesSettingsState>enabled</allowedImagesSettingsState>
  <requestId>c52a7ef3-8752-4000-8c74-f2aee21f7b26</requestId>
</EnableAllowedImagesSettingsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### EnableApplicationStatusCheckSuppression

Suppresses application status checks for the specified instances.

```http
POST / HTTP/1.1

Action=EnableApplicationStatusCheckSuppression
&Version=2016-11-15
&ClientToken=214efd77-4e17-4d44-b29b-50d7b82b70d6
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation EnableApplicationStatusCheckSuppression is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000161</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableAwsNetworkPerformanceMetricSubscription

Enables Infrastructure Performance subscriptions.

```http
POST / HTTP/1.1

Action=EnableAwsNetworkPerformanceMetricSubscription
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter Source</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000162</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### EnableCapacityManager

Enables EC2 Capacity Manager for your account.

```http
POST / HTTP/1.1

Action=EnableCapacityManager
&Version=2016-11-15
&ClientToken=85b57171-f55b-4fb1-993b-e4e7dd8b5aeb
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<EnableCapacityManagerResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityManagerStatus>enabled</capacityManagerStatus>
  <organizationsAccess>false</organizationsAccess>
  <requestId>762ec23c-a475-4dbe-a4aa-5db0a6295bba</requestId>
</EnableCapacityManagerResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### EnableEbsEncryptionByDefault

Enables EBS encryption by default for your account in the current Region.

```http
POST / HTTP/1.1

Action=EnableEbsEncryptionByDefault
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<EnableEbsEncryptionByDefaultResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ebsEncryptionByDefault>true</ebsEncryptionByDefault>
  <requestId>341d0113-3a82-4067-be05-cfade23e7c37</requestId>
</EnableEbsEncryptionByDefaultResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### EnableFastLaunch

When you enable Windows fast launch for a Windows AMI, images are pre-provisioned, using snapshots to launch instances up to 65% faster.

```http
POST / HTTP/1.1

Action=EnableFastLaunch
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAMIID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000163</RequestID>
</Response>
```

`400` `InvalidAMIID.Malformed`: the placeholder values failed the operation's input validation.

### EnableFastSnapshotRestores

Enables fast snapshot restores for the specified snapshots in the specified Availability Zones.

```http
POST / HTTP/1.1

Action=EnableFastSnapshotRestores
&Version=2016-11-15
&SourceSnapshotId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>Either availabilityZone or availabilityZoneId must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000164</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### EnableImage

Re-enables a disabled AMI.

```http
POST / HTTP/1.1

Action=EnableImage
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation EnableImage is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000165</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableImageBlockPublicAccess

Enables block public access for AMIs at the account level in the specified Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=EnableImageBlockPublicAccess
&Version=2016-11-15
&ImageBlockPublicAccessState=block-new-sharing
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<EnableImageBlockPublicAccessResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <imageBlockPublicAccessState>block-new-sharing</imageBlockPublicAccessState>
  <requestId>bd4b8b50-692d-448f-8e0d-4ad8902a507a</requestId>
</EnableImageBlockPublicAccessResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### EnableImageDeprecation

Enables deprecation of the specified AMI at the specified date and time.

```http
POST / HTTP/1.1

Action=EnableImageDeprecation
&Version=2016-11-15
&ImageId=example
&DeprecateAt=2026-11-16T12%3A11%3A33Z
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation EnableImageDeprecation is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000166</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableImageDeregistrationProtection

Enables deregistration protection for an AMI.

```http
POST / HTTP/1.1

Action=EnableImageDeregistrationProtection
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation EnableImageDeregistrationProtection is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000167</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableInstanceSqlHaStandbyDetections

Enable Amazon EC2 instances running in an SQL Server High Availability cluster for SQL Server High Availability instance standby detection monitoring.

```http
POST / HTTP/1.1

Action=EnableInstanceSqlHaStandbyDetections
&Version=2016-11-15
&InstanceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>The instance ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000168</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### EnableIpamInternetRegistryAssociation

Enables Resource Public Key Infrastructure (RPKI) on an existing IPAM internet registry association by providing BGP Public Key Infrastructure (BPKI) certificate details.

```http
POST / HTTP/1.1

Action=EnableIpamInternetRegistryAssociation
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
&RpkiVersion=example
&ServiceUri=example
&ChildHandle=example
&ParentHandle=example
&ParentBpkiTa=example
&ClientToken=0a3b8832-f30a-4f4c-bb3b-b01f6a4a25ea
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation EnableIpamInternetRegistryAssociation is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000169</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableIpamOrganizationAdminAccount

Enable an Organizations member account as the IPAM admin account.

```http
POST / HTTP/1.1

Action=EnableIpamOrganizationAdminAccount
&Version=2016-11-15
&DelegatedAdminAccountId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation EnableIpamOrganizationAdminAccount is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000016a</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableIpamPolicy

Enables an IPAM policy.

```http
POST / HTTP/1.1

Action=EnableIpamPolicy
&Version=2016-11-15
&IpamPolicyId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamPolicyId.Malformed</Code>
      <Message>The ipam-policy ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000016b</RequestID>
</Response>
```

`400` `InvalidIpamPolicyId.Malformed`: the placeholder values failed the operation's input validation.

### EnableReachabilityAnalyzerOrganizationSharing

Establishes a trust relationship between Reachability Analyzer and Organizations.

```http
POST / HTTP/1.1

Action=EnableReachabilityAnalyzerOrganizationSharing
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<EnableReachabilityAnalyzerOrganizationSharingResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <returnValue>true</returnValue>
  <requestId>6b6f8be2-432d-47c6-889c-3ceda3ddcbde</requestId>
</EnableReachabilityAnalyzerOrganizationSharingResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### EnableRouteServerPropagation

Defines which route tables the route server can update with routes.

```http
POST / HTTP/1.1

Action=EnableRouteServerPropagation
&Version=2016-11-15
&RouteServerId=example
&RouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000016c</RequestID>
</Response>
```

`400` `InvalidRouteServerId.Malformed`: the placeholder values failed the operation's input validation.

### EnableSerialConsoleAccess

Enables access to the EC2 serial console of all instances for your account.

```http
POST / HTTP/1.1

Action=EnableSerialConsoleAccess
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<EnableSerialConsoleAccessResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <serialConsoleAccessEnabled>true</serialConsoleAccessEnabled>
  <requestId>ed87dab9-dc07-4a6e-9764-bf3d5914b85b</requestId>
</EnableSerialConsoleAccessResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### EnableSnapshotBlockPublicAccess

Enables or modifies the block public access for snapshots setting at the account level for the specified Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=EnableSnapshotBlockPublicAccess
&Version=2016-11-15
&State=block-all-sharing
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<EnableSnapshotBlockPublicAccessResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <state>block-all-sharing</state>
  <requestId>a07eda2e-9da2-4508-a0a2-44913ba309dc</requestId>
</EnableSnapshotBlockPublicAccessResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### EnableTransitGatewayRouteTablePropagation

Enables the specified attachment to propagate routes to the specified propagation route table.

```http
POST / HTTP/1.1

Action=EnableTransitGatewayRouteTablePropagation
&Version=2016-11-15
&TransitGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableId.Malformed</Code>
      <Message>Invalid Transit Gateway Route Table id example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000016d</RequestID>
</Response>
```

`400` `InvalidRouteTableId.Malformed`: the placeholder values failed the operation's input validation.

### EnableVgwRoutePropagation

Enables a virtual private gateway (VGW) to propagate routes to the specified route table of a VPC.

```http
POST / HTTP/1.1

Action=EnableVgwRoutePropagation
&Version=2016-11-15
&GatewayId=example
&RouteTableId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation EnableVgwRoutePropagation is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000016e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableVolumeIO

Enables I/O operations for a volume that had I/O operations disabled because the data on the volume was potentially inconsistent.

```http
POST / HTTP/1.1

Action=EnableVolumeIO
&Version=2016-11-15
&VolumeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVolumeID.Malformed</Code>
      <Message>The volume ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000016f</RequestID>
</Response>
```

`400` `InvalidVolumeID.Malformed`: the placeholder values failed the operation's input validation.

### EnableVpcClassicLink

This action is deprecated.

```http
POST / HTTP/1.1

Action=EnableVpcClassicLink
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation EnableVpcClassicLink is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000170</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### EnableVpcClassicLinkDnsSupport

This action is deprecated.

```http
POST / HTTP/1.1

Action=EnableVpcClassicLinkDnsSupport
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation EnableVpcClassicLinkDnsSupport is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000171</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ExportClientVpnClientCertificateRevocationList

Downloads the client certificate revocation list for the specified Client VPN endpoint.

```http
POST / HTTP/1.1

Action=ExportClientVpnClientCertificateRevocationList
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ExportClientVpnClientCertificateRevocationList is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000172</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ExportClientVpnClientConfiguration

Downloads the contents of the Client VPN endpoint configuration file for the specified Client VPN endpoint.

```http
POST / HTTP/1.1

Action=ExportClientVpnClientConfiguration
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ExportClientVpnClientConfiguration is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000173</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ExportImage

Exports an Amazon Machine Image (AMI) to a VM file.

```http
POST / HTTP/1.1

Action=ExportImage
&Version=2016-11-15
&ClientToken=c8624c70-5982-436c-9bde-a31eae9baa92
&DiskImageFormat=VMDK
&ImageId=example
&S3ExportLocation.S3Bucket=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAMIID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000174</RequestID>
</Response>
```

`400` `InvalidAMIID.Malformed`: the placeholder values failed the operation's input validation.

### ExportTransitGatewayRoutes

Exports routes from the specified transit gateway route table to the specified S3 bucket.

```http
POST / HTTP/1.1

Action=ExportTransitGatewayRoutes
&Version=2016-11-15
&TransitGatewayRouteTableId=example
&S3Bucket=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ExportTransitGatewayRoutes is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000175</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ExportVerifiedAccessInstanceClientConfiguration

Exports the client configuration for a Verified Access instance.

```http
POST / HTTP/1.1

Action=ExportVerifiedAccessInstanceClientConfiguration
&Version=2016-11-15
&VerifiedAccessInstanceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ExportVerifiedAccessInstanceClientConfiguration is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000176</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetActiveVpnTunnelStatus

Returns the currently negotiated security parameters for an active VPN tunnel, including IKE version, DH groups, encryption algorithms, and integrity algorithms.

```http
POST / HTTP/1.1

Action=GetActiveVpnTunnelStatus
&Version=2016-11-15
&VpnConnectionId=example
&VpnTunnelOutsideIpAddress=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetActiveVpnTunnelStatus is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000177</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetAllowedImagesSettings

Gets the current state of the Allowed AMIs setting and the list of Allowed AMIs criteria at the account level in the specified Region.

```http
POST / HTTP/1.1

Action=GetAllowedImagesSettings
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetAllowedImagesSettingsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <state>enabled</state>
  <imageCriterionSet/>
  <managedBy>account</managedBy>
  <requestId>60775a61-d9ed-4531-a25f-69cd54d077c2</requestId>
</GetAllowedImagesSettingsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetAssociatedEnclaveCertificateIamRoles

Returns the IAM roles that are associated with the specified ACM (ACM) certificate.

```http
POST / HTTP/1.1

Action=GetAssociatedEnclaveCertificateIamRoles
&Version=2016-11-15
&CertificateArn=arn%3Aaws%3Aec2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCertificateArn.Malformed</Code>
      <Message>The request must contain a valid certificate arn</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000178</RequestID>
</Response>
```

`400` `InvalidCertificateArn.Malformed`: the placeholder values failed the operation's input validation.

### GetAssociatedIpv6PoolCidrs

Gets information about the IPv6 CIDR block associations for a specified IPv6 address pool.

```http
POST / HTTP/1.1

Action=GetAssociatedIpv6PoolCidrs
&Version=2016-11-15
&PoolId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpv6PoolID.Malformed</Code>
      <Message>The ipv6pool-ec2 ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000179</RequestID>
</Response>
```

`400` `InvalidIpv6PoolID.Malformed`: the placeholder values failed the operation's input validation.

### GetAwsNetworkPerformanceData

Gets network performance data.

```http
POST / HTTP/1.1

Action=GetAwsNetworkPerformanceData
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter StartTime</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000017a</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### GetCapacityManagerAttributes

Retrieves the current configuration and status of EC2 Capacity Manager for your account, including enablement status, Organizations access settings, and data ingestion status.

```http
POST / HTTP/1.1

Action=GetCapacityManagerAttributes
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetCapacityManagerAttributesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityManagerStatus>enabled</capacityManagerStatus>
  <organizationsAccess>false</organizationsAccess>
  <dataExportCount>1</dataExportCount>
  <ingestionStatus>initial-ingestion-in-progress</ingestionStatus>
  <requestId>b3c26df2-ff6a-4a93-bb40-8b8d419806f1</requestId>
</GetCapacityManagerAttributesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetCapacityManagerMetricData

Retrieves capacity usage metrics for your EC2 resources.

```http
POST / HTTP/1.1

Action=GetCapacityManagerMetricData
&Version=2016-11-15
&MetricName.1=reservation-total-capacity-hrs-vcpu
&StartTime=2026-11-16T12%3A11%3A33Z
&EndTime=2026-11-16T12%3A11%3A33Z
&Period=3600
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetCapacityManagerMetricData is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000017b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetCapacityManagerMetricDimensions

Retrieves the available dimension values for capacity metrics within a specified time range.

```http
POST / HTTP/1.1

Action=GetCapacityManagerMetricDimensions
&Version=2016-11-15
&GroupBy.1=resource-region
&StartTime=2026-11-16T12%3A11%3A33Z
&EndTime=2026-11-16T12%3A11%3A33Z
&MetricName.1=reservation-total-capacity-hrs-vcpu
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>EndTime must be later than StartTime</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000017c</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### GetCapacityManagerMonitoredTagKeys

Retrieves the tag keys that are currently being monitored by EC2 Capacity Manager.

```http
POST / HTTP/1.1

Action=GetCapacityManagerMonitoredTagKeys
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetCapacityManagerMonitoredTagKeysResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <capacityManagerTagKeySet/>
  <requestId>6349619f-0faa-4cc2-941a-b9f12200ad7d</requestId>
</GetCapacityManagerMonitoredTagKeysResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetCapacityReservationUsage

Gets usage information about a Capacity Reservation.

```http
POST / HTTP/1.1

Action=GetCapacityReservationUsage
&Version=2016-11-15
&CapacityReservationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000017d</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### GetClientVpnEndpointAuthorizationPolicy

Describes the authorization policy for a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=GetClientVpnEndpointAuthorizationPolicy
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetClientVpnEndpointAuthorizationPolicy is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000017e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetCoipPoolUsage

Describes the allocations from the specified customer-owned address pool.

```http
POST / HTTP/1.1

Action=GetCoipPoolUsage
&Version=2016-11-15
&PoolId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPoolID.Malformed</Code>
      <Message>The pool ID 'example' is invalid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000017f</RequestID>
</Response>
```

`400` `InvalidPoolID.Malformed`: the placeholder values failed the operation's input validation.

### GetConsoleOutput

Gets the console output for the specified instance.

```http
POST / HTTP/1.1

Action=GetConsoleOutput
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000180</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### GetConsoleScreenshot

Retrieve a JPG-format screenshot of a running instance to help with troubleshooting.

```http
POST / HTTP/1.1

Action=GetConsoleScreenshot
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000181</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### GetDeclarativePoliciesReportSummary

Retrieves a summary of the account status report.

```http
POST / HTTP/1.1

Action=GetDeclarativePoliciesReportSummary
&Version=2016-11-15
&ReportId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidDeclarativePoliciesReportId.NotFound</Code>
      <Message>The report ID 'example' does not exist.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000182</RequestID>
</Response>
```

`400` `InvalidDeclarativePoliciesReportId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetDefaultCreditSpecification

Describes the default credit option for CPU usage of a burstable performance instance family.

```http
POST / HTTP/1.1

Action=GetDefaultCreditSpecification
&Version=2016-11-15
&InstanceFamily=t2
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetDefaultCreditSpecificationResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <instanceFamilyCreditSpecification xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <instanceFamily>t2</instanceFamily>
    <cpuCredits>standard</cpuCredits>
  </instanceFamilyCreditSpecification>
  <requestId>0a674ae5-0045-4290-847d-3d34acb36eb1</requestId>
</GetDefaultCreditSpecificationResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetEbsDefaultKmsKeyId

Describes the default KMS key for EBS encryption by default for your account in this Region.

```http
POST / HTTP/1.1

Action=GetEbsDefaultKmsKeyId
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetEbsDefaultKmsKeyId is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000183</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetEbsEncryptionByDefault

Describes whether EBS encryption by default is enabled for your account in the current Region.

```http
POST / HTTP/1.1

Action=GetEbsEncryptionByDefault
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetEbsEncryptionByDefaultResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ebsEncryptionByDefault>true</ebsEncryptionByDefault>
  <requestId>1eaf312f-4fe3-4462-9a7d-48ca93aff6cb</requestId>
</GetEbsEncryptionByDefaultResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetEnabledIpamPolicy

Gets the enabled IPAM policy.

```http
POST / HTTP/1.1

Action=GetEnabledIpamPolicy
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetEnabledIpamPolicyResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <ipamPolicyEnabled>false</ipamPolicyEnabled>
  <requestId>b1dd481c-57e9-4c97-8f44-3c1e13e975a6</requestId>
</GetEnabledIpamPolicyResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetFlowLogsIntegrationTemplate

Generates a CloudFormation template that streamlines and automates the integration of VPC flow logs with Amazon Athena.

```http
POST / HTTP/1.1

Action=GetFlowLogsIntegrationTemplate
&Version=2016-11-15
&FlowLogId=example
&ConfigDeliveryS3DestinationArn=arn%3Aaws%3Aec2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'integrateServices' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000184</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### GetGroupsForCapacityReservation

Lists the resource groups to which a Capacity Reservation has been added.

```http
POST / HTTP/1.1

Action=GetGroupsForCapacityReservation
&Version=2016-11-15
&CapacityReservationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000185</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### GetHostReservationPurchasePreview

Preview a reservation purchase with configurations that match those of your Dedicated Host.

```http
POST / HTTP/1.1

Action=GetHostReservationPurchasePreview
&Version=2016-11-15
&HostIdSet.1=example
&OfferingId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidHostId.Malformed</Code>
      <Message>The specified Dedicated host IDs ['example'] are not valid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000186</RequestID>
</Response>
```

`400` `InvalidHostId.Malformed`: the placeholder values failed the operation's input validation.

### GetImageAncestry

Retrieves the ancestry chain of the specified AMI, tracing its lineage back to the root AMI.

```http
POST / HTTP/1.1

Action=GetImageAncestry
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAMIID.Malformed</Code>
      <Message>The image ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000187</RequestID>
</Response>
```

`400` `InvalidAMIID.Malformed`: the placeholder values failed the operation's input validation.

### GetImageBlockPublicAccessState

Gets the current state of block public access for AMIs at the account level in the specified Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=GetImageBlockPublicAccessState
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetImageBlockPublicAccessStateResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <imageBlockPublicAccessState>block-new-sharing</imageBlockPublicAccessState>
  <managedBy>account</managedBy>
  <requestId>75c865f1-8baf-4a52-a246-0b0b65e77f2f</requestId>
</GetImageBlockPublicAccessStateResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetInstanceMetadataDefaults

Gets the default instance metadata service (IMDS) settings that are set at the account level in the specified Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=GetInstanceMetadataDefaults
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetInstanceMetadataDefaultsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <accountLevel xmlns="http://ec2.amazonaws.com/doc/2016-11-15"/>
  <requestId>4a4e9bec-93dd-43d5-a741-499d5b32a802</requestId>
</GetInstanceMetadataDefaultsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetInstanceTpmEkPub

Gets the public endorsement key associated with the Nitro Trusted Platform Module (NitroTPM) for the specified instance.

```http
POST / HTTP/1.1

Action=GetInstanceTpmEkPub
&Version=2016-11-15
&InstanceId=example
&KeyType=rsa-2048
&KeyFormat=der
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetInstanceTpmEkPub is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000188</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetInstanceTypesFromInstanceRequirements

Returns a list of instance types with the specified instance attributes.

```http
POST / HTTP/1.1

Action=GetInstanceTypesFromInstanceRequirements
&Version=2016-11-15
&ArchitectureType.1=i386
&VirtualizationType.1=hvm
&InstanceRequirements.VCpuCount.Min=1
&InstanceRequirements.MemoryMiB.Min=1
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetInstanceTypesFromInstanceRequirements is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000189</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetInstanceUefiData

A binary representation of the UEFI variable store.

```http
POST / HTTP/1.1

Action=GetInstanceUefiData
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetInstanceUefiData is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000018a</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamAddressHistory

Retrieve historical information about a CIDR within an IPAM scope.

```http
POST / HTTP/1.1

Action=GetIpamAddressHistory
&Version=2016-11-15
&Cidr=example
&IpamScopeId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamAddressHistory is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000018b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamDiscoveredAccounts

Gets IPAM discovered accounts.

```http
POST / HTTP/1.1

Action=GetIpamDiscoveredAccounts
&Version=2016-11-15
&IpamResourceDiscoveryId=example
&DiscoveryRegion=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamResourceDiscoveryId.Malformed</Code>
      <Message>The ipam-resource-discovery ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000018c</RequestID>
</Response>
```

`400` `InvalidIpamResourceDiscoveryId.Malformed`: the placeholder values failed the operation's input validation.

### GetIpamDiscoveredPublicAddresses

Gets the public IP addresses that have been discovered by IPAM.

```http
POST / HTTP/1.1

Action=GetIpamDiscoveredPublicAddresses
&Version=2016-11-15
&IpamResourceDiscoveryId=example
&AddressRegion=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamResourceDiscoveryId.Malformed</Code>
      <Message>The ipam-resource-discovery ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000018d</RequestID>
</Response>
```

`400` `InvalidIpamResourceDiscoveryId.Malformed`: the placeholder values failed the operation's input validation.

### GetIpamDiscoveredResourceCidrs

Returns the resource CIDRs that are monitored as part of a resource discovery.

```http
POST / HTTP/1.1

Action=GetIpamDiscoveredResourceCidrs
&Version=2016-11-15
&IpamResourceDiscoveryId=example
&ResourceRegion=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamDiscoveredResourceCidrs is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000018e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamDiscoveredRoutes

Retrieves Border Gateway Protocol (BGP) routes discovered by IPAM resource discovery for a specified Region.

```http
POST / HTTP/1.1

Action=GetIpamDiscoveredRoutes
&Version=2016-11-15
&IpamResourceDiscoveryId=example
&ResourceRegion=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamResourceDiscoveryId.Malformed</Code>
      <Message>The ipam-resource-discovery ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000018f</RequestID>
</Response>
```

`400` `InvalidIpamResourceDiscoveryId.Malformed`: the placeholder values failed the operation's input validation.

### GetIpamInternetRegistryAssociationAsns

Retrieves Autonomous System Numbers (ASNs) registered with an internet registry for an IPAM internet registry association.

```http
POST / HTTP/1.1

Action=GetIpamInternetRegistryAssociationAsns
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamInternetRegistryAssociationAsns is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000190</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamInternetRegistryAssociationCidrs

Retrieves IP address CIDRs registered with an internet registry for an IPAM internet registry association.

```http
POST / HTTP/1.1

Action=GetIpamInternetRegistryAssociationCidrs
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamInternetRegistryAssociationCidrs is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000191</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamPolicyAllocationRules

Gets the allocation rules for an IPAM policy.

```http
POST / HTTP/1.1

Action=GetIpamPolicyAllocationRules
&Version=2016-11-15
&IpamPolicyId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamPolicyAllocationRules is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000192</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamPolicyOrganizationTargets

Gets the Amazon Web Services Organizations targets for an IPAM policy.

```http
POST / HTTP/1.1

Action=GetIpamPolicyOrganizationTargets
&Version=2016-11-15
&IpamPolicyId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamPolicyOrganizationTargets is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000193</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamPoolAllocations

Get a list of all the CIDR allocations in an IPAM pool.

```http
POST / HTTP/1.1

Action=GetIpamPoolAllocations
&Version=2016-11-15
&IpamPoolId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamPoolAllocations is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000194</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamPoolCidrs

Get the CIDRs provisioned to an IPAM pool.

```http
POST / HTTP/1.1

Action=GetIpamPoolCidrs
&Version=2016-11-15
&IpamPoolId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamPoolId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000195</RequestID>
</Response>
```

`400` `InvalidIpamPoolId.Malformed`: the placeholder values failed the operation's input validation.

### GetIpamPrefixListResolverRules

Retrieves the CIDR selection rules for an IPAM prefix list resolver.

```http
POST / HTTP/1.1

Action=GetIpamPrefixListResolverRules
&Version=2016-11-15
&IpamPrefixListResolverId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamPrefixListResolverRules is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000196</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamPrefixListResolverVersionEntries

Retrieves the CIDR entries for a specific version of an IPAM prefix list resolver.

```http
POST / HTTP/1.1

Action=GetIpamPrefixListResolverVersionEntries
&Version=2016-11-15
&IpamPrefixListResolverId=example
&IpamPrefixListResolverVersion=1
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamPrefixListResolverVersionEntries is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000197</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamPrefixListResolverVersions

Retrieves version information for an IPAM prefix list resolver.

```http
POST / HTTP/1.1

Action=GetIpamPrefixListResolverVersions
&Version=2016-11-15
&IpamPrefixListResolverId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamPrefixListResolverVersions is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000198</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamResourceCidrs

Returns resource CIDRs managed by IPAM in a given scope.

```http
POST / HTTP/1.1

Action=GetIpamResourceCidrs
&Version=2016-11-15
&IpamScopeId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamResourceCidrs is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000199</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamRouteOriginAuthorizations

Retrieves the current Route Origin Authorizations (ROAs) published to the RPKI for an IPAM internet registry association.

```http
POST / HTTP/1.1

Action=GetIpamRouteOriginAuthorizations
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamRouteOriginAuthorizations is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000019a</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamRouteProtectionFindings

Retrieves route protection findings for an IPAM.

```http
POST / HTTP/1.1

Action=GetIpamRouteProtectionFindings
&Version=2016-11-15
&IpamId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000019b</RequestID>
</Response>
```

`400` `InvalidIpamId.Malformed`: the placeholder values failed the operation's input validation.

### GetIpamRoutingPolicyRegistrationDeltas

Retrieves the history of routing policy registration changes for an IPAM internet registry association.

```http
POST / HTTP/1.1

Action=GetIpamRoutingPolicyRegistrationDeltas
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamRoutingPolicyRegistrationDeltas is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000019c</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetIpamRoutingPolicyRegistrations

Retrieves routing policy registrations for an IPAM internet registry association.

```http
POST / HTTP/1.1

Action=GetIpamRoutingPolicyRegistrations
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetIpamRoutingPolicyRegistrations is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000019d</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetLaunchTemplateData

Retrieves the configuration data of the specified instance.

```http
POST / HTTP/1.1

Action=GetLaunchTemplateData
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetLaunchTemplateData is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000019e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetManagedPrefixListAssociations

Gets information about the resources that are associated with the specified managed prefix list.

```http
POST / HTTP/1.1

Action=GetManagedPrefixListAssociations
&Version=2016-11-15
&PrefixListId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPrefixListId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000019f</RequestID>
</Response>
```

`400` `InvalidPrefixListId.Malformed`: the placeholder values failed the operation's input validation.

### GetManagedPrefixListEntries

Gets information about the entries for a specified managed prefix list.

```http
POST / HTTP/1.1

Action=GetManagedPrefixListEntries
&Version=2016-11-15
&PrefixListId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPrefixListId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001a0</RequestID>
</Response>
```

`400` `InvalidPrefixListId.Malformed`: the placeholder values failed the operation's input validation.

### GetManagedResourceVisibility

Retrieves the managed resource visibility configuration for the account.

```http
POST / HTTP/1.1

Action=GetManagedResourceVisibility
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetManagedResourceVisibilityResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <visibility xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <defaultVisibility>visible</defaultVisibility>
  </visibility>
  <requestId>1e3cb6a5-2c81-4432-b050-03314e3e01ec</requestId>
</GetManagedResourceVisibilityResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetNetworkInsightsAccessScopeAnalysisFindings

Gets the findings for the specified Network Access Scope analysis.

```http
POST / HTTP/1.1

Action=GetNetworkInsightsAccessScopeAnalysisFindings
&Version=2016-11-15
&NetworkInsightsAccessScopeAnalysisId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetNetworkInsightsAccessScopeAnalysisFindings is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001a1</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetNetworkInsightsAccessScopeContent

Gets the content for the specified Network Access Scope.

```http
POST / HTTP/1.1

Action=GetNetworkInsightsAccessScopeContent
&Version=2016-11-15
&NetworkInsightsAccessScopeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkInsightsAccessScopeId.NotFound</Code>
      <Message>example does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001a2</RequestID>
</Response>
```

`400` `InvalidNetworkInsightsAccessScopeId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetPasswordData

Retrieves the encrypted administrator password for a running Windows instance.

```http
POST / HTTP/1.1

Action=GetPasswordData
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001a3</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### GetReservedInstancesExchangeQuote

Returns a quote and exchange information for exchanging one or more specified Convertible Reserved Instances for a new Convertible Reserved Instance.

```http
POST / HTTP/1.1

Action=GetReservedInstancesExchangeQuote
&Version=2016-11-15
&ReservedInstanceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidReservedInstancesID.NotFound</Code>
      <Message>The reserved-instances ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001a4</RequestID>
</Response>
```

`400` `InvalidReservedInstancesID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetRouteServerAssociations

Gets information about the associations for the specified route server.

```http
POST / HTTP/1.1

Action=GetRouteServerAssociations
&Version=2016-11-15
&RouteServerId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001a5</RequestID>
</Response>
```

`400` `InvalidRouteServerId.Malformed`: the placeholder values failed the operation's input validation.

### GetRouteServerPropagations

Gets information about the route propagations for the specified route server.

```http
POST / HTTP/1.1

Action=GetRouteServerPropagations
&Version=2016-11-15
&RouteServerId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001a6</RequestID>
</Response>
```

`400` `InvalidRouteServerId.Malformed`: the placeholder values failed the operation's input validation.

### GetRouteServerRoutingDatabase

Gets the routing database for the specified route server.

```http
POST / HTTP/1.1

Action=GetRouteServerRoutingDatabase
&Version=2016-11-15
&RouteServerId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteServerId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001a7</RequestID>
</Response>
```

`400` `InvalidRouteServerId.Malformed`: the placeholder values failed the operation's input validation.

### GetSecurityGroupsForVpc

Gets security groups that can be associated by the Amazon Web Services account making the request with network interfaces in the specified VPC.

```http
POST / HTTP/1.1

Action=GetSecurityGroupsForVpc
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcId.Malformed</Code>
      <Message>The vpc ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001a8</RequestID>
</Response>
```

`400` `InvalidVpcId.Malformed`: the placeholder values failed the operation's input validation.

### GetSerialConsoleAccessStatus

Retrieves the access status of your account to the EC2 serial console of all instances.

```http
POST / HTTP/1.1

Action=GetSerialConsoleAccessStatus
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetSerialConsoleAccessStatusResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <serialConsoleAccessEnabled>true</serialConsoleAccessEnabled>
  <managedBy>account</managedBy>
  <requestId>1996efa7-a53c-40dc-9d91-2a1eba30d227</requestId>
</GetSerialConsoleAccessStatusResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetSnapshotBlockPublicAccessState

Gets the current state of block public access for snapshots setting for the account and Region.

```http
POST / HTTP/1.1

Action=GetSnapshotBlockPublicAccessState
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetSnapshotBlockPublicAccessStateResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <state>block-all-sharing</state>
  <managedBy>account</managedBy>
  <requestId>a856563a-2abb-4aa0-b24c-c6c9533e041c</requestId>
</GetSnapshotBlockPublicAccessStateResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetSpotPlacementScores

Calculates the Spot placement score for a Region or Availability Zone based on the specified target capacity and compute requirements.

```http
POST / HTTP/1.1

Action=GetSpotPlacementScores
&Version=2016-11-15
&TargetCapacity=1
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetSpotPlacementScoresResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <spotPlacementScoreSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <region>af-south-1</region>
      <score>10</score>
    </item>
    <item>
      <region>ap-east-1</region>
      <score>10</score>
    </item>
… (35 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetSubnetCidrReservations

Gets information about the subnet CIDR reservations.

```http
POST / HTTP/1.1

Action=GetSubnetCidrReservations
&Version=2016-11-15
&SubnetId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSubnetID.NotFound</Code>
      <Message>The subnet ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001a9</RequestID>
</Response>
```

`400` `InvalidSubnetID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetTransitGatewayAttachmentPropagations

Lists the route tables to which the specified resource attachment propagates routes.

```http
POST / HTTP/1.1

Action=GetTransitGatewayAttachmentPropagations
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetTransitGatewayAttachmentPropagations is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001aa</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetTransitGatewayMeteringPolicyEntries

Retrieves the entries for a transit gateway metering policy.

```http
POST / HTTP/1.1

Action=GetTransitGatewayMeteringPolicyEntries
&Version=2016-11-15
&TransitGatewayMeteringPolicyId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayMeteringPolicyIdMalformedException</Code>
      <Message>The transit-gateway-metering-policy ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ab</RequestID>
</Response>
```

`400` `InvalidTransitGatewayMeteringPolicyIdMalformedException`: the placeholder values failed the operation's input validation.

### GetTransitGatewayMulticastDomainAssociations

Gets information about the associations for the transit gateway multicast domain.

```http
POST / HTTP/1.1

Action=GetTransitGatewayMulticastDomainAssociations
&Version=2016-11-15
&TransitGatewayMulticastDomainId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetTransitGatewayMulticastDomainAssociations is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ac</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetTransitGatewayPolicyTableAssociations

Gets a list of the transit gateway policy table associations.

```http
POST / HTTP/1.1

Action=GetTransitGatewayPolicyTableAssociations
&Version=2016-11-15
&TransitGatewayPolicyTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayPolicyTableId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ad</RequestID>
</Response>
```

`400` `InvalidTransitGatewayPolicyTableId.Malformed`: the placeholder values failed the operation's input validation.

### GetTransitGatewayPolicyTableEntries

Returns a list of transit gateway policy table entries.

```http
POST / HTTP/1.1

Action=GetTransitGatewayPolicyTableEntries
&Version=2016-11-15
&TransitGatewayPolicyTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayPolicyTableId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ae</RequestID>
</Response>
```

`400` `InvalidTransitGatewayPolicyTableId.Malformed`: the placeholder values failed the operation's input validation.

### GetTransitGatewayPrefixListReferences

Gets information about the prefix list references in a specified transit gateway route table.

```http
POST / HTTP/1.1

Action=GetTransitGatewayPrefixListReferences
&Version=2016-11-15
&TransitGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableId.Malformed</Code>
      <Message>Invalid Transit Gateway Route Table id example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001af</RequestID>
</Response>
```

`400` `InvalidRouteTableId.Malformed`: the placeholder values failed the operation's input validation.

### GetTransitGatewayRouteTableAssociations

Gets information about the associations for the specified transit gateway route table.

```http
POST / HTTP/1.1

Action=GetTransitGatewayRouteTableAssociations
&Version=2016-11-15
&TransitGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableId.Malformed</Code>
      <Message>Invalid Transit Gateway Route Table id example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001b0</RequestID>
</Response>
```

`400` `InvalidRouteTableId.Malformed`: the placeholder values failed the operation's input validation.

### GetTransitGatewayRouteTablePropagations

Gets information about the route table propagations for the specified transit gateway route table.

```http
POST / HTTP/1.1

Action=GetTransitGatewayRouteTablePropagations
&Version=2016-11-15
&TransitGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidRouteTableId.Malformed</Code>
      <Message>Invalid Transit Gateway Route Table id example.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001b1</RequestID>
</Response>
```

`400` `InvalidRouteTableId.Malformed`: the placeholder values failed the operation's input validation.

### GetVerifiedAccessEndpointPolicy

Get the Verified Access policy associated with the endpoint.

```http
POST / HTTP/1.1

Action=GetVerifiedAccessEndpointPolicy
&Version=2016-11-15
&VerifiedAccessEndpointId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetVerifiedAccessEndpointPolicy is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001b2</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetVerifiedAccessEndpointTargets

Gets the targets for the specified network CIDR endpoint for Verified Access.

```http
POST / HTTP/1.1

Action=GetVerifiedAccessEndpointTargets
&Version=2016-11-15
&VerifiedAccessEndpointId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetVerifiedAccessEndpointTargets is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001b3</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetVerifiedAccessGroupPolicy

Shows the contents of the Verified Access policy associated with the group.

```http
POST / HTTP/1.1

Action=GetVerifiedAccessGroupPolicy
&Version=2016-11-15
&VerifiedAccessGroupId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetVerifiedAccessGroupPolicy is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001b4</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetVpcResourcesBlockingEncryptionEnforcement

Gets information about resources in a VPC that are blocking encryption enforcement.

```http
POST / HTTP/1.1

Action=GetVpcResourcesBlockingEncryptionEnforcement
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001b5</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### GetVpnConnectionDeviceSampleConfiguration

Download an Amazon Web Services-provided sample configuration file to be used with the customer gateway device specified for your Site-to-Site VPN connection.

```http
POST / HTTP/1.1

Action=GetVpnConnectionDeviceSampleConfiguration
&Version=2016-11-15
&VpnConnectionId=example
&VpnConnectionDeviceTypeId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetVpnConnectionDeviceSampleConfiguration is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001b6</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### GetVpnConnectionDeviceTypes

Obtain a list of customer gateway devices for which sample configuration files can be provided.

```http
POST / HTTP/1.1

Action=GetVpnConnectionDeviceTypes
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<GetVpnConnectionDeviceTypesResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpnConnectionDeviceTypeSet>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <vpnConnectionDeviceTypeId>6edf2732</vpnConnectionDeviceTypeId>
      <vendor>AXGate</vendor>
      <platform>NF</platform>
      <software>aos 3.2+</software>
    </item>
    <item>
      <vpnConnectionDeviceTypeId>7ec8f46a</vpnConnectionDeviceTypeId>
… (193 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### GetVpnTunnelReplacementStatus

Get details of available tunnel endpoint maintenance.

```http
POST / HTTP/1.1

Action=GetVpnTunnelReplacementStatus
&Version=2016-11-15
&VpnConnectionId=example
&VpnTunnelOutsideIpAddress=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation GetVpnTunnelReplacementStatus is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001b7</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ImportClientVpnClientCertificateRevocationList

Uploads a client certificate revocation list to the specified Client VPN endpoint.

```http
POST / HTTP/1.1

Action=ImportClientVpnClientCertificateRevocationList
&Version=2016-11-15
&ClientVpnEndpointId=example
&CertificateRevocationList=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ImportClientVpnClientCertificateRevocationList is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001b8</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ImportImage

To import your virtual machines (VMs) with a console-based experience, you can use the Import virtual machine images to Amazon Web Services template in the Migration Hub Orchestrator console .

```http
POST / HTTP/1.1

Action=ImportImage
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>diskContainers must name at least one disk image to import</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001b9</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ImportInstance

We recommend that you use the ImportImage API instead.

```http
POST / HTTP/1.1

Action=ImportInstance
&Version=2016-11-15
&Platform=Windows
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ImportInstance is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ba</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ImportKeyPair

Imports the public key from an RSA or ED25519 key pair that you created using a third-party tool.

```http
POST / HTTP/1.1

Action=ImportKeyPair
&Version=2016-11-15
&KeyName=example
&PublicKeyMaterial=ZXhhbXBsZQ%3D%3D
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidKeyPair.Format</Code>
      <Message>Key is not in valid OpenSSH public key format</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001bb</RequestID>
</Response>
```

`400` `InvalidKeyPair.Format`.

### ImportSnapshot

Imports a disk into an EBS snapshot.

```http
POST / HTTP/1.1

Action=ImportSnapshot
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>diskContainer must name the disk image to import</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001bc</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ImportVolume

This API action supports only single-volume VMs.

```http
POST / HTTP/1.1

Action=ImportVolume
&Version=2016-11-15
&Image.Format=VMDK
&Image.Bytes=1
&Image.ImportManifestUrl=example
&Volume.Size=1
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ImportVolume is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001bd</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListImagesInRecycleBin

Lists one or more AMIs that are currently in the Recycle Bin.

```http
POST / HTTP/1.1

Action=ListImagesInRecycleBin
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ListImagesInRecycleBin is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001be</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ListSnapshotsInRecycleBin

Lists one or more snapshots that are currently in the Recycle Bin.

```http
POST / HTTP/1.1

Action=ListSnapshotsInRecycleBin
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<ListSnapshotsInRecycleBinResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <snapshotSet/>
  <requestId>51f6032d-c82e-477c-bc0e-60ac619e1e69</requestId>
</ListSnapshotsInRecycleBinResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ListVolumesInRecycleBin

Lists one or more volumes that are currently in the Recycle Bin.

```http
POST / HTTP/1.1

Action=ListVolumesInRecycleBin
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<ListVolumesInRecycleBinResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <volumeSet/>
  <requestId>dec75c54-7971-4446-bcbe-223f53757648</requestId>
</ListVolumesInRecycleBinResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### LockSnapshot

Locks an Amazon EBS snapshot in either governance or compliance mode to protect it against accidental or malicious deletions for a specific duration.

```http
POST / HTTP/1.1

Action=LockSnapshot
&Version=2016-11-15
&SnapshotId=example
&LockMode=compliance
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation LockSnapshot is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001bf</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyAccountVpcEncryptionControl

Modifies the account-level VPC Encryption Control configuration.

```http
POST / HTTP/1.1

Action=ModifyAccountVpcEncryptionControl
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<ModifyAccountVpcEncryptionControlResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <accountVpcEncryptionControl xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <state>default-state</state>
    <mode>unmanaged</mode>
    <exclusions>
      <internetGateway>disabled</internetGateway>
      <egressOnlyInternetGateway>disabled</egressOnlyInternetGateway>
      <natGateway>disabled</natGateway>
      <virtualPrivateGateway>disabled</virtualPrivateGateway>
      <vpcPeering>disabled</vpcPeering>
… (8 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ModifyAddressAttribute

Modifies an attribute of the specified Elastic IP address.

```http
POST / HTTP/1.1

Action=ModifyAddressAttribute
&Version=2016-11-15
&AllocationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter DomainName</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001c0</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ModifyApplicationStatusCheck

Modifies an existing application status check.

```http
POST / HTTP/1.1

Action=ModifyApplicationStatusCheck
&Version=2016-11-15
&ApplicationStatusCheckId=example
&ClientToken=7a49f0df-329a-442a-b2b2-a69d94a8e192
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidApplicationStatusCheckId.Malformed</Code>
      <Message>The application-status-check ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001c1</RequestID>
</Response>
```

`400` `InvalidApplicationStatusCheckId.Malformed`: the placeholder values failed the operation's input validation.

### ModifyAvailabilityZoneGroup

Changes the opt-in status of the specified zone group for your account.

```http
POST / HTTP/1.1

Action=ModifyAvailabilityZoneGroup
&Version=2016-11-15
&GroupName=example
&OptInStatus=opted-in
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyAvailabilityZoneGroup is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001c2</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyCapacityReservation

Modifies a Capacity Reservation's capacity, instance eligibility, and the conditions under which it is to be released.

```http
POST / HTTP/1.1

Action=ModifyCapacityReservation
&Version=2016-11-15
&CapacityReservationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001c3</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### ModifyCapacityReservationFleet

Modifies a Capacity Reservation Fleet.

```http
POST / HTTP/1.1

Action=ModifyCapacityReservationFleet
&Version=2016-11-15
&CapacityReservationFleetId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationFleetId.Malformed</Code>
      <Message>Capacity Reservation Fleet ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001c4</RequestID>
</Response>
```

`400` `InvalidCapacityReservationFleetId.Malformed`: the placeholder values failed the operation's input validation.

### ModifyClientVpnEndpoint

Modifies the specified Client VPN endpoint.

```http
POST / HTTP/1.1

Action=ModifyClientVpnEndpoint
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyClientVpnEndpoint is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001c5</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyClientVpnEndpointAuthorizationPolicy

Creates or updates the authorization policy for a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=ModifyClientVpnEndpointAuthorizationPolicy
&Version=2016-11-15
&ClientVpnEndpointId=example
&ClientToken=c122770e-2349-4df8-9bc4-9ba2c1b977a2
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyClientVpnEndpointAuthorizationPolicy is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001c6</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyDefaultCreditSpecification

Modifies the default credit option for CPU usage of burstable performance instances.

```http
POST / HTTP/1.1

Action=ModifyDefaultCreditSpecification
&Version=2016-11-15
&InstanceFamily=t2
&CpuCredits=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>The CpuCredits parameter requires a value of either Standard or Unlimited. Change the value and try again.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001c7</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyEbsDefaultKmsKeyId

Changes the default KMS key for EBS encryption by default for your account in this Region.

```http
POST / HTTP/1.1

Action=ModifyEbsDefaultKmsKeyId
&Version=2016-11-15
&KmsKeyId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyEbsDefaultKmsKeyId is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001c8</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyFleet

Modifies the specified EC2 Fleet.

```http
POST / HTTP/1.1

Action=ModifyFleet
&Version=2016-11-15
&FleetId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyFleet is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001c9</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyFpgaImageAttribute

Modifies the specified attribute of the specified Amazon FPGA Image (AFI).

```http
POST / HTTP/1.1

Action=ModifyFpgaImageAttribute
&Version=2016-11-15
&FpgaImageId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyFpgaImageAttribute is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ca</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyHosts

Modify the auto-placement setting of a Dedicated Host.

```http
POST / HTTP/1.1

Action=ModifyHosts
&Version=2016-11-15
&HostId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter autoPlacement, hostRecovery, hostMaintenance, instanceType or instanceFamily</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001cb</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ModifyIdFormat

Modifies the ID format for the specified resource on a per-Region basis.

```http
POST / HTTP/1.1

Action=ModifyIdFormat
&Version=2016-11-15
&Resource=example
&UseLongIds=false
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyIdFormat is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001cc</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyIdentityIdFormat

Modifies the ID format of a resource for a specified IAM user, IAM role, or the root user for an account; or all IAM users, IAM roles, and the root user for an account.

```http
POST / HTTP/1.1

Action=ModifyIdentityIdFormat
&Version=2016-11-15
&Resource=example
&UseLongIds=false
&PrincipalArn=arn%3Aaws%3Aec2%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTargetArn.Unknown</Code>
      <Message>Invalid TargetArn: arn:aws:ec2:us-east-1:000000000000:example/example</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001cd</RequestID>
</Response>
```

`400` `InvalidTargetArn.Unknown`.

### ModifyImageAttribute

Modifies the specified attribute of the specified AMI.

```http
POST / HTTP/1.1

Action=ModifyImageAttribute
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyImageAttribute is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ce</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyInstanceAttribute

Modifies the specified attribute of the specified instance.

```http
POST / HTTP/1.1

Action=ModifyInstanceAttribute
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must name an attribute to modify: instanceType | kernel | ramdisk | userData | disableApiTermination | instanceInitiatedShutdownBehavior | blockDeviceMapping | sourceDestCheck | groupSet | ebsOptimized | sriovNetSupport | enaSupport | enclaveOptions | disableApiStop</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001cf</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ModifyInstanceCapacityReservationAttributes

Modifies the Capacity Reservation settings for a stopped instance.

```http
POST / HTTP/1.1

Action=ModifyInstanceCapacityReservationAttributes
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'capacityReservationSpecification' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001d0</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### ModifyInstanceConnectEndpoint

Modifies the specified EC2 Instance Connect Endpoint.

```http
POST / HTTP/1.1

Action=ModifyInstanceConnectEndpoint
&Version=2016-11-15
&InstanceConnectEndpointId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceConnectEndpointId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001d1</RequestID>
</Response>
```

`400` `InvalidInstanceConnectEndpointId.Malformed`: the placeholder values failed the operation's input validation.

### ModifyInstanceCpuOptions

By default, all vCPUs for the instance type are active when you launch an instance.

```http
POST / HTTP/1.1

Action=ModifyInstanceCpuOptions
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyInstanceCpuOptions is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001d2</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyInstanceCreditSpecification

Modifies the credit option for CPU usage on a running or stopped burstable performance instance.

```http
POST / HTTP/1.1

Action=ModifyInstanceCreditSpecification
&Version=2016-11-15
&InstanceCreditSpecification.1.InstanceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyInstanceCreditSpecification is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001d3</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyInstanceEventStartTime

Modifies the start time for a scheduled Amazon EC2 instance event.

```http
POST / HTTP/1.1

Action=ModifyInstanceEventStartTime
&Version=2016-11-15
&InstanceId=example
&InstanceEventId=example
&NotBefore=2026-11-16T12%3A11%3A33Z
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyInstanceEventStartTime is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001d4</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyInstanceEventWindow

Modifies the specified event window.

```http
POST / HTTP/1.1

Action=ModifyInstanceEventWindow
&Version=2016-11-15
&InstanceEventWindowId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceEventWindowId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001d5</RequestID>
</Response>
```

`400` `InvalidInstanceEventWindowId.Malformed`: the placeholder values failed the operation's input validation.

### ModifyInstanceMaintenanceOptions

Modifies the recovery behavior of your instance to disable simplified automatic recovery or set the recovery behavior to default.

```http
POST / HTTP/1.1

Action=ModifyInstanceMaintenanceOptions
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyInstanceMaintenanceOptions is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001d6</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyInstanceMetadataDefaults

Modifies the default instance metadata service (IMDS) settings at the account level in the specified Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=ModifyInstanceMetadataDefaults
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must name a setting to modify: httpTokens, httpPutResponseHopLimit, httpEndpoint, instanceMetadataTags or httpTokensEnforced</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001d7</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ModifyInstanceMetadataOptions

Modify the instance metadata parameters on a running or stopped instance.

```http
POST / HTTP/1.1

Action=ModifyInstanceMetadataOptions
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001d8</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### ModifyInstanceNetworkPerformanceOptions

Change the configuration of the network performance options for an existing instance.

```http
POST / HTTP/1.1

Action=ModifyInstanceNetworkPerformanceOptions
&Version=2016-11-15
&InstanceId=example
&BandwidthWeighting=default
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyInstanceNetworkPerformanceOptions is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001d9</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyInstancePlacement

Modifies the placement attributes for a specified instance.

```http
POST / HTTP/1.1

Action=ModifyInstancePlacement
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyInstancePlacement is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001da</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyIpam

Modify the configurations of an IPAM.

```http
POST / HTTP/1.1

Action=ModifyIpam
&Version=2016-11-15
&IpamId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001db</RequestID>
</Response>
```

`400` `InvalidIpamId.Malformed`: the placeholder values failed the operation's input validation.

### ModifyIpamPolicyAllocationRules

Modifies the allocation rules in an IPAM policy.

```http
POST / HTTP/1.1

Action=ModifyIpamPolicyAllocationRules
&Version=2016-11-15
&IpamPolicyId=example
&Locale=example
&ResourceType=alb
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyIpamPolicyAllocationRules is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001dc</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyIpamPool

Modify the configurations of an IPAM pool.

```http
POST / HTTP/1.1

Action=ModifyIpamPool
&Version=2016-11-15
&IpamPoolId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyIpamPool is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001dd</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyIpamPoolAllocation

Modifies the description of an IPAM pool allocation.

```http
POST / HTTP/1.1

Action=ModifyIpamPoolAllocation
&Version=2016-11-15
&IpamPoolAllocationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyIpamPoolAllocation is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001de</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyIpamPrefixListResolver

Modifies an IPAM prefix list resolver.

```http
POST / HTTP/1.1

Action=ModifyIpamPrefixListResolver
&Version=2016-11-15
&IpamPrefixListResolverId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyIpamPrefixListResolver is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001df</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyIpamPrefixListResolverTarget

Modifies an IPAM prefix list resolver target.

```http
POST / HTTP/1.1

Action=ModifyIpamPrefixListResolverTarget
&Version=2016-11-15
&IpamPrefixListResolverTargetId=example
&ClientToken=0b42dcc2-3ffd-4f0c-b1ad-e3a08a0376d4
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyIpamPrefixListResolverTarget is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001e0</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyIpamResourceCidr

Modify a resource CIDR.

```http
POST / HTTP/1.1

Action=ModifyIpamResourceCidr
&Version=2016-11-15
&ResourceId=example
&ResourceCidr=example
&ResourceRegion=example
&CurrentIpamScopeId=example
&Monitored=false
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyIpamResourceCidr is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001e1</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyIpamResourceDiscovery

Modifies a resource discovery.

```http
POST / HTTP/1.1

Action=ModifyIpamResourceDiscovery
&Version=2016-11-15
&IpamResourceDiscoveryId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyIpamResourceDiscovery is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001e2</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyIpamRoutingPolicyRegistration

Modifies an existing routing policy registration.

```http
POST / HTTP/1.1

Action=ModifyIpamRoutingPolicyRegistration
&Version=2016-11-15
&IpamInternetRegistryAssociationId=example
&Cidr=example
&Asn.1=example
&ClientToken=03e59d78-3c6f-44e4-8a1a-88ea1d7c908b
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter cidr is invalid. This is not a valid CIDR block.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001e3</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyIpamScope

Modify an IPAM scope.

```http
POST / HTTP/1.1

Action=ModifyIpamScope
&Version=2016-11-15
&IpamScopeId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyIpamScope is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001e4</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyLaunchTemplate

Modifies a launch template.

```http
POST / HTTP/1.1

Action=ModifyLaunchTemplate
&Version=2016-11-15
&ClientToken=ac685b96-1653-4d2f-9e21-090b1b80864c
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either the parameter LaunchTemplateId or the parameter LaunchTemplateName</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001e5</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ModifyLocalGatewayRoute

Modifies the specified local gateway route.

```http
POST / HTTP/1.1

Action=ModifyLocalGatewayRoute
&Version=2016-11-15
&LocalGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayRouteTableID.Malformed</Code>
      <Message>The local-gateway-route-table ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001e6</RequestID>
</Response>
```

`400` `InvalidLocalGatewayRouteTableID.Malformed`: the placeholder values failed the operation's input validation.

### ModifyManagedPrefixList

Modifies the specified managed prefix list.

```http
POST / HTTP/1.1

Action=ModifyManagedPrefixList
&Version=2016-11-15
&PrefixListId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidPrefixListId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001e7</RequestID>
</Response>
```

`400` `InvalidPrefixListId.Malformed`: the placeholder values failed the operation's input validation.

### ModifyManagedResourceVisibility

Modifies the managed resource visibility configuration for the account.

```http
POST / HTTP/1.1

Action=ModifyManagedResourceVisibility
&Version=2016-11-15
&DefaultVisibility=hidden
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<ModifyManagedResourceVisibilityResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <visibility xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <defaultVisibility>hidden</defaultVisibility>
  </visibility>
  <requestId>38ca0512-333f-4bb8-bf4f-d2b128e8133b</requestId>
</ModifyManagedResourceVisibilityResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ModifyNetworkInterfaceAttribute

Modifies the specified network interface attribute.

```http
POST / HTTP/1.1

Action=ModifyNetworkInterfaceAttribute
&Version=2016-11-15
&NetworkInterfaceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must name an attribute to modify: description | sourceDestCheck | groupSet | attachment | associatePublicIpAddress | enablePrimaryIpv6 | connectionTrackingSpecification | associatedSubnetIds | enaSrdSpecification</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001e8</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ModifyPrivateDnsNameOptions

Modifies the options for instance hostnames for the specified instance.

```http
POST / HTTP/1.1

Action=ModifyPrivateDnsNameOptions
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyPrivateDnsNameOptions is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001e9</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyPublicIpDnsNameOptions

Modify public hostname options for a network interface.

```http
POST / HTTP/1.1

Action=ModifyPublicIpDnsNameOptions
&Version=2016-11-15
&NetworkInterfaceId=example
&HostnameType=public-dual-stack-dns-name
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyPublicIpDnsNameOptions is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ea</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyReservedInstances

Modifies the configuration of your Reserved Instances, such as the Availability Zone, instance count, or instance type.

```http
POST / HTTP/1.1

Action=ModifyReservedInstances
&Version=2016-11-15
&ReservedInstancesId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'targetConfigurations' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001eb</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### ModifyRouteServer

Modifies the configuration of an existing route server.

```http
POST / HTTP/1.1

Action=ModifyRouteServer
&Version=2016-11-15
&RouteServerId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter persistRoutes, persistRoutesDuration or snsNotificationsEnabled</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ec</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ModifySecurityGroupRules

Modifies the rules of a security group.

```http
POST / HTTP/1.1

Action=ModifySecurityGroupRules
&Version=2016-11-15
&GroupId=example
&SecurityGroupRule.1.SecurityGroupRuleId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidGroupId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ed</RequestID>
</Response>
```

`400` `InvalidGroupId.Malformed`: the placeholder values failed the operation's input validation.

### ModifySnapshotAttribute

Adds or removes permission settings for the specified snapshot.

```http
POST / HTTP/1.1

Action=ModifySnapshotAttribute
&Version=2016-11-15
&SnapshotId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifySnapshotAttribute is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ee</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifySnapshotTier

Archives an Amazon EBS snapshot.

```http
POST / HTTP/1.1

Action=ModifySnapshotTier
&Version=2016-11-15
&SnapshotId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifySnapshotTier is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ef</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifySpotFleetRequest

Modifies the specified Spot Fleet request.

```http
POST / HTTP/1.1

Action=ModifySpotFleetRequest
&Version=2016-11-15
&SpotFleetRequestId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifySpotFleetRequest is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001f0</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifySubnetAttribute

Modifies a subnet attribute.

```http
POST / HTTP/1.1

Action=ModifySubnetAttribute
&Version=2016-11-15
&SubnetId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must name an attribute to modify: AssignIpv6AddressOnCreation, MapPublicIpOnLaunch, EnableDns64, EnableResourceNameDnsARecordOnLaunch, EnableResourceNameDnsAAAARecordOnLaunch, PrivateDnsHostnameTypeOnLaunch, MapCustomerOwnedIpOnLaunch, EnableLniAtDeviceIndex, DisableLniAtDeviceIndex</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001f1</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ModifyTrafficMirrorFilterNetworkServices

Allows or restricts mirroring network services.

```http
POST / HTTP/1.1

Action=ModifyTrafficMirrorFilterNetworkServices
&Version=2016-11-15
&TrafficMirrorFilterId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyTrafficMirrorFilterNetworkServices is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001f2</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyTrafficMirrorFilterRule

Modifies the specified Traffic Mirror rule.

```http
POST / HTTP/1.1

Action=ModifyTrafficMirrorFilterRule
&Version=2016-11-15
&TrafficMirrorFilterRuleId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyTrafficMirrorFilterRule is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001f3</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyTrafficMirrorSession

Modifies a Traffic Mirror session.

```http
POST / HTTP/1.1

Action=ModifyTrafficMirrorSession
&Version=2016-11-15
&TrafficMirrorSessionId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyTrafficMirrorSession is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001f4</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyTransitGateway

Modifies the specified transit gateway.

```http
POST / HTTP/1.1

Action=ModifyTransitGateway
&Version=2016-11-15
&TransitGatewayId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayID.NotFound</Code>
      <Message>Transit Gateway example was deleted or does not exist.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001f5</RequestID>
</Response>
```

`400` `InvalidTransitGatewayID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyTransitGatewayMeteringPolicy

Modifies a transit gateway metering policy.

```http
POST / HTTP/1.1

Action=ModifyTransitGatewayMeteringPolicy
&Version=2016-11-15
&TransitGatewayMeteringPolicyId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyTransitGatewayMeteringPolicy is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001f6</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyTransitGatewayPolicyTableEntry

Modifies the specified transit gateway policy table entry.

```http
POST / HTTP/1.1

Action=ModifyTransitGatewayPolicyTableEntry
&Version=2016-11-15
&TransitGatewayPolicyTableId=example
&PolicyRuleNumber=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayPolicyTableId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001f7</RequestID>
</Response>
```

`400` `InvalidTransitGatewayPolicyTableId.Malformed`: the placeholder values failed the operation's input validation.

### ModifyTransitGatewayPrefixListReference

Modifies a reference (route) to a prefix list in a specified transit gateway route table.

```http
POST / HTTP/1.1

Action=ModifyTransitGatewayPrefixListReference
&Version=2016-11-15
&TransitGatewayRouteTableId=example
&PrefixListId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyTransitGatewayPrefixListReference is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001f8</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyTransitGatewayVpcAttachment

Modifies the specified VPC attachment.

```http
POST / HTTP/1.1

Action=ModifyTransitGatewayVpcAttachment
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyTransitGatewayVpcAttachment is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001f9</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVerifiedAccessEndpoint

Modifies the configuration of the specified Amazon Web Services Verified Access endpoint.

```http
POST / HTTP/1.1

Action=ModifyVerifiedAccessEndpoint
&Version=2016-11-15
&VerifiedAccessEndpointId=example
&ClientToken=5c96b413-55dd-4104-b5ed-32e7f9cf191a
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVerifiedAccessEndpoint is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001fa</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVerifiedAccessEndpointPolicy

Modifies the specified Amazon Web Services Verified Access endpoint policy.

```http
POST / HTTP/1.1

Action=ModifyVerifiedAccessEndpointPolicy
&Version=2016-11-15
&VerifiedAccessEndpointId=example
&ClientToken=2e2c004c-3fc6-4251-af7c-f144ca9b7278
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVerifiedAccessEndpointPolicy is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001fb</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVerifiedAccessGroup

Modifies the specified Amazon Web Services Verified Access group configuration.

```http
POST / HTTP/1.1

Action=ModifyVerifiedAccessGroup
&Version=2016-11-15
&VerifiedAccessGroupId=example
&ClientToken=49ac3c1f-d185-416f-8b63-da1e5f138b57
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVerifiedAccessGroup is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001fc</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVerifiedAccessGroupPolicy

Modifies the specified Amazon Web Services Verified Access group policy.

```http
POST / HTTP/1.1

Action=ModifyVerifiedAccessGroupPolicy
&Version=2016-11-15
&VerifiedAccessGroupId=example
&ClientToken=efdac472-8333-4685-a9a2-3f621b00cf3f
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVerifiedAccessGroupPolicy is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001fd</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVerifiedAccessInstance

Modifies the configuration of the specified Amazon Web Services Verified Access instance.

```http
POST / HTTP/1.1

Action=ModifyVerifiedAccessInstance
&Version=2016-11-15
&VerifiedAccessInstanceId=example
&ClientToken=cd573629-6ed8-4020-863f-3a0bd3612230
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVerifiedAccessInstanceId.NotFound</Code>
      <Message>The verified-access-instance ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001fe</RequestID>
</Response>
```

`400` `InvalidVerifiedAccessInstanceId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyVerifiedAccessInstanceLoggingConfiguration

Modifies the logging configuration for the specified Amazon Web Services Verified Access instance.

```http
POST / HTTP/1.1

Action=ModifyVerifiedAccessInstanceLoggingConfiguration
&Version=2016-11-15
&VerifiedAccessInstanceId=example
&ClientToken=e05d5761-ff86-406d-844f-2d70d246bcdb
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'accessLogs' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-0000000001ff</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### ModifyVerifiedAccessTrustProvider

Modifies the configuration of the specified Amazon Web Services Verified Access trust provider.

```http
POST / HTTP/1.1

Action=ModifyVerifiedAccessTrustProvider
&Version=2016-11-15
&VerifiedAccessTrustProviderId=example
&ClientToken=f083dfd0-9663-43c6-b798-6a797d06e60a
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVerifiedAccessTrustProvider is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000200</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVolume

You can modify several parameters of an existing EBS volume, including volume size, volume type, and IOPS capacity.

```http
POST / HTTP/1.1

Action=ModifyVolume
&Version=2016-11-15
&VolumeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVolumeID.Malformed</Code>
      <Message>The volume ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000201</RequestID>
</Response>
```

`400` `InvalidVolumeID.Malformed`: the placeholder values failed the operation's input validation.

### ModifyVolumeAttribute

Modifies a volume attribute.

```http
POST / HTTP/1.1

Action=ModifyVolumeAttribute
&Version=2016-11-15
&VolumeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must name an attribute to modify: autoEnableIO</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000202</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ModifyVpcAttribute

Modifies the specified attribute of the specified VPC.

```http
POST / HTTP/1.1

Action=ModifyVpcAttribute
&Version=2016-11-15
&VpcId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must name an attribute to modify: enableDnsSupport, enableDnsHostnames or enableNetworkAddressUsageMetrics</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000203</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ModifyVpcBlockPublicAccessExclusion

Modify VPC Block Public Access (BPA) exclusions.

```http
POST / HTTP/1.1

Action=ModifyVpcBlockPublicAccessExclusion
&Version=2016-11-15
&ExclusionId=example
&InternetGatewayExclusionMode=allow-bidirectional
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcBlockPublicAccessExclusionId.NotFound</Code>
      <Message>The VPC Block Public Access exclusion ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000204</RequestID>
</Response>
```

`400` `InvalidVpcBlockPublicAccessExclusionId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyVpcBlockPublicAccessOptions

Modify VPC Block Public Access (BPA) options.

```http
POST / HTTP/1.1

Action=ModifyVpcBlockPublicAccessOptions
&Version=2016-11-15
&InternetGatewayBlockMode=off
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<ModifyVpcBlockPublicAccessOptionsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <vpcBlockPublicAccessOptions xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
    <awsAccountId>000000000000</awsAccountId>
    <awsRegion>us-east-1</awsRegion>
    <state>update-in-progress</state>
    <internetGatewayBlockMode>off</internetGatewayBlockMode>
    <lastUpdateTimestamp>2026-11-16T12:11:33.95097Z</lastUpdateTimestamp>
    <managedBy>account</managedBy>
    <exclusionsAllowed>allowed</exclusionsAllowed>
  </vpcBlockPublicAccessOptions>
… (2 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ModifyVpcEncryptionControl

Modifies the encryption control configuration for a VPC.

```http
POST / HTTP/1.1

Action=ModifyVpcEncryptionControl
&Version=2016-11-15
&VpcEncryptionControlId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcEncryptionControlId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000205</RequestID>
</Response>
```

`400` `InvalidVpcEncryptionControlId.Malformed`: the placeholder values failed the operation's input validation.

### ModifyVpcEndpoint

Modifies attributes of a specified VPC endpoint.

```http
POST / HTTP/1.1

Action=ModifyVpcEndpoint
&Version=2016-11-15
&VpcEndpointId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcEndpointId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000206</RequestID>
</Response>
```

`400` `InvalidVpcEndpointId.Malformed`: the placeholder values failed the operation's input validation.

### ModifyVpcEndpointConnectionNotification

Modifies a connection notification for VPC endpoint or VPC endpoint service.

```http
POST / HTTP/1.1

Action=ModifyVpcEndpointConnectionNotification
&Version=2016-11-15
&ConnectionNotificationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVpcEndpointConnectionNotification is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000207</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVpcEndpointPayerResponsibility

Modifies the billing account for VPC endpoint usage/charges.

```http
POST / HTTP/1.1

Action=ModifyVpcEndpointPayerResponsibility
&Version=2016-11-15
&VpcEndpointId=example
&PayerResponsibility=vpc-endpoint-account
&Scope=vpc-endpoint-charges
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVpcEndpointPayerResponsibility is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000208</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVpcEndpointServiceConfiguration

Modifies the attributes of the specified VPC endpoint service configuration.

```http
POST / HTTP/1.1

Action=ModifyVpcEndpointServiceConfiguration
&Version=2016-11-15
&ServiceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVpcEndpointServiceConfiguration is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000209</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVpcEndpointServicePayerResponsibility

Modifies the payer responsibility for your VPC endpoint service.

```http
POST / HTTP/1.1

Action=ModifyVpcEndpointServicePayerResponsibility
&Version=2016-11-15
&ServiceId=example
&PayerResponsibility=ServiceOwner
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVpcEndpointServicePayerResponsibility is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000020a</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVpcEndpointServicePermissions

Modifies the permissions for your VPC endpoint service.

```http
POST / HTTP/1.1

Action=ModifyVpcEndpointServicePermissions
&Version=2016-11-15
&ServiceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVpcEndpointServicePermissions is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000020b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVpcPeeringConnectionOptions

Modifies the VPC peering connection options on one side of a VPC peering connection.

```http
POST / HTTP/1.1

Action=ModifyVpcPeeringConnectionOptions
&Version=2016-11-15
&VpcPeeringConnectionId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVpcPeeringConnectionOptions is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000020c</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVpcTenancy

Modifies the instance tenancy attribute of the specified VPC.

```http
POST / HTTP/1.1

Action=ModifyVpcTenancy
&Version=2016-11-15
&VpcId=example
&InstanceTenancy=default
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVpcID.NotFound</Code>
      <Message>The vpc ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000020d</RequestID>
</Response>
```

`400` `InvalidVpcID.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyVpnConnection

Modifies the customer gateway or the target gateway of an Amazon Web Services Site-to-Site VPN connection.

```http
POST / HTTP/1.1

Action=ModifyVpnConnection
&Version=2016-11-15
&VpnConnectionId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVpnConnection is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000020e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVpnConnectionOptions

Modifies the connection options for your Site-to-Site VPN connection.

```http
POST / HTTP/1.1

Action=ModifyVpnConnectionOptions
&Version=2016-11-15
&VpnConnectionId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVpnConnectionOptions is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000020f</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVpnTunnelCertificate

Modifies the VPN tunnel endpoint certificate.

```http
POST / HTTP/1.1

Action=ModifyVpnTunnelCertificate
&Version=2016-11-15
&VpnConnectionId=example
&VpnTunnelOutsideIpAddress=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ModifyVpnTunnelCertificate is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000210</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyVpnTunnelOptions

Modifies the options for a VPN tunnel in an Amazon Web Services Site-to-Site VPN connection.

```http
POST / HTTP/1.1

Action=ModifyVpnTunnelOptions
&Version=2016-11-15
&VpnConnectionId=example
&VpnTunnelOutsideIpAddress=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'tunnelOptions' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000211</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### MonitorInstances

Enables detailed monitoring for a running instance.

```http
POST / HTTP/1.1

Action=MonitorInstances
&Version=2016-11-15
&InstanceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter instanceId is invalid. Expected: 'i-...'.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000212</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### MoveAddressToVpc

This action is deprecated.

```http
POST / HTTP/1.1

Action=MoveAddressToVpc
&Version=2016-11-15
&PublicIp=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation MoveAddressToVpc is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000213</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### MoveByoipCidrToIpam

Move a BYOIPv4 CIDR to IPAM from a public IPv4 pool.

```http
POST / HTTP/1.1

Action=MoveByoipCidrToIpam
&Version=2016-11-15
&Cidr=example
&IpamPoolId=example
&IpamPoolOwner=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>The CIDR (example) is not valid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000214</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### MoveCapacityReservationInstances

Move available capacity from a source Capacity Reservation to a destination Capacity Reservation.

```http
POST / HTTP/1.1

Action=MoveCapacityReservationInstances
&Version=2016-11-15
&ClientToken=b25dbcf3-36f8-472d-ae6e-c14c02094de9
&SourceCapacityReservationId=example
&DestinationCapacityReservationId=example
&InstanceCount=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000215</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### ProvisionByoipCidr

Provisions an IPv4 or IPv6 address range for use with your Amazon Web Services resources through bring your own IP addresses (BYOIP) and creates a corresponding address pool.

```http
POST / HTTP/1.1

Action=ProvisionByoipCidr
&Version=2016-11-15
&Cidr=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>The CIDR (example) is not valid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000216</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ProvisionIpamByoasn

Provisions your Autonomous System Number (ASN) for use in your Amazon Web Services account.

```http
POST / HTTP/1.1

Action=ProvisionIpamByoasn
&Version=2016-11-15
&IpamId=example
&Asn=example
&AsnAuthorizationContext.Message=example
&AsnAuthorizationContext.Signature=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ProvisionIpamByoasn is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000217</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ProvisionIpamPoolCidr

Provision a CIDR to an IPAM pool.

```http
POST / HTTP/1.1

Action=ProvisionIpamPoolCidr
&Version=2016-11-15
&IpamPoolId=example
&ClientToken=a482e727-203d-4be7-994d-72ba3f320c87
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidIpamPoolId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000218</RequestID>
</Response>
```

`400` `InvalidIpamPoolId.Malformed`: the placeholder values failed the operation's input validation.

### ProvisionPublicIpv4PoolCidr

Provision a CIDR to a public IPv4 pool.

```http
POST / HTTP/1.1

Action=ProvisionPublicIpv4PoolCidr
&Version=2016-11-15
&IpamPoolId=example
&PoolId=example
&NetmaskLength=1
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ProvisionPublicIpv4PoolCidr is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000219</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### PurchaseCapacityBlock

Purchase the Capacity Block for use with your account.

```http
POST / HTTP/1.1

Action=PurchaseCapacityBlock
&Version=2016-11-15
&CapacityBlockOfferingId=example
&InstancePlatform=Linux%2FUNIX
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityBlockOfferingIdMalformed</Code>
      <Message>Capacity Block Offering Id 'example' is malformed.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000021a</RequestID>
</Response>
```

`400` `InvalidCapacityBlockOfferingIdMalformed`: the placeholder values failed the operation's input validation.

### PurchaseCapacityBlockExtension

Purchase the Capacity Block extension for use with your account.

```http
POST / HTTP/1.1

Action=PurchaseCapacityBlockExtension
&Version=2016-11-15
&CapacityBlockExtensionOfferingId=example
&CapacityReservationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000021b</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### PurchaseHostReservation

Purchase a reservation with configurations that match those of your Dedicated Host.

```http
POST / HTTP/1.1

Action=PurchaseHostReservation
&Version=2016-11-15
&HostIdSet.1=example
&OfferingId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidHostID.Malformed</Code>
      <Message>The dedicated-host ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000021c</RequestID>
</Response>
```

`400` `InvalidHostID.Malformed`: the placeholder values failed the operation's input validation.

### PurchaseReservedInstancesOffering

Purchases a Reserved Instance for use with your account.

```http
POST / HTTP/1.1

Action=PurchaseReservedInstancesOffering
&Version=2016-11-15
&InstanceCount=1
&ReservedInstancesOfferingId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation PurchaseReservedInstancesOffering is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000021d</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### PurchaseScheduledInstances

You can no longer purchase Scheduled Instances.

```http
POST / HTTP/1.1

Action=PurchaseScheduledInstances
&Version=2016-11-15
&ClientToken=8609f2b9-80d5-4b4a-b8b5-99a7859ef3b8
&PurchaseRequest.1.InstanceCount=1
&PurchaseRequest.1.PurchaseToken=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation PurchaseScheduledInstances is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000021e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RebootInstances

Requests a reboot of the specified instances.

```http
POST / HTTP/1.1

Action=RebootInstances
&Version=2016-11-15
&InstanceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000021f</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### RegisterImage

Registers an AMI.

```http
POST / HTTP/1.1

Action=RegisterImage
&Version=2016-11-15
&Name=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterCombination</Code>
      <Message>Either imageLocation or rootDeviceName must be specified</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000220</RequestID>
</Response>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RegisterInstanceEventNotificationAttributes

Registers a set of tag keys to include in scheduled event notifications for your resources.

```http
POST / HTTP/1.1

Action=RegisterInstanceEventNotificationAttributes
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'instanceTagAttribute' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000221</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### RegisterTransitGatewayMulticastGroupMembers

Registers members (network interfaces) with the transit gateway multicast group.

```http
POST / HTTP/1.1

Action=RegisterTransitGatewayMulticastGroupMembers
&Version=2016-11-15
&TransitGatewayMulticastDomainId=example
&NetworkInterfaceIds.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RegisterTransitGatewayMulticastGroupMembers is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000222</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RegisterTransitGatewayMulticastGroupSources

Registers sources (network interfaces) with the specified transit gateway multicast group.

```http
POST / HTTP/1.1

Action=RegisterTransitGatewayMulticastGroupSources
&Version=2016-11-15
&TransitGatewayMulticastDomainId=example
&NetworkInterfaceIds.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RegisterTransitGatewayMulticastGroupSources is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000223</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RejectCapacityReservationBillingOwnership

Rejects a request to assign billing of the available capacity of a shared Capacity Reservation to your account.

```http
POST / HTTP/1.1

Action=RejectCapacityReservationBillingOwnership
&Version=2016-11-15
&CapacityReservationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RejectCapacityReservationBillingOwnership is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000224</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RejectTransitGatewayClientVpnAttachment

Rejects a Transit Gateway attachment request for a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=RejectTransitGatewayClientVpnAttachment
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayAttachmentID.Malformed</Code>
      <Message>Invalid Transit Gateway Attachment id.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000225</RequestID>
</Response>
```

`400` `InvalidTransitGatewayAttachmentID.Malformed`: the placeholder values failed the operation's input validation.

### RejectTransitGatewayMulticastDomainAssociations

Rejects a request to associate cross-account subnets with a transit gateway multicast domain.

```http
POST / HTTP/1.1

Action=RejectTransitGatewayMulticastDomainAssociations
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RejectTransitGatewayMulticastDomainAssociations is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000226</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RejectTransitGatewayPeeringAttachment

Rejects a transit gateway peering attachment request.

```http
POST / HTTP/1.1

Action=RejectTransitGatewayPeeringAttachment
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RejectTransitGatewayPeeringAttachment is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000227</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RejectTransitGatewayVpcAttachment

Rejects a request to attach a VPC to a transit gateway.

```http
POST / HTTP/1.1

Action=RejectTransitGatewayVpcAttachment
&Version=2016-11-15
&TransitGatewayAttachmentId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidTransitGatewayAttachmentID.Malformed</Code>
      <Message>Invalid Transit Gateway Attachment id.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000228</RequestID>
</Response>
```

`400` `InvalidTransitGatewayAttachmentID.Malformed`: the placeholder values failed the operation's input validation.

### RejectVpcEndpointConnections

Rejects VPC endpoint connection requests to your VPC endpoint service.

```http
POST / HTTP/1.1

Action=RejectVpcEndpointConnections
&Version=2016-11-15
&ServiceId=example
&VpcEndpointId.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RejectVpcEndpointConnections is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000229</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RejectVpcPeeringConnection

Rejects a VPC peering connection request.

```http
POST / HTTP/1.1

Action=RejectVpcPeeringConnection
&Version=2016-11-15
&VpcPeeringConnectionId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RejectVpcPeeringConnection is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000022a</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ReleaseAddress

Releases the specified Elastic IP address. [Default VPC] Releasing an Elastic IP address automatically disassociates it from any instance that it's associated with.

```http
POST / HTTP/1.1

Action=ReleaseAddress
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter allocationId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000022b</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ReleaseHosts

When you no longer want to use an On-Demand Dedicated Host it can be released.

```http
POST / HTTP/1.1

Action=ReleaseHosts
&Version=2016-11-15
&HostId.1=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<ReleaseHostsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <successful/>
  <unsuccessful>
    <item xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
      <error>
        <code>InvalidHostID.Malformed</code>
        <message>The specified Dedicated host IDs ['example'] are not valid.</message>
      </error>
      <resourceId>example</resourceId>
    </item>
… (3 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ReleaseIpamPoolAllocation

Release an allocation within an IPAM pool.

```http
POST / HTTP/1.1

Action=ReleaseIpamPoolAllocation
&Version=2016-11-15
&IpamPoolId=example
&Cidr=example
&IpamPoolAllocationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ReleaseIpamPoolAllocation is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000022c</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ReplaceIamInstanceProfileAssociation

Replaces an IAM instance profile for the specified running instance.

```http
POST / HTTP/1.1

Action=ReplaceIamInstanceProfileAssociation
&Version=2016-11-15
&AssociationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'iamInstanceProfile' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000022d</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### ReplaceImageCriteriaInAllowedImagesSettings

Sets or replaces the criteria for Allowed AMIs.

```http
POST / HTTP/1.1

Action=ReplaceImageCriteriaInAllowedImagesSettings
&Version=2016-11-15
```

```http
HTTP/1.1 200 OK
content-type: text/xml;charset=UTF-8

<ReplaceImageCriteriaInAllowedImagesSettingsResponse xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <return>true</return>
  <requestId>df72d937-c3b5-4721-a0e2-a54fb66ee7e2</requestId>
</ReplaceImageCriteriaInAllowedImagesSettingsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ReplaceImageInstanceTypeSpecification

Replaces or removes the instance type specification for an AMI.

```http
POST / HTTP/1.1

Action=ReplaceImageInstanceTypeSpecification
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ReplaceImageInstanceTypeSpecification is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000022e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ReplaceNetworkAclAssociation

Changes which network ACL a subnet is associated with.

```http
POST / HTTP/1.1

Action=ReplaceNetworkAclAssociation
&Version=2016-11-15
&AssociationId=example
&NetworkAclId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkAclID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000022f</RequestID>
</Response>
```

`400` `InvalidNetworkAclID.Malformed`: the placeholder values failed the operation's input validation.

### ReplaceNetworkAclEntry

Replaces an entry (rule) in a network ACL.

```http
POST / HTTP/1.1

Action=ReplaceNetworkAclEntry
&Version=2016-11-15
&NetworkAclId=example
&RuleNumber=1
&Protocol=example
&RuleAction=allow
&Egress=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkAclID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000230</RequestID>
</Response>
```

`400` `InvalidNetworkAclID.Malformed`: the placeholder values failed the operation's input validation.

### ReplaceRoute

Replaces an existing route within a route table in a VPC.

```http
POST / HTTP/1.1

Action=ReplaceRoute
&Version=2016-11-15
&RouteTableId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ReplaceRoute is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000231</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ReplaceRouteTableAssociation

Changes the route table associated with a given subnet, internet gateway, or virtual private gateway in a VPC.

```http
POST / HTTP/1.1

Action=ReplaceRouteTableAssociation
&Version=2016-11-15
&AssociationId=example
&RouteTableId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ReplaceRouteTableAssociation is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000232</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ReplaceTransitGatewayRoute

Replaces the specified route in the specified transit gateway route table.

```http
POST / HTTP/1.1

Action=ReplaceTransitGatewayRoute
&Version=2016-11-15
&DestinationCidrBlock=example
&TransitGatewayRouteTableId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ReplaceTransitGatewayRoute is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000233</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ReplaceVpnTunnel

Trigger replacement of specified VPN tunnel.

```http
POST / HTTP/1.1

Action=ReplaceVpnTunnel
&Version=2016-11-15
&VpnConnectionId=example
&VpnTunnelOutsideIpAddress=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ReplaceVpnTunnel is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000234</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ReportInstanceStatus

Submits feedback about the status of an instance.

```http
POST / HTTP/1.1

Action=ReportInstanceStatus
&Version=2016-11-15
&InstanceId.1=example
&Status=ok
&ReasonCode.1=instance-stuck-in-state
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>The instance ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000235</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### RequestSpotFleet

Creates a Spot Fleet request.

```http
POST / HTTP/1.1

Action=RequestSpotFleet
&Version=2016-11-15
&SpotFleetRequestConfig.IamFleetRole=example
&SpotFleetRequestConfig.TargetCapacity=1
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value 'example' for iamFleetRole. It must be the Amazon Resource Name of an IAM role</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000236</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### RequestSpotInstances

Creates a Spot Instance request.

```http
POST / HTTP/1.1

Action=RequestSpotInstances
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter launchSpecification.imageId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000237</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ResetAddressAttribute

Resets the attribute of the specified IP address.

```http
POST / HTTP/1.1

Action=ResetAddressAttribute
&Version=2016-11-15
&AllocationId=example
&Attribute=domain-name
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAllocationID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000238</RequestID>
</Response>
```

`400` `InvalidAllocationID.Malformed`: the placeholder values failed the operation's input validation.

### ResetEbsDefaultKmsKeyId

Resets the default KMS key for EBS encryption for your account in this Region to the Amazon Web Services managed KMS key for EBS.

```http
POST / HTTP/1.1

Action=ResetEbsDefaultKmsKeyId
&Version=2016-11-15
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ResetEbsDefaultKmsKeyId is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000239</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ResetFpgaImageAttribute

Resets the specified attribute of the specified Amazon FPGA Image (AFI) to its default value.

```http
POST / HTTP/1.1

Action=ResetFpgaImageAttribute
&Version=2016-11-15
&FpgaImageId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidFpgaImageID.Malformed</Code>
      <Message>Image ID 'example' is invalid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000023a</RequestID>
</Response>
```

`400` `InvalidFpgaImageID.Malformed`: the placeholder values failed the operation's input validation.

### ResetImageAttribute

Resets an attribute of an AMI to its default value.

```http
POST / HTTP/1.1

Action=ResetImageAttribute
&Version=2016-11-15
&Attribute=launchPermission
&ImageId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ResetImageAttribute is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000023b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ResetInstanceAttribute

Resets an attribute of an instance to its default value.

```http
POST / HTTP/1.1

Action=ResetInstanceAttribute
&Version=2016-11-15
&InstanceId=example
&Attribute=instanceType
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (instanceType) for parameter attribute is invalid. Unknown attribute.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000023c</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ResetNetworkInterfaceAttribute

Resets a network interface attribute.

```http
POST / HTTP/1.1

Action=ResetNetworkInterfaceAttribute
&Version=2016-11-15
&NetworkInterfaceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter sourceDestCheck</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000023d</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ResetSnapshotAttribute

Resets permission settings for the specified snapshot.

```http
POST / HTTP/1.1

Action=ResetSnapshotAttribute
&Version=2016-11-15
&Attribute=productCodes
&SnapshotId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation ResetSnapshotAttribute is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000023e</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RestoreAddressToClassic

This action is deprecated.

```http
POST / HTTP/1.1

Action=RestoreAddressToClassic
&Version=2016-11-15
&PublicIp=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RestoreAddressToClassic is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000023f</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RestoreImageFromRecycleBin

Restores an AMI from the Recycle Bin.

```http
POST / HTTP/1.1

Action=RestoreImageFromRecycleBin
&Version=2016-11-15
&ImageId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidAMIID.Malformed</Code>
      <Message>The image ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000240</RequestID>
</Response>
```

`400` `InvalidAMIID.Malformed`: the placeholder values failed the operation's input validation.

### RestoreManagedPrefixListVersion

Restores the entries from a previous version of a managed prefix list to a new version of the prefix list.

```http
POST / HTTP/1.1

Action=RestoreManagedPrefixListVersion
&Version=2016-11-15
&PrefixListId=example
&PreviousVersion=1
&CurrentVersion=1
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RestoreManagedPrefixListVersion is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000241</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RestoreSnapshotFromRecycleBin

Restores a snapshot from the Recycle Bin.

```http
POST / HTTP/1.1

Action=RestoreSnapshotFromRecycleBin
&Version=2016-11-15
&SnapshotId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidSnapshotID.Malformed</Code>
      <Message>The snapshot ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000242</RequestID>
</Response>
```

`400` `InvalidSnapshotID.Malformed`: the placeholder values failed the operation's input validation.

### RestoreSnapshotTier

Restores an archived Amazon EBS snapshot for use temporarily or permanently, or modifies the restore period or restore type for a snapshot that was previously temporarily restored.

```http
POST / HTTP/1.1

Action=RestoreSnapshotTier
&Version=2016-11-15
&SnapshotId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RestoreSnapshotTier is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000243</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RestoreVolumeFromRecycleBin

Restores a volume from the Recycle Bin.

```http
POST / HTTP/1.1

Action=RestoreVolumeFromRecycleBin
&Version=2016-11-15
&VolumeId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidVolumeID.Malformed</Code>
      <Message>The volume ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000244</RequestID>
</Response>
```

`400` `InvalidVolumeID.Malformed`: the placeholder values failed the operation's input validation.

### RevokeClientVpnIngress

Removes an ingress authorization rule from a Client VPN endpoint.

```http
POST / HTTP/1.1

Action=RevokeClientVpnIngress
&Version=2016-11-15
&ClientVpnEndpointId=example
&TargetNetworkCidr=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RevokeClientVpnIngress is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000245</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RevokeSecurityGroupEgress

Removes the specified outbound (egress) rules from the specified security group.

```http
POST / HTTP/1.1

Action=RevokeSecurityGroupEgress
&Version=2016-11-15
&GroupId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidGroupId.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000246</RequestID>
</Response>
```

`400` `InvalidGroupId.Malformed`: the placeholder values failed the operation's input validation.

### RevokeSecurityGroupIngress

Removes the specified inbound (ingress) rules from a security group.

```http
POST / HTTP/1.1

Action=RevokeSecurityGroupIngress
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either the parameter GroupId or GroupName</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000247</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### RunInstances

Launches the specified number of instances using an AMI for which you have permissions.

```http
POST / HTTP/1.1

Action=RunInstances
&Version=2016-11-15
&MaxCount=1
&MinCount=1
&ClientToken=e522a2d8-6e02-47ca-a6f9-4fa83575be52
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter ImageId</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000248</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation. See [the walkthrough](#5-runinstances) for a successful call.

### RunScheduledInstances

Launches the specified Scheduled Instances.

```http
POST / HTTP/1.1

Action=RunScheduledInstances
&Version=2016-11-15
&ClientToken=482bd1e0-3e03-4079-989b-17460d59f325
&LaunchSpecification.ImageId=example
&ScheduledInstanceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation RunScheduledInstances is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000249</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### SearchLocalGatewayRoutes

Searches for routes in the specified local gateway route table.

```http
POST / HTTP/1.1

Action=SearchLocalGatewayRoutes
&Version=2016-11-15
&LocalGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidLocalGatewayRouteTableID.Malformed</Code>
      <Message>The local-gateway-route-table ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000024a</RequestID>
</Response>
```

`400` `InvalidLocalGatewayRouteTableID.Malformed`: the placeholder values failed the operation's input validation.

### SearchTransitGatewayMulticastGroups

Searches one or more transit gateway multicast groups and returns the group membership information.

```http
POST / HTTP/1.1

Action=SearchTransitGatewayMulticastGroups
&Version=2016-11-15
&TransitGatewayMulticastDomainId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation SearchTransitGatewayMulticastGroups is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000024b</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### SearchTransitGatewayRoutes

Searches for routes in the specified transit gateway route table.

```http
POST / HTTP/1.1

Action=SearchTransitGatewayRoutes
&Version=2016-11-15
&TransitGatewayRouteTableId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<?xml version="1.0" ?>
<Response>
  <Errors>
    <Error>
      <Code>MalformedInput</Code>
      <Message>1 validation error detected: Value null at 'filters' failed to satisfy constraint: Member must not be null</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000024c</RequestID>
</Response>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### SendDiagnosticInterrupt

Sends a diagnostic interrupt to the specified Amazon EC2 instance to trigger a kernel panic (on Linux instances), or a blue screen / stop error (on Windows instances).

```http
POST / HTTP/1.1

Action=SendDiagnosticInterrupt
&Version=2016-11-15
&InstanceId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000024d</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### StartDeclarativePoliciesReport

Generates an account status report.

```http
POST / HTTP/1.1

Action=StartDeclarativePoliciesReport
&Version=2016-11-15
&S3Bucket=example
&TargetId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Invalid value 'example' for targetId. A target is a root (r-ab12), an organizational unit (ou-ab12-cdef1234) or an account (123456789012)</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000024e</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### StartInstances

Starts an Amazon EBS-backed instance that you've previously stopped.

```http
POST / HTTP/1.1

Action=StartInstances
&Version=2016-11-15
&InstanceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000024f</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### StartNetworkInsightsAccessScopeAnalysis

Starts analyzing the specified Network Access Scope.

```http
POST / HTTP/1.1

Action=StartNetworkInsightsAccessScopeAnalysis
&Version=2016-11-15
&NetworkInsightsAccessScopeId=example
&ClientToken=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkInsightsAccessScopeId.NotFound</Code>
      <Message>example does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000250</RequestID>
</Response>
```

`400` `InvalidNetworkInsightsAccessScopeId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartNetworkInsightsAnalysis

Starts analyzing the specified path.

```http
POST / HTTP/1.1

Action=StartNetworkInsightsAnalysis
&Version=2016-11-15
&NetworkInsightsPathId=example
&ClientToken=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidNetworkInsightsPathId.NotFound</Code>
      <Message>The network insights path ID 'example' does not exist</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000251</RequestID>
</Response>
```

`400` `InvalidNetworkInsightsPathId.NotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartVpcEndpointServicePrivateDnsVerification

Initiates the verification process to prove that the service provider owns the private DNS name domain for the endpoint service.

```http
POST / HTTP/1.1

Action=StartVpcEndpointServicePrivateDnsVerification
&Version=2016-11-15
&ServiceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation StartVpcEndpointServicePrivateDnsVerification is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000252</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### StopInstances

Stops an Amazon EBS-backed instance.

```http
POST / HTTP/1.1

Action=StopInstances
&Version=2016-11-15
&InstanceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000253</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation.

### TerminateClientVpnConnections

Terminates active Client VPN endpoint connections.

```http
POST / HTTP/1.1

Action=TerminateClientVpnConnections
&Version=2016-11-15
&ClientVpnEndpointId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain the parameter connectionId or username</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000254</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### TerminateInstances

Terminates (deletes) the specified instances.

```http
POST / HTTP/1.1

Action=TerminateInstances
&Version=2016-11-15
&InstanceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidInstanceID.Malformed</Code>
      <Message>Invalid id: &quot;example&quot;</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000255</RequestID>
</Response>
```

`400` `InvalidInstanceID.Malformed`: the placeholder values failed the operation's input validation. See [the walkthrough](#7-terminateinstances) for a successful call.

### UnassignIpv6Addresses

Unassigns the specified IPv6 addresses or Prefix Delegation prefixes from a network interface.

```http
POST / HTTP/1.1

Action=UnassignIpv6Addresses
&Version=2016-11-15
&NetworkInterfaceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation UnassignIpv6Addresses is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000256</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UnassignPrivateIpAddresses

Unassigns the specified secondary private IP addresses or IPv4 Prefix Delegation prefixes from a network interface.

```http
POST / HTTP/1.1

Action=UnassignPrivateIpAddresses
&Version=2016-11-15
&NetworkInterfaceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation UnassignPrivateIpAddresses is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000257</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UnassignPrivateNatGatewayAddress

Unassigns secondary private IPv4 addresses from a private NAT gateway.

```http
POST / HTTP/1.1

Action=UnassignPrivateNatGatewayAddress
&Version=2016-11-15
&NatGatewayId=example
&PrivateIpAddress.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation UnassignPrivateNatGatewayAddress is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000258</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UnlockSnapshot

Unlocks a snapshot that is locked in governance mode or that is locked in compliance mode but still in the cooling-off period.

```http
POST / HTTP/1.1

Action=UnlockSnapshot
&Version=2016-11-15
&SnapshotId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation UnlockSnapshot is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000259</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UnmonitorInstances

Disables detailed monitoring for a running instance.

```http
POST / HTTP/1.1

Action=UnmonitorInstances
&Version=2016-11-15
&InstanceId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>Value (example) for parameter instanceId is invalid. Expected: 'i-...'.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000025a</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### UpdateCapacityManagerMonitoredTagKeys

Activates or deactivates tag keys for monitoring by EC2 Capacity Manager.

```http
POST / HTTP/1.1

Action=UpdateCapacityManagerMonitoredTagKeys
&Version=2016-11-15
&ClientToken=16b467bd-4454-4c24-8007-58d0d5297d73
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must specify either ActivateTagKeys or DeactivateTagKeys</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000025b</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### UpdateCapacityManagerOrganizationsAccess

Updates the Organizations access setting for EC2 Capacity Manager.

```http
POST / HTTP/1.1

Action=UpdateCapacityManagerOrganizationsAccess
&Version=2016-11-15
&OrganizationsAccess=false
&ClientToken=6b56463b-7bd2-48dc-aea5-aa6e34284247
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>NotImplemented</Code>
      <Message>operation UpdateCapacityManagerOrganizationsAccess is scaffolded but not implemented in this emulator</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000025c</RequestID>
</Response>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### UpdateInterruptibleCapacityReservationAllocation

Modifies the number of instances allocated to an interruptible reservation, allowing you to add more capacity or reclaim capacity to your source Capacity Reservation.

```http
POST / HTTP/1.1

Action=UpdateInterruptibleCapacityReservationAllocation
&Version=2016-11-15
&CapacityReservationId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidCapacityReservationId.Malformed</Code>
      <Message>Capacity Reservation ID 'example' is malformed</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000025d</RequestID>
</Response>
```

`400` `InvalidCapacityReservationId.Malformed`: the placeholder values failed the operation's input validation.

### UpdateSecurityGroupRuleDescriptionsEgress

Updates the description of an egress (outbound) security group rule.

```http
POST / HTTP/1.1

Action=UpdateSecurityGroupRuleDescriptionsEgress
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either IpPermissions or SecurityGroupRuleDescriptions</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000025e</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### UpdateSecurityGroupRuleDescriptionsIngress

Updates the description of an ingress (inbound) security group rule.

```http
POST / HTTP/1.1

Action=UpdateSecurityGroupRuleDescriptionsIngress
&Version=2016-11-15
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>MissingParameter</Code>
      <Message>The request must contain either IpPermissions or SecurityGroupRuleDescriptions</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-00000000025f</RequestID>
</Response>
```

`400` `MissingParameter`: the placeholder values failed the operation's input validation.

### ValidateSecurityGroupQuotasForInterface

Validates whether the specified security groups can be associated with a single network interface.

```http
POST / HTTP/1.1

Action=ValidateSecurityGroupQuotasForInterface
&Version=2016-11-15
&SecurityGroupId.1=example
```

```http
HTTP/1.1 403 Forbidden
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>UnauthorizedOperation</Code>
      <Message>Caller is not allowed to call this API.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000260</RequestID>
</Response>
```

`403` `UnauthorizedOperation`: the request was refused by authorization.

### WithdrawByoipCidr

Stops advertising an address range that is provisioned as an address pool.

```http
POST / HTTP/1.1

Action=WithdrawByoipCidr
&Version=2016-11-15
&Cidr=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml;charset=UTF-8

<Response xmlns="http://ec2.amazonaws.com/doc/2016-11-15">
  <Errors>
    <Error>
      <Code>InvalidParameterValue</Code>
      <Message>The CIDR (example) is not valid.</Message>
    </Error>
  </Errors>
  <RequestID>00000000-0000-0000-0000-000000000261</RequestID>
</Response>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/ec2/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with a form-encoded body: `Action`, `Version`, and input members as capitalised, numbered keys such as `TagSpecification.1.Tag.1.Key`.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Elastic Compute Cloud answers in AWS: XML whose root is `<OperationResponse>`. Errors captured on this page are an XML document rooted at `<Response>`.
