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
package org.mitre.niem.xsd;

import java.util.List;
import java.util.Map;
import static org.mitre.niem.xml.XMLSchemaDocument.makeQN;
import org.mitre.niem.xml.XSDWriter;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NIEMXSDWriter extends XSDWriter {
    
    public NIEMXSDWriter () { }
    
    // Customize XSDWriter with attribute reorderings for NIEM XSD.
    // The appinfo namespace might have a funky prefix (supplied as aPre).
    // <appinfo:LocalTerm>:    order is @term, then others
    // <appinfo:Augmentation>: order is @class, @property, @use, @globalClassCode
    public NIEMXSDWriter (String aPre) {
        super();
        var ltQ  = makeQN(aPre, "LocalTerm");       // appinfo:LocalTerm
        var augQ = makeQN(aPre, "Augmentation");    // appinfo:Augmentation
        reorderMap.add(ltQ, "term");
        reorderMap.addAll(augQ, List.of("class", "property", "use", "globalClassCode"));
    }
}
