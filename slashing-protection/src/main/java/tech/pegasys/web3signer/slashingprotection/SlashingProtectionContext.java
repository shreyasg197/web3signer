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
package tech.pegasys.web3signer.slashingprotection;

import org.jdbi.v3.core.Jdbi;

public class SlashingProtectionContext {

  private final Jdbi slashingProtectionJdbi;
  private final Jdbi pruningJdbi;
  private final RegisteredValidators registeredValidators;
  private final SlashingProtection slashingProtection;

  public SlashingProtectionContext(
      final Jdbi slashingProtectionJdbi,
      final Jdbi pruningJdbi,
      final RegisteredValidators registeredValidators,
      final SlashingProtection slashingProtection) {
    this.slashingProtectionJdbi = slashingProtectionJdbi;
    this.pruningJdbi = pruningJdbi;
    this.registeredValidators = registeredValidators;
    this.slashingProtection = slashingProtection;
  }

  public Jdbi getSlashingProtectionJdbi() {
    return slashingProtectionJdbi;
  }

  public Jdbi getPruningJdbi() {
    return pruningJdbi;
  }

  public RegisteredValidators getRegisteredValidators() {
    return registeredValidators;
  }

  public SlashingProtection getSlashingProtection() {
    return slashingProtection;
  }
}
