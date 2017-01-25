package nearenough.protocol;

import nearenough.exceptions.InvalidTagException;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Roughtime protocol Tags
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

  // TODO(stuart) use primitive collection to eliminate boxing
  private static final Map<Integer, RtTag> wireToTag = Arrays.stream(values())
      .collect(Collectors.toMap(RtTag::intValue, Function.identity()));

  /**
   * Return the {@link RtTag} that corresponds to the provided uint32 value, or throw
   * {@link InvalidTagException} if no mapping exists.
   *
   * @param tagValue unsigned 32-bit tag
   *
   * @return the {@link RtTag} that corresponds to the value, or throw
   * {@link InvalidTagException} if no mapping exists.
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

  private final int intValue;

  RtTag(int... bytes) {
    this.intValue = (bytes[3] | bytes[2] << 8 | bytes[1] << 16 | bytes[0] << 24);
  }

  private int intValue() {
    return intValue;
  }
}
