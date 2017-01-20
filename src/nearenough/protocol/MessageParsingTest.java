package nearenough.protocol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.nio.ByteOrder;
import nearenough.exceptions.InvalidNumTagsException;
import nearenough.exceptions.InvalidRoughTimeMessage;
import nearenough.exceptions.MessageTooShortException;
import nearenough.exceptions.MessageUnalignedException;
import nearenough.exceptions.TagOffsetOverflowException;
import nearenough.exceptions.TagOffsetUnalignedException;
import nearenough.exceptions.TagsNotIncreasingException;

public class MessageParsingTest {

  private static ByteBuf makeBuf(byte[] bytes) {
    return Unpooled.copiedBuffer(bytes).order(ByteOrder.LITTLE_ENDIAN);
  }

  @Test
  public void parseEmtpyMessage() {
    // from protocol spec
    ByteBuf validEmptyMessage = makeBuf(
        new byte[] {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 // no tags
        }
    );

    RtMessage rtmsg = new RtMessage(validEmptyMessage);
    assertThat(rtmsg.numTags(), equalTo(0));
    assertThat(rtmsg.get(1234L), equalTo(null));
  }

  @Test
  public void parseSingleTagMessage() {
    // from protocol spec
    ByteBuf validSingleTag = makeBuf(
        new byte[] {
            (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 1 tag
            // no offsets
            (byte) 0x04, (byte) 0x03, (byte) 0x02, (byte) 0x01, // tag 0x1020304
            (byte) 0x50, (byte) 0x50, (byte) 0x50, (byte) 0x50  // value 0x50505050
        }
    );

    RtMessage rtmsg = new RtMessage(validSingleTag);
    assertThat(rtmsg.numTags(), equalTo(1));
    // tag 0x1020304, value 0x50505050
    assertArrayEquals(
        rtmsg.get(0x1020304L),
        new byte[]{(byte) 0x50, (byte) 0x50, (byte) 0x50, (byte) 0x50}
    );
  }

  @Test
  public void parseThreeTagsMessage() {
    // from protocol_test.cc
    ByteBuf validThreeTags = makeBuf(
        new byte[] {
            (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // 3 tags
            (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // tag #2 has offset 4
            (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // tag #3 has offset 8
            (byte) 0x54, (byte) 0x41, (byte) 0x47, (byte) 0x31,  // TAG1
            (byte) 0x54, (byte) 0x41, (byte) 0x47, (byte) 0x32,  // TAG2
            (byte) 0x54, (byte) 0x41, (byte) 0x47, (byte) 0x33,  // TAG3
            (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11,  // data for tag #1
            (byte) 0x22, (byte) 0x22, (byte) 0x22, (byte) 0x22,  // data for tag #2
            (byte) 0x33, (byte) 0x33, (byte) 0x33, (byte) 0x33   // data for tag #3
        }
    );

    RtMessage rtmsg = new RtMessage(validThreeTags);
    assertThat(rtmsg.numTags(), equalTo(3));
  }

  @Test
  public void parseStringValue() {
    byte[] headerPart = new byte[] {
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 1 tag
        // no offsets
        (byte) 0x04, (byte) 0x03, (byte) 0x02, (byte) 0x01 // tag 0x1020304
    };
    String origValue = "Roughtime is a project that aims to provide secure time synchronization.";

    ByteBuf buf = Unpooled.buffer().order(ByteOrder.LITTLE_ENDIAN);
    buf.writeBytes(headerPart).writeBytes(origValue.getBytes());

    RtMessage msg = new RtMessage(buf);
    byte[] retrievedVal = msg.get(0x1020304L);

    assertThat(retrievedVal.length, equalTo(origValue.length()));
    assertThat(new String(retrievedVal), equalTo(origValue));
  }


  @Test
  public void messageLessThan4BytesThrowsException() {
    ByteBuf invalidTooShort = makeBuf(new byte[] {(byte) 0x01});

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
        new byte[] {
            (byte) 0x00,                                         // misalign
            (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // one tag
        }
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
        new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xef}
    );

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
        new byte[] {
            (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 2 tags
            (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, // offset for tag 2 is 4
            // rest is missing
        }
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
        new byte[] {
            (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // 3 tags
            (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // tag #2 offset 4
            (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // *invalid* tag #3 offset 7
            (byte) 0x11, (byte) 0x11, (byte) 0x11, (byte) 0x11,  // TAG1
            (byte) 0x22, (byte) 0x22, (byte) 0x22, (byte) 0x22,  // TAG2
            (byte) 0x33, (byte) 0x33, (byte) 0x33, (byte) 0x33,  // TAG3
        }
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
        new byte[] {
            (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 2 tags
            (byte) 0x04, (byte) 0x03, (byte) 0x02, (byte) 0x01, // offset 0x1020304
            (byte) 0x50, (byte) 0x50, (byte) 0x50, (byte) 0x50, // value 0x50505050
            (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60  // value 0x60606060
        }
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
        new byte[] {
            (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // 2 tags
            (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // tag #2 has offset 4
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x31,  // TAG1 31
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x30,  // TAG2 30 *decreased*
            (byte) 0x50, (byte) 0x50, (byte) 0x50, (byte) 0x50,  // value 0x50505050
            (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x60   // value 0x60606060
        }
    );

    try {
      //noinspection unused
      RtMessage unused = new RtMessage(invalidTagsNotIncreasing);
      fail("expected an InvalidRoughTimeMessage exception");

    } catch (TagsNotIncreasingException e) {
      assertThat(e.getMessage(), containsString("not strictly increasing"));
    }
  }

}
