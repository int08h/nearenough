package nearenough.protocol;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.i2p.crypto.eddsa.Utils.hexToBytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;

public final class RtWireTest {

  @Test
  public void decodeMidpoint() {
    ZonedDateTime expectedMidpoint = ZonedDateTime.parse("2017-01-31T19:41:15.267Z[UTC]");

    byte[] midpointBytes = hexToBytes("90a37a1d69470500");
    ZonedDateTime readMidpoint = RtWire.timeFromMidpoint(midpointBytes);

    assertThat(readMidpoint, equalTo(expectedMidpoint));
  }

  @Test
  public void sizeOfEmptyMessage() {
    int size = RtWire.computeEncodedSize(Collections.emptyMap());
    // Empty message is 4 bytes, a single num_tags value
    assertThat(size, equalTo(4));
  }

  @Test
  public void sizeOfSingleTagMessage() {
    Map<RtTag, byte[]> map = Collections.singletonMap(RtTag.NONC, new byte[]{1, 2, 3, 4});
    int size = RtWire.computeEncodedSize(map);
    // Single tag message is 4 (num_tags) + 4 (NONC) + 4 (value)
    assertThat(size, equalTo(12));
  }

  @Test
  public void sizeOfTwoTagMessage() {
    Map<RtTag, byte[]> map = new HashMap<>();
    map.put(RtTag.NONC, new byte[4]);
    map.put(RtTag.PAD, new byte[4]);

    int size = RtWire.computeEncodedSize(map);
    // Two tag message
    //   4 num_tags
    //   8 (NONC, PAD) tags
    //   4 PAD offset
    //   8 values
    assertThat(size, equalTo(24));
  }

  @Test
  public void encodeEmptyMessage() {
    RtMessage msg = RtMessage.fromBytes(new byte[]{0, 0, 0, 0});
    ByteBuf onWire = RtWire.toWire(msg);

    // Empty message will be a single uint32
    assertThat(onWire.readableBytes(), equalTo(4));
    assertThat(onWire.readIntLE(), equalTo(0));
  }

  @Test
  public void encodeSingleTagMessage() {
    byte[] value = new byte[64];
    Arrays.fill(value, (byte) 'a');

    RtMessage msg = RtMessage.builder()
        .add(RtTag.CERT, value)
        .build();

    // Wire encoding is 4 (num_tags) + 4 (CERT) + 64 (CERT value)
    ByteBuf onWire = RtWire.toWire(msg);
    assertThat(onWire.readableBytes(), equalTo(72));

    // num_tags
    assertThat(onWire.readIntLE(), equalTo(1));

    // CERT tag
    assertThat(onWire.readInt(), equalTo(RtTag.CERT.wireEncoding()));

    // CERT value
    assertThat(onWire.readableBytes(), equalTo(value.length));
    byte[] readValue = new byte[onWire.readableBytes()];
    onWire.readBytes(readValue);
    assertArrayEquals(value, readValue);

    // Message was completely read
    assertThat(onWire.readableBytes(), equalTo(0));
  }

  @Test
  public void encodeTwoTagMessage() {
    byte[] tag1Value = new byte[24];
    byte[] tag2Value = new byte[32];
    Arrays.fill(tag1Value, (byte) '1');
    Arrays.fill(tag2Value, (byte) '2');

    RtMessage msg = RtMessage.builder()
        .add(RtTag.INDX, tag1Value)
        .add(RtTag.MAXT, tag2Value)
        .build();

    // Wire encoding
    //   4 num_tags
    //   8 (INDX, MAXT) tags
    //   4 MAXT offset
    //  24 INDX value
    //  32 MAXT value
    ByteBuf onWire = RtWire.toWire(msg);
    assertThat(onWire.readableBytes(), equalTo(4 + 8 + 4 + 24 + 32));

    // num_tags
    assertThat(onWire.readIntLE(), equalTo(2));

    // Offset past INDX value to start of MAXT value
    assertThat(onWire.readIntLE(), equalTo(tag1Value.length));

    // INDX tag
    assertThat(onWire.readInt(), equalTo(RtTag.INDX.wireEncoding()));
    // MAXT tag
    assertThat(onWire.readInt(), equalTo(RtTag.MAXT.wireEncoding()));

    // INDX value
    byte[] readTag1Value = new byte[tag1Value.length];
    onWire.readBytes(readTag1Value);
    assertArrayEquals(tag1Value, readTag1Value);

    // MAXT value
    byte[] readTag2Value = new byte[tag2Value.length];
    onWire.readBytes(readTag2Value);
    assertArrayEquals(tag2Value, readTag2Value);

    // Message was completely read
    assertThat(onWire.readableBytes(), equalTo(0));
  }
}
