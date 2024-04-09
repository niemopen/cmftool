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

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class SAXErrorHandler implements ErrorHandler {
    
    private final List<String> messages = new ArrayList<>();
    
    public SAXErrorHandler () { 
        super();
    }
    
    public List<String> messages ()     { return messages; }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        genMsg("[warn] ", e);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        genMsg("[error]", e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        genMsg("[fatal]", e);
    }

    private void genMsg(String kind, SAXParseException e) {
        String uri = e.getSystemId();
        String fn = "";
        if (uri != null) {
            int index = uri.lastIndexOf('/');
            if (index != -1) {
                fn = uri.substring(index + 1) + ":";
            }
        }
        messages.add(String.format("%s %s %d:%d %s",
                kind,
                fn,
                e.getLineNumber(),
                e.getColumnNumber(),
                e.getMessage()));
    }
}
