package nearenough.protocol;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RtHashing {

  /**
   * @return A new SHA-512 instance, re-throwing any checked exception as un-checked
   */
  public static MessageDigest newSha512() {
    try {
      return MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private final MessageDigest sha512;

  public RtHashing() {
    this.sha512 = newSha512();
  }

  public byte[] hashLeaf(byte[] leaf) {
    sha512.reset();
    sha512.update(RtConstants.TREE_LEAF_TWEAK);
    sha512.update(leaf);
    return sha512.digest();
  }

  public byte[] hashNode(byte[] left, byte[] right) {
    sha512.reset();
    sha512.update(RtConstants.TREE_NODE_TWEAK);
    sha512.update(left);
    sha512.update(right);
    return sha512.digest();
  }

}
