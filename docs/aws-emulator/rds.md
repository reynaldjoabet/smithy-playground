# Amazon Relational Database Service

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `RDS`
- **sigv4 signing name:** `rds`
- **Endpoint prefix:** `rds`
- **ARN namespace:** `rds`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `rds`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `DescribeDBInstances`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:rds" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded; charset=utf-8' \
  --data-binary 'Action=DescribeDBInstances&Version=2014-10-31' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a PostgreSQL instance, wait for it, snapshot it, then delete both. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateDBInstance

The instance starts in `creating`; the password is not echoed back.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 225
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=docs-demo-pg
&AllocatedStorage=20
&DBInstanceClass=db.t3.micro
&Engine=postgres
&MasterUsername=app
&MasterUserPassword=<redacted>
&PubliclyAccessible=false
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 5097
date: Thu, 26 Nov 2026 18:30:48 GMT

<CreateDBInstanceResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <CreateDBInstanceResult>
    <DBInstance xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <VPCNetworkingEnabled>true</VPCNetworkingEnabled>
      <HttpEndpointEnabled>false</HttpEndpointEnabled>
      <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
      <DBInstanceClass>db.t3.micro</DBInstanceClass>
      <Engine>postgres</Engine>
      <DBInstanceStatus>creating</DBInstanceStatus>
      <MasterUsername>app</MasterUsername>
      <Endpoint>
        <Address>0.0.0.0</Address>
        <Port>39253</Port>
        <HostedZoneId>Z2R2ITUGPM61AM</HostedZoneId>
      </Endpoint>
      <AllocatedStorage>20</AllocatedStorage>
      <InstanceCreateTime>2026-11-26T18:30:48.19992Z</InstanceCreateTime>
      <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
      <BackupRetentionPeriod>1</BackupRetentionPeriod>
      <DBSecurityGroups></DBSecurityGroups>
      <VpcSecurityGroups>
        <VpcSecurityGroupMembership>
          <VpcSecurityGroupId>sg-5964d6082ac46f28a</VpcSecurityGroupId>
          <Status>active</Status>
        </VpcSecurityGroupMembership>
      </VpcSecurityGroups>
      <DBParameterGroups>
        <DBParameterGroup>
          <DBParameterGroupName>default.postgres18</DBParameterGroupName>
          <ParameterApplyStatus>in-sync</ParameterApplyStatus>
        </DBParameterGroup>
      </DBParameterGroups>
      <AvailabilityZone>us-east-1a</AvailabilityZone>
      <DBSubnetGroup>
        <DBSubnetGroupName>default</DBSubnetGroupName>
        <DBSubnetGroupDescription>default</DBSubnetGroupDescription>
        <VpcId>vpc-00000000000000001</VpcId>
        <SubnetGroupStatus>Complete</SubnetGroupStatus>
        <Subnets>
          <Subnet>
            <SubnetIdentifier>subnet-00000000000000007</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1a</Name>
            </SubnetAvailabilityZone>
            <SubnetOutpost></SubnetOutpost>
            <SubnetStatus>Active</SubnetStatus>
          </Subnet>
          <Subnet>
            <SubnetIdentifier>subnet-00000000000000009</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1b</Name>
            </SubnetAvailabilityZone>
            <SubnetOutpost></SubnetOutpost>
            <SubnetStatus>Active</SubnetStatus>
          </Subnet>
          <Subnet>
            <SubnetIdentifier>subnet-0000000000000000b</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1c</Name>
            </SubnetAvailabilityZone>
… (84 more lines)
```

### 2. DescribeDBInstances

Poll until `available`. The endpoint is then a local address and port (`127.0.0.1:<port>`) that the emulator opened for the instance.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 79
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBInstances
&Version=2014-10-31
&DBInstanceIdentifier=docs-demo-pg
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 5136
date: Thu, 26 Nov 2026 18:30:48 GMT

<DescribeDBInstancesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBInstancesResult>
    <DBInstances>
      <DBInstance xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <VPCNetworkingEnabled>true</VPCNetworkingEnabled>
        <HttpEndpointEnabled>false</HttpEndpointEnabled>
        <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
        <DBInstanceClass>db.t3.micro</DBInstanceClass>
        <Engine>postgres</Engine>
        <DBInstanceStatus>creating</DBInstanceStatus>
        <MasterUsername>app</MasterUsername>
        <Endpoint>
          <Address>0.0.0.0</Address>
          <Port>39253</Port>
          <HostedZoneId>Z2R2ITUGPM61AM</HostedZoneId>
        </Endpoint>
        <AllocatedStorage>20</AllocatedStorage>
        <InstanceCreateTime>2026-11-26T18:30:48.19992Z</InstanceCreateTime>
        <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
        <BackupRetentionPeriod>1</BackupRetentionPeriod>
        <DBSecurityGroups></DBSecurityGroups>
        <VpcSecurityGroups>
          <VpcSecurityGroupMembership>
            <VpcSecurityGroupId>sg-5964d6082ac46f28a</VpcSecurityGroupId>
            <Status>active</Status>
          </VpcSecurityGroupMembership>
        </VpcSecurityGroups>
        <DBParameterGroups>
          <DBParameterGroup>
            <DBParameterGroupName>default.postgres18</DBParameterGroupName>
            <ParameterApplyStatus>in-sync</ParameterApplyStatus>
          </DBParameterGroup>
        </DBParameterGroups>
        <AvailabilityZone>us-east-1a</AvailabilityZone>
        <DBSubnetGroup>
          <DBSubnetGroupName>default</DBSubnetGroupName>
          <DBSubnetGroupDescription>default</DBSubnetGroupDescription>
          <VpcId>vpc-00000000000000001</VpcId>
          <SubnetGroupStatus>Complete</SubnetGroupStatus>
          <Subnets>
            <Subnet>
              <SubnetIdentifier>subnet-00000000000000007</SubnetIdentifier>
              <SubnetAvailabilityZone>
                <Name>us-east-1a</Name>
              </SubnetAvailabilityZone>
              <SubnetOutpost></SubnetOutpost>
              <SubnetStatus>Active</SubnetStatus>
            </Subnet>
            <Subnet>
              <SubnetIdentifier>subnet-00000000000000009</SubnetIdentifier>
              <SubnetAvailabilityZone>
                <Name>us-east-1b</Name>
              </SubnetAvailabilityZone>
              <SubnetOutpost></SubnetOutpost>
              <SubnetStatus>Active</SubnetStatus>
            </Subnet>
            <Subnet>
              <SubnetIdentifier>subnet-0000000000000000b</SubnetIdentifier>
              <SubnetAvailabilityZone>
                <Name>us-east-1c</Name>
… (86 more lines)
```

### 3. CreateDBSnapshot

A manual snapshot starts in `creating`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 115
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBSnapshot
&Version=2014-10-31
&DBSnapshotIdentifier=docs-demo-pg-snap
&DBInstanceIdentifier=docs-demo-pg
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1292
date: Thu, 26 Nov 2026 18:30:48 GMT

<CreateDBSnapshotResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <CreateDBSnapshotResult>
    <DBSnapshot xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <DBSnapshotIdentifier>docs-demo-pg-snap</DBSnapshotIdentifier>
      <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
      <SnapshotCreateTime>2026-11-26T18:30:48.723779Z</SnapshotCreateTime>
      <Engine>postgres</Engine>
      <AllocatedStorage>20</AllocatedStorage>
      <Status>creating</Status>
      <Port>5432</Port>
      <AvailabilityZone>us-east-1a</AvailabilityZone>
      <InstanceCreateTime>2026-11-26T18:30:48.19992Z</InstanceCreateTime>
      <MasterUsername>app</MasterUsername>
      <EngineVersion>18.3</EngineVersion>
      <LicenseModel>postgresql-license</LicenseModel>
      <SnapshotType>manual</SnapshotType>
      <StorageThroughput>0</StorageThroughput>
      <PercentProgress>0</PercentProgress>
      <StorageType>gp2</StorageType>
      <Encrypted>false</Encrypted>
      <BackupRetentionPeriod>1</BackupRetentionPeriod>
      <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
      <DBSnapshotArn>arn:aws:rds:us-east-1:000000000000:snapshot:docs-demo-pg-snap</DBSnapshotArn>
      <DbiResourceId>db-51P47QQD4ENYGI8N47O4LBQB2S</DbiResourceId>
      <TagList></TagList>
    </DBSnapshot>
  </CreateDBSnapshotResult>
  <ResponseMetadata>
    <RequestId>5cd901b6-b316-42fc-918c-2140ade5d27b</RequestId>
  </ResponseMetadata>
</CreateDBSnapshotResponse>
```

### 4. DescribeDBSnapshots

Poll until the snapshot is `available`.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBSnapshots
&Version=2014-10-31
&DBSnapshotIdentifier=docs-demo-pg-snap
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1331
date: Thu, 26 Nov 2026 18:30:48 GMT

<DescribeDBSnapshotsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBSnapshotsResult>
    <DBSnapshots>
      <DBSnapshot xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <DBSnapshotIdentifier>docs-demo-pg-snap</DBSnapshotIdentifier>
        <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
        <SnapshotCreateTime>2026-11-26T18:30:48.723779Z</SnapshotCreateTime>
        <Engine>postgres</Engine>
        <AllocatedStorage>20</AllocatedStorage>
        <Status>creating</Status>
        <Port>5432</Port>
        <AvailabilityZone>us-east-1a</AvailabilityZone>
        <InstanceCreateTime>2026-11-26T18:30:48.19992Z</InstanceCreateTime>
        <MasterUsername>app</MasterUsername>
        <EngineVersion>18.3</EngineVersion>
        <LicenseModel>postgresql-license</LicenseModel>
        <SnapshotType>manual</SnapshotType>
        <StorageThroughput>0</StorageThroughput>
        <PercentProgress>0</PercentProgress>
        <StorageType>gp2</StorageType>
        <Encrypted>false</Encrypted>
        <BackupRetentionPeriod>1</BackupRetentionPeriod>
        <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
        <DBSnapshotArn>arn:aws:rds:us-east-1:000000000000:snapshot:docs-demo-pg-snap</DBSnapshotArn>
        <DbiResourceId>db-51P47QQD4ENYGI8N47O4LBQB2S</DbiResourceId>
        <TagList></TagList>
      </DBSnapshot>
    </DBSnapshots>
  </DescribeDBSnapshotsResult>
  <ResponseMetadata>
    <RequestId>8f818934-56f5-4abd-b2ec-c9a25258a69f</RequestId>
  </ResponseMetadata>
</DescribeDBSnapshotsResponse>
```

### 5. DeleteDBInstance

Deleting without a final-snapshot decision is refused.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=docs-demo-pg
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 5f256634-743c-4795-8d22-c472083f37dd
content-length: 308
date: Thu, 26 Nov 2026 18:30:48 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>FinalDBSnapshotIdentifier is required unless SkipFinalSnapshot is specified.</Message>
  </Error>
  <RequestId>5f256634-743c-4795-8d22-c472083f37dd</RequestId>
</ErrorResponse>
```

### 6. DeleteDBInstance

Skipping the final snapshot deletes the instance.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183048Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=docs-demo-pg
&SkipFinalSnapshot=true
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 5100
date: Thu, 26 Nov 2026 18:30:48 GMT

<DeleteDBInstanceResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DeleteDBInstanceResult>
    <DBInstance xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <VPCNetworkingEnabled>true</VPCNetworkingEnabled>
      <HttpEndpointEnabled>false</HttpEndpointEnabled>
      <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
      <DBInstanceClass>db.t3.micro</DBInstanceClass>
      <Engine>postgres</Engine>
      <DBInstanceStatus>deleting</DBInstanceStatus>
      <MasterUsername>app</MasterUsername>
      <Endpoint>
        <Address>0.0.0.0</Address>
        <Port>39253</Port>
        <HostedZoneId>Z2R2ITUGPM61AM</HostedZoneId>
      </Endpoint>
      <AllocatedStorage>20</AllocatedStorage>
      <InstanceCreateTime>2026-11-26T18:30:48.19992Z</InstanceCreateTime>
      <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
      <BackupRetentionPeriod>1</BackupRetentionPeriod>
      <DBSecurityGroups></DBSecurityGroups>
      <VpcSecurityGroups>
        <VpcSecurityGroupMembership>
          <VpcSecurityGroupId>sg-5964d6082ac46f28a</VpcSecurityGroupId>
          <Status>active</Status>
        </VpcSecurityGroupMembership>
      </VpcSecurityGroups>
      <DBParameterGroups>
        <DBParameterGroup>
          <DBParameterGroupName>default.postgres18</DBParameterGroupName>
          <ParameterApplyStatus>in-sync</ParameterApplyStatus>
        </DBParameterGroup>
      </DBParameterGroups>
      <AvailabilityZone>us-east-1a</AvailabilityZone>
      <DBSubnetGroup>
        <DBSubnetGroupName>default</DBSubnetGroupName>
        <DBSubnetGroupDescription>default</DBSubnetGroupDescription>
        <VpcId>vpc-00000000000000001</VpcId>
        <SubnetGroupStatus>Complete</SubnetGroupStatus>
        <Subnets>
          <Subnet>
            <SubnetIdentifier>subnet-00000000000000007</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1a</Name>
            </SubnetAvailabilityZone>
            <SubnetOutpost></SubnetOutpost>
            <SubnetStatus>Active</SubnetStatus>
          </Subnet>
          <Subnet>
            <SubnetIdentifier>subnet-00000000000000009</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1b</Name>
            </SubnetAvailabilityZone>
            <SubnetOutpost></SubnetOutpost>
            <SubnetStatus>Active</SubnetStatus>
          </Subnet>
          <Subnet>
            <SubnetIdentifier>subnet-0000000000000000b</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1c</Name>
            </SubnetAvailabilityZone>
… (83 more lines)
```

