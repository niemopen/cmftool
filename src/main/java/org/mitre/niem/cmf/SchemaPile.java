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
package org.mitre.niem.cmf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A class for recording some properties of an XML Schema document pile. 
 * Needed for the second translation in XSD -> CMF -> XSD.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class SchemaPile {
    private Map<String,SchemaDocument> sdoc = new HashMap<>();
    
    public SchemaPile () {}
    
    public void addSchemaDocument (SchemaDocument sd) {
        if (sd.uri() != null) sdoc.put(sd.uri(), sd);
    }
    
    public SchemaDocument getSchemaDocument (String uri) {
        return sdoc.get(uri);
    }
    
    public Collection<SchemaDocument> getAllSchemaDocuments () {
        return sdoc.values();
    }
}
