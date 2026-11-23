#!/usr/bin/env bash

set -euo pipefail

backend_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
env_file="$backend_dir/.env"

if [[ -z "${SONAR_TOKEN:-}" && -f "$env_file" ]]; then
  SONAR_TOKEN="$(sed -n 's/^SONAR_TOKEN=//p' "$env_file" | head -n 1)"
  export SONAR_TOKEN
fi

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo "Set SONAR_TOKEN in the environment or in $env_file." >&2
  exit 1
fi

sonar_host_url="${SONAR_HOST_URL:-http://localhost:9090}"

for service in gateway userService productService mediaService server orderService; do
  echo "========================================"
  echo "Building and scanning $service..."
  echo "========================================"

  (
    cd "$backend_dir/$service"

    # Run tests and generate JaCoCo coverage
    ./mvnw -B -ntp clean verify

    # Verify JaCoCo report exists
    jacoco_report="target/site/jacoco/jacoco.xml"

    if [[ ! -f "$jacoco_report" ]]; then
      echo "ERROR: JaCoCo report not found for $service" >&2
      exit 1
    fi

    echo "JaCoCo report found: $jacoco_report"

    # Run SonarQube analysis
    ./mvnw -B -ntp \
      org.sonarsource.scanner.maven:sonar-maven-plugin:5.9.0.7382:sonar \
      -Dsonar.projectKey="buy-02-$service" \
      -Dsonar.projectName="buy-02-$service" \
      -Dsonar.host.url="$sonar_host_url" \
      -Dsonar.token="$SONAR_TOKEN" \
      -Dsonar.coverage.jacoco.xmlReportPaths="$jacoco_report"
  )

done

echo "All services successfully scanned!"
