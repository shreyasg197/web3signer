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
package tech.pegasys.web3signer.signing.config.metadata;

import tech.pegasys.signers.aws.AwsSecretsManager;
import tech.pegasys.signers.aws.AwsSecretsManagerProvider;
import tech.pegasys.signers.bls.keystore.KeyStore;
import tech.pegasys.signers.bls.keystore.KeyStoreLoader;
import tech.pegasys.signers.bls.keystore.KeyStoreValidationException;
import tech.pegasys.signers.bls.keystore.model.KeyStoreData;
import tech.pegasys.signers.fortanixdsm.FortanixDSM;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.common.Web3SignerMetricCategory;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.AwsSecretsManagerFactory;
import tech.pegasys.web3signer.signing.config.metadata.interlock.InterlockKeyProvider;
import tech.pegasys.web3signer.signing.config.metadata.yubihsm.YubiHsmOpaqueDataProvider;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public class BlsArtifactSignerFactory extends AbstractArtifactSignerFactory {

  private final LabelledMetric<OperationTimer> privateKeyRetrievalTimer;
  private final Function<BlsArtifactSignerArgs, ArtifactSigner> signerFactory;
  private final AwsSecretsManagerProvider awsSecretsManagerProvider;
  private final FortanixDSM fortanixDsmProvider;

  public BlsArtifactSignerFactory(
      final Path configsDirectory,
      final MetricsSystem metricsSystem,
      final HashicorpConnectionFactory connectionFactory,
      final InterlockKeyProvider interlockKeyProvider,
      final YubiHsmOpaqueDataProvider yubiHsmOpaqueDataProvider,
      final AwsSecretsManagerProvider awsSecretsManagerProvider,
      final FortanixDSM fortanixDsmProvider,
      final Function<BlsArtifactSignerArgs, ArtifactSigner> signerFactory) {
    super(connectionFactory, configsDirectory, interlockKeyProvider, yubiHsmOpaqueDataProvider);
    privateKeyRetrievalTimer =
        metricsSystem.createLabelledTimer(
            Web3SignerMetricCategory.SIGNING,
            "private_key_retrieval_time",
            "Time taken to retrieve private key",
            "signer");
    this.signerFactory = signerFactory;
    this.awsSecretsManagerProvider = awsSecretsManagerProvider;
    this.fortanixDsmProvider = fortanixDsmProvider;
  }

  @Override
  public ArtifactSigner create(final FileRawSigningMetadata fileRawSigningMetadata) {
    try (final TimingContext ignored = privateKeyRetrievalTimer.labels("file-raw").startTimer()) {
      return signerFactory.apply(
          new BlsArtifactSignerArgs(
              new BLSKeyPair(BLSSecretKey.fromBytes(fileRawSigningMetadata.getPrivateKeyBytes())),
              SignerOrigin.FILE_RAW));
    }
  }

  @Override
  public ArtifactSigner create(final FileKeyStoreMetadata fileKeyStoreMetadata) {
    try (final TimingContext ignored =
        privateKeyRetrievalTimer.labels("file-keystore").startTimer()) {
      return createKeystoreArtifact(fileKeyStoreMetadata);
    }
  }

  @Override
  public ArtifactSigner create(final HashicorpSigningMetadata hashicorpMetadata) {
    try (final TimingContext ignored = privateKeyRetrievalTimer.labels("hashicorp").startTimer()) {
      final Bytes privateKeyBytes = extractBytesFromVault(hashicorpMetadata);
      final BLSKeyPair keyPair =
          new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKeyBytes)));
      return signerFactory.apply(new BlsArtifactSignerArgs(keyPair, SignerOrigin.HASHICORP));
    }
  }

  @Override
  public ArtifactSigner create(final AzureSecretSigningMetadata azureSecretSigningMetadata) {
    try (final TimingContext ignored = privateKeyRetrievalTimer.labels("azure").startTimer()) {
      final Bytes privateKeyBytes = extractBytesFromVault(azureSecretSigningMetadata);
      final BLSKeyPair keyPair =
          new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKeyBytes)));
      return signerFactory.apply(new BlsArtifactSignerArgs(keyPair, SignerOrigin.AZURE));
    }
  }

  @Override
  public ArtifactSigner create(final InterlockSigningMetadata interlockSigningMetadata) {
    try (final TimingContext ignored = privateKeyRetrievalTimer.labels("interlock").startTimer()) {
      final Bytes32 keyBytes = Bytes32.wrap(extractBytesFromInterlock(interlockSigningMetadata));
      final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(keyBytes));
      return signerFactory.apply(new BlsArtifactSignerArgs(keyPair, SignerOrigin.INTERLOCK));
    }
  }

  @Override
  public ArtifactSigner create(final YubiHsmSigningMetadata yubiHsmSigningMetadata) {
    try (final TimingContext ignored = privateKeyRetrievalTimer.labels("yubihsm").startTimer()) {
      final Bytes32 keyBytes = Bytes32.wrap(extractOpaqueDataFromYubiHsm(yubiHsmSigningMetadata));
      final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(keyBytes));
      return signerFactory.apply(new BlsArtifactSignerArgs(keyPair, SignerOrigin.YUBI_HSM));
    }
  }

  @Override
  public ArtifactSigner create(
      final FortanixDsmSecretSigningMetadata fortanixDsmSecretSigningMetadata) {
    try (final TimingContext ignored =
        privateKeyRetrievalTimer.labels("fortanixdsm").startTimer()) {
      final Bytes32 keyBytes =
          Bytes32.wrap(extractBytesFromFortanixDsm(fortanixDsmSecretSigningMetadata));
      final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(keyBytes));
      return signerFactory.apply(new BlsArtifactSignerArgs(keyPair, SignerOrigin.FORTANIXDSM));
    }
  }

  @Override
  public ArtifactSigner create(final AwsKeySigningMetadata awsKeySigningMetadata) {
    try (final TimingContext ignored = privateKeyRetrievalTimer.labels("aws").startTimer()) {
      final Bytes32 keyBytes = Bytes32.wrap(extractBytesFromSecretsManager(awsKeySigningMetadata));
      final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(keyBytes));
      return signerFactory.apply(new BlsArtifactSignerArgs(keyPair, SignerOrigin.AWS));
    }
  }

  private ArtifactSigner createKeystoreArtifact(final FileKeyStoreMetadata fileKeyStoreMetadata) {
    final Path keystoreFile = makeRelativePathAbsolute(fileKeyStoreMetadata.getKeystoreFile());
    final Path keystorePasswordFile =
        makeRelativePathAbsolute(fileKeyStoreMetadata.getKeystorePasswordFile());
    try {
      final KeyStoreData keyStoreData = KeyStoreLoader.loadFromFile(keystoreFile);
      final String password = loadPassword(keystorePasswordFile);
      final Bytes privateKey = KeyStore.decrypt(password, keyStoreData);
      final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKey)));
      return signerFactory.apply(
          new BlsArtifactSignerArgs(
              keyPair, SignerOrigin.FILE_KEYSTORE, Optional.ofNullable(keyStoreData.getPath())));
    } catch (final KeyStoreValidationException e) {
      throw new SigningMetadataException(e.getMessage(), e);
    }
  }

  private Bytes extractBytesFromSecretsManager(final AwsKeySigningMetadata metadata) {
    final AwsSecretsManager awsSecretsManager =
        AwsSecretsManagerFactory.createAwsSecretsManager(awsSecretsManagerProvider, metadata);
    return awsSecretsManager
        .fetchSecret(metadata.getSecretName())
        .map(Bytes::fromHexString)
        .orElseThrow(
            () -> new SigningMetadataException("Failed to fetch secret from AWS Secrets Manager"));
  }

  private Bytes extractBytesFromFortanixDsm(final FortanixDsmSecretSigningMetadata metadata) {
    try {
      FortanixDSM.createWithApiKeyCredential(
          fortanixDsmProvider, metadata.getServer(), metadata.getApiKey(), false, false);
      final Optional<Bytes> secret = fortanixDsmProvider.fetchSecret(metadata.getSecretName());
      return secret.get();
    } catch (RuntimeException e) {
      throw new SigningMetadataException(
          "Failed to fetch secret from FortanixDsm: " + e.getMessage(), e);
    }
  }

  @Override
  public KeyType getKeyType() {
    return KeyType.BLS;
  }
}
