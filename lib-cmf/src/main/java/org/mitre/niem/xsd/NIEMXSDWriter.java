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
import static org.mitre.niem.cmf.Component.makeQN;
import org.mitre.niem.xml.XSDWriter;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NIEMXSDWriter extends XSDWriter {
    
    public NIEMXSDWriter () { }
    
    public NIEMXSDWriter (Map<String,String> bc2pre) {
        super();
        var aPre = bc2pre.get("APPINFO");
        var ltQ  = makeQN(aPre, "LocalTerm");
        var augQ = makeQN(aPre, "Augmentation");
        reorderMap.add(ltQ, "term");
        reorderMap.addAll(augQ, List.of("class", "property", "use", "globalClassCode"));
    }
}
