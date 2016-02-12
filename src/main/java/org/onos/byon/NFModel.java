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

import org.apache.felix.scr.annotations.*;
import org.onosproject.net.*;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Network Store implementation backed by consistent map.
 */
@Component(immediate = true)
public class NFModel {

    private static Logger log = LoggerFactory.getLogger(NFModel.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    private Map<String, Set<HostId>> networks;

    private ConsistentMap<String, Set<HostId>> nets;

    private Map<Integer, ConnectPoint> gateways = new HashMap<>();

    //    Map to detect the NF <nº edge, <Port 1, Port2>>
    private Map<Integer, HashMap<PortNumber, PortNumber>> NF = new HashMap<Integer, HashMap<PortNumber, PortNumber>>();
    //    private HashMap<PortNumber, PortNumber> presentNF = new HashMap<>();
    private HashMap<PortNumber, PortNumber> presentNF;

    public NFModel() {
    }


    @Activate
    public void activate() {

    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    private int edge;
    private DeviceId deviceId;
    private String name;
    private PortNumber ingress;
    private PortNumber egress;


    public void setedge(int edge) {
        this.edge = edge;
    }

    public void setdeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIngress(PortNumber ingress) {
        this.ingress = ingress;
    }

    public void setEgress(PortNumber egress) {
        this.egress = egress;
    }

    public DeviceId getDeviceId () { return deviceId; }

    public String getName() {
        return name;
    }

    public PortNumber getIngress() {
        return ingress;
    }

    public PortNumber getEgress() {
        return egress;
    }
}
