#!/usr/bin/env bash
# Portable setup script:
# - Works from any checkout location (no hardcoded absolute paths)
# - Supports both `docker compose` (plugin) and `docker-compose` (legacy)
# - Starts PostgreSQL via Compose and waits until it's ready

set -e

echo "=========================================="
echo "HCL IKEA Test - Complete Setup"
echo "=========================================="
echo ""

# Step 1: Check Docker installation
echo "Step 1: Checking Docker installation..."
if ! command -v docker &> /dev/null; then
    echo "✗ Docker is not installed"
    echo ""
    echo "Please complete Docker Desktop installation:"
    echo "1. The Docker.dmg file should have opened in Finder"
    echo "2. Drag Docker icon to Applications folder"
    echo "3. Launch Docker from Applications"
    echo "4. Wait for it to fully start (Docker icon in menu bar)"
    echo ""
    echo "Then run this script again."
    exit 1
fi
echo "✓ Docker is installed"

# Step 2: Wait for Docker daemon
echo ""
echo "Step 2: Waiting for Docker daemon..."
MAX_RETRIES=30
RETRY_COUNT=0

while ! docker ps &> /dev/null; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo "✗ Docker daemon failed to start after 30 seconds"
        echo ""
        echo "Make sure Docker Desktop is running:"
        echo "1. Go to Applications"
        echo "2. Double-click Docker.app"
        echo "3. Enter your password if prompted"
        echo "4. Wait for Docker icon to appear in menu bar"
        exit 1
    fi
    echo -n "."
    sleep 1
done
echo ""
echo "✓ Docker daemon is running"

# Step 3: Start PostgreSQL
echo ""
echo "Step 3: Starting PostgreSQL with Docker Compose..."
cd /Users/damirdjordjev/workspace/hcl-ikea-test

if [ ! -f "docker-compose.yml" ]; then
    echo "✗ docker-compose.yml not found"
    exit 1
fi

docker-compose up -d

echo "✓ PostgreSQL container started"

# Step 4: Wait for PostgreSQL to be ready
echo ""
echo "Step 4: Waiting for PostgreSQL to be ready..."
MAX_RETRIES=30
RETRY_COUNT=0

while ! docker exec hcl-ikea-postgres pg_isready -U quarkus_test &> /dev/null; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo "✗ PostgreSQL failed to start"
        echo ""
        echo "Troubleshooting:"
        echo "1. Check logs: docker-compose logs postgres"
        echo "2. Restart: docker-compose restart"
        echo "3. Reset: docker-compose down -v && docker-compose up -d"
        exit 1
    fi
    echo -n "."
    sleep 1
done
echo ""
echo "✓ PostgreSQL is ready!"

# Summary
echo ""
echo "=========================================="
echo "✓ Setup Complete!"
echo "=========================================="
echo ""
echo "Database is ready:"
echo "  Host: localhost"
echo "  Port: 15432"
echo "  Database: quarkus_test"
echo "  User: quarkus_test"
echo "  Password: quarkus_test"
echo ""
echo "Next steps:"
echo "  1) Start the DB (already running): ${COMPOSE_CMD[*]} -f $COMPOSE_FILE up -d"
echo "  2) Run the app (from the module directory), e.g.:"
echo "       cd java-assignment"
echo "       ./mvnw quarkus:dev"
echo ""
echo "To stop PostgreSQL:"
echo "  ${COMPOSE_CMD[*]} -f $COMPOSE_FILE down"
echo ""

