package nearenough.protocol;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;

public final class MessageBuilderTest {

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
        .add(RtTag.INDX, value)
        .addPadding(true)
        .build();

    assertThat(msg.numTags(), equalTo(2));
    assertArrayEquals(msg.get(RtTag.INDX), value);

    //  4 numTags
    //  4 single offset
    //  8 two tags (PAD and INDX)
    //  4 INDX value length
    // --
    // 20 bytes
    //
    // 1024 - 20 = 1004 length of PAD value
    assertThat(msg.get(RtTag.PAD).length, equalTo(1004));
  }

  @Test
  public void paddingOverheadAloneReachesMinSize() {
    byte[] value = new byte[1008];

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
}
