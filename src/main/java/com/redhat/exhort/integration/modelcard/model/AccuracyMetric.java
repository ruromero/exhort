/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redhat.exhort.integration.modelcard.model;

import com.fasterxml.jackson.databind.JsonNode;

public class AccuracyMetric implements Metric {
  public final String name;
  public final Double accuracy;
  public final Double accuracyStderr;

  public AccuracyMetric(String name, JsonNode results) {
    this.name = name;
    this.accuracy = results.get("acc,none").asDouble();
    this.accuracyStderr = results.get("acc_stderr,none").asDouble();
  }
}
