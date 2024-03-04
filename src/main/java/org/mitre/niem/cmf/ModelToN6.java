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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_NOTBUILTIN;

/**
 * A class to convert a Model object to the NIEM 6 architecture
 * Rewrites namespace URIs to the OASIS namespaces
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToN6 {
    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(ModelToN6.class);
    
    // Replacements for old NIEM builtin namespace URIs:
    // URI matching utilpat[N] is replaced with utilrep[N]
    private static final List<Pattern> utilpat = new ArrayList<>();
    private static final String[] utilpdat = {
      "http://release.niem.gov/niem/appinfo/\\d\\.\\d/",   
      "http://reference.niem.gov/niem/specification/code-lists/\\d\\.\\d/code-lists-instance/",
      "http://reference.niem.gov/niem/specification/code-lists/\\d\\.\\d/code-lists-schema-appinfo/",
      "http://release.niem.gov/niem/conformanceTargets/\\d\\.\\d/",
      "http://release.niem.gov/niem/proxy/niem-xs/\\d\\.\\d/",
      "http://release.niem.gov/niem/structures/\\d\\.\\d/"
    };
    private static final String[] utilrep = {      
      "https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/",   
      "https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-instance/",
      "https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-schema-appinfo/",
      "https://docs.oasis-open.org/niemopen/ns/specification/conformanceTargets/6.0/",
      "https://docs.oasis-open.org/niemopen/ns/model/adapters/niem-xs/6.0/",
      "https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/",      
    };
    
    private static final String OASIS_PREFIX   = "https://docs.oasis-open.org/niemopen/ns/";
    private static final Pattern NIEMGOV_PAT   = Pattern.compile("http://((reference)|(release)|(publication))\\.niem\\.gov/niem/(?<spec>specification/)?");
    private static final Pattern VERSION_PAT   = Pattern.compile("/\\d+\\.0/");
    
    public ModelToN6 () { 
        for (int i = 0; i < utilpdat.length; i++) 
            utilpat.add(Pattern.compile(utilpdat[i]));
    }
    
    public void convert (Model m) throws CMFException {
//        
//        // Change namespace URI in model namespace objects
//        // Iterate over copied list of namespace objects 
//        // Remember URI changes, fix SchemaDocument objeccts later
//        Map<String,String> newURI = new HashMap<>();
//        List<Namespace> nslist    = new ArrayList<>();
//        nslist.addAll(m.getNamespaceList());
//        for (var ns : nslist) {
//            String ouri = ns.getNamespaceURI();
//            String nuri = changeURI(ouri);
//            if (null != nuri) {
//                try {
//                    ns.setNamespaceURI(nuri);
//                    newURI.put(ouri, nuri);                
//                } catch (CMFException ex) {
//                    LOG.error(String.format("Could not replace namespace URI %s with %s: %s",
//                            ouri, nuri, ex.getMessage()));
//                }
//                newURI.put(ouri, nuri);
//                LOG.debug(String.format("NS %-40.40s -> %s\n", ouri, nuri));
//            }
//        }
//        // Fix the SchemaDocument objects
//        var sdocs = new ArrayList<SchemaDocument>();
//        sdocs.addAll(m.schemadoc().values());
//        for (var sd : sdocs) {
//            var ouri = sd.targetNS();
//            var nuri = newURI.get(ouri);
//            
//            // Some SchemaDocument objects don't have corresponding Namespace objects
//            // Fix them here
//            if (null == nuri) {
//                var builtin = NamespaceKind.builtin(sd.targetNS());
//                if (builtin < NIEM_NOTBUILTIN) 
//                nuri = NamespaceKind.getBuiltinNS(builtin, "NIEM6", "6");
//            
//            }
//            sd.setTargetNS(nuri);
//            sd.setNIEMversion("6");
//            m.schemadoc().remove(ouri);
//            m.schemadoc().put(nuri, sd);
//            LOG.debug(String.format("SD %-40.40s -> %s\n", ouri, nuri));     
//            
//            // Change the conformance targets to NIEM 6
//            var ncts = new StringBuilder();
//            var cts = sd.confTargets();
//            if (null == cts) continue;
//            var ctl = cts.split("\\s+");
//            var sep = "";
//            for (int i = 0; i < ctl.length; i++) {
//                var ct = ctl[i];
//                var nct = changeURI(ct);
//                nct = nct.replaceFirst("/naming-and-design-rules/", "/XNDR/");
//                var match = VERSION_PAT.matcher(ct);
//                if (match.find()) ct = match.replaceFirst("/6.0/");
//                ncts.append(sep);
//                ncts.append(nct);
//                sep = " ";
//            }
//            sd.setConfTargets(ncts.toString());
//        }
    }
    
    private String changeURI(String ouri) {
        
        // Convert a builtin namespace URI
        for (int i = 0; i < utilpdat.length; i++) {
            var match = utilpat.get(i).matcher(ouri);
            if (match.matches()) return utilrep[i];
        }
        // Convert niem.gov to OASIS
        Matcher match = NIEMGOV_PAT.matcher(ouri);
        if (match.lookingAt()) {
            String nuri;
            if (null == match.group("spec")) nuri = match.replaceFirst(OASIS_PREFIX+"model/");
            else nuri = match.replaceFirst(OASIS_PREFIX+"specification/");
            match = VERSION_PAT.matcher(nuri);
            if (match.find()) {
                nuri = match.replaceFirst("/6.0/");
            }
            return nuri;
        }
        return null;
    }
}
