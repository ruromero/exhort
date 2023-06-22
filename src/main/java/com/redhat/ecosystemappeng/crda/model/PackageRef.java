/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package com.redhat.ecosystemappeng.crda.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.packageurl.PackageURL;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PackageRef(
    String name,
    String version,
    @JsonSerialize(using = PackageURLSerializer.class) PackageURL purl) {

  public PackageRef {
    Objects.requireNonNull(name);
    Objects.requireNonNull(version);
  }

  @JsonIgnore
  public String getId() {
    return name + "@" + version;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (!(other instanceof PackageRef)) {
      return false;
    }
    return Objects.equals(name, ((PackageRef) other).name);
  }

  public static PackageRef parse(String gav) {
    String[] parts = gav.split(":");
    if (parts.length < 4 || parts.length > 6) {
      throw new IllegalArgumentException("Unexpected GAV format. " + gav);
    }
    String name = parts[0] + ":" + parts[1];
    if (parts.length < 6) {
      return build(name, parts[3]);
    }
    return build(name, parts[4]);
  }

  public static PackageRef build(PackageURL purl) {
    if (Objects.isNull(purl.getNamespace())) {
      return new PackageRef(purl.getName(), purl.getVersion(), purl);
    }
    return new PackageRef(purl.getNamespace() + ":" + purl.getName(), purl.getVersion(), purl);
  }

  public static PackageRef build(String name, String version) {
    return new PackageRef(name, version, null);
  }

  public static PackageRef build(String namespace, String name, String version) {
    if (Objects.isNull(namespace)) {
      return new PackageRef(name, version, null);
    }
    return new PackageRef(namespace + ":" + name, version, null);
  }

  public String toGav() {
    return String.format("%s:%s", name, version);
  }

  @Override
  public String toString() {
    return toGav();
  }
}
