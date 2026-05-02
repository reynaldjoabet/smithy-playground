#!/usr/bin/env bash
set -euo pipefail

# Update Smithy models from aws/api-models-aws GitHub repo.
# Models are copied into src/main/resources/models.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEST="$REPO_ROOT/src/main/resources/models"
TMPDIR=$(mktemp -d)

trap 'rm -rf "$TMPDIR"' EXIT

echo "Cloning aws/api-models-aws (sparse)..."
cd "$TMPDIR"
git clone --depth 1 --filter=blob:none --sparse \
    https://github.com/aws/api-models-aws.git repo 2>&1 | tail -1

cd repo

# Service mapping: our_name:repo_dir
SERVICES=(
    # API Gateway
    "api-gateway:api-gateway"
    "apigatewaymanagementapi:apigatewaymanagementapi"
    "apigatewayv2:apigatewayv2"
    # Identity
    "cognito-identity:cognito-identity"
    "cognito-identity-provider:cognito-identity-provider"
    "identitystore:identitystore"
    # Databases
    "dynamodb:dynamodb"
    "dynamodb-streams:dynamodb-streams"
    "docdb:docdb"
    "docdb-elastic:docdb-elastic"
    "neptune:neptune"
    "neptune-graph:neptune-graph"
    "elasticache:elasticache"
    "memorydb:memorydb"
    "keyspaces:keyspaces"
    "dax:dax"
    "dsql:dsql"
    # IAM
    "iam:iam"
    # CloudWatch
    "cloudwatch:cloudwatch"
    "cloudwatch-events:cloudwatch-events"
    "cloudwatch-logs:cloudwatch-logs"
    # EC2
    "ec2:ec2"
    "ec2-instance-connect:ec2-instance-connect"
    # EKS
    "eks:eks"
    "eks-auth:eks-auth"
    # Elastic Beanstalk
    "elastic-beanstalk:elastic-beanstalk"
    # Load Balancing
    "elastic-load-balancing:elastic-load-balancing"
    "elastic-load-balancing-v2:elastic-load-balancing-v2"
    # Kafka / Kafka Connect
    "kafka:kafka"
    "kafkaconnect:kafkaconnect"
    # Lambda
    "lambda:lambda"
    # Network Firewall
    "network-firewall:network-firewall"
    # RDS
    "rds:rds"
    "rds-data:rds-data"
    # Resource Groups
    "resource-groups:resource-groups"
    "resource-groups-tagging-api:resource-groups-tagging-api"
    # Route 53
    "route-53:route-53"
    "route-53-domains:route-53-domains"
    "route53-recovery-cluster:route53-recovery-cluster"
    "route53-recovery-control-config:route53-recovery-control-config"
    "route53-recovery-readiness:route53-recovery-readiness"
    "route53globalresolver:route53globalresolver"
    "route53profiles:route53profiles"
    "route53resolver:route53resolver"
    # S3
    "s3:s3"
    "s3-control:s3-control"
    "s3outposts:s3outposts"
    "s3tables:s3tables"
    "s3vectors:s3vectors"
    # Secrets Manager
    "secrets-manager:secrets-manager"
    # SNS / SQS
    "sns:sns"
    "sqs:sqs"
    # SSO
    "sso:sso"
    "sso-admin:sso-admin"
    # STS
    "sts:sts"
    # WAF
    "waf:waf"
    "waf-regional:waf-regional"
    "wafv2:wafv2"
    # VPC
    "vpc-lattice:vpc-lattice"
)

# Sparse checkout only the models we need
SPARSE_DIRS=()
for mapping in "${SERVICES[@]}"; do
    repo_dir="${mapping#*:}"
    SPARSE_DIRS+=("models/$repo_dir")
done
git sparse-checkout set "${SPARSE_DIRS[@]}"

# Copy each model
for mapping in "${SERVICES[@]}"; do
    our_name="${mapping%%:*}"
    repo_dir="${mapping#*:}"
    json_file=$(find "models/$repo_dir" -name "*.json" -type f | head -1)
    if [ -z "$json_file" ]; then
        echo "WARNING: No model found for $our_name (repo dir: $repo_dir)"
        continue
    fi
    mkdir -p "$DEST/$our_name"
    cp "$json_file" "$DEST/$our_name/$our_name.json"
    echo "Updated $our_name/$our_name.json from $json_file"
done

echo ""
echo "Done. Review changes with: git diff src/main/resources/models/"