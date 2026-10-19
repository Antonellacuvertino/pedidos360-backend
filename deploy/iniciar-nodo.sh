#!/usr/bin/env bash
set -euo pipefail

cd /opt/pedidos360/backend
env_file=deploy/.env.node
test -s "$env_file"
chmod 600 "$env_file"

if grep -q '^SERVICE_NAME=' "$env_file"; then
  compose_file=deploy/compose.microservice.yml
else
  compose_file=deploy/compose.bff.yml
fi

docker compose --env-file "$env_file" -f "$compose_file" config --quiet
docker compose --env-file "$env_file" -f "$compose_file" up --build -d
docker compose --env-file "$env_file" -f "$compose_file" ps
