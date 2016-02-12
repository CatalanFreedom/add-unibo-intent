/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onos.byon.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onos.byon.NetworkService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.ConnectivityIntentCommand;
import org.onosproject.net.HostId;

import java.util.ArrayList;
import java.util.List;

/**
 * CLI to add a host to a network.
 */
@Command(scope = "byon", name = "add-unibo-intent", description = "Add somo unibo intents to a network")
public class AddFirstUNIBOIntentCommand extends ConnectivityIntentCommand {

    @Argument(index = 0, name = "ingress", description = "Network name",
            required = true, multiValued = false)
    String ingress = null;

    @Argument(index = 1, name = "pathObject1", description = "Host Id Source",
            required = true, multiValued = false)
    String pathObject1 = null;

    @Argument(index = 2, name = "pathObject2", description = "Host Id Destination",
            required = false, multiValued = false)
    String pathObject2 = null;

    @Argument(index = 3, name = "pathObject3", description = "Way Point device",
            required = false, multiValued = false)
    String pathObject3 = null;

    @Argument(index = 4, name = "pathObject4", description = "Way Point device",
            required = false, multiValued = false)
    String pathObject4 = null;

    @Argument(index = 5, name = "pathObject5", description = "Way Point device",
            required = false, multiValued = false)
    String pathObject5 = null;

    @Option(name = "-dup", aliases = "--duplicate", description = "Duplicate the Packet to the f.e. DPI",
            required = false, multiValued = false)
    private String dpi = null;

    @Option(name = "-ddup", aliases = "--dduplicate", description = "Duplicate the Packet to the f.e. DPI in the come back also",
            required = false, multiValued = false)
    private String dpii = null;

    @Option(name = "-r", aliases = "--repeat", description = "Cross it in the come back way",
            required = false, multiValued = true)
    private String rep = null;

    @Override
    protected void execute() {
        NetworkService networkService = get(NetworkService.class);

        List<String> objectsToCross = new ArrayList<>();
        objectsToCross.add(ingress);
        objectsToCross.add(pathObject1);
        if (pathObject2 != null) {
            objectsToCross.add(pathObject2);
        }
        if (pathObject3 != null) {
            objectsToCross.add(pathObject3);
        }
        if (pathObject4 != null) {
            objectsToCross.add(pathObject4);
        }
        if (pathObject5 != null) {
            objectsToCross.add(pathObject5);
        }

        if (dpii == null) {
            networkService.addSecondUNIBOIntent(objectsToCross, dpi, true);
        } else {
            networkService.addSecondUNIBOIntent(objectsToCross, dpii, true);
        }



        if (rep != null || dpii != null) {
            String rep2 = rep.substring(1, rep.length() - 1);
            String[] rep3 = rep2.split(", ");

            List<String> objectsToCrossBack = new ArrayList<>();
            objectsToCrossBack.add(objectsToCross.get(objectsToCross.toArray().length - 1));
            for (int i=1; i<=rep3.length; i++) {
                objectsToCrossBack.add(rep3[rep3.length - i]);
            }
            objectsToCrossBack.add(ingress);

            if (dpii != null) {
                networkService.addSecondUNIBOIntent(objectsToCrossBack, dpii, false);
            } else {
                networkService.addSecondUNIBOIntent(objectsToCrossBack, null, false);
            }
        }

        print("Added the UNIBO chaining intent");
//        print("Added intent from %s to %s, with a waypoint in %s. In the network %s", ingressDeviceString, egressDeviceString, RequestPath, ReplyPath);
    }
}
