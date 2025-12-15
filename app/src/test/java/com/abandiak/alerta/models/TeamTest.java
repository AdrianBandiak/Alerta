package com.abandiak.alerta.models;

import static org.junit.Assert.*;

import com.abandiak.alerta.data.model.Team;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TeamTest {

    @Test
    public void testTeamConstructorAndGetters() {
        Timestamp ts = Timestamp.now();

        Team team = new Team(
                "team1",
                "Rescue Squad",
                "Primary rescue unit",
                "ABC123",
                "user123",
                "Creator name",
                ts,
                0xFF112233,
                "RegionNorth"
        );

        assertEquals("team1", team.getId());
        assertEquals("Rescue Squad", team.getName());
        assertEquals("Primary rescue unit", team.getDescription());
        assertEquals("ABC123", team.getCode());
        assertEquals("user123", team.getCreatedBy());
        assertEquals(ts, team.getCreatedAt());
        assertEquals(0xFF112233, team.getColor());
        assertEquals("RegionNorth", team.getRegion());

        assertEquals("", team.getLastMessage());
        assertNull(team.getLastTimestamp());
    }

    @Test
    public void testSetters() {
        Team team = new Team();

        team.setId("T001");
        team.setName("Medical Team");
        team.setDescription("Responsible for first aid");
        team.setRegion("Central");
        team.setColor(0xFF00FF00);

        Timestamp now = Timestamp.now();
        team.setCreatedAt(now);
        team.setCreatedBy("admin123");

        team.setLastMessage("New update");
        team.setLastTimestamp(now);

        assertEquals("T001", team.getId());
        assertEquals("Medical Team", team.getName());
        assertEquals("Responsible for first aid", team.getDescription());
        assertEquals("Central", team.getRegion());
        assertEquals(0xFF00FF00, team.getColor());

        assertEquals(now, team.getCreatedAt());
        assertEquals("admin123", team.getCreatedBy());

        assertEquals("New update", team.getLastMessage());
        assertEquals(now, team.getLastTimestamp());
    }

    @Test
    public void testMembersIndex() {
        Team team = new Team();

        List<String> members = Arrays.asList("user1", "user2", "user3");
        team.setMembersIndex(members);

        assertEquals(3, team.getMembersIndex().size());
        assertTrue(team.getMembersIndex().contains("user1"));
        assertTrue(team.getMembersIndex().contains("user2"));
        assertTrue(team.getMembersIndex().contains("user3"));
    }

    @Test
    public void testTimestampMillisMethods() {
        Team team = new Team();

        assertEquals(0, team.getCreatedAtMillis());
        assertEquals(0, team.getLastTimestampMillis());

        Timestamp ts = Timestamp.now();
        team.setCreatedAt(ts);
        team.setLastTimestamp(ts);

        assertEquals(ts.toDate().getTime(), team.getCreatedAtMillis());
        assertEquals(ts.toDate().getTime(), team.getLastTimestampMillis());
    }
}
