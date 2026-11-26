# Amazon ElastiCache

## Wire protocol

- **Protocol:** `awsQuery`
- **SDK id:** `ElastiCache`
- **sigv4 signing name:** `elasticache`
- **Endpoint prefix:** `elasticache`
- **ARN namespace:** `elasticache`

## Calling the emulator

Every request and response on this page was sent to and returned by a running AWS Emulator at `https://127.0.0.1:9297` on 2026-11-26 (emulator 1.0.629). Requests are signed with SigV4 for Region `us-east-1` and signing name `elasticache`, using the credentials the emulator writes to `~/.aws/emulator/9297/emulator_credentials`; its certificate authority is `~/.aws/emulator/9297/ca.pem`. The access key, signatures and any secrets the emulator returned are replaced with placeholders such as `<redacted>`, and bodies are pretty-printed (form-encoded bodies are shown one parameter per line; on the wire they are a single `&`-joined line).

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
X-Amz-Date: 20261126T182958Z
Content-Length: 227
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
date: Thu, 26 Nov 2026 18:29:58 GMT

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
    <RequestId>dc5f9e47-4aea-4abe-9c12-2ad28b0ffdec</RequestId>
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
X-Amz-Date: 20261126T182958Z
Content-Length: 164
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
content-length: 1799
date: Thu, 26 Nov 2026 18:29:58 GMT

<CreateCacheClusterResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <CreateCacheClusterResult>
    <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <CacheClusterId>docs-demo-redis</CacheClusterId>
      <ClientDownloadLandingPage>https://console.aws.amazon.com/elasticache/home#client-download:</ClientDownloadLandingPage>
      <CacheNodeType>cache.t3.micro</CacheNodeType>
      <Engine>redis</Engine>
      <EngineVersion>7.1.0</EngineVersion>
      <CacheClusterStatus>creating</CacheClusterStatus>
      <NumCacheNodes>1</NumCacheNodes>
      <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
      <CacheClusterCreateTime>2026-11-26T18:29:58.851358Z</CacheClusterCreateTime>
      <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
      <PendingModifiedValues></PendingModifiedValues>
      <CacheSecurityGroups></CacheSecurityGroups>
      <CacheParameterGroup>
        <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
        <ParameterApplyStatus>in-sync</ParameterApplyStatus>
        <CacheNodeIdsToReboot></CacheNodeIdsToReboot>
      </CacheParameterGroup>
      <CacheSubnetGroupName>docs-demo</CacheSubnetGroupName>
      <AutoMinorVersionUpgrade>true</AutoMinorVersionUpgrade>
      <SnapshotRetentionLimit>0</SnapshotRetentionLimit>
      <AuthTokenEnabled>false</AuthTokenEnabled>
      <TransitEncryptionEnabled>false</TransitEncryptionEnabled>
      <AtRestEncryptionEnabled>false</AtRestEncryptionEnabled>
      <ARN>arn:aws:elasticache:us-east-1:000000000000:cluster:docs-demo-redis</ARN>
      <ReplicationGroupLogDeliveryEnabled>false</ReplicationGroupLogDeliveryEnabled>
      <LogDeliveryConfigurations></LogDeliveryConfigurations>
      <NetworkType>ipv4</NetworkType>
      <IpDiscovery>ipv4</IpDiscovery>
    </CacheCluster>
  </CreateCacheClusterResult>
  <ResponseMetadata>
    <RequestId>85bf6fde-2e43-4cf8-84e3-04b7d80db49d</RequestId>
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
X-Amz-Date: 20261126T182958Z
Content-Length: 101
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheClusters
&Version=2015-02-02
&CacheClusterId=docs-demo-redis
&ShowCacheNodeInfo=true
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1842
date: Thu, 26 Nov 2026 18:29:58 GMT

<DescribeCacheClustersResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeCacheClustersResult>
    <CacheClusters>
      <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <CacheClusterId>docs-demo-redis</CacheClusterId>
        <ClientDownloadLandingPage>https://console.aws.amazon.com/elasticache/home#client-download:</ClientDownloadLandingPage>
        <CacheNodeType>cache.t3.micro</CacheNodeType>
        <Engine>redis</Engine>
        <EngineVersion>7.1.0</EngineVersion>
        <CacheClusterStatus>creating</CacheClusterStatus>
        <NumCacheNodes>1</NumCacheNodes>
        <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
        <CacheClusterCreateTime>2026-11-26T18:29:58.851358Z</CacheClusterCreateTime>
        <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
        <PendingModifiedValues></PendingModifiedValues>
        <CacheSecurityGroups></CacheSecurityGroups>
        <CacheParameterGroup>
          <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
          <ParameterApplyStatus>in-sync</ParameterApplyStatus>
          <CacheNodeIdsToReboot></CacheNodeIdsToReboot>
        </CacheParameterGroup>
        <CacheSubnetGroupName>docs-demo</CacheSubnetGroupName>
        <AutoMinorVersionUpgrade>true</AutoMinorVersionUpgrade>
        <SnapshotRetentionLimit>0</SnapshotRetentionLimit>
        <AuthTokenEnabled>false</AuthTokenEnabled>
        <TransitEncryptionEnabled>false</TransitEncryptionEnabled>
        <AtRestEncryptionEnabled>false</AtRestEncryptionEnabled>
        <ARN>arn:aws:elasticache:us-east-1:000000000000:cluster:docs-demo-redis</ARN>
        <ReplicationGroupLogDeliveryEnabled>false</ReplicationGroupLogDeliveryEnabled>
        <LogDeliveryConfigurations></LogDeliveryConfigurations>
        <NetworkType>ipv4</NetworkType>
        <IpDiscovery>ipv4</IpDiscovery>
      </CacheCluster>
    </CacheClusters>
  </DescribeCacheClustersResult>
  <ResponseMetadata>
    <RequestId>88251d24-4391-4f5f-a333-833eb5ba9a9f</RequestId>
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
X-Amz-Date: 20261126T182958Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheClusters
&Version=2015-02-02
&CacheClusterId=nope
```

**Response**

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 982c9d82-0c07-48b9-a18e-d5a0b365d404
content-length: 261
date: Thu, 26 Nov 2026 18:29:58 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheClusterNotFound</Code>
    <Message>CacheCluster not found: nope</Message>
  </Error>
  <RequestId>982c9d82-0c07-48b9-a18e-d5a0b365d404</RequestId>
</ErrorResponse>
```

### 5. DeleteCacheCluster

Deletion moves the cluster to `deleting`.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183001Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteCacheCluster
&Version=2015-02-02
&CacheClusterId=docs-demo-redis
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1896
date: Thu, 26 Nov 2026 18:30:01 GMT

<DeleteCacheClusterResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DeleteCacheClusterResult>
    <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <CacheClusterId>docs-demo-redis</CacheClusterId>
      <ConfigurationEndpoint>
        <Address>127.0.165.221</Address>
        <Port>42461</Port>
      </ConfigurationEndpoint>
      <ClientDownloadLandingPage>https://console.aws.amazon.com/elasticache/home#client-download:</ClientDownloadLandingPage>
      <CacheNodeType>cache.t3.micro</CacheNodeType>
      <Engine>redis</Engine>
      <EngineVersion>7.1.0</EngineVersion>
      <CacheClusterStatus>deleting</CacheClusterStatus>
      <NumCacheNodes>1</NumCacheNodes>
      <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
      <CacheClusterCreateTime>2026-11-26T18:29:58.851358Z</CacheClusterCreateTime>
      <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
      <PendingModifiedValues></PendingModifiedValues>
      <CacheSecurityGroups></CacheSecurityGroups>
      <CacheParameterGroup>
        <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
        <ParameterApplyStatus>in-sync</ParameterApplyStatus>
        <CacheNodeIdsToReboot></CacheNodeIdsToReboot>
      </CacheParameterGroup>
      <CacheSubnetGroupName>docs-demo</CacheSubnetGroupName>
      <AutoMinorVersionUpgrade>true</AutoMinorVersionUpgrade>
      <SnapshotRetentionLimit>0</SnapshotRetentionLimit>
      <AuthTokenEnabled>false</AuthTokenEnabled>
      <TransitEncryptionEnabled>false</TransitEncryptionEnabled>
      <AtRestEncryptionEnabled>false</AtRestEncryptionEnabled>
      <ARN>arn:aws:elasticache:us-east-1:000000000000:cluster:docs-demo-redis</ARN>
      <ReplicationGroupLogDeliveryEnabled>false</ReplicationGroupLogDeliveryEnabled>
      <LogDeliveryConfigurations></LogDeliveryConfigurations>
      <NetworkType>ipv4</NetworkType>
      <IpDiscovery>ipv4</IpDiscovery>
    </CacheCluster>
  </DeleteCacheClusterResult>
  <ResponseMetadata>
    <RequestId>627b672a-0e83-4364-838e-bd4b4c301309</RequestId>
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
X-Amz-Date: 20261126T183004Z
Content-Length: 78
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheClusters
&Version=2015-02-02
&CacheClusterId=docs-demo-redis
```

**Response**

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 26d614de-d5d0-41cb-b6c9-f59a9fa6af5e
content-length: 272
date: Thu, 26 Nov 2026 18:30:04 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheClusterNotFound</Code>
    <Message>CacheCluster not found: docs-demo-redis</Message>
  </Error>
  <RequestId>26d614de-d5d0-41cb-b6c9-f59a9fa6af5e</RequestId>
</ErrorResponse>
```

### 7. DeleteCacheSubnetGroup

Remove the subnet group.

**Request**

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183004Z
Content-Length: 79
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteCacheSubnetGroup
&Version=2015-02-02
&CacheSubnetGroupName=docs-demo
```

**Response**

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 279
date: Thu, 26 Nov 2026 18:30:04 GMT

<DeleteCacheSubnetGroupResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DeleteCacheSubnetGroupResult></DeleteCacheSubnetGroupResult>
  <ResponseMetadata>
    <RequestId>524882fa-e265-4fce-a7ce-3b3d97554687</RequestId>
  </ResponseMetadata>
</DeleteCacheSubnetGroupResponse>
```

## Operations

Every operation of the model, each called once with placeholder values for its required members only (`example`, `1`, the first enum value, and so on), with the emulator's real answer. A placeholder rarely names a real resource, so most answers are the operation's validation or not-found error; that is still the exact response shape a client has to handle. Every request and response shows all of its headers, as in the walkthrough.

Of 77 operations, 18 accepted the placeholder call, 1 answer `501 NotImplemented`, and the rest refused it with a modelled error. Operations marked *Implemented as of emulator 1.0.595* answered `501` in 1.0.385 and were captured again on 2026-11-24.

### AddTagsToResource

A tag is a key-value pair where the key and value are case-sensitive.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183004Z
Content-Length: 64
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AddTagsToResource
&Version=2015-02-02
&ResourceName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 4c4bd8ea-7a8e-4832-88af-b27a9c1d7d1f
content-length: 234
date: Thu, 26 Nov 2026 18:30:04 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter Tags must be provided.</Message>
  </Error>
  <RequestId>4c4bd8ea-7a8e-4832-88af-b27a9c1d7d1f</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### AuthorizeCacheSecurityGroupIngress

Allows network ingress to a cache security group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183004Z
Content-Length: 152
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=AuthorizeCacheSecurityGroupIngress
&Version=2015-02-02
&CacheSecurityGroupName=example
&EC2SecurityGroupName=example
&EC2SecurityGroupOwnerId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 67550e45-8dcc-4535-bd47-0461c3c435e7
content-length: 317
date: Thu, 26 Nov 2026 18:30:04 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Use of cache security groups is not permitted in this API version for your account.</Message>
  </Error>
  <RequestId>67550e45-8dcc-4535-bd47-0461c3c435e7</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### BatchApplyUpdateAction

Apply the service update.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183004Z
Content-Length: 74
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=BatchApplyUpdateAction
&Version=2015-02-02
&ServiceUpdateName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 048b8d1b-9001-456f-a458-0d70b4ee20cd
content-length: 340
date: Thu, 26 Nov 2026 18:30:04 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>CacheClusterIds or ReplicationGroupIds must name at least one resource to apply service update example to.</Message>
  </Error>
  <RequestId>048b8d1b-9001-456f-a458-0d70b4ee20cd</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### BatchStopUpdateAction

Stop the service update.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183004Z
Content-Length: 73
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=BatchStopUpdateAction
&Version=2015-02-02
&ServiceUpdateName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2f58f827-56d3-4ed3-a073-60fa520f98c0
content-length: 339
date: Thu, 26 Nov 2026 18:30:04 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>CacheClusterIds or ReplicationGroupIds must name at least one resource to stop service update example on.</Message>
  </Error>
  <RequestId>2f58f827-56d3-4ed3-a073-60fa520f98c0</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CompleteMigration

Completes the migration of data.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CompleteMigration
&Version=2015-02-02
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 1716d9bd-00f5-46ae-bd6d-f6a5e233bfc2
content-length: 336
date: Thu, 26 Nov 2026 18:30:04 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Either ReplicationGroupId or ServerlessCacheName must be provided to name the target of the migration.</Message>
  </Error>
  <RequestId>1716d9bd-00f5-46ae-bd6d-f6a5e233bfc2</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CopyServerlessCacheSnapshot

Creates a copy of an existing serverless cache’s snapshot.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 137
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CopyServerlessCacheSnapshot
&Version=2015-02-02
&SourceServerlessCacheSnapshotName=example
&TargetServerlessCacheSnapshotName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 63de5f4b-bbb5-4f98-92df-6048ae3dc6be
content-length: 297
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheSnapshotNotFoundFault</Code>
    <Message>Source serverless cache snapshot does not exist.</Message>
  </Error>
  <RequestId>63de5f4b-bbb5-4f98-92df-6048ae3dc6be</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CopySnapshot

