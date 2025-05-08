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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.camel.Header;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.exhort.integration.modelcard.model.Guardrail;
import com.redhat.exhort.integration.modelcard.model.Level;
import com.redhat.exhort.integration.modelcard.model.Metric;
import com.redhat.exhort.integration.modelcard.model.ModelCard;
import com.redhat.exhort.integration.modelcard.model.Rank;
import com.redhat.exhort.integration.modelcard.model.Recommendation;
import com.redhat.exhort.integration.modelcard.model.Task;
import com.redhat.exhort.integration.modelcard.model.TaskMapping;
import com.redhat.exhort.integration.modelcard.model.Threshold;

import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@ApplicationScoped
public class ModelCardService {

  private static final Logger LOGGER = Logger.getLogger(ModelCardService.class);
  private static final String THRESHOLDS_FILE = "thresholds.json";
  private static final String GUARDRAILS_FILE = "guardrails.json";
  private static final String TASK_MAPPINGS_FILE = "task-mappings.json";

  @Inject S3Client s3Client;

  @Inject ObjectMapper mapper;

  @Inject
  @ConfigProperty(name = "s3.bucket.name")
  String s3BucketName;

  Map<String, TreeMap<Double, String>> rankings = new HashMap<>();
  List<Threshold> thresholds = new ArrayList<>();
  Map<String, Guardrail> guardrails = new HashMap<>();
  Map<String, TaskMapping> taskMappings = new HashMap<>();

  public Set<String> listModelCards() {
    var response = s3Client.listObjectsV2(builder -> builder.bucket(s3BucketName).build());
    return response.contents().stream()
        .map(S3Object::key)
        // Filter out root configuration files and folder entries
        .filter(key -> key.contains("/") && !key.endsWith("/"))
        .map(key -> key.replace(".json", ""))
        .collect(Collectors.toSet());
  }

  @Startup
  void load() {
    loadRankings();
    reloadConfigFiles();
  }

