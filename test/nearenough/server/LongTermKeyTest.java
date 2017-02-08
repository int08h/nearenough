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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import nearenough.protocol.RtEd25519;
import nearenough.protocol.RtMessage;
import nearenough.protocol.RtTag;
import nearenough.server.clock.TestingClock;
import nearenough.util.BytesUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class LongTermKeyTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final byte[] SEED = new byte[MIN_SEED_LENGTH];

  static {
    Arrays.fill(SEED, (byte) 'a');
  }

  @Test
  public void successfulLongTermSignatureRoundTrip() throws Exception {
    LongTermKey ltk = new LongTermKey(SEED);
    RtEd25519.Verifier verifier = new RtEd25519.Verifier(ltk.longTermPublicKey());

    byte[] content = "This is a test message".getBytes();

    byte[] signature = ltk.signLongTermKey(content);
    verifier.update(content);

    assertTrue(verifier.verify(signature));
  }

  @Test
  public void successfulDelegatedSignatureRoundTrip() throws Exception {
    LongTermKey ltk = new LongTermKey(SEED);

    for (int i = 0; i < 10; i++) {
      ltk.newDelegatedKey();

      byte[] content = (i + " bottles of beer").getBytes();

      RtEd25519.Verifier verifier = new RtEd25519.Verifier(ltk.delegatedPublicKey());

      byte[] signature = ltk.signDelegatedKey(content);
      verifier.update(content);

      assertTrue(verifier.verify(signature));
    }
  }

  @Test
  public void signingPriorToDelegationBoundsThrows() throws Exception {
    TestingClock clock = new TestingClock();
    LongTermKey key = new LongTermKey(SEED, Duration.ofDays(10), clock);

    key.newDelegatedKey();

    clock.decrement(5, TimeUnit.MINUTES);

    assertThat(clock.now(), lessThan(key.delegationStart()));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("outside delegated key bounds");
    key.signDelegatedKey(new byte[1]);
  }

  @Test
  public void signingAfterDelegationBoundsThrows() throws Exception {
    TestingClock clock = new TestingClock();
    LongTermKey key = new LongTermKey(SEED, Duration.ofDays(10), clock);

    key.newDelegatedKey();

    clock.advance(15, TimeUnit.DAYS);

    assertThat(clock.now(), greaterThan(key.delegationStart()));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("outside delegated key bounds");
    key.signDelegatedKey(new byte[1]);
  }

  @Test
  public void certMessageIsValid() throws Exception {
    TestingClock clock = new TestingClock();
    LongTermKey key = new LongTermKey(SEED, Duration.ofDays(1), clock);

    RtMessage cert = key.asCertMessage();
    assertThat(cert.numTags(), equalTo(2));

    // extract the DELE message
    RtMessage dele = RtMessage.fromBytes(cert.get(RtTag.DELE));
    assertThat(dele.numTags(), equalTo(3));

    // PUBK
    assertArrayEquals(dele.get(RtTag.PUBK), key.delegatedPublicKey());

    // MINT
    long minT = BytesUtil.getLongLE(dele.get(RtTag.MINT), 0);
    assertThat(minT, equalTo(key.delegationStart()));

    // MAXT
    long maxT = BytesUtil.getLongLE(dele.get(RtTag.MAXT), 0);
    assertThat(maxT, equalTo(key.delegationEnd()));

    // SIG on DELE
    RtEd25519.Verifier longTermVerify = new RtEd25519.Verifier(key.longTermPublicKey());
    longTermVerify.update(cert.get(RtTag.DELE));
    assertTrue(longTermVerify.verify(cert.get(RtTag.SIG)));
  }
}
