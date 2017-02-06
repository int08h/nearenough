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

package nearenough.protocol;

import static nearenough.protocol.RtConstants.MIN_SEED_LENGTH;
import static nearenough.protocol.RtConstants.PUBKEY_LENGTH;
import static nearenough.util.Preconditions.checkArgument;

import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

/**
 * Provides Ed25519 signing and verification.
 */
public final class RtEd25519 {

  private static final EdDSAParameterSpec ED25519_SPEC = EdDSANamedCurveTable.getByName(
      EdDSANamedCurveTable.CURVE_ED25519_SHA512
  );

  /**
   * Sign byte-strings using the private key derived from the provided <em>seed</em>.
   */
  public static final class Signer {

    private final Signature signer;
    private final EdDSAPrivateKey privateKey;

    /**
     * Sign byte-strings using the private key derived from the provided <em>seed</em>.
     *
     * @param seedBytes A <em>seed</em> to create a private key. Not the actual private key. The
     * seed will be transformed and expanded into an Ed25519 private key as described in <a
     * href="https://tools.ietf.org/html/rfc8032#page-13">RFC 8032</a>.
     */
    public Signer(byte[] seedBytes) throws InvalidKeyException, SignatureException {
      checkArgument(seedBytes.length >= MIN_SEED_LENGTH, "insufficient private key seed length");

      EdDSAPrivateKeySpec privateSpec = new EdDSAPrivateKeySpec(seedBytes, ED25519_SPEC);
      this.privateKey = new EdDSAPrivateKey(privateSpec);
      this.signer = new EdDSAEngine(RtHashing.newSha512());
      signer.initSign(privateKey);
    }

    /**
     * Updates the data to be signed, using the specified array of bytes.
     *
     * @param content the byte array to use for the update.
     */
    public void update(byte[] content) throws SignatureException {
      signer.update(content);
    }

    /**
     * Returns a byte[64] containing an Ed25519 signature of all the data updated.
     * <p>
     * A call to this method resets this signature object to the state it was in when previously
     * initialized. The object is reset and available to generate another signature, if desired,
     * via new calls to update and sign.
     *
     * @return a byte[64] containing an Ed25519 signature of all the data updated
     */
    public byte[] sign() throws SignatureException {
      return signer.sign();
    }

    /**
     * @return The Ed2519 public key corresponding to the private key derived from the initial seed.
     */
    public byte[] getPubKey() {
      return privateKey.getAbyte();
    }
  }

  /**
   * Verify the Ed25519 signature of byte-strings using the provided public key.
   */
  public static final class Verifier {

    private final Signature verifier;

    /**
     * Verify the Ed25519 signature of byte-strings using the provided public key.
     *
     * @param publicKeyBytes A byte[32] containing the Ed25519 public key to use for verifications.
     */
    public Verifier(byte[] publicKeyBytes) throws InvalidKeyException, SignatureException {
      checkArgument(publicKeyBytes.length == PUBKEY_LENGTH, "incorrect public key size");

      EdDSAPublicKeySpec publicSpec = new EdDSAPublicKeySpec(publicKeyBytes, ED25519_SPEC);
      PublicKey publicKey = new EdDSAPublicKey(publicSpec);

      this.verifier = new EdDSAEngine(RtHashing.newSha512());
      verifier.initVerify(publicKey);
    }

    /**
     * Updates the data to be verified, using the specified array of bytes.
     *
     * @param content the byte array to use for the update.
     */
    public void update(byte[] content) throws SignatureException {
      verifier.update(content);
    }

    /**
     * Verifies the provided Ed25519 signature.
     * <p>
     * A call to this method resets this verification object to the state it was in when previously
     * initialized. The object is reset and available to generate another verification, if desired,
     * via new calls to update and verify.
     *
     * @param signature byte[64] of Ed25519 signature
     * @return True if the signature is valid, false otherwise
     */
    public boolean verify(byte[] signature) throws SignatureException {
      return verifier.verify(signature);
    }
  }
}
