/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2021 The MITRE Corporation.
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

import org.mitre.niem.nmf.ObjectType;

/**
 * A record for data from a model XML file that is not part of the wrapped
 * model object.  The data describes a child model object to be added to
 * some parent object.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLDataRecord {
    public ObjectType obj    = null;    // data in this record is for this model object
    public String stringVal  = null;    // for a simple element, the element string contents
    public int index         = -1;      // when replacing a reference placeholder in a list, this is its index
    public int lineNumber    = 0;       // starting line # of XML element (for diagnostic messages)     
        
    XMLDataRecord () { }
    XMLDataRecord (ObjectType o, int line) {
        obj = o;
        lineNumber = line;
    }
}
