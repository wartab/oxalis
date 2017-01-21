/*
 * Copyright (c) 2010 - 2015 Norwegian Agency for Pupblic Government and eGovernment (Difi)
 *
 * This file is part of Oxalis.
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission
 * - subsequent versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence
 *  is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package eu.peppol.document;

import eu.peppol.PeppolStandardBusinessHeader;
import eu.peppol.datagenerator.FileGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author steinar
 *         Date: 24.06.15
 *         Time: 15.58
 */
public class SbdhFastParserTest {

    public static final Logger log = LoggerFactory.getLogger(SbdhFastParserTest.class);
    public static final String EHF_INVOICE_NO_SBDH_XML = "/ehf-invoice-no-sbdh.xml";
    private File xmlSampleFile;

    @BeforeMethod
    public void setUp() {
        xmlSampleFile = FileGenerator.generate(FileGenerator.MB * 100L);
    }

    @AfterMethod
    public void tearDown() {
        if (xmlSampleFile.exists() && xmlSampleFile.isFile()) {
            xmlSampleFile.delete();
        }
    }

    /**
     * Parses a rather large xml document with SBDH.
     *
     * @throws Exception
     */
    @Test
    public void parseMediumSizedFile() throws Exception {
        FileInputStream fileInputStream = new FileInputStream(xmlSampleFile);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

        long start = System.currentTimeMillis();
        PeppolStandardBusinessHeader sbdh = SbdhFastParser.parse(fileInputStream);
        long stop = System.currentTimeMillis();
        long elapsed = stop - start;

        log.debug("Time elapsed : " + elapsed + "ms");
        boolean sbdhDetected = sbdh != null;

        assertTrue(bufferedInputStream.available() > 0);
        assertTrue(sbdhDetected, "SBDH was not detected");
    }


    @Test(expectedExceptions = IllegalStateException.class)
    public void parseXmlFileWithoutSBDH() {
        InputStream resourceAsStream = getClass().getResourceAsStream(EHF_INVOICE_NO_SBDH_XML);

        PeppolStandardBusinessHeader sbdh = SbdhFastParser.parse(resourceAsStream);
        assertNull(sbdh);
    }
}