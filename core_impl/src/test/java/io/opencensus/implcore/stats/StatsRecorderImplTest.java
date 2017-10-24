/*
 * Copyright 2017, OpenCensus Authors
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

package io.opencensus.implcore.stats;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.grpc.Context;
import io.opencensus.implcore.internal.SimpleEventQueue;
import io.opencensus.stats.Aggregation.Sum;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.StatsComponent;
import io.opencensus.stats.StatsRecord;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.View.AggregationWindow.Cumulative;
import io.opencensus.stats.ViewData;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.Tag;
import io.opencensus.tags.Tag.TagString;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey.TagKeyString;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.TagValue.TagValueString;
import io.opencensus.tags.unsafe.ContextUtils;
import io.opencensus.testing.common.TestClock;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link StatsRecorderImpl}. */
@RunWith(JUnit4.class)
public final class StatsRecorderImplTest {
  private static final TagKeyString KEY = TagKeyString.create("KEY");
  private static final TagValueString VALUE = TagValueString.create("VALUE");
  private static final TagValueString VALUE_2 = TagValueString.create("VALUE_2");
  private static final MeasureDouble MEASURE_DOUBLE =
      MeasureDouble.create("my measurement", "description", "us");
  private static final View.Name VIEW_NAME = View.Name.create("my view");

  private final StatsComponent statsComponent =
      new StatsComponentImplBase(new SimpleEventQueue(), TestClock.create());

  private final ViewManager viewManager = statsComponent.getViewManager();
  private final StatsRecorder statsRecorder = statsComponent.getStatsRecorder();

  @Test
  public void record_CurrentContextNotSet() {
    View view =
        View.create(
            VIEW_NAME,
            "description",
            MEASURE_DOUBLE,
            Sum.create(),
            Arrays.asList(KEY),
            Cumulative.create());
    viewManager.registerView(view);
    statsRecorder.newRecord().put(MEASURE_DOUBLE, 1.0).record();
    ViewData viewData = viewManager.getView(VIEW_NAME);

    // record() should have used the default TagContext, so the tag value should be null.
    assertThat(viewData.getAggregationMap().keySet())
        .containsExactly(Arrays.asList((TagValue) null));
  }

  @Test
  public void record_CurrentContextSet() {
    View view =
        View.create(
            VIEW_NAME,
            "description",
            MEASURE_DOUBLE,
            Sum.create(),
            Arrays.asList(KEY),
            Cumulative.create());
    viewManager.registerView(view);
    Context orig =
        Context.current()
            .withValue(
                ContextUtils.TAG_CONTEXT_KEY, new SimpleTagContext(TagString.create(KEY, VALUE)))
            .attach();
    try {
      statsRecorder.newRecord().put(MEASURE_DOUBLE, 1.0).record();
    } finally {
      Context.current().detach(orig);
    }
    ViewData viewData = viewManager.getView(VIEW_NAME);

    // record() should have used the given TagContext.
    assertThat(viewData.getAggregationMap().keySet()).containsExactly(Arrays.asList(VALUE));
  }

  @Test
  public void recordTwice() {
    View view =
        View.create(
            VIEW_NAME,
            "description",
            MEASURE_DOUBLE,
            Sum.create(),
            Arrays.asList(KEY),
            Cumulative.create());
    viewManager.registerView(view);
    StatsRecord statsRecord = statsRecorder.newRecord().put(MEASURE_DOUBLE, 1.0);
    statsRecord.recordWithExplicitTagContext(new SimpleTagContext(TagString.create(KEY, VALUE)));
    statsRecord.recordWithExplicitTagContext(new SimpleTagContext(TagString.create(KEY, VALUE_2)));
    ViewData viewData = viewManager.getView(VIEW_NAME);

    // There should be two entries.
    StatsTestUtil.assertAggregationMapEquals(
        viewData.getAggregationMap(),
        ImmutableMap.of(
            Arrays.asList(VALUE),
            StatsTestUtil.createAggregationData(Sum.create(), MEASURE_DOUBLE, 1.0),
            Arrays.asList(VALUE_2),
            StatsTestUtil.createAggregationData(Sum.create(), MEASURE_DOUBLE, 1.0)),
        1e-6);
  }

  private static final class SimpleTagContext extends TagContext {
    private final List<Tag> tags;

    SimpleTagContext(Tag... tags) {
      this.tags = Collections.unmodifiableList(Lists.newArrayList(tags));
    }

    @Override
    protected Iterator<Tag> getIterator() {
      return tags.iterator();
    }
  }
}
