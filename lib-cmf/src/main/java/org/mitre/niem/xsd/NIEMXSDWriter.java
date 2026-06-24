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

import java.util.Objects;
import org.mitre.niem.xml.XSDWriter;
import org.w3c.dom.Element;

/**
 * XML Schema writer with additional NIEM appinfo-specific attribute ordering.
 *
 * <p>The constructor takes the QName prefix used for NIEM appinfo elements.
 * For example, if the prefix is {@code "appinfo"}, then:
 *
 * <ul>
 *   <li>{@code appinfo:LocalTerm}: {@code term}, then all others</li>
 *   <li>{@code appinfo:Augmentation}: {@code class}, {@code property},
 *       {@code use}, {@code globalClassCode}, then all others</li>
 * </ul>
 *
 * <p>All normal {@link XSDWriter} ordering rules still apply to other elements.
 */
public class NIEMXSDWriter extends XSDWriter {

    private final String appinfoPrefix;

    public NIEMXSDWriter(String appinfoPrefix) {
        super();
        this.appinfoPrefix = Objects.requireNonNull(appinfoPrefix, "appinfoPrefix must not be null");
    }

    @Override
    protected int attributeRank(Element elem, String attrName) {
        if (elem == null || attrName == null) {
            return super.attributeRank(elem, attrName);
        }

        String prefix = elementPrefix(elem);
        String local = elementLocalName(elem);

        if (appinfoPrefix.equals(prefix)) {
            if ("LocalTerm".equals(local)) {
                return rank(attrName, "term");
            }

            if ("Augmentation".equals(local)) {
                return rank(attrName, "class", "property", "use", "globalClassCode");
            }
        }

        return super.attributeRank(elem, attrName);
    }

    protected String elementPrefix(Element elem) {
        String pfx = elem.getPrefix();
        if (pfx != null) return pfx;

        String tn = elem.getTagName();
        int c = tn.indexOf(':');
        return c >= 0 ? tn.substring(0, c) : "";
    }

    protected String elementLocalName(Element elem) {
        String ln = elem.getLocalName();
        if (ln != null) return ln;

        String tn = elem.getTagName();
        int c = tn.indexOf(':');
        return c >= 0 ? tn.substring(c + 1) : tn;
    }
}

