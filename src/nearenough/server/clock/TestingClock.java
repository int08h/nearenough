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

package nearenough.server.clock;

import java.util.concurrent.TimeUnit;

/**
 * A {@link ClockSource} to be used for tests
 */
public final class TestingClock implements ClockSource {

  private long now;

  public TestingClock() {
    this.now = System.currentTimeMillis();
  }

  public TestingClock(long now) {
    this.now = now;
  }

  @Override
  public long now() {
    return now;
  }

  public void setNow(long now) {
    this.now = now;
  }

  public void advance(int amount, TimeUnit unit) {
    this.now += unit.toMillis(amount);
  }

  public void decrement(int amount, TimeUnit unit) {
    this.now -= unit.toMillis(amount);
  }
}
