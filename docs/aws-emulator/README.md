# AWS services, as implemented by AWS Emulator

[AWS Emulator](https://github.com/aws/aws-emulator) is a local emulator of the AWS APIs, built one Rust crate per service from the same Smithy models vendored under `src/main/resources/models`. Each page shows how the service is reached on the wire, walks through it end to end with real requests and responses captured from the emulator at `https://127.0.0.1:9297`, shows what every operation answers, and explains how a request is handled.

| Service | Protocol |
|---|---|
| [Amazon API Gateway V2 (HTTP and WebSocket APIs)](apigatewayv2.md) | `restJson1` |
| [AWS Secrets Manager](secrets-manager.md) | `awsJson1_1` |
| [Application Auto Scaling](application-auto-scaling.md) | `awsJson1_1` |
| [AWS Auto Scaling Plans](auto-scaling-plans.md) | `awsJson1_1` |
| [Auto Scaling](auto-scaling.md) | `awsQuery` |
| [Amazon DynamoDB](dynamodb.md) | `awsJson1_0` |
| [Amazon Elastic Compute Cloud](ec2.md) | `ec2Query` |
| [AWS WAFV2](wafv2.md) | `awsJson1_1` |
| [Amazon VPC Lattice](vpc-lattice.md) | `restJson1` |
| [AWS Security Token Service](sts.md) | `awsQuery` |
| [AWS SSO OIDC](sso-oidc.md) | `restJson1` |
| [AWS Single Sign-On Admin](sso-admin.md) | `awsJson1_1` |
| [AWS Single Sign-On](sso.md) | `restJson1` |
| [Amazon Simple Queue Service](sqs.md) | `awsJson1_0` |
| [Amazon Simple Notification Service](sns.md) | `awsQuery` |
| [Amazon Simple Storage Service](s3.md) | `restXml` |
| [Amazon Route 53](route-53.md) | `restXml` |
| [Amazon Route 53 Domains](route-53-domains.md) | `awsJson1_1` |
| [Amazon Relational Database Service](rds.md) | `awsQuery` |
| [AWS Network Security Manager Customer API](network-security-manager.md) | `restJson1` |
| [AWS Network Manager](network-manager.md) | `restJson1` |
| [Amazon Managed Streaming for Apache Kafka](kafka.md) | `restJson1` |
| [AWS SSO Identity Store](identitystore.md) | `awsJson1_1` |
| [AWS Identity and Access Management](iam.md) | `awsQuery` |
| [Elastic Load Balancing v2 (Application, Network and Gateway)](elastic-load-balancing-v2.md) | `awsQuery` |
| [Elastic Load Balancing (Classic)](elastic-load-balancing.md) | `awsQuery` |
| [Amazon ElastiCache](elasticache.md) | `awsQuery` |
| [Amazon EKS Auth](eks-auth.md) | `restJson1` |
| [Amazon Elastic Kubernetes Service](eks.md) | `restJson1` |
| [Amazon Elastic Container Service](ecs.md) | `awsJson1_1` |
| [Amazon Elastic Container Registry](ecr.md) | `awsJson1_1` |
| [AWS CloudHSM](cloudhsm-v2.md) | `awsJson1_1` |
| [AWS CloudHSM Classic](cloudhsm.md) | `awsJson1_1` |
| [Amazon Cognito User Pools](cognito-identity-provider.md) | `awsJson1_1` |
| [Amazon Cognito Identity Pools](cognito-identity.md) | `awsJson1_1` |
| [AWS Network Firewall](network-firewall.md) | `awsJson1_0` |


## How the emulator is built

Researchers at Anthropic demonstrated that AI agents could autonomously build a C compiler. This was a lightbulb moment: If given a well-defined spec and a reference implementation, agents can autonomously build a solution that faithfully implements the specification. AWS has both of those; the AWS API Models are our spec and the actual AWS cloud is our reference implementation.

One group of agents looks for gaps, both by comparing emulator responses against real AWS and by statically comparing the emulator’s code against the published Smithy models, and files an issue for each gap it finds. Another group picks up an issue, writes tests, implements the behavior, and reviews the change for correctness, performance, and security. When the change is ready, it is pushed to a source repository, and that push triggers an AWS CodePipeline run that runs the fidelity tests before the change is published to the public GitHub repository.

## Running the emulator in Docker

The repo's [Dockerfile](../../Dockerfile) installs the emulator's RPM on Amazon Linux 2023 and runs it as an unprivileged user on port 9297.

To run AWS Emulator in a container:

1. Create the directory that the container shares with your machine. On Linux, if you skip this step, Docker makes the directory owned by root, and your SDK can't read it.

   macOS and Linux:

   ```sh
   mkdir -p ~/.aws/emulator
   ```

2. Build the image from the repository root:

   ```sh
   docker build -t aws-emulator .
   ```

3. Start the container.

   macOS and Linux:

   ```sh
   PORT=9297
   docker run --rm --memory=2g --memory-swap=2g -p 127.0.0.1:$PORT:$PORT \
     -e HOME="$HOME" -v "$HOME/.aws/emulator:$HOME/.aws/emulator" \
     aws-emulator -p $PORT
   ```

   The `-b 0.0.0.0` option in the Dockerfile's `ENTRYPOINT` makes the emulator listen on all addresses inside the container, so Docker can forward its port; `-p 127.0.0.1:$PORT:$PORT` opens that port only to your own machine. The `-v` option lets your SDK read the credentials and certificate that the emulator creates. It also keeps the telemetry ID between runs, so each run isn't counted as a new install.

4. In the shell you'll use, set the environment variables that point the AWS CLI at the emulator, and unset any variables that could supply real AWS credentials.

   ```sh
   export AWS_ENDPOINT_URL=https://127.0.0.1:9297
   export AWS_SHARED_CREDENTIALS_FILE=~/.aws/emulator/9297/emulator_credentials
   export AWS_CONFIG_FILE=~/.aws/emulator/9297/config
   export AWS_EC2_METADATA_DISABLED=true
   export AWS_DISABLE_HOST_PREFIX_INJECTION=true
   export AWS_REGION=us-east-1
   export AWS_DEFAULT_REGION=us-east-1
   unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN AWS_PROFILE
   ```

The emulator then serves HTTPS on `https://127.0.0.1:9297` with a self-signed certificate. The emulator uses a single account, `000000000000`. The Dockerfile downloads the `aarch64` package; on an x86_64 host, change `aarch64` to `x86_64` in its download URL.
