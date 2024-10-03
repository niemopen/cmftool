/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2024 The MITRE Corporation.
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
package org.mitre.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Obtains a resource stream from src/main/resources (if executing from IDE),
 * or from the JAR (if not).
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Resource {
    private static final Logger LOG = LogManager.getLogger(Resource.class);
    private static Resource obj = new Resource();
    
    private final String jarPath;
    private final String resPath;
    
    protected Resource () {
        jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        resPath = FilenameUtils.concat(jarPath, "../../../../src/main/resources/");
    }
    
    public static URI getURI (String rpath) {
        return obj.uri(rpath);
    }
    
    public static InputStream getStream (String rpath) {
        return obj.stream(rpath);
    }
    
    private URI uri (String rpath) {
        if (jarPath.endsWith(".jar")) {
            try {
                var result = getClass().getResource(rpath).toURI();
                return result;
            } catch (URISyntaxException ex) {
                LOG.error(ex.getMessage());
            }
            return null;
        }
        var resF = new File(resPath, rpath);
        return resF.toURI();
    }
    
    private InputStream stream (String rpath) {
        if (jarPath.endsWith(".jar")) {
            var istr = getClass().getResourceAsStream(rpath);
            if (null == istr) LOG.error("can't obtain resource {} from JAR", rpath);
            return istr;
        }
        var resF = new File(resPath, rpath);
        try {
            var istr = new FileInputStream(resF);
            return istr;
        } catch (FileNotFoundException ex) {
            LOG.error("can't obtain resource {}: {}", rpath, ex.getMessage());
        }
        return null;
    }
}