  private void loadRankings() {
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

  @Scheduled(every = "10m")
  void reloadConfigFiles() {
    loadThresholds();
    loadGuardrails();
    loadTaskMappings();
  }

  private void loadThresholds() {
    try {
      var response =
          s3Client.getObject(
              GetObjectRequest.builder().bucket(s3BucketName).key(THRESHOLDS_FILE).build());
      thresholds =
          mapper.readValue(response.readAllBytes(), new TypeReference<List<Threshold>>() {});
    } catch (IOException e) {
      LOGGER.error("Failed to load " + THRESHOLDS_FILE, e);
    }
  }

  private void loadGuardrails() {
    try {
      var response =
          s3Client.getObject(
              GetObjectRequest.builder().bucket(s3BucketName).key(GUARDRAILS_FILE).build());
      List<Guardrail> guardrailList =
          mapper.readValue(response.readAllBytes(), new TypeReference<List<Guardrail>>() {});
      guardrails =
          guardrailList.stream().collect(Collectors.toMap(Guardrail::name, guardrail -> guardrail));
    } catch (IOException e) {
      LOGGER.error("Failed to load " + GUARDRAILS_FILE, e);
    }
  }

  private void loadTaskMappings() {
    try {
      var response =
          s3Client.getObject(
              GetObjectRequest.builder().bucket(s3BucketName).key(TASK_MAPPINGS_FILE).build());
      List<TaskMapping> mappings =
          mapper.readValue(response.readAllBytes(), new TypeReference<List<TaskMapping>>() {});
      taskMappings =
          mappings.stream().collect(Collectors.toMap(TaskMapping::task, mapping -> mapping));
    } catch (IOException e) {
      LOGGER.error("Failed to load " + TASK_MAPPINGS_FILE, e);
    }
  }

  private void addRecommendation(
      String taskName,
      String metricName,
      Level level,
      Map<String, Recommendation> recommendations) {
    if (level == null || !taskMappings.containsKey(taskName)) {
      return;
    }

    taskMappings.get(taskName).mappings().stream()
        .filter(mapping -> mapping.metrics().contains(metricName))
        .filter(mapping -> (level.category() * 100.0 / level.totalCategories()) < 50)
        .forEach(
            mapping ->
                mapping
                    .categories()
                    .forEach(
                        category ->
                            guardrails.values().stream()
                                .filter(g -> g.categories().contains(category))
                                .forEach(
                                    g -> {
                                      var recommendation = recommendations.get(g.name());
                                      if (recommendation == null) {
                                        recommendation = new Recommendation(new HashSet<>(), g);
                                        recommendations.put(g.name(), recommendation);
                                      }
                                      recommendation.categories().add(category);
                                    })));
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
    var higherIsBetterMetrics = getHigherIsBetterMetrics(modelCard);
    Map<String, Task> tasks = new HashMap<>();
    Map<String, Recommendation> recommendations = new HashMap<>();
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
                          var rank =
                              getRank(taskName, metricName, metricValue, higherIsBetterMetrics);
                          var level = getLevel(taskName, metricName, metricValue);
                          metrics.put(
                              metricName,
                              new Metric(metricName, metricValue, stdErrValue, rank, level));

                          addRecommendation(taskName, metricName, level, recommendations);
                        }
                      });
              tasks.put(taskName, new Task(taskName, metrics));
            });
    return new ModelCard(name, source, tasks, recommendations);
  }

  private Rank getRank(
      String task,
      String metric,
      double metricValue,
      Map<String, Set<String>> higherIsBetterMetrics) {
    var key = task + "/" + metric;
    if (!rankings.containsKey(key)) {
      return Rank.UNKNOWN;
    }
    var taskRank = 1;
    var higherIsBetter =
        higherIsBetterMetrics.containsKey(task) && higherIsBetterMetrics.get(task).contains(metric);
    var values = higherIsBetter ? rankings.get(key).keySet() : rankings.get(key).descendingKeySet();

    for (var value : values) {
      var compare = value.compareTo(metricValue);
      if (compare >= 0) {
        return new Rank(taskRank, rankings.get(key).size());
      }
      taskRank++;
    }
    return new Rank(taskRank, rankings.get(key).size());
  }

  private Level getLevel(String task, String metric, double metricValue) {
    var threshold =
        thresholds.stream()
            .filter(
                t ->
                    (t.task() == null || task.startsWith(t.task()))
                        && t.metrics().stream().anyMatch(m -> metric.equals(m)))
            .findFirst()
            .orElse(null);

    if (threshold == null) {
      return null;
    }

    return threshold.levels().stream()
        .filter(
            level -> {
              if (level.lowThreshold() != null && level.highThreshold() != null) {
                return metricValue >= level.lowThreshold() && metricValue < level.highThreshold();
              } else if (level.lowThreshold() != null) {
                return metricValue >= level.lowThreshold();
              } else if (level.highThreshold() != null) {
                return metricValue < level.highThreshold();
              }
              return false;
            })
        .map(
            level ->
                new Level(
                    level.name(),
                    level.interpretation(),
                    level.lowThreshold(),
                    level.highThreshold(),
                    level.category(),
                    threshold.levels().size()))
        .findFirst()
        .orElse(null);
  }

  private Map<String, Set<String>> getHigherIsBetterMetrics(JsonNode modelCard) {
    var higherIsBetter = new HashMap<String, Set<String>>();
    modelCard
        .get("higher_is_better")
        .fields()
        .forEachRemaining(
            task -> {
              var taskName = task.getKey();
              var taskResults = task.getValue();
              taskResults
                  .fields()
                  .forEachRemaining(
                      metric -> {
                        var metricName = metric.getKey();
                        var isBetter = metric.getValue().asBoolean();
                        if (isBetter) {
                          higherIsBetter
                              .computeIfAbsent(taskName, k -> new HashSet<>())
                              .add(metricName);
                        }
                      });
            });
    return higherIsBetter;
  }
}
