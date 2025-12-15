package com.abandiak.alerta.ui.login;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import androidx.test.espresso.intent.Intents;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.Intents.intended;


import androidx.test.core.app.ActivityScenario;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.auth.LoginActivity;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.util.*;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class LoginActivityTest {

    @Before
    public void setup() {
        LoginActivity.disableFinishForTests = true;

        FakeAuthProvider.reset();
        FakeUserStore.reset();

        FakeAuthProvider.addUser("test1@test.com", "test1234", "UID123");

        LoginActivity.authOverride = new FakeAuthProvider();
        LoginActivity.dbOverride = new FakeFirestoreProvider();
    }


    private ActivityScenario<LoginActivity> launch() {
        return ActivityScenario.launch(LoginActivity.class);
    }

    @Test
    public void testInvalidEmailFormat() {
        launch();

        onView(withId(R.id.editTextEmail)).perform(typeText("badEmail"), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword)).perform(typeText("password"), closeSoftKeyboard());
        onView(withId(R.id.buttonLogin)).perform(click());

        onView(withText("Invalid email format."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyFields() {
        launch();

        onView(withId(R.id.buttonLogin)).perform(click());

        onView(withText("Please enter email and password."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testWrongCredentials() {
        FakeAuthProvider.setFailNextLogin(true);

        launch();

        onView(withId(R.id.editTextEmail)).perform(typeText("wrong@test.com"));
        onView(withId(R.id.editTextPassword)).perform(typeText("wrongpass"), closeSoftKeyboard());
        onView(withId(R.id.buttonLogin)).perform(click());

        onView(withText("Login failed: Invalid credentials."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLoginIncompleteProfile() {

        FakeAuthProvider.addUser("x@test.com", "pass1234", "UID_INCOMPLETE");

        Map<String, Object> data = new HashMap<>();
        data.put("profileCompleted", false);
        FakeUserStore.putUser("UID_INCOMPLETE", data);

        launch();

        onView(withId(R.id.editTextEmail)).perform(typeText("x@test.com"));
        onView(withId(R.id.editTextPassword)).perform(typeText("pass1234"), closeSoftKeyboard());
        onView(withId(R.id.buttonLogin)).perform(click());

        onView(withText("Please complete your profile."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLoginCompleteProfile() {

        FakeAuthProvider.addUser("test@test.com", "pass1234", "U1");

        Map<String, Object> data = new HashMap<>();
        data.put("profileCompleted", true);
        FakeUserStore.putUser("U1", data);

        Intents.init();

        ActivityScenario<LoginActivity> scenario = launch();

        onView(withId(R.id.editTextEmail)).perform(typeText("test@test.com"));
        onView(withId(R.id.editTextPassword)).perform(typeText("pass1234"), closeSoftKeyboard());
        onView(withId(R.id.buttonLogin)).perform(click());

        intended(hasComponent(HomeActivity.class.getName()));

        Intents.release();
    }



    @Test
    public void testLoginCurrentUserNull() {

        FakeAuthProvider.addUser("u@test.com", "pass", "UIDX");

        FakeAuthProvider.setFailNextLogin(true);

        launch();

        onView(withId(R.id.editTextEmail)).perform(typeText("u@test.com"));
        onView(withId(R.id.editTextPassword)).perform(typeText("pass"), closeSoftKeyboard());
        onView(withId(R.id.buttonLogin)).perform(click());

        onView(withText("Login failed: Invalid credentials."))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }
}
