package com.fulfilment.application.monolith.location;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocationGatewayTest {

    @Test
    void resolvesKnownLocation_caseInsensitive() {
        LocationGateway gateway = new LocationGateway();
        var location = gateway.resolveByIdentifier("zwolle-001");
        assertNotNull(location);
        assertEquals("ZWOLLE-001", location.identification);
    }

    @Test
    void returnsNullForUnknownLocation() {
        LocationGateway gateway = new LocationGateway();
        assertNull(gateway.resolveByIdentifier("UNKNOWN-999"));
    }

    @Test
    void returnsNullForBlankOrNull() {
        LocationGateway gateway = new LocationGateway();
        assertNull(gateway.resolveByIdentifier("  "));
        assertNull(gateway.resolveByIdentifier(null));
    }
}
