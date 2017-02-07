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

package nearenough.client;

import static nearenough.protocol.RtConstants.CERTIFICATE_CONTEXT;
import static nearenough.protocol.RtConstants.NONCE_LENGTH;
import static nearenough.protocol.RtConstants.PUBKEY_LENGTH;
import static nearenough.protocol.RtConstants.SIGNATURE_LENGTH;
import static nearenough.protocol.RtConstants.SIGNED_RESPONSE_CONTEXT;
import static nearenough.util.Preconditions.checkArgument;
import static nearenough.util.Preconditions.checkNotNull;
import static nearenough.util.Preconditions.checkState;

import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Random;
import nearenough.exceptions.InvalidRoughTimeMessage;
import nearenough.exceptions.MerkleTreeInvalid;
import nearenough.exceptions.MidpointInvalid;
import nearenough.exceptions.SignatureInvalid;
import nearenough.protocol.RtEd25519;
import nearenough.protocol.RtHashing;
import nearenough.protocol.RtMessage;
import nearenough.protocol.RtTag;
import nearenough.util.BytesUtil;

/**
 * Creates RoughTime client requests and processes server responses.
 * <p>
 * The high-level API consists of:
 * <ul>
 *   <li>{@link #createRequest()} - Constructs a RoughTime client request</li>
 *   <li>{@link #processResponse(RtMessage)} - Validates the server's response</li>
 *   <li>{@link #isResponseValid()} - Indicates if the response passed validation</li>
 *   <li>{@link #midpoint()} and {@link #radius()} - Returns the server's provided time value
 *   (midpoint) and uncertainty (radius) if the response was valid</li>
 * </ul>
 *
 * Typical client code will use {@link RoughtimeClient} similar to this:
 * <pre>
 *   // The RoughTime server's long term public key, must be obtained a priori
 *   byte[] serverLongTermPublicKey = { ... };
 *
 *   // Create client passing the server's long-term key
 *   RoughtimeClient client = new RoughtimeClient(serverLongTermPublicKey);
 *
 *   // Construct a request, then encode it for transmission
 *   RtMessage request = client.createRequest();
 *   ByteBuf encodedRequest = RtWire.toWire(request);
 *
 *   // ... send encodedRequest using NIO, Netty, or some other mechanism ...
 *
 *   RtMessage response = // ... and receive response via NIO, Netty, etc ...
 *
 *   // Process the response
 *   client.processResponse(response);
 *
 *   // Check the result
 *   if (client.isResponseValid()) {
 *     Instant midpoint = Instant.ofEpochMilli(client.midpoint() / 1000L);
 *     System.out.println("midpoint: " + midpoint);
 *   } else {
 *     System.out.println("Invalid response: " + client.invalidResponseCause().getMessage());
 *   }
 * </pre>
 *
 * An expert API is present and should be avoided by most client code.
 */
public final class RoughtimeClient {

  private final byte[] nonce;
  private final byte[] longTermPubKey;

  private long midpoint;
  private int radius;
  private byte[] delegatedKey;
  private long delegationMinT;
  private long delegationMaxT;
  private boolean isResponseValid;
  private InvalidRoughTimeMessage invalidResponseCause;

  /**
   * Creates a new instance using {@link SecureRandom} as the random number generator for generating
   * a nonce.
   *
   * @param publicKey long-term public key of the Roughtime server
   */
  public RoughtimeClient(byte[] publicKey) {
    this(publicKey, new SecureRandom());
  }

  /**
   * Creates a new instance using the provided random number generator to generate a nonce
   *
   * @param publicKey long-term public key of the Roughtime server
   * @param random random number generator used to create the nonce
   */
  public RoughtimeClient(byte[] publicKey, Random random) {
    checkArgument((publicKey != null) && (publicKey.length == PUBKEY_LENGTH), "invalid public key");
    checkNotNull(random, "random");

    this.nonce = new byte[NONCE_LENGTH];
    this.longTermPubKey = Arrays.copyOf(publicKey, publicKey.length);
    random.nextBytes(nonce);
  }

