# Nearenough
**Nearenough** is a Java implementation of the 
[Roughtime](https://roughtime.googlesource.com/roughtime) secure time synchronization protocol.

[Roughtime](https://roughtime.googlesource.com/roughtime) is a protocol that aims to achieve rough 
time synchronisation in a secure way that doesn't depend on any particular time server, and in such
a way that, if a time server does misbehave, clients end up with cryptographic proof of it. It was 
created by Adam Langley and Robert Obryk.

## Links
* [Nearenough Github repo](https://github.com/int08h/nearenough)
* [Roughtime project](https://roughtime.googlesource.com/roughtime)
* [Netty project](http://netty.io/)

## Building
Nearenough bundles all required dependencies in the `lib` directory. Add those `.jar` files to
your IDE's project classpath. Building is IDE-only for the moment. 

## Client Quickstart

### Client Examples
See [NioClient.java](../src/examples/NioClient.java) and 
[NettyClient.java](../src/examples/NettyClient.java) for examples of sending a request to a 
Roughtime server and processing the response.

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
Nearenough is not stable yet. Expect significant changes as the code evolves.

* Protocol - Feature complete
* Client - Feature complete
* Server - Not started
  
## Contributors
* Stuart Stock, original author (stuart {at} int08h.com)

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
