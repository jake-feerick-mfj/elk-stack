# POST event log

curl -X POST http://localhost:8000/logs \
  -H "Content-Type: application/json" \
  -d '{
        "id": "1234",
        "agencyId": "agency-xyz",
        "status": "DELIVERED",
        "payload": {
            "email": "user@example.com",
            "slack": "@user"
        }
      }'

# GET event
curl -X GET http://localhost:8000/events