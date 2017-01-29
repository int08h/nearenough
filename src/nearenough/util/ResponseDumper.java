package nearenough.util;

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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Random;
import nearenough.protocol.RtConstants;
import nearenough.protocol.RtEd25519;
import nearenough.protocol.RtEncoding;
import nearenough.protocol.RtHashing;
import nearenough.protocol.RtMessage;
import nearenough.protocol.RtTag;

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
    private final byte[] nonce;
    private final byte[] expectedLeafHash;

    public RequestHandler(InetSocketAddress addr) {
      Random rand = new Random();
      RtHashing hasher = new RtHashing();

      this.addr = addr;
      this.nonce = new byte[RtConstants.NONCE_LENGTH];
      rand.nextBytes(nonce);
      this.expectedLeafHash = hasher.hashLeaf(nonce);

      System.out.println("NONC       = " + ByteBufUtil.hexDump(nonce));
      System.out.println("LEAF(NONC) = " + ByteBufUtil.hexDump(expectedLeafHash));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      RtMessage msg = RtMessage.builder()
          .addPadding(true)
          .add(RtTag.NONC, nonce)
          .build();

      ByteBuf buf = RtEncoding.toWire(msg);

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

      ctx.close();
    }

    private void verifySrepSignature(RtMessage response) throws InvalidKeyException, SignatureException {
      RtMessage certMsg = RtMessage.fromBytes(response.get(RtTag.CERT));
      RtMessage deleMsg = RtMessage.fromBytes(certMsg.get(RtTag.DELE));

      byte[] pubKey = deleMsg.get(RtTag.PUBK);
      RtEd25519.Verifier verifier = new RtEd25519.Verifier(pubKey);
      verifier.update(RtConstants.SIGNED_RESPONSE_CONTEXT.getBytes());
      verifier.update(response.get(RtTag.SREP));

      System.out.println("SREP signature is " + verifier.verify(response.get(RtTag.SIG)));
    }

    private void verifyCertSignature(RtMessage response) throws InvalidKeyException, SignatureException {
      RtEd25519.Verifier verifier = new RtEd25519.Verifier(GOOGLE_PUBKEY);
      RtMessage certMsg = RtMessage.fromBytes(response.get(RtTag.CERT));

      verifier.update(RtConstants.CERTIFICATE_CONTEXT.getBytes());
      verifier.update(certMsg.get(RtTag.DELE));

      byte[] certSig = certMsg.get(RtTag.SIG);
      assert certSig.length == 64 : "bad length";

      System.out.println("CERT signature is " + verifier.verify(certSig));
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
