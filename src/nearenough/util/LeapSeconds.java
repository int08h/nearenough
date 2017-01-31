package nearenough.util;

import java.time.Instant;

/**
 * Simplistic accounting of UTC leap-seconds to work around lack of leap-second support in
 * Java's native date/time libraries.
 * <p>
 * Java maintains its timekeeping using a time-scale known as the <em>Java Time-Scale</em>. This
 * time-scale divides all days into exactly 86,400 seconds, thus is ignorant of leap-seconds. See
 * {@link Instant} for the gory details.
 * <p>
 * Roughtime, being leap-second aware, necessitates a way to account for UTC leap-seconds. Herein
 * we cheat and simply track the number of accumulated leap seconds since 1972.
 * <p>
 * An alternate way to do this would be to rely on something like the excellent
 * <a href="http://www.threeten.org/threeten-extra/">ThreeTen-Extra</a> library and its
 * <a href="http://www.threeten.org/threeten-extra/apidocs/org/threeten/extra/scale/UtcInstant.html">
 * UtcInstant</a>.
 */
public final class LeapSeconds {

  /** The number of leap seconds that have occurred in UTC as-of 2017-01-01 */
  public static final int NUM_LEAP_SECONDS_2017 = 28;
}
