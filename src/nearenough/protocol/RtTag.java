package nearenough.protocol;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import nearenough.exceptions.InvalidTagException;

import java.util.EnumSet;
import java.util.Set;

import static nearenough.util.Preconditions.checkNotNull;

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

  // Primitive collection eliminates boxing in fromUnsignedInt which is a hot method
  private static final IntObjectMap<RtTag> ENCODING_TO_TAG = new IntObjectHashMap<>(values().length);

  // Tags for which values are themselves RtMessages
  private static final Set<RtTag> NESTED_TAGS = EnumSet.of(CERT, DELE, SREP);

  static {
    for (RtTag rtTag : values()) {
      ENCODING_TO_TAG.put(rtTag.wireEncoding(), rtTag);
    }
  }

  /**
   * Return the {@link RtTag} that corresponds to the provided uint32 value, or throws
   * {@link InvalidTagException} if no mapping exists.
   *
   * @param tagValue unsigned 32-bit tag
   *
   * @return the {@link RtTag} that corresponds to the value, or throws {@link InvalidTagException}
   * if no mapping exists.
   */
  public static RtTag fromUnsignedInt(int tagValue) throws InvalidTagException {
    RtTag tag = ENCODING_TO_TAG.get(tagValue);

    if (tag != null) {
      return tag;
    } else {
      //noinspection NumericCastThatLosesPrecision
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

  private final int wireEncoding;
  private final int valueLE;

  RtTag(int... bytes) {
    this.wireEncoding = (bytes[3] | bytes[2] << 8 | bytes[1] << 16 | bytes[0] << 24);
    this.valueLE = Integer.reverseBytes(wireEncoding);
  }

  /**
   * @return The on-the-wire representation of this tag.
   */
  public int wireEncoding() {
    return wireEncoding;
  }

  /**
   * @return The little-endian representation of this tag.
   */
  public int valueLE() {
    return valueLE;
  }

  /**
   * @return True if this tag is numerically less than {@code other}, false otherwise.
   */
  public boolean isLessThan(RtTag other) {
    checkNotNull(other, "cannot compare to null RtTag");
    // Enforcement of the "tags in strictly increasing order" rule is done using the
    // little-endian encoding of the ASCII tag value; e.g. 'SIG\x00' is 0x00474953 and
    // 'NONC' is 0x434e4f4e
    return Integer.compareUnsigned(valueLE, other.valueLE) < 0;
  }

  /**
   * @return True if the value of this tag is another {@link RtMessage}, false otherwise.
   */
  public boolean isNested() {
    return NESTED_TAGS.contains(this);
  }
}
