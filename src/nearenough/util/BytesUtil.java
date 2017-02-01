package nearenough.util;

/**
 * Utility methods for working with {@code byte[]}. Extracted from Netty and EdDSA-Java.
 */
public final class BytesUtil {

  /**
   * Converts a hex string to bytes.
   * @param s the hex string to be converted.
   * @return the byte[]
   */
  public static byte[] hexToBytes(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
          + Character.digit(s.charAt(i+1), 16));
    }
    return data;
  }

  /**
   * Converts bytes to a hex string.
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
    return ((long) memory[index] & 0xff) << 56 |
        ((long) memory[index + 1] & 0xff) << 48 |
        ((long) memory[index + 2] & 0xff) << 40 |
        ((long) memory[index + 3] & 0xff) << 32 |
        ((long) memory[index + 4] & 0xff) << 24 |
        ((long) memory[index + 5] & 0xff) << 16 |
        ((long) memory[index + 6] & 0xff) << 8 |
        (long) memory[index + 7] & 0xff;
  }

  public static long getLongLE(byte[] memory, int index) {
    return (long) memory[index] & 0xff |
        ((long) memory[index + 1] & 0xff) << 8 |
        ((long) memory[index + 2] & 0xff) << 16 |
        ((long) memory[index + 3] & 0xff) << 24 |
        ((long) memory[index + 4] & 0xff) << 32 |
        ((long) memory[index + 5] & 0xff) << 40 |
        ((long) memory[index + 6] & 0xff) << 48 |
        ((long) memory[index + 7] & 0xff) << 56;
  }
}
