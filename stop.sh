#!/bin/bash

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║           Stopping Kafka Airline Booking Demo               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Stop Spring Boot application
echo -e "${YELLOW}Stopping Spring Boot application...${NC}"
pkill -f "spring-boot:run"
pkill -f "kafka-airline-booking"
echo -e "${GREEN}✓ Application stopped${NC}"
echo ""

# Stop Docker containers
echo -e "${YELLOW}Stopping Kafka infrastructure...${NC}"
docker-compose down
echo -e "${GREEN}✓ Kafka infrastructure stopped${NC}"
echo ""

# Clean up log file
if [ -f "app.log" ]; then
    echo -e "${YELLOW}Cleaning up log files...${NC}"
    rm app.log
    echo -e "${GREEN}✓ Logs cleaned${NC}"
fi

echo ""
echo -e "${GREEN}All services stopped successfully!${NC}"
echo ""
