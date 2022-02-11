/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2022 The MITRE Corporation.
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
package org.mitre.niem.xsd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.niem.cmf.Namespace.NSK_BUILTIN;
import static org.mitre.niem.cmf.Namespace.NSK_CORE;
import static org.mitre.niem.cmf.Namespace.NSK_DOMAIN;
import static org.mitre.niem.cmf.Namespace.NSK_EXTENSION;
import static org.mitre.niem.cmf.Namespace.NSK_OTHERNIEM;
import org.mitre.niem.cmf.NamespaceMap;
import org.mitre.niem.xsd.NamespaceInfo.NSDeclRec;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NamespaceInfoTest {
    
    public NamespaceInfoTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testNamespaceKindsPriorities () throws IOException {
        NamespaceInfo nsi = new NamespaceInfo();
        File f = new File("src/test/resources/xsd/nameinfo.xsd");
        nsi.processSchemaDocument(f.toURI().toString());
        Files.walk(Paths.get("src/test/resources/xsd/nameinfo-niem"))
                .filter(Files::isRegularFile)
                .forEach((p) -> {
                    String furi = p.toFile().toURI().toString();
                    if (furi.endsWith(".xsd")) {
                        nsi.processSchemaDocument(furi);
                    }
                    
                });
        assertEquals(12, nsi.targetNamespaces().size());
        assertEquals(NSK_EXTENSION, nsi.getNSKind("http://example.com/CrashDriver/1.1/"));
        assertEquals(NSK_CORE, nsi.getNSKind("http://release.niem.gov/niem/niem-core/5.0/"));
        assertEquals(NSK_DOMAIN, nsi.getNSKind("http://release.niem.gov/niem/domains/humanServices/5.0/"));
        assertEquals(NSK_DOMAIN, nsi.getNSKind("http://release.niem.gov/niem/domains/jxdm/7.0/"));
        assertEquals(NSK_OTHERNIEM, nsi.getNSKind("http://release.niem.gov/niem/codes/unece_rec20/5.0/"));
        assertEquals(NSK_OTHERNIEM, nsi.getNSKind("http://release.niem.gov/niem/codes/aamva_d20/5.0/"));
        assertEquals(NSK_BUILTIN, nsi.getNSKind("http://release.niem.gov/niem/structures/5.0/"));
        assertEquals(NSK_BUILTIN, nsi.getNSKind("http://release.niem.gov/niem/conformanceTargets/3.0/"));
        assertEquals(NSK_BUILTIN, nsi.getNSKind("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-instance/"));
        assertEquals(NSK_BUILTIN, nsi.getNSKind("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-schema-appinfo/"));
        assertEquals(NSK_BUILTIN, nsi.getNSKind("http://release.niem.gov/niem/proxy/niem-xs/5.0/"));
        assertEquals(NSK_BUILTIN, nsi.getNSKind("http://release.niem.gov/niem/appinfo/5.0/"));
        
        List<NSDeclRec> decls = nsi.getNSdecls();
        NamespaceMap nsmap = new NamespaceMap();
        for (NSDeclRec ndr : decls) {
            nsmap.assignPrefix(ndr.prefix, ndr.uri);
        }
        assertEquals("jxdm", nsmap.getPrefix("http://release.niem.gov/niem/domains/jxdm/7.0/"));
        assertEquals("aamva_d20", nsmap.getPrefix("http://example.com/somethingElse/"));
    }
}
