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
package tech.pegasys.web3signer.slashingprotection;

import static tech.pegasys.web3signer.slashingprotection.dao.DatabaseVersionDao.EXPECTED_DATABASE_VERSION;

import tech.pegasys.web3signer.slashingprotection.dao.DatabaseVersionDao;
import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import org.jdbi.v3.core.Jdbi;

public class SlashingProtectionContextFactory {

  public static SlashingProtectionContext create(
      final SlashingProtectionParameters slashingProtectionParameters) {
    final Jdbi jdbi =
        DbConnection.createConnection(
            slashingProtectionParameters.getDbUrl(),
            slashingProtectionParameters.getDbUsername(),
            slashingProtectionParameters.getDbPassword(),
            slashingProtectionParameters.getDbPoolConfigurationFile());
    verifyVersion(jdbi);

    // create separate Jdbi instance for pruning operations
    final Jdbi pruningJdbi =
        DbConnection.createPruningConnection(
            slashingProtectionParameters.getDbUrl(),
            slashingProtectionParameters.getDbUsername(),
            slashingProtectionParameters.getDbPassword(),
            slashingProtectionParameters.getDbPoolConfigurationFile());

    final ValidatorsDao validatorsDao = new ValidatorsDao();
    final RegisteredValidators registeredValidators = new RegisteredValidators(jdbi, validatorsDao);
    final DbSlashingProtection dbSlashingProtection =
        new DbSlashingProtection(
            jdbi,
            pruningJdbi,
            validatorsDao,
            new SignedBlocksDao(),
            new SignedAttestationsDao(),
            new MetadataDao(),
            new LowWatermarkDao(),
            slashingProtectionParameters.getPruningEpochsToKeep(),
            slashingProtectionParameters.getPruningSlotsPerEpoch(),
            registeredValidators);
    return new SlashingProtectionContext(
        jdbi, pruningJdbi, registeredValidators, dbSlashingProtection);
  }

  private static void verifyVersion(final Jdbi jdbi) {
    final DatabaseVersionDao databaseVersionDao = new DatabaseVersionDao();

    final int version;
    try {
      version = jdbi.withHandle(databaseVersionDao::findDatabaseVersion);
    } catch (final IllegalStateException e) {
      final String errorMsg =
          String.format("Failed to read database version, expected %s", EXPECTED_DATABASE_VERSION);
      throw new IllegalStateException(errorMsg, e);
    }

    if (version != EXPECTED_DATABASE_VERSION) {
      final String errorMsg =
          String.format(
              "Database version (%s) does not match expected version (%s), please run migrations and try again.",
              version, EXPECTED_DATABASE_VERSION);
      throw new IllegalStateException(errorMsg);
    }
  }
}
