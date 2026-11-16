# Amazon Relational Database Service

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `RDS`
- **sigv4 signing name:** `rds`
- **Endpoint prefix:** `rds`
- **ARN namespace:** `rds`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `rds`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

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
X-Amz-Date: 20261116T121017Z
Content-Length: 225
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
content-length: 4297
date: Mon, 16 Nov 2026 12:10:17 GMT

<CreateDBInstanceResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <CreateDBInstanceResult>
    <DBInstance xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
      <DBInstanceClass>db.t3.micro</DBInstanceClass>
      <Engine>postgres</Engine>
      <DBInstanceStatus>creating</DBInstanceStatus>
      <MasterUsername>app</MasterUsername>
      <Endpoint>
        <Address>127.0.0.1</Address>
        <Port>43985</Port>
        <HostedZoneId>Z2R2ITUGPM61AM</HostedZoneId>
      </Endpoint>
      <AllocatedStorage>20</AllocatedStorage>
      <InstanceCreateTime>2026-11-16T12:10:17.925369Z</InstanceCreateTime>
      <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
      <BackupRetentionPeriod>1</BackupRetentionPeriod>
      <DBSecurityGroups/>
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
            <SubnetOutpost/>
            <SubnetStatus>Active</SubnetStatus>
          </Subnet>
          <Subnet>
            <SubnetIdentifier>subnet-00000000000000009</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1b</Name>
            </SubnetAvailabilityZone>
            <SubnetOutpost/>
            <SubnetStatus>Active</SubnetStatus>
          </Subnet>
          <Subnet>
            <SubnetIdentifier>subnet-0000000000000000b</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1c</Name>
            </SubnetAvailabilityZone>
            <SubnetOutpost/>
            <SubnetStatus>Active</SubnetStatus>
… (57 more lines)
```

### 2. DescribeDBInstances

Poll until `available`. The endpoint is then a local address and port (`127.0.0.1:<port>`) that the emulator opened for the instance.

*The emulator was asked 2 times, about two seconds apart; the last exchange is shown.*

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121019Z
Content-Length: 79
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBInstances
&Version=2014-10-31
&DBInstanceIdentifier=docs-demo-pg
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 4317
date: Mon, 16 Nov 2026 12:10:19 GMT

<DescribeDBInstancesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBInstancesResult>
    <DBInstances>
      <DBInstance xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
        <DBInstanceClass>db.t3.micro</DBInstanceClass>
        <Engine>postgres</Engine>
        <DBInstanceStatus>available</DBInstanceStatus>
        <MasterUsername>app</MasterUsername>
        <Endpoint>
          <Address>127.0.0.1</Address>
          <Port>43985</Port>
          <HostedZoneId>Z2R2ITUGPM61AM</HostedZoneId>
        </Endpoint>
        <AllocatedStorage>20</AllocatedStorage>
        <InstanceCreateTime>2026-11-16T12:10:17.925369Z</InstanceCreateTime>
        <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
        <BackupRetentionPeriod>1</BackupRetentionPeriod>
        <DBSecurityGroups/>
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
              <SubnetOutpost/>
              <SubnetStatus>Active</SubnetStatus>
            </Subnet>
            <Subnet>
              <SubnetIdentifier>subnet-00000000000000009</SubnetIdentifier>
              <SubnetAvailabilityZone>
                <Name>us-east-1b</Name>
              </SubnetAvailabilityZone>
              <SubnetOutpost/>
              <SubnetStatus>Active</SubnetStatus>
            </Subnet>
            <Subnet>
              <SubnetIdentifier>subnet-0000000000000000b</SubnetIdentifier>
              <SubnetAvailabilityZone>
                <Name>us-east-1c</Name>
              </SubnetAvailabilityZone>
              <SubnetOutpost/>
… (57 more lines)
```

### 3. CreateDBSnapshot

A manual snapshot starts in `creating`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121020Z
Content-Length: 115
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateDBSnapshot
&Version=2014-10-31
&DBSnapshotIdentifier=docs-demo-pg-snap
&DBInstanceIdentifier=docs-demo-pg
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1295
date: Mon, 16 Nov 2026 12:10:20 GMT

<CreateDBSnapshotResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <CreateDBSnapshotResult>
    <DBSnapshot xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <DBSnapshotIdentifier>docs-demo-pg-snap</DBSnapshotIdentifier>
      <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
      <SnapshotCreateTime>2026-11-16T12:10:20.016918Z</SnapshotCreateTime>
      <Engine>postgres</Engine>
      <AllocatedStorage>20</AllocatedStorage>
      <Status>creating</Status>
      <Port>5432</Port>
      <AvailabilityZone>us-east-1a</AvailabilityZone>
      <InstanceCreateTime>2026-11-16T12:10:17.925369Z</InstanceCreateTime>
      <MasterUsername>app</MasterUsername>
      <EngineVersion>18.3</EngineVersion>
      <LicenseModel>postgresql-license</LicenseModel>
      <SnapshotType>manual</SnapshotType>
      <StorageThroughput>125</StorageThroughput>
      <PercentProgress>0</PercentProgress>
      <StorageType>gp3</StorageType>
      <Encrypted>false</Encrypted>
      <BackupRetentionPeriod>1</BackupRetentionPeriod>
      <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
      <DBSnapshotArn>arn:aws:rds:us-east-1:000000000000:snapshot:docs-demo-pg-snap</DBSnapshotArn>
      <DbiResourceId>db-XB1NLPJCWCADLT701YYIWRMWV0</DbiResourceId>
      <TagList/>
    </DBSnapshot>
  </CreateDBSnapshotResult>
  <ResponseMetadata>
    <RequestId>0e52bec8-dfd6-467b-ac52-b0d34d07fb53</RequestId>
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
X-Amz-Date: 20261116T121022Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeDBSnapshots
&Version=2014-10-31
&DBSnapshotIdentifier=docs-demo-pg-snap
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1337
date: Mon, 16 Nov 2026 12:10:22 GMT

<DescribeDBSnapshotsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBSnapshotsResult>
    <DBSnapshots>
      <DBSnapshot xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <DBSnapshotIdentifier>docs-demo-pg-snap</DBSnapshotIdentifier>
        <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
        <SnapshotCreateTime>2026-11-16T12:10:20.016918Z</SnapshotCreateTime>
        <Engine>postgres</Engine>
        <AllocatedStorage>20</AllocatedStorage>
        <Status>available</Status>
        <Port>5432</Port>
        <AvailabilityZone>us-east-1a</AvailabilityZone>
        <InstanceCreateTime>2026-11-16T12:10:17.925369Z</InstanceCreateTime>
        <MasterUsername>app</MasterUsername>
        <EngineVersion>18.3</EngineVersion>
        <LicenseModel>postgresql-license</LicenseModel>
        <SnapshotType>manual</SnapshotType>
        <StorageThroughput>125</StorageThroughput>
        <PercentProgress>100</PercentProgress>
        <StorageType>gp3</StorageType>
        <Encrypted>false</Encrypted>
        <BackupRetentionPeriod>1</BackupRetentionPeriod>
        <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
        <DBSnapshotArn>arn:aws:rds:us-east-1:000000000000:snapshot:docs-demo-pg-snap</DBSnapshotArn>
        <DbiResourceId>db-XB1NLPJCWCADLT701YYIWRMWV0</DbiResourceId>
        <TagList/>
      </DBSnapshot>
    </DBSnapshots>
  </DescribeDBSnapshotsResult>
  <ResponseMetadata>
    <RequestId>3a889669-f8fe-4c3e-9deb-8fcbe66f2c82</RequestId>
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
X-Amz-Date: 20261116T121022Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=docs-demo-pg
```

**Response**

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 514fa533-7bc7-465d-a650-6c97d9ad9a67
content-length: 308
date: Mon, 16 Nov 2026 12:10:22 GMT

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>FinalDBSnapshotIdentifier is required unless SkipFinalSnapshot is specified.</Message>
  </Error>
  <RequestId>514fa533-7bc7-465d-a650-6c97d9ad9a67</RequestId>
</ErrorResponse>
```

### 6. DeleteDBInstance

Skipping the final snapshot deletes the instance.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121022Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=docs-demo-pg
&SkipFinalSnapshot=true
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 4277
date: Mon, 16 Nov 2026 12:10:22 GMT

<DeleteDBInstanceResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DeleteDBInstanceResult>
    <DBInstance xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
      <DBInstanceClass>db.t3.micro</DBInstanceClass>
      <Engine>postgres</Engine>
      <DBInstanceStatus>deleting</DBInstanceStatus>
      <MasterUsername>app</MasterUsername>
      <Endpoint>
        <Address>127.0.0.1</Address>
        <Port>43985</Port>
        <HostedZoneId>Z2R2ITUGPM61AM</HostedZoneId>
      </Endpoint>
      <AllocatedStorage>20</AllocatedStorage>
      <InstanceCreateTime>2026-11-16T12:10:17.925369Z</InstanceCreateTime>
      <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
      <BackupRetentionPeriod>1</BackupRetentionPeriod>
      <DBSecurityGroups/>
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
            <SubnetOutpost/>
            <SubnetStatus>Active</SubnetStatus>
          </Subnet>
          <Subnet>
            <SubnetIdentifier>subnet-00000000000000009</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1b</Name>
            </SubnetAvailabilityZone>
            <SubnetOutpost/>
            <SubnetStatus>Active</SubnetStatus>
          </Subnet>
          <Subnet>
            <SubnetIdentifier>subnet-0000000000000000b</SubnetIdentifier>
            <SubnetAvailabilityZone>
              <Name>us-east-1c</Name>
            </SubnetAvailabilityZone>
            <SubnetOutpost/>
            <SubnetStatus>Active</SubnetStatus>
