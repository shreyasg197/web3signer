/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.web3signer.tests.publickeys;

import static java.util.Map.entry;
import static org.hamcrest.Matchers.containsInAnyOrder;

import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "DSM_SERVER", matches = ".*")
public class FortanixDsmAcceptanceTest extends KeyIdentifiersAcceptanceTestBase {
  private static final String SERVER = System.getenv("DSM_SERVER");
  private static final String API_KEY = System.getenv("API_KEY");
  // Add two BLS keys to Fortanix DSM and set the env for each key as KEY_ID1 and KEY_ID2
  private static String secrets[] = {System.getenv("KEY_ID1"), System.getenv("KEY_ID2")};

  // following keys are expected to be pre-loaded in FortanixDSM as Secret Data (HEX).
  private static final Map<Integer, String> PRE_LOADED_BLS_PRIVATE_KEYS =
      Map.ofEntries(
          entry(0, "5ec00b6842e0021be1dbacb18b8e5c834cb41fbbf8f7857ee43f1c1ccbe63c4f"),
          entry(1, "73d51abbd89cb8196f0efb6892f94d68fccc2c35f0b84609e5f12c55dd85aba8"));

  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();

  @Test
  public void blsKeysAreLoadedFromFortanixDsm() {
    createConfigurationFiles(PRE_LOADED_BLS_PRIVATE_KEYS.keySet(), KeyType.BLS);
    initAndStartSigner(calculateMode(KeyType.BLS));

    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    final String[] expectedPublicKeys =
        PRE_LOADED_BLS_PRIVATE_KEYS.values().stream()
            .map(key -> BLSSecretKey.fromBytes(Bytes32.fromHexString(key)).toPublicKey().toString())
            .toArray(String[]::new);
    validateApiResponse(response, containsInAnyOrder(expectedPublicKeys));
  }

  private void createConfigurationFiles(final Set<Integer> KeySet, final KeyType keyType) {
    KeySet.forEach(
        index -> {
          final Path configFile = testDirectory.resolve("fortanixdsm_" + index + ".yaml");
          METADATA_FILE_HELPERS.createFortanixYamlFileAt(
              configFile, SERVER, API_KEY, secrets[index], keyType);
        });
  }
}
