/*
 * Copyright 2010-2018 Norwegian Agency for Public Management and eGovernment (Difi)
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/community/eupl/og_page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package network.oxalis.commons.persist;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import network.oxalis.api.evidence.EvidenceFactory;
import network.oxalis.api.inbound.InboundMetadata;
import network.oxalis.api.lang.EvidenceException;
import network.oxalis.api.model.TransmissionIdentifier;
import network.oxalis.api.persist.PersisterHandler;
import network.oxalis.api.util.Type;
import network.oxalis.commons.filesystem.FileUtils;
import network.oxalis.vefa.peppol.common.model.Header;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * @author erlend
 * @since 4.0.0
 */
@Slf4j
@Singleton
@Type("default")
public class DefaultPersister implements PersisterHandler {

    private final EvidenceFactory evidenceFactory;

    private final Path inboundFolder;

    @Inject
    public DefaultPersister(@Named("inbound") Path inboundFolder, EvidenceFactory evidenceFactory) {
        this.inboundFolder = inboundFolder;
        this.evidenceFactory = evidenceFactory;
    }

    @Override
    public Path persist(TransmissionIdentifier transmissionIdentifier, Header header, InputStream inputStream)
            throws IOException {
        Path path = PersisterUtils.createArtifactFolders(inboundFolder, header).resolve(
                String.format("%s.doc.xml", FileUtils.filterString(transmissionIdentifier.getIdentifier())));

        try (OutputStream outputStream = Files.newOutputStream(path)) {
            ByteStreams.copy(inputStream, outputStream);
        }

        log.debug("Payload persisted to: {}", path);

        return path;
    }

    @Override
    public void persist(InboundMetadata inboundMetadata, Path payloadPath) throws IOException {
        String transmissionIdentifier = inboundMetadata.getTransmissionIdentifier().getIdentifier();
        String filteredTransmissionIdentifier = FileUtils.filterString(transmissionIdentifier);
        Path directory = PersisterUtils.createArtifactFolders(inboundFolder, inboundMetadata.getHeader());

        String receiptFileName = String.format("%s.receipt.dat", filteredTransmissionIdentifier);
        Path receiptPath = directory.resolve(receiptFileName);

        try (OutputStream outputStream = Files.newOutputStream(receiptPath)) {
            evidenceFactory.write(outputStream, inboundMetadata);
        } catch (EvidenceException e) {
            throw new IOException("Unable to persist receipt.", e);
        }

        String certificateFileName = String.format("%s.sender.dat", filteredTransmissionIdentifier);
        Path certificatePath = directory.resolve(certificateFileName);

        try (OutputStream outputStream = Files.newOutputStream(certificatePath)) {
            X509Certificate certificate = inboundMetadata.getCertificate();
            outputStream.write(certificate.getEncoded());
        } catch (CertificateEncodingException | IOException e) {
            log.error("Unable to persist certificate to: {}.", certificatePath, e);
        }

        log.debug("Receipt persisted to: {}", receiptPath);
    }

    /**
     * @since 4.0.3
     */
    @Override
    public void persist(TransmissionIdentifier transmissionIdentifier, Header header,
                        Path payloadPath, Exception exception) {
        try {
            log.warn("Transmission '{}' failed duo to {}.", transmissionIdentifier, exception.getMessage());

            // Delete temp file
            Files.delete(payloadPath);
        } catch (IOException e) {
            log.warn("Unable to delete file: {}", payloadPath, e);
        }
    }
}
