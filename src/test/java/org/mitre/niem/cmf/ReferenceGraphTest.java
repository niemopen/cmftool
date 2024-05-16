/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2023 The MITRE Corporation.
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
package org.mitre.niem.cmf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.xsd.ModelXMLReader;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ReferenceGraphTest {
    
   static String testDir = "src/test/resources/cmf/";
   
    public ReferenceGraphTest() {
    }

//    @Test
//    public void testReachable() throws FileNotFoundException {
//        FileInputStream mis = new FileInputStream(testDir+"augCCwE.cmf");
//        ModelXMLReader mr = new ModelXMLReader();
//        Model m = mr.readXML(mis);
//        var my = m.getNamespaceByURI("http://example.com/N6AugEx/1.0/");
//        var nc = m.getNamespaceByURI("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/");
//        var j  = m.getNamespaceByURI("https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/");
//        assertNotNull(my);
//        assertNotNull(nc);
//        assertNotNull(j);
//        var rg = new ReferenceGraph(m.getComponentList());
//        var rf = rg.reachableFrom(my);
//        assertThat(rf).contains(my, nc);
//        assertThat(rf).doesNotContain(j);
//     }
    
}
