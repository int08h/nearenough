package nearenough.protocol;

/**
 * Constants and magic numbers of the Roughtime protocol
 */
public final class RtConstants {

  /** Minimum size of a client request */
  public static final int MIN_REQUEST_LENGTH_BYTES = 1024;

  /** Size of the client's nonce */
  public static final int NONCE_LENGTH_BYTES = 64;

  /** Size of server's timestamp value */
  public static final int TIMESTAMP_LENGTH_BYTES = 8;

  /** Size of server's time uncertainty value */
  public static final int RADIUS_LENGTH_BYTES = 4;

  /** Prefixed to the server's certificate before generating or verifying certificate's signature */
  public static final String CERTIFICATE_CONTEXT = "RoughTime v1 delegation signature--\\x00";

  /** Prefixed to the server's response before generating or verifying the server's signature */
  public static final String SIGNED_RESPONSE_CONTEXT = "RoughTime v1 response signature\\x00";

  /** Value prepended to leaves prior to hashing */
  public static final byte TREE_LEAF_TWEAK = 0x00;

  /** Value prepended to nodes prior to hashing */
  public static final byte TREE_NODE_TWEAK = 0x01;
}
