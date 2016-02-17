
package org.onos.byon;

import org.apache.felix.scr.annotations.*;
import org.onosproject.net.*;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Network Store implementation backed by consistent map.
 */
@Component(immediate = true)
public class GREModel {

    private static Logger log = LoggerFactory.getLogger(GREModel.class);

    @Activate
    public void activate() {

    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    private String id;
    private int edgeSrc;
    private int edgeDst;
    private ConnectPoint connectPoint;
    private Port port;


    public void setId(String id) {
        this.id = id;
    }

    public void setEdgeSrc(int edgeSrc) {
        this.edgeSrc = edgeSrc;
    }

    public void setEdgeDst(int edgeDst) {
        this.edgeDst = edgeDst;
    }

    public void setConnectPoint(ConnectPoint connectPoint) {
        this.connectPoint = connectPoint;
    }

    public void setPort(Port port) {
        this.port = port;
    }

    public String getId () { return id; }

    public int getEdgeSrc() {
        return edgeSrc;
    }

    public int getEdgeDst() {
        return edgeDst;
    }

    public ConnectPoint getConnectPoint() {
        return connectPoint;
    }

    public Port getPort() { return port; }
}