  /**
   * Creates a new instance using the provided nonce. It is the caller's responsibility to ensure
   * the uniqueness of the nonce.
   *
   * @param publicKey long-term public key of the Roughtime server
   * @param nonce nonce to use in request
   */
  public RoughtimeClient(byte[] publicKey, byte[] nonce) {
    checkArgument((publicKey != null) && (publicKey.length == PUBKEY_LENGTH), "invalid public key");
    checkArgument((nonce != null) && (nonce.length == NONCE_LENGTH), "invalid nonce");

    this.nonce = Arrays.copyOf(nonce, nonce.length);
    this.longTermPubKey = Arrays.copyOf(publicKey, publicKey.length);
  }

  /**
   * @return A copy of this instance's nonce value
   */
  public byte[] nonce() {
    return Arrays.copyOf(nonce, nonce.length);
  }

  /**
   * @return The midpoint of the response if the response is valid ({@link #isResponseValid} returns
   * {@code true}), otherwise returns zero (0).
   */
  public long midpoint() {
    return isResponseValid ? midpoint : 0;
  }

  /**
   * @return The radius of the response if the response is valid ({@link #isResponseValid} returns
   * {@code true}), otherwise returns zero (0).
   */
  public int radius() {
    return isResponseValid ? radius : 0;
  }

  /**
   * @return {@code True} if and only if the response was valid in all respects, false otherwise.
   * If {@code False} use {@link #invalidResponseCause()} to obtain the cause of the invalidity.
   */
  public boolean isResponseValid() {
    return isResponseValid;
  }