### 7. DeleteDBSnapshot

Manual snapshots outlive their instance and are deleted on their own.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBSnapshot
&Version=2014-10-31
&DBSnapshotIdentifier=docs-demo-pg-snap
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1295
date: Thu, 26 Nov 2026 18:30:49 GMT

<DeleteDBSnapshotResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DeleteDBSnapshotResult>
    <DBSnapshot xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <DBSnapshotIdentifier>docs-demo-pg-snap</DBSnapshotIdentifier>
      <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
      <SnapshotCreateTime>2026-11-26T18:30:48.723779Z</SnapshotCreateTime>
      <Engine>postgres</Engine>
      <AllocatedStorage>20</AllocatedStorage>
      <Status>available</Status>
      <Port>5432</Port>
      <AvailabilityZone>us-east-1a</AvailabilityZone>
      <InstanceCreateTime>2026-11-26T18:30:48.19992Z</InstanceCreateTime>
      <MasterUsername>app</MasterUsername>
      <EngineVersion>18.3</EngineVersion>
      <LicenseModel>postgresql-license</LicenseModel>
      <SnapshotType>manual</SnapshotType>
      <StorageThroughput>0</StorageThroughput>
      <PercentProgress>100</PercentProgress>
      <StorageType>gp2</StorageType>
      <Encrypted>false</Encrypted>
      <BackupRetentionPeriod>1</BackupRetentionPeriod>
      <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
      <DBSnapshotArn>arn:aws:rds:us-east-1:000000000000:snapshot:docs-demo-pg-snap</DBSnapshotArn>
      <DbiResourceId>db-51P47QQD4ENYGI8N47O4LBQB2S</DbiResourceId>
      <TagList></TagList>
    </DBSnapshot>
  </DeleteDBSnapshotResult>
  <ResponseMetadata>
    <RequestId>e3ddac45-e574-4be9-b991-c57767c3a6cc</RequestId>
  </ResponseMetadata>
</DeleteDBSnapshotResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 168 operations, 37 accepted the placeholder call, 8 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.629* answered `501` in 1.0.595. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AddRoleToDBCluster

Associates an Identity and Access Management (IAM) role with a DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 101
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddRoleToDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
&RoleArn=example0000000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f3a8e9e5-9162-4dcd-8cba-ce05009b10f3
content-length: 401
date: Thu, 26 Nov 2026 18:30:49 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>1 validation error detected: Value &apos;example0000000000000&apos; at &apos;roleArn&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws[a-z-]*:iam::[0-9]*:role/.*</Message>
  </Error>
  <RequestId>f3a8e9e5-9162-4dcd-8cba-ce05009b10f3</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AddRoleToDBInstance

Associates an Amazon Web Services Identity and Access Management (IAM) role with a DB instance.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 165
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddRoleToDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
&RoleArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&FeatureName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2bb5dd46-fa46-4241-b895-b0ed76fba41a
content-length: 294
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid Role ARN: arn:aws:rds:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>2bb5dd46-fa46-4241-b895-b0ed76fba41a</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AddSourceIdentifierToSubscription

Adds a source identifier to an existing RDS event notification subscription.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 109
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddSourceIdentifierToSubscription
&Version=2014-10-31
&SubscriptionName=example
&SourceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 4a263159-456d-4132-99c7-4e64c4ed1db1
content-length: 262
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>SubscriptionNotFound</Code>
    <Message>Event Subscription example not found.</Message>
  </Error>
  <RequestId>4a263159-456d-4132-99c7-4e64c4ed1db1</RequestId>
</ErrorResponse>
```

`404` `SubscriptionNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AddTagsToResource

Adds metadata tags to an Amazon RDS resource.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddTagsToResource
&Version=2014-10-31
&ResourceName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 906efd0c-14ec-4b50-a4ba-9bb7a21d6890
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Please specify a value for parameter &apos;tags&apos;.</Message>
  </Error>
  <RequestId>906efd0c-14ec-4b50-a4ba-9bb7a21d6890</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ApplyPendingMaintenanceAction

Applies a pending maintenance action to a resource (for example, to a DB instance).

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 120
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ApplyPendingMaintenanceAction
&Version=2014-10-31
&ResourceIdentifier=example
&ApplyAction=example
&OptInType=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9fcb0d06-a5b5-44d1-944c-b3d01adf272d
content-length: 251
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Unknown resource: example</Message>
  </Error>
  <RequestId>9fcb0d06-a5b5-44d1-944c-b3d01adf272d</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AuthorizeDBSecurityGroupIngress

Enables ingress to a DBSecurityGroup using one of two forms of authorization.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 85
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AuthorizeDBSecurityGroupIngress
&Version=2014-10-31
&DBSecurityGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 638eb117-d609-4481-8fcc-5d4fa07af0a6
content-length: 262
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSecurityGroupNotFound</Code>
    <Message>DBSecurityGroup not found: example</Message>
  </Error>
  <RequestId>638eb117-d609-4481-8fcc-5d4fa07af0a6</RequestId>
</ErrorResponse>
```

`404` `DBSecurityGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### BacktrackDBCluster

Backtracks a DB cluster to a specific time, without creating a new DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 109
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=BacktrackDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
&BacktrackTo=2026-11-16T12%3A11%3A26Z
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 1a2da395-398c-4e8f-b52a-1744872dfbb5
content-length: 262
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>Could not find specified DB Cluster</Message>
  </Error>
  <RequestId>1a2da395-398c-4e8f-b52a-1744872dfbb5</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CancelDBCapacityReservation

Cancels a DB capacity reservation.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 93
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CancelDBCapacityReservation
&Version=2014-10-31
&DBCapacityReservationIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: f50dd9d2-2c30-4140-94d3-bf3d3304b3f6
content-length: 305
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBCapacityReservationNotFound</Code>
    <Message>The specified DBCapacityReservation &apos;example&apos; does not exist.</Message>
  </Error>
  <RequestId>f50dd9d2-2c30-4140-94d3-bf3d3304b3f6</RequestId>
</ErrorResponse>
```

`404` `DBCapacityReservationNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CancelExportTask

Cancels an export task in progress that is exporting a snapshot or cluster to Amazon S3.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CancelExportTask
&Version=2014-10-31
&ExportTaskIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 321cdb25-2273-4c92-8f41-f3e07734efdc
content-length: 266
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>ExportTaskNotFound</Code>
    <Message>The export task example doesn&apos;t exist.</Message>
  </Error>
  <RequestId>321cdb25-2273-4c92-8f41-f3e07734efdc</RequestId>
</ErrorResponse>
```

`404` `ExportTaskNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopyDBClusterParameterGroup

Copies the specified DB cluster parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 198
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CopyDBClusterParameterGroup
&Version=2014-10-31
&SourceDBClusterParameterGroupIdentifier=example
&TargetDBClusterParameterGroupIdentifier=example
&TargetDBClusterParameterGroupDescription=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 950c96c4-b9ac-4dd6-92b0-891853d9b7da
content-length: 259
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>Parameter group does not exist</Message>
  </Error>
  <RequestId>950c96c4-b9ac-4dd6-92b0-891853d9b7da</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopyDBClusterSnapshot

Copies a snapshot of a DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 131
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CopyDBClusterSnapshot
&Version=2014-10-31
&SourceDBClusterSnapshotIdentifier=example
&TargetDBClusterSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 78e246a1-5583-40a6-ad7f-0b1ad1ed36ea
content-length: 298
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterSnapshotNotFoundFault</Code>
    <Message>Requested snapshot example not found in source region us-east-1</Message>
  </Error>
  <RequestId>78e246a1-5583-40a6-ad7f-0b1ad1ed36ea</RequestId>
</ErrorResponse>
```

`404` `DBClusterSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopyDBParameterGroup

Copies the specified DB parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 170
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CopyDBParameterGroup
&Version=2014-10-31
&SourceDBParameterGroupIdentifier=example
&TargetDBParameterGroupIdentifier=example
&TargetDBParameterGroupDescription=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 66998b1e-46c6-4444-ac12-e98a9cf26e79
content-length: 259
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>Parameter group does not exist</Message>
  </Error>
  <RequestId>66998b1e-46c6-4444-ac12-e98a9cf26e79</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopyDBSnapshot

Copies the specified DB snapshot.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 110
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CopyDBSnapshot
&Version=2014-10-31
&SourceDBSnapshotIdentifier=example
&TargetDBSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: f2a860be-2390-462f-883b-8c7f8949f42f
content-length: 286
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSnapshotNotFound</Code>
    <Message>Requested snapshot example not found in source region us-east-1</Message>
  </Error>
  <RequestId>f2a860be-2390-462f-883b-8c7f8949f42f</RequestId>
</ErrorResponse>
```

`404` `DBSnapshotNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopyOptionGroup

Copies the specified option group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 150
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CopyOptionGroup
&Version=2014-10-31
&SourceOptionGroupIdentifier=example
&TargetOptionGroupIdentifier=example
&TargetOptionGroupDescription=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 506a9b0d-184f-4b95-9a1d-844f56d11ace
content-length: 274
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>OptionGroupNotFoundFault</Code>
    <Message>Specified OptionGroupName: example not found.</Message>
  </Error>
  <RequestId>506a9b0d-184f-4b95-9a1d-844f56d11ace</RequestId>
</ErrorResponse>
```

`404` `OptionGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateBlueGreenDeployment

Creates a blue/green deployment.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateBlueGreenDeployment
&Version=2014-10-31
&BlueGreenDeploymentName=example
&Source=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 118a8f38-04ef-488c-bb04-0f23aa0b794b
content-length: 381
date: Thu, 26 Nov 2026 18:30:49 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>1 validation error detected: Value &apos;example&apos; at &apos;source&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: arn:[A-Za-z][0-9A-Za-z-:._]*</Message>
  </Error>
  <RequestId>118a8f38-04ef-488c-bb04-0f23aa0b794b</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateCustomDBEngineVersion

Creates a custom DB engine version (CEV).

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateCustomDBEngineVersion
&Version=2014-10-31
&Engine=example
&EngineVersion=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 49dafde2-fe91-46fa-a54f-52998518609a
content-length: 243
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid DB engine</Message>
  </Error>
  <RequestId>49dafde2-fe91-46fa-a54f-52998518609a</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBCapacityReservation

Creates a DB capacity reservation.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 201
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBCapacityReservation
&Version=2014-10-31
&DBCapacityReservationName=example
&DBInstanceClass=example
&Engine=example
&CapacityPerAvailabilityZone=1
&AvailabilityZones.AvailabilityZone.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: ab190d30-87ea-49ce-878c-8446f4fc55cc
content-length: 397
date: Thu, 26 Nov 2026 18:30:49 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>1 validation error detected: Value &apos;example&apos; at &apos;dBInstanceClass&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: db\.[0-9a-z]{2,6}\.[0-9a-z\.]{4,20}</Message>
  </Error>
  <RequestId>ab190d30-87ea-49ce-878c-8446f4fc55cc</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBCluster

Creates a new Amazon Aurora DB cluster or Multi-AZ DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 78be84c5-d090-4b6e-9e6b-837edca2dd70
content-length: 243
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid DB engine</Message>
  </Error>
  <RequestId>78be84c5-d090-4b6e-9e6b-837edca2dd70</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBClusterEndpoint

Creates a new custom endpoint and associates it with an Amazon Aurora DB cluster.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 134
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBClusterEndpoint
&Version=2014-10-31
&DBClusterIdentifier=example
&DBClusterEndpointIdentifier=example
&EndpointType=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 578cd091-851b-4518-9b31-6d8a4f12a885
content-length: 312
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>invalid value &apos;example&apos; for EndpointType: must be one of READER, WRITER, ANY</Message>
  </Error>
  <RequestId>578cd091-851b-4518-9b31-6d8a4f12a885</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBClusterParameterGroup

Creates a new DB cluster parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 142
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBClusterParameterGroup
&Version=2014-10-31
&DBClusterParameterGroupName=example
&DBParameterGroupFamily=example
&Description=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 410c0d76-8531-4391-8e95-763834fc26b4
content-length: 292
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>ParameterGroupFamily example is not a valid parameter group family</Message>
  </Error>
  <RequestId>410c0d76-8531-4391-8e95-763834fc26b4</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBClusterSnapshot

Creates a snapshot of a DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 113
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBClusterSnapshot
&Version=2014-10-31
&DBClusterSnapshotIdentifier=example
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: c99c2b51-e593-40d7-8605-045a3c7b25ca
content-length: 255
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>DBCluster not found: example</Message>
  </Error>
  <RequestId>c99c2b51-e593-40d7-8605-045a3c7b25ca</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateDBInstance

Creates a new DB instance.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 110
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
&DBInstanceClass=example
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 65086258-3f68-4268-a9cc-3290ab6b7b32
content-length: 243
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid DB engine</Message>
  </Error>
  <RequestId>65086258-3f68-4268-a9cc-3290ab6b7b32</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createdbinstance) for a successful call.

### CreateDBInstanceReadReplica

Creates a new DB instance that acts as a read replica for an existing source DB instance or Multi-AZ DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 82
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBInstanceReadReplica
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 3a65c11f-da1a-4b87-98ad-a28ce8118257
content-length: 350
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>either SourceDBInstanceIdentifier or SourceDBClusterIdentifier must be specified: a read replica replicates one source</Message>
  </Error>
  <RequestId>3a65c11f-da1a-4b87-98ad-a28ce8118257</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### CreateDBParameterGroup

Creates a new DB parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 128
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBParameterGroup
&Version=2014-10-31
&DBParameterGroupName=example
&DBParameterGroupFamily=example
&Description=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 6e76907d-fa8c-4c7d-8999-a66cbdf56465
content-length: 292
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>ParameterGroupFamily example is not a valid parameter group family</Message>
  </Error>
  <RequestId>6e76907d-fa8c-4c7d-8999-a66cbdf56465</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBProxy

Creates a new DB proxy.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 179
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBProxy
&Version=2014-10-31
&DBProxyName=example
&EngineFamily=MYSQL
&RoleArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&VpcSubnetIds.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d674331c-27cb-474a-b2eb-71e9e1b60e31
content-length: 323
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidSubnet</Code>
    <Message>&apos;example&apos; is not an EC2 subnet id (expected subnet- followed by 8 or 17 hexadecimal characters)</Message>
  </Error>
  <RequestId>d674331c-27cb-474a-b2eb-71e9e1b60e31</RequestId>
</ErrorResponse>
```

`400` `InvalidSubnet`.

### CreateDBProxyEndpoint

Creates a DBProxyEndpoint .

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 125
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBProxyEndpoint
&Version=2014-10-31
&DBProxyName=example
&DBProxyEndpointName=example
&VpcSubnetIds.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 58f11be9-c4b0-4d5f-b1c8-faa5c13fc7ba
content-length: 323
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidSubnet</Code>
    <Message>&apos;example&apos; is not an EC2 subnet id (expected subnet- followed by 8 or 17 hexadecimal characters)</Message>
  </Error>
  <RequestId>58f11be9-c4b0-4d5f-b1c8-faa5c13fc7ba</RequestId>
</ErrorResponse>
```

`400` `InvalidSubnet`.

### CreateDBSecurityGroup

Creates a new DB security group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 110
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBSecurityGroup
&Version=2014-10-31
&DBSecurityGroupName=example
&DBSecurityGroupDescription=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 682
date: Thu, 26 Nov 2026 18:30:49 GMT

<CreateDBSecurityGroupResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <CreateDBSecurityGroupResult>
    <DBSecurityGroup xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <OwnerId>000000000000</OwnerId>
      <DBSecurityGroupName>example</DBSecurityGroupName>
      <DBSecurityGroupDescription>example</DBSecurityGroupDescription>
      <VpcId>vpc-bcb2053b5ea151bab</VpcId>
      <EC2SecurityGroups></EC2SecurityGroups>
      <IPRanges></IPRanges>
      <DBSecurityGroupArn>arn:aws:rds:us-east-1:000000000000:secgrp:example</DBSecurityGroupArn>
    </DBSecurityGroup>
  </CreateDBSecurityGroupResult>
  <ResponseMetadata>
    <RequestId>9ba5b531-c114-4cd3-8f9f-02c73661d842</RequestId>
  </ResponseMetadata>
</CreateDBSecurityGroupResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateDBShardGroup

Creates a new DB shard group for Aurora Limitless Database.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 112
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBShardGroup
&Version=2014-10-31
&DBShardGroupIdentifier=example
&DBClusterIdentifier=example
&MaxACU=1
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: a9545b5d-d689-43f5-a463-705f817089f5
content-length: 261
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>The DB cluster doesn&apos;t exist.</Message>
  </Error>
  <RequestId>a9545b5d-d689-43f5-a463-705f817089f5</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateDBSnapshot

Creates a snapshot of a DB instance.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 100
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBSnapshot
&Version=2014-10-31
&DBSnapshotIdentifier=example
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 9f589979-4d01-406f-a6ed-f9d88f3291a6
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>9f589979-4d01-406f-a6ed-f9d88f3291a6</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#3-createdbsnapshot) for a successful call.

### CreateDBSubnetGroup

Creates a new DB subnet group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 141
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBSubnetGroup
&Version=2014-10-31
&DBSubnetGroupName=example
&DBSubnetGroupDescription=example
&SubnetIds.SubnetIdentifier.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 56182e00-828b-44e5-8d83-47349f2f75a8
content-length: 323
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidSubnet</Code>
    <Message>&apos;example&apos; is not an EC2 subnet id (expected subnet- followed by 8 or 17 hexadecimal characters)</Message>
  </Error>
  <RequestId>56182e00-828b-44e5-8d83-47349f2f75a8</RequestId>
</ErrorResponse>
```

`400` `InvalidSubnet`.

### CreateEventSubscription

Creates an RDS event notification subscription.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 149
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateEventSubscription
&Version=2014-10-31
&SubscriptionName=example
&SnsTopicArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d198c5f4-2100-4518-88f7-0fdf26df7bb3
content-length: 408
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>SNSInvalidTopic</Code>
    <Message>invalid value arn:aws:rds:us-east-1:000000000000:example/example for SnsTopicArn: expected an Amazon SNS topic ARN of the form arn:aws:sns:&lt;region&gt;:&lt;account&gt;:&lt;topic name&gt;</Message>
  </Error>
  <RequestId>d198c5f4-2100-4518-88f7-0fdf26df7bb3</RequestId>
</ErrorResponse>
```

`400` `SNSInvalidTopic`.

### CreateGlobalCluster

Creates an Aurora global database spread across multiple Amazon Web Services Regions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 943
date: Thu, 26 Nov 2026 18:30:49 GMT

<CreateGlobalClusterResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <CreateGlobalClusterResult>
    <GlobalCluster xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <GlobalClusterIdentifier>example</GlobalClusterIdentifier>
      <GlobalClusterResourceId>cluster-6cffedcf13353749</GlobalClusterResourceId>
      <GlobalClusterArn>arn:aws:rds::000000000000:global-cluster:example</GlobalClusterArn>
      <Status>available</Status>
      <EngineLifecycleSupport>open-source-rds-extended-support</EngineLifecycleSupport>
      <StorageEncrypted>false</StorageEncrypted>
      <StorageEncryptionType>sse-rds</StorageEncryptionType>
      <DeletionProtection>false</DeletionProtection>
      <GlobalClusterMembers></GlobalClusterMembers>
      <Endpoint>example.global-0dnv0i1gmikr.global.rds.amazonaws.com</Endpoint>
      <TagList></TagList>
    </GlobalCluster>
  </CreateGlobalClusterResult>
  <ResponseMetadata>
    <RequestId>95c7e569-d926-43e4-9395-357e86e3f509</RequestId>
  </ResponseMetadata>
</CreateGlobalClusterResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateIntegration

Creates a zero-ETL integration with Amazon Redshift.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 158
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateIntegration
&Version=2014-10-31
&SourceArn=example
&TargetArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&IntegrationName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 65688956-9220-479c-920c-e363664875c1
content-length: 445
date: Thu, 26 Nov 2026 18:30:49 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>1 validation error detected: Value &apos;example&apos; at &apos;sourceArn&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws[a-z\-]*:rds(-[a-z]*)?:[a-z0-9\-]*:[0-9]*:(cluster|db):[a-z][a-z0-9]*(-[a-z0-9]+)*</Message>
  </Error>
  <RequestId>65688956-9220-479c-920c-e363664875c1</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateOptionGroup

Creates a new option group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 144
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateOptionGroup
&Version=2014-10-31
&OptionGroupName=example
&EngineName=example
&MajorEngineVersion=example
&OptionGroupDescription=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 6b12371b-a1ab-48d2-bd60-fe11d73dadd6
content-length: 452
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>invalid value &apos;example&apos; for EngineName: valid values are db2-ae, db2-ce, db2-se, mariadb, mysql, oracle-ee, oracle-ee-cdb, oracle-se2, oracle-se2-cdb, postgres, sqlserver-ee, sqlserver-se, sqlserver-ex, sqlserver-web</Message>
  </Error>
  <RequestId>6b12371b-a1ab-48d2-bd60-fe11d73dadd6</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateTenantDatabase

Creates a tenant database in a DB instance that uses the multi-tenant configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 119
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateTenantDatabase
&Version=2014-10-31
&DBInstanceIdentifier=example
&TenantDBName=example
&MasterUsername=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 09e48189-cd6c-4efa-b4ef-145cf06a8d68
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>09e48189-cd6c-4efa-b4ef-145cf06a8d68</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteBlueGreenDeployment

Deletes a blue/green deployment.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 89
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteBlueGreenDeployment
&Version=2014-10-31
&BlueGreenDeploymentIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 079031e9-3f0a-45a1-8956-f9a41b01a730
content-length: 275
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid blue-green deployment identifier: example</Message>
  </Error>
  <RequestId>079031e9-3f0a-45a1-8956-f9a41b01a730</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteCustomDBEngineVersion

Deletes a custom engine version.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteCustomDBEngineVersion
&Version=2014-10-31
&Engine=example
&EngineVersion=example
```

```http
HTTP/1.1 403 Forbidden
content-type: text/xml
x-amzn-requestid: 76d2b2b2-af3a-41cd-969b-01e996a3a1ab
content-length: 318
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>The custom engine version you requested may not exist or you don&apos;t have the required permission.</Message>
  </Error>
  <RequestId>76d2b2b2-af3a-41cd-969b-01e996a3a1ab</RequestId>
</ErrorResponse>
```

`403` `AccessDenied`: the request was refused by authorization.

### DeleteDBCluster

The DeleteDBCluster action deletes a previously provisioned DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: b6b03ebe-4895-45f7-9847-70048800f739
content-length: 295
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>The source cluster could not be found or cannot be accessed: example</Message>
  </Error>
  <RequestId>b6b03ebe-4895-45f7-9847-70048800f739</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBClusterAutomatedBackup

Deletes automated backups using the DbClusterResourceId value of the source DB cluster or the Amazon Resource Name (ARN) of the automated backups.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBClusterAutomatedBackup
&Version=2014-10-31
&DbClusterResourceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: 4741e9a3-ab74-4e0f-ad8f-987708e27e97
content-length: 312
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DeleteDBClusterAutomatedBackup is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>4741e9a3-ab74-4e0f-ad8f-987708e27e97</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteDBClusterEndpoint

Deletes a custom endpoint and removes it from an Amazon Aurora DB cluster.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 85
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBClusterEndpoint
&Version=2014-10-31
&DBClusterEndpointIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 81a09c1a-bc00-4a2d-bc15-89ef4f45dbcc
content-length: 270
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterEndpointNotFoundFault</Code>
    <Message>DBClusterEndpoint example not found</Message>
  </Error>
  <RequestId>81a09c1a-bc00-4a2d-bc15-89ef4f45dbcc</RequestId>
</ErrorResponse>
```

`400` `DBClusterEndpointNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBClusterParameterGroup

Deletes a specified DB cluster parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBClusterParameterGroup
&Version=2014-10-31
&DBClusterParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 38e314ff-205a-49f4-9be2-f52ed280fc66
content-length: 271
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBClusterParameterGroup not found: example</Message>
  </Error>
  <RequestId>38e314ff-205a-49f4-9be2-f52ed280fc66</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBClusterSnapshot

Deletes a DB cluster snapshot.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 85
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBClusterSnapshot
&Version=2014-10-31
&DBClusterSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: e64f3e22-6367-4fd4-87d2-97c24fe1f8e8
content-length: 271
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterSnapshotNotFoundFault</Code>
    <Message>DBClusterSnapshot not found: example</Message>
  </Error>
  <RequestId>e64f3e22-6367-4fd4-87d2-97c24fe1f8e8</RequestId>
</ErrorResponse>
```

`404` `DBClusterSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBInstance

Deletes a previously provisioned DB instance.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 98a3ec04-10a2-4c4a-b019-28157710b861
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance example not found.</Message>
  </Error>
  <RequestId>98a3ec04-10a2-4c4a-b019-28157710b861</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#6-deletedbinstance) for a successful call.

### DeleteDBInstanceAutomatedBackup

Deletes automated backups using the DbiResourceId value of the source DB instance or the Amazon Resource Name (ARN) of the automated backups.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 57
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBInstanceAutomatedBackup
&Version=2014-10-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b6ed7fc2-4597-438d-82be-aa8221834178
content-length: 319
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>DbiResourceId must be specified when DBInstanceAutomatedBackupsArn isn&apos;t specified</Message>
  </Error>
  <RequestId>b6ed7fc2-4597-438d-82be-aa8221834178</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### DeleteDBParameterGroup

Deletes a specified DB parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBParameterGroup
&Version=2014-10-31
&DBParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 6a627fa4-575e-42f2-b818-94c12d974ef8
content-length: 264
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBParameterGroup not found: example</Message>
  </Error>
  <RequestId>6a627fa4-575e-42f2-b818-94c12d974ef8</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBProxy

Deletes an existing DB proxy.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBProxy
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d2392e23-78e6-4674-a956-a41abcc41ec9
content-length: 263
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBProxyNotFoundFault</Code>
    <Message>DBProxy &apos;example&apos; not found.</Message>
  </Error>
  <RequestId>d2392e23-78e6-4674-a956-a41abcc41ec9</RequestId>
</ErrorResponse>
```