Makes a copy of an existing snapshot.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 92
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CopySnapshot
&Version=2015-02-02
&SourceSnapshotName=example
&TargetSnapshotName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 7893cc79-5dd5-499d-b4da-44c3c3f59c8a
content-length: 270
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>SnapshotNotFoundFault</Code>
    <Message>Snapshot with name example not found</Message>
  </Error>
  <RequestId>7893cc79-5dd5-499d-b4da-44c3c3f59c8a</RequestId>
</ErrorResponse>
```

`404` `SnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateCacheCluster

Creates a cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateCacheCluster
&Version=2015-02-02
&CacheClusterId=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1684
date: Thu, 26 Nov 2026 18:30:05 GMT

<CreateCacheClusterResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <CreateCacheClusterResult>
    <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <CacheClusterId>example</CacheClusterId>
      <ClientDownloadLandingPage>https://console.aws.amazon.com/elasticache/home#client-download:</ClientDownloadLandingPage>
      <Engine>redis</Engine>
      <EngineVersion>7.1.0</EngineVersion>
      <CacheClusterStatus>creating</CacheClusterStatus>
      <NumCacheNodes>1</NumCacheNodes>
      <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
      <CacheClusterCreateTime>2026-11-26T18:30:05.006049Z</CacheClusterCreateTime>
      <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
      <PendingModifiedValues></PendingModifiedValues>
      <CacheSecurityGroups></CacheSecurityGroups>
      <CacheParameterGroup>
        <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
        <ParameterApplyStatus>in-sync</ParameterApplyStatus>
        <CacheNodeIdsToReboot></CacheNodeIdsToReboot>
      </CacheParameterGroup>
      <AutoMinorVersionUpgrade>true</AutoMinorVersionUpgrade>
