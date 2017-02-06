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

import static nearenough.util.Preconditions.checkNotNull;
import static nearenough.util.Preconditions.checkState;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.util.Iterator;
import java.util.Map;

/**
 * Encodes/decodes {@link RtMessage Roughtime messages} and fields to/from their on-the-wire format.
 */
public final class RtWire {

  /**
   * Encode the given message for network transmission using the system default ByteBuf allocator.
   *
   * @return A {@link ByteBuf} containing this message encoded for transmission.
   */
  public static ByteBuf toWire(RtMessage msg) {
    return toWire(msg, ByteBufAllocator.DEFAULT);
  }

  /**
   * Encode the given message for network transmission using the provided ByteBuf allocator.
   *
   * @return A {@link ByteBuf} containing this message encoded for transmission.
   */
  public static ByteBuf toWire(RtMessage msg, ByteBufAllocator allocator) {
    checkNotNull(msg, "msg");
    checkNotNull(allocator, "allocator");

    int encodedSize = computeEncodedSize(msg.mapping());
    ByteBuf buf = allocator.buffer(encodedSize);
    checkState(buf.writableBytes() >= 4, "nonsensical output buf size %s", buf.writableBytes());

    writeNumTags(msg, buf);
    writeOffsets(msg, buf);
    writeTags(msg, buf);
    writeValues(msg, buf);

    // Output buffer should have been completely used
    checkState(buf.writableBytes() == 0, "message was not completely written");

    return buf;
  }

  /**
   * @return The size in bytes of the on-the-wire encoding of the provided {@code map}.
   */
  /*package*/ static int computeEncodedSize(Map<RtTag, byte[]> map) {
    int numTagsSum = 4;
    int tagsSum = 4 * map.size();
    int offsetsSum = map.size() < 2 ? 0 : (4 * (map.size() - 1));
    int valuesSum = map.values().stream().mapToInt(value -> value.length).sum();

    return numTagsSum + tagsSum + offsetsSum + valuesSum;
  }

  private static void writeNumTags(RtMessage msg, ByteBuf buf) {
    buf.writeIntLE(msg.numTags());
  }

  private static void writeOffsets(RtMessage msg, ByteBuf buf) {
    if (msg.numTags() < 2) {
      return;
    }

    Iterator<byte[]> iter = msg.mapping().values().iterator();
    int offsetSum = iter.next().length;

    while (iter.hasNext()) {
      buf.writeIntLE(offsetSum);
      offsetSum += iter.next().length;
    }

    checkState(offsetSum >= 0);
  }

  private static void writeTags(RtMessage msg, ByteBuf buf) {
    for (RtTag rtTag : msg.mapping().keySet()) {
      buf.writeInt(rtTag.wireEncoding());
    }
  }

  private static void writeValues(RtMessage msg, ByteBuf buf) {
    for (byte[] bytes : msg.mapping().values()) {
      buf.writeBytes(bytes);
    }
  }

  // Utility class
  private RtWire() {
  }

}
