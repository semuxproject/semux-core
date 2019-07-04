# RESTful API

Semux provides a rich RESTful API for interacting with the blockchain programatically.

## Configuration

Semux API server can be enabled by changing following properties in your `config/semux.properties`:
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

## API Explorer

Once you've successfully started Semux API server, a Swagger UI page is available locally as an API explorer at: 
http://localhost:5171/index.html

## API Clients

Semux provides a [Swagger API Spec in JSON](../src/main/resources/org/semux/api/swagger/v2.2.0.json) since API version 2. 
You can either generate an API client by yourself using [Swagger Codegen](https://github.com/swagger-api/swagger-codegen) 
or use the following pre-generated clients:

- Javascript: https://github.com/semuxproject/semux-js
- PHP: https://github.com/semuxproject/semux-php
- Python: https://github.com/mdodong/semuxapi-py
- Java: https://github.com/orogvany/semux-java-client
