#!/bin/bash

# Banking Ledger Deployment Script
# This script helps setup and deploy the distributed banking ledger system

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║     Banking Ledger Microservices - Deployment Script       ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"

# Functions
function build_project() {
    echo -e "\n${YELLOW}[1/3] Building all services...${NC}"
    cd "$SCRIPT_DIR"
    mvn clean package -DskipTests
    echo -e "${GREEN}✓ Build completed successfully!${NC}"
}

function start_containers() {
    echo -e "\n${YELLOW}[2/3] Starting Docker containers...${NC}"
    cd "$SCRIPT_DIR"
    docker-compose up -d
    echo -e "${GREEN}✓ Containers started!${NC}"
    
    echo -e "\n${YELLOW}Waiting for services to be ready...${NC}"
    sleep 10
}

function health_check() {
    echo -e "\n${YELLOW}[3/3] Checking service health...${NC}"
    
    echo -e "\n${YELLOW}Eureka Server:${NC}"
    curl -s http://localhost:8761/eureka/apps | grep -o '<status>[^<]*</status>' | head -1 || echo "Not ready yet..."
    
    echo -e "\n${YELLOW}Account Service:${NC}"
    curl -s http://localhost:8081/actuator/health | grep -o '"status":"[^"]*"' || echo "Not ready yet..."
    
    echo -e "\n${YELLOW}Transfer Service:${NC}"
    curl -s http://localhost:8082/actuator/health | grep -o '"status":"[^"]*"' || echo "Not ready yet..."
    
    echo -e "\n${YELLOW}Notification Service:${NC}"
    curl -s http://localhost:8083/actuator/health | grep -o '"status":"[^"]*"' || echo "Not ready yet..."
    
    echo -e "\n${YELLOW}API Gateway:${NC}"
    curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' || echo "Not ready yet..."
    
    echo -e "\n${GREEN}✓ Health checks completed!${NC}"
}

function show_services() {
    echo -e "\n${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                      Services Ready                        ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
    
    echo -e "\n${YELLOW}API Endpoints:${NC}"
    echo -e "  ${GREEN}API Gateway:${NC}            http://localhost:8080"
    echo -e "  ${GREEN}Account Service:${NC}        http://localhost:8081"
    echo -e "  ${GREEN}Transfer Service:${NC}       http://localhost:8082"
    echo -e "  ${GREEN}Notification Service:${NC}   http://localhost:8083"
    echo -e "  ${GREEN}Eureka Dashboard:${NC}       http://localhost:8761/eureka"
    echo -e "  ${GREEN}MailHog (Email):${NC}       http://localhost:8025"
    
    echo -e "\n${YELLOW}Database:${NC}"
    echo -e "  ${GREEN}PostgreSQL:${NC}            localhost:5432 (user: postgres, password: password)"
    
    echo -e "\n${YELLOW}Message Broker:${NC}"
    echo -e "  ${GREEN}Kafka:${NC}                 localhost:9092"
    
    echo -e "\n${YELLOW}Quick Test:${NC}"
    echo -e "  Create Account:  POST http://localhost:8080/accounts"
    echo -e "  List Accounts:   GET  http://localhost:8080/accounts"
    echo -e "  Transfer Money:  POST http://localhost:8080/transfers"
    
    echo -e "\n${YELLOW}Docker Commands:${NC}"
    echo -e "  View logs:       docker-compose logs -f"
    echo -e "  Stop services:   docker-compose down"
    echo -e "  Rebuild images:  docker-compose up -d --build"
}

# Menu
while true; do
    echo -e "\n${YELLOW}Select an option:${NC}"
    echo "1) Full deployment (build + start + health check)"
    echo "2) Build services only"
    echo "3) Start Docker containers"
    echo "4) Stop containers"
    echo "5) View logs"
    echo "6) Health check"
    echo "7) Show service info"
    echo "8) Exit"
    
    read -p "Enter your choice (1-8): " choice
    
    case $choice in
        1)
            build_project
            start_containers
            health_check
            show_services
            ;;
        2)
            build_project
            ;;
        3)
            start_containers
            ;;
        4)
            echo -e "\n${YELLOW}Stopping containers...${NC}"
            cd "$SCRIPT_DIR"
            docker-compose down
            echo -e "${GREEN}✓ Containers stopped!${NC}"
            ;;
        5)
            cd "$SCRIPT_DIR"
            docker-compose logs -f
            ;;
        6)
            health_check
            ;;
        7)
            show_services
            ;;
        8)
            echo -e "\n${GREEN}Goodbye!${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid choice. Please try again.${NC}"
            ;;
    esac
done
