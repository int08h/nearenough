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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public final class RtWireTest {

  @Test
  public void sizeOfEmptyMessage() {
    int size = RtWire.computeEncodedSize(Collections.emptyMap());
    // Empty message is 4 bytes, a single num_tags value
    assertThat(size, equalTo(4));
  }

  @Test
  public void sizeOfSingleTagMessage() {
    Map<RtTag, byte[]> map = Collections.singletonMap(RtTag.NONC, new byte[]{1, 2, 3, 4});
    int size = RtWire.computeEncodedSize(map);
    // Single tag message is 4 (num_tags) + 4 (NONC) + 4 (value)
    assertThat(size, equalTo(12));
  }

  @Test
  public void sizeOfTwoTagMessage() {
    Map<RtTag, byte[]> map = new HashMap<>();
    map.put(RtTag.NONC, new byte[4]);
    map.put(RtTag.PAD, new byte[4]);

    int size = RtWire.computeEncodedSize(map);
    // Two tag message
    //   4 num_tags
    //   8 (NONC, PAD) tags
    //   4 PAD offset
    //   8 values
    assertThat(size, equalTo(24));
  }

  @Test
  public void encodeEmptyMessage() {
    RtMessage msg = RtMessage.fromBytes(new byte[]{0, 0, 0, 0});
    ByteBuf onWire = RtWire.toWire(msg);

    // Empty message will be a single uint32
    assertThat(onWire.readableBytes(), equalTo(4));
    assertThat(onWire.readIntLE(), equalTo(0));
  }

  @Test
  public void encodeSingleTagMessage() {
    byte[] value = new byte[64];
    Arrays.fill(value, (byte) 'a');

    RtMessage msg = RtMessage.builder()
        .add(RtTag.CERT, value)
        .build();

    // Wire encoding is 4 (num_tags) + 4 (CERT) + 64 (CERT value)
    ByteBuf onWire = RtWire.toWire(msg);
    assertThat(onWire.readableBytes(), equalTo(72));

    // num_tags
    assertThat(onWire.readIntLE(), equalTo(1));

    // CERT tag
    assertThat(onWire.readInt(), equalTo(RtTag.CERT.wireEncoding()));

    // CERT value
    assertThat(onWire.readableBytes(), equalTo(value.length));
    byte[] readValue = new byte[onWire.readableBytes()];
    onWire.readBytes(readValue);
    assertArrayEquals(value, readValue);

    // Message was completely read
    assertThat(onWire.readableBytes(), equalTo(0));
  }

  @Test
  public void encodeTwoTagMessage() {
    byte[] indxValue = new byte[24];
    byte[] maxtValue = new byte[32];
    Arrays.fill(indxValue, (byte) '1');
    Arrays.fill(maxtValue, (byte) '2');

    RtMessage msg = RtMessage.builder()
        .add(RtTag.INDX, indxValue)
        .add(RtTag.MAXT, maxtValue)
        .build();

    // Wire encoding
    //   4 num_tags
    //   8 (MAXT, INDX) tags
    //   4 INDX offset
    //  24 MAXT value
    //  32 INDX value
    ByteBuf onWire = RtWire.toWire(msg);
    assertThat(onWire.readableBytes(), equalTo(4 + 8 + 4 + 24 + 32));

    // num_tags
    assertThat(onWire.readIntLE(), equalTo(2));

    // Offset past MAXT value to start of INDX value
    assertThat(onWire.readIntLE(), equalTo(maxtValue.length));

    // MAXT tag
    assertThat(onWire.readInt(), equalTo(RtTag.MAXT.wireEncoding()));
    // INDX tag
    assertThat(onWire.readInt(), equalTo(RtTag.INDX.wireEncoding()));

    // MAXT value
    byte[] readTag1Value = new byte[maxtValue.length];
    onWire.readBytes(readTag1Value);
    assertArrayEquals(maxtValue, readTag1Value);

    // INDX value
    byte[] readTag2Value = new byte[indxValue.length];
    onWire.readBytes(readTag2Value);
    assertArrayEquals(indxValue, readTag2Value);

    // Message was completely read
    assertThat(onWire.readableBytes(), equalTo(0));
  }
}
