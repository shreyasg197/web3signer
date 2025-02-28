/*
 * Copyright 2021 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.web3signer.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import tech.pegasys.web3signer.core.Runner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(VertxExtension.class)
class OpenApiSpecsExtractorTest {

  @ParameterizedTest
  @ValueSource(
      strings = {"eth1/web3signer.yaml", "eth2/web3signer.yaml", "filecoin/web3signer.yaml"})
  void openapiSpecsAreExtractedAndLoaded(final String spec, final Vertx vertx) throws Exception {
    final OpenApiSpecsExtractor openApiSpecsExtractor =
        new OpenApiSpecsExtractor.OpenApiSpecsExtractorBuilder()
            .withConvertRelativeRefToAbsoluteRef(true)
            .build();
    final Optional<Path> specPath = openApiSpecsExtractor.getSpecFilePathAtDestination(spec);

    // assert that routerBuilder is able to load the extracted specs
    assertThatCode(
            () ->
                Runner.getRouterBuilder(vertx, specPath.get().toUri().toString())
                    .securityHandler("bearerAuth", x -> {})
                    .createRouter())
        .doesNotThrowAnyException();

    // assert that relative ref has been converted to absolute ref
    if (spec.equals("eth2/web3signer.yaml")) {
      final String sign_yaml =
          openApiSpecsExtractor.getDestinationSpecPaths().stream()
              .filter(path -> path.endsWith("sign.yaml"))
              .findFirst()
              .map(this::readString)
              .orElseThrow();

      assertThat(sign_yaml)
          .contains(
              openApiSpecsExtractor
                  .getDestinationDirectory()
                  .resolve("eth2/signing/schemas.yaml")
                  .toString());
    }
  }

  private String readString(final Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
