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
package tech.pegasys.web3signer.slashingprotection.validator;

import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_COMMITTED;

import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;

import java.util.Optional;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeExecutor;
import net.jodah.failsafe.RetryPolicy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementException;

public class GenesisValidatorRootValidator {

  private static final Logger LOG = LogManager.getLogger();
  private final Jdbi jdbi;
  private final MetadataDao metadataDao;
  private final FailsafeExecutor<Object> failsafeExecutor;

  public GenesisValidatorRootValidator(final Jdbi jdbi, final MetadataDao metadataDao) {
    this.jdbi = jdbi;
    this.metadataDao = metadataDao;
    this.failsafeExecutor =
        Failsafe.with(new RetryPolicy<>().handle(StatementException.class).withMaxRetries(1));
  }

  public boolean checkGenesisValidatorsRootAndInsertIfEmpty(Bytes32 genesisValidatorsRoot) {
    return failsafeExecutor.get(
        () ->
            jdbi.inTransaction(
                READ_COMMITTED, handle -> validateGvr(handle, genesisValidatorsRoot)));
  }

  public boolean genesisValidatorRootExists() {
    return failsafeExecutor.get(
        () ->
            jdbi.inTransaction(
                READ_COMMITTED,
                handle -> metadataDao.findGenesisValidatorsRoot(handle).isPresent()));
  }

  private boolean validateGvr(final Handle handle, final Bytes32 genesisValidatorsRoot) {
    final Optional<Bytes32> dbGvr = metadataDao.findGenesisValidatorsRoot(handle);
    final boolean isValidGvr = dbGvr.map(gvr -> gvr.equals(genesisValidatorsRoot)).orElse(true);
    if (!isValidGvr) {
      LOG.warn(
          "Supplied genesis validators root {} does not match value in database",
          genesisValidatorsRoot);
    } else if (dbGvr.isEmpty()) {
      metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot);
    }
    return isValidGvr;
  }
}
