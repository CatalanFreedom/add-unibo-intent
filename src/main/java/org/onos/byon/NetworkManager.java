
package org.onos.byon;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.*;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;



@Component(immediate = true)
@Service
public class NetworkManager extends AbstractListenerManager<NetworkEvent, NetworkListener> implements NetworkService {

    private static Logger log = LoggerFactory.getLogger(NetworkManager.class);

    public static final String HOST_FORMAT = "%s~%s";
    public static final String KEY_FORMAT = "%s,%s";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;


    private final NetworkStoreDelegate delegate = new InternalStoreDelegate();
    protected ApplicationId appId;
    protected long key = 0;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onos.unibo");
        eventDispatcher.addSink(NetworkEvent.class, listenerRegistry);
        store.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        eventDispatcher.removeSink(NetworkEvent.class);
        store.unsetDelegate(delegate);
        log.info("Stopped");
    }


    /*
     *
     * The class should implement the NetworkStoreDelegate interface and
     * its notify method.
     */
    private class InternalStoreDelegate implements NetworkStoreDelegate {
        @Override
        public void notify(NetworkEvent event) {
            post(event);
        }
    }


    public void addIntent() {

        ConnectPoint one = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");
        ConnectPoint two = ConnectPoint.deviceConnectPoint("of:0000000000000004/5");
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(2)
                .build();

        Intent intent = PointToPointIntent.builder()
                .appId(appId)
                .ingressPoint(one)
                .egressPoint(two)
                //.selector(selector)
//                .treatment(treatment)
                .build();
        intentService.submit(intent);


        one = ConnectPoint.deviceConnectPoint("of:0000000000000009/4");
        two = ConnectPoint.deviceConnectPoint("of:0000000000000009/2");
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchTunnelId(2)
                .build();


        Intent intent2 = PointToPointIntent.builder()
                .appId(appId)
                .ingressPoint(one)
                .egressPoint(two)
//                .selector(selector)
                .build();
        intentService.submit(intent2);

    }



    @Override
    public void addFirstUNIBOIntent(List<String> objectsToCross, String dpi){

//        Classification of all the wayPoints of the path of our Chain between hostConnectPoints and deviceConnectPoints
        List<ConnectPoint> connectsToCross = new ArrayList<>();
        for (String p : objectsToCross) {
            if (p.contains("of")) {
                connectsToCross.add(ConnectPoint.deviceConnectPoint(p + "/4"));
            } else if (p.contains("NF")) {
//                connectsToCross.add(ConnectPoint.deviceConnectPoint("of:0000000000000001/4"));
                connectsToCross.add(store.getIngressByNFsName(p));
            } else {
                connectsToCross.add(ConnectPoint.hostConnectPoint(p + "/0"));
            }
        }

//        Checking if we have a DPI to duplicate the packets and send it.
        ConnectPoint dpiConnectPoint = null;
        if (dpi != null) {
            dpiConnectPoint = ConnectPoint.hostConnectPoint(dpi + "/0");
        }

        boolean changingEdge = false;
        ConnectPoint nextConnectToCross = null;

//        We install an intent for each jump in our chain.
//        Depending in the kind of jump, we will install one kind of intent or another one.
        for ( int i=0; i<objectsToCross.size()-1; i++ ) {

//              Is the present jump between 2 points in the same edge?...
            if (areInTheSameEDGE(connectsToCross.get(i), connectsToCross.get(i+1))) {


//                DPI in the present edge
                if (areInTheSameEDGE(connectsToCross.get(i), dpiConnectPoint)) {

//                    path(i):Host, path(i+1):NF
                    if (connectsToCross.get(i).elementId() instanceof HostId &&
                            connectsToCross.get(i+1).elementId() instanceof DeviceId) {
                        Set<ConnectPoint> egressPoints = new HashSet<>(2);
                        egressPoints.add(connectsToCross.get(i+1));
                        egressPoints.add(hostToDevLocation(dpiConnectPoint));
                        Intent intent = SinglePointToMultiPointIntent.builder()
                                .appId(appId)
                                .ingressPoint(hostToDevLocation(connectsToCross.get(i)))
                                .egressPoints(egressPoints)
                                .build();
                        intentService.submit(intent);
                        dpiConnectPoint = null;
                    }

//                    path(i):Host, path(i+1):Host
                    if (connectsToCross.get(i).elementId() instanceof HostId &&
                            connectsToCross.get(i+1).elementId() instanceof HostId) {
                        Set<ConnectPoint> egressPoints = new HashSet<>(2);
                        egressPoints.add(hostToDevLocation(connectsToCross.get(i+1)));
                        egressPoints.add(hostToDevLocation(dpiConnectPoint));
                        Intent intent = SinglePointToMultiPointIntent.builder()
                                .appId(appId)
                                .ingressPoint(hostToDevLocation(connectsToCross.get(i)))
                                .egressPoints(egressPoints)
                                .build();
                        intentService.submit(intent);
                        dpiConnectPoint = null;
                    }


                } else {        // No DPI or NOT in the present edge

                    if (connectsToCross.get(i).elementId() instanceof DeviceId &&
                            connectsToCross.get(i+1).elementId() instanceof HostId) {

                        Intent intent = PointToPointIntent.builder()
                                .appId(appId)
//                .key(generateKey(network, hostIdSrc, hostIdDst))
                                .ingressPoint(store.getEgressByNFIngress(connectsToCross.get(i)))
                                .egressPoint(hostToDevLocation(connectsToCross.get(i+1)))
//                .treatment(treatment)
                                .build();
                        intentService.submit(intent);
                    }


                    if (connectsToCross.get(i).elementId() instanceof HostId &&
                            connectsToCross.get(i+1).elementId() instanceof HostId) {
                        Intent intent = PointToPointIntent.builder()
                                .appId(appId)
//                .key(generateKey(network, hostIdSrc, hostIdDst))
                                .ingressPoint(hostToDevLocation(connectsToCross.get(i)))
                                .egressPoint(hostToDevLocation(connectsToCross.get(i+1)))
//                .treatment(treatment)
                                .build();
                        intentService.submit(intent);
                    }
                }


            } else {
                nextConnectToCross = connectsToCross.get(i+1);
                ConnectPoint actualConnect = connectsToCross.get(i);
//                connectsToCross.remove(i);
//                connectsToCross.add(i, ConnectPoint.deviceConnectPoint("of:0000000000000001/5"));
//                connectsToCross.add(i, store.getEgressByNFIngress(actualConnect));
                connectsToCross.remove(i+1);
//                connectsToCross.add(i+1, ConnectPoint.hostConnectPoint("00:00:00:00:00:08/-1/0"));

                connectsToCross.add(i+1, store.getGwByConnectPoint(connectsToCross.get(i), true));
                changingEdge = true;
                i--;
                continue;
            }

            if (changingEdge) {
                connectsToCross.remove(i);
                connectsToCross.add(i, store.getGwByConnectPoint(nextConnectToCross, true));
//                connectsToCross.add(i, ConnectPoint.hostConnectPoint("00:00:00:00:00:09/-1/0"));
                connectsToCross.add(i+1, nextConnectToCross);
                changingEdge = false;
                i--;

            }


        }
    }

    @Override
    public void addSecondUNIBOIntent(List<String> objectsToCross, String dpi, boolean go) {

//        Classification of all the wayPoints of the path of our Chain between hostConnectPoints and deviceConnectPoints
        List<ConnectPoint> connectsToCross = new ArrayList<>();
        for (String p : objectsToCross) {
            if (p.contains("NF")) {
                connectsToCross.add(store.getIngressByNFsName(p));
            } else {
                connectsToCross.add(ConnectPoint.hostConnectPoint(p + "/0"));
            }
        }

//        Checking if we have a DPI to duplicate the packets and send it.
        ConnectPoint dpiConnectPoint = null;
        if (dpi != null) {
            dpiConnectPoint = ConnectPoint.hostConnectPoint(dpi + "/0");
        }

        HostId srcHostId = connectsToCross.get(0).hostId();
        Host srcHost = hostService.getHost(srcHostId);
        Set<IpAddress> ipAdressesSrc = srcHost.ipAddresses();

        HostId dstHostId = connectsToCross.get(connectsToCross.size()-1).hostId();
        Host dstHost = hostService.getHost(dstHostId);
        Set<IpAddress> ipAdressesDst = dstHost.ipAddresses();

        IpPrefix ipSrc = IpPrefix.valueOf(ipAdressesSrc.iterator().next(), 32);
        IpPrefix ipDst = IpPrefix.valueOf(ipAdressesDst.iterator().next(), 32);

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(ipSrc)
                .matchIPDst(ipDst)
                .build();

//        We install an intent for each jump in our chain.
//        Depending in the kind of jump, we will install one kind of intent or another one.
        for (int i = 0; i < objectsToCross.size() - 1; i++) {

//              Is the present jump between 2 points in the same edge?...
            if (areInTheSameEDGE(connectsToCross.get(i), connectsToCross.get(i + 1))) {


//                DPI in the present edge
                if (areInTheSameEDGE(connectsToCross.get(i), dpiConnectPoint)) {

                        Set<ConnectPoint> egressPoints = new HashSet<>(2);
                        egressPoints.add(hostToDevLocation(connectsToCross.get(i + 1)));
                        egressPoints.add(hostToDevLocation(dpiConnectPoint));
                        Intent intent = SinglePointToMultiPointIntent.builder()
                                .appId(appId)
                                .ingressPoint(hostToDevLocation(connectsToCross.get(i)))
                                .egressPoints(egressPoints)
                                .selector(selector)
                                .build();
                        intentService.submit(intent);
                        dpiConnectPoint = null;


                } else {        // No DPI or NOT in the present edge

                    if (connectsToCross.get(i).elementId() instanceof HostId &&
                            connectsToCross.get(i + 1).elementId() instanceof DeviceId) {
                        Intent intent = PointToPointIntent.builder()
                                .appId(appId)
//                .key(generateKey(network, hostIdSrc, hostIdDst))
                                .ingressPoint(hostToDevLocation(connectsToCross.get(i)))
                                .egressPoint(hostToDevLocation(connectsToCross.get(i + 1)))
                                .selector(selector)
//                .treatment(treatment)
                                .build();
                        intentService.submit(intent);
                    }

                    if (connectsToCross.get(i).elementId() instanceof DeviceId &&
                            connectsToCross.get(i + 1).elementId() instanceof DeviceId) {

                        Intent intent = PointToPointIntent.builder()
                                .appId(appId)
//                .key(generateKey(network, hostIdSrc, hostIdDst))
                                .ingressPoint(store.getEgressByNFIngress(connectsToCross.get(i)))
                                .egressPoint(connectsToCross.get(i + 1))
                                .selector(selector)
//                .treatment(treatment)
                                .build();
                        intentService.submit(intent);
                    }
                }


            } else {

                if (areInTheSameEDGE(connectsToCross.get(i), dpiConnectPoint)) {
                    Intent intent = PointToPointIntent.builder()
                            .appId(appId)
//                .key(generateKey(network, hostIdSrc, hostIdDst))
                            .ingressPoint(hostToDevLocation(connectsToCross.get(i)))
                            .egressPoint(hostToDevLocation(dpiConnectPoint))
                            .selector(selector)
//                .treatment(treatment)
                            .build();
                    intentService.submit(intent);
                    dpiConnectPoint = null;
                }
                connectsToCross.remove(i);
                connectsToCross.add(i, store.getGwByConnectPoint(connectsToCross.get(i), go));
                i--;
            }
        }

        if (areInTheSameEDGE(store.getGwByConnectPoint(dpiConnectPoint, go), dpiConnectPoint)) {
            Intent intent = PointToPointIntent.builder()
                    .appId(appId)
//                .key(generateKey(network, hostIdSrc, hostIdDst))
                    .ingressPoint(hostToDevLocation(store.getGwByConnectPoint(dpiConnectPoint, go)))
                    .egressPoint(hostToDevLocation(dpiConnectPoint))
                    .selector(selector)
//                .treatment(treatment)
                    .build();
            intentService.submit(intent);
        }
    }



    @Override
    public void addThirdUNIBOIntent(List<String> objectsToCross, String dpi, boolean go){

//        Classification of all the wayPoints of the path of our Chain between hostConnectPoints and deviceConnectPoints
        List<ConnectPoint> connectsToCross = new ArrayList<>();
        for (String p : objectsToCross) {
            if (p.contains("NF")) {
                connectsToCross.add(store.getIngressByNFsName(p));
            } else {
                connectsToCross.add(ConnectPoint.hostConnectPoint(p + "/0"));
            }
        }

//        Checking if we have a DPI to duplicate the packets and send it.
        ConnectPoint dpiConnectPoint = null;
        if (dpi != null) {
            dpiConnectPoint = ConnectPoint.hostConnectPoint(dpi + "/0");
        }


        // Selectors
        HostId srcHostId = connectsToCross.get(0).hostId();
        Host srcHost = hostService.getHost(srcHostId);
        Set<IpAddress> ipAdressesSrc = srcHost.ipAddresses();

        HostId dstHostId = connectsToCross.get(connectsToCross.size()-1).hostId();
        Host dstHost = hostService.getHost(dstHostId);
        Set<IpAddress> ipAdressesDst = dstHost.ipAddresses();

        IpPrefix ipSrc = IpPrefix.valueOf(ipAdressesSrc.iterator().next(), 32);
        IpPrefix ipDst = IpPrefix.valueOf(ipAdressesDst.iterator().next(), 32);

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(ipSrc)
                .matchIPDst(ipDst)
                .build();


        boolean changingEdge = false;
        ConnectPoint nextConnectToCross = null;

//        We install an intent for each jump in our chain.
//        Depending in the kind of jump, we will install one kind of intent or another one.
        for ( int i=0; i<objectsToCross.size()-1; i++ ) {

//              Is the present jump between 2 points in the same edge?...
            if (areInTheSameEDGE(connectsToCross.get(i), connectsToCross.get(i+1))) {


//                DPI in the present edge
                if (areInTheSameEDGE(connectsToCross.get(i), dpiConnectPoint)) {

                        Set<ConnectPoint> egressPoints = new HashSet<>(2);
                        egressPoints.add(connectsToCross.get(i+1));
                        egressPoints.add(hostToDevLocation(dpiConnectPoint));
                        Intent intent = SinglePointToMultiPointIntent.builder()
                                .appId(appId)
                                .key(genKey(connectsToCross.get(i), connectsToCross.get(i+1)))
                                .ingressPoint(hostToDevLocation(connectsToCross.get(i)))
                                .egressPoints(egressPoints)
                                .selector(selector)
                                .build();
                        intentService.submit(intent);
                        dpiConnectPoint = null;


                } else {        // No DPI or NOT in the present edge

//                    From [Host to NF] or [Host to Tunnel]
                    if (connectsToCross.get(i).elementId() instanceof HostId &&
                            connectsToCross.get(i+1).elementId() instanceof DeviceId) {

                        Intent intent = PointToPointIntent.builder()
                                .appId(appId)
                                .key(genKey(connectsToCross.get(i), connectsToCross.get(i+1)))
                                .ingressPoint(hostToDevLocation(connectsToCross.get(i)))
                                .egressPoint(hostToDevLocation(connectsToCross.get(i+1)))
                                .selector(selector)
                                .build();
                        intentService.submit(intent);
                    }


//                    From [NF to NF] or [NF to Tunnel]
                    if (connectsToCross.get(i).elementId() instanceof DeviceId &&
                            store.getEgressByNFIngress(connectsToCross.get(i)) != null &&
                            connectsToCross.get(i+1).elementId() instanceof DeviceId ) {

                        Intent intent = PointToPointIntent.builder()
                                .appId(appId)
                                .key(genKey(connectsToCross.get(i), connectsToCross.get(i+1)))
                                .ingressPoint(store.getEgressByNFIngress(connectsToCross.get(i)))
                                .egressPoint(hostToDevLocation(connectsToCross.get(i+1)))
                                .selector(selector)
                                .build();
                        intentService.submit(intent);
                    }

 //                    From [Tunnel to NF]
                    if (connectsToCross.get(i).elementId() instanceof DeviceId &&
                            store.getEgressByNFIngress(connectsToCross.get(i)) == null &&
                            connectsToCross.get(i+1).elementId() instanceof DeviceId ) {

                        Intent intent = PointToPointIntent.builder()
                                .appId(appId)
                                .key(genKey(connectsToCross.get(i), connectsToCross.get(i+1)))
                                .ingressPoint(hostToDevLocation(connectsToCross.get(i)))
                                .egressPoint(hostToDevLocation(connectsToCross.get(i+1)))
                                .selector(selector)
                                .build();
                        intentService.submit(intent);
                    }

                }

            } else {
                ConnectPoint actualConnect = connectsToCross.get(i);
                nextConnectToCross = connectsToCross.get(i+1);
                connectsToCross.remove(i+1);

                int edgeSrc = store.getEdgeByConnectPoint(actualConnect);
                int edgeDst = store.getEdgeByConnectPoint(nextConnectToCross);
                connectsToCross.add(i+1, store.getTunnelByEdgesSrcDst(edgeSrc, edgeDst));
                changingEdge = true;
                i--;
                continue;
            }

            if (changingEdge) {
                int edgeSrc = store.getEdgeByConnectPoint(nextConnectToCross);
                int edgeDst = store.getEdgeByConnectPoint(connectsToCross.get(i));
                connectsToCross.remove(i);
                connectsToCross.add(i, store.getTunnelByEdgesSrcDst(edgeSrc, edgeDst));
                connectsToCross.remove(i+1);
                connectsToCross.add(i+1, nextConnectToCross);
                changingEdge = false;
                i--;
            }
        }
    }






