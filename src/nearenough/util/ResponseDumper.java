package nearenough.util;

import static nearenough.protocol.RtConstants.TIMESTAMP_LENGTH;
import static nearenough.util.Preconditions.checkState;
import static net.i2p.crypto.eddsa.Utils.hexToBytes;

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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import nearenough.client.RoughtimeClient;
import nearenough.exceptions.MerkleTreeInvalid;
import nearenough.exceptions.MidpointInvalid;
import nearenough.exceptions.SignatureInvalid;
import nearenough.protocol.RtMessage;
import nearenough.protocol.RtTag;
import nearenough.protocol.RtWire;

/**
 * Send a one-off request to the given Roughtime server and dump the response (if any)
 *
 * Dev tool only, not an exemplar of the "right way" to do things.
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
      System.out.printf(
          "Read message of %d bytes from %s:\n", msg.content().readableBytes(), msg.sender()
      );
      System.out.println(ByteBufUtil.prettyHexDump(msg.content()));
      RtMessage response = new RtMessage(msg.content());
      System.out.println(response);

      verifyCertSignature(response);
      verifySrepSignature(response);
      verifyNonceIncluded(response);
      verifyMidpointBounds(response);

      printTime(response);

      ctx.close();
    }

    private void verifyMidpointBounds(RtMessage response) {
      try {
        client.verifyMidpointBounds(response);
        System.out.println("Midpoint is GOOD");
      } catch (MidpointInvalid e) {
        System.out.println("Midpoint INVALID: " + e.getMessage());
      }
    }

    private void verifyNonceIncluded(RtMessage response) {
      try {
        client.verifyNonceIncluded(response);
        System.out.println("Nonce IS included in response");
      } catch (MerkleTreeInvalid e) {
        System.out.println("Nonce NOT included in response: " + e.getMessage());
      }
    }

    private void printTime(RtMessage response) {
      ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
      RtMessage srepMsg = RtMessage.fromBytes(response.get(RtTag.SREP));
      byte[] midpBytes = srepMsg.get(RtTag.MIDP);
      checkState(midpBytes.length == TIMESTAMP_LENGTH);

      System.out.println("Midp : " + RtWire.timeFromMidpoint(midpBytes));
      System.out.println("Now  : " + now);
    }

    private void verifySrepSignature(RtMessage response) {
      try {
        client.verifyTopLevelSignature(response);
        System.out.println("SREP signature is GOOD");
      } catch (SignatureInvalid e) {
        System.out.println("SREP signature BAD: " + e.getMessage());
      }
    }

    private void verifyCertSignature(RtMessage response) {
      try {
        client.verifyDelegatedKey(response);
        System.out.println("CERT signature is GOOD");
      } catch (SignatureInvalid e) {
        System.out.println("CERT signature BAD: " + e.getMessage());
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      System.out.println("Exception caught: " + cause.getMessage());
      ctx.close();
      throw new RuntimeException(cause);
    }
  }

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