… (55 more lines)
```

### 7. DeleteDBSnapshot

Manual snapshots outlive their instance and are deleted on their own.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T121022Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/rds/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteDBSnapshot
&Version=2014-10-31
&DBSnapshotIdentifier=docs-demo-pg-snap
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1298
date: Mon, 16 Nov 2026 12:10:22 GMT

<DeleteDBSnapshotResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DeleteDBSnapshotResult>
    <DBSnapshot xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <DBSnapshotIdentifier>docs-demo-pg-snap</DBSnapshotIdentifier>
      <DBInstanceIdentifier>docs-demo-pg</DBInstanceIdentifier>
      <SnapshotCreateTime>2026-11-16T12:10:20.016918Z</SnapshotCreateTime>
      <Engine>postgres</Engine>
      <AllocatedStorage>20</AllocatedStorage>
      <Status>available</Status>
      <Port>5432</Port>
      <AvailabilityZone>us-east-1a</AvailabilityZone>
      <InstanceCreateTime>2026-11-16T12:10:17.925369Z</InstanceCreateTime>
      <MasterUsername>app</MasterUsername>
      <EngineVersion>18.3</EngineVersion>
      <LicenseModel>postgresql-license</LicenseModel>
      <SnapshotType>manual</SnapshotType>
      <StorageThroughput>125</StorageThroughput>
      <PercentProgress>100</PercentProgress>
      <StorageType>gp3</StorageType>
      <Encrypted>false</Encrypted>
      <BackupRetentionPeriod>1</BackupRetentionPeriod>
      <PreferredBackupWindow>07:00-07:30</PreferredBackupWindow>
      <DBSnapshotArn>arn:aws:rds:us-east-1:000000000000:snapshot:docs-demo-pg-snap</DBSnapshotArn>
      <DbiResourceId>db-XB1NLPJCWCADLT701YYIWRMWV0</DbiResourceId>
      <TagList/>
    </DBSnapshot>
  </DeleteDBSnapshotResult>
  <ResponseMetadata>
    <RequestId>feb53553-9bc8-4d3b-9c44-500b4b6e03a8</RequestId>
  </ResponseMetadata>
</DeleteDBSnapshotResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 168 operations, 31 accepted the placeholder call, 39 answer `501 NotImplemented`, and the rest refused it with a modelled error.

### AddRoleToDBCluster

Associates an Identity and Access Management (IAM) role with a DB cluster.

```http
POST / HTTP/1.1

Action=AddRoleToDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
&RoleArn=example0000000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>1 validation error detected: Value 'example0000000000000' at 'roleArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws[a-z-]*:iam::[0-9]*:role/.*$</Message>
  </Error>
  <RequestId>a8d61fdd-0c1e-4d43-80ba-1f320b894e6b</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AddRoleToDBInstance

Associates an Amazon Web Services Identity and Access Management (IAM) role with a DB instance.

```http
POST / HTTP/1.1

Action=AddRoleToDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
&RoleArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&FeatureName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid Role ARN: arn:aws:rds:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>a5e5af33-c52a-496d-95d2-6e4347dba84d</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AddSourceIdentifierToSubscription

Adds a source identifier to an existing RDS event notification subscription.

```http
POST / HTTP/1.1

Action=AddSourceIdentifierToSubscription
&Version=2014-10-31
&SubscriptionName=example
&SourceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>SubscriptionNotFound</Code>
    <Message>Event Subscription example not found.</Message>
  </Error>
  <RequestId>c06c9e27-08a3-4746-80b1-28f5f9913e94</RequestId>
</ErrorResponse>
```

`404` `SubscriptionNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### AddTagsToResource

Adds metadata tags to an Amazon RDS resource.

```http
POST / HTTP/1.1

Action=AddTagsToResource
&Version=2014-10-31
&ResourceName=example
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
  <RequestId>9c443f80-cc11-4862-a59d-5b1983b55508</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### ApplyPendingMaintenanceAction

Applies a pending maintenance action to a resource (for example, to a DB instance).

```http
POST / HTTP/1.1

Action=ApplyPendingMaintenanceAction
&Version=2014-10-31
&ResourceIdentifier=example
&ApplyAction=example
&OptInType=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Unknown resource: example</Message>
  </Error>
  <RequestId>ea269f48-54d7-45dc-a0f9-c931fb0bc220</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AuthorizeDBSecurityGroupIngress

Enables ingress to a DBSecurityGroup using one of two forms of authorization.

```http
POST / HTTP/1.1

Action=AuthorizeDBSecurityGroupIngress
&Version=2014-10-31
&DBSecurityGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSecurityGroupNotFound</Code>
    <Message>DBSecurityGroup not found: example</Message>
  </Error>
  <RequestId>c053a535-eb84-449e-b037-f5c23134e5d6</RequestId>
</ErrorResponse>
```

`404` `DBSecurityGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### BacktrackDBCluster

Backtracks a DB cluster to a specific time, without creating a new DB cluster.

```http
POST / HTTP/1.1

Action=BacktrackDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
&BacktrackTo=2026-11-16T12%3A11%3A26Z
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>Could not find specified DB Cluster</Message>
  </Error>
  <RequestId>03d4b368-4db6-48d9-ab63-73e9b3ea85db</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CancelDBCapacityReservation

Cancels a DB capacity reservation.

```http
POST / HTTP/1.1

Action=CancelDBCapacityReservation
&Version=2014-10-31
&DBCapacityReservationIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation CancelDBCapacityReservation is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>73d5947c-09bc-4d38-9fdb-623b0b6a93cb</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CancelExportTask

Cancels an export task in progress that is exporting a snapshot or cluster to Amazon S3.

```http
POST / HTTP/1.1

Action=CancelExportTask
&Version=2014-10-31
&ExportTaskIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>ExportTaskNotFound</Code>
    <Message>The export task example doesn't exist.</Message>
  </Error>
  <RequestId>3bfb9b83-4b7f-4120-91fe-acdae58b4016</RequestId>
</ErrorResponse>
```

`404` `ExportTaskNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopyDBClusterParameterGroup

Copies the specified DB cluster parameter group.

```http
POST / HTTP/1.1

Action=CopyDBClusterParameterGroup
&Version=2014-10-31
&SourceDBClusterParameterGroupIdentifier=example
&TargetDBClusterParameterGroupIdentifier=example
&TargetDBClusterParameterGroupDescription=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBClusterParameterGroup not found: example</Message>
  </Error>
  <RequestId>6fa19a39-cb17-4f0a-a8e9-fed476f9d513</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopyDBClusterSnapshot

Copies a snapshot of a DB cluster.

```http
POST / HTTP/1.1

Action=CopyDBClusterSnapshot
&Version=2014-10-31
&SourceDBClusterSnapshotIdentifier=example
&TargetDBClusterSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterSnapshotNotFoundFault</Code>
    <Message>Requested snapshot example not found in source region us-east-1</Message>
  </Error>
  <RequestId>fe952f0c-8553-4e68-91c3-30fdfe6cc551</RequestId>
</ErrorResponse>
```

`404` `DBClusterSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopyDBParameterGroup

Copies the specified DB parameter group.

```http
POST / HTTP/1.1

Action=CopyDBParameterGroup
&Version=2014-10-31
&SourceDBParameterGroupIdentifier=example
&TargetDBParameterGroupIdentifier=example
&TargetDBParameterGroupDescription=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>Parameter group does not exist</Message>
  </Error>
  <RequestId>fe0e3e2d-241c-4b5b-9518-8c95d0e14f9c</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopyDBSnapshot

Copies the specified DB snapshot.

```http
POST / HTTP/1.1

Action=CopyDBSnapshot
&Version=2014-10-31
&SourceDBSnapshotIdentifier=example
&TargetDBSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSnapshotNotFound</Code>
    <Message>Requested snapshot example not found in source region us-east-1</Message>
  </Error>
  <RequestId>36f419f4-318b-4b96-b583-c5e7cb3a35df</RequestId>
</ErrorResponse>
```

`404` `DBSnapshotNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopyOptionGroup

Copies the specified option group.

```http
POST / HTTP/1.1

Action=CopyOptionGroup
&Version=2014-10-31
&SourceOptionGroupIdentifier=example
&TargetOptionGroupIdentifier=example
&TargetOptionGroupDescription=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>OptionGroupNotFoundFault</Code>
    <Message>Specified OptionGroupName: example not found.</Message>
  </Error>
  <RequestId>4da10242-cbd3-49e9-b5ed-01569f3f8b70</RequestId>
</ErrorResponse>
```

`404` `OptionGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateBlueGreenDeployment

Creates a blue/green deployment.

```http
POST / HTTP/1.1

Action=CreateBlueGreenDeployment
&Version=2014-10-31
&BlueGreenDeploymentName=example
&Source=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>1 validation error detected: Value 'example' at 'source' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:[A-Za-z][0-9A-Za-z-:._]*$</Message>
  </Error>
  <RequestId>65919dbb-bd42-469f-b912-709c7e11708e</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateCustomDBEngineVersion

Creates a custom DB engine version (CEV).

```http
POST / HTTP/1.1

Action=CreateCustomDBEngineVersion
&Version=2014-10-31
&Engine=example
&EngineVersion=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid DB engine</Message>
  </Error>
  <RequestId>a4d6ad71-bd17-4133-9245-8be314b087a2</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBCapacityReservation

Creates a DB capacity reservation.

```http
POST / HTTP/1.1

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

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>1 validation error detected: Value 'example' at 'dBInstanceClass' failed to satisfy constraint: Member must satisfy regular expression pattern: ^db\.[0-9a-z]{2,6}\.[0-9a-z\.]{4,20}$</Message>
  </Error>
  <RequestId>fbc51317-7a9d-44d0-9115-c355798c6a84</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBCluster

Creates a new Amazon Aurora DB cluster or Multi-AZ DB cluster.

```http
POST / HTTP/1.1

Action=CreateDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid DB engine: example is not one of aurora-mysql, aurora-postgresql, docdb, mysql, neptune, postgres</Message>
  </Error>
  <RequestId>433c991f-a061-4f1e-a32d-fd32f0e27e3e</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBClusterEndpoint

Creates a new custom endpoint and associates it with an Amazon Aurora DB cluster.

```http
POST / HTTP/1.1

Action=CreateDBClusterEndpoint
&Version=2014-10-31
&DBClusterIdentifier=example
&DBClusterEndpointIdentifier=example
&EndpointType=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation CreateDBClusterEndpoint is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>68180cc4-f27e-4d32-924d-b1f9e47e30ca</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateDBClusterParameterGroup

Creates a new DB cluster parameter group.

```http
POST / HTTP/1.1

Action=CreateDBClusterParameterGroup
&Version=2014-10-31
&DBClusterParameterGroupName=example
&DBParameterGroupFamily=example
&Description=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>ParameterGroupFamily example is not a valid parameter group family</Message>
  </Error>
  <RequestId>63f4bfb5-21e0-4878-8540-e35d5fedd4e8</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBClusterSnapshot

Creates a snapshot of a DB cluster.

```http
POST / HTTP/1.1

