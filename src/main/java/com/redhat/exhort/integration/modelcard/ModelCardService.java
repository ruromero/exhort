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

package com.redhat.exhort.integration.modelcard;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.camel.Header;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.exhort.integration.modelcard.model.Metric;
import com.redhat.exhort.integration.modelcard.model.ModelCard;
import com.redhat.exhort.integration.modelcard.model.Rank;
import com.redhat.exhort.integration.modelcard.model.Task;

import io.quarkus.runtime.Startup;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@ApplicationScoped
public class ModelCardService {

  private static final Logger LOGGER = Logger.getLogger(ModelCardService.class);
  @Inject S3Client s3Client;

  @Inject ObjectMapper mapper;

  @Inject
  @ConfigProperty(name = "s3.bucket.name")
  String s3BucketName;

  Map<String, TreeMap<Double, String>> rankings = new HashMap<>();

  private static final Set<String> POSITIVE_METRICS =
      Set.of("acc", "acc_norm", "accuracy_amb", "accuracy_disamb");

  public Set<String> listModelCards() {
    var response = s3Client.listObjectsV2(builder -> builder.bucket(s3BucketName).build());
    return response.contents().stream()
        .map(S3Object::key)
        .filter(key -> !key.endsWith("/")) // Filter out folder entries
        .collect(Collectors.toSet());
  }

  @Startup
  void loadRankings() {
    listModelCards().stream()
        .forEach(
            cardName -> {
              var keys = cardName.split("/");
              try {
                var modelCard = getModelCard(keys[0], keys[1]);
                modelCard
                    .tasks()
                    .forEach(
                        (taskName, task) -> {
                          task.metrics()
                              .forEach(
                                  (metricName, metric) -> {
                                    rankings
                                        .computeIfAbsent(
                                            taskName + "/" + metricName, k -> new TreeMap<>())
                                        .put(metric.value(), cardName);
                                  });
                        });
              } catch (IOException e) {
                LOGGER.warn("Unable to load model card for " + cardName, e);
              }
            });
  }

  public ModelCard getModelCard(
      @Header("modelNs") String modelNs, @Header("modelName") String modelName) throws IOException {
    var response =
        s3Client.getObject(
            GetObjectRequest.builder().bucket(s3BucketName).key(modelNs + "/" + modelName).build());
    var modelCard = mapper.readTree(response.readAllBytes());
    var name = modelCard.get("model_name").asText();
    var source = modelCard.get("model_source").asText();
    var results = modelCard.get("results");
    Map<String, Task> tasks = new HashMap<>();
    results
        .fields()
        .forEachRemaining(
            task -> {
              var taskName = task.getKey();
              var taskResults = task.getValue();
              Map<String, Metric> metrics = new HashMap<>();
              taskResults
                  .fields()
                  .forEachRemaining(
                      result -> {
                        var key = result.getKey();
                        if (key.endsWith(",none") && !key.endsWith("stderr,none")) {
                          var metricName = key.substring(0, key.length() - ",none".length());
                          var metricValue = result.getValue().asDouble();
                          var stdErrKey = metricName + "_stderr,none";
                          Double stdErrValue = null;
                          if (taskResults.has(stdErrKey)) {
                            stdErrValue = taskResults.get(stdErrKey).asDouble();
                          }
                          metrics.put(
                              metricName,
                              new Metric(
                                  metricName,
                                  metricValue,
                                  stdErrValue,
                                  getRank(taskName, metricName, metricValue)));
                        }
                      });
              tasks.put(taskName, new Task(taskName, metrics));
            });
    return new ModelCard(name, source, tasks);
  }

  private Rank getRank(String task, String metric, double metricValue) {
    var key = task + "/" + metric;
    if (!rankings.containsKey(key)) {
      return Rank.UNKNOWN;
    }
    var isPositive = POSITIVE_METRICS.contains(metric);
    var taskRank = 1;
    var values = isPositive ? rankings.get(key).keySet() : rankings.get(key).descendingKeySet();

    for (var value : values) {
      var compare = value.compareTo(metricValue);
      if (compare >= 0) {
        return new Rank(taskRank, rankings.get(key).size());
      }
      taskRank++;
    }
    return new Rank(taskRank, rankings.get(key).size());
  }
}
