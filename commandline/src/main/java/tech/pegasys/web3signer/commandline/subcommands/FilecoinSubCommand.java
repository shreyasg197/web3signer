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
package tech.pegasys.web3signer.commandline.subcommands;

import tech.pegasys.web3signer.core.FilecoinRunner;
import tech.pegasys.web3signer.core.Runner;
import tech.pegasys.web3signer.signing.filecoin.FilecoinNetwork;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

@Command(
    name = FilecoinSubCommand.COMMAND_NAME,
    description = "Handle Filecoin signing operations and address reporting",
    subcommands = {HelpCommand.class},
    mixinStandardHelpOptions = true)
public class FilecoinSubCommand extends ModeSubCommand {

  public static final String COMMAND_NAME = "filecoin";

  @Option(
      names = {"--network"},
      description = "Filecoin network to use for addresses (default: ${DEFAULT-VALUE})",
      paramLabel = "<network name>",
      arity = "1")
  private final FilecoinNetwork network = FilecoinNetwork.MAINNET;

  @Override
  public Runner createRunner() {
    return new FilecoinRunner(config, network);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  protected void validateArgs() {
    // no special validation required
  }
}
