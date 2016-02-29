
package org.onos.byon;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs network events.
 */
@Component(immediate = true)
public class NetworkEventMonitor {
    private static Logger log = LoggerFactory.getLogger(NetworkEventMonitor.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkService service;

    private final Listener listener = new Listener();

    @Activate
    protected void activate() {
        service.addListener(listener);
        log.info("Started");
    }

    @Deactivate
     protected void deactivate() {
        service.removeListener(listener);
        log.info("Stopped");
    }

    private class Listener implements NetworkListener {
        @Override
        public void event(NetworkEvent event) {
            log.info("{}", event);
        }
    }
}

