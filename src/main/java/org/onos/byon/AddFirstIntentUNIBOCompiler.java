/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onos.byon;

import org.apache.felix.scr.annotations.*;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.*;
import org.onosproject.net.intent.constraint.AsymmetricPathConstraint;
import org.onosproject.net.intent.impl.compiler.ConnectivityIntentCompiler;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static org.onosproject.net.flow.DefaultTrafficSelector.builder;

/**
 * A intent compiler for {@link HostToHostIntent}.
 */
//@Component(immediate = true)
public class AddFirstIntentUNIBOCompiler {
//        extends ConnectivityIntentCompiler<HostToHostIntent> {

    protected ApplicationId appId;
    protected PathService pathService;
    protected HostService hostService;
    protected LinkService linkService;
    protected DeviceService deviceService;
    protected TopologyService topologyService;
    protected IntentService intentService;

    public AddFirstIntentUNIBOCompiler(ApplicationId appId, PathService pathService, HostService hostService, LinkService linkService, DeviceService deviceService, TopologyService topologyService, IntentService intentService) {
        this.appId = appId;
        this.pathService = pathService;
        this.hostService = hostService;
        this.linkService = linkService;
        this.deviceService = deviceService;
        this.topologyService = topologyService;
        this.intentService = intentService;

    }

//    public List<Intent> compilerUNIBO(String ingressDeviceString, String egressDeviceString) {
    public void compilerUNIBO(String ingressDeviceString, String egressDeviceString) {

        System.out.println("PERFECT!! The edge networks have been configurated... Let's configure the Dynamic Chaining");
        System.out.println(" ");
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Your Network has the following Hosts, from which one do you want to begin the Dynamic Chaining?");
        System.out.println(".................................................");
        System.out.println(" ");

        Topology myTopo = topologyService.currentTopology();
        Iterator<TopologyCluster> clusters;
        Iterator<DeviceId> devices;
        Iterator<Link> links;


        clusters =  topologyService.getClusters(myTopo).iterator();
        while (clusters.hasNext()) { // To loop all the CLUSTERS
            TopologyCluster i = clusters.next(); // i is the CLUSTER in the present iteration
            System.out.println("Edge Network number: " + i.id().index());
            System.out.println("................................");
            devices = topologyService.getClusterDevices(myTopo, i).iterator();

            while (devices.hasNext()) { // To loop all the DEVICES in the present cluster
                DeviceId j = devices.next(); // j is the DEVICE in the present iteration
                System.out.println("Switch: " + j + ":");
                Iterator<Host> hostIterator = hostService.getConnectedHosts(j).iterator();

                while (hostIterator.hasNext()) {
                    Host l = hostIterator.next();
                    if (l.location().deviceId().equals(j)) {
                        System.out.println(l.mac() + " --- ip: " + l.ipAddresses());
                    }
                }
                System.out.println("....................");
            }
            System.out.println(" ");
            System.out.println(".................................................");
        }

        System.out.println("From which host do you want to begin your Dynamic Chaining? (mac or ip)");
        String hostToBegin = "";
        try {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            hostToBegin = stdin.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println ("The Host selected is: \"" + hostToBegin +"\"");


        String hostToFinish = "";
        System.out.println("To which host do you want to arrive in your Dynamic Chaining? (mac or ip)");
        try {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            hostToFinish = stdin.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println ("The Host selected is: \"" + hostToFinish +"\"");
        System.out.println(" ");
        System.out.println(".................................................");
        System.out.println(" ");



        System.out.println("The First edge has a WANA and/or a TC in the following positions");
        System.out.println(".................................................");
        System.out.println(" ");


        int cardinal = 1;
        clusters =  topologyService.getClusters(myTopo).iterator();

        while (clusters.hasNext()) { // To loop all the CLUSTERS
            TopologyCluster ii = clusters.next(); // i is the CLUSTER in the present iteration
            devices = topologyService.getClusterDevices(myTopo, ii).iterator();

            while (devices.hasNext()) { // To loop all the DEVICES in the present cluster
                DeviceId jj = devices.next(); // j is the DEVICE in the present iteration
                links = linkService.getDeviceLinks(jj).iterator();

                while (links.hasNext()) { // To loop all the LINKS in the present device
                    Link k = links.next(); // k is the LINK in the present iteration
                    if (k.src().deviceId().equals(k.dst().deviceId())) {
                        System.out.println(cardinal + ".  Position: " + k.src().deviceId() + " Between the Ports: " + k.src().port() + " and " + k.dst().port());
                        cardinal = cardinal+1;
                    }
                }
            }
        }


        System.out.println(" ");
        System.out.println("Do you want to cross any of them? Write it's number. Otherwise write No(n)");

        String bridgeToCross = "";
        try {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            bridgeToCross = stdin.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println ("You have choses to cross: \"" + bridgeToCross +"\"");


        ConnectPoint ingress;
//        if (ingressDeviceString.contains("of")) {
//            ingress = ConnectPoint.deviceConnectPoint(ingressDeviceString);
//        } else {
            ingress = ConnectPoint.hostConnectPoint("00:00:00:00:00:01/-1/0");
//        }

        ConnectPoint egress;
//        if (egressDeviceString.contains("of")) {
            egress = ConnectPoint.deviceConnectPoint("of:0000000000000003/1");
//        } else {
//            egress = ConnectPoint.hostConnectPoint(egressDeviceString + "id=00:00:00:00:00:01/-1/0");
//        }

        ElementId one = ingress.elementId();
        ElementId two = egress.elementId();

//        Path pathOne = getPath(one, two);
        if (one.toString().contains("of") || two.toString().contains("of")) {
            pointToPointIntent("of:0000000000000001/1" , "of:0000000000000001/4");
            pointToPointIntent("of:0000000000000002/1" , "of:0000000000000003/2");
            pointToPointIntent("of:0000000000000003/2" , "of:0000000000000005/1");
            pointToPointIntent("of:0000000000000005/1" , "of:0000000000000004/1");
        }



//       if (one.toString().contains("cacadevacaapostoflant")) {
//
//            HostId iepaaHostID = HostId.hostId("00:00:00:00:00:01/-1");
//            Host iepaaHost = hostService.getHost(iepaaHostID);
//            return Arrays.asList(createPathIntent(pathOne, iepaaHost));
//        } else {
//            HostId oneHostId = ingress.hostId();
//            Host oneHost = hostService.getHost(oneHostId);
//            HostId twoHostId = egress.hostId();
//            Host twoHost = hostService.getHost(twoHostId);
//            return Arrays.asList(createPathIntent(pathOne, oneHost, twoHost));
//        }

    }


    private Path getPath(ElementId one, ElementId two) {
        Set<Path> paths = pathService.getPaths(one, two);
        return paths.iterator().next();
    }


    // Inverts the specified path. This makes an assumption that each link in
    // the path has a reverse link available. Under most circumstances, this
    // assumption will hold.
    private Path invertPath(Path path) {
        List<Link> reverseLinks = new ArrayList<>(path.links().size());
        for (Link link : path.links()) {
            reverseLinks.add(0, reverseLink(link));
        }
        return new DefaultPath(path.providerId(), reverseLinks, path.cost());
    }

    // Produces a reverse variant of the specified link.
    private Link reverseLink(Link link) {
        return new DefaultLink(link.providerId(), link.dst(), link.src(),
                               link.type(), link.state(), link.isDurable());
    }

    // Creates a path intent from the specified path and original connectivity intent.
    private Intent createPathIntent(Path path, Host src, Host dst) {
        TrafficSelector selector = builder()
                .matchEthSrc(src.mac()).matchEthDst(dst.mac()).build();
        return PathIntent.builder()
                .appId(appId)
//                .selector(selector)
//                .treatment(intent.treatment())
                .path(path)
//                .constraints(intent.constraints())
//                .priority(intent.priority())
                .build();
    }

    // Creates a path intent from the specified path and original connectivity intent.
    private Intent createPathIntent(Path path, Host src) {
        MacAddress macSrc = src.mac();
        TrafficSelector selector = builder()
                .matchEthSrc(src.mac()).build();
        return PathIntent.builder()
                .appId(appId)
//                .selector(selector)
//                .treatment(intent.treatment())
                .path(path)
//                .constraints(intent.constraints())
//                .priority(intent.priority())
                .build();
    }

    private void pointToPointIntent(String ingress, String egress) {
//        ConnectPoint ingressP = ConnectPoint.hostConnectPoint("00:00:00:00:00:01/-1/1");
        ConnectPoint ingressP = ConnectPoint.deviceConnectPoint(ingress);
        ConnectPoint egressP = ConnectPoint.deviceConnectPoint(egress);
        Intent intent2 = PointToPointIntent.builder()
                .appId(appId)
//                .key(generateKey(network, hostIdSrc, hostIdDst))
                .ingressPoint(ingressP)
                .egressPoint(egressP)
//                .constraints(constraints)
//                .treatment(treatment)
                .build();
        intentService.submit(intent2);
    }


    private void UNIBOIntentInstaller (String ingress, String egress) {

        ConnectPoint ingressConectPoint;
        if (ingress.contains("of")) {
            ingressConectPoint = ConnectPoint.deviceConnectPoint(ingress);
        } else {
        ingressConectPoint = ConnectPoint.hostConnectPoint(ingress);
        }

        ConnectPoint egressConectPoint;
        if (egress.contains("of")) {
            egressConectPoint = ConnectPoint.deviceConnectPoint(egress);
        } else {
            egressConectPoint = ConnectPoint.hostConnectPoint(egress);
        }


        if (ingress.contains("of") || egress.contains("of")) {

        }

    }




}




























