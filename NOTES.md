# Notes
The spec is here:
https://tools.ietf.org/html/draft-ietf-httpbis-http2-17#page-86

# Tests

# TODO
Run through a FindBugs plugin

## H2 Ping
client sends preface

## HTTP1/1 Upgrade

```
GET / HTTP/1.1
Host: server.example.com
Connection: Upgrade, HTTP2-Settings
Upgrade: h2c
HTTP2-Settings: <base64url encoding of HTTP/2 SETTINGS payload>
```