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

import static nearenough.util.BytesUtil.hexToBytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class RtEd25519Test {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void verifyEmptyMessageSignature() throws Exception {
    byte[] pubKey = hexToBytes("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a");
    byte[] signature = hexToBytes(
        "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"
    );

    RtEd25519.Verifier verifier = new RtEd25519.Verifier(pubKey);
    assertThat("empty message didn't validate", verifier.verify(signature));
  }

  @Test
  public void verifyMessageSignature() throws Exception {
    byte[] pubKey = hexToBytes("c0dac102c4533186e25dc43128472353eaabdb878b152aeb8e001f92d90233a7");
    byte[] message = hexToBytes("5f4c8989");
    byte[] signature = hexToBytes(
        "124f6fc6b0d100842769e71bd530664d888df8507df6c56dedfdb509aeb93416e26b918d38aa06305df3095697c18b2aa832eaa52edc0ae49fbae5a85e150c07"
    );

    RtEd25519.Verifier verifier = new RtEd25519.Verifier(pubKey);
    verifier.update(message);
    assertThat("message didn't validate", verifier.verify(signature));
  }

  @Test
  public void signEmptyMessageSignature() throws Exception {
    byte[] seed = hexToBytes("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60");
    byte[] signature = hexToBytes(
        "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"
    );

    RtEd25519.Signer signer = new RtEd25519.Signer(seed);
    assertArrayEquals("empty message signature doesn't match", signature, signer.sign());
  }

  @Test
  public void signMessageSignature() throws Exception {
    byte[] seed = hexToBytes("0d4a05b07352a5436e180356da0ae6efa0345ff7fb1572575772e8005ed978e9");
    byte[] message = hexToBytes("cbc77b");
    byte[] signature = hexToBytes(
        "d9868d52c2bebce5f3fa5a79891970f309cb6591e3e1702a70276fa97c24b3a8e58606c38c9758529da50ee31b8219cba45271c689afa60b0ea26c99db19b00c"
    );

    RtEd25519.Signer signer = new RtEd25519.Signer(seed);
    signer.update(message);
    assertArrayEquals("message signature doesn't match", signature, signer.sign());
  }

  @Test
  public void roundTripSignAndVerify() throws Exception {
    byte[] seed = hexToBytes("334a05b07352a5436e180356da0ae6efa0345ff7fb1572575772e8005ed978e9");
    byte[] message = "Hello world".getBytes();

    RtEd25519.Signer signer = new RtEd25519.Signer(seed);
    signer.update(message);
    byte[] signature = signer.sign();

    byte[] pubKey = signer.getPubKey();
    RtEd25519.Verifier verifier = new RtEd25519.Verifier(pubKey);
    verifier.update(message);

    assertTrue("signature validates", verifier.verify(signature));
  }

  @Test
  public void invalidSeedLengthThrows() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("insufficient private key seed length");

    new RtEd25519.Signer(new byte[1]);
  }

  @Test
  public void invalidPublicKeyLengthThrows() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("incorrect public key size");

    new RtEd25519.Verifier(new byte[1]);
  }

}
