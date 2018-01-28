/*
 * Copyright (c) 2017 int08h LLC. All rights reserved.
 *
 * int08h LLC licenses Nearenough (the "Software") to you under the Apache License, version 2.0
 * (the "License"); you may not use this Software except in compliance with the License. You may
 * obtain a copy of the License from the LICENSE file included with the Software or at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nearenough.util;

/**
 * Utility methods for working with {@code byte[]}. Extracted from Netty and EdDSA-Java.
 */
public final class BytesUtil {

  /**
   * Converts a hex string to bytes.
   *
   * @param s the hex string to be converted.
   * @return the byte[]
   */
  @SuppressWarnings("NumericCastThatLosesPrecision")
  public static byte[] hexToBytes(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
          + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  /**
   * Converts bytes to a hex string.
   *
   * @param raw the byte[] to be converted.
   * @return the hex representation as a string.
   */
  public static String bytesToHex(byte[] raw) {
    if (raw == null) {
      return null;
    }
    final StringBuilder hex = new StringBuilder(2 * raw.length);
    for (final byte b : raw) {
      hex.append(Character.forDigit((b & 0xF0) >> 4, 16))
          .append(Character.forDigit((b & 0x0F), 16));
    }
    return hex.toString();
  }

  public static int getInt(byte[] memory, int index) {
    return (memory[index] & 0xff) << 24 |
        (memory[index + 1] & 0xff) << 16 |
        (memory[index + 2] & 0xff) << 8 |
        memory[index + 3] & 0xff;
  }

  public static int getIntLE(byte[] memory, int index) {
    return memory[index] & 0xff |
        (memory[index + 1] & 0xff) << 8 |
        (memory[index + 2] & 0xff) << 16 |
        (memory[index + 3] & 0xff) << 24;
  }

  public static long getLong(byte[] memory, int index) {
    return (memory[index] & 0xffL) << 56 |
        (memory[index + 1] & 0xffL) << 48 |
        (memory[index + 2] & 0xffL) << 40 |
        (memory[index + 3] & 0xffL) << 32 |
        (memory[index + 4] & 0xffL) << 24 |
        (memory[index + 5] & 0xffL) << 16 |
        (memory[index + 6] & 0xffL) << 8 |
        memory[index + 7] & 0xffL;
  }

  public static long getLongLE(byte[] memory, int index) {
    return memory[index] & 0xffL |
        (memory[index + 1] & 0xffL) << 8 |
        (memory[index + 2] & 0xffL) << 16 |
        (memory[index + 3] & 0xffL) << 24 |
        (memory[index + 4] & 0xffL) << 32 |
        (memory[index + 5] & 0xffL) << 40 |
        (memory[index + 6] & 0xffL) << 48 |
        (memory[index + 7] & 0xffL) << 56;
  }

  public static void setInt(byte[] memory, int index, int value) {
    memory[index] = (byte) (value >>> 24);
    memory[index + 1] = (byte) (value >>> 16);
    memory[index + 2] = (byte) (value >>> 8);
    memory[index + 3] = (byte) value;
  }

  public static void setIntLE(byte[] memory, int index, int value) {
    memory[index] = (byte) value;
    memory[index + 1] = (byte) (value >>> 8);
    memory[index + 2] = (byte) (value >>> 16);
    memory[index + 3] = (byte) (value >>> 24);
  }

  public static void setLong(byte[] memory, int index, long value) {
    memory[index] = (byte) (value >>> 56);
    memory[index + 1] = (byte) (value >>> 48);
    memory[index + 2] = (byte) (value >>> 40);
    memory[index + 3] = (byte) (value >>> 32);
    memory[index + 4] = (byte) (value >>> 24);
    memory[index + 5] = (byte) (value >>> 16);
    memory[index + 6] = (byte) (value >>> 8);
    memory[index + 7] = (byte) value;
  }

  public static void setLongLE(byte[] memory, int index, long value) {
    memory[index] = (byte) value;
    memory[index + 1] = (byte) (value >>> 8);
    memory[index + 2] = (byte) (value >>> 16);
    memory[index + 3] = (byte) (value >>> 24);
    memory[index + 4] = (byte) (value >>> 32);
    memory[index + 5] = (byte) (value >>> 40);
    memory[index + 6] = (byte) (value >>> 48);
    memory[index + 7] = (byte) (value >>> 56);
  }
}
