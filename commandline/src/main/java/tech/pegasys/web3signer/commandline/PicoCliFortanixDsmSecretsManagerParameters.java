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
package tech.pegasys.web3signer.commandline;

import tech.pegasys.web3signer.signing.config.FortanixDsmSecretsManagerParameters;

import picocli.CommandLine.Option;

public class PicoCliFortanixDsmSecretsManagerParameters
    implements FortanixDsmSecretsManagerParameters {
  @Option(
      names = {"--fortanix-dsm-enabled"},
      description =
          "Set true if Web3signer should try and load key from specified Fortanix DSM server "
              + "(Default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>")
  private boolean FortanixDsmEnalbed = false;

  @Option(
      names = {"--server"},
      description = "Set to string value of Fortanix DSM server" + "(Default: ${DEFAULT-VALUE})",
      paramLabel = "<SERVER>")
  private String Server;

  @Option(
      names = {"--api-key"},
      description =
          "Set to string value of Api Key for Fortanix DSM app" + "(Default: ${DEFAULT-VALUE})",
      paramLabel = "<API_KEY>")
  private String ApiKey;

  @Option(
      names = {"--secret-name"},
      description =
          "Set to string value of secret stored in Fortanix DSM" + "(Default: ${DEFAULT-VALUE})",
      paramLabel = "<SECRET_NAME>")
  private String SecretName;

  @Override
  public boolean isFortanixDsmEnabled() {
    return FortanixDsmEnalbed;
  }

  @Override
  public String getServer() {
    return Server;
  }

  @Override
  public String getApiKey() {
    return ApiKey;
  }

  @Override
  public String getSecretName() {
    return SecretName;
  }
}
