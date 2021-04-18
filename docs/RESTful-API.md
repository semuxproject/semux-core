# RESTful API

Semux provides a rich set of RESTful APIs for interacting with the blockchain programmatically.

Note that You can also access the same set of APIs via the [Console](./Console.md)

## Configuration

Your local Semux API server can be enabled by changing following properties in your `config/semux.properties`:
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
http://localhost:5171/

## Web API Explorers

If you want to interact with the semux network without having a local client running, you can access many (but not all)
of the API calls at some hosted APIs. This gives you a swagger client similar to a locally running one.

- Mainnet: https://api.semux.info/

## API Clients

Semux provides a [Swagger API Spec in JSON](../src/main/resources/org/semux/api/swagger/v2.2.0.json) since API version 2. 
You can either generate an API client by yourself using [Swagger Codegen](https://github.com/swagger-api/swagger-codegen) 
or use the following pre-generated clients:

- Javascript: https://github.com/semuxproject/semux-js
- Java: https://github.com/orogvany/semux-java-client
