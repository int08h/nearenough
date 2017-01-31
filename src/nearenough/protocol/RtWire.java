package nearenough.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import nearenough.util.BytesUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Map;

import static nearenough.protocol.RtConstants.TIMESTAMP_LENGTH;
import static nearenough.util.Preconditions.*;

/**
 * Encodes/decodes {@link RtMessage Roughtime messages} and fields to/from their on-the-wire format.
 */
public final class RtWire {

  /**
   * Convert the on-the-wire UTC midpoint value to a {@link ZonedDateTime} in UTC.
   *
   * @param midpBytes The MIDP value
   *
   * @return A {@link ZonedDateTime} that corresponds to the UTC time from the provided MIDP.
   */
  public static ZonedDateTime timeFromMidpoint(byte[] midpBytes) {
    checkArgument(midpBytes.length == TIMESTAMP_LENGTH, "invalid MIDP length %s", midpBytes.length);

    long midp = BytesUtil.getLongLE(midpBytes, 0);
    Instant midpInst = Instant.ofEpochMilli(midp / 1000);
    return ZonedDateTime.ofInstant(midpInst, ZoneId.of("UTC"));
  }

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
  private RtWire() {}

}
