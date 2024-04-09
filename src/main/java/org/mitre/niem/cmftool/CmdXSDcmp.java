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
package org.mitre.niem.cmftool;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import static org.apache.xerces.xs.XSConstants.ATTRIBUTE_DECLARATION;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.TYPE_DEFINITION;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.mitre.niem.xsd.XMLSchema;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Parameters(commandDescription = "compare two XML schemas")

public class CmdXSDcmp implements JCCommand {
    @Parameter(names = {"-d","--debug"}, description = "turn on debug logging")
    private boolean debugFlag = false;
    
    @Parameter(names = "-q", description = "no output, exit status only")
    private boolean quiet = false;
    
    @Parameter(names = "-s", description = "filename separator character")
    private String fsep = ","; 
    
    @Parameter(names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
        
    @Parameter(description = "s1.xsd,cat1.xml,... s2.xsd,cat2.xml,...")
    private List<String> mainArgs;
    
    static final Logger LOG = LogManager.getLogger(CmdXSDcmp.class);    
    
    int diffCt = 0;
        
    CmdXSDcmp () {
    }
  
    CmdXSDcmp (JCommander jc) {
    }

    public static void main (String[] args) {       
        CmdXSDcmp obj = new CmdXSDcmp();
        obj.runMain(args);
    }
    
    @Override
    public void runMain (String[] args) {
        JCommander jc = new JCommander(this);
        CMFUsageFormatter uf = new CMFUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.setProgramName("XSDcmp");
        jc.parse(args);
        run(jc);
    }
    
    @Override
    public void runCommand (JCommander cob) {
        cob.setProgramName("cmftool xcmp");
        run(cob);
    }    
    
    private void run (JCommander cob) {
        if (debugFlag) {
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
        }
        if (help) {
            cob.usage();
            System.exit(0);
        }
        if (mainArgs == null || mainArgs.isEmpty()) {
            cob.usage();
            System.exit(2);
        }
        if (mainArgs.get(0).startsWith("-s") && mainArgs.get(0).length() == 3) {
            String s = mainArgs.remove(0);
            fsep = s.substring(2);
        }        
        if (mainArgs.isEmpty()) {
            cob.usage();
            System.exit(1);            
        }        
        // Single argument of "-" signals end of arguments, allows filename "-foo"
        String na = mainArgs.get(0);
        if (na.startsWith("-")) {
            if (na.length() == 1) {
                mainArgs.remove(0);
            } else {
                System.out.println("unknown option: " + na);
                cob.usage();
                System.exit(2);
            }
        }     
        if (2 != mainArgs.size()) {
            cob.usage();
            System.exit(2);
        }
        XSModel xsOne = genXSModel(mainArgs.get(0));
        XSModel xsTwo = genXSModel(mainArgs.get(1));
        
        XSNamedMap mOne = xsOne.getComponents(TYPE_DEFINITION);
        XSNamedMap mTwo = xsTwo.getComponents(TYPE_DEFINITION); 
        compareMaps(mOne, mTwo);
        
        mOne = xsOne.getComponents(ELEMENT_DECLARATION);
        mTwo = xsTwo.getComponents(ELEMENT_DECLARATION);
        compareMaps(mOne, mTwo);
        
        mOne = xsOne.getComponents(ATTRIBUTE_DECLARATION);
        mTwo = xsTwo.getComponents(ATTRIBUTE_DECLARATION);
        compareMaps(mOne, mTwo);
        
        System.exit(0 == diffCt ? 0 : 1);
    }
    
    private XSModel genXSModel (String argS) {
        String[] argL = argS.split(fsep);
        XMLSchema s = null;
        try {
            s = new XMLSchema(argL);
        } catch (IOException ex) {
            System.out.println("Could not generate schema from '" + argS + "': " + ex.getMessage());
            System.exit(2);
        } catch (XMLSchema.XMLSchemaException ex) {
            java.util.logging.Logger.getLogger(CmdXSDcmp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        XSModel xm = s.xsmodel();
        List<String> msgs = s.xsModelMsgs();
        if (null == xm || !msgs.isEmpty()) {
            System.out.println("Generating schema from '" + argS + "'");
            for (String m : msgs) System.out.println(m);
            if (null == xm) {
                System.out.println("Could not generate XSModel");
                System.exit(2);
            }
        }
        return xm;
    }     
    
    private void compareMaps (XSNamedMap mOne, XSNamedMap mTwo) {
        List<XSObject> obsOne = objectList(mOne);
        List<XSObject> obsTwo = objectList(mTwo);
        obsOne.sort(Comparator.comparing(XSObject::getNamespace).thenComparing(XSObject::getName));
        obsTwo.sort(Comparator.comparing(XSObject::getNamespace).thenComparing(XSObject::getName));
        LOG.debug("one:"); for (var x : obsOne) LOG.debug(clarkName(x));
        LOG.debug("two:"); for (var x : obsTwo) LOG.debug(clarkName(x));
        int iOne = 0;
        int iTwo = 0;
        while (iOne < obsOne.size() && iTwo < obsTwo.size()) {
            XSObject oOne = obsOne.get(iOne);
            XSObject oTwo = obsTwo.get(iTwo);
            String nOne = clarkName(oOne);
            String nTwo = clarkName(oTwo);
            int cr = nOne.compareTo(nTwo);
            if (cr < 0) {
                reportDiff("Only in schema one: " + clarkName(oOne));
                iOne++;
            }
            else if (cr > 0) {
                reportDiff("Only in schema two: " + clarkName(oTwo));
                iTwo++;
            }
            else {
                if (oOne.getType() != oTwo.getType())
                    reportDiff("Different types for " + clarkName(oOne));
                else
                    compareObjects(oOne, oTwo);
                iOne++;
                iTwo++;
            }
        }
        while (iOne < obsOne.size()) {
            reportDiff("Only in schema one: " + clarkName(obsOne.get(iOne++)));
        }
        while (iTwo < obsTwo.size()) {
            reportDiff("Only in schema one: " + clarkName(obsTwo.get(iOne++)));
        }        
    }
    
    private List<XSObject> objectList (XSNamedMap map) {
        List<XSObject> res = new ArrayList<>();
        for (int i = 0; i < map.getLength(); i++) {
            res.add(map.item(i));
        }
        return res;
    }
    
    private String clarkName (XSObject o) {
        return String.format("{%s}%s", o.getNamespace(), o.getName());
    }
    
    private void reportDiff (String msg) {
        System.out.println(msg);
        diffCt++;
    }
    
    private void compareObjects (XSObject oOne, XSObject oTwo) {
        
    }

}
