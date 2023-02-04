# java-microservices

## aim:
This repo sought to replicate a POS system in a microservice pattern

## built:
Api-Gateway
  - Using Spring Cloud Gateway

Discovery Server
  - Service discovery using Netflix-Eureka

Inventory Service
  - Inventory API to check if product is in stock
  - MySql DB

Order Service
  - Order API synchronous communication with Inventory Service 
    - Implemented Circuit Breaker with Resilience4j
  - Order API asynchronous communication with Notification Service
    - Implemented Kafka 
    - Kafka producer

Product Service
  - Product API to create and get products
  - Mongo DB

Notification Service
  - Kafka consumer
  
Add-ons
  - Implemented Distributed Tracing with Brave and Zipkin
  - Secure microservices using Keycloak

## enhancements:
- Dockerize application
- Implement Prometheus and Grafana for monitoring