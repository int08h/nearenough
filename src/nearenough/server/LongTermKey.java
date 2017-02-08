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

package nearenough.server;

import static nearenough.protocol.RtConstants.MIN_SEED_LENGTH;
import static nearenough.util.Preconditions.checkArgument;
import static nearenough.util.Preconditions.checkNotNull;
import static nearenough.util.Preconditions.checkState;

import io.netty.buffer.ByteBuf;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import nearenough.protocol.RtEd25519;
import nearenough.protocol.RtMessage;
import nearenough.protocol.RtTag;
import nearenough.protocol.RtWire;
import nearenough.server.clock.ClockSource;
import nearenough.server.clock.SystemClock;
import nearenough.util.BytesUtil;

/**
 * A long-term server Ed25519 key pair. Provides convenient delegated certificate generation and
 * rotation.
 */
public final class LongTermKey {

  private final RtEd25519.Signer longTermKey;
  private final Duration delegationDuration;
  private final Random random;
  private final ClockSource clock;

  private RtEd25519.Signer delegatedKey;
  private long delegationStart;
  private long delegationEnd;

  public LongTermKey(byte[] seed) throws SignatureException, InvalidKeyException {
    this(seed, Duration.ofDays(7), new SystemClock());
  }

  public LongTermKey(byte[] seed, Duration delegationDuration, ClockSource clock)
      throws SignatureException, InvalidKeyException
  {
    checkArgument((seed != null) && (seed.length >= MIN_SEED_LENGTH), "invalid private seed");
    checkArgument(
        (delegationDuration != null) && (delegationDuration.getSeconds() > 60),
        "invalid delegation duration, must be >60 seconds"
    );
    checkNotNull(clock, "clock");

    this.longTermKey = new RtEd25519.Signer(seed);
    this.delegationDuration = delegationDuration;
    this.random = new SecureRandom();
    this.clock = clock;

    newDelegatedKey();
  }

  /**
   * Generate a new delegated key pair
   */
  public void newDelegatedKey() throws SignatureException, InvalidKeyException {
    byte[] nonce = new byte[MIN_SEED_LENGTH];
    random.nextBytes(nonce);
    delegatedKey = new RtEd25519.Signer(nonce);
    delegationStart = clock.now();
    delegationEnd = delegationStart + TimeUnit.SECONDS.toMillis(delegationDuration.getSeconds());
  }

  /**
   * @return A CERT message composed of:
   * <ol>
   *  <li>DELE containing current delegated PUBK along with its MINT and MAXT time bounds</li>
   *  <li>SIG of the DELE message using the long-term key</li>
   * </ol>
   */
  public RtMessage asCertMessage() throws SignatureException {
    byte[] minT = new byte[8];
    BytesUtil.setLongLE(minT, 0, delegationStart);

    byte[] maxT = new byte[8];
    BytesUtil.setLongLE(maxT, 0, delegationEnd);

    RtMessage deleMsg = RtMessage.builder()
        .add(RtTag.PUBK, delegatedPublicKey())
        .add(RtTag.MINT, minT)
        .add(RtTag.MAXT, maxT)
        .build();

    ByteBuf buf = RtWire.toWire(deleMsg);
    byte[] deleBytes = new byte[buf.readableBytes()];
    buf.readBytes(deleBytes);

    byte[] sigBytes = signLongTermKey(deleBytes);

    return RtMessage.builder()
        .add(RtTag.SIG, sigBytes)
        .add(RtTag.DELE, deleBytes)
        .build();
  }

  /**
   * @return The long-term public key.
   */
  public byte[] longTermPublicKey() {
    return longTermKey.getPubKey();
  }

  /**
   * @return The public key of the current delegated key.
   */
  public byte[] delegatedPublicKey() {
    return delegatedKey.getPubKey();
  }

  /**
   * @return Epoch milliseconds starting point of the delegated key's validity
   */
  public long delegationStart() {
    return delegationStart;
  }

  /**
   * @return Epoch milliseconds end point of the delegated key's validity
   */
  public long delegationEnd() {
    return delegationEnd;
  }

  /**
   * Sign the provided content using the long-term key
   *
   * @param content  Content to be signed
   * @return Ed25519 signature of content using the long-term key
   *
   * @throws SignatureException If signing operation fails
   */
  public byte[] signLongTermKey(byte[] content) throws SignatureException {
    longTermKey.update(content);
    return longTermKey.sign();
  }

  /**
   * Sign the provided content using the current delegated key
   *
   * @param content  Content to be signed
   * @return Ed25519 signature of content using the current delegated key
   *
   * @throws SignatureException If signing operation fails
   */
  public byte[] signDelegatedKey(byte[] content) throws SignatureException {
    long now = clock.now();
    checkState(
        (now >= delegationStart) && (now <= delegationEnd),
        "current time is outside delegated key bounds"
    );

    delegatedKey.update(content);
    return delegatedKey.sign();
  }
}
