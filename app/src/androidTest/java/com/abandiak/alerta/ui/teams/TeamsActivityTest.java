package com.abandiak.alerta.ui.teams;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.teams.TeamsActivity;
import com.abandiak.alerta.data.model.Team;
import com.abandiak.alerta.util.FakeTeamRepository;
import com.abandiak.alerta.util.ToastMatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class TeamsActivityTest {

    private ActivityScenario<TeamsActivity> launchActivity() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                TeamsActivity.class
        );
        intent.putExtra("theme", R.style.TestTheme);
        intent.putExtra("disable_anim", true);

        ActivityScenario<TeamsActivity> scenario =
                ActivityScenario.launch(intent);

        SystemClock.sleep(200);
        return scenario;
    }

    @Before
    public void setup() {
        FakeTeamRepository.reset();
        TeamsActivity.repoOverride = new FakeTeamRepository();
        SystemClock.sleep(150);
    }

    @After
    public void teardown() {
        TeamsActivity.repoOverride = null;
        SystemClock.sleep(150);
    }

    @Test
    public void testEmptyListShowsEmptyState() {
        launchActivity();

        onView(withId(R.id.emptyState)).check(matches(isDisplayed()));
        onView(withId(R.id.recyclerTeams))
                .check(matches(withEffectiveVisibility(Visibility.GONE)));
    }

    @Test
    public void testListShowsTeams() {
        Team t = new Team();
        t.setId("T1");
        t.setName("Rescue Squad");
        t.setColor(0xFFAA0000);

        FakeTeamRepository.setTeams(Collections.singletonList(t));

        launchActivity();
        SystemClock.sleep(200);

        onView(withText("Rescue Squad")).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateTeamFlow() {
        launchActivity();

        onView(withId(R.id.btnCreateTeam)).perform(click());
        SystemClock.sleep(200);

        onView(withId(R.id.inputTeamName)).perform(typeText("Alpha Team"));
        closeSoftKeyboard();

        onView(withId(R.id.inputTeamDesc)).perform(typeText("Unit A"));
        closeSoftKeyboard();

        onView(withId(R.id.inputTeamRegion)).perform(typeText("Region X"));
        closeSoftKeyboard();

        onView(withId(R.id.btnCreate)).perform(click());
        SystemClock.sleep(300);

        onView(withText("Team created."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));

        SystemClock.sleep(250);

        onView(withText("Alpha Team")).check(matches(isDisplayed()));
    }

    @Test
    public void testJoinTeam() {
        Team t = new Team();
        t.setId("TEAM123");
        t.setName("Bravo Team");

        FakeTeamRepository.setTeams(Collections.singletonList(t));
        FakeTeamRepository.setCodeMapping("ABCDEF", "TEAM123");

        launchActivity();

        onView(withId(R.id.btnJoinTeam)).perform(click());
        SystemClock.sleep(200);

        onView(withId(R.id.inputCode)).perform(typeText("ABCDEF"));
        closeSoftKeyboard();

        onView(withId(R.id.btnJoin)).perform(click());
        SystemClock.sleep(300);

        onView(withText("Joined team."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testOpenTeamDetails() {
        Team t = new Team();
        t.setId("T1");
        t.setName("Gamma Team");
        t.setDescription("Fast response");

        FakeTeamRepository.setTeams(Collections.singletonList(t));

        launchActivity();
        SystemClock.sleep(200);

        onView(withText("Gamma Team")).perform(click());
        SystemClock.sleep(200);

        onView(withId(R.id.textTeamName))
                .check(matches(withText("Gamma Team")));
        onView(withId(R.id.textTeamDescription))
                .check(matches(withText("Fast response")));
    }

    @Test
    public void testDeleteTeam() {
        Team t = new Team();
        t.setId("DEL1");
        t.setName("Delete Me Team");

        FakeTeamRepository.setTeams(Collections.singletonList(t));

        launchActivity();
        SystemClock.sleep(200);

        onView(withText("Delete Me Team")).perform(click());
        SystemClock.sleep(200);

        onView(withId(R.id.btnDelete)).perform(click());
        SystemClock.sleep(150);

        onView(withId(R.id.btnConfirm)).perform(click());
        SystemClock.sleep(350);

        onView(withText("Team deleted."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEditTeamDialogOpens() {
        Team t = new Team();
        t.setId("EDIT1");
        t.setName("Original Name");
        t.setDescription("Test Desc");
        t.setRegion("West");

        FakeTeamRepository.setTeams(Collections.singletonList(t));

        launchActivity();
        SystemClock.sleep(200);

        onView(withText("Original Name")).perform(click());
        SystemClock.sleep(200);

        onView(withId(R.id.btnEdit)).perform(click());
        SystemClock.sleep(200);

        onView(withId(R.id.dialogTitle))
                .check(matches(withText("Edit Team")));

        onView(withId(R.id.inputTeamName))
                .check(matches(withText("Original Name")));
    }
}
