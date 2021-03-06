/*
 * Copyright 2018, OpenCensus Authors
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

package io.opencensus.contrib.dropwizard5;

import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricName;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Util methods for generating the metric name(unique) and description. */
final class DropWizardUtils {
  private static final String SOURCE = "dropwizard5";
  private static final char DELIMITER = '_';

  /**
   * Returns the metric name.
   *
   * @param name the initial metric name
   * @param type the initial type of the metric.
   * @return a string the unique metric name
   */
  static String generateFullMetricName(String name, String type) {
    return SOURCE + DELIMITER + name + DELIMITER + type;
  }

  /**
   * Returns the metric description.
   *
   * @param metricName the initial metric name
   * @param metric the codahale metric class.
   * @return a String the custom metric description
   */
  static String generateFullMetricDescription(String metricName, Metric metric) {
    return "Collected from "
        + SOURCE
        + " (metric="
        + metricName
        + ", type="
        + metric.getClass().getName()
        + ")";
  }

  static AbstractMap.SimpleImmutableEntry<List<LabelKey>, List<LabelValue>> generateLabels(
      MetricName dropwizardMetric) {
    List<LabelKey> labelKeys = new ArrayList<>();
    List<LabelValue> labelValues = new ArrayList<>();
    for (Map.Entry<String, String> tag : dropwizardMetric.getTags().entrySet()) {
      labelKeys.add(LabelKey.create(tag.getKey(), tag.getKey()));
      labelValues.add(LabelValue.create(tag.getValue()));
    }
    return new AbstractMap.SimpleImmutableEntry<>(labelKeys, labelValues);
  }

  private DropWizardUtils() {}
}
