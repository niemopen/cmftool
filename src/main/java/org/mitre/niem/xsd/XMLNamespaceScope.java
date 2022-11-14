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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.javatuples.Pair;

/**
 * Works with a SAX parser to maintain the namespace declarations in scope.  For
 * applications that need to resolve QNames that appear in the XML data.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLNamespaceScope {
    private final Stack<Boolean> scopeHasMapping = new Stack<>();
    private final Stack<Map<String, String>> scopes = new Stack<>();
    private Map<String, String> currentMap = new HashMap<>();
    private boolean mapFlag = false;

    public XMLNamespaceScope() {
    }

    /**
     * Call this from ContentHandler.startElement.
     */
    public void onStartElement () {
        scopeHasMapping.push(mapFlag);
        if (mapFlag) {
            scopes.push(currentMap);
            currentMap = new HashMap<>();
            currentMap.putAll(scopes.peek());
        }
        mapFlag = false;
    }

    /**
     * Call this from ContentHandler.endElement. Way simpler than trying to
     * cope with individual endPrefixMapping events, and you wind up in the
     * same place.
     */
    public void onEndElement () {
        if (scopeHasMapping.peek()) {
            currentMap = scopes.pop();
        }
        scopeHasMapping.pop();
    }

    /**
     * Call this from ContentHandler.startPrefixMapping.
     *
     * @param prefix
     * @param uri
     */
    public void onStartPrefixMapping (String prefix, String uri) {
        currentMap.put(prefix, uri);
        mapFlag = true;
    }

    /**
     * Expand a QName into a <namespaceURI,localName> pair. Returns null if
     * the argument is not a valid QName or has no binding in the current scope.
     * @param str QName to expand
     * @return <namespaceURI,localName> or null
     */
    public Pair<String, String> expandQName (String str) {
        if (null == str) return null;
        int cpos = str.indexOf(":");
        if (cpos < 0) return Pair.with(null, str);
        if (cpos == str.length() - 1) return null;
        
        String prefix = str.substring(0, cpos);
        String lname = str.substring(cpos + 1);
        if (0 <= lname.indexOf(":")) return null;
        String uri = currentMap.get(prefix);
        if (null == uri) return null;
        return Pair.with(uri, lname);
    }

    public class XMLNamespaceMapException extends Exception {
        public XMLNamespaceMapException(String s) { super(s); }
    }
}
