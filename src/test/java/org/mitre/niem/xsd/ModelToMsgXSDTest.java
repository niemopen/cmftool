/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2024 The MITRE Corporation.
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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.cmf.NamespaceKind;

/**
 *
 * @author sar
 */
public class ModelToMsgXSDTest {
    
    public ModelToMsgXSDTest() {
    }

    @Test
    public void testFixConformanceTargets () {
        NamespaceKind.reset();
        var mw   = new ModelToMsgXSD(null);
        var ctas = "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument";
        var rv   = mw.fixConformanceTargets(ctas);
        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
        
        ctas = "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument";
        rv   = mw.fixConformanceTargets(ctas);
        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
        
        ctas = "http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument";
        rv   = mw.fixConformanceTargets(ctas);
        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#ReferenceSchemaDocument");
        
        ctas = "http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument " +
               "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument " +
               "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument";
        rv   = mw.fixConformanceTargets(ctas);
        var rvs = rv.split("\\s+");
        assertEquals(rvs[0], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#ReferenceSchemaDocument");
        assertEquals(rvs[1], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
        assertEquals(rvs[2], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");

        int i = 0;
    }
    
}
