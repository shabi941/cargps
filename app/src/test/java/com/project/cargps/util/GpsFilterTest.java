package com.project.cargps.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GpsFilterTest {

    @Test
    public void shouldFilterByJump_acceptsRecoveryPointAfterLongGap() {
        boolean shouldFilter = GpsFilter.shouldFilterByJump(
                23.100000, 113.100000,
                23.200000, 113.200000,
                true,
                10 * 60 * 1000L
        );

        assertFalse(shouldFilter);
    }

    @Test
    public void shouldFilterByJump_rejectsUnreasonableShortGapJump() {
        boolean shouldFilter = GpsFilter.shouldFilterByJump(
                23.100000, 113.100000,
                23.200000, 113.200000,
                true,
                1 * 1000L
        );

        assertTrue(shouldFilter);
    }
}
