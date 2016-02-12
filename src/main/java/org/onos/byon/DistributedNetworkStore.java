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

package org.onos.byon;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.*;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onos.byon.NetworkEvent.Type.NETWORK_ADDED;
import static org.onos.byon.NetworkEvent.Type.NETWORK_REMOVED;
import static org.onos.byon.NetworkEvent.Type.NETWORK_UPDATED;

/**
 * Network Store implementation backed by consistent map.
 */
@Component(immediate = true)
@Service
public class DistributedNetworkStore
        // TODO Lab 6: Extend the AbstractStore class for the store delegate
        extends AbstractStore<NetworkEvent, NetworkStoreDelegate>
        implements NetworkStore {

    private static Logger log = LoggerFactory.getLogger(DistributedNetworkStore.class);

    /*
     * TODO Lab 5: Get a reference to the storage service
     *
     * All you need to do is uncomment the following two lines.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    /*
     * TODO Lab 5: Replace the ConcurrentMap with ConsistentMap
     */
    private Map<String, Set<HostId>> networks;

    private ConsistentMap<String, Set<HostId>> nets;

    private Map<Integer, ConnectPoint> egressGateways = new HashMap<>();

    private Map<Integer, ConnectPoint> ingressGateways = new HashMap<>();

//    Map to detect the NF <nº edge, <Port 1, Port2>>
    private NFModel presentNF;
    private Map<Integer,List<HashMap<DeviceId, NFModel>>> NF = new HashMap<Integer, List<HashMap<DeviceId, NFModel>>>();

    private List<NFModel> NFunctions = new ArrayList<>();

    /*
     * TODO Lab 6: Create a listener instance of InternalListener
     *
     * You will first need to implement the class (at the bottom of the file).
     */
    private final InternalListener listener = new InternalListener();

    @Activate
    public void activate() {
        /**
         * TODO Lab 5: Replace the ConcurrentHashMap with ConsistentMap
         *
         * You should use storageService.consistentMapBuilder(), and the
         * serializer: Serializer.using(KryoNamespaces.API)
         */




        nets = storageService.<String, Set<HostId>>consistentMapBuilder()
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .withName("byon-networks")
                .build();

        /*
         * TODO Lab 6: Add the listener to the networks map
         *
         * Use nets.addListener()
         */
        nets.addListener(listener);
        networks = nets.asJavaMap();
        log.info("Started");
        fillNFsMap();
    }

    @Deactivate
    public void deactivate() {
        /*
         * TODO Lab 6: Remove the listener from the networks map
         *
         * Use nets.removeListener()
         */
        nets.removeListener(listener);
        log.info("Stopped");
    }

    @Override
    public void putNetwork(String network) {
        networks.putIfAbsent(network, Sets.<HostId>newHashSet());
    }

    @Override
    public void removeNetwork(String network) {
        networks.remove(network);
    }

    @Override
    public Set<String> getNetworks() {
        return ImmutableSet.copyOf(networks.keySet());
    }

    @Override
    public boolean addHost(String network, HostId hostId) {
        /*
         * TODO Lab 5: Update the Set to Versioned<Set<HostId>>
         *
         * You will also need to extract the value in the if statement.
         */
        Set<HostId> existingHosts = checkNotNull(networks.get(network),
                                                            "Network %s does not exist", network);
        if (existingHosts.contains(hostId)) {
            return false;
        }

        networks.computeIfPresent(network,
                                  (k, v) -> {
                                      Set<HostId> result = Sets.newHashSet(v);
                                      result.add(hostId);
                                      return result;
                                  });
        return true;
    }

    @Override
    public void removeHost(String network, HostId hostId) {
        /*
         * TODO Lab 5: Update the Set to Versioned<Set<HostId>>
         */
        Set<HostId> hosts =
                networks.computeIfPresent(network,
                                          (k, v) -> {
                                              Set<HostId> result = Sets.newHashSet(v);
                                              result.remove(hostId);
                                              return result;
                                          });
        checkNotNull(hosts, "Network %s does not exist", network);
    }

    @Override
    public Set<HostId> getHosts(String network) {
        /*
         * TODO Lab 5: Update return value
         *
         * ConsistentMap returns a Versioned<V>, so you need to extract the value
         */
        return checkNotNull(networks.get(network),
                            "Please create the network first");
    }

    /*
     * TODO Lab 6: Implement an InternalListener class for remote map events
     *
     * The class should implement the MapEventListener interface and
     * its event method.
     */
    private class InternalListener implements MapEventListener<String, Set<HostId>> {
        @Override
        public void event(MapEvent<String, Set<HostId>> mapEvent) {
            final NetworkEvent.Type type;
            switch (mapEvent.type()) {
                case INSERT:
                    type = NETWORK_ADDED;
                    break;
                case UPDATE:
                    type = NETWORK_UPDATED;
                    break;
                case REMOVE:
                default:
                    type = NETWORK_REMOVED;
                    break;
            }
            notifyDelegate(new NetworkEvent(type, mapEvent.key()));
        }
    }


    @Override
    public void putIngressGateway(int edgeId, HostId gateway) {

        ingressGateways.put(edgeId, ConnectPoint.hostConnectPoint(gateway.toString() + "/0"));
    }

    @Override
    public void putEgressGateway(int edgeId, HostId gateway) {

        egressGateways.put(edgeId, ConnectPoint.hostConnectPoint(gateway.toString() + "/0"));
    }

    @Override
    public ConnectPoint getGwByConnectPoint(ConnectPoint connectPoint, boolean go){
        Iterator<ConnectPoint> gws = null;
        if (go) {
            gws = ingressGateways.values().iterator();
        } else {
            gws = egressGateways.values().iterator();
        }
        while (gws.hasNext()) {

            ConnectPoint actualGw = gws.next();
            if (areInTheSameEDGE(connectPoint, actualGw)) {
                return actualGw;
            }
        }
        return null;
    }



    private boolean areInTheSameEDGE(ConnectPoint one, ConnectPoint two) {

        if (one==null || two==null) {
            return false;
        }

        Topology myTopo = topologyService.currentTopology();
        Iterator<TopologyCluster> clusters;
        Iterator<DeviceId> devices;
        Iterator<Link> links;

        clusters =  topologyService.getClusters(myTopo).iterator();
        while (clusters.hasNext()) { // To loop all the CLUSTERS
            int oneFlag =0, twoFlag=0;
            TopologyCluster i = clusters.next(); // i is the CLUSTER in the present iteration
            devices = topologyService.getClusterDevices(myTopo, i).iterator();

            while (devices.hasNext()) { // To loop all the DEVICES in the present cluster
                DeviceId j = devices.next(); // j is the DEVICE in the present iteration
                ElementId jj = (ElementId) j;
                if (jj.equals(one.elementId())) {
                    oneFlag=1;
                }
                if (jj.equals(two.elementId())) {
                    twoFlag=1;
                }
                Iterator<Host> hostIterator = hostService.getConnectedHosts(j).iterator();

                while (hostIterator.hasNext()) {
                    Host l = hostIterator.next();
                    ElementId ll = (ElementId) l.id();
                    if (ll.equals(one.elementId())) {
                        oneFlag=1;
                    }
                    if (ll.equals(two.elementId())) {
                        twoFlag=1;
                    }
                }
            }
            if ( oneFlag + twoFlag ==2) {
//                System.out.println("In the same EDGE");
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isTheIngressGw(HostId hostId) {
        if (ingressGateways.containsValue(ConnectPoint.hostConnectPoint(hostId.toString() + "/0")))
            return true;
        return false;
    }

    @Override
    public boolean isTheEgressGw(HostId hostId) {
        if (egressGateways.containsValue(ConnectPoint.hostConnectPoint(hostId.toString() + "/0")))
            return true;
        return false;
    }


    private void fillNFsMap() {
        Topology myTopo = topologyService.currentTopology();
        Iterator<TopologyCluster> clusters;
        Iterator<DeviceId> devices;
        Iterator<Link> links;

        int cardinal = 1;
        boolean repited = false;
        clusters =  topologyService.getClusters(myTopo).iterator();

        while (clusters.hasNext()) { // To loop all the CLUSTERS
            TopologyCluster ii = clusters.next(); // i is the CLUSTER in the present iteration
            devices = topologyService.getClusterDevices(myTopo, ii).iterator();

            while (devices.hasNext()) { // To loop all the DEVICES in the present cluster
                DeviceId jj = devices.next(); // j is the DEVICE in the present iteration
                links = linkService.getDeviceLinks(jj).iterator();

                while (links.hasNext()) { // To loop all the LINKS in the present device
                    presentNF = new NFModel();
                    Link k = links.next(); // k is the LINK in the present iteration
                    if (k.src().deviceId().equals(k.dst().deviceId())) {
//                        System.out.println(cardinal + ".  Position: " + k.src().deviceId() + " Between the Ports: " + k.src().port() + " and " + k.dst().port());

                        Iterator<NFModel> bridges = NFunctions.iterator();
                        while (bridges.hasNext()) {
                            NFModel actualBridge = bridges.next();
                            if (actualBridge.getDeviceId().equals(jj) &&
                                    (actualBridge.getIngress().equals(k.dst().port()) || actualBridge.getIngress().equals(k.src().port()))) {
                                repited = true;
                                continue;
                            }
                        }
                        if (!repited) {
                            presentNF.setName("NF" + cardinal);
                            presentNF.setdeviceId(jj);
                            presentNF.setedge(ii.id().index());
                            presentNF.setIngress(k.src().port());
                            presentNF.setEgress(k.dst().port());
                            NFunctions.add(presentNF);
                            cardinal = cardinal+1;
                            repited = false;
                        }
                    }
                    repited = false;
                }
            }
        }
    }


    public Map<Integer, List<HashMap<DeviceId, NFModel>>> getNFs (){
        return NF;
    }

    public List<NFModel> getNFsByDeviceId (DeviceId deviceId) {
        List<NFModel> NFinDeviceId = new ArrayList<>();
        Iterator<NFModel> bridges = NFunctions.iterator();
        while (bridges.hasNext()) {
            NFModel actualBridge = bridges.next();
            if (actualBridge.getDeviceId().equals(deviceId)) {
                NFinDeviceId.add(actualBridge);
            }
        }
        return NFinDeviceId;
    }

    public ConnectPoint getIngressByNFsName (String name) {
        ConnectPoint connectPointNF = null;
        Iterator<NFModel> bridges = NFunctions.iterator();
        while (bridges.hasNext()) {
            NFModel actualBridge = bridges.next();
            if (actualBridge.getName().equals(name)) {
                connectPointNF = ConnectPoint.deviceConnectPoint(actualBridge.getDeviceId().toString() + "/" + actualBridge.getIngress().toString());
            }
        }
        return connectPointNF;
    }

    public ConnectPoint getEgressByNFsName (String name) {
        ConnectPoint connectPointNF = null;
        Iterator<NFModel> bridges = NFunctions.iterator();
        while (bridges.hasNext()) {
            NFModel actualBridge = bridges.next();
            if (actualBridge.getName().equals(name)) {
                connectPointNF = ConnectPoint.deviceConnectPoint(actualBridge.getDeviceId().toString() + "/" + actualBridge.getEgress().toString());
            }
        }
        return connectPointNF;
    }

    public ConnectPoint getEgressByNFIngress (ConnectPoint ingress) {
        ConnectPoint connectPointNF = null;
        Iterator<NFModel> bridges = NFunctions.iterator();
        while (bridges.hasNext()) {
            NFModel actualBridge = bridges.next();
            if (actualBridge.getDeviceId().equals(ingress.deviceId()) && actualBridge.getIngress().equals(ingress.port())) {
                connectPointNF = ConnectPoint.deviceConnectPoint(actualBridge.getDeviceId().toString() + "/" + actualBridge.getEgress().toString());
            }
        }
        return connectPointNF;
    }
}
