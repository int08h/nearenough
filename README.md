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

## Implementation Status
Nearenough is in its infancy. Expect significant changes as the code evolves.
  
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
