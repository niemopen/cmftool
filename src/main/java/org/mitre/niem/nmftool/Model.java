/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2021 The MITRE Corporation.
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
package org.mitre.niem.nmftool;

import java.io.File;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.mitre.niem.nmf.impl.ModelTypeEx;
import org.mitre.niem.nmf.impl.ObjectFactoryEx;
import org.mitre.niem.nmf.impl.ObjectTypeEx;

/**
 *
 * A class for reading and writing a NIEM model from/to a file.
 * 
 * @author Scott Renner
 *  <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Model {
    
    public static Model readXML (File f) throws JAXBException {
        
        JAXBContext jc = JAXBContext.newInstance("org.mitre.niem.nmf");
        Unmarshaller u = jc.createUnmarshaller();
        u.setProperty("org.glassfish.jaxb.core.ObjectFactory",new ObjectFactoryEx());
        u.setEventHandler(new jakarta.xml.bind.helpers.DefaultValidationEventHandler());
        
        JAXBElement e = (JAXBElement) u.unmarshal(f);
        ModelTypeEx mdat = (ModelTypeEx) e.getValue();
        
        for (ObjectTypeEx o : ObjectTypeEx.all) {
            o.dump();
        }
        
        mdat.addComponents();
        return null;
    }
}
