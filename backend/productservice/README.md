
## Endpoints

Docker Compose publishes the Product Service directly over HTTP at `http://localhost:8082`. The recommended client entry point is the HTTPS API Gateway at `https://localhost:8080`, which preserves the same `/products` paths.

### Public Endpoints (No token required)
- `GET /products`: List all products
- `GET /products/{id}`: Get a product by ID
- `GET /actuator/health`: Health check

### Seller Endpoints (Requires `SELLER` role token)
- `POST /products`: Create a product
- `PUT /products/{id}`: Update an existing product (only the owner)
- `DELETE /products/{id}`: Delete a product (only the owner)

## Curl Examples

### List all products (Public)
```bash
curl -X GET http://localhost:8082/products
```

### Create a product (Seller only)
```bash
curl -X POST http://localhost:8082/products \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Super Widget",
    "description": "An awesome widget",
    "price": 19.99,
    "quantity": 100,
    "imageUrls": ["http://example.com/image1.png"]
  }'
```

### Update a product (Seller only)
```bash
curl -X PUT http://localhost:8082/products/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Super Widget 2.0",
    "description": "Updated description",
    "price": 24.99,
    "quantity": 50,
    "imageUrls": []
  }'
```
