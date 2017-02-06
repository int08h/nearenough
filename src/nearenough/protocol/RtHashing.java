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

package nearenough.protocol;

import static nearenough.util.Preconditions.checkNotNull;

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
    checkNotNull(leaf, "leaf");

    sha512.update(RtConstants.TREE_LEAF_TWEAK);
    sha512.update(leaf);
    return sha512.digest();
  }

  public byte[] hashNode(byte[] left, byte[] right) {
    checkNotNull(left, "left");
    checkNotNull(right, "right");

    sha512.update(RtConstants.TREE_NODE_TWEAK);
    sha512.update(left);
    sha512.update(right);
    return sha512.digest();
  }

}
