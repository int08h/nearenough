package nearenough.client;

import nearenough.exceptions.MidpointInvalid;
import nearenough.exceptions.MerkleTreeInvalid;
import nearenough.exceptions.SignatureInvalid;
import nearenough.protocol.*;
import nearenough.util.BytesUtil;

import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Random;

import static nearenough.protocol.RtConstants.CERTIFICATE_CONTEXT;
import static nearenough.protocol.RtConstants.NONCE_LENGTH;
import static nearenough.protocol.RtConstants.PUBKEY_LENGTH;
import static nearenough.protocol.RtConstants.SIGNATURE_LENGTH;
import static nearenough.protocol.RtConstants.SIGNED_RESPONSE_CONTEXT;
import static nearenough.util.Preconditions.checkArgument;
import static nearenough.util.Preconditions.checkState;

public final class RoughtimeClient {

  private final byte[] nonce;
  private final Random random;
  private final RtEd25519.Verifier longTermKeyVerifier;

  private byte[] delegatedKey;
  private long delegationMinT;
  private long delegationMaxT;

  /**
   * Creates a new instance using {@link SecureRandom} as the random number generator
   *
   * @param publicKey long-term public key of the Roughtime server
   */
  public RoughtimeClient(byte[] publicKey) {
    this(publicKey, new SecureRandom());
  }

  /**
   * Creates a new instance using the provided random number generator
   *
   * @param publicKey long-term public key of the Roughtime server
   * @param random random number generator used to create nonces
   */
  public RoughtimeClient(byte[] publicKey, Random random) {
    checkArgument((publicKey != null) && (publicKey.length == PUBKEY_LENGTH), "invalid public key");

    try {
      this.nonce = new byte[NONCE_LENGTH];
      this.random = random;
      this.longTermKeyVerifier = new RtEd25519.Verifier(publicKey);

      random.nextBytes(nonce);

    } catch (InvalidKeyException | SignatureException e) {
      throw new IllegalArgumentException("Unable to create verifier from public key", e);
    }
  }

  /**
   * @return This instance's nonce value
   */
  public byte[] nonce() {
    return nonce;
  }

  /**
   * Create a client request {@link RtMessage} populated with a unique nonce (NONC tag). The
   * returned message is ready to be sent to a Roughtime server.
   *
   * @return a {@link RtMessage} populated with a unique nonce (NONC tag)
   */
  public RtMessage createRequest() {
    return RtMessage.builder()
        .addPadding(true)
        .add(RtTag.NONC, nonce)
        .build();
  }

  /**
   * Verify the signature on the DELE message. Throws {@link SignatureInvalid} if the signature is
   * invalid.
   *
   * @param responseMsg The response {@link RtMessage} reply from the Roughtime server.
   *
   * @throws SignatureInvalid The signature doesn't verify or is otherwise invalid
   */
  public void verifyDelegatedKey(RtMessage responseMsg) throws SignatureInvalid {
    RtMessage certMsg = RtMessage.fromBytes(responseMsg.get(RtTag.CERT));
    byte[] deleBytes = certMsg.get(RtTag.DELE);
    byte[] certSig = certMsg.get(RtTag.SIG);

    validateDelegationSignature(deleBytes, certSig);

    RtMessage deleMsg = RtMessage.fromBytes(deleBytes);
    delegatedKey = deleMsg.get(RtTag.PUBK);
    delegationMinT = BytesUtil.getLongLE(deleMsg.get(RtTag.MINT), 0);
    delegationMaxT = BytesUtil.getLongLE(deleMsg.get(RtTag.MAXT), 0);
  }

  /**
   * Verify the top-level signature (SIG) on the signed-response (SREP) by the delegated key.
   *
   * @param responseMsg The response {@link RtMessage} reply from the Roughtime server.
   *
   * @throws SignatureInvalid The signature doesn't verify or is otherwise invalid
   */
  public void verifyTopLevelSignature(RtMessage responseMsg) throws SignatureInvalid {
    checkState(delegatedKey != null, "verifyDelegatedKey must be called first");

    byte[] srepBytes = responseMsg.get(RtTag.SREP);
    byte[] sigBytes = responseMsg.get(RtTag.SIG);
    validateSignedResponse(srepBytes, sigBytes);
  }