Action=CreateDBClusterSnapshot
&Version=2014-10-31
&DBClusterSnapshotIdentifier=example
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>DBCluster not found: example</Message>
  </Error>
  <RequestId>89f1adcf-c88d-442c-bb77-bfa0c7813253</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateDBInstance

Creates a new DB instance.

```http
POST / HTTP/1.1

Action=CreateDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
&DBInstanceClass=example
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid DB engine</Message>
  </Error>
  <RequestId>6fcf7ffd-e53d-4554-bf9e-afb0f460cdeb</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation. See [the walkthrough](#1-createdbinstance) for a successful call.

### CreateDBInstanceReadReplica

Creates a new DB instance that acts as a read replica for an existing source DB instance or Multi-AZ DB cluster.

```http
POST / HTTP/1.1

Action=CreateDBInstanceReadReplica
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>either SourceDBInstanceIdentifier or SourceDBClusterIdentifier must be specified: a read replica replicates one source</Message>
  </Error>
  <RequestId>234346c2-60b2-49a2-9204-32a7b1131632</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### CreateDBParameterGroup

Creates a new DB parameter group.

```http
POST / HTTP/1.1

Action=CreateDBParameterGroup
&Version=2014-10-31
&DBParameterGroupName=example
&DBParameterGroupFamily=example
&Description=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>ParameterGroupFamily example is not a valid parameter group family</Message>
  </Error>
  <RequestId>77bbfba0-94c4-49df-bbbf-c7c9c0bdc84f</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateDBProxy

Creates a new DB proxy.

```http
POST / HTTP/1.1

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

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidSubnet</Code>
    <Message>'example' is not an EC2 subnet id (expected subnet- followed by 8 or 17 hexadecimal characters)</Message>
  </Error>
  <RequestId>7d75f647-8e5a-4d54-91e4-4294d903a505</RequestId>
</ErrorResponse>
```

`400` `InvalidSubnet`.

### CreateDBProxyEndpoint

Creates a DBProxyEndpoint .

```http
POST / HTTP/1.1

Action=CreateDBProxyEndpoint
&Version=2014-10-31
&DBProxyName=example
&DBProxyEndpointName=example
&VpcSubnetIds.member.1=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation CreateDBProxyEndpoint is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>7c636c40-950c-4b5c-bdbb-efb4b94cfcd2</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### CreateDBSecurityGroup

Creates a new DB security group.

```http
POST / HTTP/1.1

Action=CreateDBSecurityGroup
&Version=2014-10-31
&DBSecurityGroupName=example
&DBSecurityGroupDescription=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateDBSecurityGroupResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <CreateDBSecurityGroupResult>
    <DBSecurityGroup xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <OwnerId>000000000000</OwnerId>
      <DBSecurityGroupName>example</DBSecurityGroupName>
      <DBSecurityGroupDescription>example</DBSecurityGroupDescription>
      <VpcId>vpc-bcb2053b5ea151bab</VpcId>
      <EC2SecurityGroups/>
      <IPRanges/>
      <DBSecurityGroupArn>arn:aws:rds:us-east-1:000000000000:secgrp:example</DBSecurityGroupArn>
    </DBSecurityGroup>
  </CreateDBSecurityGroupResult>
  <ResponseMetadata>
    <RequestId>6fe80114-5289-44b3-abc8-c8086ed38e21</RequestId>
  </ResponseMetadata>
</CreateDBSecurityGroupResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateDBShardGroup

Creates a new DB shard group for Aurora Limitless Database.

```http
POST / HTTP/1.1

Action=CreateDBShardGroup
&Version=2014-10-31
&DBShardGroupIdentifier=example
&DBClusterIdentifier=example
&MaxACU=1
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>The DB cluster doesn't exist.</Message>
  </Error>
  <RequestId>07c34cc7-8f22-4b74-9ef5-4981d8c4d5e9</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateDBSnapshot

Creates a snapshot of a DB instance.

```http
POST / HTTP/1.1

Action=CreateDBSnapshot
&Version=2014-10-31
&DBSnapshotIdentifier=example
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>ad35ad5a-4c1a-43b6-a7b9-2bf516a47788</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#3-createdbsnapshot) for a successful call.

### CreateDBSubnetGroup

Creates a new DB subnet group.

```http
POST / HTTP/1.1

Action=CreateDBSubnetGroup
&Version=2014-10-31
&DBSubnetGroupName=example
&DBSubnetGroupDescription=example
&SubnetIds.SubnetIdentifier.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidSubnet</Code>
    <Message>'example' is not an EC2 subnet id (expected subnet- followed by 8 or 17 hexadecimal characters)</Message>
  </Error>
  <RequestId>ae973d4d-2bab-4448-b1eb-97f04766e44f</RequestId>
</ErrorResponse>
```

`400` `InvalidSubnet`.

### CreateEventSubscription

Creates an RDS event notification subscription.

```http
POST / HTTP/1.1

Action=CreateEventSubscription
&Version=2014-10-31
&SubscriptionName=example
&SnsTopicArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>SNSInvalidTopic</Code>
    <Message>invalid value arn:aws:rds:us-east-1:000000000000:example/example for SnsTopicArn: expected an Amazon SNS topic ARN of the form arn:aws:sns:&lt;region&gt;:&lt;account&gt;:&lt;topic name&gt;</Message>
  </Error>
  <RequestId>50e3cdef-3d45-455d-a930-7b3cdecebdaf</RequestId>
</ErrorResponse>
```

`400` `SNSInvalidTopic`.

### CreateGlobalCluster

Creates an Aurora global database spread across multiple Amazon Web Services Regions.

```http
POST / HTTP/1.1

Action=CreateGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateGlobalClusterResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <CreateGlobalClusterResult>
    <GlobalCluster xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <GlobalClusterIdentifier>example</GlobalClusterIdentifier>
      <GlobalClusterResourceId>cluster-5e8b37e56599a183</GlobalClusterResourceId>
      <GlobalClusterArn>arn:aws:rds::000000000000:global-cluster:example</GlobalClusterArn>
      <Status>available</Status>
      <EngineLifecycleSupport>open-source-rds-extended-support</EngineLifecycleSupport>
      <StorageEncrypted>false</StorageEncrypted>
      <StorageEncryptionType>sse-rds</StorageEncryptionType>
      <DeletionProtection>false</DeletionProtection>
      <GlobalClusterMembers/>
      <Endpoint>example.global-utrjz1d7t0id.global.rds.amazonaws.com</Endpoint>
      <TagList/>
    </GlobalCluster>
  </CreateGlobalClusterResult>
  <ResponseMetadata>
    <RequestId>3f0444b8-8196-4085-a010-6b376a1498b8</RequestId>
  </ResponseMetadata>
</CreateGlobalClusterResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateIntegration

Creates a zero-ETL integration with Amazon Redshift.

```http
POST / HTTP/1.1

Action=CreateIntegration
&Version=2014-10-31
&SourceArn=example
&TargetArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&IntegrationName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>1 validation error detected: Value 'example' at 'sourceArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws[a-z\-]*:rds(-[a-z]*)?:[a-z0-9\-]*:[0-9]*:(cluster|db):[a-z][a-z0-9]*(-[a-z0-9]+)*$</Message>
  </Error>
  <RequestId>ee66899f-a27d-4c5e-acde-66194860f518</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateOptionGroup

Creates a new option group.

```http
POST / HTTP/1.1

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

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>invalid value 'example' for EngineName: valid values are db2-ae, db2-ce, db2-se, mariadb, mysql, oracle-ee, oracle-ee-cdb, oracle-se2, oracle-se2-cdb, postgres, sqlserver-ee, sqlserver-se, sqlserver-ex, sqlserver-web</Message>
  </Error>
  <RequestId>88da4071-e174-4290-8cb9-dfb47813aca4</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateTenantDatabase

Creates a tenant database in a DB instance that uses the multi-tenant configuration.

```http
POST / HTTP/1.1

Action=CreateTenantDatabase
&Version=2014-10-31
&DBInstanceIdentifier=example
&TenantDBName=example
&MasterUsername=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>7f929179-e935-4fe4-9f42-6d7c2b4f5de1</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteBlueGreenDeployment

Deletes a blue/green deployment.

```http
POST / HTTP/1.1

Action=DeleteBlueGreenDeployment
&Version=2014-10-31
&BlueGreenDeploymentIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>invalid value 'example' for BlueGreenDeploymentIdentifier: a blue/green deployment identifier is assigned by the service and reads bgd- followed by the token it assigned</Message>
  </Error>
  <RequestId>177219b2-2382-483b-abf4-1adbd562707d</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteCustomDBEngineVersion

Deletes a custom engine version.

```http
POST / HTTP/1.1

Action=DeleteCustomDBEngineVersion
&Version=2014-10-31
&Engine=example
&EngineVersion=example
```

```http
HTTP/1.1 403 Forbidden
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>The custom engine version you requested may not exist or you don't have the required permission.</Message>
  </Error>
  <RequestId>4a039487-b706-42f9-a05e-d0d711d8664e</RequestId>
</ErrorResponse>
```

`403` `AccessDenied`: the request was refused by authorization.

### DeleteDBCluster

The DeleteDBCluster action deletes a previously provisioned DB cluster.

```http
POST / HTTP/1.1

Action=DeleteDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>The source cluster could not be found or cannot be accessed: example</Message>
  </Error>
  <RequestId>7c7b4fba-2026-49b5-b39e-76097f1f67aa</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBClusterAutomatedBackup

Deletes automated backups using the DbClusterResourceId value of the source DB cluster or the Amazon Resource Name (ARN) of the automated backups.

```http
POST / HTTP/1.1

Action=DeleteDBClusterAutomatedBackup
&Version=2014-10-31
&DbClusterResourceId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DeleteDBClusterAutomatedBackup is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>2e1b0fea-d9c9-4c3a-89b6-930a9556e98a</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteDBClusterEndpoint

Deletes a custom endpoint and removes it from an Amazon Aurora DB cluster.

