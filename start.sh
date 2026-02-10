#!/bin/bash

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Kafka Airline Booking - Ordering Problem Demo Startup      ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Step 1: Check prerequisites
echo -e "${BLUE}[1/5] Checking prerequisites...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed${NC}"
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ All prerequisites met${NC}"
echo ""

# Step 2: Start Kafka
echo -e "${BLUE}[2/5] Starting Kafka infrastructure...${NC}"
docker-compose up -d

# Wait for Kafka to be ready
echo -e "${YELLOW}Waiting for Kafka to be ready (30 seconds)...${NC}"
sleep 30

echo -e "${GREEN}✓ Kafka infrastructure started${NC}"
echo -e "${GREEN}  - Kafka: localhost:9092${NC}"
echo -e "${GREEN}  - Kafka UI: http://localhost:8080${NC}"
echo ""

# Step 3: Build application
echo -e "${BLUE}[3/5] Building Spring Boot application...${NC}"
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Application built successfully${NC}"
echo ""

# Step 4: Start application
echo -e "${BLUE}[4/5] Starting Spring Boot application...${NC}"
echo -e "${YELLOW}Application will start in background...${NC}"

nohup mvn spring-boot:run > app.log 2>&1 &
APP_PID=$!

echo -e "${GREEN}✓ Application started (PID: $APP_PID)${NC}"
echo -e "${GREEN}  - API: http://localhost:8081${NC}"
echo ""

# Wait for app to be ready
echo -e "${YELLOW}Waiting for application to be ready (20 seconds)...${NC}"
sleep 20

# Step 5: Instructions
echo -e "${BLUE}[5/5] Setup Complete!${NC}"
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                     READY TO DEMO                            ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${GREEN}🚀 All services are running!${NC}"
echo ""
echo "📊 Kafka UI: http://localhost:8080"
echo "🌐 Application: http://localhost:8081"
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "                    RUN DEMOS"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo -e "${RED}🔴 Demo PROBLEM (out-of-order):${NC}"
echo "   curl -X POST http://localhost:8081/api/bookings/demo-problem"
echo ""
echo -e "${GREEN}🟢 Demo SOLUTION (ordered):${NC}"
echo "   curl -X POST http://localhost:8081/api/bookings/demo-solved"
echo ""
echo -e "${BLUE}📊 Demo MULTIPLE bookings:${NC}"
echo "   curl -X POST http://localhost:8081/api/bookings/demo-multiple"
echo ""
echo -e "${YELLOW}⚖️  COMPARISON (side-by-side):${NC}"
echo "   curl -X POST http://localhost:8081/api/bookings/demo-comparison"
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo -e "${BLUE}📋 View logs:${NC}"
echo "   tail -f app.log"
echo ""
echo -e "${BLUE}🛑 Stop all services:${NC}"
echo "   ./stop.sh"
echo ""
