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

import java.math.BigInteger;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * A class for a natural order comparison of strings with numbers.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NaturalOrderIgnoreCaseComparator implements Comparator<String> {

    private static final Pattern PAT = Pattern.compile("(\\d+|\\D+)");
    private static final NaturalOrderIgnoreCaseComparator NOC = new NaturalOrderIgnoreCaseComparator();
    
    public static int comp (String one, String two) {
        return NOC.compare(one, two);
    }
    
    @Override
    public int compare(String one, String two) {
        var mOne = PAT.matcher(one);
        var mTwo = PAT.matcher(two);
        while (mOne.find() && mTwo.find()) {
            var sOne = mOne.group(); 
            var sTwo = mTwo.group();
            int res;
            if (isNumeric(sOne) && isNumeric(sTwo)) {
                if (sOne.length() > 8 || sTwo.length() > 8) {
                    var bOne = new BigInteger(sOne);
                    var bTwo = new BigInteger(sTwo);
                    res = bOne.compareTo(bTwo);
                }
                else res = Integer.compare(Integer.parseInt(sOne), Integer.parseInt(sTwo));
           }
           else res = sOne.compareToIgnoreCase(sTwo);
           if (res != 0) return res;
        }
        return mOne.hitEnd() && mTwo.hitEnd() ? 0 : mOne.hitEnd() ? -1 : 1;
    }
    
    private boolean isNumeric (String s) {
        return s.chars().allMatch(Character::isDigit);
    }
}
