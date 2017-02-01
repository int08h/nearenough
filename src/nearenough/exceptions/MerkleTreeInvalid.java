package nearenough.exceptions;

/**
 * The response's Merkle tree is invalid.
 */
public class MerkleTreeInvalid extends InvalidRoughTimeMessage {
  public MerkleTreeInvalid(String message) {
    super(message);
  }
}
