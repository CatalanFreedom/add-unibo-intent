
package org.onos.byon;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
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
        // Extend the AbstractStore class for the store delegate
        extends AbstractStore<NetworkEvent, NetworkStoreDelegate>
        implements NetworkStore {

    private static Logger log = LoggerFactory.getLogger(DistributedNetworkStore.class);

//     Get a reference to the storage service

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;


    private Map<String, Set<HostId>> networks;

    private ConsistentMap<String, Set<HostId>> nets;

    private Map<Integer, ConnectPoint> egressGateways = new HashMap<>();

    private Map<Integer, ConnectPoint> ingressGateways = new HashMap<>();

//    Map to detect the NF <nº edge, <Port 1, Port2>>
    private NFModel presentNF;
    private Map<Integer,List<HashMap<DeviceId, NFModel>>> NF = new HashMap<Integer, List<HashMap<DeviceId, NFModel>>>();

    private List<NFModel> NFunctions = new ArrayList<>();

    private List<GREModel> greTunnels = new ArrayList<>();

//    private ConnectPoint[][][] matrixTunnels = new ConnectPoint[10][10][2];

    private final InternalListener listener = new InternalListener();

    @Activate
    public void activate() {

        nets = storageService.<String, Set<HostId>>consistentMapBuilder()
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .withName("byon-networks")
                .build();

        nets.addListener(listener);
        networks = nets.asJavaMap();
        log.info("Started");
        fillNFsMap();
        fillGreeList();
    }

    @Deactivate
    public void deactivate() {
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
        return checkNotNull(networks.get(network),
                            "Please create the network first");
    }

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



    @Override
    public List<Port> getTunnel( DeviceId deviceId) {
        List<Port> tunnels = new ArrayList<>();
        Iterator<Port> ports;

        ports = deviceService.getPorts(deviceId).iterator();

        while (ports.hasNext()) { // To loop all the PORTS in the present device
            Port k = ports.next(); // k is the PORT in the present iteration
            Set<String> keys = k.annotations().keys();

            if (k.annotations().value(keys.iterator().next()).contains("gre")) {
//                System.out.println(k.annotations().value(keys.iterator().next()));
                tunnels.add(k);
            }
        }
        return tunnels;
    }


    private void fillGreeList() {
        Topology myTopo = topologyService.currentTopology();
        Iterator<TopologyCluster> clusters;
        Iterator<DeviceId> devices;
        Iterator<Port> ports;

        int cardinal = 1;
        boolean repited = false;
        clusters =  topologyService.getClusters(myTopo).iterator();

        while (clusters.hasNext()) { // To loop all the CLUSTERS
            TopologyCluster ii = clusters.next(); // i is the CLUSTER in the present iteration
            devices = topologyService.getClusterDevices(myTopo, ii).iterator();

            while (devices.hasNext()) { // To loop all the DEVICES in the present cluster
                DeviceId jj = devices.next(); // j is the DEVICE in the present iteration
                ports = deviceService.getPorts(jj).iterator();
                while (ports.hasNext()) { // To loop all the PORTS in the present device
                    Port k = ports.next(); // k is the PORT in the present iteration
                    Set<String> keys = k.annotations().keys();
                    if (k.annotations().value(keys.iterator().next()).contains("gre")) {
                        GREModel greModel = new GREModel();
                        String greId = k.annotations().value(keys.iterator().next());
                        greModel.setId(greId);
                        greModel.setEdgeSrc(getClusterSrcByGreeId(greId));
                        greModel.setEdgeDst(getClusterDstByGreeId(greId));
                        greModel.setConnectPoint(ConnectPoint.deviceConnectPoint(jj.toString() + "/" + k.number().toString()));
                        greModel.setPort(k);
                        greTunnels.add(greModel);
                    }
                }
            }
        }
    }

    private int getClusterSrcByGreeId(String greId) {
        int clusterSrc = -1;
        char[] cad = greId.toCharArray();
        for(int i=0; i<cad.length; i++) {
            if(cad[i] == 'c') {
                try {
                    char iep = cad[i+1];
                    clusterSrc = cad[i+1] - 48;   //From char to int (48 is the ascii value of 0)
                    return clusterSrc;
                } catch (Exception e) {
                }
            }
        }
        return clusterSrc;
    }

    private int getClusterDstByGreeId(String greId) {
        int clusterSrc = -1;
        int count = 0;
        char[] cad = greId.toCharArray();
        for(int i=0; i<cad.length; i++) {
            if(cad[i] == 'c') {
                try {
                    char iep = cad[i+1];
                    clusterSrc = cad[i+1] - 48;   //From char to int (48 is the ascii value of 0)
                    count++;
                    if(count==2) {
                        return clusterSrc;
                    }
                } catch (Exception e) {
                }
            }
        }
        return clusterSrc = -1;
    }

    public List<GREModel> getGreByDeviceId(DeviceId deviceId) {
        List<GREModel> GREinDeviceId = new ArrayList<>();
        Iterator<GREModel> tunnels = greTunnels.iterator();
        while (tunnels.hasNext()) {
            GREModel actualTunnel = tunnels.next();
            if (actualTunnel.getConnectPoint().deviceId().equals(deviceId)) {
                GREinDeviceId.add(actualTunnel);
            }
        }
        return GREinDeviceId;
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


    @Override
    public int getEdgeByConnectPoint (ConnectPoint connectPoint) {
        ConnectPoint gw;

        if (getGwByConnectPoint(connectPoint, false) != null) {
            gw =getGwByConnectPoint(connectPoint, false);
        } else {
            gw =getGwByConnectPoint(connectPoint, true);
        }

        Host gwHost = hostService.getHost(gw.hostId());
        String ip = gwHost.ipAddresses().toString();
        String[] temp = ip.split("\\.", 4);
        int edge = Integer.parseInt(temp[2]);

        return edge;
    }


    @Override
    public ConnectPoint getTunnelByEdgesSrcDst (int Src, int Dst) {

        for (int i=0; i<greTunnels.size(); i++) {
            if (greTunnels.get(i).getEdgeSrc() == Src && greTunnels.get(i).getEdgeDst() == Dst) {
                return greTunnels.get(i).getConnectPoint();
            }
        }

        return null;
    }


}
