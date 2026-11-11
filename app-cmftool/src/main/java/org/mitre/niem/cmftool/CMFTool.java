/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
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

import java.util.ArrayList;
import java.util.List;
import org.mitre.niem.utility.BuildInfo;
import org.mitre.niem.utility.HelpOnEmptyCommandExecutionStrategy;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
    name = "cmftool",
    mixinStandardHelpOptions = true,
    versionProvider = CMFTool.ToolVersionProvider.class
)
public class CMFTool implements Runnable {

    private static final String BANNER_SECTION_KEY = "banner";

    @Spec
    CommandSpec spec;

    public static void main(String[] args) {
        int rc = newCommandLine().execute(args);
        System.exit(rc);
    }

    private static CommandLine newCommandLine() {
        CommandLine cmd = new CommandLine(new CMFTool());

        cmd.addSubcommand("x2m", new CmdXSDtoCMF());
        cmd.addSubcommand("m2x", new CmdCMFtoXSDModel());
        cmd.addSubcommand("m2xmsg", new CmdCMFtoXMLSchema());
        cmd.addSubcommand("m2jmsg", new CmdCMFtoJSONSchema());
        cmd.addSubcommand("m2m", new CmdCMFtoCMF());
        cmd.addSubcommand("m2r", new CmdCMFtoRDF());
        cmd.addSubcommand("m2map", new CmdCMFtoMapping());
        cmd.addSubcommand("m2context", new CmdCMFtoContext());
        cmd.addSubcommand("mval", new CmdCMFValidate());
//      cmd.addSubcommand("xval", new CmdXSDValidate());
        cmd.addSubcommand("xcanon", new CmdXSDCanonicalize());
        cmd.addSubcommand("help", new CommandHelp());

        configureCommandTree(cmd, 100);
        cmd.setExecutionStrategy(new HelpOnEmptyCommandExecutionStrategy());

        return cmd;
    }

    private static void configureCommandTree(CommandLine cmd, int width) {
        cmd.getCommandSpec().usageMessage().width(width);
        cmd.getCommandSpec().versionProvider(new ToolVersionProvider());
        installBannerSection(cmd);

        for (CommandLine sub : cmd.getSubcommands().values()) {
            configureCommandTree(sub, width);
        }
    }

    private static void installBannerSection(CommandLine cmd) {
        var usage = cmd.getCommandSpec().usageMessage();
        usage.sectionMap().put(BANNER_SECTION_KEY, help -> bannerText());

        var keys = new ArrayList<>(usage.sectionKeys());
        keys.remove(BANNER_SECTION_KEY);
        keys.add(0, BANNER_SECTION_KEY);
        usage.sectionKeys(keys);
    }

    private static String bannerText() {
        BuildInfo info = BuildInfo.forClass(CMFTool.class);
        return String.format(
            "Version: %s (%s)%n" +
            "Suggestions and bug reports: https://github.com/niemopen/cmftool/issues%n%n",
            info.getImplementationVersion(),
            info.getBuildDate()
        );
    }

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public static class ToolVersionProvider implements IVersionProvider {
        @Override
        public String[] getVersion() {
            BuildInfo info = BuildInfo.forClass(CMFTool.class);
            return new String[] {
                String.format("%s (%s)", info.getImplementationVersion(), info.getBuildDate())
            };
        }
    }

    @Command(
        name = "help",
        description = "this list of cmftool commands",
        mixinStandardHelpOptions = true
    )
    static class CommandHelp implements java.util.concurrent.Callable<Integer> {

        @Spec
        CommandSpec spec;

        @Parameters(
            arity = "0..1",
            description = "display help for this command"
        )
        List<String> helpArgs = new ArrayList<>();

        @Override
        public Integer call() {
            CommandLine root = spec.root().commandLine();

            if (helpArgs != null && !helpArgs.isEmpty()) {
                String cmdName = helpArgs.get(0);
                CommandLine sub = root.getSubcommands().get(cmdName);
                if (sub == null) {
                    root.getErr().println("Unknown command: " + cmdName);
                    root.usage(root.getOut());
                    return 2;
                }
                sub.usage(root.getOut());
                return 0;
            }

            root.usage(root.getOut());
            return 0;
        }
    }
}
