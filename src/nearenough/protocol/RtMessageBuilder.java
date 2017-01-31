package nearenough.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import static nearenough.protocol.RtConstants.MIN_REQUEST_LENGTH;
import static nearenough.util.Preconditions.checkArgument;
import static nearenough.util.Preconditions.checkNotNull;

public final class RtMessageBuilder {

  private final Map<RtTag, byte[]> map = new TreeMap<>(Comparator.comparing(RtTag::wireEncoding));

  private ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
  private boolean shouldAddPadding = false;

  public RtMessageBuilder add(RtTag tag, byte[] value) {
    checkNotNull(tag, "tag must be non-null");

    map.put(tag, value);
    return this;
  }

  public RtMessageBuilder add(RtTag tag, ByteBuf value) {
    checkNotNull(tag, "tag must be non-null");
    checkNotNull(value, "value must be non-null");

    byte[] bytes = new byte[value.readableBytes()];
    value.readBytes(bytes);
    map.put(tag, bytes);

    return this;
  }

  public RtMessageBuilder add(RtTag tag, RtMessage msg) {
    checkNotNull(tag, "tag must be non-null");
    checkNotNull(msg, "msg must be non-null");

    ByteBuf encoded = RtEncoding.toWire(msg, allocator);
    return add(tag, encoded);
  }

  public RtMessageBuilder addPadding(boolean shouldPad) {
    this.shouldAddPadding = shouldPad;
    return this;
  }

  public RtMessageBuilder allocator(ByteBufAllocator allocator) {
    checkNotNull(allocator, "allocator must be non-null");

    this.allocator = allocator;
    return this;
  }

  public RtMessage build() {
    checkArgument(!map.isEmpty(), "Cannot build an empty RtMessage");

    int encodedSize = RtEncoding.computeEncodedSize(map);

    if (encodedSize < MIN_REQUEST_LENGTH && shouldAddPadding) {
      // Additional bytes added to message size for PAD tag (4) and its offset field (4)
      int padOverhead = 8;
      // The overhead bytes alone may be sufficient to reach the minimum size; it's possible
      // to end up with a zero-length pad value
      int paddingBytes = Math.max(0, (MIN_REQUEST_LENGTH - encodedSize - padOverhead));
      byte[] padding = new byte[paddingBytes];
      map.put(RtTag.PAD, padding);
    }

    return new RtMessage(this);
  }

  /*package*/ Map<RtTag, byte[]> mapping() {
    return map;
  }

}
