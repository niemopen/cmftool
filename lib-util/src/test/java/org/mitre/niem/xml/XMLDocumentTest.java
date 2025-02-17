/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2025 The MITRE Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.niem.xml;

import java.io.File;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLDocumentTest {

    private final static String resDN  = "src/test/resources/";
    private final static File resDF  = new File(resDN);
    
    public XMLDocumentTest() {
    }

    @Test
    public void testGetXMLDocumentElementNamespace_String () throws Exception {
        String res;
        res = XMLDocument.getXMLDocumentElementNamespace(new File(resDN, "xsd/xsDocTest.xsd"));
        assertEquals(res, "http://www.w3.org/2001/XMLSchema");
        res = XMLDocument.getXMLDocumentElementNamespace(new File(resDN, "/xsd/00-README.txt"));
        assertEquals(res, "");
        var thrown = Assertions.assertThrows(FileNotFoundException.class, () -> {
            var r = XMLDocument.getXMLDocumentElementNamespace("src/test/resources/xsd/nosuch");
        });
        assertNotNull(thrown);
    }

    @Test
    public void testGetXMLDocumentElementNamespace () throws Exception {
        String res;
        res = XMLDocument.getXSDTargetNamespace(new File(resDN, "xsd/xsDocTest.xsd"));
        assertEquals(res, "http://example.com/test/");
        res = XMLDocument.getXSDTargetNamespace(new File(resDN, "cat/cat1.xml"));
        assertEquals(res, "");
    }
    
}
