package nearenough.protocol;

import nearenough.exceptions.InvalidTagException;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

  private static final Map<Integer, RtTag> wireToTag = Arrays.stream(values())
      .collect(Collectors.toMap(RtTag::value, Function.identity()));

  public static RtTag fromUnsignedInt(int val) {
    RtTag tag = wireToTag.get(val);

    if (tag != null) {
      return tag;
    } else {
      String exMsg = String.format(
          "'%c%c%c%c' (0x%08x)",
          (char) (val >> 24 & 0xff),
          (char) (val >> 16 & 0xff),
          (char) (val >> 8 & 0xff),
          (char) (val & 0xff),
          val
      );
      throw new InvalidTagException(exMsg);
    }
  }

  private final int intValueLE;

  RtTag(int... bytes) {
    this.intValueLE = (bytes[3] | bytes[2] << 8 | bytes[1] << 16 | bytes[0] << 24);
  }

  /**
   * @return On-the-wire little endian value
   */
  public int value() {
    return intValueLE;
  }
}
