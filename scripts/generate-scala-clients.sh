#!/usr/bin/env bash
# Regenerates the Scala clients in modules/client-example (codecs derived by Hearth, the default) and
# Mirror-derived copies in its tests, with the generator in modules/client-codegen.
# Each run replaces the package directory it writes.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="$root/modules/client-example/src/main/scala"
models="$root/src/main/resources/models"

generate() { # <model file> <service shape id> <scala package> [output dir] [--derivation=...]
  sbt --client "clientCodegen/runMain smithyscala.codegen.Main ${5:-} $1 $2 $3 ${4:-$out}"
}

generate "$models/sqs/sqs.json"                          'com.amazonaws.sqs#AmazonSQS'              example.sqs
generate "$models/dynamodb/dynamodb.json"                'com.amazonaws.dynamodb#DynamoDB_20120810' example.dynamodb
generate "$root/modules/client-example/model/weather.smithy" 'example.weather#Weather'              example.weather

# Copies with the runtime's Mirror derivation, for client-example's ParitySpec.
mirror="$root/modules/client-example/src/test/scala"
generate "$models/dynamodb/dynamodb.json"                'com.amazonaws.dynamodb#DynamoDB_20120810' example.mirror.dynamodb "$mirror" --derivation=mirror
generate "$root/modules/client-example/model/weather.smithy" 'example.weather#Weather'              example.mirror.weather  "$mirror" --derivation=mirror
