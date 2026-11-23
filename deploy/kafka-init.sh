#!/usr/bin/env bash
set -euo pipefail

bootstrap=kafka-1:29092,kafka-2:29092,kafka-3:29092

for attempt in $(seq 1 60); do
  if kafka-broker-api-versions --bootstrap-server "$bootstrap" >/dev/null 2>&1; then
    brokers=$(kafka-broker-api-versions --bootstrap-server "$bootstrap" 2>/dev/null | grep -c 'id: ' || true)
    if [ "$brokers" -ge 3 ]; then
      break
    fi
  fi
  if [ "$attempt" -eq 60 ]; then
    echo 'No estan disponibles los tres brokers Kafka.' >&2
    exit 1
  fi
  sleep 5
done

create_topic() {
  local name=$1 retention=$2
  kafka-topics --bootstrap-server "$bootstrap" --create --if-not-exists \
    --topic "$name" --partitions 3 --replication-factor 3 \
    --config "retention.ms=$retention" \
    --config cleanup.policy=delete \
    --config min.insync.replicas=2
  kafka-topics --bootstrap-server "$bootstrap" --describe --topic "$name"
}

create_topic payment-attempts 604800000
create_topic product-views 259200000
create_topic payment-attempts-dlt 1209600000
create_topic product-views-dlt 1209600000
