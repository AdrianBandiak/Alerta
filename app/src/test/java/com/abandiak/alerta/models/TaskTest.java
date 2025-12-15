package com.abandiak.alerta.models;

import static org.junit.Assert.*;

import com.abandiak.alerta.data.model.Task;

import org.junit.Test;

public class TaskTest {

    @Test
    public void testDefaultConstructorAndSettersGetters() {
        Task task = new Task();

        task.setId("T001");
        task.setTitle("Check supplies");
        task.setCreatedBy("user123");
        task.setTime("10:30");
        task.setCompleted(true);
        task.setDate("2025-01-01");
        task.setDescription("Verify first aid kits");
        task.setPriority("HIGH");
        task.setEndDate("2025-01-05");
        task.setType("CRITICAL");
        task.setTeamId("teamABC");
        task.setTeamColor(0xFF112233);

        assertEquals("T001", task.getId());
        assertEquals("Check supplies", task.getTitle());
        assertEquals("user123", task.getCreatedBy());
        assertEquals("10:30", task.getTime());
        assertTrue(task.isCompleted());
        assertEquals("2025-01-01", task.getDate());
        assertEquals("Verify first aid kits", task.getDescription());
        assertEquals("HIGH", task.getPriority());
        assertEquals("2025-01-05", task.getEndDate());
        assertEquals("CRITICAL", task.getType());
        assertEquals("teamABC", task.getTeamId());
        assertEquals(Integer.valueOf(0xFF112233), task.getTeamColor());
    }

    @Test
    public void testParameterizedConstructor() {
        Task task = new Task(
                "T100",
                "Secure area",
                "admin007",
                "14:00",
                false,
                "2025-02-10"
        );

        assertEquals("T100", task.getId());
        assertEquals("Secure area", task.getTitle());
        assertEquals("admin007", task.getCreatedBy());
        assertEquals("14:00", task.getTime());
        assertFalse(task.isCompleted());
        assertEquals("2025-02-10", task.getDate());

        assertEquals("NORMAL", task.getType());

        assertNull(task.getDescription());
        assertNull(task.getPriority());
        assertNull(task.getEndDate());
        assertNull(task.getTeamId());
        assertNull(task.getTeamColor());
    }
}