```http
POST / HTTP/1.1

Action=DeleteDBClusterEndpoint
&Version=2014-10-31
&DBClusterEndpointIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DeleteDBClusterEndpoint is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>385e991e-461d-4850-ae1a-d2ab4b8769f9</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteDBClusterParameterGroup

Deletes a specified DB cluster parameter group.

```http
POST / HTTP/1.1

Action=DeleteDBClusterParameterGroup
&Version=2014-10-31
&DBClusterParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBClusterParameterGroup not found: example</Message>
  </Error>
  <RequestId>4d32bb22-783f-4e27-9cae-b9340256e778</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBClusterSnapshot

Deletes a DB cluster snapshot.

```http
POST / HTTP/1.1

Action=DeleteDBClusterSnapshot
&Version=2014-10-31
&DBClusterSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterSnapshotNotFoundFault</Code>
    <Message>DBClusterSnapshot not found: example</Message>
  </Error>
  <RequestId>c6ff359a-9ee3-44e1-b82b-8dfcf5ea6dfc</RequestId>
</ErrorResponse>
```

`404` `DBClusterSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBInstance

Deletes a previously provisioned DB instance.

```http
POST / HTTP/1.1

Action=DeleteDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance example not found.</Message>
  </Error>
  <RequestId>5c01e1cf-dedd-4226-b8aa-c026c5c73193</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#6-deletedbinstance) for a successful call.

### DeleteDBInstanceAutomatedBackup

Deletes automated backups using the DbiResourceId value of the source DB instance or the Amazon Resource Name (ARN) of the automated backups.

```http
POST / HTTP/1.1

Action=DeleteDBInstanceAutomatedBackup
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DeleteDBInstanceAutomatedBackup is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>7f346f26-dfb5-4d01-997d-3ac6647d70f6</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteDBParameterGroup

Deletes a specified DB parameter group.

```http
POST / HTTP/1.1

Action=DeleteDBParameterGroup
&Version=2014-10-31
&DBParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBParameterGroup not found: example</Message>
  </Error>
  <RequestId>34b9b36d-ef15-475f-8577-ab95659b2cea</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBProxy

Deletes an existing DB proxy.

```http
POST / HTTP/1.1

Action=DeleteDBProxy
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBProxyNotFoundFault</Code>
    <Message>DBProxy 'example' not found.</Message>
  </Error>
  <RequestId>5b88b4c8-87f3-487a-9f18-40d475dff324</RequestId>
</ErrorResponse>
```

`404` `DBProxyNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBProxyEndpoint

Deletes a DBProxyEndpoint .

```http
POST / HTTP/1.1

Action=DeleteDBProxyEndpoint
&Version=2014-10-31
&DBProxyEndpointName=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DeleteDBProxyEndpoint is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>2f62c233-ac46-442e-9094-02c680edb121</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DeleteDBSecurityGroup

Deletes a DB security group.

```http
POST / HTTP/1.1

Action=DeleteDBSecurityGroup
&Version=2014-10-31
&DBSecurityGroupName=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeleteDBSecurityGroupResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DeleteDBSecurityGroupResult/>
  <ResponseMetadata>
    <RequestId>abb16b39-cee5-4c95-9ae4-f993dff4e77a</RequestId>
  </ResponseMetadata>
</DeleteDBSecurityGroupResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteDBShardGroup

Deletes an Aurora Limitless Database DB shard group.

```http
POST / HTTP/1.1

Action=DeleteDBShardGroup
&Version=2014-10-31
&DBShardGroupIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBShardGroupNotFound</Code>
    <Message>DB shard group not found or can not be accessed</Message>
  </Error>
  <RequestId>7c1543aa-93b5-44ae-b515-e0ac44d70cad</RequestId>
</ErrorResponse>
```

`404` `DBShardGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteDBSnapshot

Deletes a DB snapshot.

```http
POST / HTTP/1.1

Action=DeleteDBSnapshot
&Version=2014-10-31
&DBSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSnapshotNotFound</Code>
    <Message>DBSnapshot not found: example</Message>
  </Error>
  <RequestId>a7b28405-b8da-49df-8a48-6a09dc19ac75</RequestId>
</ErrorResponse>
```

`404` `DBSnapshotNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deletedbsnapshot) for a successful call.

### DeleteDBSubnetGroup

Deletes a DB subnet group.

```http
POST / HTTP/1.1

Action=DeleteDBSubnetGroup
&Version=2014-10-31
&DBSubnetGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSubnetGroupNotFoundFault</Code>
    <Message>DB subnet group 'example' does not exist.</Message>
  </Error>
  <RequestId>2c00ff57-bafe-4016-b9c5-ac90130d9560</RequestId>
</ErrorResponse>
```

`404` `DBSubnetGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteEventSubscription

Deletes an RDS event notification subscription.

```http
POST / HTTP/1.1

Action=DeleteEventSubscription
&Version=2014-10-31
&SubscriptionName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>SubscriptionNotFound</Code>
    <Message>Event Subscription example not found.</Message>
  </Error>
  <RequestId>277fae1b-4510-4e7a-b209-946dd724e6d0</RequestId>
</ErrorResponse>
```

`404` `SubscriptionNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteGlobalCluster

Deletes a global database cluster.

```http
POST / HTTP/1.1

Action=DeleteGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DeleteGlobalClusterResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DeleteGlobalClusterResult>
    <GlobalCluster xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
      <GlobalClusterIdentifier>example</GlobalClusterIdentifier>
      <GlobalClusterResourceId>cluster-5e8b37e56599a183</GlobalClusterResourceId>
      <GlobalClusterArn>arn:aws:rds::000000000000:global-cluster:example</GlobalClusterArn>
      <Status>available</Status>
      <EngineLifecycleSupport>open-source-rds-extended-support</EngineLifecycleSupport>
      <StorageEncrypted>false</StorageEncrypted>
      <StorageEncryptionType>sse-rds</StorageEncryptionType>
      <DeletionProtection>false</DeletionProtection>
      <GlobalClusterMembers/>
      <Endpoint>example.global-utrjz1d7t0id.global.rds.amazonaws.com</Endpoint>
      <TagList/>
    </GlobalCluster>
  </DeleteGlobalClusterResult>
  <ResponseMetadata>
    <RequestId>433c4f89-d37c-4c1d-b132-a0fbf4d620c8</RequestId>
  </ResponseMetadata>
</DeleteGlobalClusterResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DeleteIntegration

Deletes a zero-ETL integration with Amazon Redshift.

```http
POST / HTTP/1.1

Action=DeleteIntegration
&Version=2014-10-31
&IntegrationIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>IntegrationNotFoundFault</Code>
    <Message>Integration with identifier arn:aws:rds:us-east-1:000000000000:integration:example could not be found.</Message>
  </Error>
  <RequestId>56420a64-83a6-43f9-8a0b-b3e5b3bb7230</RequestId>
</ErrorResponse>
```

`404` `IntegrationNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteOptionGroup

Deletes an existing option group.

```http
POST / HTTP/1.1

Action=DeleteOptionGroup
&Version=2014-10-31
&OptionGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>OptionGroupNotFoundFault</Code>
    <Message>Specified OptionGroupName: example not found.</Message>
  </Error>
  <RequestId>6955b795-961f-4027-a6c9-5bc6eaea8b45</RequestId>
</ErrorResponse>
```

`404` `OptionGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteTenantDatabase

Deletes a tenant database from your DB instance.

```http
POST / HTTP/1.1

Action=DeleteTenantDatabase
&Version=2014-10-31
&DBInstanceIdentifier=example
&TenantDBName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>e979bf31-1f66-42b0-9371-982557126aa2</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeregisterDBProxyTargets

Remove the association between one or more DBProxyTarget data structures and a DBProxyTargetGroup .

```http
POST / HTTP/1.1

Action=DeregisterDBProxyTargets
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>Missing DB instance or DB cluster identifier.</Message>
  </Error>
  <RequestId>f98502ee-9b85-41a0-8767-1e97b6d25b88</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### DescribeAccountAttributes

Lists all of the attributes for a customer account.

```http
POST / HTTP/1.1

Action=DescribeAccountAttributes
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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

Action=DescribeBlueGreenDeployments
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeBlueGreenDeploymentsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeBlueGreenDeploymentsResult>
    <BlueGreenDeployments/>
  </DescribeBlueGreenDeploymentsResult>
  <ResponseMetadata>
    <RequestId>51d70bea-7cee-47aa-9226-9f44c0abd282</RequestId>
  </ResponseMetadata>
</DescribeBlueGreenDeploymentsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCertificates

Lists the set of certificate authority (CA) certificates provided by Amazon RDS for this Amazon Web Services account.

```http
POST / HTTP/1.1

Action=DescribeCertificates
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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

```http
POST / HTTP/1.1

Action=DescribeDBCapacityReservations
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeDBCapacityReservations is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>d9888ea7-650d-460f-b078-d6db3d315d91</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeDBClusterAutomatedBackups

Displays backups for both current and deleted DB clusters.

```http
POST / HTTP/1.1

Action=DescribeDBClusterAutomatedBackups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBClusterAutomatedBackupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBClusterAutomatedBackupsResult>
    <DBClusterAutomatedBackups/>
  </DescribeDBClusterAutomatedBackupsResult>
  <ResponseMetadata>
    <RequestId>a6ef24b6-a8de-42c8-a5e5-a5891f1695a9</RequestId>
  </ResponseMetadata>
</DescribeDBClusterAutomatedBackupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBClusterBacktracks

Returns information about backtracks for a DB cluster.

```http
POST / HTTP/1.1

Action=DescribeDBClusterBacktracks
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeDBClusterBacktracks is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>7ffe8e53-77d8-4303-9cf1-85da61a3b904</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeDBClusterEndpoints

Returns information about endpoints for an Amazon Aurora DB cluster.

```http
POST / HTTP/1.1

Action=DescribeDBClusterEndpoints
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeDBClusterEndpoints is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>fecce358-46d2-405b-af00-73dd1e25f931</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeDBClusterParameterGroups

Returns a list of DBClusterParameterGroup descriptions.

```http
POST / HTTP/1.1

Action=DescribeDBClusterParameterGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBClusterParameterGroupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBClusterParameterGroupsResult>
    <DBClusterParameterGroups/>
  </DescribeDBClusterParameterGroupsResult>
  <ResponseMetadata>
    <RequestId>f537d37e-0c60-4523-918b-5cc02aaecdfe</RequestId>
  </ResponseMetadata>
</DescribeDBClusterParameterGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBClusterParameters

Returns the detailed parameter list for a particular DB cluster parameter group.

```http
POST / HTTP/1.1

