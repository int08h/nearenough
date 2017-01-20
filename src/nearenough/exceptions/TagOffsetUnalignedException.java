package nearenough.exceptions;

/** Tag's value offset is not a multiple of 4 */
public class TagOffsetUnalignedException extends InvalidRoughTimeMessage {

  public TagOffsetUnalignedException(String message) {
    super(message);
  }
}
