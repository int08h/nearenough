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

package nearenough.util;

import static nearenough.util.BytesUtil.bytesToHex;
import static nearenough.util.BytesUtil.hexToBytes;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import nearenough.client.RoughtimeClient;
import nearenough.protocol.RtMessage;
import nearenough.protocol.RtWire;

/**
 * Send a one-off request to the given Roughtime server and dump the response (if any)
 */
public final class ResponseDumper {

  private static final byte[] GOOGLE_PUBKEY = hexToBytes(
      "7ad3da688c5c04c635a14786a70bcf30224cc25455371bf9d4a2bfb64b682534"
  );

  private static final class RequestHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final InetSocketAddress addr;
    private final RoughtimeClient client;

    public RequestHandler(InetSocketAddress addr) {
      this.addr = addr;
      this.client = new RoughtimeClient(GOOGLE_PUBKEY);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      RtMessage msg = client.createRequest();
      ByteBuf buf = RtWire.toWire(msg);

      ctx.writeAndFlush(new DatagramPacket(buf, addr))
          .addListener(fut -> {
            if (!fut.isSuccess()) {
              System.out.println("Send failed " + fut.cause().getMessage());
            }
          });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
      System.out.println("NONCE: " + bytesToHex(client.nonce()));
      System.out.printf(
          "Read message of %d bytes from %s:\n", msg.content().readableBytes(), msg.sender()
      );
      System.out.println(ByteBufUtil.prettyHexDump(msg.content()));

      RtMessage response = new RtMessage(msg.content());
      System.out.println(response);
      client.processResponse(response);

      if (client.isResponseValid()) {
        Instant midpoint = Instant.ofEpochMilli(client.midpoint() / 1000L);
        Instant local = Instant.now();
        int radiusSec = client.radius() / 1_000_000;
        System.out.println("midpoint    : " + midpoint + " (radius " + radiusSec + " sec)");
        System.out.println("local clock : " + local);
      } else {
        System.out.println("Response INVALID: " + client.invalidResponseCause().getMessage());
      }

      ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      System.out.println("Exception caught: " + cause.getMessage());
      ctx.close();
      throw new RuntimeException(cause);
    }
  }

  //
  // For example: roughtime.sandbox.google.com 2002
  //
  public static void main(String[] args) throws InterruptedException, NoSuchAlgorithmException {
    if (args.length != 2) {
      System.out.println("Usage: ResponseDumper SERVER PORT");
      System.exit(-1);
    }

    String server = args[0];
    int port = Integer.parseInt(args[1]);
    InetSocketAddress addr = new InetSocketAddress(server, port);

    System.out.printf("Will make request to %s\n", addr);

    NioEventLoopGroup group = new NioEventLoopGroup();
    Bootstrap bootstrap = new Bootstrap()
        .group(group)
        .channel(NioDatagramChannel.class)
        .remoteAddress(addr)
        .handler(new RequestHandler(addr));

    ChannelFuture connectFuture = bootstrap.connect();
    connectFuture.addListener(fut -> {
      if (!fut.isSuccess()) {
        System.out.println("Connect fail:");
        System.out.println(fut.cause().getMessage());
      }
    });

    connectFuture.channel().closeFuture().sync();
    group.shutdownGracefully();
  }
}