Action=DescribeDBClusterParameters
&Version=2014-10-31
&DBClusterParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBClusterParameterGroup not found: example</Message>
  </Error>
  <RequestId>216cd976-ba7a-499e-b62b-ee45e6aa0d4b</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBClusterSnapshotAttributes

Returns a list of DB cluster snapshot attribute names and values for a manual DB cluster snapshot.

```http
POST / HTTP/1.1

Action=DescribeDBClusterSnapshotAttributes
&Version=2014-10-31
&DBClusterSnapshotIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterSnapshotNotFoundFault</Code>
    <Message>DBClusterSnapshot not found: example</Message>
  </Error>
  <RequestId>f878669f-e4e1-4f12-ba32-dea4af264c27</RequestId>
</ErrorResponse>
```

`404` `DBClusterSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBClusterSnapshots

Returns information about DB cluster snapshots.

```http
POST / HTTP/1.1

Action=DescribeDBClusterSnapshots
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBClusterSnapshotsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBClusterSnapshotsResult>
    <DBClusterSnapshots/>
  </DescribeDBClusterSnapshotsResult>
  <ResponseMetadata>
    <RequestId>cec2d038-2f5a-4375-86ed-6445df5247cb</RequestId>
  </ResponseMetadata>
</DescribeDBClusterSnapshotsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBClusters

Describes existing Amazon Aurora DB clusters and Multi-AZ DB clusters.

```http
POST / HTTP/1.1

Action=DescribeDBClusters
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBClustersResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBClustersResult>
    <DBClusters/>
  </DescribeDBClustersResult>
  <ResponseMetadata>
    <RequestId>053e4cc9-01f5-4f07-b036-1fb188d5f91a</RequestId>
  </ResponseMetadata>
</DescribeDBClustersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBEngineVersions

Describes the properties of specific versions of DB engines.

```http
POST / HTTP/1.1

Action=DescribeDBEngineVersions
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBEngineVersionsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBEngineVersionsResult>
    <Marker>b3JhY2xlLXNlMi1jZGIvMjEuMC4wLjAucnUtMjAyNC0xMC5ydXItMjAyNC0xMC5yMQ==</Marker>
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
            <MajorEngineVersion>5.7</MajorEngineVersion>
… (14934 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBInstanceAutomatedBackups

Displays backups for both current and deleted instances.

```http
POST / HTTP/1.1

Action=DescribeDBInstanceAutomatedBackups
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeDBInstanceAutomatedBackups is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>0cfe2e17-20cd-455c-9bbf-b64ca46f0a22</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeDBInstances

Describes provisioned RDS instances.

```http
POST / HTTP/1.1

Action=DescribeDBInstances
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBInstancesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBInstancesResult>
    <DBInstances/>
  </DescribeDBInstancesResult>
  <ResponseMetadata>
    <RequestId>4081f1ea-e7be-496f-9a96-53111ce37ba7</RequestId>
  </ResponseMetadata>
</DescribeDBInstancesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-describedbinstances) for a successful call.

### DescribeDBLogFiles

Returns a list of DB log files for the DB instance.

```http
POST / HTTP/1.1

Action=DescribeDBLogFiles
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>a90317c0-d7cb-4b6f-8aa5-5f76d78afc3f</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBMajorEngineVersions

Describes the properties of specific major versions of DB engines.

```http
POST / HTTP/1.1

Action=DescribeDBMajorEngineVersions
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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

