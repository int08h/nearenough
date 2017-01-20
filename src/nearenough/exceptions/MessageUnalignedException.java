package nearenough.exceptions;

/** Message length is not a multiple of 4 */
public class MessageUnalignedException extends InvalidRoughTimeMessage {
  public MessageUnalignedException(String message) {
    super(message);
  }
}
