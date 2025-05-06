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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.aws2.s3.AWS2S3Constants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.exhort.integration.modelcard.model.AccuracyMetric;
import com.redhat.exhort.integration.modelcard.model.BiasMetric;
import com.redhat.exhort.integration.modelcard.model.Metric;
import com.redhat.exhort.integration.modelcard.model.ModelCard;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ApplicationScoped
public class ModelCardIntegration extends EndpointRouteBuilder {

  @Inject ObjectMapper mapper;

  @ConfigProperty(name = "api.s3.timeout", defaultValue = "20s")
  String timeout;

  @Override
  public void configure() {
    // fmt:off
    from(direct("getModelCard"))
      .routeId("getModelCard")
      .circuitBreaker()
        .faultToleranceConfiguration()
          .timeoutEnabled(true)
          .timeoutDuration(timeout)
        .end()
        .setHeader(AWS2S3Constants.KEY, simple("${header.modelNs}/${header.modelName}"))
        .to("aws2-s3://{{s3.bucket.name}}?operation=getObject&useDefaultCredentialsProvider=true")
        .process(this::convertToModelCard)
        .marshal().json()
      .endCircuitBreaker()
      .onFallback()
        .process(this::processResponseError);
    // fmt:on
  }

  private void convertToModelCard(Exchange exchange) {
    var response = exchange.getIn().getBody(InputStream.class);
    try {
      var modelCard = mapper.readTree(response);
      var name = modelCard.get("model_name").asText();
      var source = modelCard.get("model_source").asText();
      var results = modelCard.get("results");
      Map<String, Metric> metrics = new HashMap<>();
      results
          .fields()
          .forEachRemaining(
              task -> {
                var taskName = task.getKey();
                var taskResults = task.getValue();
                if (taskResults.has("likelihood_diff,none")) {
                  metrics.put(taskName, new BiasMetric(taskName, taskResults));
                } else {
                  metrics.put(taskName, new AccuracyMetric(taskName, taskResults));
                }
              });
      var card = new ModelCard(name, source, metrics);
      exchange.getIn().setBody(card);
      exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, Response.Status.OK.getStatusCode());
      exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      exchange
          .getIn()
          .setHeader(Exchange.HTTP_RESPONSE_CODE, Response.Status.BAD_REQUEST.getStatusCode());
      exchange.getIn().setBody("Invalid model card JSON format: " + ex.getMessage());
    } catch (IOException ex) {
      exchange
          .getIn()
          .setHeader(
              Exchange.HTTP_RESPONSE_CODE, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      exchange.getIn().setBody("Error reading model card: " + ex.getMessage());
    }
  }

  private void processResponseError(Exchange exchange) {
    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
    if (cause == null) {
      exchange
          .getIn()
          .setHeader(
              Exchange.HTTP_RESPONSE_CODE, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      exchange.getIn().setBody("Unknown error occurred while processing model card");
      return;
    }

    Throwable unwrappedCause = cause;
    while (unwrappedCause instanceof org.apache.camel.RuntimeCamelException
        && unwrappedCause.getCause() != null) {
      unwrappedCause = unwrappedCause.getCause();
    }

    if (unwrappedCause instanceof TimeoutException) {
      exchange
          .getIn()
          .setHeader(Exchange.HTTP_RESPONSE_CODE, Response.Status.GATEWAY_TIMEOUT.getStatusCode());
      exchange
          .getIn()
          .setBody("Request timed out while fetching model card: " + unwrappedCause.getMessage());
    } else if (cause instanceof NoSuchKeyException) {
      exchange
          .getIn()
          .setHeader(Exchange.HTTP_RESPONSE_CODE, Response.Status.NOT_FOUND.getStatusCode());
      exchange.getIn().setBody("Model card not found in S3: " + cause.getMessage());
    } else if (cause instanceof S3Exception) {
      exchange
          .getIn()
          .setHeader(
              Exchange.HTTP_RESPONSE_CODE, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      exchange.getIn().setBody("S3 error while fetching model card: " + cause.getMessage());
    } else {
      exchange
          .getIn()
          .setHeader(
              Exchange.HTTP_RESPONSE_CODE, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      exchange.getIn().setBody("Error processing model card: " + cause.getMessage());
    }
  }
}
