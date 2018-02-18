/*
 * Copyright (c) 2017 int08h LLC. All rights reserved.
 *
 * int08h LLC licenses Nearenough (the "Software") to you under the Apache License, version 2.0
 * (the "License"); you may not use this Software except in compliance with the License. You may
 * obtain a copy of the License from the LICENSE file included with the Software or at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nearenough.examples;

import static nearenough.util.BytesUtil.hexToBytes;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.time.Instant;
import nearenough.client.RoughtimeClient;
import nearenough.protocol.RtMessage;
import nearenough.protocol.RtWire;

/**
 * Use Java NIO to send a request to the given Roughtime server and dump the response (if any)
 */
public final class NioClient {

  // Hostname and port of the public int08h.com Roughtime server
  private static final String INT08H_SERVER_HOST = "roughtime.int08h.com";
  private static final int INT08H_SERVER_PORT = 2002;

  // Long-term public key of the public int08h.com Roughtime server
  private static final byte[] INT08H_SERVER_PUBKEY = hexToBytes(
      "016e6e0284d24c37c6e4d7d8d5b4e1d3c1949ceaa545bf875616c9dce0c9bec1"
  );

  @SuppressWarnings("Duplicates")
  public static void main(String[] args) throws IOException, InterruptedException {
    InetSocketAddress addr = new InetSocketAddress(INT08H_SERVER_HOST, INT08H_SERVER_PORT);
    System.out.printf("Sending request to %s\n", addr);

    // Nonblocking NIO UDP channel for the remote Roughtime server
    DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
    channel.configureBlocking(false);

    // Create a new RoughtimeClient instance
    RoughtimeClient client = new RoughtimeClient(INT08H_SERVER_PUBKEY);

    // Create a request message
    RtMessage request = client.createRequest();

    // Encode for transmission
    ByteBuf encodedRequest = RtWire.toWire(request);

    // Send the message
    channel.send(encodedRequest.nioBuffer(), addr);
    int bytesWritten = channel.send(encodedRequest.nioBuffer(), addr);

    // Ensure the message was sent
    if (bytesWritten != encodedRequest.readableBytes()) {
      throw new RuntimeException("failed to fully write request");
    }

    // Space for receiving the reply
    ByteBuffer recvBuf = ByteBuffer.allocate(4096);
    int attempts = 50;

    // Simple loop to look for the first response. Wait for max 5 seconds.
    while (--attempts > 0) {
      recvBuf.clear();
      channel.receive(recvBuf);
      recvBuf.flip();
      if (recvBuf.hasRemaining()) {
        break;
      }
      Thread.sleep(100L);
    }

    if (recvBuf.hasRemaining()) {
      // A reply from the server has been received
      System.out.printf("Read message of %d bytes from %s:\n", recvBuf.remaining(), addr);

      // Parse the response
      RtMessage response = RtMessage.fromByteBuffer(recvBuf);
      System.out.println(response);

      // Validate the response. Checks that the message is well-formed, all signatures are valid,
      // and our nonce is present in the response.
      client.processResponse(response);

      if (client.isResponseValid()) {
        // Validation passed, the response is good

        // The "midpoint" is the Roughtime server's reported timestamp (in microseconds). And the
        // "radius" is a span of uncertainty around that midpoint. A Roughtime server asserts that
        // its "true time" lies within the span.
        Instant midpoint = Instant.ofEpochMilli(client.midpoint() / 1_000L);
        int radiusSec = client.radius() / 1_000_000;
        System.out.println("midpoint    : " + midpoint + " (radius " + radiusSec + " sec)");

        // For comparison, also print the local clock. If the midpoint and your local time
        // are widely different, check your local machine's time sync!
        Instant local = Instant.now();
        System.out.println("local clock : " + local);

      } else {
        // Validation failed. Print out the reason why.
        System.out.println("Response INVALID: " + client.invalidResponseCause().getMessage());
      }

    } else {
      // No reply within five seconds
      System.out.println("No response from " + addr);
    }

    System.exit(0);
  }
}
