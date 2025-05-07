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
import java.util.concurrent.TimeoutException;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ApplicationScoped
public class ModelCardIntegration extends EndpointRouteBuilder {

  @ConfigProperty(name = "api.s3.timeout", defaultValue = "20s")
  String timeout;

  @Inject ModelCardService modelCardService;

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
        .process(this::getModelCard)
        .marshal().json()
      .endCircuitBreaker()
      .onFallback()
        .process(this::processResponseError);

    from(direct("listModelCards"))
      .routeId("listModelCards")
      .circuitBreaker()
        .faultToleranceConfiguration()
          .timeoutEnabled(true)
          .timeoutDuration(timeout)
        .end()
        .process(this::listModelCards)
        .marshal().json()
      .endCircuitBreaker()
      .onFallback()
        .process(this::processResponseError);
    // fmt:on
  }

  private void getModelCard(Exchange exchange) {

    try {
      var modelCard =
          modelCardService.getModelCard(
              exchange.getIn().getHeader("modelNs", String.class),
              exchange.getIn().getHeader("modelName", String.class));
      exchange.getIn().setBody(modelCard);
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

  private void listModelCards(Exchange exchange) {
    try {
      var modelCards = modelCardService.listModelCards();
      exchange.getIn().setBody(modelCards);
      exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, Response.Status.OK.getStatusCode());
      exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
    } catch (S3Exception ex) {
      exchange
          .getIn()
          .setHeader(
              Exchange.HTTP_RESPONSE_CODE, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      exchange.getIn().setBody("Error listing model cards: " + ex.getMessage());
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
