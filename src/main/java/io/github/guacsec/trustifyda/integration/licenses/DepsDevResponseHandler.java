/*
 * Copyright 2023-2025 Trustify Dependency Analytics Authors
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

package io.github.guacsec.trustifyda.integration.licenses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.guacsec.trustifyda.api.v5.LicenseCategory;
import io.github.guacsec.trustifyda.api.v5.LicenseIdentifier;
import io.github.guacsec.trustifyda.api.v5.LicenseInfo;
import io.github.guacsec.trustifyda.api.v5.LicenseProviderResult;
import io.github.guacsec.trustifyda.api.v5.LicensesSummary;
import io.github.guacsec.trustifyda.api.v5.PackageLicenseResult;
import io.github.guacsec.trustifyda.api.v5.ProviderStatus;
import io.github.guacsec.trustifyda.integration.backend.BackendUtils;
import io.github.guacsec.trustifyda.model.licenses.LicenseSplitResult;
import io.github.guacsec.trustifyda.monitoring.MonitoringProcessor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DepsDevResponseHandler {

  @ConfigProperty(name = "api.licenses.depsdev.host", defaultValue = "https://api.deps.dev/")
  String depsDevHost;

  private static final String DEPS_DEV_SOURCE = "deps.dev";

  private static final Logger LOGGER = Logger.getLogger(DepsDevResponseHandler.class);

  @Inject ObjectMapper mapper;

  @Inject MonitoringProcessor monitoringProcessor;

  @Inject SpdxLicenseService spdxLicenseService;

  public void handleResponse(Exchange exchange) {
    try {
      var body = exchange.getIn().getBody(String.class);
      if (body == null || body.isBlank()) {
        throw new RuntimeException("Empty response from Deps.dev licenses API");
      }
      var results = new HashMap<String, PackageLicenseResult>();

      var json = mapper.readTree(body);
      if (!json.has("responses") || !json.get("responses").isArray()) {
        throw new RuntimeException("Invalid response format: missing or non-array 'responses'");
      }
      var responses = (ArrayNode) json.get("responses");
      responses.forEach(
          response -> {
            var infos = new ArrayList<LicenseInfo>();
            var request = (ObjectNode) response.get("request");
            var purl = request.get("purl").asText();
            if (response.has("result")) {

              var result = (ObjectNode) response.get("result");
              if (result.has("version")) {
                var version = result.get("version");
                var licensesNode = (ArrayNode) version.get("licenseDetails");
                licensesNode.forEach(
                    licenseNode -> {
                      var spdx = licenseNode.get("spdx").asText();
                      var info =
                          spdxLicenseService.fromLicenseId(spdx, DEPS_DEV_SOURCE, depsDevHost);
                      infos.add(info);
                    });
              }
            }

            var packageResult =
                new PackageLicenseResult().concluded(getConcluded(infos)).evidence(infos);
            results.put(purl, packageResult);
          });
      exchange
          .getMessage()
          .setBody(
              new LicenseSplitResult(
                  new ProviderStatus().ok(true).name(DEPS_DEV_SOURCE).message("OK").code(200),
                  results));
    } catch (JsonProcessingException ex) {
      LOGGER.error("Error parsing JSON response", ex);
      throw new RuntimeException("Error parsing JSON response", ex);
    }
  }

  public LicenseSplitResult aggregateLicenses(
      LicenseSplitResult oldResult, LicenseSplitResult newResult) {
    if (oldResult == null) {
      return newResult;
    }
    Map<String, PackageLicenseResult> mergedPackages = new HashMap<>(oldResult.packages());
    if (newResult != null) {
      mergedPackages.putAll(newResult.packages());
    }
    var status = getWorstStatus(oldResult, newResult);
    return new LicenseSplitResult(status, mergedPackages);
  }

  private ProviderStatus getWorstStatus(
      LicenseSplitResult oldResult, LicenseSplitResult newResult) {
    if (oldResult == null) {
      if (newResult == null) {
        return null;
      }
      return newResult.status();
    }
    if (newResult == null) {
      return oldResult.status();
    }
    // If the old status is not OK, propagate the failure. Otherwise, use the new status.
    return !oldResult.status().getOk() ? oldResult.status() : newResult.status();
  }

  public List<LicenseProviderResult> toResultList(LicenseSplitResult results) {
    var response = new LicenseProviderResult();
    response.packages(results.packages());
    response.summary(buildSummary(results.packages()));
    response.status(results.status());

    return List.of(response);
  }

  public void processResponseError(Exchange exchange) {
    var result = BackendUtils.getErrorMappingFromExchange(exchange);
    var status =
        new ProviderStatus()
            .ok(false)
            .name(DEPS_DEV_SOURCE)
            .message(result.mapping().message())
            .code(result.mapping().statusCode());
    LOGGER.warnf(
        "Unable to process request to Deps.dev licenses API: %s", result.mapping().message());
    monitoringProcessor.processProviderError(exchange, result.exception(), DEPS_DEV_SOURCE);
    exchange.getMessage().setBody(new LicenseSplitResult(status, Collections.emptyMap()));
  }

  private LicensesSummary buildSummary(Map<String, PackageLicenseResult> results) {
    List<LicenseInfo> allInfos =
        results.values().stream()
            .map(PackageLicenseResult::getEvidence)
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .toList();
    Map<String, Long> summaryCount =
        allInfos.stream()
            .filter(Objects::nonNull)
            .flatMap(
                info ->
                    (info.getIdentifiers() != null
                            ? info.getIdentifiers()
                            : List.<LicenseIdentifier>of())
                        .stream())
            .filter(Objects::nonNull)
            .flatMap(
                id -> {
                  List<String> labels = new ArrayList<>();
                  if (Boolean.TRUE.equals(id.getIsDeprecated())) {
                    labels.add("DEPRECATED");
                  }
                  if (Boolean.TRUE.equals(id.getIsOsiApproved())) {
                    labels.add("OSI_APPROVED");
                  }
                  if (Boolean.TRUE.equals(id.getIsFsfLibre())) {
                    labels.add("FSF_LIBRE");
                  }
                  if (id.getCategory() != null) {
                    labels.add(id.getCategory().name());
                  }
                  labels.add("TOTAL");
                  return labels.stream();
                })
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    return new LicensesSummary()
        .total(summaryCount.getOrDefault("TOTAL", 0L).intValue())
        .concluded(results.size())
        .permissive(summaryCount.getOrDefault(LicenseCategory.PERMISSIVE.name(), 0L).intValue())
        .weakCopyleft(
            summaryCount.getOrDefault(LicenseCategory.WEAK_COPYLEFT.name(), 0L).intValue())
        .strongCopyleft(
            summaryCount.getOrDefault(LicenseCategory.STRONG_COPYLEFT.name(), 0L).intValue())
        .unknown(summaryCount.getOrDefault(LicenseCategory.UNKNOWN.name(), 0L).intValue())
        .fsfLibre(summaryCount.getOrDefault("FSF_LIBRE", 0L).intValue())
        .osiApproved(summaryCount.getOrDefault("OSI_APPROVED", 0L).intValue())
        .deprecated(summaryCount.getOrDefault("DEPRECATED", 0L).intValue());
  }

  /** The concluded license is the most permissive license in the list. */
  private LicenseInfo getConcluded(List<LicenseInfo> infos) {
    LicenseInfo concluded = null;
    for (var info : infos) {
      if (concluded == null) {
        concluded = info;
      } else {
        if (!spdxLicenseService.isMorePermissive(concluded.getCategory(), info.getCategory())) {
          concluded = info;
        }
      }
    }
    return concluded;
  }
}