`404` `DBProxyNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBProxyEndpoint

Deletes a DBProxyEndpoint .

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBProxyEndpoint
&Version=2014-10-31
&DBProxyEndpointName=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: a7b3a63d-7035-40a6-b338-4a98785fad8e
content-length: 303
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DeleteDBProxyEndpoint is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>a7b3a63d-7035-40a6-b338-4a98785fad8e</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteDBSecurityGroup

Deletes a DB security group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBSecurityGroup
&Version=2014-10-31
&DBSecurityGroupName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 267
date: Thu, 26 Nov 2026 18:30:49 GMT

<DeleteDBSecurityGroupResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DeleteDBSecurityGroupResult></DeleteDBSecurityGroupResult>
  <ResponseMetadata>
    <RequestId>d1ccaf99-cded-4871-9251-e9bd774af1d6</RequestId>
  </ResponseMetadata>
</DeleteDBSecurityGroupResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteDBShardGroup

Deletes an Aurora Limitless Database DB shard group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBShardGroup
&Version=2014-10-31
&DBShardGroupIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: e93f967b-7cfb-419d-8195-f704840b960c
content-length: 272
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBShardGroupNotFound</Code>
    <Message>DB shard group not found or can not be accessed</Message>
  </Error>
  <RequestId>e93f967b-7cfb-419d-8195-f704840b960c</RequestId>
</ErrorResponse>
```

`404` `DBShardGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBSnapshot

Deletes a DB snapshot.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBSnapshot
&Version=2014-10-31
&DBSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 44a5397a-0579-4caf-adf4-58d2f9387586
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSnapshotNotFound</Code>
    <Message>DBSnapshot not found: example</Message>
  </Error>
  <RequestId>44a5397a-0579-4caf-adf4-58d2f9387586</RequestId>
</ErrorResponse>
```

`404` `DBSnapshotNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deletedbsnapshot) for a successful call.

### DeleteDBSubnetGroup

Deletes a DB subnet group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBSubnetGroup
&Version=2014-10-31
&DBSubnetGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d8c9c920-8861-48fd-b83c-0abc0b0993c0
content-length: 282
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSubnetGroupNotFoundFault</Code>
    <Message>DB subnet group &apos;example&apos; does not exist.</Message>
  </Error>
  <RequestId>d8c9c920-8861-48fd-b83c-0abc0b0993c0</RequestId>
</ErrorResponse>
```

`404` `DBSubnetGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteEventSubscription

Deletes an RDS event notification subscription.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 74
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteEventSubscription
&Version=2014-10-31
&SubscriptionName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: afb123a8-399e-4c9a-b8c9-a5a5b283d672
content-length: 262
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>SubscriptionNotFound</Code>
    <Message>Event Subscription example not found.</Message>
  </Error>
  <RequestId>afb123a8-399e-4c9a-b8c9-a5a5b283d672</RequestId>
</ErrorResponse>
```

`404` `SubscriptionNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteGlobalCluster

Deletes a global database cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 943
date: Thu, 26 Nov 2026 18:30:49 GMT

<DeleteGlobalClusterResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DeleteGlobalClusterResult>
    <GlobalCluster xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <GlobalClusterIdentifier>example</GlobalClusterIdentifier>
      <GlobalClusterResourceId>cluster-6cffedcf13353749</GlobalClusterResourceId>
      <GlobalClusterArn>arn:aws:rds::000000000000:global-cluster:example</GlobalClusterArn>
      <Status>available</Status>
      <EngineLifecycleSupport>open-source-rds-extended-support</EngineLifecycleSupport>
      <StorageEncrypted>false</StorageEncrypted>
      <StorageEncryptionType>sse-rds</StorageEncryptionType>
      <DeletionProtection>false</DeletionProtection>
      <GlobalClusterMembers></GlobalClusterMembers>
      <Endpoint>example.global-0dnv0i1gmikr.global.rds.amazonaws.com</Endpoint>
      <TagList></TagList>
    </GlobalCluster>
  </DeleteGlobalClusterResult>
  <ResponseMetadata>
    <RequestId>f111b787-fc41-41b8-a4cc-b745ed017447</RequestId>
  </ResponseMetadata>
</DeleteGlobalClusterResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteIntegration

Deletes a zero-ETL integration with Amazon Redshift.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteIntegration
&Version=2014-10-31
&IntegrationIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 3fcb2930-f9a1-48a2-afc7-3d3902608912
content-length: 461
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>IntegrationNotFoundFault</Code>
    <Message>Integration with identifier arn:aws:rds:us-east-1:000000000000:integration:example could not be found. The integration might have been deleted, or you provided an incorrect identifier. Check the integration identifier and try again.</Message>
  </Error>
  <RequestId>3fcb2930-f9a1-48a2-afc7-3d3902608912</RequestId>
</ErrorResponse>
```

`404` `IntegrationNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteOptionGroup

Deletes an existing option group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteOptionGroup
&Version=2014-10-31
&OptionGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 03b948df-d058-41a8-9aa7-0b27836b6019
content-length: 274
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>OptionGroupNotFoundFault</Code>
    <Message>Specified OptionGroupName: example not found.</Message>
  </Error>
  <RequestId>03b948df-d058-41a8-9aa7-0b27836b6019</RequestId>
</ErrorResponse>
```

`404` `OptionGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTenantDatabase

Deletes a tenant database from your DB instance.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteTenantDatabase
&Version=2014-10-31
&DBInstanceIdentifier=example
&TenantDBName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: cd2f858c-3f22-4468-a7f1-33b7d1a2dfd3
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>cd2f858c-3f22-4468-a7f1-33b7d1a2dfd3</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeregisterDBProxyTargets

Remove the association between one or more DBProxyTarget data structures and a DBProxyTargetGroup .

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeregisterDBProxyTargets
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: aa96a5d9-8110-4bcf-9696-d183539a3605
content-length: 277
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>Missing DB instance or DB cluster identifier.</Message>
  </Error>
  <RequestId>aa96a5d9-8110-4bcf-9696-d183539a3605</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### DescribeAccountAttributes

Lists all of the attributes for a customer account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeAccountAttributes
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 2472
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeAccountAttributesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeAccountAttributesResult>
    <AccountQuotas>
      <AccountQuota xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <AccountQuotaName>DBInstances</AccountQuotaName>
        <Used>0</Used>
        <Max>40</Max>
      </AccountQuota>
      <AccountQuota>
        <AccountQuotaName>ReservedDBInstances</AccountQuotaName>
        <Used>0</Used>
        <Max>40</Max>
      </AccountQuota>
      <AccountQuota>
        <AccountQuotaName>AllocatedStorage</AccountQuotaName>
        <Used>0</Used>
        <Max>100000</Max>
      </AccountQuota>
      <AccountQuota>
        <AccountQuotaName>DBSecurityGroups</AccountQuotaName>
… (84 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeBlueGreenDeployments

Describes one or more blue/green deployments.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeBlueGreenDeployments
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 340
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeBlueGreenDeploymentsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeBlueGreenDeploymentsResult>
    <BlueGreenDeployments></BlueGreenDeployments>
  </DescribeBlueGreenDeploymentsResult>
  <ResponseMetadata>
    <RequestId>b805e8a2-4650-46c8-9b72-66dc0fba733f</RequestId>
  </ResponseMetadata>
</DescribeBlueGreenDeploymentsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCertificates

Lists the set of certificate authority (CA) certificates provided by Amazon RDS for this Amazon Web Services account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCertificates
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1622
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeCertificatesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeCertificatesResult>
    <DefaultCertificateForNewLaunches>rds-ca-rsa2048-g1</DefaultCertificateForNewLaunches>
    <Certificates>
      <Certificate xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <CertificateIdentifier>rds-ca-ecc384-g1</CertificateIdentifier>
        <CertificateType>CA</CertificateType>
        <Thumbprint>2ee3dcc06e50192559b13929e73484354f23387d</Thumbprint>
        <ValidFrom>2021-05-24T22:06:59Z</ValidFrom>
        <ValidTill>2121-05-24T23:06:59Z</ValidTill>
        <CertificateArn>arn:aws:rds:us-east-1::cert:rds-ca-ecc384-g1</CertificateArn>
        <CustomerOverride>false</CustomerOverride>
      </Certificate>
      <Certificate>
        <CertificateIdentifier>rds-ca-rsa4096-g1</CertificateIdentifier>
        <CertificateType>CA</CertificateType>
        <Thumbprint>19da4f2af579a8ae1f6a0fa77aa5befd874b4cab</Thumbprint>
        <ValidFrom>2021-05-24T22:03:20Z</ValidFrom>
        <ValidTill>2121-05-24T23:03:20Z</ValidTill>
        <CertificateArn>arn:aws:rds:us-east-1::cert:rds-ca-rsa4096-g1</CertificateArn>
… (17 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBCapacityReservations

Describes one or more DB capacity reservations.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 56
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBCapacityReservations
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 352
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBCapacityReservationsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBCapacityReservationsResult>
    <DBCapacityReservations></DBCapacityReservations>
  </DescribeDBCapacityReservationsResult>
  <ResponseMetadata>
    <RequestId>80ffa822-f6f5-4884-8762-4931f26dd9cc</RequestId>
  </ResponseMetadata>
</DescribeDBCapacityReservationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBClusterAutomatedBackups

Displays backups for both current and deleted DB clusters.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBClusterAutomatedBackups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 370
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBClusterAutomatedBackupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBClusterAutomatedBackupsResult>
    <DBClusterAutomatedBackups></DBClusterAutomatedBackups>
  </DescribeDBClusterAutomatedBackupsResult>
  <ResponseMetadata>
    <RequestId>d8a34e03-dfc6-4f4d-a049-dcb405c44921</RequestId>
  </ResponseMetadata>
</DescribeDBClusterAutomatedBackupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBClusterBacktracks

Returns information about backtracks for a DB cluster.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBClusterBacktracks
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 32b52cf7-4a43-4dec-9a8a-f4f6dfc81723
content-length: 262
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>Could not find specified DB Cluster</Message>
  </Error>
  <RequestId>32b52cf7-4a43-4dec-9a8a-f4f6dfc81723</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBClusterEndpoints

Returns information about endpoints for an Amazon Aurora DB cluster.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBClusterEndpoints
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 328
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBClusterEndpointsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBClusterEndpointsResult>
    <DBClusterEndpoints></DBClusterEndpoints>
  </DescribeDBClusterEndpointsResult>
  <ResponseMetadata>
    <RequestId>2887f3d3-297b-48dd-b5ee-e54b8fe090c5</RequestId>
  </ResponseMetadata>
</DescribeDBClusterEndpointsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBClusterParameterGroups

Returns a list of DBClusterParameterGroup descriptions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBClusterParameterGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 11385
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBClusterParameterGroupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBClusterParameterGroupsResult>
    <DBClusterParameterGroups>
      <DBClusterParameterGroup xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <DBClusterParameterGroupName>default.aurora-mysql5.7</DBClusterParameterGroupName>
        <DBParameterGroupFamily>aurora-mysql5.7</DBParameterGroupFamily>
        <Description>Default cluster parameter group for aurora-mysql5.7</Description>
        <DBClusterParameterGroupArn>arn:aws:rds:us-east-1:000000000000:cluster-pg:default.aurora-mysql5.7</DBClusterParameterGroupArn>
      </DBClusterParameterGroup>
      <DBClusterParameterGroup>
        <DBClusterParameterGroupName>default.aurora-mysql8.0</DBClusterParameterGroupName>
        <DBParameterGroupFamily>aurora-mysql8.0</DBParameterGroupFamily>
        <Description>Default cluster parameter group for aurora-mysql8.0</Description>
        <DBClusterParameterGroupArn>arn:aws:rds:us-east-1:000000000000:cluster-pg:default.aurora-mysql8.0</DBClusterParameterGroupArn>
      </DBClusterParameterGroup>
      <DBClusterParameterGroup>
        <DBClusterParameterGroupName>default.aurora-postgresql11</DBClusterParameterGroupName>
        <DBParameterGroupFamily>aurora-postgresql11</DBParameterGroupFamily>
        <Description>Default cluster parameter group for aurora-postgresql11</Description>
        <DBClusterParameterGroupArn>arn:aws:rds:us-east-1:000000000000:cluster-pg:default.aurora-postgresql11</DBClusterParameterGroupArn>
… (157 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBClusterParameters

Returns the detailed parameter list for a particular DB cluster parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 89
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBClusterParameters
&Version=2014-10-31
&DBClusterParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 4ae46ebb-31d1-4eb9-ab97-6d7c10dde9c3
content-length: 271
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBClusterParameterGroup not found: example</Message>
  </Error>
  <RequestId>4ae46ebb-31d1-4eb9-ab97-6d7c10dde9c3</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBClusterSnapshotAttributes

Returns a list of DB cluster snapshot attribute names and values for a manual DB cluster snapshot.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 97
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBClusterSnapshotAttributes
&Version=2014-10-31
&DBClusterSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: ad4c026e-87e0-4524-b2b2-f4d2fdfd7f01
content-length: 271
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterSnapshotNotFoundFault</Code>
    <Message>DBClusterSnapshot not found: example</Message>
  </Error>
  <RequestId>ad4c026e-87e0-4524-b2b2-f4d2fdfd7f01</RequestId>
</ErrorResponse>
```

