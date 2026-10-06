/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;

/**
  * A class for writing an XSD message schema from a Model object.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NewModelToXSD {
    private static final Logger LOG = LogManager.getLogger(NewModelToXSD.class);
    
    protected Model m;
    protected String useArchVersion = null;     // ignore versions in model, use this one
    protected String catalogPath = null;        // write XML catalog here
    protected Namespace rootNS = null;          // root namespace gets extra imports, maybe
    protected Map<String,String> pathSpec;      // user specified nsU -> file path in outD
    
    protected NewModelToXSD () { }
    
    public NewModelToXSD (Model m)     { 
        this.m = m; 
        pathSpec = new HashMap<>();
    }

    /**
     * Called to use a single architecture version in the generated schema
     * documents instead of the version in the namespace objects. This controls
     * the utility schema documents (eg. structures.xsd) in the pile. It doesn't
     * change the namespace URI of any model component. Takes valueslike
     * "NIEM5.0".
     * 
     * @param vers 
     */
    public void setArchVersion (String vers) {
        useArchVersion = vers;
    }
    
    /**
     * Called to generate an XML Catalog file at this relative path from
     * the schema document pile root.
     * 
     * @param path 
     */
    public void setCatalogPath (String path) {
        catalogPath = path;
    }   
    
    /**
     * Called to specify the "root namespace". The schema document for that
     * namespace will include extra xs:import elements as needed to ensure that
     * the entire schema can be assembled from this document alone.
     * 
     * @param nsPrefixOrURI 
     */
    public void setRootNamespace (String nsPrefixOrURI) {
        if (null == nsPrefixOrURI) return;
        rootNS = m.namespaceObj(nsPrefixOrURI);
        if (null == rootNS)
            LOG.error("Can't make '{}' the root namespace; not in model", nsPrefixOrURI);
    }
    
    /**
     * Call this to provide a relative path for schema documents in the pile,
     * in case you don't like the paths recorded in the model namespace objects
     * (or the default paths, if the model has none). The map is from namespace
     * URI to relative file path in the pile.
     * 
     * @param paths 
     */
    public void setNamespacePaths (Map<String,String> paths) {
        pathSpec = new HashMap<>(paths);
    }

    /**
     * Writes the model to an XSD pile in the specified location.  The schema 
     * document for each model namespace gets the NIEM version specified in each
     * model namespace object.
     * @param outD 
     */
    public void writeModelXSD (File outL) throws ParserConfigurationException, IOException {   
        
        collectRequiredNamespaces();
    }
    
    
    protected void collectRequiredNamespaces () {
        
    }

}
