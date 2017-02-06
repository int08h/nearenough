package nearenough.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public final class RtMessageBuilderTest {

  @Test
  public void tagsInBuiltMessageAreInAscendingOrder() {
    RtMessage msg = RtMessage.builder()
        .add(RtTag.ROOT, new byte[64])
        .add(RtTag.RADI, new byte[4])
        .add(RtTag.MIDP, new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00})
        .add(RtTag.SIG, new byte[4])
        .add(RtTag.MAXT, new byte[4])
        .build();

    ArrayList<RtTag> tags = new ArrayList<>(msg.mapping().keySet());

    RtTag prevTag = tags.get(0);
    for (int i = 1; i < tags.size(); i++) {
      RtTag currentTag = tags.get(i);

      String testMsg = String.format(
          "prev = %s %x, current = %s %x",
          prevTag, prevTag.valueLE(), currentTag, currentTag.valueLE()
      );

      assertTrue(testMsg, prevTag.isLessThan(currentTag));
    }
  }

  @Test
  public void addSingleTagNoPadding() {
    byte[] value = {1, 2, 3, 4};

    RtMessage msg = RtMessage.builder()
        .add(RtTag.INDX, value)
        .addPadding(false)
        .build();

    assertThat(msg.numTags(), equalTo(1));
    assertArrayEquals(msg.get(RtTag.INDX), value);
  }

  @Test
  public void addSingleTagWithPadding() {
    byte[] value = {6, 7, 8, 9};

    RtMessage msg = RtMessage.builder()
        .add(RtTag.NONC, value)
        .addPadding(true)
        .build();

    assertThat(msg.numTags(), equalTo(2));
    assertArrayEquals(msg.get(RtTag.NONC), value);

    //  4 numTags
    //  4 single offset
    //  8 two tags (NONC and PAD)
    //  4 NONC value length
    // --
    // 20 bytes
    //
    // 1024 - 20 = 1004 length of PAD value
    assertThat(msg.get(RtTag.PAD).length, equalTo(1004));

    ArrayList<RtTag> tags = new ArrayList<>(msg.mapping().keySet());
    assertThat(tags.get(0), equalTo(RtTag.NONC));
    assertThat(tags.get(1), equalTo(RtTag.PAD));
  }

  @Test
  public void paddingOverheadAloneReachesMinSize() {
    byte[] value = new byte[1008];
    Arrays.fill(value, (byte) 'x');

    RtMessage msg = RtMessage.builder()
        .add(RtTag.SIG, value)
        .addPadding(true)
        .build();

    assertThat(msg.numTags(), equalTo(2));
    assertArrayEquals(msg.get(RtTag.SIG), value);

    //    4 numTags
    //    4 single offset
    //    8 two tags (SIG and PAD)
    // 1008 SIG value length
    //    0 PAD value length
    // --
    // 1024 bytes
    assertThat(msg.get(RtTag.PAD).length, equalTo(0));
  }

  @Test
  public void addValueOverloads() {
    byte[] value1 = new byte[64];
    Arrays.fill(value1, (byte) 'b');

    ByteBuf value2Buf = ByteBufAllocator.DEFAULT.buffer(14);
    byte[] value2 = "This is a test".getBytes();
    value2Buf.writeBytes(value2);

    RtMessage value3Msg = RtMessage.builder().add(RtTag.PAD, new byte[12]).build();
    ByteBuf value3Buf = RtWire.toWire(value3Msg);
    byte[] value3 = new byte[value3Buf.readableBytes()];
    value3Buf.readBytes(value3);

    RtMessage msg = RtMessage.builder()
        .add(RtTag.INDX, value1)
        .add(RtTag.MAXT, value2Buf)
        .add(RtTag.NONC, value3Msg)
        .build();

    assertArrayEquals(msg.get(RtTag.INDX), value1);
    assertArrayEquals(msg.get(RtTag.MAXT), value2);
    assertArrayEquals(msg.get(RtTag.NONC), value3);
  }
}
