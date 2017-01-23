package nearenough.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import nearenough.exceptions.*;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

public class MessageParsingTest {

  @Test
  public void parseEmtpyMessage() {
    // from protocol spec
    ByteBuf validEmptyMessage = makeBuf(
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 // no tags
    );

    RtMessage rtmsg = new RtMessage(validEmptyMessage);
    assertThat(rtmsg.numTags(), equalTo(0));
    assertThat(rtmsg.get(RtTag.CERT), equalTo(null));
  }

  @Test
  public void parseSingleTagMessage() {
    ByteBuf validSingleTag = makeBuf(
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 1 tag
                                                            // no offsets
        (byte) 'T',  (byte) 'R',  (byte) 'E',  (byte) 'C',  // CERT tag
        (byte) 0x50, (byte) 0x50, (byte) 0x50, (byte) 0x50  // value 0x50505050
    );

    RtMessage rtmsg = new RtMessage(validSingleTag);
    assertThat(rtmsg.numTags(), equalTo(1));
    assertArrayEquals(
        rtmsg.get(RtTag.CERT), new byte[]{(byte) 0x50, (byte) 0x50, (byte) 0x50, (byte) 0x50}
    );
  }

  @Test
  public void parseThreeTagsMessage() {
    ByteBuf validThreeTags = makeBuf(
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // 3 tags
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // tag #2 has offset 4
        (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // tag #3 has offset 8
        (byte) 'E',  (byte) 'L',  (byte) 'E',  (byte) 'D',   // DELE
        (byte) 'X',  (byte) 'D',  (byte) 'N',  (byte) 'I',   // INDX
        (byte) 0xff, (byte) 'D',  (byte) 'A',  (byte) 'P',   // PAD
        (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11,  // data for DELE
        (byte) 0x22, (byte) 0x22, (byte) 0x22, (byte) 0x22,  // data for INDX
        (byte) 0x33, (byte) 0x33, (byte) 0x33, (byte) 0x33   // data for PAD
    );

    RtMessage rtmsg = new RtMessage(validThreeTags);
    assertThat(rtmsg.numTags(), equalTo(3));
    assertArrayEquals(
        rtmsg.get(RtTag.DELE), new byte[]{(byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11}
    );
    assertArrayEquals(
        rtmsg.get(RtTag.INDX), new byte[]{(byte) 0x22, (byte) 0x22, (byte) 0x22, (byte) 0x22}
    );
    assertArrayEquals(
        rtmsg.get(RtTag.PAD), new byte[]{(byte) 0x33, (byte) 0x33, (byte) 0x33, (byte) 0x33}
    );
  }

  @Test
  public void parseStringValue() {
    byte[] headerPart = new byte[]{
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 1 tag
                                                            // no offsets
        (byte) 'T',  (byte) 'R',  (byte) 'E',  (byte) 'C'   // CERT
    };

    String origValue = "Roughtime is a project that aims to provide secure time synchronization.";

    ByteBuf buf = Unpooled.buffer();
    buf.writeBytes(headerPart).writeBytes(origValue.getBytes());

    RtMessage msg = new RtMessage(buf);
    byte[] retrievedVal = msg.get(RtTag.CERT);

    assertThat(retrievedVal.length, equalTo(origValue.length()));
    assertThat(new String(retrievedVal), equalTo(origValue));
  }

  @Test
  public void messageLessThan4BytesThrowsException() {
    ByteBuf invalidTooShort = makeBuf((byte) 0x01);

    try {
      //noinspection unused
      RtMessage unused = new RtMessage(invalidTooShort);
      fail("expected an InvalidRoughTimeMessage exception");

    } catch (MessageTooShortException e) {
      assertThat(e.getMessage(), containsString("too short"));
    }
  }

  @Test
  public void messageNotMultipleOf4ThrowsException() {
    ByteBuf invalidNotMultipleOf4 = makeBuf(
        (byte) 0x00,                                        // misalign
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00  // one tag
    );

    try {
      //noinspection unused
      RtMessage unused = new RtMessage(invalidNotMultipleOf4);
      fail("expected an InvalidRoughTimeMessage exception");

    } catch (MessageUnalignedException e) {
      assertThat(e.getMessage(), containsString("not multiple of 4"));
    }
  }


  @Test
  public void messageWithCrazyNumTagsValueThrowsException() {
    //noinspection NumericCastThatLosesPrecision
    ByteBuf invalidNumTagsTooBig = makeBuf(
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xef);

    try {
      //noinspection unused
      RtMessage unused = new RtMessage(invalidNumTagsTooBig);
      fail("expected an InvalidRoughTimeMessage exception");

    } catch (InvalidNumTagsException e) {
      assertThat(e.getMessage(), containsString("invalid num_tags"));
    }
  }

  @Test
  public void messageWithInsufficientPayloadLengthThrowsException() {
    ByteBuf invalidInsufficientPayload = makeBuf(
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 2 tags
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00  // offset for tag 2 is 4
        // rest is missing
    );

    try {
      //noinspection unused
      RtMessage unused = new RtMessage(invalidInsufficientPayload);
      fail("expected an InvalidRoughTimeMessage exception");

    } catch (MessageTooShortException e) {
      assertThat(e.getMessage(), containsString("insufficient length"));
    }
  }

  @Test
  public void offsetNotMultipleOf4ThrowsExceptions() {
    ByteBuf invalidOffsetIsNotMultipleOf4 = makeBuf(
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // 3 tags
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // tag #2 offset 4
        (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // *invalid* tag #3 offset 7
        (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11,  // TAG1
        (byte) 0x22, (byte) 0x22, (byte) 0x22, (byte) 0x22,  // TAG2
        (byte) 0x33, (byte) 0x33, (byte) 0x33, (byte) 0x33   // TAG3
    );

    try {
      //noinspection unused
      RtMessage unused = new RtMessage(invalidOffsetIsNotMultipleOf4);
      fail("expected an InvalidRoughTimeMessage exception");

    } catch (TagOffsetUnalignedException e) {
      assertThat(e.getMessage(), containsString("offset 1 not multiple of 4"));
    }
  }

  @Test
  public void offsetPastEndOfMessageThrowsException() {
    ByteBuf invalidOffsetIsPastEndOfMessage = makeBuf(
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 2 tags
        (byte) 0x04, (byte) 0x03, (byte) 0x02, (byte) 0x01, // offset 0x1020304
        (byte) 0x50, (byte) 0x50, (byte) 0x50, (byte) 0x50, // value 0x50505050
        (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60  // value 0x60606060
    );

    try {
      //noinspection unused
      RtMessage unused = new RtMessage(invalidOffsetIsPastEndOfMessage);
      fail("expected an InvalidRoughTimeMessage exception");

    } catch (TagOffsetOverflowException e) {
      assertThat(e.getMessage(), containsString("offset 0 overflow"));
    }

  }

  @Test
  public void tagsNotIncreasingThrowsException() {
    ByteBuf invalidTagsNotIncreasing = makeBuf(
        (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // 2 tags
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // tag #2 has offset 4
        (byte) 'X',  (byte) 'D',  (byte) 'N',  (byte) 'I',   // INDX
        (byte) 'E',  (byte) 'L',  (byte) 'E',  (byte) 'D',   // DELE *decreased*
        (byte) 0x50, (byte) 0x50, (byte) 0x50, (byte) 0x50,  // value 0x50505050
        (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60   // value 0x60606060
    );

    try {
      //noinspection unused
      RtMessage unused = new RtMessage(invalidTagsNotIncreasing);
      fail("expected an InvalidRoughTimeMessage exception");

    } catch (TagsNotIncreasingException e) {
      assertThat(e.getMessage(), containsString("not strictly increasing"));
    }
  }

  private static ByteBuf makeBuf(byte... bytes) {
    return Unpooled.copiedBuffer(bytes);
  }

}
