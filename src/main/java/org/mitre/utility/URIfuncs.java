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
import java.net.URI;

/**
 * Convenience functions for working with URIs
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class URIfuncs {
    
    /**
     * Returns a File object from a string containing a file URI.
     * Returns null if the string isn't a valid file URI.
     * @param u
     * @return 
     */
    static public File URIStringToFile (String u) {
        URI uri;
        try { uri = new URI(u); } catch (Exception ex) { return null; }
        if (!"file".equals(uri.getScheme())) return null;
        var path = uri.getPath();
               
        return new File(uri.getPath());
    }
}
