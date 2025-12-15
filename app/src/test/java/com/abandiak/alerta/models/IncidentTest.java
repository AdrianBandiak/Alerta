package com.abandiak.alerta.models;

import org.junit.Test;
import static org.junit.Assert.*;

import com.abandiak.alerta.data.model.Incident;
import java.util.*;

public class IncidentTest {

    @Test
    public void testConstructorBasicFields() {
        Incident incident = new Incident(
                "Fire near school",
                "Smoke observed near building",
                "warning",
                "u123hash",
                52.1,
                21.0,
                "mazowieckie",
                "user123",
                Arrays.asList("public", "teamABC"),
                "http://photo.url"
        );

        assertEquals("Fire near school", incident.getTitle());
        assertEquals("Smoke observed near building", incident.getDescription());
        assertEquals("warning", incident.getType());
        assertEquals("u123hash", incident.getGeohash());
        assertEquals(52.1, incident.getLat(), 0.0001);
        assertEquals(21.0, incident.getLng(), 0.0001);
        assertEquals("mazowieckie", incident.getRegion());
        assertEquals("user123", incident.getCreatedBy());
        assertEquals(Arrays.asList("public", "teamABC"), incident.getAud());
        assertEquals("http://photo.url", incident.getPhotoUrl());

        assertFalse(incident.isVerified());
        assertNull(incident.getVerifiedBy());
        assertNotNull(incident.getLogs());
        assertEquals(1, incident.getLogs().size());
    }

    @Test
    public void testSetters() {
        Incident incident = new Incident(
                "Test", "Desc", "info",
                null, 10, 20,
                "region", "user",
                null, null
        );

        incident.setTeamId("TEAM1");
        incident.setTeamColor(123456);
        incident.setVerified(true);
        incident.setVerifiedBy("adminUser");

        assertEquals("TEAM1", incident.getTeamId());
        assertEquals(123456, incident.getTeamColor());
        assertTrue(incident.isVerified());
        assertEquals("adminUser", incident.getVerifiedBy());
    }

    @Test
    public void testToMapBasicBranches() {
        Incident incident = new Incident(
                "Incident title",
                "Desc",
                "critical",
                "geohash123",
                10.0,
                20.0,
                "podlasie",
                "userABC",
                Collections.singletonList("public"),
                "photo_link"
        );

        incident.setTeamId("T1");
        incident.setTeamColor(999);

        Map<String, Object> map = incident.toMap();

        assertEquals("Incident title", map.get("title"));
        assertEquals("Desc", map.get("description"));
        assertEquals("critical", map.get("type"));
        assertEquals(10.0, (double) map.get("lat"), 0.0001);
        assertEquals(20.0, (double) map.get("lng"), 0.0001);
        assertEquals("podlasie", map.get("region"));
        assertEquals("userABC", map.get("createdBy"));
        assertEquals(Collections.singletonList("public"), map.get("aud"));
        assertEquals("geohash123", map.get("geohash"));
        assertEquals("photo_link", map.get("photoUrl"));
        assertEquals("T1", map.get("teamId"));
        assertEquals(999, map.get("teamColor"));
        assertNotNull(map.get("logs"));
        assertNotNull(map.get("createdAt"));
    }

    @Test
    public void testConstructorFallbacks() {
        Incident incident = new Incident(
                "Fallback",
                "Missing aud",
                "info",
                null,
                15.0,
                30.0,
                "pomorskie",
                "userXYZ",
                null,
                null
        );

        assertTrue(incident.getAud().isEmpty());

        Map<String, Object> map = incident.toMap();

        assertEquals(Collections.singletonList("public"), map.get("aud"));
    }

    @Test
    public void testRegionBucketAndLogsBranches() {
        Incident incident = new Incident(
                "Complex",
                "Testing other branches",
                "alert",
                "hashX",
                1.1,
                2.2,
                "lubelskie",
                "usr",
                Arrays.asList("a", "b"),
                "url"
        );


        incident.setLogs(null);

        Map<String, Object> map = incident.toMap();

        assertEquals(Arrays.asList("a", "b"), map.get("aud"));

        assertNull(map.get("logs"));

        assertFalse(map.containsKey("regionBucket"));
    }

}