Action=DescribeDBParameterGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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
… (391 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBParameters

Returns the detailed parameter list for a particular DB parameter group.

```http
POST / HTTP/1.1

Action=DescribeDBParameters
&Version=2014-10-31
&DBParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBParameterGroup not found: example</Message>
  </Error>
  <RequestId>f0f969b1-942d-450b-9eed-a6f20798dce5</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBProxies

Returns information about DB proxies.

```http
POST / HTTP/1.1

Action=DescribeDBProxies
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBProxiesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBProxiesResult>
    <DBProxies/>
  </DescribeDBProxiesResult>
  <ResponseMetadata>
    <RequestId>6e1a6291-e907-4896-a69c-9cdd46743302</RequestId>
  </ResponseMetadata>
</DescribeDBProxiesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBProxyEndpoints

Returns information about DB proxy endpoints.

```http
POST / HTTP/1.1

Action=DescribeDBProxyEndpoints
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeDBProxyEndpoints is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>903aa866-b210-407e-a2a1-165be33aacf0</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeDBProxyTargetGroups

Returns information about DB proxy target groups, represented by DBProxyTargetGroup data structures.

```http
POST / HTTP/1.1

Action=DescribeDBProxyTargetGroups
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBProxyNotFoundFault</Code>
    <Message>DBProxy 'example' not found.</Message>
  </Error>
  <RequestId>a9f61e40-ff07-413a-96a2-44260fc1abab</RequestId>
</ErrorResponse>
```

`404` `DBProxyNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBProxyTargets

Returns information about DBProxyTarget objects.

```http
POST / HTTP/1.1

Action=DescribeDBProxyTargets
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBProxyNotFoundFault</Code>
    <Message>DBProxy 'example' not found.</Message>
  </Error>
  <RequestId>cc026334-c77a-459d-87ca-52e8c39741df</RequestId>
</ErrorResponse>
```

`404` `DBProxyNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeDBRecommendations

Describes the recommendations to resolve the issues for your DB instances, DB clusters, and DB parameter groups.

```http
POST / HTTP/1.1

Action=DescribeDBRecommendations
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBRecommendationsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBRecommendationsResult>
    <DBRecommendations/>
  </DescribeDBRecommendationsResult>
  <ResponseMetadata>
    <RequestId>b0040483-8206-47da-9272-abdabd55b4f0</RequestId>
  </ResponseMetadata>
</DescribeDBRecommendationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBSecurityGroups

Returns a list of DBSecurityGroup descriptions.

```http
POST / HTTP/1.1

Action=DescribeDBSecurityGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBSecurityGroupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBSecurityGroupsResult>
    <DBSecurityGroups/>
  </DescribeDBSecurityGroupsResult>
  <ResponseMetadata>
    <RequestId>78912712-620a-49d0-9c20-b0bc0cb1c5a0</RequestId>
  </ResponseMetadata>
</DescribeDBSecurityGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBShardGroups

Describes existing Aurora Limitless Database DB shard groups.

```http
POST / HTTP/1.1

Action=DescribeDBShardGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBShardGroupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBShardGroupsResult>
    <DBShardGroups/>
  </DescribeDBShardGroupsResult>
  <ResponseMetadata>
    <RequestId>7c6d5f37-7915-4c9e-b8f9-67d7c32ac349</RequestId>
  </ResponseMetadata>
</DescribeDBShardGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBSnapshotAttributes

Returns a list of DB snapshot attribute names and values for a manual DB snapshot.

```http
POST / HTTP/1.1

Action=DescribeDBSnapshotAttributes
&Version=2014-10-31
&DBSnapshotIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeDBSnapshotAttributes is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>e0895536-6ac3-48cb-8e36-9f5ac29c681d</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeDBSnapshotTenantDatabases

Describes the tenant databases that exist in a DB snapshot.

```http
POST / HTTP/1.1

Action=DescribeDBSnapshotTenantDatabases
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBSnapshotTenantDatabasesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBSnapshotTenantDatabasesResult>
    <DBSnapshotTenantDatabases/>
  </DescribeDBSnapshotTenantDatabasesResult>
  <ResponseMetadata>
    <RequestId>befc87a9-9245-4339-b2ad-771ecbbaa6c9</RequestId>
  </ResponseMetadata>
</DescribeDBSnapshotTenantDatabasesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeDBSnapshots

Returns information about DB snapshots.

```http
POST / HTTP/1.1

Action=DescribeDBSnapshots
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeDBSnapshotsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeDBSnapshotsResult>
    <DBSnapshots/>
  </DescribeDBSnapshotsResult>
  <ResponseMetadata>
    <RequestId>002dc8d3-867b-49c4-9291-6a2db3c9755a</RequestId>
  </ResponseMetadata>
</DescribeDBSnapshotsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#4-describedbsnapshots) for a successful call.

### DescribeDBSubnetGroups

Returns a list of DBSubnetGroup descriptions.

```http
POST / HTTP/1.1

Action=DescribeDBSubnetGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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
            <SubnetOutpost/>
            <SubnetStatus>Active</SubnetStatus>
          </Subnet>
          <Subnet>
            <SubnetIdentifier>subnet-00000000000000009</SubnetIdentifier>
            <SubnetAvailabilityZone>
… (25 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeEngineDefaultClusterParameters

Returns the default engine and system parameter information for the cluster database engine.

```http
POST / HTTP/1.1

Action=DescribeEngineDefaultClusterParameters
&Version=2014-10-31
&DBParameterGroupFamily=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>ParameterGroupFamily example is not a valid parameter group family</Message>
  </Error>
  <RequestId>c82af792-16b1-4a64-b674-2bc7f9293f08</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribeEngineDefaultParameters

Returns the default engine and system parameter information for the specified database engine.

```http
POST / HTTP/1.1

Action=DescribeEngineDefaultParameters
&Version=2014-10-31
&DBParameterGroupFamily=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>ParameterGroupFamily example is not a valid parameter group family</Message>
  </Error>
  <RequestId>fbd19e0d-e3cc-46f3-9ecf-6872a496a2c9</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribeEventCategories

Displays a list of categories for all event source types, or, if specified, for a specified source type.

```http
POST / HTTP/1.1

Action=DescribeEventCategories
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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
… (69 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeEventSubscriptions

Lists all the subscription descriptions for a customer account.

```http
POST / HTTP/1.1

Action=DescribeEventSubscriptions
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeEventSubscriptionsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeEventSubscriptionsResult>
    <EventSubscriptionsList/>
  </DescribeEventSubscriptionsResult>
  <ResponseMetadata>
    <RequestId>feb618b2-f236-486b-b5bb-447b97348fa8</RequestId>
  </ResponseMetadata>
</DescribeEventSubscriptionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeEvents

Returns events related to DB instances, DB clusters, DB parameter groups, DB security groups, DB snapshots, DB cluster snapshots, and RDS Proxies for the past 14 days.

```http
POST / HTTP/1.1

Action=DescribeEvents
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

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
        <Date>2026-11-16T12:10:17.958351Z</Date>
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

Action=DescribeExportTasks
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeExportTasksResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeExportTasksResult>
    <ExportTasks/>
  </DescribeExportTasksResult>
  <ResponseMetadata>
    <RequestId>0f80d1e3-3c9d-4a1f-a07c-c427f0303766</RequestId>
  </ResponseMetadata>
</DescribeExportTasksResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeGlobalClusters

Returns information about Aurora global database clusters.

```http
POST / HTTP/1.1

Action=DescribeGlobalClusters
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeGlobalClustersResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeGlobalClustersResult>
    <GlobalClusters/>
  </DescribeGlobalClustersResult>
  <ResponseMetadata>
    <RequestId>a7cefc05-a76a-427d-a9cf-03a9196ef2fc</RequestId>
  </ResponseMetadata>
</DescribeGlobalClustersResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeIntegrations

Describe one or more zero-ETL integrations with Amazon Redshift.

```http
POST / HTTP/1.1

Action=DescribeIntegrations
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeIntegrationsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeIntegrationsResult>
    <Integrations/>
  </DescribeIntegrationsResult>
  <ResponseMetadata>
    <RequestId>c2a5c844-f5c2-4abf-9887-24fd68045285</RequestId>
  </ResponseMetadata>
</DescribeIntegrationsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeOptionGroupOptions

Describes all available options for the specified engine.

```http
POST / HTTP/1.1

Action=DescribeOptionGroupOptions
&Version=2014-10-31
&EngineName=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeOptionGroupOptions is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>fa15cc44-3ce8-486f-ad27-34bfb4c6d7a0</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeOptionGroups

Describes the available option groups.

```http
POST / HTTP/1.1

Action=DescribeOptionGroups
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeOptionGroupsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeOptionGroupsResult>
    <OptionGroupsList>
      <OptionGroup xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
        <OptionGroupName>default:db2-ae-11-5</OptionGroupName>
        <OptionGroupDescription>Default option group for db2-ae 11.5</OptionGroupDescription>
        <EngineName>db2-ae</EngineName>
        <MajorEngineVersion>11.5</MajorEngineVersion>
        <Options/>
        <AllowsVpcAndNonVpcInstanceMemberships>true</AllowsVpcAndNonVpcInstanceMemberships>
        <OptionGroupArn>arn:aws:rds:us-east-1:000000000000:og:default:db2-ae-11-5</OptionGroupArn>
      </OptionGroup>
      <OptionGroup>
        <OptionGroupName>default:db2-ae-12-1</OptionGroupName>
        <OptionGroupDescription>Default option group for db2-ae 12.1</OptionGroupDescription>
        <EngineName>db2-ae</EngineName>
        <MajorEngineVersion>12.1</MajorEngineVersion>
        <Options/>
        <AllowsVpcAndNonVpcInstanceMemberships>true</AllowsVpcAndNonVpcInstanceMemberships>
        <OptionGroupArn>arn:aws:rds:us-east-1:000000000000:og:default:db2-ae-12-1</OptionGroupArn>
… (331 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeOrderableDBInstanceOptions

Describes the orderable DB instance options for a specified DB engine.

```http
POST / HTTP/1.1

Action=DescribeOrderableDBInstanceOptions
&Version=2014-10-31
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid DB engine</Message>
  </Error>
  <RequestId>42b19cea-2b81-48af-b44d-36ce8e32fbdf</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribePendingMaintenanceActions

Returns a list of resources (for example, DB instances) that have at least one pending maintenance action.

```http
POST / HTTP/1.1

Action=DescribePendingMaintenanceActions
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribePendingMaintenanceActionsResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribePendingMaintenanceActionsResult>
    <PendingMaintenanceActions/>
  </DescribePendingMaintenanceActionsResult>
  <ResponseMetadata>
    <RequestId>05c06f45-6519-433e-bba6-62189ae958ec</RequestId>
  </ResponseMetadata>
</DescribePendingMaintenanceActionsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReservedDBInstances

Returns information about reserved DB instances for this account, or about a specified reserved DB instance.

```http
POST / HTTP/1.1

Action=DescribeReservedDBInstances
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeReservedDBInstances is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>6e14272f-4d44-489b-8137-4e6e049ee2e4</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeReservedDBInstancesOfferings

Lists available reserved DB instance offerings.

```http
POST / HTTP/1.1

Action=DescribeReservedDBInstancesOfferings
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeReservedDBInstancesOfferings is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>4d3683cc-edb9-418f-9d64-2f5828a30456</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeServerlessV2PlatformVersions

Describes the properties of specific platform versions for Aurora Serverless v2.

```http
POST / HTTP/1.1

Action=DescribeServerlessV2PlatformVersions
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeServerlessV2PlatformVersions is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>4932f3e1-e8e5-4dbf-866b-1251098f8d80</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeSourceRegions

Returns a list of the source Amazon Web Services Regions where the current Amazon Web Services Region can create a read replica, copy a DB snapshot from, or replicate automated backups from.

```http
POST / HTTP/1.1

Action=DescribeSourceRegions
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation DescribeSourceRegions is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>62d12ca2-c36b-4b7c-8b0f-83283d287358</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### DescribeTenantDatabases

Describes the tenant databases in a DB instance that uses the multi-tenant configuration.

```http
POST / HTTP/1.1

Action=DescribeTenantDatabases
&Version=2014-10-31
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeTenantDatabasesResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <DescribeTenantDatabasesResult>
    <TenantDatabases/>
  </DescribeTenantDatabasesResult>
  <ResponseMetadata>
    <RequestId>10f87485-ac8d-4151-b126-5ef682ec8262</RequestId>
  </ResponseMetadata>
</DescribeTenantDatabasesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeValidDBInstanceModifications

You can call DescribeValidDBInstanceModifications to learn what modifications you can make to your DB instance.

```http
POST / HTTP/1.1

Action=DescribeValidDBInstanceModifications
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>4217c8dd-499b-4e46-a7ce-dc864cb0f9d0</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DisableHttpEndpoint

Disables the HTTP endpoint for the specified DB cluster.

```http
POST / HTTP/1.1

Action=DisableHttpEndpoint
&Version=2014-10-31
&ResourceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid resource name: arn:aws:rds:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>dcce82b9-8eb6-4b20-9981-5c025063cd21</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DownloadDBLogFilePortion

Downloads all or a portion of the specified log file, up to 1 MB in size.

```http
POST / HTTP/1.1

Action=DownloadDBLogFilePortion
&Version=2014-10-31
&DBInstanceIdentifier=example
&LogFileName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>bbe4a3f2-5bf6-496b-87b2-05a5487f6523</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### EnableHttpEndpoint

Enables the HTTP endpoint for the DB cluster.

```http
POST / HTTP/1.1

Action=EnableHttpEndpoint
&Version=2014-10-31
&ResourceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid resource name: arn:aws:rds:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>384eb156-addc-4141-8662-2d295b471c80</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### FailoverDBCluster

Forces a failover for a DB cluster.

```http
POST / HTTP/1.1

Action=FailoverDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>The source cluster could not be found or cannot be accessed: example</Message>
  </Error>
  <RequestId>d068f2ab-e158-4a72-afe3-e3cbd6f2aa61</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### FailoverGlobalCluster

Promotes the specified secondary DB cluster to be the primary DB cluster in the global database cluster to fail over or switch over a global database.

```http
POST / HTTP/1.1

Action=FailoverGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
&TargetDbClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalClusterNotFoundFault</Code>
    <Message>Global cluster 'example' not found</Message>
  </Error>
  <RequestId>d706cfe6-ccd2-40f0-98d5-6c9ee4ff007a</RequestId>
</ErrorResponse>
```

`404` `GlobalClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ListTagsForResource

Lists all tags on an Amazon RDS resource.

```http
POST / HTTP/1.1

Action=ListTagsForResource
&Version=2014-10-31
&ResourceName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid resource name: example</Message>
  </Error>
  <RequestId>fb090085-114a-4084-a4c5-4c990bbca6c6</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyActivityStream

Changes the audit policy state of a database activity stream to either locked (default) or unlocked.

```http
POST / HTTP/1.1

Action=ModifyActivityStream
&Version=2014-10-31
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyActivityStream is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>6517a18c-eed4-458c-b23b-c18b901487ee</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyCertificates

Override the system-default Secure Sockets Layer/Transport Layer Security (SSL/TLS) certificate for Amazon RDS for new DB instances, or remove the override.

```http
POST / HTTP/1.1

Action=ModifyCertificates
&Version=2014-10-31
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>Specify either CertificateIdentifier, to override the default certificate for new DB instances, or RemoveCustomerOverride, to remove an override that is set</Message>
  </Error>
  <RequestId>e20250b1-889d-41a2-8dad-b1d784326d52</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### ModifyCurrentDBClusterCapacity

Set the capacity of an Aurora Serverless v1 DB cluster to a specific value.

```http
POST / HTTP/1.1

Action=ModifyCurrentDBClusterCapacity
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyCurrentDBClusterCapacity is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>38d70555-2b55-41de-bd8c-abf223666bca</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyCustomDBEngineVersion

Modifies the status of a custom engine version (CEV).

```http
POST / HTTP/1.1

Action=ModifyCustomDBEngineVersion
&Version=2014-10-31
&Engine=example
&EngineVersion=example
```

```http
HTTP/1.1 403 Forbidden
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>AccessDenied</Code>
    <Message>The custom engine version you requested may not exist or you don't have the required permission.</Message>
  </Error>
  <RequestId>eca34c5d-1007-4c0d-926a-adb3f5614ff1</RequestId>
</ErrorResponse>
```

`403` `AccessDenied`: the request was refused by authorization.

### ModifyDBCapacityReservation

Modifies a DB capacity reservation.

```http
POST / HTTP/1.1

Action=ModifyDBCapacityReservation
&Version=2014-10-31
&DBCapacityReservationIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyDBCapacityReservation is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>0938dfec-8951-49c0-9f95-21fc694350d3</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyDBCluster

Modifies the settings of an Amazon Aurora DB cluster or a Multi-AZ DB cluster.

```http
POST / HTTP/1.1

Action=ModifyDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>DBCluster not found</Message>
  </Error>
  <RequestId>219a46ed-ce0e-4f3c-8082-f269427cf421</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyDBClusterEndpoint

Modifies the properties of an endpoint in an Amazon Aurora DB cluster.

```http
POST / HTTP/1.1

Action=ModifyDBClusterEndpoint
&Version=2014-10-31
&DBClusterEndpointIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyDBClusterEndpoint is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>6af7304f-2316-4ae0-9d33-2bd94c6b0a0b</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyDBClusterParameterGroup

Modifies the parameters of a DB cluster parameter group.

```http
POST / HTTP/1.1

Action=ModifyDBClusterParameterGroup
&Version=2014-10-31
&DBClusterParameterGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>MalformedInput</Code>
    <Message>1 validation error detected: Value null at 'parameters' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>fb197544-2326-4a4e-ac6f-471954332f1d</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### ModifyDBClusterSnapshotAttribute

Adds an attribute and values to, or removes an attribute and values from, a manual DB cluster snapshot.

```http
POST / HTTP/1.1

Action=ModifyDBClusterSnapshotAttribute
&Version=2014-10-31
&DBClusterSnapshotIdentifier=example
&AttributeName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationException</Code>
    <Message>invalid value 'example' for AttributeName: the only valid value is 'restore'</Message>
  </Error>
  <RequestId>000fd8da-b787-4b6f-a77a-5a33e7e2d36d</RequestId>
</ErrorResponse>
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### ModifyDBInstance

Modifies settings for a DB instance.

```http
POST / HTTP/1.1

Action=ModifyDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DB instance not found: example</Message>
  </Error>
  <RequestId>fc64d020-bfed-46ff-8458-1524dfe003bf</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyDBParameterGroup

Modifies the parameters of a DB parameter group.

```http
POST / HTTP/1.1

Action=ModifyDBParameterGroup
&Version=2014-10-31
&DBParameterGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>MalformedInput</Code>
    <Message>1 validation error detected: Value null at 'parameters' failed to satisfy constraint: Member must not be null</Message>
  </Error>
  <RequestId>a6ab0b19-b7ae-4175-a0a4-90cf81dbef05</RequestId>
</ErrorResponse>
```

`400` `MalformedInput`: the placeholder values failed the operation's input validation.

### ModifyDBProxy

Changes the settings for an existing DB proxy.

```http
POST / HTTP/1.1

Action=ModifyDBProxy
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBProxyNotFoundFault</Code>
    <Message>DBProxy 'example' not found.</Message>
  </Error>
  <RequestId>8c795933-30a6-4b26-b28e-3e90713149a0</RequestId>
</ErrorResponse>
```

`404` `DBProxyNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyDBProxyEndpoint

Changes the settings for an existing DB proxy endpoint.

```http
POST / HTTP/1.1

Action=ModifyDBProxyEndpoint
&Version=2014-10-31
&DBProxyEndpointName=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyDBProxyEndpoint is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>c6b236e6-bc61-4f62-af5a-f56a66db7461</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyDBProxyTargetGroup

Modifies the properties of a DBProxyTargetGroup .

```http
POST / HTTP/1.1

Action=ModifyDBProxyTargetGroup
&Version=2014-10-31
&TargetGroupName=example
&DBProxyName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBProxyNotFoundFault</Code>
    <Message>DBProxy 'example' not found.</Message>
  </Error>
  <RequestId>ea78a63e-2704-4ab5-8629-a5417affe495</RequestId>
</ErrorResponse>
```

`404` `DBProxyNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyDBRecommendation

Updates the recommendation status and recommended action status for the specified recommendation.

```http
POST / HTTP/1.1

Action=ModifyDBRecommendation
&Version=2014-10-31
&RecommendationId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyDBRecommendation is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>4e377c33-a603-43e4-bcc5-2cda7e143610</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyDBShardGroup

Modifies the settings of an Aurora Limitless Database DB shard group.

```http
POST / HTTP/1.1

Action=ModifyDBShardGroup
&Version=2014-10-31
&DBShardGroupIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBShardGroupNotFound</Code>
    <Message>The DB shard group with requested identifier in requested account doesn't exist.</Message>
  </Error>
  <RequestId>d6ad77f2-df93-4e25-a14f-82a5b20a2799</RequestId>
</ErrorResponse>
```

`404` `DBShardGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyDBSnapshot

Updates a manual DB snapshot with a new engine version.

```http
POST / HTTP/1.1

Action=ModifyDBSnapshot
&Version=2014-10-31
&DBSnapshotIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyDBSnapshot is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>fd2e00f9-3500-4ef7-9a51-c290dcf6333d</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyDBSnapshotAttribute

Adds an attribute and values to, or removes an attribute and values from, a manual DB snapshot.

```http
POST / HTTP/1.1

Action=ModifyDBSnapshotAttribute
&Version=2014-10-31
&DBSnapshotIdentifier=example
&AttributeName=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyDBSnapshotAttribute is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>cac4188b-835b-4330-9955-f51ad0fec371</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyDBSubnetGroup

Modifies an existing DB subnet group.

```http
POST / HTTP/1.1

Action=ModifyDBSubnetGroup
&Version=2014-10-31
&DBSubnetGroupName=example
&SubnetIds.SubnetIdentifier.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidSubnet</Code>
    <Message>'example' is not an EC2 subnet id (expected subnet- followed by 8 or 17 hexadecimal characters)</Message>
  </Error>
  <RequestId>d61c7f18-08f3-4494-99b5-8e0d0fcc10e2</RequestId>
</ErrorResponse>
```

`400` `InvalidSubnet`.

### ModifyEventSubscription

Modifies an existing RDS event notification subscription.

```http
POST / HTTP/1.1

Action=ModifyEventSubscription
&Version=2014-10-31
&SubscriptionName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>SubscriptionNotFound</Code>
    <Message>Event Subscription example not found.</Message>
  </Error>
  <RequestId>37cbcfe0-2884-4ae4-9f28-ef11fe79f600</RequestId>
</ErrorResponse>
```

`404` `SubscriptionNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyGlobalCluster

Modifies a setting for an Amazon Aurora global database cluster.

```http
POST / HTTP/1.1

Action=ModifyGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalClusterNotFoundFault</Code>
    <Message>Global cluster 'example' not found</Message>
  </Error>
  <RequestId>2e0e46ce-3d20-4cc5-8d70-bbd4cef2607d</RequestId>
</ErrorResponse>
```

`404` `GlobalClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyIntegration

Modifies a zero-ETL integration with Amazon Redshift.

```http
POST / HTTP/1.1

Action=ModifyIntegration
&Version=2014-10-31
&IntegrationIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyIntegration is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>07ac8a99-149b-4c4b-9088-c12509243f8e</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### ModifyOptionGroup

Modifies an existing option group.

```http
POST / HTTP/1.1

Action=ModifyOptionGroup
&Version=2014-10-31
&OptionGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>OptionGroupNotFoundFault</Code>
    <Message>Specified OptionGroupName: example not found.</Message>
  </Error>
  <RequestId>91ebd26a-b386-42f2-bcb9-8f4ed1423ebc</RequestId>
</ErrorResponse>
```

`404` `OptionGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyTenantDatabase

Modifies an existing tenant database in a DB instance.

```http
POST / HTTP/1.1

Action=ModifyTenantDatabase
&Version=2014-10-31
&DBInstanceIdentifier=example
&TenantDBName=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation ModifyTenantDatabase is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>7b6cde45-c94b-4a66-a3c9-2f75b5107bad</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### PromoteReadReplica

Promotes a read replica DB instance to a standalone DB instance.

```http
POST / HTTP/1.1

Action=PromoteReadReplica
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance example not found.</Message>
  </Error>
  <RequestId>47a380e8-72e2-4e07-af1c-d9e224fd9cf8</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PromoteReadReplicaDBCluster

Promotes a read replica DB cluster to a standalone DB cluster.

```http
POST / HTTP/1.1

Action=PromoteReadReplicaDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>The source cluster could not be found or cannot be accessed: example</Message>
  </Error>
  <RequestId>13aa0e62-b9f2-4075-804e-f3f3b9057cf1</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### PurchaseReservedDBInstancesOffering

Purchases a reserved DB instance offering.

```http
POST / HTTP/1.1

Action=PurchaseReservedDBInstancesOffering
&Version=2014-10-31
&ReservedDBInstancesOfferingId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation PurchaseReservedDBInstancesOffering is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>f1ac1b6b-681a-4f05-b79e-3950999c1bce</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RebootDBCluster

You might need to reboot your DB cluster, usually for maintenance reasons.

```http
POST / HTTP/1.1

Action=RebootDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>DBCluster example not found.</Message>
  </Error>
  <RequestId>bbeb53cb-7d1e-49cb-9c32-87e0438a814e</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RebootDBInstance

You might need to reboot your DB instance, usually for maintenance reasons.

```http
POST / HTTP/1.1

Action=RebootDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance not found: example</Message>
  </Error>
  <RequestId>4d21bddb-da2a-45a9-899d-86b92e459e6c</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RebootDBShardGroup

You might need to reboot your DB shard group, usually for maintenance reasons.

```http
POST / HTTP/1.1

Action=RebootDBShardGroup
&Version=2014-10-31
&DBShardGroupIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBShardGroupNotFound</Code>
    <Message>DB shard group not found or can not be accessed</Message>
  </Error>
  <RequestId>4162c1f5-c427-47de-967a-3765aca5a705</RequestId>
</ErrorResponse>
```

`404` `DBShardGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RegisterDBProxyTargets

Associate one or more DBProxyTarget data structures with a DBProxyTargetGroup .

```http
POST / HTTP/1.1

Action=RegisterDBProxyTargets
&Version=2014-10-31
&DBProxyName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>Missing DB instance or DB cluster identifier.</Message>
  </Error>
  <RequestId>c757f72a-3bd2-4cff-ab60-588aaa13c6b2</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RemoveFromGlobalCluster

Detaches an Aurora secondary cluster from an Aurora global database cluster.

```http
POST / HTTP/1.1

Action=RemoveFromGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
&DbClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalClusterNotFoundFault</Code>
    <Message>Global cluster 'example' not found</Message>
  </Error>
  <RequestId>bd2c5fa6-f5a8-40c7-9321-e54de3e08d81</RequestId>
</ErrorResponse>
```

`404` `GlobalClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveRoleFromDBCluster

Removes the asssociation of an Amazon Web Services Identity and Access Management (IAM) role from a DB cluster.

```http
POST / HTTP/1.1

Action=RemoveRoleFromDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
&RoleArn=example0000000000000
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>1 validation error detected: Value 'example0000000000000' at 'roleArn' failed to satisfy constraint: Member must satisfy regular expression pattern: ^arn:aws[a-z-]*:iam::[0-9]*:role/.*$</Message>
  </Error>
  <RequestId>0efdb2fe-1ce1-4507-8866-820bebbdc3c9</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### RemoveRoleFromDBInstance

Disassociates an Amazon Web Services Identity and Access Management (IAM) role from a DB instance.

```http
POST / HTTP/1.1

Action=RemoveRoleFromDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
&RoleArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&FeatureName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid Role ARN: arn:aws:rds:us-east-1:000000000000:example/example</Message>
  </Error>
  <RequestId>170010d1-1260-4aa2-b2e8-03c490cac300</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### RemoveSourceIdentifierFromSubscription

Removes a source identifier from an existing RDS event notification subscription.

```http
POST / HTTP/1.1

Action=RemoveSourceIdentifierFromSubscription
&Version=2014-10-31
&SubscriptionName=example
&SourceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>SubscriptionNotFound</Code>
    <Message>Event Subscription example not found.</Message>
  </Error>
  <RequestId>9d4c3613-daa8-4361-8455-6c5e89ead343</RequestId>
</ErrorResponse>
```

`404` `SubscriptionNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveTagsFromResource

Removes metadata tags from an Amazon RDS resource.

```http
POST / HTTP/1.1

Action=RemoveTagsFromResource
&Version=2014-10-31
&ResourceName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid resource name: example</Message>
  </Error>
  <RequestId>6d5ccf7b-be2c-4d04-8469-abfe84eeb3f1</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ResetDBClusterParameterGroup

Modifies the parameters of a DB cluster parameter group to the default value.

```http
POST / HTTP/1.1

Action=ResetDBClusterParameterGroup
&Version=2014-10-31
&DBClusterParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBClusterParameterGroup not found: example</Message>
  </Error>
  <RequestId>d2da8da7-52e5-4394-91cc-e62350b01c4b</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ResetDBParameterGroup

Modifies the parameters of a DB parameter group to the engine/system default value.

```http
POST / HTTP/1.1

Action=ResetDBParameterGroup
&Version=2014-10-31
&DBParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBParameterGroupNotFound</Code>
    <Message>DBParameterGroup not found: example</Message>
  </Error>
  <RequestId>9f7f6ad8-daf9-4521-8b73-7a1029f6cefd</RequestId>
</ErrorResponse>
```

`404` `DBParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RestoreDBClusterFromS3

Creates an Amazon Aurora DB cluster from MySQL data stored in an Amazon S3 bucket.

```http
POST / HTTP/1.1

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
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation RestoreDBClusterFromS3 is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>81b099f3-a7f1-429b-acfb-0e91a0840edf</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RestoreDBClusterFromSnapshot

Creates a new DB cluster from a DB snapshot or DB cluster snapshot.

```http
POST / HTTP/1.1

Action=RestoreDBClusterFromSnapshot
&Version=2014-10-31
&DBClusterIdentifier=example
&SnapshotIdentifier=example
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid DB engine: example is not one of aurora-mysql, aurora-postgresql, docdb, mysql, neptune, postgres</Message>
  </Error>
  <RequestId>54ea68a6-1491-4126-8d3a-4306b58ed2ac</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### RestoreDBClusterToPointInTime

Restores a DB cluster to an arbitrary point in time.

```http
POST / HTTP/1.1

Action=RestoreDBClusterToPointInTime
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation RestoreDBClusterToPointInTime is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>7e74eb0f-c1dc-4db5-9e96-7323fa4e063f</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RestoreDBInstanceFromDBSnapshot

Creates a new DB instance from a DB snapshot.

```http
POST / HTTP/1.1

Action=RestoreDBInstanceFromDBSnapshot
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>DBSnapshotIdentifier must be specified when DBClusterSnapshotIdentifier isn't specified</Message>
  </Error>
  <RequestId>2d4de323-1f48-49c4-a253-caf639b11669</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RestoreDBInstanceFromS3

Amazon Relational Database Service (Amazon RDS) supports importing MySQL databases by using backup files.

```http
POST / HTTP/1.1

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
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation RestoreDBInstanceFromS3 is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>2b5cca14-b43e-4dc1-adbe-c3b5a97ac4b6</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RestoreDBInstanceToPointInTime

Restores a DB instance to an arbitrary point in time.

```http
POST / HTTP/1.1

Action=RestoreDBInstanceToPointInTime
&Version=2014-10-31
&TargetDBInstanceIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation RestoreDBInstanceToPointInTime is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>7073634d-b4f5-4431-934e-d9f0ec152a03</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### RevokeDBSecurityGroupIngress

Revokes ingress from a DBSecurityGroup for previously authorized IP ranges or EC2 or VPC security groups.

```http
POST / HTTP/1.1

Action=RevokeDBSecurityGroupIngress
&Version=2014-10-31
&DBSecurityGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBSecurityGroupNotFound</Code>
    <Message>DBSecurityGroup not found: example</Message>
  </Error>
  <RequestId>7610608c-e1e7-40db-b019-e8f62e716e46</RequestId>
</ErrorResponse>
```

`404` `DBSecurityGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartActivityStream

Starts a database activity stream to monitor activity on the database.

```http
POST / HTTP/1.1

Action=StartActivityStream
&Version=2014-10-31
&ResourceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
&Mode=sync
&KmsKeyId=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation StartActivityStream is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>8435bb3a-f3e2-4d8b-8ff9-20aee2d3c670</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### StartDBCluster

Starts an Amazon Aurora DB cluster that was stopped using the Amazon Web Services console, the stop-db-cluster CLI command, or the StopDBCluster operation.

```http
POST / HTTP/1.1

Action=StartDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>DBCluster example not found.</Message>
  </Error>
  <RequestId>fa29685a-1fc8-4a06-b1ba-dbade8a831f8</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartDBInstance

Starts an Amazon RDS DB instance that was stopped using the Amazon Web Services console, the stop-db-instance CLI command, or the StopDBInstance operation.

```http
POST / HTTP/1.1

Action=StartDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance example not found.</Message>
  </Error>
  <RequestId>8a643355-ae2d-45bd-b7b9-a286db7b1f5d</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StartDBInstanceAutomatedBackupsReplication

Enables replication of automated backups to a different Amazon Web Services Region.

```http
POST / HTTP/1.1

Action=StartDBInstanceAutomatedBackupsReplication
&Version=2014-10-31
&SourceDBInstanceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation StartDBInstanceAutomatedBackupsReplication is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>5914f8e5-a43b-4118-aea9-f2fd48bb798f</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### StartExportTask

Starts an export of DB snapshot or DB cluster data to Amazon S3.

```http
POST / HTTP/1.1

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

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>KMSKeyNotAccessibleFault</Code>
    <Message>The KMS key example doesn't exist or is disabled.</Message>
  </Error>
  <RequestId>8e7adb99-98e6-471d-a623-95404504d9f1</RequestId>
</ErrorResponse>
```

`400` `KMSKeyNotAccessibleFault`.

### StopActivityStream

Stops a database activity stream that was started using the Amazon Web Services console, the start-activity-stream CLI command, or the StartActivityStream operation.

```http
POST / HTTP/1.1

Action=StopActivityStream
&Version=2014-10-31
&ResourceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation StopActivityStream is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>b1f8ff2f-9299-43f8-9c6b-ddf373d3fb74</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### StopDBCluster

Stops an Amazon Aurora DB cluster.

```http
POST / HTTP/1.1

Action=StopDBCluster
&Version=2014-10-31
&DBClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBClusterNotFoundFault</Code>
    <Message>DBCluster example not found.</Message>
  </Error>
  <RequestId>062bffb2-007a-4c37-a23c-20a00cc240e9</RequestId>
</ErrorResponse>
```

`404` `DBClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StopDBInstance

Stops an Amazon RDS DB instance temporarily.

```http
POST / HTTP/1.1

Action=StopDBInstance
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>DBInstanceNotFound</Code>
    <Message>DBInstance example not found.</Message>
  </Error>
  <RequestId>b53c6730-3cb6-4f36-ad2f-74af1caf5d6d</RequestId>
</ErrorResponse>
```

`404` `DBInstanceNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### StopDBInstanceAutomatedBackupsReplication

Stops automated backup replication for a DB instance.

```http
POST / HTTP/1.1

Action=StopDBInstanceAutomatedBackupsReplication
&Version=2014-10-31
&SourceDBInstanceArn=arn%3Aaws%3Ards%3Aus-east-1%3A000000000000%3Aexample%2Fexample
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation StopDBInstanceAutomatedBackupsReplication is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>09930c4e-f58b-47ea-9e00-e80b58cb4ca9</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### SwitchoverBlueGreenDeployment

Switches over a blue/green deployment.

```http
POST / HTTP/1.1

Action=SwitchoverBlueGreenDeployment
&Version=2014-10-31
&BlueGreenDeploymentIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation SwitchoverBlueGreenDeployment is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>e455befc-7ed0-43f9-adca-36d4feb54e0f</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### SwitchoverGlobalCluster

Switches over the specified secondary DB cluster to be the new primary DB cluster in the global database cluster.

```http
POST / HTTP/1.1

Action=SwitchoverGlobalCluster
&Version=2014-10-31
&GlobalClusterIdentifier=example
&TargetDbClusterIdentifier=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalClusterNotFoundFault</Code>
    <Message>Global cluster 'example' not found</Message>
  </Error>
  <RequestId>8fd7ac4f-8ae7-455a-baff-cd45fe498e22</RequestId>
</ErrorResponse>
```

`404` `GlobalClusterNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### SwitchoverReadReplica

Switches over an Oracle standby database in an Oracle Data Guard environment, making it the new primary database.

```http
POST / HTTP/1.1

Action=SwitchoverReadReplica
&Version=2014-10-31
&DBInstanceIdentifier=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://rds.amazonaws.com/doc/2014-10-31/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation SwitchoverReadReplica is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>5d7d5040-3efd-47ee-89f6-cd0f32571d23</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

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
