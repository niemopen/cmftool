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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class FileCompare {
    
    static public String compareIgnoringTrailingWhitespace (File f1, File f2) {
        try {
            FileReader fr1 = new FileReader(f1);
            FileReader fr2 = new FileReader(f2);
            BufferedReader br1 = new BufferedReader(fr1);
            BufferedReader br2 = new BufferedReader(fr2);
            String line1 = br1.readLine();
            String line2 = br2.readLine();
            int ln = 1;
            while (null != line1 && null != line2) {
                line1 = line1.stripTrailing();
                line2 = line2.stripTrailing();
                if (!line1.equals(line2)) return "files differ, line "+ln;
                line1 = br1.readLine();
                line2 = br2.readLine();
                ln++;
            }
            if (null != line1 || null != line2) return "files differ, line "+ln;
            return null;
        } catch (Exception ex) {
            return ex.getLocalizedMessage();
        }
    }
}
