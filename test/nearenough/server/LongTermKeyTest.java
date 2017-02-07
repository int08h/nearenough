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
import static nearenough.util.BytesUtil.hexToBytes;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import nearenough.protocol.RtConstants;
import nearenough.protocol.RtEd25519;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class LongTermKeyTest {

  private static final byte[] SEED = new byte[MIN_SEED_LENGTH];

  static {
    Arrays.fill(SEED, (byte) 'a');
  }

  @Test
  public void successfulLongTermSignatureRoundTrip() throws Exception {
    LongTermKey ltk = new LongTermKey(SEED);
    RtEd25519.Verifier verifier = new RtEd25519.Verifier(ltk.longTermPublicKey());

    byte[] content = "This is a test message".getBytes();

    byte[] signature = ltk.makeLongTermSignature(content);
    verifier.update(content);

    assertTrue(verifier.verify(signature));
  }

  @Test
  public void successfulDelegatedSignatureRoundTrip() throws Exception {
    LongTermKey ltk = new LongTermKey(SEED);
    ltk.newDelegatedKey();

    byte[] content = "800 fill power down".getBytes();

    RtEd25519.Verifier verifier = new RtEd25519.Verifier(ltk.delegatedPublicKey());

    byte[] signature = ltk.makeDelegatedSignature(content);
    verifier.update(content);

    assertTrue(verifier.verify(signature));
  }

}
