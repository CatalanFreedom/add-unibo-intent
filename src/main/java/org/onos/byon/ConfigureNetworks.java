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

import org.onlab.packet.MacAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.*;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyService;

import java.io.InputStreamReader;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;

import static org.onosproject.net.flow.DefaultTrafficSelector.builder;

/**
 * A intent compiler for {@link HostToHostIntent}.
 */
//@Component(immediate = true)
public class ConfigureNetworks {

    protected ApplicationId appId;
    protected PathService pathService;
    protected HostService hostService;
    protected TopologyService topologyService;
    protected ClusterService clusterService;
    protected IntentService intentService;
    protected DeviceService deviceService;
    protected NetworkStore store;

    public ConfigureNetworks(ApplicationId appId, PathService pathService, HostService hostService,
                             TopologyService topologyService, ClusterService clusterService, DeviceService deviceService,
                             IntentService intentService, NetworkStore store) {
        this.appId = appId;
        this.pathService = pathService;
        this.hostService = hostService;
        this.topologyService = topologyService;
        this.clusterService = clusterService;
        this.deviceService = deviceService;
        this.intentService = intentService;
        this.store = store;
    }

    public void networkConfigurer() {

        Set<ConnectPoint> ingressPoints = new HashSet<>();

        System.out.println("--------------------------------------------------");

        Topology mytopo = topologyService.currentTopology();
        Set<TopologyCluster> myclusters = topologyService.getClusters(mytopo);
        Iterable<Host> hosts = hostService.getHosts();

        System.out.println(" ");
        System.out.println("THIS NFV TOPOLOGY IS COMPOSED BY " + myclusters.size() + " EDGE NETWORKS");
        System.out.println("Let's configure your edge networks:");
        System.out.println(".................................................");
        System.out.println(" ");

        Iterator<TopologyCluster> clusters = myclusters.iterator();
        while (clusters.hasNext()) {
            Iterator<Host> hostIterator = hosts.iterator();
            TopologyCluster i = clusters.next();
            System.out.println("This is the Cluster number " + i.id().index());
            System.out.println("Number of Switchs: " + i.deviceCount());
            System.out.println("It is composed by the following Hosts:");
            while (hostIterator.hasNext() ) {
                Host j = hostIterator.next();
                String currentSwitch = j.location().deviceId().toString();
                String currentSwitch2 = topologyService.getClusterDevices(mytopo, i).toString();
                if (topologyService.getClusterDevices(mytopo, i).toString().contains(j.location().deviceId().toString())){
                    System.out.println(j.id() + " ip:" + j.ipAddresses() + ". It is connected the switch: " + j.location());
                }
            }
            System.out.println("Which one is the gw of this edge?");
            String gwPosition = "";
            try {
                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                gwPosition = stdin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println ("The gw is in: \"" + gwPosition +"\"");

//            store.putGateway(i.id().index(), HostId.hostId(gwPosition));

            HostId gwHostId = HostId.hostId(gwPosition);
            Host gwHost = hostService.getHost(gwHostId);

//            System.out.println(gwHost.location().deviceId().toString() + gwHost.location().port().toString());
            ConnectPoint egressPoint = ConnectPoint.deviceConnectPoint(gwHost.location().deviceId().toString() + "/" + gwHost.location().port().toString());
//            ConnectPoint egressPoint = ConnectPoint.hostConnectPoint(gwHost.id().toString() + "/0");
            DeviceId switchToGW = gwHost.location().deviceId();
            PortNumber switchToGWPort = gwHost.location().port();
            Set<DeviceId> devices = topologyService.getClusterDevices(mytopo, i);
            Iterator<DeviceId> devicesIterator = devices.iterator();
            while (devicesIterator.hasNext()) {
                DeviceId actualDeviceId = devicesIterator.next();
                Device actualDevice = deviceService.getDevice(actualDeviceId);
                List<Port> ports = deviceService.getPorts(actualDeviceId);
                Iterator<Port> portIterator = ports.iterator();
                while (portIterator.hasNext()) {
                    Port actualPort = portIterator.next();
//                    System.out.println(actualDeviceId.toString() + "/" + actualPort.number().toString());
                    if (!actualPort.number().toString().contains("LOCAL") && !actualPort.number().equals(gwHost.location().port())) {
                        ingressPoints.add(ConnectPoint.deviceConnectPoint(actualDeviceId.toString() + "/" + actualPort.number().toString()));
                    }
                }
            }
            configureGW( ingressPoints, egressPoint);
            ingressPoints.clear();

            System.out.println("--------------------------------------------------");
            System.out.println(" ");
        }

        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


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

    private void configureGW(Set<ConnectPoint> ingressPoints, ConnectPoint egressPoint) {
        Intent intent = MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .egressPoint(egressPoint)
                .ingressPoints(ingressPoints)
                .build();
//        intentService.submit(intent);
    }
}
