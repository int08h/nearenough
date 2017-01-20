package nearenough.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.util.HashMap;
import java.util.Map;

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

  public static RtTag fromUnsignedInt(long value) {
    if (mapping == null) {
      createMapping();
    }
    return mapping.get(value);
  }

  private static Map<Long, RtTag> mapping;

  private final ByteBuf buf;

  RtTag(int... bytes) {
    this.buf = Unpooled.unreleasableBuffer(ByteBufAllocator.DEFAULT.buffer(4));

    for (int i = 0; i < bytes.length; i++) {
      buf.setByte(i, bytes[i]);
    }
  }

  public ByteBuf buf() {
    return buf;
  }

  public long asUnsignedInt() {
    return buf.getUnsignedInt(0);
  }

  private static void createMapping() {
    mapping = new HashMap<>();
    for (RtTag rtTag : values()) {
      mapping.put(rtTag.asUnsignedInt(), rtTag);
    }
  }

}
