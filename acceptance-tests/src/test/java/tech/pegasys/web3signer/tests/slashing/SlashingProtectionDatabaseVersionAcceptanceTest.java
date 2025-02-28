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
package tech.pegasys.web3signer.tests.slashing;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;

import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.DatabaseUtil;

import java.nio.file.Path;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SlashingProtectionDatabaseVersionAcceptanceTest {

  public static final String DB_USERNAME = "postgres";
  public static final String DB_PASSWORD = "postgres";

  @Test
  void missingOrWrongVersionCauseAppToHaltOnStartup(@TempDir Path testDirectory) {
    // NB: This test fails when running with a ThreadRunner (i.e. in IDE) due to the
    // System.exit() call in the Web3signerApp (which exits the test, rather than terminating
    // the app (thus it work as expected with a ProcessRunner (i.e. when run from gradle)).

    final String dbUrl = DatabaseUtil.create().databaseUrl();
    final Jdbi jdbi = Jdbi.create(dbUrl, DB_USERNAME, DB_PASSWORD);
    jdbi.useHandle(h -> h.execute("DROP TABLE database_version"));

    final SignerConfigurationBuilder builder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withSlashingEnabled(true)
            .withSlashingProtectionDbUsername(DB_USERNAME)
            .withSlashingProtectionDbPassword(DB_PASSWORD)
            .withMetricsEnabled(true)
            .withSlashingProtectionDbUrl(dbUrl)
            .withKeyStoreDirectory(testDirectory)
            .withHttpPort(9000); // to ensure

    final Signer signer = new Signer(builder.build(), null);

    signer.start();
    waitFor(() -> assertThat(signer.isRunning()).isTrue());
    waitFor(() -> assertThat(signer.isRunning()).isFalse());
  }
}
