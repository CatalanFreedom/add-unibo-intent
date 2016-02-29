
package org.onos.byon;

import org.onosproject.net.HostId;

import java.util.List;
import java.util.Set;

/**
 * Service that allows to create virtual named networks
 * which provide mesh connectivity between hosts of a
 * given network.
 */
public interface NetworkService {


    /**
     * Register a listener for network events.
     *
     * @param listener listener
     */
//   Add addListener to the interface
    void addListener(NetworkListener listener);

    /**
     * Unregister a listener for network events.
     *
     * @param listener listener
     */
//    Add removeListener to the interface
    void removeListener(NetworkListener listener);

    /**
     * Adds an intent to the given network.
     *
     * @param objectsToCross ingress Point
     * @param dpi ingress Point
     */
    void addFirstUNIBOIntent(List<String> objectsToCross, String dpi);

    /**
     * Adds an intent to the given network.
     *
     * @param objectsToCross ingress Point
     * @param dpi ingress Point
     * @param go true if is the go way and false if it is the come back way
     */
    void addSecondUNIBOIntent(List<String> objectsToCross, String dpi, boolean go);

    /**
     * Show all the components of the present network
     */
    void showUNIBONetwork();

    /**
     * Configure the gateways of the network
     */
    void configureNetwork();

    /**
     * Show all the Network Function (NF) bridges
     */
    void showNFsBridges();

    public void addThirdUNIBOIntent(List<String> objectsToCross, String dpi, boolean go);

    public void addIntent();

}
