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
package org.mitre.niem.utility;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Convenience functions for working with file URIs
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class URIfuncs {

    /**
     * Returns a File object from a URI object with a file scheme
     * Returns null if it's not a file URI.
     */
    static public File URItoFile (URI uri) {              
        if (!"file".equals(uri.getScheme())) return null;
        return new File(uri.getPath());
    }
    
    /**
     * Returns a File object from a string containing a file URI.
     * Returns null if the string isn't a valid file URI.
     */
    static public File URIStringToFile (String u) {
        URI uri;
        try { uri = new URI(u); } catch (Exception ex) { return null; } // IGNORE
        if (!"file".equals(uri.getScheme())) return null;
        var path = uri.getPath();
               
        return new File(uri.getPath());
    }
    
    /**
     * Returns a file URI object for a canonicalized File.  The URI object will
     * thus have an absolute path.
     */
    static public URI FileToCanonicalURI (File f) {
        var rF = f;
        try { rF = f.getCanonicalFile(); } catch (IOException ex) {} // IGNORE
        return rF.toURI();
    }
}
