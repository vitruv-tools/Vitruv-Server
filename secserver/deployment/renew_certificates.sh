#!/bin/bash

echo "Start Script: $(date)"

# TODO: adjust paths (and domain)
CERT_PATH="/etc/letsencrypt/live/vitruv-server.org/fullchain.pem"
DOCKER_COMPOSE_PATH="$HOME/deploy/docker-compose.yml"
CONTAINER_NAME=$(awk '/container_name:/ {print $2}' /home/ubuntu/deploy/docker-compose.yml)
echo "CONTAINER_NAME: $CONTAINER_NAME"

# Compute certificate expiry date
CURRENT_DATE=$(date +"%Y-%m-%d")
echo "Current date: $CURRENT_DATE"
EXPIRY_DATE=$(openssl x509 -in "$CERT_PATH" -noout -enddate | cut -d= -f2 | awk '{print $1, $2, $4}' | xargs -I{} date -d "{}" +"%Y-%m-%d")
echo "Certificate expires at: $EXPIRY_DATE"
DAYS_LEFT=$(( ($(date -d "$EXPIRY_DATE" +%s) - $(date -d "$CURRENT_DATE" +%s)) / 86400 ))
echo "Days left until expiration: $DAYS_LEFT"

# If certificate expires in the next 7 days it will be renewed
if [[ "$DAYS_LEFT" -le 7 ]]; then
    echo "Certificate expires soon. Renewing now..."

    echo "... stopping Docker container: $CONTAINER_NAME"
    docker stop "$CONTAINER_NAME"

    echo "... renewing certificates with Certbot"
    sudo certbot renew --force-renewal --cert-name vitruv-server.org

    echo "... restarting Docker container: $CONTAINER_NAME"
    docker-compose -f "$DOCKER_COMPOSE_PATH" up -d
else
    echo "Certificate is still valid for more than 7 days. No actions required."
fi

echo "Stop Script"
echo ""
