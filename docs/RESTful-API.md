# Semux API

## Configuration 

Semux API server can be enabled by adding following properties into your `config/semux.properties`:
```
# Be sure to set up authentication first before enabling API
api.enabled = true

# Listening address and port
api.listenIp = 127.0.0.1
api.listenPort = 5171

# Basic authentication
api.username = YOUR_API_USERNAME
api.password = YOUR_API_PASSWORD
```

## Base Unit

The base unit of Semux API is Nano SEM (10<sup>-9</sup> SEM)

## Documentation

Semux API documentation can be found at: https://semuxproject.github.io/semux-api-docs/

## API Explorer

Once you've successfully started Semux API server, a Swagger UI page is available locally as an API explorer at: http://localhost:5171/v2.0.0/swagger.html

## API Clients

Semux started to provide a [Swagger API Spec in JSON](../src/main/resources/org/semux/api/v2_0_0/swagger.json) since API version 2. You can either generate an API client by yourself using [Swagger Codegen](https://github.com/swagger-api/swagger-codegen) or use the following pre-generated clients:

- Javascript: https://github.com/semuxproject/semux-js-sdk
- PHP: https://github.com/semuxproject/semux-php-sdk
- Python: https://github.com/mdodong/semuxapi-py