`404` `DBClusterSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBClusterSnapshots

Returns information about DB cluster snapshots.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBClusterSnapshots
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 328
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBClusterSnapshotsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBClusterSnapshotsResult>
    <DBClusterSnapshots></DBClusterSnapshots>
  </DescribeDBClusterSnapshotsResult>
  <ResponseMetadata>
    <RequestId>430efc2b-7f8a-452d-b78c-c115af6d5acb</RequestId>
  </ResponseMetadata>
</DescribeDBClusterSnapshotsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBClusters

Describes existing Amazon Aurora DB clusters and Multi-AZ DB clusters.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBClusters
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 280
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBClustersResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBClustersResult>
    <DBClusters></DBClusters>
  </DescribeDBClustersResult>
  <ResponseMetadata>
    <RequestId>44367d6e-ea23-497f-99d3-f74c18c612c3</RequestId>
  </ResponseMetadata>
</DescribeDBClustersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBEngineVersions

Describes the properties of specific versions of DB engines.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBEngineVersions
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 686322
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBEngineVersionsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBEngineVersionsResult>
    <Marker>bmVwdHVuZS8xLjQuOC4w</Marker>
    <DBEngineVersions>
      <DBEngineVersion xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <MinorEngineVersion>mysql_aurora.2.11.1</MinorEngineVersion>
        <SupportsRDSProxy>true</SupportsRDSProxy>
        <SupportsCluster>false</SupportsCluster>
        <SupportsDataGroups>false</SupportsDataGroups>
        <SupportsEnhancedBinlog>false</SupportsEnhancedBinlog>
        <SupportsChangeDataCaptureStreams>false</SupportsChangeDataCaptureStreams>
        <Engine>aurora-mysql</Engine>
        <MajorEngineVersion>5.7</MajorEngineVersion>
        <EngineVersion>5.7.mysql_aurora.2.11.1</EngineVersion>
        <DBParameterGroupFamily>aurora-mysql5.7</DBParameterGroupFamily>
        <DBEngineDescription>Aurora MySQL</DBEngineDescription>
        <DBEngineVersionDescription>Aurora (MySQL 5.7) 2.11.1</DBEngineVersionDescription>
        <ValidUpgradeTarget>
          <UpgradeTarget>
            <SupportsDataGroups>false</SupportsDataGroups>
… (16793 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBInstanceAutomatedBackups

Displays backups for both current and deleted instances.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 60
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBInstanceAutomatedBackups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 376
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBInstanceAutomatedBackupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBInstanceAutomatedBackupsResult>
    <DBInstanceAutomatedBackups></DBInstanceAutomatedBackups>
  </DescribeDBInstanceAutomatedBackupsResult>
  <ResponseMetadata>
    <RequestId>05ec1ae5-ec23-4e57-a4b4-a1984d60273d</RequestId>
  </ResponseMetadata>
</DescribeDBInstanceAutomatedBackupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBInstances

Describes provisioned RDS instances.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBInstances
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 286
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBInstancesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBInstancesResult>
    <DBInstances></DBInstances>
  </DescribeDBInstancesResult>
  <ResponseMetadata>
    <RequestId>6c7ed0d3-e541-4891-8d14-03e9d21213dd</RequestId>
  </ResponseMetadata>
</DescribeDBInstancesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-describedbinstances) for a successful call.

### DescribeDBLogFiles

Returns a list of DB log files for the DB instance.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBLogFiles
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: b146a7a6-5898-471b-97fd-b83e634716ae
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>b146a7a6-5898-471b-97fd-b83e634716ae</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBMajorEngineVersions

Describes the properties of specific major versions of DB engines.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 55
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBMajorEngineVersions
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 20431
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBMajorEngineVersionsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBMajorEngineVersionsResult>
    <DBMajorEngineVersions>
      <DBMajorEngineVersion xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <Engine>aurora-mysql</Engine>
        <MajorEngineVersion>5.7</MajorEngineVersion>
        <SupportedEngineLifecycles>
          <SupportedEngineLifecycle>
            <LifecycleSupportName>open-source-rds-standard-support</LifecycleSupportName>
            <LifecycleSupportStartDate>2018-02-06T00:00:00Z</LifecycleSupportStartDate>
            <LifecycleSupportEndDate>2024-10-31T23:59:59.999Z</LifecycleSupportEndDate>
          </SupportedEngineLifecycle>
          <SupportedEngineLifecycle>
            <LifecycleSupportName>open-source-rds-extended-support</LifecycleSupportName>
            <LifecycleSupportStartDate>2024-11-01T00:00:00Z</LifecycleSupportStartDate>
            <LifecycleSupportEndDate>2029-06-30T23:59:59.999Z</LifecycleSupportEndDate>
          </SupportedEngineLifecycle>
        </SupportedEngineLifecycles>
      </DBMajorEngineVersion>
      <DBMajorEngineVersion>
… (481 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBParameterGroups

Returns a list of DBParameterGroup descriptions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBParameterGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 23795
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBParameterGroupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBParameterGroupsResult>
    <DBParameterGroups>
      <DBParameterGroup xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <DBParameterGroupName>default.aurora-mysql5.7</DBParameterGroupName>
        <DBParameterGroupFamily>aurora-mysql5.7</DBParameterGroupFamily>
        <Description>Default parameter group for aurora-mysql5.7</Description>
        <DBParameterGroupArn>arn:aws:rds:us-east-1:000000000000:pg:default.aurora-mysql5.7</DBParameterGroupArn>
      </DBParameterGroup>
      <DBParameterGroup>
        <DBParameterGroupName>default.aurora-mysql8.0</DBParameterGroupName>
        <DBParameterGroupFamily>aurora-mysql8.0</DBParameterGroupFamily>
        <Description>Default parameter group for aurora-mysql8.0</Description>
        <DBParameterGroupArn>arn:aws:rds:us-east-1:000000000000:pg:default.aurora-mysql8.0</DBParameterGroupArn>
      </DBParameterGroup>
      <DBParameterGroup>
        <DBParameterGroupName>default.aurora-postgresql11</DBParameterGroupName>
        <DBParameterGroupFamily>aurora-postgresql11</DBParameterGroupFamily>
        <Description>Default parameter group for aurora-postgresql11</Description>
        <DBParameterGroupArn>arn:aws:rds:us-east-1:000000000000:pg:default.aurora-postgresql11</DBParameterGroupArn>
… (397 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBParameters

Returns the detailed parameter list for a particular DB parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBParameters
&Version=2014-10-31
&DBParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: c15cef83-ccfa-46cd-bd6d-90c7cc7a9beb
content-length: 264
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBParameterGroup not found: example</Message>
  </Error>
  <RequestId>c15cef83-ccfa-46cd-bd6d-90c7cc7a9beb</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBProxies

Returns information about DB proxies.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBProxies
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 274
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBProxiesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBProxiesResult>
    <DBProxies></DBProxies>
  </DescribeDBProxiesResult>
  <ResponseMetadata>
    <RequestId>084925cd-cb80-4769-9911-748431af1076</RequestId>
  </ResponseMetadata>
</DescribeDBProxiesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBProxyEndpoints

Returns information about DB proxy endpoints.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBProxyEndpoints
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 316
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBProxyEndpointsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBProxyEndpointsResult>
    <DBProxyEndpoints></DBProxyEndpoints>
  </DescribeDBProxyEndpointsResult>
  <ResponseMetadata>
    <RequestId>b5ad8bdc-1f98-4995-893d-01096c176851</RequestId>
  </ResponseMetadata>
</DescribeDBProxyEndpointsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBProxyTargetGroups

Returns information about DB proxy target groups, represented by DBProxyTargetGroup data structures.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBProxyTargetGroups
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: e8f4437d-eb19-4eaa-94a5-bdd491ecc29f
content-length: 263
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBProxyNotFoundFault</Code>
    <Message>DBProxy &apos;example&apos; not found.</Message>
  </Error>
  <RequestId>e8f4437d-eb19-4eaa-94a5-bdd491ecc29f</RequestId>
</ErrorResponse>
```

`404` `DBProxyNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBProxyTargets

Returns information about DBProxyTarget objects.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBProxyTargets
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 2db981f3-59df-4d66-8e66-760649e0f68e
content-length: 263
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBProxyNotFoundFault</Code>
    <Message>DBProxy &apos;example&apos; not found.</Message>
  </Error>
  <RequestId>2db981f3-59df-4d66-8e66-760649e0f68e</RequestId>
</ErrorResponse>
```

`404` `DBProxyNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBRecommendations

Describes the recommendations to resolve the issues for your DB instances, DB clusters, and DB parameter groups.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBRecommendations
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 322
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBRecommendationsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBRecommendationsResult>
    <DBRecommendations></DBRecommendations>
  </DescribeDBRecommendationsResult>
  <ResponseMetadata>
    <RequestId>6283bb3b-1e3a-4f5d-a0be-2e6432d21c38</RequestId>
  </ResponseMetadata>
</DescribeDBRecommendationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBSecurityGroups

Returns a list of DBSecurityGroup descriptions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBSecurityGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 316
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBSecurityGroupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBSecurityGroupsResult>
    <DBSecurityGroups></DBSecurityGroups>
  </DescribeDBSecurityGroupsResult>
  <ResponseMetadata>
    <RequestId>3269297b-bdaa-4589-bf32-9b4768dac5d8</RequestId>
  </ResponseMetadata>
</DescribeDBSecurityGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBShardGroups

Describes existing Aurora Limitless Database DB shard groups.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBShardGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 298
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBShardGroupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBShardGroupsResult>
    <DBShardGroups></DBShardGroups>
  </DescribeDBShardGroupsResult>
  <ResponseMetadata>
    <RequestId>5f330da2-48d2-4496-a5c1-b57da9428a1a</RequestId>
  </ResponseMetadata>
</DescribeDBShardGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBSnapshotAttributes

Returns a list of DB snapshot attribute names and values for a manual DB snapshot.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBSnapshotAttributes
&Version=2014-10-31
&DBSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d3e36bb1-d9b2-40d2-90f8-99a552a26e57
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSnapshotNotFound</Code>
    <Message>DBSnapshot not found: example</Message>
  </Error>
  <RequestId>d3e36bb1-d9b2-40d2-90f8-99a552a26e57</RequestId>
</ErrorResponse>
```

