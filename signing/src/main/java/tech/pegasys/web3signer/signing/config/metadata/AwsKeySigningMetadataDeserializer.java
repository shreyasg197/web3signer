/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.config.metadata;

import tech.pegasys.web3signer.signing.config.AwsAuthenticationMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class AwsKeySigningMetadataDeserializer extends StdDeserializer<AwsKeySigningMetadata> {

  public static final String AUTH_MODE = "authenticationMode";
  public static final String REGION = "region";
  public static final String ACCESS_KEY_ID = "accessKeyId";
  public static final String SECRET_ACCESS_KEY = "secretAccessKey";
  public static final String SECRET_NAME = "secretName";

  @SuppressWarnings("Unused")
  public AwsKeySigningMetadataDeserializer() {
    this(null);
  }

  protected AwsKeySigningMetadataDeserializer(final Class<?> vc) {
    super(vc);
  }

  @Override
  public AwsKeySigningMetadata deserialize(
      final JsonParser parser, final DeserializationContext context) throws IOException {

    AwsAuthenticationMode authMode = AwsAuthenticationMode.SPECIFIED;
    String region = null;
    String accessKeyId = null;
    String secretAccess_key = null;
    String secretName = null;

    final JsonNode node = parser.getCodec().readTree(parser);

    if (node.get(AUTH_MODE) != null) {
      try {
        authMode = AwsAuthenticationMode.valueOf(node.get(AUTH_MODE).asText());
      } catch (final IllegalArgumentException e) {
        throw new JsonMappingException(
            parser, "Error converting " + AUTH_MODE + ": " + e.getMessage());
      }
    }

    if (node.get(REGION) != null) {
      region = node.get(REGION).asText();
    }

    if (node.get(ACCESS_KEY_ID) != null) {
      accessKeyId = node.get(ACCESS_KEY_ID).asText();
    }

    if (node.get(SECRET_ACCESS_KEY) != null) {
      secretAccess_key = node.get(SECRET_ACCESS_KEY).asText();
    }

    if (node.get(SECRET_NAME) != null) {
      secretName = node.get(SECRET_NAME).asText();
    }

    final AwsKeySigningMetadata awsKeySigningMetadata =
        new AwsKeySigningMetadata(authMode, region, accessKeyId, secretAccess_key, secretName);

    validate(parser, awsKeySigningMetadata);

    return awsKeySigningMetadata;
  }

  private void validate(final JsonParser parser, AwsKeySigningMetadata awsKeySigningMetadata)
      throws JsonMappingException {
    final List<String> missingParameters = new ArrayList<>();

    // globally required fields
    if (awsKeySigningMetadata.getRegion() == null) {
      missingParameters.add(REGION);
    }

    if (awsKeySigningMetadata.getSecretName() == null) {
      missingParameters.add(SECRET_NAME);
    }

    // Specified auth mode required fields
    if (awsKeySigningMetadata.getAuthenticationMode() == AwsAuthenticationMode.SPECIFIED) {
      if (awsKeySigningMetadata.getAccessKeyId() == null) {
        missingParameters.add(ACCESS_KEY_ID);
      }

      if (awsKeySigningMetadata.getSecretAccessKey() == null) {
        missingParameters.add(SECRET_ACCESS_KEY);
      }
    }

    if (!missingParameters.isEmpty()) {
      throw new JsonMappingException(
          parser, "Missing values for required parameters: " + String.join(",", missingParameters));
    }
  }
}
