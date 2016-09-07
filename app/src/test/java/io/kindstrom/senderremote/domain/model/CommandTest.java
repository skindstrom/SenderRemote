package io.kindstrom.senderremote.domain.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CommandTest {
    private final Pin pin = Pin.create("1234");

    @Test
    public void temperature() throws Exception {
        assertEquals("TEMP 1234", Command.temperature(pin));
    }

    @Test
    public void humidity() throws Exception {
        assertEquals("HUMID 1234", Command.humidity(pin));
    }

    @Test
    public void measurements() throws Exception {
        assertEquals("MEAS 1234", Command.measurements(pin));
    }

    @Test
    public void status() throws Exception {
        assertEquals("STATUS 1234", Command.status(pin));
    }

    @Test
    public void pin() throws Exception {
        assertEquals("PIN 456789 1234", Command.pin(Pin.create("456789"), pin));
    }
}