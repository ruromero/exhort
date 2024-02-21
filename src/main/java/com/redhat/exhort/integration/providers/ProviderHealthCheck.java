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

package com.redhat.exhort.integration.providers;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.health.AbstractHealthCheck;
import org.eclipse.microprofile.config.ConfigProvider;

public abstract class ProviderHealthCheck extends AbstractHealthCheck {

  public ProviderHealthCheck(String providerName) {
    super("providers", providerName);
    var disabled =
        ConfigProvider.getConfig()
            .getConfigValue("api." + providerName + ".disabled")
            .getRawValue();
    setEnabled(!Boolean.valueOf(disabled));
  }

  @Override
  protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
    var response =
        getCamelContext()
            .createProducerTemplate()
            .send(
                getHealthCheckRoute(),
                ExchangeBuilder.anExchange(getCamelContext()).build());

    var code = response.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
    if (code < 400) {
      builder.up();
    } else {
      builder.down();
    }
  }

  @Override
  public boolean isLiveness() {
    return false;
  }

  protected abstract String getHealthCheckRoute();
}