`404` `DBSnapshotNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBSnapshotTenantDatabases

Describes the tenant databases that exist in a DB snapshot.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBSnapshotTenantDatabases
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 370
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBSnapshotTenantDatabasesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBSnapshotTenantDatabasesResult>
    <DBSnapshotTenantDatabases></DBSnapshotTenantDatabases>
  </DescribeDBSnapshotTenantDatabasesResult>
  <ResponseMetadata>
    <RequestId>307bac50-1b9b-44a3-ac61-1bac0091d83c</RequestId>
  </ResponseMetadata>
</DescribeDBSnapshotTenantDatabasesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBSnapshots

Returns information about DB snapshots.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBSnapshots
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 286
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBSnapshotsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBSnapshotsResult>
    <DBSnapshots></DBSnapshots>
  </DescribeDBSnapshotsResult>
  <ResponseMetadata>
    <RequestId>4470b885-7aa2-403b-bd0f-8706d3da0983</RequestId>
  </ResponseMetadata>
</DescribeDBSnapshotsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-describedbsnapshots) for a successful call.

### DescribeDBSubnetGroups

Returns a list of DBSubnetGroup descriptions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 48
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBSubnetGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 2042
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeDBSubnetGroupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBSubnetGroupsResult>
    <DBSubnetGroups>
      <DBSubnetGroup xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <DBSubnetGroupName>default</DBSubnetGroupName>
        <DBSubnetGroupDescription>default</DBSubnetGroupDescription>
        <VpcId>vpc-00000000000000001</VpcId>
        <SubnetGroupStatus>Complete</SubnetGroupStatus>
        <Subnets>
          <Subnet>
            <SubnetIdentifier>subnet-00000000000000007</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1a</Name>
            </SubnetAvailabilityZone>
            <SubnetOutpost></SubnetOutpost>
            <SubnetStatus>Active</SubnetStatus>
          </Subnet>
          <Subnet>
            <SubnetIdentifier>subnet-00000000000000009</SubnetIdentifier>
            <SubnetAvailabilityZone>
… (49 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeEngineDefaultClusterParameters

Returns the default engine and system parameter information for the cluster database engine.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 95
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeEngineDefaultClusterParameters
&Version=2014-10-31
&DBParameterGroupFamily=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 48f1dab5-314c-49e8-a5a6-057aa80e96cb
content-length: 292
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>ParameterGroupFamily example is not a valid parameter group family</Message>
  </Error>
  <RequestId>48f1dab5-314c-49e8-a5a6-057aa80e96cb</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribeEngineDefaultParameters

Returns the default engine and system parameter information for the specified database engine.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 88
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeEngineDefaultParameters
&Version=2014-10-31
&DBParameterGroupFamily=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: cfaa6f09-3957-4a12-8caf-2f098a8598d5
content-length: 292
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>ParameterGroupFamily example is not a valid parameter group family</Message>
  </Error>
  <RequestId>cfaa6f09-3957-4a12-8caf-2f098a8598d5</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribeEventCategories

Displays a list of categories for all event source types, or, if specified, for a specified source type.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeEventCategories
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 3874
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeEventCategoriesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeEventCategoriesResult>
    <EventCategoriesMapList>
      <EventCategoriesMap xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <SourceType>db-snapshot</SourceType>
        <EventCategories>
          <EventCategory>notification</EventCategory>
          <EventCategory>creation</EventCategory>
          <EventCategory>deletion</EventCategory>
          <EventCategory>restoration</EventCategory>
        </EventCategories>
      </EventCategoriesMap>
      <EventCategoriesMap>
        <SourceType>db-parameter-group</SourceType>
        <EventCategories>
          <EventCategory>configuration change</EventCategory>
        </EventCategories>
      </EventCategoriesMap>
      <EventCategoriesMap>
        <SourceType>db-instance</SourceType>
… (98 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeEventSubscriptions

Lists all the subscription descriptions for a customer account.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeEventSubscriptions
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 336
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeEventSubscriptionsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeEventSubscriptionsResult>
    <EventSubscriptionsList></EventSubscriptionsList>
  </DescribeEventSubscriptionsResult>
  <ResponseMetadata>
    <RequestId>dab2c75f-af09-4ed2-860f-9c75a016131c</RequestId>
  </ResponseMetadata>
</DescribeEventSubscriptionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeEvents

Returns events related to DB instances, DB clusters, DB parameter groups, DB security groups, DB snapshots, DB cluster snapshots, and RDS Proxies for the past 14 days.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeEvents
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1646
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeEventsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeEventsResult>
    <Events>
      <Event xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <SourceIdentifier>docs-demo-pg</SourceIdentifier>
        <SourceType>db-instance</SourceType>
        <Message>DB instance created</Message>
        <EventCategories>
          <EventCategory>creation</EventCategory>
        </EventCategories>
        <Date>2026-11-26T18:30:48.203969Z</Date>
        <SourceArn>arn:aws:rds:us-east-1:000000000000:db:docs-demo-pg</SourceArn>
      </Event>
      <Event>
        <SourceIdentifier>docs-demo-pg-snap</SourceIdentifier>
        <SourceType>db-snapshot</SourceType>
        <Message>Creating manual snapshot</Message>
        <EventCategories>
          <EventCategory>creation</EventCategory>
        </EventCategories>
… (29 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeExportTasks

Returns information about a snapshot or cluster export to Amazon S3.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 45
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeExportTasks
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 286
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeExportTasksResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeExportTasksResult>
    <ExportTasks></ExportTasks>
  </DescribeExportTasksResult>
  <ResponseMetadata>
    <RequestId>ed9ef8c9-9f3b-4f23-b642-af44cb584a05</RequestId>
  </ResponseMetadata>
</DescribeExportTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeGlobalClusters

Returns information about Aurora global database clusters.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 48
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeGlobalClusters
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 304
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeGlobalClustersResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeGlobalClustersResult>
    <GlobalClusters></GlobalClusters>
  </DescribeGlobalClustersResult>
  <ResponseMetadata>
    <RequestId>4ef0c11e-125e-427e-8a26-b93ed8cf17f2</RequestId>
  </ResponseMetadata>
</DescribeGlobalClustersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIntegrations

Describe one or more zero-ETL integrations with Amazon Redshift.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeIntegrations
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 292
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeIntegrationsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeIntegrationsResult>
    <Integrations></Integrations>
  </DescribeIntegrationsResult>
  <ResponseMetadata>
    <RequestId>ef0c1cc9-e2a8-4792-9a61-dba71c99e561</RequestId>
  </ResponseMetadata>
</DescribeIntegrationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeOptionGroupOptions

Describes all available options for the specified engine.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeOptionGroupOptions
&Version=2014-10-31
&EngineName=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: 9f9b550a-98ed-437d-89e1-9c95eda9acb7
content-length: 308
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeOptionGroupOptions is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>9f9b550a-98ed-437d-89e1-9c95eda9acb7</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeOptionGroups

Describes the available option groups.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeOptionGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 17285
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeOptionGroupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeOptionGroupsResult>
    <OptionGroupsList>
      <OptionGroup xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <OptionGroupName>default:db2-ae-11-5</OptionGroupName>
        <OptionGroupDescription>Default option group for db2-ae 11.5</OptionGroupDescription>
        <EngineName>db2-ae</EngineName>
        <MajorEngineVersion>11.5</MajorEngineVersion>
        <Options></Options>
        <AllowsVpcAndNonVpcInstanceMemberships>true</AllowsVpcAndNonVpcInstanceMemberships>
        <OptionGroupArn>arn:aws:rds:us-east-1:000000000000:og:default:db2-ae-11-5</OptionGroupArn>
      </OptionGroup>
      <OptionGroup>
        <OptionGroupName>default:db2-ae-12-1</OptionGroupName>
        <OptionGroupDescription>Default option group for db2-ae 12.1</OptionGroupDescription>
        <EngineName>db2-ae</EngineName>
        <MajorEngineVersion>12.1</MajorEngineVersion>
        <Options></Options>
        <AllowsVpcAndNonVpcInstanceMemberships>true</AllowsVpcAndNonVpcInstanceMemberships>
        <OptionGroupArn>arn:aws:rds:us-east-1:000000000000:og:default:db2-ae-12-1</OptionGroupArn>
… (331 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeOrderableDBInstanceOptions

Describes the orderable DB instance options for a specified DB engine.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeOrderableDBInstanceOptions
&Version=2014-10-31
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c4e14d39-5def-4ef6-8d20-e25847c161ef
content-length: 243
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid DB engine</Message>
  </Error>
  <RequestId>c4e14d39-5def-4ef6-8d20-e25847c161ef</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribePendingMaintenanceActions

Returns a list of resources (for example, DB instances) that have at least one pending maintenance action.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribePendingMaintenanceActions
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 370
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribePendingMaintenanceActionsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribePendingMaintenanceActionsResult>
    <PendingMaintenanceActions></PendingMaintenanceActions>
  </DescribePendingMaintenanceActionsResult>
  <ResponseMetadata>
    <RequestId>8409a050-ac02-4fa9-a386-775e96019dd1</RequestId>
  </ResponseMetadata>
</DescribePendingMaintenanceActionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReservedDBInstances

Returns information about reserved DB instances for this account, or about a specified reserved DB instance.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeReservedDBInstances
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 334
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeReservedDBInstancesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeReservedDBInstancesResult>
    <ReservedDBInstances></ReservedDBInstances>
  </DescribeReservedDBInstancesResult>
  <ResponseMetadata>
    <RequestId>8ee037bf-8c60-4c3a-826e-80ba14754bfa</RequestId>
  </ResponseMetadata>
</DescribeReservedDBInstancesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReservedDBInstancesOfferings

Lists available reserved DB instance offerings.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeReservedDBInstancesOfferings
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 62889
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeReservedDBInstancesOfferingsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeReservedDBInstancesOfferingsResult>
    <Marker>MmFiMzA2ZjItMjEzMi0zYjkxLWJlYzctOGFmOGNmMWRmZjli</Marker>
    <ReservedDBInstancesOfferings>
      <ReservedDBInstancesOffering xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <ReservedDBInstancesOfferingId>727096c2-0e6f-eaab-6c48-036d3fd6b744</ReservedDBInstancesOfferingId>
        <DBInstanceClass>db.t3.micro</DBInstanceClass>
        <Duration>31536000</Duration>
        <FixedPrice>50.04</FixedPrice>
        <UsagePrice>0.0</UsagePrice>
        <CurrencyCode>USD</CurrencyCode>
        <ProductDescription>aurora mysql</ProductDescription>
        <OfferingType>Partial Upfront</OfferingType>
        <MultiAZ>false</MultiAZ>
        <RecurringCharges>
          <RecurringCharge>
            <RecurringChargeAmount>0.00571</RecurringChargeAmount>
            <RecurringChargeFrequency>Hourly</RecurringChargeFrequency>
          </RecurringCharge>
        </RecurringCharges>
… (1690 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeServerlessV2PlatformVersions

Describes the properties of specific platform versions for Aurora Serverless v2.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 62
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeServerlessV2PlatformVersions
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: 6b5a579c-4961-4720-b70b-1603620ec220
content-length: 318
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeServerlessV2PlatformVersions is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>6b5a579c-4961-4720-b70b-1603620ec220</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeSourceRegions

Returns a list of the source Amazon Web Services Regions where the current Amazon Web Services Region can create a read replica, copy a DB snapshot from, or replicate automated backups from.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeSourceRegions
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: e38cb3ed-90f8-4adc-8408-b508b8486720
content-length: 303
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeSourceRegions is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>e38cb3ed-90f8-4adc-8408-b508b8486720</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeTenantDatabases

Describes the tenant databases in a DB instance that uses the multi-tenant configuration.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 49
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeTenantDatabases
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 310
date: Thu, 26 Nov 2026 18:30:49 GMT

<DescribeTenantDatabasesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeTenantDatabasesResult>
    <TenantDatabases></TenantDatabases>
  </DescribeTenantDatabasesResult>
  <ResponseMetadata>
    <RequestId>67ec8431-9eaa-445a-ab69-4bd9a9a53400</RequestId>
  </ResponseMetadata>
</DescribeTenantDatabasesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeValidDBInstanceModifications

You can call DescribeValidDBInstanceModifications to learn what modifications you can make to your DB instance.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeValidDBInstanceModifications
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 75ad7d8c-1737-438c-8ff9-c75b138c89e9
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>75ad7d8c-1737-438c-8ff9-c75b138c89e9</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisableHttpEndpoint

Disables the HTTP endpoint for the specified DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 120
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DisableHttpEndpoint
&Version=2014-10-31
&ResourceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2f0f878f-a54d-4446-9fd9-59f1865bf373
content-length: 299
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid resource name: arn:aws:rds:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>2f0f878f-a54d-4446-9fd9-59f1865bf373</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DownloadDBLogFilePortion

Downloads all or a portion of the specified log file, up to 1 MB in size.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DownloadDBLogFilePortion
&Version=2014-10-31
&DBInstanceIdentifier=example
&LogFileName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 9dac1d75-b025-46df-ac01-f6787deada38
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>9dac1d75-b025-46df-ac01-f6787deada38</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### EnableHttpEndpoint

Enables the HTTP endpoint for the DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 119
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=EnableHttpEndpoint
&Version=2014-10-31
&ResourceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: ad34bdae-5a0d-4c64-9032-5b8945caeeba
content-length: 299
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid resource name: arn:aws:rds:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>ad34bdae-5a0d-4c64-9032-5b8945caeeba</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### FailoverDBCluster

Forces a failover for a DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=FailoverDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d35b5698-af4c-4da9-9839-76143d101e5b
content-length: 295
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>The source cluster could not be found or cannot be accessed: example</Message>
  </Error>
  <RequestId>d35b5698-af4c-4da9-9839-76143d101e5b</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### FailoverGlobalCluster

Promotes the specified secondary DB cluster to be the primary DB cluster in the global database cluster to fail over or switch over a global database.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 113
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=FailoverGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
&TargetDbClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 432a5568-f2eb-476f-8044-653b2695cc9d
content-length: 275
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalClusterNotFoundFault</Code>
    <Message>Global cluster &apos;example&apos; not found</Message>
  </Error>
  <RequestId>432a5568-f2eb-476f-8044-653b2695cc9d</RequestId>
</ErrorResponse>
```

`404` `GlobalClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTagsForResource

Lists all tags on an Amazon RDS resource.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 66
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListTagsForResource
&Version=2014-10-31
&ResourceName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 93a547dc-f861-40f5-8cd7-427de6e80e2a
content-length: 256
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid resource name: example</Message>
  </Error>
  <RequestId>93a547dc-f861-40f5-8cd7-427de6e80e2a</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyActivityStream

Changes the audit policy state of a database activity stream to either locked (default) or unlocked.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 46
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyActivityStream
&Version=2014-10-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 210dba06-ab41-474c-b1af-aeb807c56aae
content-length: 291
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter ResourceArn must be provided and must not be blank.</Message>
  </Error>
  <RequestId>210dba06-ab41-474c-b1af-aeb807c56aae</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyCertificates

Override the system-default Secure Sockets Layer/Transport Layer Security (SSL/TLS) certificate for Amazon RDS for new DB instances, or remove the override.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyCertificates
&Version=2014-10-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 6d088f94-b016-4bf6-9522-2f6e17ac35e4
content-length: 388
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>Specify either CertificateIdentifier, to override the default certificate for new DB instances, or RemoveCustomerOverride, to remove an override that is set</Message>
  </Error>
  <RequestId>6d088f94-b016-4bf6-9522-2f6e17ac35e4</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### ModifyCurrentDBClusterCapacity

Set the capacity of an Aurora Serverless v1 DB cluster to a specific value.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyCurrentDBClusterCapacity
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: a2e0e487-57d2-4324-92f2-0eb83912a534
content-length: 255
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>DBCluster example not found.</Message>
  </Error>
  <RequestId>a2e0e487-57d2-4324-92f2-0eb83912a534</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyCustomDBEngineVersion

Modifies the status of a custom engine version (CEV).

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyCustomDBEngineVersion
&Version=2014-10-31
&Engine=example
&EngineVersion=example
```

