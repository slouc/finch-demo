# Minimal finch REST API server 
## (on top of finagle/TwitterServer)

### What is this?

This is a minimal finch server implementation running on top of TwitterServer with circe encoding/decoding.

Available endpoints:

`GET /hello/world/{name}
`POST /post/stuff with body: { "stuff" : "whatever" }`

### Usage

All it takes to get going is a simple `sbt run`. Server runs on `localhost:8080`.
