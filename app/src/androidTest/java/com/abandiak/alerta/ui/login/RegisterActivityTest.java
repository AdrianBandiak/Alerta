package com.abandiak.alerta.ui.login;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.core.app.ActivityScenario;

import static org.junit.Assert.assertTrue;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.auth.RegisterActivity;
import com.abandiak.alerta.util.*;

import org.junit.Before;
import org.junit.Test;

public class RegisterActivityTest {

    @Before
    public void setup() {
        FakeAuthProvider.reset();
        FakeUserStore.reset();

        RegisterActivity.authOverride = new FakeAuthProvider();
        RegisterActivity.dbOverride = new FakeFirestoreProvider();
        RegisterActivity.disableFinishForTests = true;
    }

    private ActivityScenario<RegisterActivity> launch() {
        return ActivityScenario.launch(RegisterActivity.class);
    }

    @Test
    public void testEmptyFields() {
        launch();

        onView(withId(R.id.registerButton)).perform(click());

        onView(withText(R.string.fill_all_fields))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testInvalidEmail() {
        launch();

        onView(withId(R.id.emailInput)).perform(typeText("badEmail"), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("Password1"));
        onView(withId(R.id.confirmPasswordInput)).perform(typeText("Password1"));
        onView(withId(R.id.registerButton)).perform(click());

        onView(withText(R.string.invalid_email))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testWeakPassword() {
        launch();

        onView(withId(R.id.emailInput)).perform(typeText("test@test.com"));
        onView(withId(R.id.passwordInput)).perform(typeText("123"));
        onView(withId(R.id.confirmPasswordInput)).perform(typeText("123"), closeSoftKeyboard());
        onView(withId(R.id.registerButton)).perform(click());

        onView(withText(R.string.invalid_password))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testPasswordsDoNotMatch() {
        launch();

        onView(withId(R.id.emailInput)).perform(typeText("test@test.com"));
        onView(withId(R.id.passwordInput)).perform(typeText("Password1"));
        onView(withId(R.id.confirmPasswordInput)).perform(typeText("Password2"), closeSoftKeyboard());
        onView(withId(R.id.registerButton)).perform(click());

        onView(withText(R.string.passwords_do_not_match))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSuccessfulRegistration() {

        RegisterActivity.disableFinishForTests = true;
        RegisterActivity.testNavigatedToLogin = false;

        RegisterActivity.authOverride = new FakeAuthProvider();
        RegisterActivity.dbOverride = new FakeFirestoreProvider();

        ActivityScenario.launch(RegisterActivity.class);

        onView(withId(R.id.emailInput)).perform(typeText("user@test.com"), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("Test1234"), closeSoftKeyboard());
        onView(withId(R.id.confirmPasswordInput)).perform(typeText("Test1234"), closeSoftKeyboard());

        onView(withId(R.id.registerButton)).perform(click());

        assertTrue(RegisterActivity.testNavigatedToLogin);
    }

}