  /**
   * Verify that this instance's nonce is included in the response's Merkle tree
   *
   * @param responseMsg The response {@link RtMessage} reply from the Roughtime server.
   *
   * @throws MerkleTreeInvalid if the repsonse does not include this instance's nonce or the
   * response's Merkle tree is invalid in some other way
   */
  public void verifyNonceIncluded(RtMessage responseMsg) throws MerkleTreeInvalid {
    byte[] srepBytes = responseMsg.get(RtTag.SREP);
    RtMessage srepMsg = RtMessage.fromBytes(srepBytes);

    byte[] root = srepMsg.get(RtTag.ROOT);
    byte[] path = responseMsg.get(RtTag.PATH);
    int index = BytesUtil.getIntLE(responseMsg.get(RtTag.INDX), 0);

    validateMerkleTree(root, path, index);
  }

  /**
   * Verify that the repsonse's midpoint (MIDP) is within the time bounds of the delegated key.
   *
   * @param responseMsg The response {@link RtMessage} reply from the Roughtime server.
   *
   * @throws MidpointInvalid if the response's midpoint falls outside the DELE time bounds
   */
  public void verifyMidpointBounds(RtMessage responseMsg) throws MidpointInvalid {
    byte[] srepBytes = responseMsg.get(RtTag.SREP);
    RtMessage srepMsg = RtMessage.fromBytes(srepBytes);

    long midpoint = BytesUtil.getLongLE(srepMsg.get(RtTag.MIDP), 0);
    validateMidpointBounds(midpoint);
  }

  /**
   * Verifies the long-term key signature of the delegation (DELE) certificate.
   */
  private void validateDelegationSignature(byte[] deleBytes, byte[] signature) {
    if (signature.length != SIGNATURE_LENGTH) {
      throw new SignatureInvalid("SIG is the wrong length: " + signature.length);
    }

    try {
      longTermKeyVerifier.update(CERTIFICATE_CONTEXT);
      longTermKeyVerifier.update(deleBytes);

      if (!longTermKeyVerifier.verify(signature)) {
        throw new SignatureInvalid("signature does not match");
      }
    } catch (SignatureException e) {
      throw new SignatureInvalid(e.getMessage());
    }
  }

  /**
   * Validate the top-level signature (SIG) on the signed-response (SREP) by the delegated key.
   */
  private void validateSignedResponse(byte[] srepBytes, byte[] signature) {
    if (signature.length != SIGNATURE_LENGTH) {
      throw new SignatureInvalid("SIG is the wrong length: " + signature.length);
    }

    try {
      RtEd25519.Verifier verifier = new RtEd25519.Verifier(delegatedKey);
      verifier.update(SIGNED_RESPONSE_CONTEXT);
      verifier.update(srepBytes);

      if (!verifier.verify(signature)) {
        throw new SignatureInvalid("delegated signature does not match");
      }
    } catch (InvalidKeyException | SignatureException e) {
      throw new SignatureInvalid(e.getMessage());
    }
  }

  /**
   * Verify that this instance's nonce is included in the response's Merkle tree
   */
  private void validateMerkleTree(byte[] root, byte[] path, int index) {
    RtHashing hasher = new RtHashing();

    if (path.length == 0 && index == 0) {
      // Response includes a single nonce
      byte[] expectedRoot = hasher.hashLeaf(nonce);

      if (!Arrays.equals(root, expectedRoot)) {
        throw new MerkleTreeInvalid("nonce not found in response Merkle tree");
      }

    } else if (path.length > 0 && index > 0){
      // Response includes more than once nonce
      // TODO(stuart) full Merkle tree validation

    } else {
      // Protocol spec violation
      String exMsg = String.format("invalid state: path.length=%d, index=%d", path.length, index);
      throw new MerkleTreeInvalid(exMsg);
    }
  }

  /**
   * Verify that the midpoint is within the delegation time bound; ergo (MINT <= MIDP <= MAXT)
   */
  private void validateMidpointBounds(long midp) {
    boolean isBeforeMinT = Long.compareUnsigned(midp, delegationMinT) < 0;
    boolean isAfterMaxT = Long.compareUnsigned(midp, delegationMaxT) > 0;

    if (isBeforeMinT || isAfterMaxT) {
      ZonedDateTime zdtMidp = ZonedDateTime.from(Instant.ofEpochMilli(midp / 1000L));
      ZonedDateTime zdtMinT = ZonedDateTime.from(Instant.ofEpochMilli(delegationMinT / 1000L));
      ZonedDateTime zdtMaxT = ZonedDateTime.from(Instant.ofEpochMilli(delegationMaxT / 1000L));

      String message = String.format(
          "Midpoint falls outside delegation bounds: midp=%s, bounds=[%s, %s]",
          zdtMidp, zdtMinT, zdtMaxT
      );

      throw new MidpointInvalid(message);
    }
  }

}