//      This function returns the ConnectPoint where a Host is located if we give it a Host Connect Point.
//      Otherwise it returns the same ConnectPoint it has recived.
    private ConnectPoint hostToDevLocation (ConnectPoint one) {

        if (one.elementId() instanceof HostId) {
            DeviceId oneLocDev = hostService.getHost(one.hostId()).location().deviceId();
            PortNumber oneLocPortDev = hostService.getHost(one.hostId()).location().port();
            one = ConnectPoint.deviceConnectPoint(oneLocDev.toString() + "/" + oneLocPortDev.toString());
        }
        return one;
    }


    public boolean areInTheSameEDGE(ConnectPoint one, ConnectPoint two) {

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




    public void showUNIBONetwork(){
        Topology myTopo = topologyService.currentTopology();
        Iterator<TopologyCluster> clusters;
        Iterator<DeviceId> devices;
        Iterator<DeviceId> devices2;
        List<NFModel> nfs;
        List<Port> tunnels;
        clusters =  topologyService.getClusters(myTopo).iterator();

        System.out.println(" ");
        System.out.println(" ");
        System.out.println("Your Topology contains " + topologyService.getClusters(myTopo).size() + " network edges");
        System.out.println("________________________________________________________________________________________");


        while (clusters.hasNext()) { // To loop all the CLUSTERS
            System.out.println(" ");
            TopologyCluster i = clusters.next(); // i is the CLUSTER in the present iteration
            System.out.println(" ");
            System.out.println(" ");
            System.out.println("Edge Network number: " + i.id().index());
            System.out.println("__________________________________");
            devices = topologyService.getClusterDevices(myTopo, i).iterator();
            devices2 = topologyService.getClusterDevices(myTopo, i).iterator();

            while (devices.hasNext()) { // To loop all the DEVICES in the present cluster
                DeviceId j = devices.next(); // j is the DEVICE in the present iteration
                System.out.println(" ");
                System.out.println("Switch: " + j + ":");
                Iterator<Host> hostIterator = hostService.getConnectedHosts(j).iterator();

                while (hostIterator.hasNext()) {
                    Host l = hostIterator.next();
                    if (l.location().deviceId().equals(j)) {
//                        System.out.println(store.getEdgeByConnectPoint (ConnectPoint.hostConnectPoint(l.id().toString() + "/1")));
                        if (store.isTheIngressGw(l.id())) {
                            System.out.println("Host " + l.mac() + " --- ip: " + l.ipAddresses() + "   --It is the Ingress gw--");
                        } else if (store.isTheEgressGw(l.id())) {
                            System.out.println("Host " + l.mac() + " --- ip: " + l.ipAddresses() + "   --It is the Egress gw--");
                        } else {
                            System.out.println("Host " + l.mac() + " --- ip: " + l.ipAddresses());
                        }
                    }
                }

                DeviceId jj = devices2.next(); // j is the DEVICE in the present iteration
                nfs = store.getNFsByDeviceId(jj);
                for (int g=0; g<nfs.size();g++) {
                    System.out.println("(" + nfs.get(g).getName() + ") - Between the Ports: " + nfs.get(g).getIngress() + " - " + nfs.get(g).getEgress());
                }

                tunnels = store.getTunnel(jj);
                for (int n=0; n<tunnels.size(); n++) {
                    Set<String> keys = tunnels.get(n).annotations().keys();
                    System.out.println("(" + tunnels.get(n).annotations().value(keys.iterator().next())
                            + ") Gre tunnel in port " + tunnels.get(n).number());
                }

                System.out.println("...........................................");
            }
            System.out.println(" ");
        }
    }


    protected Key genKey(ConnectPoint one, ConnectPoint two) {
//        String hosts = one.elementId().toString() + two.elementId().toString();
//        return Key.of(hosts, appId);
        key++;
        return Key.of(key, appId);
    }



    public void configureNetwork() {
        GWsConfigurer configMyNet = new GWsConfigurer(appId, pathService, hostService, topologyService,
                clusterService, deviceService, intentService, store);
        configMyNet.networkConfigurer();

//        add-unibo-intent -dup 00:00:00:00:00:03/-1 00:00:00:00:00:01/-1 NF2 00:00:00:00:00:0C/-1
    }


    public void showNFsBridges() {
        store.getNFs();
    }

}
