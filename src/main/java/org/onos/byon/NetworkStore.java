
package org.onos.byon;

import org.onosproject.net.*;
import org.onosproject.store.Store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks networks and their hosts.
 */
// Extend Store<NetworkEvent, NetworkStoreDelegate>
public interface NetworkStore extends Store<NetworkEvent, NetworkStoreDelegate> {
    /**
     * Create a named network.
     *
     * @param network network name
     */
    void putNetwork(String network);

    /**
     * Removes a named network.
     *
     * @param network network name
     */
    void removeNetwork(String network);

    /**
     * Returns a set of network names.
     *
     * @return a set of network names
     */
    Set<String> getNetworks();

    /**
     * Adds a host to the given network.
     *
     * @param network network name
     * @param hostId host id
     * @return true if the host was added; false if it already exists
     */
    boolean addHost(String network, HostId hostId);

    /**
     * Removes a host from the given network.
     *
     * @param network network name
     * @param hostId host id
     */
    void removeHost(String network, HostId hostId);

    /**
     * Returns all the hosts in a network.
     *
     * @param network network name
     * @return set of host ids
     */
    Set<HostId> getHosts(String network);

    /**
     * Create a named network.
     *
     * @param edgeId cluster/edge identificator
     * @param gateway gateway device of the edge
     */
    void putIngressGateway(int edgeId, HostId gateway);

    /**
     * Create a named network.
     *
     * @param edgeId cluster/edge identificator
     * @param gateway gateway device of the edge
     */
    void putEgressGateway(int edgeId, HostId gateway);

    /**
     * Returns all the hosts in a network.
     *
     * @param connectPoint network name
     * @param go true if is the go way and false if it is the come back way
     * @return the DeviceId of the edge where the connect point is allocated
     */
    ConnectPoint getGwByConnectPoint(ConnectPoint connectPoint, boolean go);

    /**
     * Unswer if a host is or not the gw of its edge
     *
     * @param hostId present host
     * @return true if the host is the gw of its edge
     */
    boolean isTheIngressGw(HostId hostId);

    /**
     * Unswer if a host is or not the gw of its edge
     *
     * @param hostId present host
     * @return true if the host is the gw of its edge
     */
    boolean isTheEgressGw(HostId hostId);

    /**
     * Returns all NFs in the network
     */
    Map<Integer, List<HashMap<DeviceId, NFModel>>> getNFs();

    /**
     *
     * @param deviceId where we want to get the NFs
     * @return the List<NFModel> in the device that has been asked
     */
    public List<NFModel> getNFsByDeviceId (DeviceId deviceId);

    /**
     *
     * @param name of the Network Function
     * @return the ingress connect point of the Network Function name
     */
    ConnectPoint getIngressByNFsName (String name);

    /**
     *
     * @param name of the Network Function
     * @return the egress connect point of the Network Function name
     */
    ConnectPoint getEgressByNFsName (String name);

    /**
     *
     * @param ingress of the Network Function
     * @return the egress connect point of the Network Function
     */
    ConnectPoint getEgressByNFIngress (ConnectPoint ingress);


    public List<Port> getTunnel(DeviceId deviceId);

    public int getEdgeByConnectPoint (ConnectPoint connectPoint);

    public ConnectPoint getTunnelByEdgesSrcDst (int Src, int Dst);


}
