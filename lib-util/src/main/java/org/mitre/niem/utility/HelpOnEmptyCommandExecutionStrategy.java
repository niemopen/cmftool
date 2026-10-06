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
package org.mitre.niem.utility;

import picocli.CommandLine;

public class HelpOnEmptyCommandExecutionStrategy implements CommandLine.IExecutionStrategy {

    private final CommandLine.IExecutionStrategy delegate = new CommandLine.RunLast();

    @Override
    public int execute(CommandLine.ParseResult parseResult)
        throws CommandLine.ExecutionException, CommandLine.ParameterException {

        var chain = parseResult.asCommandLineList();
        CommandLine leaf = chain.get(chain.size() - 1);
        CommandLine.ParseResult leafResult = leaf.getParseResult();

        boolean noArgsForLeaf =
            leafResult.matchedOptions().isEmpty()
                && leafResult.matchedPositionals().isEmpty()
                && !leafResult.hasSubcommand();

        boolean isRootCommand = chain.size() == 1;
        boolean isHelpSubcommand = "help".equals(leaf.getCommandName());

        // Let the root command run so CMFTool.run() can print banner + usage.
        // Also let the explicit help subcommand run normally.
        if (noArgsForLeaf && !isRootCommand && !isHelpSubcommand) {
            leaf.usage(leaf.getOut());
            return leaf.getCommandSpec().exitCodeOnUsageHelp();
        }

        return delegate.execute(parseResult);
    }
}
