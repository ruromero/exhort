/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package com.redhat.exhort.integration.providers.tpa;

import java.time.Duration;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.exhort.integration.Constants;
import com.redhat.exhort.integration.providers.VulnerabilityProvider;

import io.quarkus.oidc.client.OidcClients;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class TpaIntegration extends EndpointRouteBuilder {

  private static final String TPA_CLIENT_TENANT = "tpa";
  private static final int TPA_CLIENT_TIMEOUT = 10;

  @ConfigProperty(name = "api.tpa.timeout", defaultValue = "30s")
  String timeout;

  @ConfigProperty(name = "quarkus.oidc-client.tpa.enabled", defaultValue = "true")
  boolean tpaEnabled;

  @Inject VulnerabilityProvider vulnerabilityProvider;
  @Inject TpaResponseHandler responseHandler;
  @Inject TpaRequestBuilder requestBuilder;

  @Inject OidcClients oidcClients;

  @Override
  public void configure() throws Exception {
    // fmt:off
    from(direct("tpaScan"))
      .routeId("tpaScan")
      .choice()
      .when(method(TpaRequestBuilder.class, "isEmpty"))
        .setBody(method(responseHandler, "emptyResponse"))
        .transform().method(responseHandler, "buildReport")
      .endChoice()
      .otherwise()
        .to(direct("tpaSplitRequest"))
        .transform().method(responseHandler, "buildReport");

    from(direct("tpaSplitRequest"))
      .routeId("tpaSplitRequest")
      .transform(method(TpaRequestBuilder.class, "split"))
      .split(body(), AggregationStrategies.beanAllowNull(responseHandler, "aggregateSplit"))
        .parallelProcessing()
          .transform().method(requestBuilder, "buildRequest")
          .process(this::processRequest)
          .process(requestBuilder::addAuthentication)
        .circuitBreaker()
          .faultToleranceConfiguration()
            .timeoutEnabled(true)
            .timeoutDuration(timeout)
          .end()
          .to(http("{{api.tpa.host}}"))
          .transform(method(responseHandler, "responseToIssues"))
        .onFallback()
          .process(responseHandler::processResponseError);
  

    from(direct("tpaHealthCheck"))
      .routeId("tpaHealthCheck")
      .setProperty(Constants.PROVIDER_NAME, constant(Constants.TPA_PROVIDER))
      .choice()
         .when(method(vulnerabilityProvider, "getEnabled").contains(Constants.TPA_PROVIDER))
            .to(direct("tpaHealthCheckEndpoint"))
         .otherwise()
            .to(direct("healthCheckProviderDisabled"));

    from(direct("tpaHealthCheckEndpoint"))
      .routeId("tpaHealthCheckEndpoint")
      .process(this::processHealthRequest)
      .circuitBreaker()
         .faultToleranceConfiguration()
            .timeoutEnabled(true)
            .timeoutDuration(timeout)
         .end()
         .process(requestBuilder::addAuthentication)
         .to(http("{{api.tpa.management.host}}"))
         .setHeader(Exchange.HTTP_RESPONSE_TEXT,constant("Service is up and running"))
         .setBody(constant("Service is up and running"))
      .onFallback()
         .setBody(constant(Constants.TPA_PROVIDER + "Service is down"))
         .setHeader(Exchange.HTTP_RESPONSE_CODE,constant(Response.Status.SERVICE_UNAVAILABLE))
      .end();

    from(direct("tpaValidateCredentials"))
      .routeId("tpaValidateCredentials")
      .circuitBreaker()
        .faultToleranceConfiguration()
          .timeoutEnabled(true)
          .timeoutDuration(timeout)
        .end()
        .process(this::processTokenRequest)
        .process(requestBuilder::addAuthentication)
        .to(http("{{api.tpa.host}}"))
        .setBody(constant("Token validated successfully"))
      .onFallback()
        .process(responseHandler::processTokenFallBack);
    // fmt:on
  }

  private void processRequest(Exchange exchange) {
    Message message = exchange.getMessage();
    message.removeHeader(Exchange.HTTP_RAW_QUERY);
    message.removeHeader(Exchange.HTTP_QUERY);
    message.removeHeader(Exchange.HTTP_URI);
    message.removeHeader(Constants.ACCEPT_ENCODING_HEADER);

    message.setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    message.setHeader(Exchange.HTTP_PATH, Constants.TPA_ANALYZE_PATH);
    message.setHeader(Exchange.HTTP_METHOD, HttpMethod.POST);

    String token = message.getHeader(Constants.TPA_TOKEN_HEADER, String.class);
    if (token == null && !tpaEnabled) {
      token = "placeholder";
    }
    if (token == null) {
      token =
          oidcClients
              .getClient(TPA_CLIENT_TENANT)
              .getTokens()
              .await()
              .atMost(Duration.ofSeconds(TPA_CLIENT_TIMEOUT))
              .getAccessToken();
    }
    if (token == null) {
      throw new IllegalStateException("No access token available.");
    }
    message.setHeader("Authorization", "Bearer " + token);
  }

  private void processHealthRequest(Exchange exchange) {
    Message message = exchange.getMessage();
    message.removeHeader(Exchange.HTTP_QUERY);
    message.removeHeader(Exchange.HTTP_URI);
    message.removeHeader(Exchange.HTTP_HOST);
    message.removeHeader(Constants.ACCEPT_ENCODING_HEADER);
    message.removeHeader(Exchange.CONTENT_TYPE);
    message.setHeader(Exchange.HTTP_PATH, Constants.TPA_HEALTH_PATH);
    message.setHeader(Exchange.HTTP_METHOD, HttpMethod.GET);
  }

  private void processTokenRequest(Exchange exchange) {
    Message message = exchange.getMessage();
    message.removeHeader(Exchange.HTTP_URI);
    message.removeHeader(Exchange.HTTP_HOST);
    message.removeHeader(Constants.ACCEPT_ENCODING_HEADER);
    message.removeHeader(Exchange.CONTENT_TYPE);
    message.setHeader(Exchange.HTTP_PATH, Constants.TPA_TOKEN_PATH);
    message.setHeader(Exchange.HTTP_METHOD, HttpMethod.GET);
    message.setHeader(Exchange.HTTP_QUERY, "limit=0");
  }
}
