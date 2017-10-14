# Nearenough 

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)
[![Build Status](https://travis-ci.org/int08h/nearenough.svg?branch=master)](https://travis-ci.org/int08h/nearenough)

**Nearenough** is a Java client of the [Roughtime](https://roughtime.googlesource.com/roughtime) 
secure time synchronization protocol.

Nearenough aims to be 100% interoperable with the wider Roughtime ecosystem.

## About the Roughtime Protocol
[Roughtime](https://roughtime.googlesource.com/roughtime) is a protocol that aims to achieve rough 
time synchronisation in a secure way that doesn't depend on any particular time server, and in such
a way that, if a time server does misbehave, clients end up with cryptographic proof of it. It was 
created by Adam Langley and Robert Obryk.

## Links
* [Nearenough Github repo](https://github.com/int08h/nearenough)
* [Roughtime project](https://roughtime.googlesource.com/roughtime)
* My blog posts [describing Roughtime features](https://int08h.com/post/to-catch-a-lying-timeserver/) and 
  exploring the [Nearenough API and details of Roughtime messages](https://int08h.com/post/roughtime-message-anatomy/).

## Building
Nearenough bundles all required dependencies in the `lib` directory. Add those `.jar` files to
your IDE's project classpath. Building is IDE-only for the moment. 

## Quickstart

### Client Examples
See [`examples/NioClient.java`](../master/examples/NioClient.java) and 
[`examples/NettyClient.java`](../master/examples/NettyClient.java) for examples of how to send a 
request to a Roughtime server and process the response.

### DIY Client
If implementing your own client, the general idea is:

```java
// The RoughTime server's long term public key, must be obtained a priori
byte[] serverLongTermPublicKey = { ... };

// Create client passing the server's long-term key
RoughtimeClient client = new RoughtimeClient(serverLongTermPublicKey);

// Construct a request, then encode it for transmission
RtMessage request = client.createRequest();
ByteBuf encodedRequest = RtWire.toWire(request);

// send encodedRequest using NIO, Netty, or some other mechanism...
RtMessage response = // ...and receive the response via NIO, Netty, etc ...

// Process the response
client.processResponse(response);

// Check the result
if (client.isResponseValid()) {
  Instant midpoint = Instant.ofEpochMilli(client.midpoint() / 1000L);
  System.out.println("midpoint: " + midpoint);
} else {
  System.out.println("Invalid response: " + client.invalidResponseCause().getMessage());
} 
```
See the javadocs in [`RoughtimeClient.java`](../master/src/nearenough/client/RoughtimeClient.java) 
for more information.

## Implementation Status
Nearenough is stable. 

* Protocol - Client protocol is feature complete. 
* Client - Feature complete except for ecosystem-style request chaining.
  
## Contributors
* Stuart Stock, original author and current maintainer (stuart {at} int08h.com)

If you would like to contribute to Nearenough, please see the guidelines in 
[CONTRIBUTING.md](../master/CONTRIBUTING.md).

## Copyright and License
Nearenough is Copyright (c) 2017 int08h LLC. All rights reserved. 

int08h LLC licenses Nearenough (the "Software") to you under the Apache License, version 2.0 
(the "License"); you may not use this Software except in compliance with the License. You may obtain 
a copy of the License from the [LICENSE](../master/LICENSE) file included with the Software or at:

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License 
is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
implied. See the License for the specific language governing permissions and limitations under 
the License.
