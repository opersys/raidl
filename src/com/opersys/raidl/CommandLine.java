/*
 * Copyright (C) 2014 Opersys inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opersys.raidl;

import java.util.*;

public class CommandLine {

    private LinkedList<String> finalArgs;

    private Set<String> passedOpts;

    public boolean hasOption(String opts) {
        return passedOpts.contains(opts);
    }

    public String[] getArgs() {
        return finalArgs.toArray(new String[finalArgs.size()]);
    }

    public CommandLine(String[] args, String ... opts) throws CommandLineException {
        Set<String> validOpts;
        LinkedList<String> rawArgsList;

        this.finalArgs = new LinkedList<String>();
        this.passedOpts = new TreeSet<String>();

        rawArgsList = new LinkedList<String>(Arrays.asList(args));
        validOpts = new TreeSet<String>(Arrays.asList(opts));

        for (String arg : rawArgsList) {
            if (validOpts.contains(arg))
                this.passedOpts.add(arg);
            else
                if (arg.startsWith("-"))
                    throw new CommandLineException(arg);
                else
                    finalArgs.add(arg);
        }
    }
}
