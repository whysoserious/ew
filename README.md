# Ticket service

This sample application demonstrates a simple ticket service built with Lagom. It contains two services: a user service, for managing users, and an event service, for managing events and ticket reservations.

Event service consumes data fetched from user service.

Both services persist its data to a relational database using Lagom's persistence API and demonstrate how to persist state using Lagom.

## Requirements

This sample requires Kafka and Postgres as external services. They are pre-configured in `docker-compose.yml` file. Meaning that before running the service, you first need to start the services using the following command:

```bash
docker-compose up -d
```

Postgres instances will be available on ports `5433` for user service and `5434` for event service and Kafka on port `9092`.

## Running in dev mode

After setting up all the requirements, to run the application in dev mode, execute the following command:

```bash
sbt runAll
```

## Running tests

After setting up all the requirements, to run tests, execute the following command:

```bash
sbt test
```

## User service

The user service offers three REST endpoints:

* Create a new user:

```bash
curl -v -H "Content-Type: application/json" -d '{"name": "Jon Kilroy"}' -X POST http://localhost:9000/user | jq .
```

* Get a user:

```bash
curl -v http://localhost:9000/user/4db6ed98-a68f-4a54-9d6e-8aecf57a7684 | jq .
```

* Add number of reserved tickets (`cnt` can be less than 0):

```bash
curl -v -H "Content-Type: application/json" -d '{"cnt": 4}' -X PATCH http://localhost:9000/user/4db6ed98-a68f-4a54-9d6e-8aecf57a7684 | jq .
```

## Event service

The event service offers three REST endpoints:

* Create a new event:

```bash
curl -v -H "Content-Type: application/json" -d '{"name": "Olympics 2021", "availableTickets": 30}' -X POST http://localhost:9000/event | jq .
```

* Get an event:

```bash
curl -v http://localhost:9000/event/6e94190b-8d7c-40b2-ac61-9169d46130b4 | jq .
```

* Create a reservation for given event and user:

```bash
curl -v -H "Content-Type: application/json" \
  -d '{"userId": "80bf5592-c2bf-40af-ba90-f624f0fb1d2e", "ticketsCnt": 4, "reservationTime": "2020-12-03T10:15:30+01:00[Europe/Warsaw]"}' \
  -X POST \
  http://localhost:9000/event/6e94190b-8d7c-40b2-ac61-9169d46130b4/reservation | jq .
```

