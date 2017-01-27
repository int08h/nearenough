package nearenough.protocol;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public final class RtMessageBuilder {

  final Map<RtTag, byte[]> map = new TreeMap<>(Comparator.comparing(RtTag::wireEncoding));

}