… (15 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#2-createcachecluster) for a successful call.

### CreateCacheParameterGroup

Creates a new Amazon ElastiCache cache parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 137
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateCacheParameterGroup
&Version=2015-02-02
&CacheParameterGroupName=example
&CacheParameterGroupFamily=example
&Description=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b4f70786-d251-40be-b174-4a4209578aa2
content-length: 306
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>CacheParameterGroupFamily example is not a valid parameter group family.</Message>
  </Error>
  <RequestId>b4f70786-d251-40be-b174-4a4209578aa2</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateCacheSecurityGroup

Creates a new cache security group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 101
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateCacheSecurityGroup
&Version=2015-02-02
&CacheSecurityGroupName=example
&Description=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 2ac01e42-1ba7-42ca-95ca-f9ec6e17af7a
content-length: 317
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Use of cache security groups is not permitted in this API version for your account.</Message>
  </Error>
  <RequestId>2ac01e42-1ba7-42ca-95ca-f9ec6e17af7a</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateCacheSubnetGroup

Creates a new cache subnet group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 150
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateCacheSubnetGroup
&Version=2015-02-02
&CacheSubnetGroupName=example
&CacheSubnetGroupDescription=example
&SubnetIds.SubnetIdentifier.1=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: ad4d5306-6525-4b4e-8454-7474f582d89e
content-length: 279
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidSubnet</Code>
    <Message>Invalid value example for SubnetIds: not a subnet id.</Message>
  </Error>
  <RequestId>ad4d5306-6525-4b4e-8454-7474f582d89e</RequestId>
</ErrorResponse>
```

`400` `InvalidSubnet`. See [the walkthrough](#1-createcachesubnetgroup) for a successful call.

### CreateGlobalReplicationGroup

Global Datastore offers fully managed, fast, reliable and secure cross-region replication.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 127
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupIdSuffix=example
&PrimaryReplicationGroupId=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 6bcfda05-d8c0-48d3-ac0a-544887bd6f7c
content-length: 289
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ReplicationGroupNotFoundFault</Code>
    <Message>Replication group &apos;example&apos; not found</Message>
  </Error>
  <RequestId>6bcfda05-d8c0-48d3-ac0a-544887bd6f7c</RequestId>
</ErrorResponse>
```

`404` `ReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateReplicationGroup

Creates a Valkey or Redis OSS (cluster mode disabled) or a Valkey or Redis OSS (cluster mode enabled) replication group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 111
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateReplicationGroup
&Version=2015-02-02
&ReplicationGroupId=example
&ReplicationGroupDescription=example
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 2036
date: Thu, 26 Nov 2026 18:30:05 GMT

<CreateReplicationGroupResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <CreateReplicationGroupResult>
    <ReplicationGroup xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
      <ReplicationGroupId>example</ReplicationGroupId>
      <Description>example</Description>
      <GlobalReplicationGroupInfo></GlobalReplicationGroupInfo>
      <Status>creating</Status>
      <PendingModifiedValues></PendingModifiedValues>
      <MemberClusters>
        <ClusterId>example-001</ClusterId>
      </MemberClusters>
      <NodeGroups>
        <NodeGroup>
          <NodeGroupId>0001</NodeGroupId>
          <Status>creating</Status>
          <PrimaryEndpoint>
            <Address>127.0.0.1</Address>
            <Port>40803</Port>
          </PrimaryEndpoint>
          <ReaderEndpoint>
… (38 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### CreateServerlessCache

Creates a serverless cache.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 90
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateServerlessCache
&Version=2015-02-02
&ServerlessCacheName=example
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d1c53a6b-b432-4cb3-884e-7d7fdcdf766e
content-length: 285
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Specified engine does not support serverless cache.</Message>
  </Error>
  <RequestId>d1c53a6b-b432-4cb3-884e-7d7fdcdf766e</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateServerlessCacheSnapshot

This API creates a copy of an entire ServerlessCache at a specific moment in time.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 119
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateServerlessCacheSnapshot
&Version=2015-02-02
&ServerlessCacheSnapshotName=example
&ServerlessCacheName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 4d6ca86c-96d5-4772-a766-87a0e17467fd
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheNotFoundFault</Code>
    <Message>Serverless cache with name example not found.</Message>
  </Error>
  <RequestId>4d6ca86c-96d5-4772-a766-87a0e17467fd</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### CreateSnapshot

Creates a copy of an entire cluster or replication group at a specific moment in time.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateSnapshot
&Version=2015-02-02
&SnapshotName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 006cf5d8-b8bf-446a-a624-41cc0229628d
content-length: 367
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>Either CacheClusterId or ReplicationGroupId must be specified: a snapshot is taken of an existing cluster or replication group.</Message>
  </Error>
  <RequestId>006cf5d8-b8bf-446a-a624-41cc0229628d</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### CreateUser

For Valkey engine version 7.2 onwards and Redis OSS 6.0 to 7.1: Creates a user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 104
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

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
x-amzn-requestid: 7d07deeb-93ea-444e-a58e-1c1b96942c80
content-length: 301
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Access control options are available for Redis version 6.0 onwards.</Message>
  </Error>
  <RequestId>7d07deeb-93ea-444e-a58e-1c1b96942c80</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### CreateUserGroup

For Valkey engine version 7.2 onwards and Redis OSS 6.0 to 7.1: Creates a user group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 76
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=CreateUserGroup
&Version=2015-02-02
&UserGroupId=example
&Engine=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 65b169ce-ed28-4bd5-b876-9548598865e7
content-length: 301
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Access control options are available for Redis version 6.0 onwards.</Message>
  </Error>
  <RequestId>65b169ce-ed28-4bd5-b876-9548598865e7</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DecreaseNodeGroupsInGlobalReplicationGroup

Decreases the number of node groups in a Global datastore

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 141
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DecreaseNodeGroupsInGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=example
&NodeGroupCount=1
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: bc640717-36e6-4dc9-a176-288875691947
content-length: 408
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: this emulator applies a Global datastore change at once, and the GlobalReplicationGroup shape has no member to report a pending one.</Message>
  </Error>
  <RequestId>bc640717-36e6-4dc9-a176-288875691947</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DecreaseReplicaCount

Dynamically decreases the number of replicas in a Valkey or Redis OSS (cluster mode disabled) replication group or the number of replica nodes in one or more node groups (shards) of a Valkey or Redis OSS (cluster mode enabled) replication group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 96
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DecreaseReplicaCount
&Version=2015-02-02
&ReplicationGroupId=example
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a861a953-708b-4604-a4c3-0faff4d28db9
content-length: 326
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: ApplyImmediately=False is not currently supported.</Message>
  </Error>
  <RequestId>a861a953-708b-4604-a4c3-0faff4d28db9</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteCacheCluster

Deletes a previously provisioned cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 67
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteCacheCluster
&Version=2015-02-02
&CacheClusterId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: aaa07da9-cb76-4812-a298-b38a88422db1
content-length: 413
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidCacheClusterState</Code>
    <Message>Can only delete cache clusters with state in: available, failed, incompatible-parameters, incompatible-network, restore-failed, recovering. Cluster example has state: creating.</Message>
  </Error>
  <RequestId>aaa07da9-cb76-4812-a298-b38a88422db1</RequestId>
</ErrorResponse>
```

`400` `InvalidCacheClusterState`. See [the walkthrough](#5-deletecachecluster) for a successful call.

### DeleteCacheParameterGroup

Deletes the specified cache parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 83
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteCacheParameterGroup
&Version=2015-02-02
&CacheParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: a12b5493-0139-403a-b277-e61608a6af5a
content-length: 277
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheParameterGroupNotFound</Code>
    <Message>CacheParameterGroupnot found: example</Message>
  </Error>
  <RequestId>a12b5493-0139-403a-b277-e61608a6af5a</RequestId>
</ErrorResponse>
```

`404` `CacheParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteCacheSecurityGroup

Deletes a cache security group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteCacheSecurityGroup
&Version=2015-02-02
&CacheSecurityGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 0b9b5c7b-3b7c-425a-80fa-ea52f2665c4b
content-length: 317
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Use of cache security groups is not permitted in this API version for your account.</Message>
  </Error>
  <RequestId>0b9b5c7b-3b7c-425a-80fa-ea52f2665c4b</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DeleteCacheSubnetGroup

Deletes a cache subnet group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteCacheSubnetGroup
&Version=2015-02-02
&CacheSubnetGroupName=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 94b39823-41c0-4ab1-997f-fcf7e14dfebb
content-length: 284
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheSubnetGroupNotFoundFault</Code>
    <Message>Cache Subnet Group example does not exist.</Message>
  </Error>
  <RequestId>94b39823-41c0-4ab1-997f-fcf7e14dfebb</RequestId>
</ErrorResponse>
```

`400` `CacheSubnetGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error. See [the walkthrough](#7-deletecachesubnetgroup) for a successful call.

### DeleteGlobalReplicationGroup

Deleting a Global datastore is a two-step process: First, you must DisassociateGlobalReplicationGroup to remove the secondary clusters in the Global datastore.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 123
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=example
&RetainPrimaryReplicationGroup=false
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 46fea0c2-4b75-469b-b163-bc8eba090f3c
content-length: 282
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalReplicationGroupNotFoundFault</Code>
    <Message>Global Replication Group not found</Message>
  </Error>
  <RequestId>46fea0c2-4b75-469b-b163-bc8eba090f3c</RequestId>
</ErrorResponse>
```

`404` `GlobalReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteReplicationGroup

Deletes an existing replication group.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteReplicationGroup
&Version=2015-02-02
&ReplicationGroupId=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 66e49481-aa6c-4e5d-93d8-50797c17e371
content-length: 305
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidReplicationGroupState</Code>
    <Message>Cache cluster example-001 is not in a valid state to be deleted.</Message>
  </Error>
  <RequestId>66e49481-aa6c-4e5d-93d8-50797c17e371</RequestId>
</ErrorResponse>
```

`400` `InvalidReplicationGroupState`: the placeholder values failed the operation's input validation.

### DeleteServerlessCache

Deletes a specified existing serverless cache.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 75
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteServerlessCache
&Version=2015-02-02
&ServerlessCacheName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 6a7e78ba-054e-4961-bb29-0213fdd7b2ee
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheNotFoundFault</Code>
    <Message>Serverless cache with name example not found.</Message>
  </Error>
  <RequestId>6a7e78ba-054e-4961-bb29-0213fdd7b2ee</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteServerlessCacheSnapshot

Deletes an existing serverless cache snapshot.

*Implemented as of emulator 1.0.595 (captured 2026-11-24).*

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteServerlessCacheSnapshot
&Version=2015-02-02
&ServerlessCacheSnapshotName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 6e1608a4-ff24-44eb-b7c7-6325c007f365
content-length: 311
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheSnapshotNotFoundFault</Code>
    <Message>Serverless cache snapshot with specified name does not exists.</Message>
  </Error>
  <RequestId>6e1608a4-ff24-44eb-b7c7-6325c007f365</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteSnapshot

Deletes an existing snapshot.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteSnapshot
&Version=2015-02-02
&SnapshotName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: f81845d4-92b1-479d-a8ad-1be022ff91ce
content-length: 270
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>SnapshotNotFoundFault</Code>
    <Message>Snapshot with name example not found</Message>
  </Error>
  <RequestId>f81845d4-92b1-479d-a8ad-1be022ff91ce</RequestId>
</ErrorResponse>
```

`404` `SnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteUser

For Valkey engine version 7.2 onwards and Redis OSS 6.0 onwards: Deletes a user.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteUser
&Version=2015-02-02
&UserId=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: e2fea380-eb5e-4904-aef7-7815e04dbbd6
content-length: 248
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>UserNotFound</Code>
    <Message>User example not found.</Message>
  </Error>
  <RequestId>e2fea380-eb5e-4904-aef7-7815e04dbbd6</RequestId>
</ErrorResponse>
```

`404` `UserNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DeleteUserGroup

For Valkey engine version 7.2 onwards and Redis OSS 6.0 onwards: Deletes a user group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DeleteUserGroup
&Version=2015-02-02
&UserGroupId=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: f88cfeb5-4e4c-429e-8d75-e7abee402650
content-length: 259
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>UserGroupNotFound</Code>
    <Message>User group example not found.</Message>
  </Error>
  <RequestId>f88cfeb5-4e4c-429e-8d75-e7abee402650</RequestId>
</ErrorResponse>
```

`404` `UserGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeCacheClusters

Returns information about all provisioned clusters if no cluster identifier is specified, or about a specific cache cluster if a cluster identifier is supplied.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheClusters
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 3147
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeCacheClustersResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeCacheClustersResult>
    <CacheClusters>
      <CacheCluster xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <CacheClusterId>example</CacheClusterId>
        <ClientDownloadLandingPage>https://console.aws.amazon.com/elasticache/home#client-download:</ClientDownloadLandingPage>
        <Engine>redis</Engine>
        <EngineVersion>7.1.0</EngineVersion>
        <CacheClusterStatus>creating</CacheClusterStatus>
        <NumCacheNodes>1</NumCacheNodes>
        <PreferredAvailabilityZone>us-east-1a</PreferredAvailabilityZone>
        <CacheClusterCreateTime>2026-11-26T18:30:05.006049Z</CacheClusterCreateTime>
        <PreferredMaintenanceWindow>sun:23:00-mon:01:30</PreferredMaintenanceWindow>
        <PendingModifiedValues></PendingModifiedValues>
        <CacheSecurityGroups></CacheSecurityGroups>
        <CacheParameterGroup>
          <CacheParameterGroupName>default.redis7</CacheParameterGroupName>
          <ParameterApplyStatus>in-sync</ParameterApplyStatus>
          <CacheNodeIdsToReboot></CacheNodeIdsToReboot>
        </CacheParameterGroup>
… (46 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values. See [the walkthrough](#3-describecacheclusters) for a successful call.

### DescribeCacheEngineVersions

Returns a list of the available cache engines and their versions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheEngineVersions
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 7575
date: Thu, 26 Nov 2026 18:30:05 GMT

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
… (150 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeCacheParameterGroups

Returns a list of cache parameter group descriptions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 54
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheParameterGroups
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 8132
date: Thu, 26 Nov 2026 18:30:05 GMT

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
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheParameters
&Version=2015-02-02
&CacheParameterGroupName=example
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: df8274c1-febb-4cf5-a6a3-298361b77c50
content-length: 277
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheParameterGroupNotFound</Code>
    <Message>CacheParameterGroupnot found: example</Message>
  </Error>
  <RequestId>df8274c1-febb-4cf5-a6a3-298361b77c50</RequestId>
</ErrorResponse>
```

`404` `CacheParameterGroupNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### DescribeCacheSecurityGroups

Returns a list of cache security group descriptions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheSecurityGroups
&Version=2015-02-02
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d201a04c-1635-475f-b27d-049ef265c458
content-length: 317
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Use of cache security groups is not permitted in this API version for your account.</Message>
  </Error>
  <RequestId>d201a04c-1635-475f-b27d-049ef265c458</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribeCacheSubnetGroups

Returns a list of cache subnet group descriptions.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeCacheSubnetGroups
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 330
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeCacheSubnetGroupsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeCacheSubnetGroupsResult>
    <CacheSubnetGroups></CacheSubnetGroups>
  </DescribeCacheSubnetGroupsResult>
  <ResponseMetadata>
    <RequestId>14b2173e-1332-48c1-b9da-c8d8f65980a0</RequestId>
  </ResponseMetadata>
</DescribeCacheSubnetGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeEngineDefaultParameters

Returns the default engine and system parameter information for the specified cache engine.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 91
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeEngineDefaultParameters
&Version=2015-02-02
&CacheParameterGroupFamily=example
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 623869a5-6eb4-4920-8625-f022c36f217e
content-length: 306
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>CacheParameterGroupFamily example is not a valid parameter group family.</Message>
  </Error>
  <RequestId>623869a5-6eb4-4920-8625-f022c36f217e</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### DescribeEvents

Returns events related to clusters, cache security groups, and cache parameter groups.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeEvents
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 1460
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeEventsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeEventsResult>
    <Events>
      <Event xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <SourceIdentifier>example</SourceIdentifier>
        <SourceType>replication-group</SourceType>
        <Message>Replication group created</Message>
        <Date>2026-11-26T18:30:05.016832Z</Date>
      </Event>
      <Event>
        <SourceIdentifier>example</SourceIdentifier>
        <SourceType>cache-cluster</SourceType>
        <Message>Cache cluster created</Message>
        <Date>2026-11-26T18:30:05.006049Z</Date>
      </Event>
      <Event>
        <SourceIdentifier>docs-demo</SourceIdentifier>
        <SourceType>cache-subnet-group</SourceType>
        <Message>Cache subnet group deleted</Message>
        <Date>2026-11-26T18:30:04.985569Z</Date>
… (25 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeGlobalReplicationGroups

Returns information about a particular global replication group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 57
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeGlobalReplicationGroups
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 366
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeGlobalReplicationGroupsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeGlobalReplicationGroupsResult>
    <GlobalReplicationGroups></GlobalReplicationGroups>
  </DescribeGlobalReplicationGroupsResult>
  <ResponseMetadata>
    <RequestId>633f9a70-9a02-4e57-857a-e52a98e9fc5e</RequestId>
  </ResponseMetadata>
</DescribeGlobalReplicationGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReplicationGroups

Returns information about a particular replication group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 51
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeReplicationGroups
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 2087
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeReplicationGroupsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeReplicationGroupsResult>
    <ReplicationGroups>
      <ReplicationGroup xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <ReplicationGroupId>example</ReplicationGroupId>
        <Description>example</Description>
        <GlobalReplicationGroupInfo></GlobalReplicationGroupInfo>
        <Status>creating</Status>
        <PendingModifiedValues></PendingModifiedValues>
        <MemberClusters>
          <ClusterId>example-001</ClusterId>
        </MemberClusters>
        <NodeGroups>
          <NodeGroup>
            <NodeGroupId>0001</NodeGroupId>
            <Status>creating</Status>
            <PrimaryEndpoint>
              <Address>127.0.0.1</Address>
              <Port>40803</Port>
            </PrimaryEndpoint>
… (40 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReservedCacheNodes

Returns information about reserved cache nodes for this account, or about a specified reserved cache node.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 52
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeReservedCacheNodes
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 336
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeReservedCacheNodesResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeReservedCacheNodesResult>
    <ReservedCacheNodes></ReservedCacheNodes>
  </DescribeReservedCacheNodesResult>
  <ResponseMetadata>
    <RequestId>f516e429-2ec3-460b-985c-570cd4a85cf6</RequestId>
  </ResponseMetadata>
</DescribeReservedCacheNodesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeReservedCacheNodesOfferings

Lists available reserved cache node offerings.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 61
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeReservedCacheNodesOfferings
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 56263
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeReservedCacheNodesOfferingsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeReservedCacheNodesOfferingsResult>
    <Marker>6d656d6361636865647c63616368652e72352e6c617267657c303039343630383030307c4e6f20557066726f6e74</Marker>
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
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeServerlessCacheSnapshots
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 372
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeServerlessCacheSnapshotsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeServerlessCacheSnapshotsResult>
    <ServerlessCacheSnapshots></ServerlessCacheSnapshots>
  </DescribeServerlessCacheSnapshotsResult>
  <ResponseMetadata>
    <RequestId>78ba6faa-b730-47fe-83f2-158304ad8218</RequestId>
  </ResponseMetadata>
</DescribeServerlessCacheSnapshotsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeServerlessCaches

Returns information about a specific serverless cache.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 50
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeServerlessCaches
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 324
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeServerlessCachesResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeServerlessCachesResult>
    <ServerlessCaches></ServerlessCaches>
  </DescribeServerlessCachesResult>
  <ResponseMetadata>
    <RequestId>0856d654-2c32-4397-a9c0-f51da8326c52</RequestId>
  </ResponseMetadata>
</DescribeServerlessCachesResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeServiceUpdates

Returns details of the service updates

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 48
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeServiceUpdates
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 7739
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeServiceUpdatesResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeServiceUpdatesResult>
    <ServiceUpdates>
      <ServiceUpdate xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <ServiceUpdateName>elasticache-memcached-security-update-001</ServiceUpdateName>
        <ServiceUpdateReleaseDate>2024-02-13T00:00:00Z</ServiceUpdateReleaseDate>
        <ServiceUpdateEndDate>2024-06-12T00:00:00Z</ServiceUpdateEndDate>
        <ServiceUpdateSeverity>critical</ServiceUpdateSeverity>
        <ServiceUpdateRecommendedApplyByDate>2024-03-14T00:00:00Z</ServiceUpdateRecommendedApplyByDate>
        <ServiceUpdateStatus>expired</ServiceUpdateStatus>
        <ServiceUpdateDescription>Addresses a critical security issue in the cache engine. Applying this update replaces the engine binary the nodes run; it does not change the engine version the cluster is configured with.</ServiceUpdateDescription>
        <ServiceUpdateType>security-update</ServiceUpdateType>
        <Engine>memcached</Engine>
        <EngineVersion>1.6.22</EngineVersion>
        <AutoUpdateAfterRecommendedApplyByDate>true</AutoUpdateAfterRecommendedApplyByDate>
        <EstimatedUpdateTime>15 minutes</EstimatedUpdateTime>
      </ServiceUpdate>
      <ServiceUpdate>
        <ServiceUpdateName>elasticache-memcached-security-update-002</ServiceUpdateName>
        <ServiceUpdateReleaseDate>2025-09-09T00:00:00Z</ServiceUpdateReleaseDate>
… (101 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeSnapshots

Returns information about cluster or replication group snapshots.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 43
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeSnapshots
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 282
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeSnapshotsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeSnapshotsResult>
    <Snapshots></Snapshots>
  </DescribeSnapshotsResult>
  <ResponseMetadata>
    <RequestId>2fae418d-02d4-4cbc-bab0-89c4aa495a4d</RequestId>
  </ResponseMetadata>
</DescribeSnapshotsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeUpdateActions

Returns details of the update actions

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 47
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeUpdateActions
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 10455
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeUpdateActionsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeUpdateActionsResult>
    <UpdateActions>
      <UpdateAction xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
        <CacheClusterId>example</CacheClusterId>
        <ServiceUpdateName>elasticache-redis-oss-v6-eol-cache-clusters</ServiceUpdateName>
        <ServiceUpdateReleaseDate>2025-04-15T00:00:00Z</ServiceUpdateReleaseDate>
        <ServiceUpdateSeverity>important</ServiceUpdateSeverity>
        <ServiceUpdateStatus>available</ServiceUpdateStatus>
        <ServiceUpdateRecommendedApplyByDate>2039-01-01T00:00:00Z</ServiceUpdateRecommendedApplyByDate>
        <ServiceUpdateType>engine-major-version-update</ServiceUpdateType>
        <UpdateActionAvailableDate>2026-11-26T18:30:05.006049Z</UpdateActionAvailableDate>
        <UpdateActionStatus>not-applied</UpdateActionStatus>
        <NodesUpdated>0/1</NodesUpdated>
        <UpdateActionStatusModifiedDate>2026-11-26T18:30:05.006049Z</UpdateActionStatusModifiedDate>
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
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 44
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeUserGroups
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 288
date: Thu, 26 Nov 2026 18:30:05 GMT

<DescribeUserGroupsResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <DescribeUserGroupsResult>
    <UserGroups></UserGroups>
  </DescribeUserGroupsResult>
  <ResponseMetadata>
    <RequestId>a25c2f45-c574-4e6e-8b33-a8ccef4c4ce0</RequestId>
  </ResponseMetadata>
</DescribeUserGroupsResponse>
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DescribeUsers

Returns a list of users.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 39
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DescribeUsers
&Version=2015-02-02
```

```http
HTTP/1.1 200 OK
content-type: text/xml
content-length: 676
date: Thu, 26 Nov 2026 18:30:05 GMT

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
        <UserGroupIds></UserGroupIds>
        <Authentication>
          <Type>no-password</Type>
        </Authentication>
        <ARN>arn:aws:elasticache:us-east-1:000000000000:user:default</ARN>
      </member>
    </Users>
  </DescribeUsersResult>
  <ResponseMetadata>
    <RequestId>2bddf6c8-0e04-4a25-8688-270d657a0daa</RequestId>
… (2 more lines)
```

Accepted: the emulator answered `200 OK` even with placeholder values.

### DisassociateGlobalReplicationGroup

Remove a secondary cluster from the Global datastore using the Global datastore name.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 157
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=DisassociateGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=docs-demo
&ReplicationGroupId=docs-demo
&ReplicationGroupRegion=docs-demo
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 28c62e46-b51f-4b63-a3f2-e2e8cde689ac
content-length: 282
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalReplicationGroupNotFoundFault</Code>
    <Message>Global Replication Group not found</Message>
  </Error>
  <RequestId>28c62e46-b51f-4b63-a3f2-e2e8cde689ac</RequestId>
</ErrorResponse>
```

`404` `GlobalReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ExportServerlessCacheSnapshot

Provides the functionality to export the serverless cache snapshot data to Amazon S3.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 116
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ExportServerlessCacheSnapshot
&Version=2015-02-02
&ServerlessCacheSnapshotName=docs-demo
&S3BucketName=docs-demo
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 26a48d78-da84-43d8-92ff-b6284dd7e57e
content-length: 311
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheSnapshotNotFoundFault</Code>
    <Message>Serverless cache snapshot with specified name does not exists.</Message>
  </Error>
  <RequestId>26a48d78-da84-43d8-92ff-b6284dd7e57e</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheSnapshotNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### FailoverGlobalReplicationGroup

Used to failover the primary region to a secondary region.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 151
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=FailoverGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=docs-demo
&PrimaryRegion=docs-demo
&PrimaryReplicationGroupId=docs-demo
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 82f6933c-a891-4604-985d-ad3d7d61d60d
content-length: 282
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>GlobalReplicationGroupNotFoundFault</Code>
    <Message>Global Replication Group not found</Message>
  </Error>
  <RequestId>82f6933c-a891-4604-985d-ad3d7d61d60d</RequestId>
</ErrorResponse>
```

`404` `GlobalReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### IncreaseNodeGroupsInGlobalReplicationGroup

Increase the number of node groups in the Global datastore

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 143
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=IncreaseNodeGroupsInGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=docs-demo
&NodeGroupCount=1
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 94f1e0b5-5221-4cce-bfba-82bce45b841b
content-length: 408
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: this emulator applies a Global datastore change at once, and the GlobalReplicationGroup shape has no member to report a pending one.</Message>
  </Error>
  <RequestId>94f1e0b5-5221-4cce-bfba-82bce45b841b</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### IncreaseReplicaCount

Dynamically increases the number of replicas in a Valkey or Redis OSS (cluster mode disabled) replication group or the number of replica nodes in one or more node groups (shards) of a Valkey or Redis OSS (cluster mode enabled) replication group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=IncreaseReplicaCount
&Version=2015-02-02
&ReplicationGroupId=docs-demo
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: af31ea04-2376-4977-a5b0-c10fad1992d9
content-length: 326
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: ApplyImmediately=False is not currently supported.</Message>
  </Error>
  <RequestId>af31ea04-2376-4977-a5b0-c10fad1992d9</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ListAllowedNodeTypeModifications

Lists all available node types that you can scale with your cluster's replication group's current node type.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 58
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListAllowedNodeTypeModifications
&Version=2015-02-02
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f9184e39-3d78-4954-9531-ddd724986d26
content-length: 302
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>Either CacheClusterId or ReplicationGroupId must be specified.</Message>
  </Error>
  <RequestId>f9184e39-3d78-4954-9531-ddd724986d26</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### ListTagsForResource

Lists all tags currently on a named resource.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ListTagsForResource
&Version=2015-02-02
&ResourceName=docs-demo
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 622a7499-eb00-4ca5-a6e9-14bbc3cd39a1
content-length: 267
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidARN</Code>
    <Message>Input ARN string does not have 7 components.</Message>
  </Error>
  <RequestId>622a7499-eb00-4ca5-a6e9-14bbc3cd39a1</RequestId>
</ErrorResponse>
```

`400` `InvalidARN`.

### ModifyCacheCluster

Modifies the settings for a cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyCacheCluster
&Version=2015-02-02
&CacheClusterId=docs-demo
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: bfb32993-4281-424c-bc07-49cca08d5d0e
content-length: 266
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheClusterNotFound</Code>
    <Message>CacheCluster not found: docs-demo</Message>
  </Error>
  <RequestId>bfb32993-4281-424c-bc07-49cca08d5d0e</RequestId>
</ErrorResponse>
```

`404` `CacheClusterNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyCacheParameterGroup

Modifies the parameters of a cache parameter group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 85
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyCacheParameterGroup
&Version=2015-02-02
&CacheParameterGroupName=docs-demo
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 107f6fbb-c958-462c-9338-8a83dee65810
content-length: 249
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter ParameterNameValues must be provided.</Message>
  </Error>
  <RequestId>107f6fbb-c958-462c-9338-8a83dee65810</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyCacheSubnetGroup

Modifies an existing cache subnet group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 79
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyCacheSubnetGroup
&Version=2015-02-02
&CacheSubnetGroupName=docs-demo
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d6236944-34da-4dc7-ad18-488d9fe78465
content-length: 286
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheSubnetGroupNotFoundFault</Code>
    <Message>Cache Subnet Group docs-demo does not exist.</Message>
  </Error>
  <RequestId>d6236944-34da-4dc7-ad18-488d9fe78465</RequestId>
</ErrorResponse>
```

`400` `CacheSubnetGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyGlobalReplicationGroup

Modifies the settings for a Global datastore.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 112
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=docs-demo
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: d2a36690-c499-46dd-81b2-1fe2285d6080
content-length: 408
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: this emulator applies a Global datastore change at once, and the GlobalReplicationGroup shape has no member to report a pending one.</Message>
  </Error>
  <RequestId>d2a36690-c499-46dd-81b2-1fe2285d6080</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyReplicationGroup

Modifies the settings for a replication group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyReplicationGroup
&Version=2015-02-02
&ReplicationGroupId=docs-demo
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: d7adc4bb-1539-4d51-aa02-605987966184
content-length: 291
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ReplicationGroupNotFoundFault</Code>
    <Message>Replication group &apos;docs-demo&apos; not found</Message>
  </Error>
  <RequestId>d7adc4bb-1539-4d51-aa02-605987966184</RequestId>
</ErrorResponse>
```

`404` `ReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyReplicationGroupShardConfiguration

Modifies a replication group's shards (node groups) by allowing you to add shards, remove shards, or rebalance the keyspaces among existing shards.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 135
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyReplicationGroupShardConfiguration
&Version=2015-02-02
&ReplicationGroupId=docs-demo
&NodeGroupCount=1
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 0a63f8a5-d8fc-4b13-9dc3-7d15228a1f4f
content-length: 366
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: the shard reconfiguration process begins immediately, so the only permitted value is true.</Message>
  </Error>
  <RequestId>0a63f8a5-d8fc-4b13-9dc3-7d15228a1f4f</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### ModifyServerlessCache

This API modifies the attributes of a serverless cache.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 77
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyServerlessCache
&Version=2015-02-02
&ServerlessCacheName=docs-demo
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 2099d539-77a0-4003-a8fd-3feccdb612bf
content-length: 288
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ServerlessCacheNotFoundFault</Code>
    <Message>Serverless cache with name docs-demo not found.</Message>
  </Error>
  <RequestId>2099d539-77a0-4003-a8fd-3feccdb612bf</RequestId>
</ErrorResponse>
```

`404` `ServerlessCacheNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### ModifyUser

Changes user password(s) and/or access string.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 53
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyUser
&Version=2015-02-02
&UserId=docs-demo
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: a6d0fb6f-506a-465b-a055-2150a7ba584b
content-length: 375
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>No modifications were requested: name AccessString, AppendAccessString, Engine, Passwords, NoPasswordRequired or an AuthenticationMode.</Message>
  </Error>
  <RequestId>a6d0fb6f-506a-465b-a055-2150a7ba584b</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### ModifyUserGroup

Changes the list of users that belong to the user group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 63
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ModifyUserGroup
&Version=2015-02-02
&UserGroupId=docs-demo
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: f01389ab-1b6d-4442-b2ea-f6e8db25802f
content-length: 318
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>No modifications were requested: name Engine, UserIdsToAdd or UserIdsToRemove.</Message>
  </Error>
  <RequestId>f01389ab-1b6d-4442-b2ea-f6e8db25802f</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### PrepareMigration

Prepares the serverless cache for data migration by validating the configuration and setting up the migration resources.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 72
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PrepareMigration
&Version=2015-02-02
&ServerlessCacheName=docs-demo
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 79846378-b84b-4a03-916a-c1ebbbb0743b
content-length: 254
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter CustomerNodeEndpointList must be provided.</Message>
  </Error>
  <RequestId>79846378-b84b-4a03-916a-c1ebbbb0743b</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### PurchaseReservedCacheNodesOffering

Allows you to purchase a reserved cache node offering.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 99
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=PurchaseReservedCacheNodesOffering
&Version=2015-02-02
&ReservedCacheNodesOfferingId=docs-demo
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 41313bb3-ca97-4e94-804d-9f1b56688ce5
content-length: 296
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ReservedCacheNodesOfferingNotFound</Code>
    <Message>ReservedCacheNodesOfferingId docs-demo not found.</Message>
  </Error>
  <RequestId>41313bb3-ca97-4e94-804d-9f1b56688ce5</RequestId>
</ErrorResponse>
```

`404` `ReservedCacheNodesOfferingNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RebalanceSlotsInGlobalReplicationGroup

Redistribute slots to ensure uniform distribution across existing shards in the cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 122
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RebalanceSlotsInGlobalReplicationGroup
&Version=2015-02-02
&GlobalReplicationGroupId=docs-demo
&ApplyImmediately=false
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: be9e8fab-2155-427c-936b-1d01471ff87f
content-length: 408
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Invalid value false for ApplyImmediately: this emulator applies a Global datastore change at once, and the GlobalReplicationGroup shape has no member to report a pending one.</Message>
  </Error>
  <RequestId>be9e8fab-2155-427c-936b-1d01471ff87f</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### RebootCacheCluster

Reboots some, or all, of the cache nodes within a provisioned cluster.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 114
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RebootCacheCluster
&Version=2015-02-02
&CacheClusterId=docs-demo
&CacheNodeIdsToReboot.CacheNodeId.1=docs-demo
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: ee854da2-114c-4ab2-88e2-a9018491f136
content-length: 266
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>CacheClusterNotFound</Code>
    <Message>CacheCluster not found: docs-demo</Message>
  </Error>
  <RequestId>ee854da2-114c-4ab2-88e2-a9018491f136</RequestId>
</ErrorResponse>
```

`404` `CacheClusterNotFound`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### RemoveTagsFromResource

Removes the tags identified by the TagKeys list from the named resource.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 98
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RemoveTagsFromResource
&Version=2015-02-02
&ResourceName=docs-demo
&TagKeys.member.1=docs-demo
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: e0b6dfa0-531c-4927-9f2a-536b37dcbe55
content-length: 267
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidARN</Code>
    <Message>Input ARN string does not have 7 components.</Message>
  </Error>
  <RequestId>e0b6dfa0-531c-4927-9f2a-536b37dcbe55</RequestId>
</ErrorResponse>
```

`400` `InvalidARN`.

### ResetCacheParameterGroup

Modifies the parameters of a cache parameter group to the engine or system default value.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 84
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=ResetCacheParameterGroup
&Version=2015-02-02
&CacheParameterGroupName=docs-demo
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 8a48bdf9-b71d-49dd-b017-cc27ad66c5f6
content-length: 335
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterCombination</Code>
    <Message>ParameterNameValues must name at least one parameter to reset when ResetAllParameters is false.</Message>
  </Error>
  <RequestId>8a48bdf9-b71d-49dd-b017-cc27ad66c5f6</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterCombination`: the placeholder values failed the operation's input validation.

### RevokeCacheSecurityGroupIngress

Revokes ingress from a cache security group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 155
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=RevokeCacheSecurityGroupIngress
&Version=2015-02-02
&CacheSecurityGroupName=docs-demo
&EC2SecurityGroupName=docs-demo
&EC2SecurityGroupOwnerId=docs-demo
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: b3bd6d6e-743c-4c86-9e34-70d71a97c148
content-length: 317
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Use of cache security groups is not permitted in this API version for your account.</Message>
  </Error>
  <RequestId>b3bd6d6e-743c-4c86-9e34-70d71a97c148</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### StartMigration

Starts the migration of data.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 40
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StartMigration
&Version=2015-02-02
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: cd144b6b-4af7-4fc7-bab9-98159cf38ff0
content-length: 336
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>Either ReplicationGroupId or ServerlessCacheName must be provided to name the target of the migration.</Message>
  </Error>
  <RequestId>cd144b6b-4af7-4fc7-bab9-98159cf38ff0</RequestId>
</ErrorResponse>
```

`400` `InvalidParameterValue`: the placeholder values failed the operation's input validation.

### StopMigration

Stops an ongoing data sync between the source and the serverless cache.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 69
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=StopMigration
&Version=2015-02-02
&ServerlessCacheName=docs-demo
```

```http
HTTP/1.1 501 Not Implemented
content-type: text/xml
x-amzn-requestid: c92d6424-98a6-4e49-96d5-5e1d084d83b1
content-length: 303
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Receiver</Type>
    <Code>NotImplemented</Code>
    <Message>operation StopMigration is scaffolded but not implemented in this emulator</Message>
  </Error>
  <RequestId>c92d6424-98a6-4e49-96d5-5e1d084d83b1</RequestId>
</ErrorResponse>
```

Not implemented in the emulator yet: it answers `501 NotImplemented` for every request to this operation.

### TestFailover

Represents the input of a TestFailover operation which tests automatic failover on a specified node group (called shard in the console) in a replication group (called cluster in the console).

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 81
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TestFailover
&Version=2015-02-02
&ReplicationGroupId=docs-demo
&NodeGroupId=1
```

```http
HTTP/1.1 404 Not Found
content-type: text/xml
x-amzn-requestid: 216ab5aa-02e9-48ec-993d-3f4309ecc486
content-length: 285
date: Thu, 26 Nov 2026 18:30:05 GMT

<ErrorResponse xmlns="http://elasticache.amazonaws.com/doc/2015-02-02/">
  <Error>
    <Type>Sender</Type>
    <Code>ReplicationGroupNotFoundFault</Code>
    <Message>Replication group docs-demo does not exist.</Message>
  </Error>
  <RequestId>216ab5aa-02e9-48ec-993d-3f4309ecc486</RequestId>
</ErrorResponse>
```

`404` `ReplicationGroupNotFoundFault`: the placeholder identifiers name nothing that exists, so this is the operation's not-found error.

### TestMigration

Async API to test connection between source and target replication group.

```http
POST / HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: 127.0.0.1:9297
X-Amz-Date: 20261126T183005Z
Content-Length: 68
Authorization: AWS4-HMAC-SHA256 Credential=<access-key-id>/20261126/us-east-1/elasticache/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date, Signature=<signature>

Action=TestMigration
&Version=2015-02-02
&ReplicationGroupId=docs-demo
```

```http
HTTP/1.1 400 Bad Request
content-type: text/xml
x-amzn-requestid: 9866f408-76fe-4f51-8639-8956fa10ab65
content-length: 254
date: Thu, 26 Nov 2026 18:30:05 GMT

<?xml version="1.0"?>
<ErrorResponse>
  <Error>
    <Type>Sender</Type>
    <Code>InvalidParameterValue</Code>
    <Message>The parameter CustomerNodeEndpointList must be provided.</Message>
  </Error>
  <RequestId>9866f408-76fe-4f51-8639-8956fa10ab65</RequestId>
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