```http
HTTP/1.1 403 Forbidden
content-type: text/xml
x-amzn-requestid: c0f88e2b-dabb-4716-b883-d353738e8089
content-length: 318
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>The custom engine version you requested may not exist or you don&apos;t have the required permission.</Message>
  </Error>
  <RequestId>c0f88e2b-dabb-4716-b883-d353738e8089</RequestId>
</ErrorResponse>
```

`403` `AccessDenied`: the request was refused by authorization.

### ModifyDBCapacityReservation

Modifies a DB capacity reservation.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 93
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBCapacityReservation
&Version=2014-10-31
&DBCapacityReservationIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 38b0f9ca-95db-4318-9cd7-4fea50bd960f
content-length: 263
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>No modifications were requested</Message>
  </Error>
  <RequestId>38b0f9ca-95db-4318-9cd7-4fea50bd960f</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### ModifyDBCluster

Modifies the settings of an Amazon Aurora DB cluster or a Multi-AZ DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: a628842c-e9f3-4c68-bb35-19a7148de494
content-length: 246
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>DBCluster not found</Message>
  </Error>
  <RequestId>a628842c-e9f3-4c68-bb35-19a7148de494</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyDBClusterEndpoint

Modifies the properties of an endpoint in an Amazon Aurora DB cluster.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 85
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBClusterEndpoint
&Version=2014-10-31
&DBClusterEndpointIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b504f2f0-a634-48a7-b63b-99a58a5970e2
content-length: 263
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>No modifications were requested</Message>
  </Error>
  <RequestId>b504f2f0-a634-48a7-b63b-99a58a5970e2</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### ModifyDBClusterParameterGroup

Modifies the parameters of a DB cluster parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBClusterParameterGroup
&Version=2014-10-31
&DBClusterParameterGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2732a7ac-45b1-4e9d-bbec-647ac5b022e3
content-length: 258
date: Thu, 26 Nov 2026 18:30:49 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Please specify a value for parameter &apos;parameters&apos;.</Message>
  </Error>
  <RequestId>2732a7ac-45b1-4e9d-bbec-647ac5b022e3</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyDBClusterSnapshotAttribute

Adds an attribute and values to, or removes an attribute and values from, a manual DB cluster snapshot.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 116
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBClusterSnapshotAttribute
&Version=2014-10-31
&DBClusterSnapshotIdentifier=example
&AttributeName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 58ec0236-5758-4741-8645-90cc3f8f4ce6
content-length: 320
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationException</Code>
    <Message>invalid value &apos;example&apos; for AttributeName: the only valid value is &apos;restore&apos;</Message>
  </Error>
  <RequestId>58ec0236-5758-4741-8645-90cc3f8f4ce6</RequestId>
</ErrorResponse>
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ModifyDBInstance

Modifies settings for a DB instance.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 055e76b3-8afc-401f-9dd3-8f7ff61bde1e
content-length: 253
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DB instance not found: example</Message>
  </Error>
  <RequestId>055e76b3-8afc-401f-9dd3-8f7ff61bde1e</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyDBParameterGroup

Modifies the parameters of a DB parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBParameterGroup
&Version=2014-10-31
&DBParameterGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 8e0597b2-039c-4c01-9c09-c038c6883f90
content-length: 258
date: Thu, 26 Nov 2026 18:30:49 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Please specify a value for parameter &apos;parameters&apos;.</Message>
  </Error>
  <RequestId>8e0597b2-039c-4c01-9c09-c038c6883f90</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyDBProxy

Changes the settings for an existing DB proxy.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 59
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBProxy
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 50d13582-20b3-4fab-bff8-7eea2dfb7494
content-length: 263
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBProxyNotFoundFault</Code>
    <Message>DBProxy &apos;example&apos; not found.</Message>
  </Error>
  <RequestId>50d13582-20b3-4fab-bff8-7eea2dfb7494</RequestId>
</ErrorResponse>
```

`404` `DBProxyNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyDBProxyEndpoint

Changes the settings for an existing DB proxy endpoint.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBProxyEndpoint
&Version=2014-10-31
&DBProxyEndpointName=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: c8c160d8-c213-43db-ba78-3e211f1e73e4
content-length: 303
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyDBProxyEndpoint is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>c8c160d8-c213-43db-ba78-3e211f1e73e4</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyDBProxyTargetGroup

Modifies the properties of a DBProxyTargetGroup .

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBProxyTargetGroup
&Version=2014-10-31
&TargetGroupName=example
&DBProxyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: b92efb09-bb73-4cc1-ac0f-a781712833e8
content-length: 263
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBProxyNotFoundFault</Code>
    <Message>DBProxy &apos;example&apos; not found.</Message>
  </Error>
  <RequestId>b92efb09-bb73-4cc1-ac0f-a781712833e8</RequestId>
</ErrorResponse>
```

`404` `DBProxyNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyDBRecommendation

Updates the recommendation status and recommended action status for the specified recommendation.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBRecommendation
&Version=2014-10-31
&RecommendationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: fa7e088e-e0c5-43d2-890b-69acceb116c2
content-length: 304
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyDBRecommendation is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>fa7e088e-e0c5-43d2-890b-69acceb116c2</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyDBShardGroup

Modifies the settings of an Aurora Limitless Database DB shard group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBShardGroup
&Version=2014-10-31
&DBShardGroupIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 40fe441f-944c-473e-8fd1-5300c93ad139
content-length: 310
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBShardGroupNotFound</Code>
    <Message>The DB shard group with requested identifier in requested account doesn&apos;t exist.</Message>
  </Error>
  <RequestId>40fe441f-944c-473e-8fd1-5300c93ad139</RequestId>
</ErrorResponse>
```

`404` `DBShardGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyDBSnapshot

Updates a manual DB snapshot with a new engine version.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBSnapshot
&Version=2014-10-31
&DBSnapshotIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: cd028268-df59-4bee-b681-a1b79a18d2d2
content-length: 298
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyDBSnapshot is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>cd028268-df59-4bee-b681-a1b79a18d2d2</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyDBSnapshotAttribute

Adds an attribute and values to, or removes an attribute and values from, a manual DB snapshot.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 102
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBSnapshotAttribute
&Version=2014-10-31
&DBSnapshotIdentifier=example
&AttributeName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e49cebf3-1c1b-457f-8e47-fcfc0b59ab05
content-length: 320
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationException</Code>
    <Message>invalid value &apos;example&apos; for AttributeName: the only valid value is &apos;restore&apos;</Message>
  </Error>
  <RequestId>e49cebf3-1c1b-457f-8e47-fcfc0b59ab05</RequestId>
</ErrorResponse>
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ModifyDBSubnetGroup

Modifies an existing DB subnet group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 108
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyDBSubnetGroup
&Version=2014-10-31
&DBSubnetGroupName=example
&SubnetIds.SubnetIdentifier.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2000d6e2-1ebc-4f9a-a7b3-b798b724877e
content-length: 323
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidSubnet</Code>
    <Message>&apos;example&apos; is not an EC2 subnet id (expected subnet- followed by 8 or 17 hexadecimal characters)</Message>
  </Error>
  <RequestId>2000d6e2-1ebc-4f9a-a7b3-b798b724877e</RequestId>
</ErrorResponse>
```

`400` `InvalidSubnet`.

### ModifyEventSubscription

Modifies an existing RDS event notification subscription.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 74
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyEventSubscription
&Version=2014-10-31
&SubscriptionName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 30e6c045-d5e2-4ff9-a210-751d629b06d5
content-length: 262
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>SubscriptionNotFound</Code>
    <Message>Event Subscription example not found.</Message>
  </Error>
  <RequestId>30e6c045-d5e2-4ff9-a210-751d629b06d5</RequestId>
</ErrorResponse>
```

`404` `SubscriptionNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyGlobalCluster

Modifies a setting for an Amazon Aurora global database cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: f2cb64a2-f291-4558-bc9d-ad1244c90603
content-length: 275
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalClusterNotFoundFault</Code>
    <Message>Global cluster &apos;example&apos; not found</Message>
  </Error>
  <RequestId>f2cb64a2-f291-4558-bc9d-ad1244c90603</RequestId>
</ErrorResponse>
```

`404` `GlobalClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyIntegration

Modifies a zero-ETL integration with Amazon Redshift.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyIntegration
&Version=2014-10-31
&IntegrationIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 7c19791d-b004-4480-bf15-de478d2fddde
content-length: 263
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>No modifications were requested</Message>
  </Error>
  <RequestId>7c19791d-b004-4480-bf15-de478d2fddde</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### ModifyOptionGroup

Modifies an existing option group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyOptionGroup
&Version=2014-10-31
&OptionGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 79aa8829-bae7-47f5-96db-79969cac3c3e
content-length: 282
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>At least one option must be added, modified, or removed.</Message>
  </Error>
  <RequestId>79aa8829-bae7-47f5-96db-79969cac3c3e</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyTenantDatabase

Modifies an existing tenant database in a DB instance.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyTenantDatabase
&Version=2014-10-31
&DBInstanceIdentifier=example
&TenantDBName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 9f27d31d-9a4f-4c4e-98a4-828643abbee4
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>9f27d31d-9a4f-4c4e-98a4-828643abbee4</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PromoteReadReplica

Promotes a read replica DB instance to a standalone DB instance.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PromoteReadReplica
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: e6894d85-1556-4d73-b848-b642b1428369
content-length: 253
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DB Instance not found: example</Message>
  </Error>
  <RequestId>e6894d85-1556-4d73-b848-b642b1428369</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PromoteReadReplicaDBCluster

Promotes a read replica DB cluster to a standalone DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PromoteReadReplicaDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 5d1a5ee4-cb2c-456d-8828-8dbd51867a21
content-length: 295
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>The source cluster could not be found or cannot be accessed: example</Message>
  </Error>
  <RequestId>5d1a5ee4-cb2c-456d-8828-8dbd51867a21</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PurchaseReservedDBInstancesOffering

Purchases a reserved DB instance offering.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PurchaseReservedDBInstancesOffering
&Version=2014-10-31
&ReservedDBInstancesOfferingId=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 498c45f8-00e2-41f9-a65f-28e4bbdecf1b
content-length: 285
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>ReservedDBInstancesOfferingNotFound</Code>
    <Message>ReservedDBInstanceOffering example not found.</Message>
  </Error>
  <RequestId>498c45f8-00e2-41f9-a65f-28e4bbdecf1b</RequestId>
</ErrorResponse>
```

`404` `ReservedDBInstancesOfferingNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RebootDBCluster

You might need to reboot your DB cluster, usually for maintenance reasons.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RebootDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 157e0f5c-6db1-44dd-afa1-93393ff85ae6
content-length: 295
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>The source cluster could not be found or cannot be accessed: example</Message>
  </Error>
  <RequestId>157e0f5c-6db1-44dd-afa1-93393ff85ae6</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RebootDBInstance

You might need to reboot your DB instance, usually for maintenance reasons.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 71
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RebootDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 62bd6155-e13b-4dc2-bffa-2da4366659ce
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>62bd6155-e13b-4dc2-bffa-2da4366659ce</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RebootDBShardGroup

You might need to reboot your DB shard group, usually for maintenance reasons.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RebootDBShardGroup
&Version=2014-10-31
&DBShardGroupIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 949b729c-0194-4d8c-b24e-4a9d3d7eafd4
content-length: 272
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBShardGroupNotFound</Code>
    <Message>DB shard group not found or can not be accessed</Message>
  </Error>
  <RequestId>949b729c-0194-4d8c-b24e-4a9d3d7eafd4</RequestId>
</ErrorResponse>
```

`404` `DBShardGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RegisterDBProxyTargets

Associate one or more DBProxyTarget data structures with a DBProxyTargetGroup .

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RegisterDBProxyTargets
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a6b362cf-df60-47a0-8ca7-24419515387f
content-length: 277
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>Missing DB instance or DB cluster identifier.</Message>
  </Error>
  <RequestId>a6b362cf-df60-47a0-8ca7-24419515387f</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RemoveFromGlobalCluster

Detaches an Aurora secondary cluster from an Aurora global database cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 109
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveFromGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
&DbClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 17b7fbce-ef85-4041-a083-b37268656e50
content-length: 275
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalClusterNotFoundFault</Code>
    <Message>Global cluster &apos;example&apos; not found</Message>
  </Error>
  <RequestId>17b7fbce-ef85-4041-a083-b37268656e50</RequestId>
</ErrorResponse>
```

