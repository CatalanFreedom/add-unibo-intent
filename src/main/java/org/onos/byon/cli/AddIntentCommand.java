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
import org.onos.byon.NetworkService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.HostId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner; //Importación del código de la clase Scanner desde la biblioteca Java

/**
 * CLI to add a host to a network.
 */
@Command(scope = "byon", name = "add-intent", description = "Add an intent to a network")
public class AddIntentCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostIdSrc", description = "Host Id Source",
            required = false, multiValued = false)
    String hostIdSrc = null;

    @Argument(index = 1, name = "hostToPass", description = "Host to cross",
            required = false, multiValued = false)
    String hostToPass = null;

    @Argument(index = 2, name = "hostIdDst", description = "Host Id Destination",
            required = false, multiValued = false)
    String hostIdDst = null;

    @Override
    protected void execute() {
    /*    print("Configuring your networks");
        print("You have two different networks: 10.10.10.0/24 and 10.0.0.0/30");
        print("Add a gw for the first Network");

        String entradaTeclado = "";
        Scanner entradaEscaner = new Scanner (System.in); //Creación de un objeto Scanner
        entradaTeclado = entradaEscaner.nextLine (); //Invocamos un método sobre un objeto Scanner
        System.out.println ("Entrada recibida por teclado es: \"" + entradaTeclado +"\"");

        print("Now add a gw for the second Network");
        try {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            entradaTeclado = stdin.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println ("Entrada recibida por teclado es: \"" + entradaTeclado +"\"");
*/
        NetworkService networkService = get(NetworkService.class);
        networkService.addManualIntent(HostId.hostId(hostIdSrc), hostToPass, HostId.hostId(hostIdDst));
//                addHost(network, HostId.hostId(hostId));
//        print("Added intent from %s to %s, with a waypoint in %s.", hostIdSrc, hostToPass, hostIdDst);
        print("Added intent");
    }
}
