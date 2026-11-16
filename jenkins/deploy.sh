#!/bin/sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
cd "$repo_root"

namespace=mr-jenk
release="build-${BUILD_NUMBER:-$(date +%Y%m%d%H%M%S)}"
services='server user order product media gateway frontend'

rollback() {
    echo 'Deployment failed. Restoring previous images.'
    export IMAGE_TAG=previous
    docker compose -p backend -f backend/docker-compose.yml up -d --no-build || true
    docker compose -p frontend -f frontend/docker-compose.yaml up -d --no-build || true
}

docker compose -p backend -f backend/docker-compose.yml down --remove-orphans
docker compose -p frontend -f frontend/docker-compose.yaml down --remove-orphans

for service in $services; do
    if docker image inspect "$namespace/$service:latest" >/dev/null 2>&1; then
        docker tag "$namespace/$service:latest" "$namespace/$service:previous"
    fi
done

set +e
(
    set -eu
    export IMAGE_NAMESPACE="$namespace"
    export IMAGE_TAG="$release"
    export COMPOSE_PARALLEL_LIMIT=1

    echo 'Building backend images...'
    docker compose -p backend -f backend/docker-compose.yml build --progress=plain
    echo 'Building frontend image...'
    docker compose -p frontend -f frontend/docker-compose.yaml build --progress=plain

    for service in $services; do
        docker tag "$namespace/$service:$release" "$namespace/$service:latest"
    done

    export IMAGE_TAG=latest
    echo 'Starting backend containers...'
    docker compose -p backend -f backend/docker-compose.yml up -d --no-build
    echo 'Starting frontend container...'
    docker compose -p frontend -f frontend/docker-compose.yaml up -d --no-build
)
deployment_status=$?
set -e

if [ "$deployment_status" -ne 0 ]; then
    rollback
    exit "$deployment_status"
fi

echo "Deployment completed: $release"