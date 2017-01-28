package nearenough.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.Iterator;
import java.util.Map;

/**
 * Encodes {@link RtMessage Roughtime messages} to their canonical on-the-wire format.
 */
public final class RtEncoding {

  /**
   * Encode the given message using the system default ByteBuf allocator.
   *
   * @return A {@link ByteBuf} containing this message encoded for transmission.
   */
  public static ByteBuf toWire(RtMessage msg) {
    return toWire(msg, ByteBufAllocator.DEFAULT);
  }

  /**
   * Encode the given message using the provided ByteBuf allocator.
   *
   * @return A {@link ByteBuf} containing this message encoded for transmission.
   */
  public static ByteBuf toWire(RtMessage msg, ByteBufAllocator allocator) {
    int encodedSize = computeEncodedSize(msg.mapping());
    ByteBuf buf = allocator.buffer(encodedSize);

    writeNumTags(msg, buf);
    writeOffsets(msg, buf);
    writeTags(msg, buf);
    writeValues(msg, buf);

    assert buf.writableBytes() == 0 : "message was not completely written";

    return buf;
  }

  /*package*/ static int computeEncodedSize(Map<RtTag, byte[]> map) {
    int numTagsSum = 4;
    int tagsSum = 4 * map.size();
    int offsetsSum = 4 * (map.size() - 1);
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
}
