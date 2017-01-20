# Nearenough
A Netty-based implementation of the [Roughtime](https://roughtime.googlesource.com/roughtime) 
secure time synchronization protocol.

## Links
* [Nearenough Github repo](https://github.com/int08h/nearenough)
* [Roughtime project](https://roughtime.googlesource.com/roughtime)
* [Netty project](http://netty.io/)

## Building
Nearenough bundles all required dependencies in the `lib` directory. Add those `.jar` files to
your IDE's project classpath. Building is IDE-only for the moment. 

## Implementation Status
* **Server**

  |Feature|State|
  |:-----:|:---:|
  | Parse valid client request          | DONE |
  | Single-node response                | Not started |
  | Nested (full Merkle tree) responses | Not started |
  | Long-term key management            | Not started |
  
* **Client**
  * Not started

## About the Roughtime Protocol
From the [Roughtime](https://roughtime.googlesource.com/roughtime) project page:

  > Roughtime is a protocol that aims to achieve rough time synchronisation in a secure way 
  > that doesn't depend on any particular time server, and in such a way that, if a time 
  > server does misbehave, clients end up with cryptographic proof of it.
  
The protocol was created by Adam Langley and Robert Obryk at Google.

## Contributors
* Stuart Stock, original author (stuart {at} int08h.com)

## Copyright and License
Nearenough is Copyright (c) 2017 int08h, LLC. All rights reserved. 

int08h, LLC licenses this file to you under the Apache License, version 2.0 (the "License"); you 
may not use this file except in compliance with the License. You may obtain a copy of the License 
from the [LICENSE](../blob/master/LICENSE) file included with the software or at:

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License 
is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
implied. See the License for the specific language governing permissions and limitations under 
the License.
