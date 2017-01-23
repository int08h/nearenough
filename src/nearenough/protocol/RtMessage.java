package nearenough.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import nearenough.exceptions.*;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * An immutable Roughtime protocol message.
 */
public final class RtMessage {

  private final int numTags;
  private final Map<RtTag, byte[]> map;

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

  public int numTags() {
    return numTags;
  }

  public byte[] get(RtTag tag) {
    return map.get(tag);
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
    long uintTag = msg.readUnsignedIntLE();
    RtTag tag = RtTag.fromUnsignedInt((int) uintTag);

    byte[] value = new byte[msg.readableBytes()];
    msg.readBytes(value);

    return Collections.singletonMap(tag, value);
  }

  private Map<RtTag, byte[]> extractMultiMapping(ByteBuf msg) {
    // This will leave the reader index positioned at the first tag
    int[] offsets = extractOffsets(msg);

    int startOfValues = msg.readerIndex() + (4 * numTags);
    Map<RtTag, byte[]> mapping = new EnumMap<>(RtTag.class);
    RtTag prevTag = null;

    for (int i = 0; i < offsets.length; i++) {
      long uintCurrTag = msg.readUnsignedIntLE();
      RtTag currTag = RtTag.fromUnsignedInt((int) uintCurrTag);

      if ((prevTag != null) && (currTag.ordinal() < prevTag.ordinal())) {
        throw new TagsNotIncreasingException(
            "tags not strictly increasing: prev " + prevTag + ", curr " + currTag
        );
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
    StringBuilder sb = new StringBuilder("RtMessage{\n");
    sb.append(" tags : ").append(numTags).append('\n');
    sb.append(" mapping : {\n");
    if (map != null) {
      map.forEach(
          (tag, value) -> {
            sb.append("    ").append(String.format("0x%08x", tag)).append(" = ");
            sb.append(ByteBufUtil.hexDump(value));
            sb.append('\n');
          }
      );
    }
    sb.append(" }\n");
    sb.append("}");
    return sb.toString();
  }
}
