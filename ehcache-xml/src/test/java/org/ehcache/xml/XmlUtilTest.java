/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehcache.xml;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static java.util.Collections.reverse;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.ehcache.xml.XmlUtil.mergePartialOrderings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.fail;

public class XmlUtilTest {

  @Test
  public void testPartialOrderingLogic() {
    randomly(random -> {
      List<Integer> fullOrdering = unmodifiableList(random.ints().distinct().limit(random.nextInt(100)).boxed().collect(toList()));

      int orderings = random.nextInt(10);

      Collection<List<Integer>> partialOrderings = new ArrayList<>();
      for (int i = 0; i < orderings; i++) {
        List<Integer> ordering = new ArrayList<>();
        for (Integer value : fullOrdering) {
          if (random.nextFloat() < 0.10) {
            ordering.add(value);
          }
        }
        partialOrderings.add(unmodifiableList(ordering));
      }

      List<Integer> reconstructed = mergePartialOrderings(partialOrderings);

      assertThat(reconstructed, allOf(partialOrderings.stream()
        .filter(o -> !o.isEmpty())
        .map(o -> containsInRelativeOrder(o.toArray()))
        .collect(toList())));
    });
  }

  private static void randomly(Consumer<Random> task) {
    long seed = System.nanoTime();
    try {
      task.accept(new Random(seed));
    } catch (Throwable t) {
      throw new AssertionError("Failure with random seed: " + seed, t);
    }
  }

  @Test
  public void testPartialOrderingLogicOnInconsistentOrderings() {
    randomly(random -> {
      List<Integer> fullOrdering = unmodifiableList(random.ints().distinct().limit(random.nextInt(100)).boxed().collect(toList()));

      int orderings = 1 + random.nextInt(9);

      Collection<List<Integer>> partialOrderings = new ArrayList<>();
      for (int i = 0; i < orderings; i++) {
        List<Integer> ordering = new ArrayList<>();
        for (Integer value : fullOrdering) {
          if (random.nextFloat() < 0.10) {
            ordering.add(value);
          }
        }
        partialOrderings.add(unmodifiableList(ordering));
      }

      List<Integer> conflictedOrdering = new ArrayList<>(partialOrderings.stream().findAny().orElseThrow(AssertionError::new));
      reverse(conflictedOrdering);
      partialOrderings.add(unmodifiableList(conflictedOrdering));

      try {
        mergePartialOrderings(partialOrderings);
        fail("Expected IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        assertThat(e.getMessage(), containsString("Incompatible partial orderings"));
      }
    });
  }

}