  /**
   * @return If the response is invalid ({@link #isResponseValid} returns {@code false}), the {@link
   * InvalidRoughTimeMessage exception} associated with the failure of the response to validate, or
   * {@code null} if the message is valid.
   */
  public InvalidRoughTimeMessage invalidResponseCause() {
    return isResponseValid ? null : invalidResponseCause;
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
   * Validate the server's response:
   * <ol>
   *   <li>Verify signature of the delegation (DELE) certificate</li>
   *   <li>Verify top-level signature of the signed-response (SREP) using the delegated key</li>
   *   <li>Verify the request's nonce (NONC) is included in the response's Merkle tree.</li>
   *   <li>Verify the midpoint (MIDP) lies within the delegation time bounds (MINT, MAXT).</li>
   * </ol>
   *
   * Use {@link #isResponseValid()} to check the result of the validation.
   *
   * @param response Response from the Roughtime server
   */
  public void processResponse(RtMessage response) {
    try {
      verifyDelegatedKey(response);
      verifyTopLevelSignature(response);
      verifyNonceIncluded(response);
      verifyMidpointBounds(response);
      isResponseValid = true;
    } catch (InvalidRoughTimeMessage e) {
      isResponseValid = false;
      invalidResponseCause = e;
    }
  }

  /**
   * Expert API
   * <p>
   * Verify the signature on the DELE message. Throws {@link SignatureInvalid} if the signature is
   * invalid.
   *
   * @param responseMsg The response {@link RtMessage} reply from the Roughtime server.
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
   * Expert API
   * <p>
   * Verify the top-level signature (SIG) on the signed-response (SREP) by the delegated key.
   *
   * @param responseMsg The response {@link RtMessage} reply from the Roughtime server.
   * @throws SignatureInvalid The signature doesn't verify or is otherwise invalid
   */
  public void verifyTopLevelSignature(RtMessage responseMsg) throws SignatureInvalid {
    checkState(delegatedKey != null, "verifyDelegatedKey must be called first");

    byte[] srepBytes = responseMsg.get(RtTag.SREP);
    byte[] sigBytes = responseMsg.get(RtTag.SIG);
    validateSignedResponse(srepBytes, sigBytes);
  }

  /**
   * Expert API
   * <p>
   * Verify that this instance's nonce is included in the response's Merkle tree
   *
   * @param responseMsg The response {@link RtMessage} reply from the Roughtime server.
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
   * Expert API
   * <p>
   * Verify that the repsonse's midpoint (MIDP) is within the time bounds of the delegated key.
   *
   * @param responseMsg The response {@link RtMessage} reply from the Roughtime server.
   * @throws MidpointInvalid if the response's midpoint falls outside the DELE time bounds
   */
  public void verifyMidpointBounds(RtMessage responseMsg) throws MidpointInvalid {
    byte[] srepBytes = responseMsg.get(RtTag.SREP);
    RtMessage srepMsg = RtMessage.fromBytes(srepBytes);

    midpoint = BytesUtil.getLongLE(srepMsg.get(RtTag.MIDP), 0);
    radius = BytesUtil.getIntLE(srepMsg.get(RtTag.RADI), 0);
    validateMidpointBounds(midpoint);
  }

  /**
   * Verifies the long-term key signature of the delegation (DELE) certificate.
   */
  private void validateDelegationSignature(byte[] deleBytes, byte[] signature) {
    if (signature.length != SIGNATURE_LENGTH) {
      throw new SignatureInvalid("CERT SIG value is the wrong length: " + signature.length);
    }

    try {
      RtEd25519.Verifier verifier = new RtEd25519.Verifier(longTermPubKey);
      verifier.update(CERTIFICATE_CONTEXT);
      verifier.update(deleBytes);

      if (!verifier.verify(signature)) {
        throw new SignatureInvalid("signature on DELE does not match");
      }
    } catch (InvalidKeyException | SignatureException e) {
      throw new SignatureInvalid(e.getMessage());
    }
  }

  /**
   * Validate the top-level signature (SIG) on the signed-response (SREP) by the delegated key.
   */
  private void validateSignedResponse(byte[] srepBytes, byte[] signature) {
    if (signature.length != SIGNATURE_LENGTH) {
      throw new SignatureInvalid("top-level SIG is the wrong length: " + signature.length);
    }

    try {
      RtEd25519.Verifier verifier = new RtEd25519.Verifier(delegatedKey);
      verifier.update(SIGNED_RESPONSE_CONTEXT);
      verifier.update(srepBytes);

      if (!verifier.verify(signature)) {
        throw new SignatureInvalid("signature on SREP does not match");
      }
    } catch (InvalidKeyException | SignatureException e) {
      throw new SignatureInvalid(e.getMessage());
    }
  }

  /**
   * Verify that this instance's nonce is included in the response's Merkle tree
   */
  private void validateMerkleTree(byte[] root, byte[] path, int index) {
    checkArgument((path.length == 0) || ((path.length % 64) == 0), "path not multiple of 64");

    RtHashing hasher = new RtHashing();

    if (path.length == 0 && index == 0) {
      // Response includes a single nonce
      byte[] expectedRoot = hasher.hashLeaf(nonce);

      if (!Arrays.equals(root, expectedRoot)) {
        throw new MerkleTreeInvalid("nonce not found in response Merkle tree");
      }

    } else if (path.length > 0) {
      // Response includes more than once nonce
      byte[] computedRoot = hasher.hashLeaf(nonce);

      while (path.length > 0) {
        if ((index & 1) == 0) {
          computedRoot = hasher.hashNode(computedRoot, Arrays.copyOfRange(path, 0, 64));
        } else {
          computedRoot = hasher.hashNode(Arrays.copyOfRange(path, 0, 64), computedRoot);
        }

        index >>= 1;
        path = Arrays.copyOfRange(path, 64, path.length);
      }

      if (!Arrays.equals(root, computedRoot)) {
        throw new MerkleTreeInvalid("Merkle tree validation failed");
      }

    } else {
      // Protocol spec violation
      String exMsg = String.format("invalid state: path.length=%d, index=%d", path.length, index);
      throw new MerkleTreeInvalid(exMsg);
    }
  }

  /**
   * Verify that the delegation bounds are sane (MINT < MAXT) and the midpoint is within the
   * delegation time bound (MINT <= MIDP <= MAXT)
   */
  private void validateMidpointBounds(long midp) {
    if (Long.compareUnsigned(delegationMinT, delegationMaxT) >= 0) {
      String message = String.format(
          "Delegation bounds invalid (MINT >= MAXT): bounds=[%s, %s]",
          Long.toUnsignedString(delegationMinT), Long.toUnsignedString(delegationMaxT)
      );
      throw new MidpointInvalid(message);
    }

    boolean isBeforeMinT = Long.compareUnsigned(midp, delegationMinT) < 0;
    boolean isAfterMaxT = Long.compareUnsigned(midp, delegationMaxT) > 0;

    if (isBeforeMinT || isAfterMaxT) {
      String message = String.format(
          "Midpoint falls outside delegation bounds: midp=%s, bounds=[%s, %s]",
          Long.toUnsignedString(midp), Long.toUnsignedString(delegationMinT),
          Long.toUnsignedString(delegationMaxT)
      );
      throw new MidpointInvalid(message);
    }
  }

}
