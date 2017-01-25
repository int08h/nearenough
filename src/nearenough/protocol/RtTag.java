package nearenough.protocol;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import nearenough.exceptions.InvalidTagException;

/**
 * Roughtime protocol Tags.
 */
public enum RtTag {

  CERT('C', 'E', 'R', 'T'),
  DELE('D', 'E', 'L', 'E'),
  INDX('I', 'N', 'D', 'X'),
  MAXT('M', 'A', 'X', 'T'),
  MIDP('M', 'I', 'D', 'P'),
  MINT('M', 'I', 'N', 'T'),
  NONC('N', 'O', 'N', 'C'),
  PAD('P', 'A', 'D', 0xff),
  PATH('P', 'A', 'T', 'H'),
  PUBK('P', 'U', 'B', 'K'),
  RADI('R', 'A', 'D', 'I'),
  ROOT('R', 'O', 'O', 'T'),
  SIG('S', 'I', 'G', 0x00),
  SREP('S', 'R', 'E', 'P');

  // Primitive collection is used to eliminate boxing from the hot-path of fromUnsignedInt
  private static final IntObjectMap<RtTag> wireToTag = new IntObjectHashMap<>(values().length);

  static {
    for (RtTag rtTag : values()) {
      wireToTag.put(rtTag.wireValue(), rtTag);
    }
  }

  /**
   * Return the {@link RtTag} that corresponds to the provided <em>little-endian</em> uint32 value,
   * or throws {@link InvalidTagException} if no mapping exists.
   *
   * @param tagValue little-endian unsigned 32-bit tag
   *
   * @return the {@link RtTag} that corresponds to the value, or throws {@link InvalidTagException}
   * if no mapping exists.
   */
  public static RtTag fromUnsignedInt(int tagValue) throws InvalidTagException {
    RtTag tag = wireToTag.get(tagValue);

    if (tag != null) {
      return tag;
    } else {
      String exMsg = String.format(
          "'%c%c%c%c' (0x%08x)",
          (char) (tagValue >> 24 & 0xff),
          (char) (tagValue >> 16 & 0xff),
          (char) (tagValue >> 8 & 0xff),
          (char) (tagValue & 0xff),
          tagValue
      );
      throw new InvalidTagException(exMsg);
    }
  }

  private final int wireValue;

  RtTag(int... bytes) {
    // Per the spec, Roughtime tag values are little-endian on-the-wire
    this.wireValue = (bytes[0] | bytes[1] << 8 | bytes[2] << 16 | bytes[3] << 24);
  }

  private int wireValue() {
    return wireValue;
  }
}
