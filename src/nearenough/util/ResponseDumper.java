package nearenough.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import nearenough.protocol.RtConstants;
import nearenough.protocol.RtMessage;
import nearenough.protocol.RtTag;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Send a one-off request to the given Roughtime server and dump the response (if any)
 *
 * Dev tool only, not an exemplar of the "right way" to do things.
 */
public final class ResponseDumper {

  private static final class RequestHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final InetSocketAddress addr;
    private final byte[] nonce;
    private final byte[] expectedLeafHash;

    public RequestHandler(InetSocketAddress addr) {
      Random rand = new Random();
      MessageDigest sha512;
      try {
        sha512 = MessageDigest.getInstance("SHA-512");
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }

      this.addr = addr;
      this.nonce = new byte[64];

      rand.nextBytes(nonce);

      sha512.update(RtConstants.TREE_LEAF_TWEAK);
      sha512.update(nonce);
      this.expectedLeafHash = sha512.digest();

      System.out.println("NONC       = " + ByteBufUtil.hexDump(nonce));
      System.out.println("LEAF(NONC) = " + ByteBufUtil.hexDump(expectedLeafHash));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      ByteBuf request = ctx.alloc().buffer(1024)
          .writeIntLE(2) // num tags
          .writeIntLE(64) // offset to start of PAD value
          .writeInt(RtTag.NONC.wireEncoding()) // NONC tag
          .writeInt(RtTag.PAD.wireEncoding())  // PAD tag
          .writeBytes(nonce); // nonce value

      while (request.writableBytes() > 0) {
        request.writeInt(0); // padding
      }

      ctx.writeAndFlush(new DatagramPacket(request, addr))
          .addListener(fut -> {
            if (!fut.isSuccess()) {
              System.out.println("Send failed " + fut.cause().getMessage());
            }
          });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
      System.out.printf("Read message (%s):\n", msg.sender());
      System.out.println(ByteBufUtil.prettyHexDump(msg.content()));
      RtMessage response = new RtMessage(msg.content());
      System.out.println(response);
      ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      System.out.println("Exception caught: " + cause.getMessage());
      ctx.close();
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
