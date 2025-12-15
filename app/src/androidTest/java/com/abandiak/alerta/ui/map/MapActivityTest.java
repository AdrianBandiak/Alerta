package com.abandiak.alerta.ui.map;

import androidx.test.core.app.ActivityScenario;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.map.MapActivity;
import com.abandiak.alerta.app.map.cluster.IncidentItem;
import com.abandiak.alerta.data.model.Incident;
import com.abandiak.alerta.util.*;
import com.abandiak.alerta.util.FakeClusterManager;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import android.net.Uri;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class MapActivityTest {

    @Before
    public void setup() {

        MapActivity.TEST_MODE = true;

        FakeMap fakeMap = new FakeMap();
        fakeMap.setFakeCameraPosition(new com.google.android.gms.maps.model.LatLng(50, 19));

        MapActivity.TEST_MAP = fakeMap;

        FakeIncidentRepository fakeRepo = new FakeIncidentRepository();
        fakeRepo.setIncidents(Collections.emptyList());

        MapActivity.TEST_INCIDENT_REPO = fakeRepo;

        MapActivity.TEST_CLUSTER_MANAGER = new FakeClusterManager();
    }

    @Test
    public void testFabOpensCreateIncidentSheet() {

        ActivityScenario.launch(MapActivity.class);

        onView(withId(R.id.btnAddMarkerFab)).perform(click());

        onView(withText("Incident created"))
                .check(doesNotExist());
    }

    @Test
    public void testShowIncidentDetails() {

        Incident inc = new Incident("Fire", "Desc", "INFO", 50, 19, "PL-MA", "uid");
        inc.setId("1");

        ActivityScenario<MapActivity> scenario = ActivityScenario.launch(MapActivity.class);

        scenario.onActivity(a -> {
            IncidentItem item = new IncidentItem(
                    inc.getId(), inc.getTitle(), inc.getDescription(),
                    inc.getLat(), inc.getLng(), inc.getType(),
                    null, false, inc.getCreatedBy()
            );
            a.showIncidentDetailsForTests(item);
        });

        onView(withText("Fire")).check(matches(isDisplayed()));
    }

    @Test
    public void testRemovePhotoClearsPreview() {
        ActivityScenario<MapActivity> scenario = ActivityScenario.launch(MapActivity.class);

        scenario.onActivity(a -> a.openCreateIncidentSheetForTests());

        SystemClock.sleep(300);

        scenario.onActivity(a -> {
            a.initPreviewForTests();

            a.setPickedPhotoUriForTests(Uri.parse("content://test/photo"));

            a.forcePreviewUpdateForTests();
        });

        SystemClock.sleep(300);

        onView(withId(R.id.btn_remove_photo))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.img_preview))
                .check(matches(withEffectiveVisibility(Visibility.GONE)));
    }



    @Test
    public void testIncidentDetailsShowsCoordinates() {
        Incident inc = new Incident("Fire", "Desc", "INFO", 50, 19, "PL-MA", "uid");
        inc.setId("1");

        ActivityScenario<MapActivity> scenario = ActivityScenario.launch(MapActivity.class);

        scenario.onActivity(a -> {
            IncidentItem item = new IncidentItem(
                    inc.getId(), inc.getTitle(), inc.getDescription(),
                    inc.getLat(), inc.getLng(), inc.getType(),
                    null, false, inc.getCreatedBy()
            );
            a.showIncidentDetailsForTests(item);
        });

        onView(withText("50.00° N  19.00° E"))
                .check(matches(isDisplayed()));
    }


}
