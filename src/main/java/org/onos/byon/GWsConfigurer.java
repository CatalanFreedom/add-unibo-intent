package org.onos.byon;

import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.*;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * A intent compiler for {@link HostToHostIntent}.
 */
//@Component(immediate = true)
public class GWsConfigurer {

    protected ApplicationId appId;
    protected PathService pathService;
    protected HostService hostService;
    protected TopologyService topologyService;
    protected ClusterService clusterService;
    protected IntentService intentService;
    protected DeviceService deviceService;
    protected NetworkStore store;

    public GWsConfigurer(ApplicationId appId, PathService pathService, HostService hostService,
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
            System.out.println("Which one is the Ingress gw of this edge?");
            String gwPosition = "";
            try {
                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                gwPosition = stdin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                store.putIngressGateway(i.id().index(), HostId.hostId(gwPosition));
                System.out.println ("The Ingress gw is in: \"" + gwPosition +"\"");
            } catch (Exception e) {
                System.out.println ("No Ingress gw in this edge");
            }



            System.out.println("Which one is the Egress gw of this edge?");
            gwPosition = "";
            try {
                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                gwPosition = stdin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                store.putEgressGateway(i.id().index(), HostId.hostId(gwPosition));
                System.out.println ("The Egress gw is in: \"" + gwPosition +"\"");
            } catch (Exception e) {
                System.out.println ("No Egress gw in this edge");
            }



            System.out.println("--------------------------------------------------");
            System.out.println(" ");
        }

        try {
            Thread.sleep(60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
