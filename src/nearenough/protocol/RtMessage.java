package nearenough.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import nearenough.exceptions.InvalidNumTagsException;
import nearenough.exceptions.MessageTooShortException;
import nearenough.exceptions.MessageUnalignedException;
import nearenough.exceptions.TagOffsetOverflowException;
import nearenough.exceptions.TagOffsetUnalignedException;
import nearenough.exceptions.TagsNotIncreasingException;

/**
 * An immutable Roughtime protocol message.
 * <p>
 * Roughtime messages are a map of uint32's to arbitrary byte-strings. See the
 * <a href="https://roughtime.googlesource.com/roughtime">Roughtime project site</a> for
 * the specification and more details.
 */
public final class RtMessage {

  public static RtMessageBuilder builder() {
    return new RtMessageBuilder();
  }

  /**
   * @return A new {@code RtMessage} by parsing the contents of the provided {@code byte[]}. The
   * contents of {@code bytes} must be a well-formed Roughtime message.
   */
  public static RtMessage fromBytes(byte[] bytes) {
    ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(bytes.length);
    buf.writeBytes(bytes);
    return new RtMessage(buf);
  }

  private final int numTags;
  private final Map<RtTag, byte[]> map;

  /**
   * Create a new {@code RtMessage} by parsing the contents of the provided {@code ByteBuf}. The
   * contents of {@code msg} must be a well-formed Roughtime message.
   */
  public RtMessage(ByteBuf msg) {
    checkMessageLength(msg);

    this.numTags = extractNumTags(msg);

    if (numTags == 0) {
      this.map = Collections.emptyMap();
    } else if (numTags == 1) {
      checkSufficientPayload(msg);
      this.map = extractSingleMapping(msg);
    } else {
      checkSufficientPayload(msg);
      this.map = extractMultiMapping(msg);
    }
  }

  RtMessage(RtMessageBuilder builder) {
    this.map = builder.mapping();
    this.numTags = map.size();
  }

  /**
   * @return Number of protocol tags in this message
   */
  public int numTags() {
    return numTags;
  }

  /**
   * @return The byte-string associated with {@code tag}, or {@code null} if no mapping exists.
   */
  public byte[] get(RtTag tag) {
    return map.get(tag);
  }

  /**
   * @return A read-only view of the message's mapping.
   */
  public Map<RtTag, byte[]> mapping() {
    return Collections.unmodifiableMap(map);
  }

  private int extractNumTags(ByteBuf msg) {
    long readNumTags = msg.readUnsignedIntLE();

    // Spec says max # tags can be 2^32-1, but capping at 64k tags for the moment.
    if (readNumTags < 0 || readNumTags > 0xffff) {
      throw new InvalidNumTagsException("invalid num_tags value " + readNumTags);
    }

    return (int) readNumTags;
  }

  private Map<RtTag, byte[]> extractSingleMapping(ByteBuf msg) {
    long uintTag = msg.readUnsignedInt();
    RtTag tag = RtTag.fromUnsignedInt((int) uintTag);

    byte[] value = new byte[msg.readableBytes()];
    msg.readBytes(value);

    return Collections.singletonMap(tag, value);
  }

  private Map<RtTag, byte[]> extractMultiMapping(ByteBuf msg) {
    // extractOffsets will leave the reader index positioned at the first tag
    int[] offsets = extractOffsets(msg);

    int startOfValues = msg.readerIndex() + (4 * numTags);
    Map<RtTag, byte[]> mapping = new LinkedHashMap<>(numTags);
    RtTag prevTag = null;

    for (int i = 0; i < offsets.length; i++) {
      long uintCurrTag = msg.readUnsignedInt();
      RtTag currTag = RtTag.fromUnsignedInt((int) uintCurrTag);

      if ((prevTag != null) && currTag.isLessThan(prevTag)) {
        String exMsg = String.format(
            "tags not strictly increasing: current '%s' (0x%08x), previous '%s' (0x%08x)",
            currTag, currTag.valueLE(), prevTag, prevTag.valueLE()
        );
        throw new TagsNotIncreasingException(exMsg);
      }

      int valueIdx = startOfValues + offsets[i];
      int valueLen = ((i + 1) == offsets.length) ? msg.readableBytes() - offsets[i]
                                                 : offsets[i + 1] - offsets[i];
      byte[] valueBytes = new byte[valueLen];
      msg.getBytes(valueIdx, valueBytes);

      mapping.put(currTag, valueBytes);
      prevTag = currTag;
    }

    return mapping;
  }

  private int[] extractOffsets(ByteBuf msg) {
    int numOffsets = numTags - 1;
    int endOfPayload = msg.readableBytes() - (4 * numOffsets);

    // Offset for every tag, including the value of tag 0 which starts immediately after header.
    int[] offsets = new int[numTags];
    offsets[0] = 0;

    for (int i = 0; i < numOffsets; i++) {
      long offset = msg.readUnsignedIntLE();

      if ((offset % 4) != 0) {
        throw new TagOffsetUnalignedException("offset " + i + " not multiple of 4: " + offset);
      }
      if (offset < 0 || offset > endOfPayload) {
        throw new TagOffsetOverflowException("offset " + i + " overflow: " + offset);
      }

      offsets[i + 1] = (int) offset;
    }

    return offsets;
  }

  private void checkMessageLength(ByteBuf msg) {
    int readableBytes = msg.readableBytes();

    if (readableBytes < 4) {
      throw new MessageTooShortException("too short, <4 bytes total");
    }
    if ((readableBytes % 4) != 0) {
      throw new MessageUnalignedException("message length not multiple of 4: " + readableBytes);
    }
  }

  private void checkSufficientPayload(ByteBuf msg) {
    int expectedReadable = 4 * ((numTags - 1) + numTags);

    if (msg.readableBytes() < expectedReadable) {
      throw new MessageTooShortException(
          "too short, insufficient length for numTags of " + numTags
      );
    }
  }

  @Override
  public String toString() {
    return toString(1);
  }

  public String toString(int indentLevel) {
    char[] indent1 = new char[2 * (indentLevel - 1)];
    char[] indent2 = new char[2 * indentLevel];
    Arrays.fill(indent1, ' ');
    Arrays.fill(indent2, ' ');

    StringBuilder sb = new StringBuilder("RtMessage|").append(numTags).append("|{\n");

    if (map != null) {
      map.forEach(
          (tag, value) -> {
            sb.append(indent2).append(tag.name()).append("(").append(value.length).append(") = ");
            if (tag.isNested()) {
              sb.append(fromBytes(value).toString(indentLevel + 1));
            } else {
              sb.append(ByteBufUtil.hexDump(value)).append('\n');
            }
          }
      );
    }
    sb.append(indent1).append("}\n");
    return sb.toString();
  }
}
