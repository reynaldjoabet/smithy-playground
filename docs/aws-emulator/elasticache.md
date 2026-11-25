# Amazon ElastiCache

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `ElastiCache`
- **sigv4 signing name:** `elasticache`
- **Endpoint prefix:** `elasticache`
- **ARN namespace:** `elasticache`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-16. Requests are signed with SigV4 for Region `us-east-1` and signing name `elasticache`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

The AWS CLI and SDKs need nothing more than the environment variables from the [Docker setup](README.md#running-the-emulator-in-docker). With plain `curl`, which signs requests itself, this is `DescribeCacheClusters`:

```sh
KEY=$(awk -F' *= *' '/aws_access_key_id/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
SECRET=$(awk -F' *= *' '/aws_secret_access_key/ {print $2}' ~/.aws/emulator/9297/emulator_credentials)
curl -sS --aws-sigv4 "aws:amz:us-east-1:elasticache" --user "$KEY:$SECRET" \
  --cacert ~/.aws/emulator/9297/ca.pem \
  -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded; charset=utf-8' \
  --data-binary 'Action=DescribeCacheClusters&Version=2015-02-02' \
  'https://127.0.0.1:9297/'
```

## End-to-end walkthrough

Create a cache subnet group and a Redis cluster, inspect them, and delete them. Each step is one real call; values such as ids and ARNs come from the responses of earlier steps.

### 1. CreateCacheSubnetGroup

A subnet group says which subnets the cache nodes may be placed in.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120533Z
Content-Length: 227
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateCacheSubnetGroup
&Version=2015-02-02
&CacheSubnetGroupName=docs-demo
&CacheSubnetGroupDescription=docs%20demo
&SubnetIds.SubnetIdentifier.1=subnet-00000000000000007
&SubnetIds.SubnetIdentifier.2=subnet-00000000000000009
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1129
date: Mon, 16 Nov 2026 12:05:33 GMT

<CreateCacheSubnetGroupResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <CreateCacheSubnetGroupResult>
    <CacheSubnetGroup xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <CacheSubnetGroupName>docs-demo</CacheSubnetGroupName>
      <CacheSubnetGroupDescription>docs demo</CacheSubnetGroupDescription>
      <VpcId>vpc-00000000000000001</VpcId>
      <Subnets>
        <Subnet>
          <SubnetIdentifier>subnet-00000000000000007</SubnetIdentifier>
          <SubnetAvailabilityZone>
            <Name>us-east-1a</Name>
          </SubnetAvailabilityZone>
          <SupportedNetworkTypes>
            <member>ipv4</member>
          </SupportedNetworkTypes>
        </Subnet>
        <Subnet>
          <SubnetIdentifier>subnet-00000000000000009</SubnetIdentifier>
          <SubnetAvailabilityZone>
            <Name>us-east-1b</Name>
          </SubnetAvailabilityZone>
          <SupportedNetworkTypes>
            <member>ipv4</member>
          </SupportedNetworkTypes>
        </Subnet>
      </Subnets>
      <ARN>arn:aws:elasticache:us-east-1:000000000000:subnetgroup:docs-demo</ARN>
      <SupportedNetworkTypes>
        <member>ipv4</member>
      </SupportedNetworkTypes>
    </CacheSubnetGroup>
  </CreateCacheSubnetGroupResult>
  <ResponseMetadata>
    <RequestId>877f32cc-d622-4e16-a139-a6df7004a837</RequestId>
  </ResponseMetadata>
</CreateCacheSubnetGroupResponse>
```

### 2. CreateCacheCluster

The cluster starts in `creating`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120533Z
Content-Length: 164
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateCacheCluster
&Version=2015-02-02
&CacheClusterId=docs-demo-redis
&NumCacheNodes=1
&CacheNodeType=cache.t3.micro
&Engine=redis
&CacheSubnetGroupName=docs-demo
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1272
date: Mon, 16 Nov 2026 12:05:33 GMT

<CreateCacheClusterResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <CreateCacheClusterResult>
    <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <CacheClusterId>docs-demo-redis</CacheClusterId>
      <ConfigurationEndpoint>
        <Address>127.0.0.1</Address>
        <Port>42115</Port>
      </ConfigurationEndpoint>
      <CacheNodeType>cache.t3.micro</CacheNodeType>
      <Engine>redis</Engine>
      <EngineVersion>7.1</EngineVersion>
      <CacheClusterStatus>creating</CacheClusterStatus>
      <NumCacheNodes>1</NumCacheNodes>
      <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
      <CacheClusterCreateTime>2026-11-16T12:05:33.495341Z</CacheClusterCreateTime>
      <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
      <CacheParameterGroup>
        <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
        <ParameterApplyStatus>in-sync</ParameterApplyStatus>
      </CacheParameterGroup>
      <CacheSubnetGroupName>docs-demo</CacheSubnetGroupName>
      <SnapshotRetentionLimit>0</SnapshotRetentionLimit>
      <AuthTokenEnabled>false</AuthTokenEnabled>
      <ARN>arn:aws:elasticache:us-east-1:000000000000:cluster:docs-demo-redis</ARN>
    </CacheCluster>
  </CreateCacheClusterResult>
  <ResponseMetadata>
    <RequestId>95352d7d-1d02-4994-bd0e-3a6e2e2f2dcb</RequestId>
  </ResponseMetadata>
</CreateCacheClusterResponse>
```

### 3. DescribeCacheClusters

Poll until `available`. The node's endpoint is a local address and port the emulator opened for it.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120533Z
Content-Length: 101
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheClusters
&Version=2015-02-02
&CacheClusterId=docs-demo-redis
&ShowCacheNodeInfo=true
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1691
date: Mon, 16 Nov 2026 12:05:34 GMT

<DescribeCacheClustersResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeCacheClustersResult>
    <CacheClusters>
      <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <CacheClusterId>docs-demo-redis</CacheClusterId>
        <ConfigurationEndpoint>
          <Address>127.0.0.1</Address>
          <Port>42115</Port>
        </ConfigurationEndpoint>
        <CacheNodeType>cache.t3.micro</CacheNodeType>
        <Engine>redis</Engine>
        <EngineVersion>7.1</EngineVersion>
        <CacheClusterStatus>available</CacheClusterStatus>
        <NumCacheNodes>1</NumCacheNodes>
        <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
        <CacheClusterCreateTime>2026-11-16T12:05:33.495341Z</CacheClusterCreateTime>
        <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
        <CacheParameterGroup>
          <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
          <ParameterApplyStatus>in-sync</ParameterApplyStatus>
        </CacheParameterGroup>
        <CacheSubnetGroupName>docs-demo</CacheSubnetGroupName>
        <CacheNodes>
          <CacheNode>
            <CacheNodeId>0001</CacheNodeId>
            <CacheNodeStatus>available</CacheNodeStatus>
            <CacheNodeCreateTime>2026-11-16T12:05:33.495341Z</CacheNodeCreateTime>
            <Endpoint>
              <Address>127.0.0.1</Address>
              <Port>42115</Port>
            </Endpoint>
            <ParameterGroupStatus>in-sync</ParameterGroupStatus>
            <CustomerAvailabilityZone>us-east-1a</CustomerAvailabilityZone>
          </CacheNode>
        </CacheNodes>
        <SnapshotRetentionLimit>0</SnapshotRetentionLimit>
        <AuthTokenEnabled>false</AuthTokenEnabled>
        <ARN>arn:aws:elasticache:us-east-1:000000000000:cluster:docs-demo-redis</ARN>
      </CacheCluster>
    </CacheClusters>
  </DescribeCacheClustersResult>
  <ResponseMetadata>
    <RequestId>fd9f489b-239f-43c3-bcce-c76b1f8eac19</RequestId>
  </ResponseMetadata>
</DescribeCacheClustersResponse>
```

### 4. DescribeCacheClusters

An unknown id answers `CacheClusterNotFound`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheClusters
&Version=2015-02-02
&CacheClusterId=nope
```

**Response**

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 3bd30b62-3218-4e37-904c-1c4be62426e4
content-length: 261
date: Mon, 16 Nov 2026 12:05:34 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheClusterNotFound</Code>
    <Message>CacheCluster not found: nope</Message>
  </Error>
  <RequestId>3bd30b62-3218-4e37-904c-1c4be62426e4</RequestId>
</ErrorResponse>
```

### 5. DeleteCacheCluster

Deletion moves the cluster to `deleting`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteCacheCluster
&Version=2015-02-02
&CacheClusterId=docs-demo-redis
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1272
date: Mon, 16 Nov 2026 12:05:34 GMT

<DeleteCacheClusterResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DeleteCacheClusterResult>
    <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <CacheClusterId>docs-demo-redis</CacheClusterId>
      <ConfigurationEndpoint>
        <Address>127.0.0.1</Address>
        <Port>42115</Port>
      </ConfigurationEndpoint>
      <CacheNodeType>cache.t3.micro</CacheNodeType>
      <Engine>redis</Engine>
      <EngineVersion>7.1</EngineVersion>
      <CacheClusterStatus>deleting</CacheClusterStatus>
      <NumCacheNodes>1</NumCacheNodes>
      <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
      <CacheClusterCreateTime>2026-11-16T12:05:33.495341Z</CacheClusterCreateTime>
      <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
      <CacheParameterGroup>
        <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
        <ParameterApplyStatus>in-sync</ParameterApplyStatus>
      </CacheParameterGroup>
      <CacheSubnetGroupName>docs-demo</CacheSubnetGroupName>
      <SnapshotRetentionLimit>0</SnapshotRetentionLimit>
      <AuthTokenEnabled>false</AuthTokenEnabled>
      <ARN>arn:aws:elasticache:us-east-1:000000000000:cluster:docs-demo-redis</ARN>
    </CacheCluster>
  </DeleteCacheClusterResult>
  <ResponseMetadata>
    <RequestId>0e65e3d0-3cfe-4184-a33b-d33d3021df80</RequestId>
  </ResponseMetadata>
</DeleteCacheClusterResponse>
```

### 6. DescribeCacheClusters

Poll until the cluster is gone, so the subnet group is free.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheClusters
&Version=2015-02-02
&CacheClusterId=docs-demo-redis
```

**Response**

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 96cb1177-b25a-443c-ad66-b68c49176ec5
content-length: 272
date: Mon, 16 Nov 2026 12:05:34 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheClusterNotFound</Code>
    <Message>CacheCluster not found: docs-demo-redis</Message>
  </Error>
  <RequestId>96cb1177-b25a-443c-ad66-b68c49176ec5</RequestId>
</ErrorResponse>
```

### 7. DeleteCacheSubnetGroup

Remove the subnet group.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261116T120534Z
Content-Length: 79
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261116/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteCacheSubnetGroup
&Version=2015-02-02
&CacheSubnetGroupName=docs-demo
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 279
date: Mon, 16 Nov 2026 12:05:34 GMT

<DeleteCacheSubnetGroupResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DeleteCacheSubnetGroupResult/>
  <ResponseMetadata>
    <RequestId>64fc38b5-e088-4f3d-b16d-403614b90d5e</RequestId>
  </ResponseMetadata>
</DeleteCacheSubnetGroupResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Headers repeated on every request (`Host`, `X-Amz-Date`, `Authorization`, `Content-Length`, and the fixed `Content-Type`) are left out here; the walkthrough shows them in full.

Of 77 operations, 20 accepted the placeholder call, 1 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AddTagsToResource

A tag is a key-value pair where the key and value are case-sensitive.

```http
POST / HTTP/1.1

Action=AddTagsToResource
&Version=2015-02-02
&ResourceName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter Tags must be provided.</Message>
  </Error>
  <RequestId>61ebde84-a94c-4f15-8a11-02b66ff2ac33</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AuthorizeCacheSecurityGroupIngress

Allows network ingress to a cache security group.

```http
POST / HTTP/1.1

Action=AuthorizeCacheSecurityGroupIngress
&Version=2015-02-02
&CacheSecurityGroupName=example
&EC2SecurityGroupName=example
&EC2SecurityGroupOwnerId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Use of cache security groups is not permitted in this API version for your account.</Message>
  </Error>
  <RequestId>e8afe9b6-9ae8-4022-bd5c-b8aa4be620a8</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### BatchApplyUpdateAction

Apply the service update.

```http
POST / HTTP/1.1

Action=BatchApplyUpdateAction
&Version=2015-02-02
&ServiceUpdateName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>CacheClusterIds or ReplicationGroupIds must name at least one resource to apply service update example to.</Message>
  </Error>
  <RequestId>9be4c3d1-475b-4a25-801c-d879ddfe6c08</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### BatchStopUpdateAction

Stop the service update.

```http
POST / HTTP/1.1

Action=BatchStopUpdateAction
&Version=2015-02-02
&ServiceUpdateName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>CacheClusterIds or ReplicationGroupIds must name at least one resource to stop service update example on.</Message>
  </Error>
  <RequestId>bfc72e09-3b71-454a-956e-a0d8f3e2f99f</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CompleteMigration

Completes the migration of data.

```http
POST / HTTP/1.1

Action=CompleteMigration
&Version=2015-02-02
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Either ReplicationGroupId or ServerlessCacheName must be provided to name the target of the migration.</Message>
  </Error>
  <RequestId>e7745464-2be0-4725-b034-4b72c23a5cce</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CopyServerlessCacheSnapshot

Creates a copy of an existing serverless cache’s snapshot.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=CopyServerlessCacheSnapshot
&Version=2015-02-02
&SourceServerlessCacheSnapshotName=example
&TargetServerlessCacheSnapshotName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheSnapshotNotFoundFault</Code>
    <Message>Source serverless cache snapshot does not exist.</Message>
  </Error>
  <RequestId>39c141c0-8689-4f95-b8a5-eaccdcf0f44f</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopySnapshot

Makes a copy of an existing snapshot.

```http
POST / HTTP/1.1

Action=CopySnapshot
&Version=2015-02-02
&SourceSnapshotName=example
&TargetSnapshotName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>SnapshotNotFoundFault</Code>
    <Message>Snapshot with name example not found</Message>
  </Error>
  <RequestId>7c12a782-dd48-4298-b5d2-19df1ca4accb</RequestId>
</ErrorResponse>
```

`404` `SnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateCacheCluster

Creates a cluster.

```http
POST / HTTP/1.1

Action=CreateCacheCluster
&Version=2015-02-02
&CacheClusterId=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateCacheClusterResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <CreateCacheClusterResult>
    <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <CacheClusterId>example</CacheClusterId>
      <ConfigurationEndpoint>
        <Address>127.0.0.1</Address>
        <Port>45509</Port>
      </ConfigurationEndpoint>
      <Engine>redis</Engine>
      <EngineVersion>7.1</EngineVersion>
      <CacheClusterStatus>creating</CacheClusterStatus>
      <NumCacheNodes>1</NumCacheNodes>
      <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
      <CacheClusterCreateTime>2026-11-16T12:11:29.301631Z</CacheClusterCreateTime>
      <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
      <CacheParameterGroup>
        <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
        <ParameterApplyStatus>in-sync</ParameterApplyStatus>
      </CacheParameterGroup>
      <SnapshotRetentionLimit>0</SnapshotRetentionLimit>
… (8 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-createcachecluster) for a successful call.

### CreateCacheParameterGroup

Creates a new Amazon ElastiCache cache parameter group.

```http
POST / HTTP/1.1

Action=CreateCacheParameterGroup
&Version=2015-02-02
&CacheParameterGroupName=example
&CacheParameterGroupFamily=example
&Description=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>CacheParameterGroupFamily example is not a valid parameter group family.</Message>
  </Error>
  <RequestId>8056997f-ea08-401b-88f2-6233b6bc3800</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateCacheSecurityGroup

Creates a new cache security group.

```http
POST / HTTP/1.1

Action=CreateCacheSecurityGroup
&Version=2015-02-02
&CacheSecurityGroupName=example
&Description=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Use of cache security groups is not permitted in this API version for your account.</Message>
  </Error>
  <RequestId>995160c9-5452-4423-adc0-25107954d506</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateCacheSubnetGroup

Creates a new cache subnet group.

```http
POST / HTTP/1.1

Action=CreateCacheSubnetGroup
&Version=2015-02-02
&CacheSubnetGroupName=example
&CacheSubnetGroupDescription=example
&SubnetIds.SubnetIdentifier.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidSubnet</Code>
    <Message>Invalid value example for SubnetIds: not a subnet id.</Message>
  </Error>
  <RequestId>a79cf3f8-7dbc-4547-9d42-2c03749c8863</RequestId>
</ErrorResponse>
```

`400` `InvalidSubnet`. See [the walkthrough](#1-createcachesubnetgroup) for a successful call.

### CreateGlobalReplicationGroup

Global Datastore offers fully managed, fast, reliable and secure cross-region replication.

```http
POST / HTTP/1.1

Action=CreateGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupIdSuffix=example
&PrimaryReplicationGroupId=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ReplicationGroupNotFoundFault</Code>
    <Message>Replication group 'example' not found</Message>
  </Error>
  <RequestId>5e90912e-5fa6-4bc0-a71c-e4208d3c18c3</RequestId>
</ErrorResponse>
```

`404` `ReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateReplicationGroup

Creates a Valkey or Redis OSS (cluster mode disabled) or a Valkey or Redis OSS (cluster mode enabled) replication group.

```http
POST / HTTP/1.1

Action=CreateReplicationGroup
&Version=2015-02-02
&ReplicationGroupId=example
&ReplicationGroupDescription=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<CreateReplicationGroupResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <CreateReplicationGroupResult>
    <ReplicationGroup xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <ReplicationGroupId>example</ReplicationGroupId>
      <Description>example</Description>
      <Status>creating</Status>
      <MemberClusters>
        <ClusterId>example-001</ClusterId>
      </MemberClusters>
      <NodeGroups>
        <NodeGroup>
          <NodeGroupId>0001</NodeGroupId>
          <Status>creating</Status>
          <PrimaryEndpoint>
            <Address>127.0.0.1</Address>
            <Port>42339</Port>
          </PrimaryEndpoint>
          <NodeGroupMembers>
            <NodeGroupMember>
              <CacheClusterId>example-001</CacheClusterId>
… (28 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateServerlessCache

Creates a serverless cache.

```http
POST / HTTP/1.1

Action=CreateServerlessCache
&Version=2015-02-02
&ServerlessCacheName=example
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Specified engine does not support serverless cache.</Message>
  </Error>
  <RequestId>2ceb8708-1fc5-4129-96bb-90601e0d2b44</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateServerlessCacheSnapshot

This API creates a copy of an entire ServerlessCache at a specific moment in time.

```http
POST / HTTP/1.1

Action=CreateServerlessCacheSnapshot
&Version=2015-02-02
&ServerlessCacheSnapshotName=example
&ServerlessCacheName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheNotFoundFault</Code>
    <Message>Serverless cache with name example not found.</Message>
  </Error>
  <RequestId>91c27103-5cd9-402a-b89c-3c537a98755f</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateSnapshot

Creates a copy of an entire cluster or replication group at a specific moment in time.

```http
POST / HTTP/1.1

Action=CreateSnapshot
&Version=2015-02-02
&SnapshotName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>Either CacheClusterId or ReplicationGroupId must be specified: a snapshot is taken of an existing cluster or replication group.</Message>
  </Error>
  <RequestId>aed378e6-0c11-4be1-ae69-8ea13fbad182</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### CreateUser

For Valkey engine version 7.2 onwards and Redis OSS 6.0 to 7.1: Creates a user.

```http
POST / HTTP/1.1

Action=CreateUser
&Version=2015-02-02
&UserId=example
&UserName=example
&Engine=example
&AccessString=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Access control options are available for Redis version 6.0 onwards.</Message>
  </Error>
  <RequestId>01ae8d09-c107-4801-b0d3-48aec7d03958</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateUserGroup

For Valkey engine version 7.2 onwards and Redis OSS 6.0 to 7.1: Creates a user group.

```http
POST / HTTP/1.1

Action=CreateUserGroup
&Version=2015-02-02
&UserGroupId=example
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Access control options are available for Redis version 6.0 onwards.</Message>
  </Error>
  <RequestId>76bce913-36b3-4db9-9752-f420583d472c</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DecreaseNodeGroupsInGlobalReplicationGroup

Decreases the number of node groups in a Global datastore

```http
POST / HTTP/1.1

Action=DecreaseNodeGroupsInGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=example
&NodeGroupCount=1
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: this emulator applies a Global datastore change at once, and the GlobalReplicationGroup shape has no member to report a pending one.</Message>
  </Error>
  <RequestId>50678c7a-c613-4e84-b9e9-f0ad1fc6b0ab</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DecreaseReplicaCount

Dynamically decreases the number of replicas in a Valkey or Redis OSS (cluster mode disabled) replication group or the number of replica nodes in one or more node groups (shards) of a Valkey or Redis OSS (cluster mode enabled) replication group.

```http
POST / HTTP/1.1

Action=DecreaseReplicaCount
&Version=2015-02-02
&ReplicationGroupId=example
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: ApplyImmediately=False is not currently supported.</Message>
  </Error>
  <RequestId>3a4de0ff-88a2-4956-8643-f84dfdc19fc4</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteCacheCluster

Deletes a previously provisioned cluster.

```http
POST / HTTP/1.1

Action=DeleteCacheCluster
&Version=2015-02-02
&CacheClusterId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidCacheClusterState</Code>
    <Message>Cache cluster example is in creating state, not available.</Message>
  </Error>
  <RequestId>f080c18c-81a1-4494-8b4b-1a0efa283ac0</RequestId>
</ErrorResponse>
```

`400` `InvalidCacheClusterState`. See [the walkthrough](#5-deletecachecluster) for a successful call.

### DeleteCacheParameterGroup

Deletes the specified cache parameter group.

```http
POST / HTTP/1.1

Action=DeleteCacheParameterGroup
&Version=2015-02-02
&CacheParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheParameterGroupNotFound</Code>
    <Message>CacheParameterGroupnot found: example</Message>
  </Error>
  <RequestId>c279657b-de0d-4e97-b60c-15415fa0b9b7</RequestId>
</ErrorResponse>
```

`404` `CacheParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteCacheSecurityGroup

Deletes a cache security group.

```http
POST / HTTP/1.1

Action=DeleteCacheSecurityGroup
&Version=2015-02-02
&CacheSecurityGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Use of cache security groups is not permitted in this API version for your account.</Message>
  </Error>
  <RequestId>a5601b8b-1fd1-4cb3-ae2a-951291c0b6d9</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteCacheSubnetGroup

Deletes a cache subnet group.

```http
POST / HTTP/1.1

Action=DeleteCacheSubnetGroup
&Version=2015-02-02
&CacheSubnetGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheSubnetGroupNotFoundFault</Code>
    <Message>Cache Subnet Group example does not exist.</Message>
  </Error>
  <RequestId>76e24aba-7935-4dd6-b66f-4fe7c92e56e0</RequestId>
</ErrorResponse>
```

`400` `CacheSubnetGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deletecachesubnetgroup) for a successful call.

### DeleteGlobalReplicationGroup

Deleting a Global datastore is a two-step process: First, you must DisassociateGlobalReplicationGroup to remove the secondary clusters in the Global datastore.

```http
POST / HTTP/1.1

Action=DeleteGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=example
&RetainPrimaryReplicationGroup=false
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalReplicationGroupNotFoundFault</Code>
    <Message>Global Replication Group not found</Message>
  </Error>
  <RequestId>475151bd-9ae5-4051-a2f5-b5f4eb4f8bc1</RequestId>
</ErrorResponse>
```

`404` `GlobalReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteReplicationGroup

Deletes an existing replication group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DeleteReplicationGroup
&Version=2015-02-02
&ReplicationGroupId=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ReplicationGroupNotFoundFault</Code>
    <Message>Specified replication group does not exist</Message>
  </Error>
  <RequestId>d825d910-a891-47bf-80e8-996172dc4bae</RequestId>
</ErrorResponse>
```

`404` `ReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteServerlessCache

Deletes a specified existing serverless cache.

```http
POST / HTTP/1.1

Action=DeleteServerlessCache
&Version=2015-02-02
&ServerlessCacheName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheNotFoundFault</Code>
    <Message>Serverless cache with name example not found.</Message>
  </Error>
  <RequestId>873b1841-0cff-495f-9f96-45acc96f0ada</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteServerlessCacheSnapshot

Deletes an existing serverless cache snapshot.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1

Action=DeleteServerlessCacheSnapshot
&Version=2015-02-02
&ServerlessCacheSnapshotName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheSnapshotNotFoundFault</Code>
    <Message>Serverless cache snapshot with specified name does not exists.</Message>
  </Error>
  <RequestId>2e503795-bd5a-4a0f-bbf3-ae75920cf444</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSnapshot

Deletes an existing snapshot.

```http
POST / HTTP/1.1

Action=DeleteSnapshot
&Version=2015-02-02
&SnapshotName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>SnapshotNotFoundFault</Code>
    <Message>Snapshot with name example not found</Message>
  </Error>
  <RequestId>858629fa-570d-4afd-ada1-831633ef9edf</RequestId>
</ErrorResponse>
```

`404` `SnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteUser

For Valkey engine version 7.2 onwards and Redis OSS 6.0 onwards: Deletes a user.

```http
POST / HTTP/1.1

Action=DeleteUser
&Version=2015-02-02
&UserId=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>UserNotFound</Code>
    <Message>User example not found.</Message>
  </Error>
  <RequestId>524bbc43-408c-4e5d-8f65-af06f362a49a</RequestId>
</ErrorResponse>
```

`404` `UserNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteUserGroup

For Valkey engine version 7.2 onwards and Redis OSS 6.0 onwards: Deletes a user group.

```http
POST / HTTP/1.1

Action=DeleteUserGroup
&Version=2015-02-02
&UserGroupId=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>UserGroupNotFound</Code>
    <Message>User group example not found.</Message>
  </Error>
  <RequestId>ed7482a8-8839-4e70-9452-ce3e40edf778</RequestId>
</ErrorResponse>
```

`404` `UserGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeCacheClusters

Returns information about all provisioned clusters if no cluster identifier is specified, or about a specific cache cluster if a cluster identifier is supplied.

```http
POST / HTTP/1.1

Action=DescribeCacheClusters
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeCacheClustersResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeCacheClustersResult>
    <CacheClusters>
      <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <CacheClusterId>example</CacheClusterId>
        <ConfigurationEndpoint>
          <Address>127.0.0.1</Address>
          <Port>45509</Port>
        </ConfigurationEndpoint>
        <Engine>redis</Engine>
        <EngineVersion>7.1</EngineVersion>
        <CacheClusterStatus>available</CacheClusterStatus>
        <NumCacheNodes>1</NumCacheNodes>
        <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
        <CacheClusterCreateTime>2026-11-16T12:11:29.301631Z</CacheClusterCreateTime>
        <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
        <CacheParameterGroup>
          <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
          <ParameterApplyStatus>in-sync</ParameterApplyStatus>
        </CacheParameterGroup>
… (35 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#3-describecacheclusters) for a successful call.

### DescribeCacheEngineVersions

Returns a list of the available cache engines and their versions.

```http
POST / HTTP/1.1

Action=DescribeCacheEngineVersions
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeCacheEngineVersionsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeCacheEngineVersionsResult>
    <CacheEngineVersions>
      <CacheEngineVersion xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <Engine>memcached</Engine>
        <EngineVersion>1.4.5</EngineVersion>
        <CacheParameterGroupFamily>memcached1.4</CacheParameterGroupFamily>
        <CacheEngineDescription>memcached</CacheEngineDescription>
        <CacheEngineVersionDescription>memcached version 1.4.5</CacheEngineVersionDescription>
      </CacheEngineVersion>
      <CacheEngineVersion>
        <Engine>memcached</Engine>
        <EngineVersion>1.4.14</EngineVersion>
        <CacheParameterGroupFamily>memcached1.4</CacheParameterGroupFamily>
        <CacheEngineDescription>memcached</CacheEngineDescription>
        <CacheEngineVersionDescription>memcached version 1.4.14</CacheEngineVersionDescription>
      </CacheEngineVersion>
      <CacheEngineVersion>
        <Engine>memcached</Engine>
        <EngineVersion>1.4.24</EngineVersion>
… (220 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCacheParameterGroups

Returns a list of cache parameter group descriptions.

```http
POST / HTTP/1.1

Action=DescribeCacheParameterGroups
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeCacheParameterGroupsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeCacheParameterGroupsResult>
    <CacheParameterGroups>
      <CacheParameterGroup xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <CacheParameterGroupName>default.memcached1.4</CacheParameterGroupName>
        <CacheParameterGroupFamily>memcached1.4</CacheParameterGroupFamily>
        <Description>Default parameter group for memcached1.4</Description>
        <IsGlobal>false</IsGlobal>
        <ARN>arn:aws:elasticache:us-east-1:000000000000:parametergroup:default.memcached1.4</ARN>
      </CacheParameterGroup>
      <CacheParameterGroup>
        <CacheParameterGroupName>default.memcached1.5</CacheParameterGroupName>
        <CacheParameterGroupFamily>memcached1.5</CacheParameterGroupFamily>
        <Description>Default parameter group for memcached1.5</Description>
        <IsGlobal>false</IsGlobal>
        <ARN>arn:aws:elasticache:us-east-1:000000000000:parametergroup:default.memcached1.5</ARN>
      </CacheParameterGroup>
      <CacheParameterGroup>
        <CacheParameterGroupName>default.memcached1.6</CacheParameterGroupName>
        <CacheParameterGroupFamily>memcached1.6</CacheParameterGroupFamily>
… (136 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCacheParameters

Returns the detailed parameter list for a particular cache parameter group.

```http
POST / HTTP/1.1

Action=DescribeCacheParameters
&Version=2015-02-02
&CacheParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheParameterGroupNotFound</Code>
    <Message>CacheParameterGroupnot found: example</Message>
  </Error>
  <RequestId>30bcad57-fb2a-4dee-84a4-6039d09fd431</RequestId>
</ErrorResponse>
```

`404` `CacheParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeCacheSecurityGroups

Returns a list of cache security group descriptions.

```http
POST / HTTP/1.1

Action=DescribeCacheSecurityGroups
&Version=2015-02-02
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Use of cache security groups is not permitted in this API version for your account.</Message>
  </Error>
  <RequestId>ec1fa29e-239d-4b48-ab21-8cdc4b225d51</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribeCacheSubnetGroups

Returns a list of cache subnet group descriptions.

```http
POST / HTTP/1.1

Action=DescribeCacheSubnetGroups
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeCacheSubnetGroupsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeCacheSubnetGroupsResult>
    <CacheSubnetGroups/>
  </DescribeCacheSubnetGroupsResult>
  <ResponseMetadata>
    <RequestId>d976a51d-ea9d-4487-a3cb-20f307416b47</RequestId>
  </ResponseMetadata>
</DescribeCacheSubnetGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeEngineDefaultParameters

Returns the default engine and system parameter information for the specified cache engine.

```http
POST / HTTP/1.1

Action=DescribeEngineDefaultParameters
&Version=2015-02-02
&CacheParameterGroupFamily=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>CacheParameterGroupFamily example is not a valid parameter group family.</Message>
  </Error>
  <RequestId>90b0d321-d202-4be1-a113-04e45156d836</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribeEvents

Returns events related to clusters, cache security groups, and cache parameter groups.

```http
POST / HTTP/1.1

Action=DescribeEvents
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeEventsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeEventsResult>
    <Events>
      <Event xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <SourceIdentifier>example</SourceIdentifier>
        <SourceType>replication-group</SourceType>
        <Message>Replication group status changed from creating to available</Message>
        <Date>2026-11-16T12:11:29.820329Z</Date>
      </Event>
      <Event>
        <SourceIdentifier>example-001</SourceIdentifier>
        <SourceType>cache-cluster</SourceType>
        <Message>Cache cluster status changed from creating to available</Message>
        <Date>2026-11-16T12:11:29.820329Z</Date>
      </Event>
      <Event>
        <SourceIdentifier>example</SourceIdentifier>
        <SourceType>cache-cluster</SourceType>
        <Message>Cache cluster status changed from creating to available</Message>
        <Date>2026-11-16T12:11:29.801631Z</Date>
… (49 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeGlobalReplicationGroups

Returns information about a particular global replication group.

```http
POST / HTTP/1.1

Action=DescribeGlobalReplicationGroups
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeGlobalReplicationGroupsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeGlobalReplicationGroupsResult>
    <GlobalReplicationGroups/>
  </DescribeGlobalReplicationGroupsResult>
  <ResponseMetadata>
    <RequestId>aa761378-b5d9-466e-9a1f-dee0fc02b23b</RequestId>
  </ResponseMetadata>
</DescribeGlobalReplicationGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReplicationGroups

Returns information about a particular replication group.

```http
POST / HTTP/1.1

Action=DescribeReplicationGroups
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeReplicationGroupsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeReplicationGroupsResult>
    <ReplicationGroups>
      <ReplicationGroup xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <ReplicationGroupId>example</ReplicationGroupId>
        <Description>example</Description>
        <Status>available</Status>
        <MemberClusters>
          <ClusterId>example-001</ClusterId>
        </MemberClusters>
        <NodeGroups>
          <NodeGroup>
            <NodeGroupId>0001</NodeGroupId>
            <Status>available</Status>
            <PrimaryEndpoint>
              <Address>127.0.0.1</Address>
              <Port>42339</Port>
            </PrimaryEndpoint>
            <NodeGroupMembers>
              <NodeGroupMember>
… (34 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReservedCacheNodes

Returns information about reserved cache nodes for this account, or about a specified reserved cache node.

```http
POST / HTTP/1.1

Action=DescribeReservedCacheNodes
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeReservedCacheNodesResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeReservedCacheNodesResult>
    <ReservedCacheNodes/>
  </DescribeReservedCacheNodesResult>
  <ResponseMetadata>
    <RequestId>5def4b40-bd52-422c-b2da-c2a71afd048a</RequestId>
  </ResponseMetadata>
</DescribeReservedCacheNodesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReservedCacheNodesOfferings

Lists available reserved cache node offerings.

```http
POST / HTTP/1.1

Action=DescribeReservedCacheNodesOfferings
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeReservedCacheNodesOfferingsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeReservedCacheNodesOfferingsResult>
    <Marker>6d656d6361636865647c63616368652e74332e6d6963726f7c303039343630383030307c4e6f20557066726f6e74</Marker>
    <ReservedCacheNodesOfferings>
      <ReservedCacheNodesOffering xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <ReservedCacheNodesOfferingId>8728fb22-5596-2ba6-28fa-e97a5e001d35</ReservedCacheNodesOfferingId>
        <CacheNodeType>cache.m3.medium</CacheNodeType>
        <Duration>31536000</Duration>
        <FixedPrice>420.48</FixedPrice>
        <UsagePrice>0</UsagePrice>
        <ProductDescription>memcached</ProductDescription>
        <OfferingType>All Upfront</OfferingType>
        <RecurringCharges>
          <RecurringCharge>
            <RecurringChargeAmount>0</RecurringChargeAmount>
            <RecurringChargeFrequency>Hourly</RecurringChargeFrequency>
          </RecurringCharge>
        </RecurringCharges>
      </ReservedCacheNodesOffering>
      <ReservedCacheNodesOffering>
… (1490 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeServerlessCacheSnapshots

Returns information about serverless cache snapshots.

```http
POST / HTTP/1.1

Action=DescribeServerlessCacheSnapshots
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeServerlessCacheSnapshotsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeServerlessCacheSnapshotsResult>
    <ServerlessCacheSnapshots/>
  </DescribeServerlessCacheSnapshotsResult>
  <ResponseMetadata>
    <RequestId>748cd95c-a690-453d-a805-c263091bd1bb</RequestId>
  </ResponseMetadata>
</DescribeServerlessCacheSnapshotsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeServerlessCaches

Returns information about a specific serverless cache.

```http
POST / HTTP/1.1

Action=DescribeServerlessCaches
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeServerlessCachesResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeServerlessCachesResult>
    <ServerlessCaches/>
  </DescribeServerlessCachesResult>
  <ResponseMetadata>
    <RequestId>2365145b-5e29-4cfd-a7ac-3a8b5d77c4c4</RequestId>
  </ResponseMetadata>
</DescribeServerlessCachesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeServiceUpdates

Returns details of the service updates

```http
POST / HTTP/1.1

Action=DescribeServiceUpdates
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeServiceUpdatesResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeServiceUpdatesResult>
    <ServiceUpdates>
      <ServiceUpdate xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <ServiceUpdateName>elasticache-memcached-security-update-001</ServiceUpdateName>
        <ServiceUpdateReleaseDate>2026-05-20T12:05:33.443297Z</ServiceUpdateReleaseDate>
        <ServiceUpdateEndDate>2026-09-17T12:05:33.443297Z</ServiceUpdateEndDate>
        <ServiceUpdateSeverity>critical</ServiceUpdateSeverity>
        <ServiceUpdateRecommendedApplyByDate>2026-06-19T12:05:33.443297Z</ServiceUpdateRecommendedApplyByDate>
        <ServiceUpdateStatus>expired</ServiceUpdateStatus>
        <ServiceUpdateDescription>A critical security update for memcached 1.6.22.</ServiceUpdateDescription>
        <ServiceUpdateType>security-update</ServiceUpdateType>
        <Engine>memcached</Engine>
        <EngineVersion>1.6.22</EngineVersion>
        <AutoUpdateAfterRecommendedApplyByDate>true</AutoUpdateAfterRecommendedApplyByDate>
        <EstimatedUpdateTime>15 minutes</EstimatedUpdateTime>
      </ServiceUpdate>
      <ServiceUpdate>
        <ServiceUpdateName>elasticache-memcached-security-update-002</ServiceUpdateName>
        <ServiceUpdateReleaseDate>2026-11-09T12:05:33.443297Z</ServiceUpdateReleaseDate>
… (101 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSnapshots

Returns information about cluster or replication group snapshots.

```http
POST / HTTP/1.1

Action=DescribeSnapshots
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeSnapshotsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeSnapshotsResult>
    <Snapshots/>
  </DescribeSnapshotsResult>
  <ResponseMetadata>
    <RequestId>a14eba08-9162-41bf-a99a-017a4da5114c</RequestId>
  </ResponseMetadata>
</DescribeSnapshotsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeUpdateActions

Returns details of the update actions

```http
POST / HTTP/1.1

Action=DescribeUpdateActions
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeUpdateActionsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeUpdateActionsResult>
    <UpdateActions>
      <UpdateAction xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <CacheClusterId>example</CacheClusterId>
        <ServiceUpdateName>elasticache-redis-oss-v6-eol-cache-clusters</ServiceUpdateName>
        <ServiceUpdateReleaseDate>2026-09-17T12:05:33.443297Z</ServiceUpdateReleaseDate>
        <ServiceUpdateSeverity>important</ServiceUpdateSeverity>
        <ServiceUpdateStatus>available</ServiceUpdateStatus>
        <ServiceUpdateRecommendedApplyByDate>2027-03-16T12:05:33.443297Z</ServiceUpdateRecommendedApplyByDate>
        <ServiceUpdateType>engine-major-version-update</ServiceUpdateType>
        <UpdateActionAvailableDate>2026-11-16T12:11:29.301631Z</UpdateActionAvailableDate>
        <UpdateActionStatus>not-applied</UpdateActionStatus>
        <NodesUpdated>0/1</NodesUpdated>
        <UpdateActionStatusModifiedDate>2026-11-16T12:11:29.301631Z</UpdateActionStatusModifiedDate>
        <SlaMet>n/a</SlaMet>
        <EstimatedUpdateTime>15 minutes</EstimatedUpdateTime>
        <Engine>redis</Engine>
      </UpdateAction>
      <UpdateAction>
… (181 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeUserGroups

Returns a list of user groups.

```http
POST / HTTP/1.1

Action=DescribeUserGroups
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeUserGroupsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeUserGroupsResult>
    <UserGroups/>
  </DescribeUserGroupsResult>
  <ResponseMetadata>
    <RequestId>2683d4c9-dc14-4e0d-9cfb-c9fcfe481f69</RequestId>
  </ResponseMetadata>
</DescribeUserGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeUsers

Returns a list of users.

```http
POST / HTTP/1.1

Action=DescribeUsers
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<DescribeUsersResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeUsersResult>
    <Users>
      <member xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <UserId>default</UserId>
        <UserName>default</UserName>
        <Status>active</Status>
        <Engine>redis</Engine>
        <MinimumEngineVersion>6.0</MinimumEngineVersion>
        <AccessString>on ~* &amp;* +@all</AccessString>
        <UserGroupIds/>
        <Authentication>
          <Type>no-password</Type>
        </Authentication>
        <ARN>arn:aws:elasticache:us-east-1:000000000000:user:default</ARN>
      </member>
    </Users>
  </DescribeUsersResult>
  <ResponseMetadata>
    <RequestId>82a59dfe-3147-4590-909a-cd3a4ec77c9e</RequestId>
… (2 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DisassociateGlobalReplicationGroup

Remove a secondary cluster from the Global datastore using the Global datastore name.

```http
POST / HTTP/1.1

Action=DisassociateGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=example
&ReplicationGroupId=example
&ReplicationGroupRegion=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalReplicationGroupNotFoundFault</Code>
    <Message>Global Replication Group not found</Message>
  </Error>
  <RequestId>5749d46f-850e-444f-913d-776a0e32e610</RequestId>
</ErrorResponse>
```

`404` `GlobalReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ExportServerlessCacheSnapshot

Provides the functionality to export the serverless cache snapshot data to Amazon S3.

```http
POST / HTTP/1.1

Action=ExportServerlessCacheSnapshot
&Version=2015-02-02
&ServerlessCacheSnapshotName=example
&S3BucketName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheSnapshotNotFoundFault</Code>
    <Message>Serverless cache snapshot with specified name does not exists.</Message>
  </Error>
  <RequestId>c85c7496-920d-47a3-ab61-fd5a59d388af</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### FailoverGlobalReplicationGroup

Used to failover the primary region to a secondary region.

```http
POST / HTTP/1.1

Action=FailoverGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=example
&PrimaryRegion=example
&PrimaryReplicationGroupId=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalReplicationGroupNotFoundFault</Code>
    <Message>Global Replication Group not found</Message>
  </Error>
  <RequestId>4d6816d9-1754-4123-a49d-21668928d16b</RequestId>
</ErrorResponse>
```

`404` `GlobalReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### IncreaseNodeGroupsInGlobalReplicationGroup

Increase the number of node groups in the Global datastore

```http
POST / HTTP/1.1

Action=IncreaseNodeGroupsInGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=example
&NodeGroupCount=1
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: this emulator applies a Global datastore change at once, and the GlobalReplicationGroup shape has no member to report a pending one.</Message>
  </Error>
  <RequestId>2eb10e11-a12d-4614-a1e8-87c750e1383a</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### IncreaseReplicaCount

Dynamically increases the number of replicas in a Valkey or Redis OSS (cluster mode disabled) replication group or the number of replica nodes in one or more node groups (shards) of a Valkey or Redis OSS (cluster mode enabled) replication group.

```http
POST / HTTP/1.1

Action=IncreaseReplicaCount
&Version=2015-02-02
&ReplicationGroupId=example
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: ApplyImmediately=False is not currently supported.</Message>
  </Error>
  <RequestId>3c8ea87b-a930-4ee5-b1d0-29e01b8b88e5</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ListAllowedNodeTypeModifications

Lists all available node types that you can scale with your cluster's replication group's current node type.

```http
POST / HTTP/1.1

Action=ListAllowedNodeTypeModifications
&Version=2015-02-02
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>Either CacheClusterId or ReplicationGroupId must be specified.</Message>
  </Error>
  <RequestId>85642aa3-1014-4ed3-83ac-a8b94b3a6fb0</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### ListTagsForResource

Lists all tags currently on a named resource.

```http
POST / HTTP/1.1

Action=ListTagsForResource
&Version=2015-02-02
&ResourceName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidARN</Code>
    <Message>Input ARN string does not have 7 components.</Message>
  </Error>
  <RequestId>101f6aee-6fe6-4cad-b88d-07f1aa463855</RequestId>
</ErrorResponse>
```

`400` `InvalidARN`.

### ModifyCacheCluster

Modifies the settings for a cluster.

```http
POST / HTTP/1.1

Action=ModifyCacheCluster
&Version=2015-02-02
&CacheClusterId=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ModifyCacheClusterResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <ModifyCacheClusterResult>
    <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <CacheClusterId>example</CacheClusterId>
      <ConfigurationEndpoint>
        <Address>127.0.0.1</Address>
        <Port>45509</Port>
      </ConfigurationEndpoint>
      <Engine>redis</Engine>
      <EngineVersion>7.1</EngineVersion>
      <CacheClusterStatus>available</CacheClusterStatus>
      <NumCacheNodes>1</NumCacheNodes>
      <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
      <CacheClusterCreateTime>2026-11-16T12:11:29.301631Z</CacheClusterCreateTime>
      <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
      <CacheParameterGroup>
        <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
        <ParameterApplyStatus>in-sync</ParameterApplyStatus>
      </CacheParameterGroup>
      <CacheNodes>
… (21 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ModifyCacheParameterGroup

Modifies the parameters of a cache parameter group.

```http
POST / HTTP/1.1

Action=ModifyCacheParameterGroup
&Version=2015-02-02
&CacheParameterGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter ParameterNameValues must be provided.</Message>
  </Error>
  <RequestId>71d35590-62ef-4f7c-aac0-e671f4295b13</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyCacheSubnetGroup

Modifies an existing cache subnet group.

```http
POST / HTTP/1.1

Action=ModifyCacheSubnetGroup
&Version=2015-02-02
&CacheSubnetGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheSubnetGroupNotFoundFault</Code>
    <Message>Cache Subnet Group example does not exist.</Message>
  </Error>
  <RequestId>c40aed2e-708e-4314-8528-4b124596ee7f</RequestId>
</ErrorResponse>
```

`400` `CacheSubnetGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyGlobalReplicationGroup

Modifies the settings for a Global datastore.

```http
POST / HTTP/1.1

Action=ModifyGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=example
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: this emulator applies a Global datastore change at once, and the GlobalReplicationGroup shape has no member to report a pending one.</Message>
  </Error>
  <RequestId>1005b2b9-d64f-45fe-877f-cead5a1bf557</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyReplicationGroup

Modifies the settings for a replication group.

```http
POST / HTTP/1.1

Action=ModifyReplicationGroup
&Version=2015-02-02
&ReplicationGroupId=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml

<ModifyReplicationGroupResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <ModifyReplicationGroupResult>
    <ReplicationGroup xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <ReplicationGroupId>example</ReplicationGroupId>
      <Description>example</Description>
      <Status>available</Status>
      <MemberClusters>
        <ClusterId>example-001</ClusterId>
      </MemberClusters>
      <NodeGroups>
        <NodeGroup>
          <NodeGroupId>0001</NodeGroupId>
          <Status>available</Status>
          <PrimaryEndpoint>
            <Address>127.0.0.1</Address>
            <Port>42339</Port>
          </PrimaryEndpoint>
          <NodeGroupMembers>
            <NodeGroupMember>
              <CacheClusterId>example-001</CacheClusterId>
… (32 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### ModifyReplicationGroupShardConfiguration

Modifies a replication group's shards (node groups) by allowing you to add shards, remove shards, or rebalance the keyspaces among existing shards.

```http
POST / HTTP/1.1

Action=ModifyReplicationGroupShardConfiguration
&Version=2015-02-02
&ReplicationGroupId=example
&NodeGroupCount=1
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: the shard reconfiguration process begins immediately, so the only permitted value is true.</Message>
  </Error>
  <RequestId>ba945e20-3cfb-4892-ac0b-e1eb1f9f16b1</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyServerlessCache

This API modifies the attributes of a serverless cache.

```http
POST / HTTP/1.1

Action=ModifyServerlessCache
&Version=2015-02-02
&ServerlessCacheName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheNotFoundFault</Code>
    <Message>Serverless cache with name example not found.</Message>
  </Error>
  <RequestId>7a2b6d34-7710-4b38-8974-55f49a104ab7</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyUser

Changes user password(s) and/or access string.

```http
POST / HTTP/1.1

Action=ModifyUser
&Version=2015-02-02
&UserId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>No modifications were requested: name AccessString, AppendAccessString, Engine, Passwords, NoPasswordRequired or an AuthenticationMode.</Message>
  </Error>
  <RequestId>19798239-f490-4994-b5a8-bb949bffdd2c</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### ModifyUserGroup

Changes the list of users that belong to the user group.

```http
POST / HTTP/1.1

Action=ModifyUserGroup
&Version=2015-02-02
&UserGroupId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>No modifications were requested: name Engine, UserIdsToAdd or UserIdsToRemove.</Message>
  </Error>
  <RequestId>a239548b-d71c-42eb-a39d-8dd3ff6e7170</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### PrepareMigration

Prepares the serverless cache for data migration by validating the configuration and setting up the migration resources.

```http
POST / HTTP/1.1

Action=PrepareMigration
&Version=2015-02-02
&ServerlessCacheName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter CustomerNodeEndpointList must be provided.</Message>
  </Error>
  <RequestId>80be22d7-8e60-4a4f-a678-3dbd4ecdafa5</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### PurchaseReservedCacheNodesOffering

Allows you to purchase a reserved cache node offering.

```http
POST / HTTP/1.1

Action=PurchaseReservedCacheNodesOffering
&Version=2015-02-02
&ReservedCacheNodesOfferingId=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ReservedCacheNodesOfferingNotFound</Code>
    <Message>ReservedCacheNodesOfferingId example not found.</Message>
  </Error>
  <RequestId>e80f2311-4998-42d5-baa4-084af6655ffb</RequestId>
</ErrorResponse>
```

`404` `ReservedCacheNodesOfferingNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RebalanceSlotsInGlobalReplicationGroup

Redistribute slots to ensure uniform distribution across existing shards in the cluster.

```http
POST / HTTP/1.1

Action=RebalanceSlotsInGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=example
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: this emulator applies a Global datastore change at once, and the GlobalReplicationGroup shape has no member to report a pending one.</Message>
  </Error>
  <RequestId>be57a6a4-0697-4408-ae14-dbc44a5cf44b</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### RebootCacheCluster

Reboots some, or all, of the cache nodes within a provisioned cluster.

```http
POST / HTTP/1.1

Action=RebootCacheCluster
&Version=2015-02-02
&CacheClusterId=example
&CacheNodeIdsToReboot.CacheNodeId.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ValidationException</Code>
    <Message>Invalid value example for CacheNodeIdsToReboot: cache cluster example has no such cache node.</Message>
  </Error>
  <RequestId>4e7dbbe3-6fb1-43fe-a1fd-05fdd58ad4a3</RequestId>
</ErrorResponse>
```

`400` `ValidationException`: the placeholder values failed the operation's input validation.

### RemoveTagsFromResource

Removes the tags identified by the TagKeys list from the named resource.

```http
POST / HTTP/1.1

Action=RemoveTagsFromResource
&Version=2015-02-02
&ResourceName=example
&TagKeys.member.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidARN</Code>
    <Message>Input ARN string does not have 7 components.</Message>
  </Error>
  <RequestId>63602752-3963-48e8-9a94-27db0532bdf2</RequestId>
</ErrorResponse>
```

`400` `InvalidARN`.

### ResetCacheParameterGroup

Modifies the parameters of a cache parameter group to the engine or system default value.

```http
POST / HTTP/1.1

Action=ResetCacheParameterGroup
&Version=2015-02-02
&CacheParameterGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>ParameterNameValues must name at least one parameter to reset when ResetAllParameters is false.</Message>
  </Error>
  <RequestId>d8594b03-88d4-4b1f-83c6-712129c95b19</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RevokeCacheSecurityGroupIngress

Revokes ingress from a cache security group.

```http
POST / HTTP/1.1

Action=RevokeCacheSecurityGroupIngress
&Version=2015-02-02
&CacheSecurityGroupName=example
&EC2SecurityGroupName=example
&EC2SecurityGroupOwnerId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Use of cache security groups is not permitted in this API version for your account.</Message>
  </Error>
  <RequestId>484845b5-e9ea-4967-a5c5-257550e9f244</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### StartMigration

Starts the migration of data.

```http
POST / HTTP/1.1

Action=StartMigration
&Version=2015-02-02
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Either ReplicationGroupId or ServerlessCacheName must be provided to name the target of the migration.</Message>
  </Error>
  <RequestId>f772cc93-4cef-4aa9-9efa-499ff7878f63</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### StopMigration

Stops an ongoing data sync between the source and the serverless cache.

```http
POST / HTTP/1.1

Action=StopMigration
&Version=2015-02-02
&ServerlessCacheName=example
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation StopMigration is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>79dc2311-ecd1-4ad2-a22f-9025db61d0c6</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### TestFailover

Represents the input of a TestFailover operation which tests automatic failover on a specified node group (called shard in the console) in a replication group (called cluster in the console).

```http
POST / HTTP/1.1

Action=TestFailover
&Version=2015-02-02
&ReplicationGroupId=example
&NodeGroupId=1
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>NodeGroupNotFoundFault</Code>
    <Message>Node group 1 not found in replication group example.</Message>
  </Error>
  <RequestId>1d76b4bd-79a5-4f04-8e3f-b4f75ee4e477</RequestId>
</ErrorResponse>
```

`404` `NodeGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TestMigration

Async API to test connection between source and target replication group.

```http
POST / HTTP/1.1

Action=TestMigration
&Version=2015-02-02
&ReplicationGroupId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml

<?xml version="1.0" ?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter CustomerNodeEndpointList must be provided.</Message>
  </Error>
  <RequestId>b67cd934-b93a-4fad-85dc-2182a353ba18</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

## How it works

### The path of a request

1. **Connection.** The client opens TLS to `127.0.0.1:9297`; the emulator's certificate is signed by the CA in `~/.aws/emulator/9297/ca.pem`, which is why the CLI and SDKs are pointed at that bundle.
2. **Routing.** One endpoint serves every emulated service. The emulator picks this one from the SigV4 credential scope (`…/us-east-1/elasticache/aws4_request` in the `Authorization` header) and the operation from the request itself: `POST /` with a form-encoded body: `Action` names the operation, `Version` the API version, and every input member is a flattened key such as `Tags.member.1.Key`.
3. **Authentication.** The signature is verified against the emulator's credential store. The default credentials belong to IAM user `emulator` in account `000000000000`, the only account the emulator has.
4. **Validation.** The input is decoded and checked against the Smithy model (`@required`, lengths, patterns, ranges, enum values) before any handler runs; most of the errors in [Operations](#operations) are produced here.
5. **Handling.** The operation's handler applies the service's own rules and reads or writes the service's state.
6. **Response.** The result is serialized the way Amazon ElastiCache answers in AWS: XML wrapped in `<OperationResponse><OperationResult>`. Errors captured on this page are an XML document rooted at `<ErrorResponse>`.

### Local endpoints

Where AWS returns a regional hostname, the emulator returns a local address: the cache node endpoint (`DescribeCacheClusters` with `ShowCacheNodeInfo`) is `127.0.0.1:<port>`, a port the emulator opened for that resource. Those addresses are local to the emulator; the [Docker setup](README.md#running-the-emulator-in-docker) publishes only port 9297, so from your machine only the API endpoint is reachable.
