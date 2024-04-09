/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2023 The MITRE Corporation.
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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.utility.IndefiniteArticle.aOrAn;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class IndefiniteArticleTest {
    
    public IndefiniteArticleTest() {
    }

    @Test
    public void testAorAn() {
        assertEquals("an", aOrAn("apple"));
        assertEquals("a", aOrAn("bat"));
        assertEquals("an", aOrAn("education"));
        assertEquals("a", aOrAn("FIAT car"));
        assertEquals("an", aOrAn("FAA policy"));
        assertEquals("a", aOrAn("FAL rifle"));
        assertEquals("an", aOrAn("honest vote"));
        assertEquals("a", aOrAn("honeysuckle bush"));
        assertEquals("a", aOrAn("horoscope"));
        assertEquals("a", aOrAn("NASA employee"));
        assertEquals("an", aOrAn("NGA employee"));
        assertEquals("an", aOrAn("RDF triple"));
        assertEquals("a", aOrAn("unanimous vote"));
        assertEquals("an", aOrAn("unanticipated outcome"));
        assertEquals("a", aOrAn("used car"));
        assertEquals("an", aOrAn("800 number"));
    }
    
}
