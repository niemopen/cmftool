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


import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class CmdLineLogCapture {
    
    private List<String> msgs     = new ArrayList<>();
    private final LoggerContext context = (LoggerContext) LogManager.getContext(false);
    private final Configuration config  = context.getConfiguration();
    private final Layout<?> layout      = PatternLayout.newBuilder().withPattern("%m").build();
    private final Appender appender     = new Appender("CmdLineLogCapture", layout);
    
    public CmdLineLogCapture () { }
    
    public void startCapture () {
        appender.start();
    }
    
    public void stopCapture () {
        appender.stop();
    }
    
    public List<String> messages ()     { return msgs; }
    
    public void clear ()                { msgs = new ArrayList<>(); }
    
    private class Appender extends AbstractAppender {
        
        protected Appender (String name, Layout<?> layout) { 
            super(name, null, layout, false);
        }

        @Override
        public void append(LogEvent event) {
            var lvl = String.format("[%s]", event.getLevel());
            var msg = String.format("%-5.5s %s", lvl, event.getMessage().getFormattedMessage());
            msgs.add(msg);
        }
    }
}
