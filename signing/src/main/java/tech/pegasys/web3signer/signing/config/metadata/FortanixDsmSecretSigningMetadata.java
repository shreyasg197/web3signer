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

import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.FortanixDsmSecretsManagerParameters;

public class FortanixDsmSecretSigningMetadata extends SigningMetadata
    implements FortanixDsmSecretsManagerParameters {
  private final String ApiKey;
  private final String Server;
  private final String SecretName;

  public FortanixDsmSecretSigningMetadata(
      final String ApiKey, final String Server, final String SecretName, final KeyType keyType) {
    super(keyType != null ? keyType : KeyType.BLS);
    this.ApiKey = ApiKey;
    this.Server = Server;
    this.SecretName = SecretName;
  }

  @Override
  public boolean isFortanixDsmEnabled() {
    return true;
  }

  @Override
  public String getApiKey() {
    return ApiKey;
  }

  @Override
  public String getServer() {
    return Server;
  }

  @Override
  public String getSecretName() {
    return SecretName;
  }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory factory) {
    return factory.create(this);
  }
}
