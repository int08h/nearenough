package nearenough.util;

/**
 * Utility methods for working with {@code byte[]}. Extracted from Netty.
 */
public final class BytesUtil {

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
