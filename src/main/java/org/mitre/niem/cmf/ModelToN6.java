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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import static org.mitre.niem.cmf.NamespaceKind.NSK_CORE;

/**
 * A class to convert a Model object to the NIEM 6 architecture
 * Rewrites namespace URIs to the OASIS namespaces
 * Adds nc:ObjectType, nc:AssociationType, and sets object inheritance
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToN6 {
    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(ModelToN6.class);
    
    private static final String coreURI = "https://docs.oasis-open.org/niemopen/niem-core/6.0/#";
    private static final String oasis   = "https://docs.oasis-open.org/niemopen/";
    private static final Pattern nspat  = Pattern.compile("http://((reference)|(release)|(publication))\\.niem\\.gov/niem/");
    private static final Pattern vers   = Pattern.compile("/\\d\\.\\d/");
    
    public ModelToN6 () { }
    
    public void convert (Model m) throws CMFException {
        
        // Change the namespace URIs
        for (var ns : m.getNamespaceList()) {
            var ouri = ns.getNamespaceURI();
            var nuri = ouri;
            Matcher match = nspat.matcher(ouri);
            if (match.lookingAt()) {
                nuri = match.replaceFirst(oasis);
                if (!nuri.endsWith("#")) nuri = nuri + "#";
                match = vers.matcher(nuri);
                if (match.find()) nuri = match.replaceFirst("/6.0/");
            }
            try {
                ns.setNamespaceURI(nuri);
            } catch (CMFException ex) {
                LOG.error(String.format("Could not replace namespace URI %s with %s: %s",
                        ouri, nuri, ex.getMessage()));
            }
        }
        // Find NIEM Core namespace object
        Namespace coreNS = null;
        for (var ns : m.getNamespaceList()) {
            if (NSK_CORE == NamespaceKind.kind(ns.getNamespaceURI())) {
                coreNS = ns;
                break;
            }
        }
        // Create NIEM Core namespace if necessary
        if (null == coreNS) {
            coreNS = new Namespace("nc", coreURI);
            coreNS.setDefinition("NIEM Core");
            m.addNamespace(coreNS);
        }
        // Add nc:ObjectType and nc:AssociationType if necessary
        var ncObjectType = m.getClassType(coreURI, "ObjectType");
        if (null == ncObjectType) {
            ncObjectType = new ClassType(coreNS, "ObjectType");
            ncObjectType.setDefinition("a data type for a thing with its own lifespan that has some existence.");
            m.addComponent(ncObjectType);
        }
        var ncAssocType = m.getClassType(coreURI, "AssociationType");
        if (null == ncAssocType) {
            ncAssocType = new ClassType(coreNS, "AssociationType");
            ncAssocType.setDefinition("A data type for a relationship between two or more objects, including any properties of that relationship.");
            m.addComponent(ncAssocType);            
        }
        // All NIEM classes extend nc:ObjectType or nc:AssociationType
        for (var c : m.getComponentList()) {
            var cl = c.asClassType();
            if (null == cl) continue;
            if (ncObjectType == cl || ncAssocType == cl) continue;
            if (null == cl.getExtensionOfClass()) {
                var cname = cl.getName();
                if (cname.endsWith("AssociationType")) cl.setExtensionOfClass(ncAssocType);
                else cl.setExtensionOfClass(ncObjectType);
            }   
        }
        // Fix the SchemaDocument objects
        var sdocs = new ArrayList<SchemaDocument>();
        sdocs.addAll(m.schemadoc().values());
        for (var sd : sdocs) {
            var olduri = sd.targetNS();
            var prefix = sd.targetPrefix();
            var nsuri  = m.namespaceMap().getURI(prefix);
            if (null == nsuri) {
                var util = NamespaceKind.utilityKind(sd.targetNS());
                nsuri = NamespaceKind.getUtilityNS(util, "6");
            }
            m.schemadoc().remove(olduri);
            m.schemadoc().put(nsuri, sd);
            sd.setTargetNS(nsuri);
            sd.setNIEMversion("6");
            var nct = new StringBuilder();
            var cts = sd.confTargets();
            if (null == cts) continue;
            var ctl = cts.split("\\s+");
            var sep = "";
            for (int i = 0; i < ctl.length; i++) {
                var ct = ctl[i];
                var match = nspat.matcher(ct);
                if (match.lookingAt()) ct = match.replaceFirst(oasis);
                match = vers.matcher(ct);
                if (match.find()) ct = match.replaceFirst("/6.0/");
                nct.append(sep).append(ct);
                sep = " ";
            }
            sd.setConfTargets(nct.toString());
        }
    }
}
