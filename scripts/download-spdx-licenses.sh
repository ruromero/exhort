#!/bin/bash

version="3.27.0"  # https://github.com/spdx/license-list-data/releases
url="https://github.com/spdx/license-list-data/archive/refs/tags/v${version}.tar.gz"

if [ ! -d "target" ]; then
  mkdir -p "target"
fi

if [ ! -f "target/spdx-licenses-${version}.tar.gz" ]; then
  wget -O "target/spdx-licenses-${version}.tar.gz" "${url}"
fi

if [ ! -d "target/license-list-data-${version}" ]; then
  tar -xzf "target/spdx-licenses-${version}.tar.gz" -C "target"
fi

if [ -d "src/main/resources/spdx-licenses" ]; then
  rm -rf "src/main/resources/spdx-licenses"
fi
cp -r "target/license-list-data-${version}/json/details" "src/main/resources/spdx-licenses"
