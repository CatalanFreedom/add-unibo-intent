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
import com.google.common.collect.Iterables;
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
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.*;
import org.onosproject.net.behaviour.IpTunnelEndPoint;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.*;
import org.onosproject.net.intent.constraint.AsymmetricPathConstraint;
import org.onosproject.net.intent.constraint.WaypointConstraint;
import org.onosproject.net.intent.impl.compiler.HostToHostIntentCompiler;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * BYON Application component.
 */
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

    /*
     * TODO Lab 3: Get a reference to the intent service
     *
     * All you need to do is uncomment the following two lines.
     */
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


    /*
     * TODO Lab 6: Instantiate a NetworkStoreDelegate
     *
     * You will first need to implement the class (at the bottom of the file).
     */
    private final NetworkStoreDelegate delegate = new InternalStoreDelegate();

    protected ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onos.unibo");
        /*
         * TODO Lab 6: Remove delegate and event sink
         *
         * 1. Add the listener registry to the event dispatcher using eventDispatcher.addSink()
         * 2. Set the delegate in the store
         */
        eventDispatcher.addSink(NetworkEvent.class, listenerRegistry);
        store.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        /*
         * TODO Lab 6: Remove delegate and event sink
         *
         * 1. Remove the event class from the event dispatcher using eventDispatcher.removeSink()
         * 2. Unset the delegate from the store
         */
        eventDispatcher.removeSink(NetworkEvent.class);
        store.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createNetwork(String network) {
        checkNotNull(network, "Network name cannot be null");
        checkState(!network.contains(","), "Network names cannot contain commas");
        /*
         * TODO Lab 2: Add the network to the store
         */
        store.putNetwork(network);
    }

    @Override
    public void deleteNetwork(String network) {
        checkNotNull(network, "Network name cannot be null");
        /*
         * TODO Lab 2: Remove the network from the store
         */
        store.removeNetwork(network);
        /*
         * TODO Lab 4: Remove the intents when the network is deleted
         */
        removeIntents(network, Optional.empty());
    }

    @Override
    public Set<String> getNetworks() {
        /*
         * TODO Lab 2: Get the networks from the store and return them
         */
        return store.getNetworks();
    }

    @Override
    public void addHost(String network, HostId hostId) {
        checkNotNull(network, "Network name cannot be null");
        checkNotNull(hostId, "HostId cannot be null");
        /*
         * TODO Lab 2: Add the host to the network in the store
         *
         * TODO Lab 3: Connect the host to the network using intents -- addIntents()
         *     You only need to add the intents if this is the first time that
         *     the host is added. (Check the store's return value)
         */
        if (store.addHost(network, hostId)) {
            addIntents(network, hostId, store.getHosts(network));
        }
    }

    @Override
    public void removeHost(String network, HostId hostId) {
        checkNotNull(network, "Network name cannot be null");
        checkNotNull(hostId, "HostId cannot be null");
        /*
         * TODO Lab 2: Remove the host from the network in the store
         *
         * TODO Lab 4: Remove the host's intents from the network
         */
        store.removeHost(network, hostId);
        removeIntents(network, Optional.of(hostId));
    }

    @Override
    public Set<HostId> getHosts(String network) {
        checkNotNull(network, "Network name cannot be null");
        /*
         * TODO Lab 2: Retrieve the hosts from the store and return them
         */
        return store.getHosts(network);
    }

    /**
     * Adds an intent between a new host and all others in the network.
     *
     * @param network network name
     * @param src the new host
     * @param hostsInNet all hosts in the network
     */
    private void addIntents(String network, HostId src, Set<HostId> hostsInNet) {
        /*
         * TODO Lab 3: Implement add intents
         *
         * 1. Create a HostToHostIntent intent between src and every other host in
         *    the network using HostToHostIntent.builder()
         * 2. Generate the intent key using generateKey(), so they can be removed later
         * 3. Submit the intents using intentService.submit()
         */
        hostsInNet.forEach(dst -> {
            if (!src.equals(dst)) {
                Intent intent = HostToHostIntent.builder()
                        .appId(appId)
                        .key(generateKey(network, src, dst))
                        .one(src)
                        .two(dst)
                        .build();
                intentService.submit(intent);
            }
        });
    }

    /**
     * Removes intents that involve the specified host in a network.
     *
     * @param network network name
     * @param hostId host to remove; all hosts if empty
     */
    private void removeIntents(String network, Optional<HostId> hostId) {
        /*
         * TODO Lab 4: Implement remove intents
         *
         * 1. Get the intents from the intent service using intentService.getIntents()
         * 2. Using matches() to filter intents for this network and hostId
         * 3. Withdrawn intentService.withdraw()
         */
        Iterables.filter(intentService.getIntents(), i -> matches(network, hostId, i))
                .forEach(intentService::withdraw);
    }

    /**
     * Returns ordered intent key from network and two hosts.
     *
     * @param network network name
     * @param one host one
     * @param two host two
     * @return canonical intent string key
     */
    protected Key generateKey(String network, HostId one, HostId two) {
        String hosts = one.toString().compareTo(two.toString()) < 0 ?
                format(HOST_FORMAT, one, two) : format(HOST_FORMAT, two, one);
        return Key.of(format(KEY_FORMAT, network, hosts), appId);
    }

    /**
     * Matches an intent to a network and optional host.
     *
     * @param network network name
     * @param id optional host id, wildcard if missing
     * @param intent intent to match
     * @return true if intent matches, false otherwise
     */
    protected boolean matches(String network, Optional<HostId> id, Intent intent) {
        if (!Objects.equals(appId, intent.appId())) {
            // different app ids
            return false;
        }

        String key = intent.key().toString();
        if (!key.startsWith(network)) {
            // different network
            return false;
        }

        if (!id.isPresent()) {
            // no host id specified; wildcard match
            return true;
        }

        HostId hostId = id.get();
        String[] fields = key.split(",");
        // return result of id match in host portion of key
        return fields.length > 1 && fields[1].contains(hostId.toString());
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



    public void addManualIntent(HostId hostIdSrc, String hostToPass, HostId hostIdDst){

//        DeviceId di1 = DeviceId.deviceId(hostToPass);
////        DeviceId di2 = DeviceId.deviceId("of:0000000000000003");
//        List<Constraint> constraints = new ArrayList<Constraint>();
//        constraints.add(new WaypointConstraint(di1));
//        constraints.add(new AsymmetricPathConstraint());
//        constraints.add(new WaypointConstraint(di2));

//        Host dstHost = hostService.getHost(hostIdDst);
//        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.portNumber(2)).build();

//        ConnectPoint ingressP = ConnectPoint.hostConnectPoint("00:00:00:00:00:01/-1/1");
//        ConnectPoint egressP = ConnectPoint.deviceConnectPoint("of:0000000000000003/1");
//        Intent intent2 = PointToPointIntent.builder()
//                .appId(appId)
//                .key(generateKey(network, hostIdSrc, hostIdDst))
//                .ingressPoint(ingressP)
//                .egressPoint(egressP)
//                .constraints(constraints)
////                .treatment(treatment)
//                .build();
//        intentService.submit(intent2);




//        Path pathOne = get

//        Intent intent = HostToHostIntent.builder()
//                .appId(appId)
//                .key(generateKey(network, hostIdSrc, hostIdDst))
//                .one(hostIdSrc)
//                .two(hostIdDst)
//                .constraints(constraints)
////                .treatment(treatment)
//                .build();
//        intentService.submit(intent);

        ConnectPoint ingressP = ConnectPoint.hostConnectPoint("00:00:00:00:00:01/-1/1");
        HostId myHostIdOne = ingressP.hostId();
        IpPrefix myprefix = IpPrefix.valueOf("10.10.10.1/32");

        ConnectPoint egressP = ConnectPoint.hostConnectPoint("00:00:00:00:00:03/-1/1");
        HostId myHostIdTwo = egressP.hostId();

        TrafficSelector myselector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(myprefix)
                .build();

//        Host srHost = hostService.getHost(myhostId);

        Intent intent = HostToHostIntent.builder()
                .appId(appId)
                .one(myHostIdOne)
                .two(myHostIdTwo)
                .selector(myselector)
                .build();
        intentService.submit(intent);


        IpTunnelEndPoint tunel = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf("10.10.10.1/32"));




        Topology mytopo = topologyService.currentTopology();
        Set<TopologyCluster> myclusters = topologyService.getClusters(mytopo);
        Set<DeviceId> deviceIds = topologyService.getClusterDevices(mytopo, myclusters.iterator().next());
        TopologyGraph mygraph = topologyService.getGraph(mytopo);

        Iterator<Host> it = hostService.getHosts().iterator();
//        while (it.hasNext()) {
////            it.next().location().deviceId();
////            System.out.println(it.next().id().toString());
//        }

//        System.out.println(hostService.getHost(hostIdSrc).location().port());
//        System.out.println(hostService.getHost(hostIdDst).location().deviceId());

//        Set<Path> paths = pathService.getPaths(hostIdSrc, hostIdDst, weight(intent.constraints()));
//        ConnectPoint temporal = ConnectPoint.deviceConnectPoint(hostToPass);
//        Set<Path> paths = pathService.getPaths(hostIdSrc, temporal.elementId());
//        System.out.println(paths);

//        System.out.println(mytopo);
//        System.out.println(myclusters);
//        System.out.println(deviceIds);
//        System.out.println(mygraph);


        ConfigureNetworks configMyNet = new ConfigureNetworks(appId, pathService, hostService, topologyService, clusterService, deviceService, intentService, store);
        configMyNet.networkConfigurer();


        List<Intent> compilerUNIBOList = new ArrayList<>(3);
        AddFirstIntentUNIBOCompiler firstUnibo = new AddFirstIntentUNIBOCompiler(appId, pathService, hostService, linkService, deviceService, topologyService, intentService);
        firstUnibo.compilerUNIBO(hostIdSrc.toString(), hostToPass.toString());
//        compilerUNIBOList.add(firstUnibo.compilerUNIBO(hostIdSrc.toString(), hostToPass.toString()).get(0));
//        compilerUNIBOList.add(firstUnibo.compilerUNIBO("of:0000000000000003/1", hostIdDst.toString()).get(0));
//        Iterator itr = compilerUNIBOList.iterator();
//        while (itr.hasNext()) {
//            intentService.submit((Intent)itr.next());
//        }



    }


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
//                .constraints(constraints)
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
//                .constraints(constraints)
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


    public void addSecondUNIBOIntent(List<String> objectsToCross, String dpi, boolean go) {

//        Classification of all the wayPoints of the path of our Chain between hostConnectPoints and deviceConnectPoints
        List<ConnectPoint> connectsToCross = new ArrayList<>();
        for (String p : objectsToCross) {
            if (p.contains("NF")) {
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
//                .constraints(constraints)
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
//                .constraints(constraints)
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
//                .constraints(constraints)
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
//                .constraints(constraints)
//                .treatment(treatment)
                    .build();
            intentService.submit(intent);
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
        Iterator<Link> links;
        List<NFModel> nfs;
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
                System.out.println("...........................................");
            }
            System.out.println(" ");
        }
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
