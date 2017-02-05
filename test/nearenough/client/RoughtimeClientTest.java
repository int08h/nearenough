package nearenough.client;

import nearenough.exceptions.MerkleTreeInvalid;
import nearenough.exceptions.MidpointInvalid;
import nearenough.exceptions.SignatureInvalid;
import nearenough.protocol.RtConstants;
import nearenough.protocol.RtMessage;
import nearenough.protocol.RtTag;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

import static nearenough.util.BytesUtil.hexToBytes;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public final class RoughtimeClientTest {

  // Google Roughtime server long-term public key
  private static final byte[] GOOGLE_PUBKEY = hexToBytes(
      "7ad3da688c5c04c635a14786a70bcf30224cc25455371bf9d4a2bfb64b682534"
  );

  // Nonce used in request to generate below reply
  private static final byte[] NONCE = hexToBytes(
      "f86780655780f0a7e62a9cfdb3b7b73bd7773557aadde6274680d05a1857d2cfa901bdcc62c6dee91a5bdfbae2cacbbfeb4d59921d39d62c86e6aaeabea1d887"
  );

  /*
   * Response below was from the Google Roughtime server, responding to the above nonce
   *
   *  RtMessage|5|{
   *    SIG(64) = 8377ffb3f3ba0ccb4dd18cf0c866075afbfd552e2492fae939a0465e6583cd33254a0e19a8e1264bb306085010e05a90d152bebf03bd6e43820356e554372000
   *    PATH(0) =
   *    SREP(100) = RtMessage|3|{
   *      RADI(4) = 40420f00
   *      MIDP(8) = fe42b30ece470500
   *      ROOT(64) = 710ed4bc7d867fdf3634764b9986f65bf5b2c73d6929832ca56dbeeced73c8f64af46d8542ca114f4064b11fa4b3d038b38e44400a66d5314f74a9efb4538bba
   *    }
   *    CERT(152) = RtMessage|2|{
   *      SIG(64) = 3ed157a3f453df76d96ded77e785760fb22fdbd4f1359fe51ee2762979ef29d524a1889a44fa68523d213315fec8f28b00efe6f9adb8d67d7fc9eabcca57fe01
   *      DELE(72) = RtMessage|3|{
   *        PUBK(32) = 5cb8d3705758b0bddf45a1e21cd40a8e2e6ae314c1c4811d64674fee4a4c2a89
   *        MINT(8) = 0010db74cb470500
   *        MAXT(8) = 00f00f0a30480500
   *      }
   *    }
   *    INDX(4) = 00000000
   *  }
   * midpoint    : 2017-02-05T20:06:59.017Z (radius 1 sec)
   *
   */
  private static final byte[] RESPONSE = hexToBytes(
    //  0 1 2 3 4 5 6 7 8 9 a b c d e f
      "050000004000000040000000a4000000" + // 0000 ....@...@.......
      "3c010000534947005041544853524550" + // 0010 <...SIG.PATHSREP
      "43455254494e44588377ffb3f3ba0ccb" + // 0020 CERTINDX.w......
      "4dd18cf0c866075afbfd552e2492fae9" + // 0030 M....f.Z..U.$...
      "39a0465e6583cd33254a0e19a8e1264b" + // 0040 9.F^e..3%J....&K
      "b306085010e05a90d152bebf03bd6e43" + // 0050 ...P..Z..R....nC
      "820356e5543720000300000004000000" + // 0060 ..V.T7 .........
      "0c000000524144494d494450524f4f54" + // 0070 ....RADIMIDPROOT
      "40420f00fe42b30ece470500710ed4bc" + // 0080 @B...B...G..q...
      "7d867fdf3634764b9986f65bf5b2c73d" + // 0090 }...64vK...[...=
      "6929832ca56dbeeced73c8f64af46d85" + // 00a0 i).,.m...s..J.m.
      "42ca114f4064b11fa4b3d038b38e4440" + // 00b0 B..O@d.....8..D@
      "0a66d5314f74a9efb4538bba02000000" + // 00c0 .f.1Ot...S......
      "400000005349470044454c453ed157a3" + // 00d0 @...SIG.DELE>.W.
      "f453df76d96ded77e785760fb22fdbd4" + // 00e0 .S.v.m.w..v../..
      "f1359fe51ee2762979ef29d524a1889a" + // 00f0 .5....v)y.).$...
      "44fa68523d213315fec8f28b00efe6f9" + // 0100 D.hR=!3.........
      "adb8d67d7fc9eabcca57fe0103000000" + // 0110 ...}.....W......
      "20000000280000005055424b4d494e54" + // 0120  ...(...PUBKMINT
      "4d4158545cb8d3705758b0bddf45a1e2" + // 0130 MAXT\..pWX...E..
      "1cd40a8e2e6ae314c1c4811d64674fee" + // 0140 .....j......dgO.
      "4a4c2a890010db74cb47050000f00f0a" + // 0150 JL*....t.G......
      "3048050000000000"                   // 0160 0H......
  );

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void successfulResponseValidation() {
    RoughtimeClient client = new RoughtimeClient(GOOGLE_PUBKEY, NONCE);
    client.processResponse(RtMessage.fromBytes(RESPONSE));

    assertTrue(client.isResponseValid());
    assertThat(client.midpoint(), equalTo(1486325219017470L));
    assertThat(client.radius(), equalTo(1_000_000));
  }

  @Test
  public void invalidSrepSignature() {
    byte[] responseCopy = Arrays.copyOf(RESPONSE, RESPONSE.length);
    // modify the top-level SIG on SREP
    responseCopy[0x28] += 1;
    RtMessage responseMsg = RtMessage.fromBytes(responseCopy);

    RoughtimeClient client = new RoughtimeClient(GOOGLE_PUBKEY, NONCE);
    client.processResponse(responseMsg);

    assertFalse("response fails to validate", client.isResponseValid());
    assertThat(client.invalidResponseCause(), instanceOf(SignatureInvalid.class));
    assertThat(
        client.invalidResponseCause().getMessage(),
        containsString("signature on SREP does not match")
    );
  }

  @Test
  public void invalidDeleSignature() {
    byte[] responseCopy = Arrays.copyOf(RESPONSE, RESPONSE.length);
    // modify the SIG on the DELE delegated certificate
    responseCopy[0xdc] += 1;
    RtMessage responseMsg = RtMessage.fromBytes(responseCopy);

    RoughtimeClient client = new RoughtimeClient(GOOGLE_PUBKEY, NONCE);
    client.processResponse(responseMsg);

    assertFalse("response fails to validate", client.isResponseValid());
    assertThat(client.invalidResponseCause(), instanceOf(SignatureInvalid.class));
    assertThat(
        client.invalidResponseCause().getMessage(),
        containsString("signature on DELE does not match")
    );
  }

  @Test
  public void responseDoesNotIncludeRequestNonce() {
    RoughtimeClient client = new RoughtimeClient(GOOGLE_PUBKEY, new byte[NONCE.length]);
    client.processResponse(RtMessage.fromBytes(RESPONSE));

    assertFalse("response fails to validate", client.isResponseValid());
    assertThat(client.invalidResponseCause(), instanceOf(MerkleTreeInvalid.class));
    assertThat(
        client.invalidResponseCause().getMessage(),
        containsString("nonce not found in response")
    );
  }

  @Test
  public void midpointIsBeforeDelegationBounds() {
    RoughtimeClient client = new RoughtimeClient(GOOGLE_PUBKEY, NONCE);
    client.processResponse(RtMessage.fromBytes(RESPONSE));
    assertTrue(client.isResponseValid());

    // using the lower-level API, feed in an SREP with midpoint of 255
    RtMessage msg = RtMessage.builder()
        .add(RtTag.RADI, new byte[4])
        .add(RtTag.MIDP, new byte[]{(byte) 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00})
        .build();

    RtMessage srepMsg = RtMessage.builder().add(RtTag.SREP, msg).build();

    thrown.expect(MidpointInvalid.class);
    thrown.expectMessage("falls outside delegation bounds: midp=255");
    client.verifyMidpointBounds(srepMsg);
  }

  @Test
  public void midpointIsAfterDelegationBounds() {
    RoughtimeClient client = new RoughtimeClient(GOOGLE_PUBKEY, NONCE);
    client.processResponse(RtMessage.fromBytes(RESPONSE));
    assertTrue(client.isResponseValid());

    // using the lower-level API, feed in an SREP with midpoint of Long.MAX_VALUE
    RtMessage msg = RtMessage.builder()
        .add(RtTag.RADI, new byte[4])
        .add(RtTag.MIDP,
            new byte[]{
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x7f
        })
        .build();

    RtMessage srepMsg = RtMessage.builder().add(RtTag.SREP, msg).build();

    thrown.expect(MidpointInvalid.class);
    thrown.expectMessage("falls outside delegation bounds: midp=" + Long.MAX_VALUE);
    client.verifyMidpointBounds(srepMsg);
  }

  @Test
  public void verifyDeepMerkleTree() {
    // TODO(stuart)
  }

  @Test
  public void invalidPublicKeyGeneratesException() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("invalid public key");

    new RoughtimeClient(new byte[0]);
  }

  @Test
  public void invalidNonceGeneratesException() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("invalid nonce");

    new RoughtimeClient(new byte[RtConstants.PUBKEY_LENGTH], new byte[0]);
  }

  @Test
  public void nonceProvidedAtConstructionIsReturned() {
    RoughtimeClient client = new RoughtimeClient(new byte[RtConstants.PUBKEY_LENGTH], NONCE);
    assertArrayEquals(client.nonce(), NONCE);

    RtMessage request = client.createRequest();
    assertArrayEquals(request.get(RtTag.NONC), NONCE);
  }
}
