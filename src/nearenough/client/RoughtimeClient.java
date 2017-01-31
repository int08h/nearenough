package nearenough.client;

import nearenough.exceptions.DelegationInvalid;
import nearenough.protocol.*;

import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.time.ZonedDateTime;

import static nearenough.protocol.RtConstants.PUBKEY_LENGTH;
import static nearenough.protocol.RtConstants.SIGNATURE_LENGTH;
import static nearenough.util.Preconditions.checkArgument;

public final class RoughtimeClient {

  private final String server;
  private final int port;
  private final RtEd25519.Verifier longTermKeyVerifier;

  /**
   * @param server hostname of the Roughtime server to contact
   * @param port port the Roughtime server is listening on
   * @param publicKey long-term public key of the Roughtime server
   */
  public RoughtimeClient(String server, int port, byte[] publicKey) {
    checkArgument(server != null && !server.isEmpty(), "null or empty server");
    checkArgument(port > 0 && port < 65536, "invalid port");
    checkArgument((publicKey != null) && (publicKey.length == PUBKEY_LENGTH), "invalid public key");

    this.server = server;
    this.port = port;
    try {
      this.longTermKeyVerifier = new RtEd25519.Verifier(publicKey);
    } catch (InvalidKeyException | SignatureException e) {
      throw new IllegalArgumentException("Unable to create delegation verifier from public key", e);
    }
  }

  /**
   * Obtain the online public key, verifying the signature on the DELE message. Throws
   * {@link DelegationInvalid} if the signature is invalid.
   *
   * @param certMsg The {@link RtMessage} from the value of the {@link RtTag#CERT} tag
   *
   * @return Validated online public key (PUBK) from the DELE message
   *
   * @throws DelegationInvalid The signature doesn't verify or is otherwise invalid
   */
  public byte[] getAndValidateOnlineKey(RtMessage certMsg) throws DelegationInvalid {
    byte[] deleBytes = certMsg.get(RtTag.DELE);
    byte[] certSig = certMsg.get(RtTag.SIG);

    validateDelegationSignature(deleBytes, certSig);

    RtMessage deleMsg = RtMessage.fromBytes(deleBytes);
    return deleMsg.get(RtTag.PUBK);
  }

  /**
   * Verifies the long-term key signature of the delegation (DELE) certificate.
   */
  private void validateDelegationSignature(byte[] deleBytes, byte[] certSig) {
    try {
      longTermKeyVerifier.update(RtConstants.CERTIFICATE_CONTEXT);
      longTermKeyVerifier.update(deleBytes);

      if (certSig.length != SIGNATURE_LENGTH) {
        throw new DelegationInvalid("SIG0 is the wrong length: " + certSig.length);
      }

      if (!longTermKeyVerifier.verify(certSig)) {
        throw new DelegationInvalid("signature does not match");
      }
    } catch (SignatureException e) {
      throw new DelegationInvalid(e.getMessage());
    }
  }

  /**
   * Verify that the midpoint is within the delegation time bound; ergo (MINT <= MIDP <= MAXT)
   * TODO(stuart) unsigned integer comparison
   */
  private void validateDelegationBounds(RtMessage deleMsg, ZonedDateTime midp) {
    ZonedDateTime mint = RtWire.timeFromMidpoint(deleMsg.get(RtTag.MINT));
    ZonedDateTime maxt = RtWire.timeFromMidpoint(deleMsg.get(RtTag.MAXT));

    if ((midp.isBefore(mint) || midp.isAfter(maxt)) && !(midp.equals(mint) || midp.equals(maxt))) {
      String message = String.format(
          "Midpoint falls outside delegation bounds: midp=%s, bounds=[%s, %s]", midp, mint, maxt
      );
      throw new DelegationInvalid(message);
    }
  }

}
