package com.project.cargps.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GpsRecoveryGateTest {
    @Test
    public void coldStartRequiresTwoConsistentPoints() {
        GpsRecoveryGate gate = new GpsRecoveryGate();
        assertTrue(gate.shouldHold(22.5400, 114.0500, 1_000, false, 0));
        assertFalse(gate.shouldHold(22.5401, 114.0501, 2_000, false, 0));
    }

    @Test
    public void wrongFirstFixDoesNotBecomeAnchor() {
        GpsRecoveryGate gate = new GpsRecoveryGate();
        assertTrue(gate.shouldHold(23.1000, 113.2000, 1_000, false, 0));
        assertTrue(gate.shouldHold(22.5400, 114.0500, 2_000, false, 0));
        assertFalse(gate.shouldHold(22.5401, 114.0501, 3_000, false, 0));
    }

    @Test
    public void normalTrackingDoesNotDelayPoints() {
        GpsRecoveryGate gate = new GpsRecoveryGate();
        assertFalse(gate.shouldHold(22.5400, 114.0500, 10_000, true, 1_000));
    }

    @Test
    public void longGapRequiresRecoveryConfirmation() {
        GpsRecoveryGate gate = new GpsRecoveryGate();
        assertTrue(gate.shouldHold(22.5400, 114.0500, 600_000, true, 360_000));
        assertFalse(gate.shouldHold(22.5401, 114.0501, 601_000, true, 361_000));
    }
}
