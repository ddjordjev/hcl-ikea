package com.fulfilment.application.monolith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ErrorMapperTest {

    private ErrorMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new ErrorMapper();
        Field field = ErrorMapper.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(mapper, new ObjectMapper());
    }

    @Test
    void clientErrorKeepsStatusCode() {
        Response response = mapper.toResponse(new WebApplicationException("Not found", 404));
        assertEquals(404, response.getStatus());

        ObjectNode body = (ObjectNode) response.getEntity();
        assertEquals(404, body.get("code").asInt());
        assertEquals("Not found", body.get("error").asText());
    }

    @Test
    void mapsGenericExceptionTo500() {
        Response response = mapper.toResponse(new RuntimeException("something broke"));
        assertEquals(500, response.getStatus());

        ObjectNode body = (ObjectNode) response.getEntity();
        assertEquals(500, body.get("code").asInt());
    }

    @Test
    void handlesNullMessage() {
        Response response = mapper.toResponse(new RuntimeException((String) null));
        assertEquals(500, response.getStatus());

        ObjectNode body = (ObjectNode) response.getEntity();
        assertFalse(body.has("error"));
    }
}