`404` `GlobalClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveRoleFromDBCluster

Removes the asssociation of an Amazon Web Services Identity and Access Management (IAM) role from a DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 106
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveRoleFromDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
&RoleArn=example0000000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 651d271e-9ba7-45e5-bdec-d66c8ae98d67
content-length: 401
date: Thu, 26 Nov 2026 18:30:49 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>1 validation error detected: Value &apos;example0000000000000&apos; at &apos;roleArn&apos; failed to satisfy constraint: Member must satisfy regular expression pattern: arn:aws[a-z-]*:iam::[0-9]*:role/.*</Message>
  </Error>
  <RequestId>651d271e-9ba7-45e5-bdec-d66c8ae98d67</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### RemoveRoleFromDBInstance

Disassociates an Amazon Web Services Identity and Access Management (IAM) role from a DB instance.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 170
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveRoleFromDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
&RoleArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&FeatureName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 6ceaffd2-1de4-41ab-b136-e0d4d0269b25
content-length: 294
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid Role ARN: arn:aws:rds:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>6ceaffd2-1de4-41ab-b136-e0d4d0269b25</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### RemoveSourceIdentifierFromSubscription

Removes a source identifier from an existing RDS event notification subscription.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 114
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveSourceIdentifierFromSubscription
&Version=2014-10-31
&SubscriptionName=example
&SourceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 445e6ae8-3e1d-4c7b-a959-2cfc09d41a10
content-length: 262
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>SubscriptionNotFound</Code>
    <Message>Event Subscription example not found.</Message>
  </Error>
  <RequestId>445e6ae8-3e1d-4c7b-a959-2cfc09d41a10</RequestId>
</ErrorResponse>
```

`404` `SubscriptionNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveTagsFromResource

Removes metadata tags from an Amazon RDS resource.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 94
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveTagsFromResource
&Version=2014-10-31
&ResourceName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9e5a0258-a253-4613-a164-d7c1d8438222
content-length: 256
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid resource name: example</Message>
  </Error>
  <RequestId>9e5a0258-a253-4613-a164-d7c1d8438222</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ResetDBClusterParameterGroup

Modifies the parameters of a DB cluster parameter group to the default value.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ResetDBClusterParameterGroup
&Version=2014-10-31
&DBClusterParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: bb32e709-b585-41cf-8700-6037aa4bf35a
content-length: 271
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBClusterParameterGroup not found: example</Message>
  </Error>
  <RequestId>bb32e709-b585-41cf-8700-6037aa4bf35a</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ResetDBParameterGroup

Modifies the parameters of a DB parameter group to the engine/system default value.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ResetDBParameterGroup
&Version=2014-10-31
&DBParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 0c3bb9d2-1a7e-4b8f-9661-2b04f7f0e604
content-length: 264
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBParameterGroup not found: example</Message>
  </Error>
  <RequestId>0c3bb9d2-1a7e-4b8f-9661-2b04f7f0e604</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RestoreDBClusterFromS3

Creates an Amazon Aurora DB cluster from MySQL data stored in an Amazon S3 bucket.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 266
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RestoreDBClusterFromS3
&Version=2014-10-31
&DBClusterIdentifier=example
&Engine=example
&MasterUsername=example
&SourceEngine=example
&SourceEngineVersion=example
&S3BucketName=example
&S3IngestionRoleArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 885a52c2-e5db-4aa6-aa4c-b564f6cabbe6
content-length: 295
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>You can&apos;t restore to example from an S3 backup of example.</Message>
  </Error>
  <RequestId>885a52c2-e5db-4aa6-aa4c-b564f6cabbe6</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RestoreDBClusterFromSnapshot

Creates a new DB cluster from a DB snapshot or DB cluster snapshot.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 124
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RestoreDBClusterFromSnapshot
&Version=2014-10-31
&DBClusterIdentifier=example
&SnapshotIdentifier=example
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 7280b2ca-8d48-4699-bf44-588cc63f873c
content-length: 243
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid DB engine</Message>
  </Error>
  <RequestId>7280b2ca-8d48-4699-bf44-588cc63f873c</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### RestoreDBClusterToPointInTime

Restores a DB cluster to an arbitrary point in time.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RestoreDBClusterToPointInTime
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 1b11b8e9-85d2-4b2e-8043-3471c6b84e3c
content-length: 362
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>either SourceDBClusterIdentifier or SourceDbClusterResourceId must be specified: a point-in-time restore reads a source DB cluster</Message>
  </Error>
  <RequestId>1b11b8e9-85d2-4b2e-8043-3471c6b84e3c</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RestoreDBInstanceFromDBSnapshot

Creates a new DB instance from a DB snapshot.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 86
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RestoreDBInstanceFromDBSnapshot
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 8cc5471c-680a-4faa-98ec-707591619925
content-length: 324
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>DBSnapshotIdentifier must be specified when DBClusterSnapshotIdentifier isn&apos;t specified</Message>
  </Error>
  <RequestId>8cc5471c-680a-4faa-98ec-707591619925</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RestoreDBInstanceFromS3

Amazon Relational Database Service (Amazon RDS) supports importing MySQL databases by using backup files.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 269
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RestoreDBInstanceFromS3
&Version=2014-10-31
&DBInstanceIdentifier=example
&DBInstanceClass=example
&Engine=example
&SourceEngine=example
&SourceEngineVersion=example
&S3BucketName=example
&S3IngestionRoleArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 83114e1e-a226-4f0c-b7df-d227c6a35627
content-length: 295
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>You can&apos;t restore to example from an S3 backup of example.</Message>
  </Error>
  <RequestId>83114e1e-a226-4f0c-b7df-d227c6a35627</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RestoreDBInstanceToPointInTime

Restores a DB instance to an arbitrary point in time.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RestoreDBInstanceToPointInTime
&Version=2014-10-31
&TargetDBInstanceIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 6e5af218-c0ff-4ec7-af0a-7e85f7859127
content-length: 313
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>If UseLatestRestoreTime is not true, the RestoreTime parameter must be specified.</Message>
  </Error>
  <RequestId>6e5af218-c0ff-4ec7-af0a-7e85f7859127</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RevokeDBSecurityGroupIngress

Revokes ingress from a DBSecurityGroup for previously authorized IP ranges or EC2 or VPC security groups.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 82
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RevokeDBSecurityGroupIngress
&Version=2014-10-31
&DBSecurityGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: acd30412-d395-4d85-b636-e7b23a59f4bd
content-length: 262
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSecurityGroupNotFound</Code>
    <Message>DBSecurityGroup not found: example</Message>
  </Error>
  <RequestId>acd30412-d395-4d85-b636-e7b23a59f4bd</RequestId>
</ErrorResponse>
```

`404` `DBSecurityGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartActivityStream

Starts a database activity stream to monitor activity on the database.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 147
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StartActivityStream
&Version=2014-10-31
&ResourceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Mode=sync
&KmsKeyId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: c47c91c0-f478-4320-bcfe-d9246d81a4e8
content-length: 336
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>KMSKeyNotAccessibleFault</Code>
    <Message>The specified KMS key [example] does not exist, is not enabled or you do not have permissions to access it.</Message>
  </Error>
  <RequestId>c47c91c0-f478-4320-bcfe-d9246d81a4e8</RequestId>
</ErrorResponse>
```

`400` `KMSKeyNotAccessibleFault`.

### StartDBCluster

Starts an Amazon Aurora DB cluster that was stopped using the Amazon Web Services console, the stop-db-cluster CLI command, or the StopDBCluster operation.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StartDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 33bcc739-2a8f-4bd3-9f04-897340334a9a
content-length: 255
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>DBCluster example not found.</Message>
  </Error>
  <RequestId>33bcc739-2a8f-4bd3-9f04-897340334a9a</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartDBInstance

Starts an Amazon RDS DB instance that was stopped using the Amazon Web Services console, the stop-db-instance CLI command, or the StopDBInstance operation.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 70
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StartDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 2717a7d1-36d1-407a-9eb0-f1dcc50efc92
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance example not found.</Message>
  </Error>
  <RequestId>2717a7d1-36d1-407a-9eb0-f1dcc50efc92</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartDBInstanceAutomatedBackupsReplication

Enables replication of automated backups to a different Amazon Web Services Region.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 151
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StartDBInstanceAutomatedBackupsReplication
&Version=2014-10-31
&SourceDBInstanceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f71badc4-3bee-4af3-9519-dd8659a3818c
content-length: 351
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter SourceDBInstanceArn must name the ARN of a DB instance; got arn:aws:rds:us-east-1:000000000000:example/example.</Message>
  </Error>
  <RequestId>f71badc4-3bee-4af3-9519-dd8659a3818c</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### StartExportTask

Starts an export of DB snapshot or DB cluster data to Amazon S3.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 255
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StartExportTask
&Version=2014-10-31
&ExportTaskIdentifier=example
&SourceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&S3BucketName=example
&IamRoleArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&KmsKeyId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 916c80af-b71b-4c9f-9766-9b734f4dee75
content-length: 283
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>KMSKeyNotAccessibleFault</Code>
    <Message>The KMS key example doesn&apos;t exist or is disabled.</Message>
  </Error>
  <RequestId>916c80af-b71b-4c9f-9766-9b734f4dee75</RequestId>
</ErrorResponse>
```

`400` `KMSKeyNotAccessibleFault`.

### StopActivityStream

Stops a database activity stream that was started using the Amazon Web Services console, the start-activity-stream CLI command, or the StartActivityStream operation.

*Implemented as of emulator 1.0.629 (captured 2026-11-26).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 119
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StopActivityStream
&Version=2014-10-31
&ResourceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 1fee975a-7d8b-4a35-8b7e-a81e201acba5
content-length: 299
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid resource name: arn:aws:rds:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>1fee975a-7d8b-4a35-8b7e-a81e201acba5</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### StopDBCluster

Stops an Amazon Aurora DB cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StopDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: a076cab9-0135-48d6-9e84-4aa10786184f
content-length: 255
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>DBCluster example not found.</Message>
  </Error>
  <RequestId>a076cab9-0135-48d6-9e84-4aa10786184f</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StopDBInstance

Stops an Amazon RDS DB instance temporarily.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StopDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 7e206fc5-0063-4eff-9f8a-353c34773f85
content-length: 252
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance example not found.</Message>
  </Error>
  <RequestId>7e206fc5-0063-4eff-9f8a-353c34773f85</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StopDBInstanceAutomatedBackupsReplication

Stops automated backup replication for a DB instance.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 150
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StopDBInstanceAutomatedBackupsReplication
&Version=2014-10-31
&SourceDBInstanceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d9303c26-9baf-4dfb-a1cc-80c4ec56068b
content-length: 351
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter SourceDBInstanceArn must name the ARN of a DB instance; got arn:aws:rds:us-east-1:000000000000:example/example.</Message>
  </Error>
  <RequestId>d9303c26-9baf-4dfb-a1cc-80c4ec56068b</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### SwitchoverBlueGreenDeployment

Switches over a blue/green deployment.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 93
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SwitchoverBlueGreenDeployment
&Version=2014-10-31
&BlueGreenDeploymentIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e1d2e2e7-35f8-4866-9781-6b82c631607f
content-length: 275
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid blue-green deployment identifier: example</Message>
  </Error>
  <RequestId>e1d2e2e7-35f8-4866-9781-6b82c631607f</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### SwitchoverGlobalCluster

Switches over the specified secondary DB cluster to be the new primary DB cluster in the global database cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 115
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SwitchoverGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
&TargetDbClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: e2a8d302-3a5b-426c-9ce3-b06a553c2f5f
content-length: 275
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalClusterNotFoundFault</Code>
    <Message>Global cluster &apos;example&apos; not found</Message>
  </Error>
  <RequestId>e2a8d302-3a5b-426c-9ce3-b06a553c2f5f</RequestId>
</ErrorResponse>
```

`404` `GlobalClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SwitchoverReadReplica

Switches over an Oracle standby database in an Oracle Data Guard environment, making it the new primary database.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183049Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=SwitchoverReadReplica
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 44a71c3b-e7a4-4105-99e8-e883d063e76d
content-length: 266
date: Thu, 26 Nov 2026 18:30:49 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>The DB instance wasn&apos;t found: example.</Message>
  </Error>
  <RequestId>44a71c3b-e7a4-4105-99e8-e883d063e76d</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/rds/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with a form-encoded body: `Action` names the operation, `Version` the API version, and every input member is a flattened key such as `Tags.member.1.Key`.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon Relational Database Service answers in AWS: XML wrapped in `<OperationResponse><OperationResult>`. Errors captured on this page are an XML document rooted at `<ErrorResponse>`.

### Local endpoints

Where AWS returns a regional hostname, the emulator returns a local address: the DB instance endpoint (`DescribeDBInstances` → `Endpoint`) is `127.0.0.1:<port>`, a port the emulator opened for that resource. Those addresses are local to the emulator; the [Docker setup](README.md#running-the-emulator-in-docker) publishes only port 9297, so from your machine only the API endpoint is reachable.
