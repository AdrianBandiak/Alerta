package com.abandiak.alerta.app.messages;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.app.map.MapActivity;
import com.abandiak.alerta.app.more.MoreActivity;
import com.abandiak.alerta.app.tasks.TasksActivity;
import com.abandiak.alerta.app.teams.TeamsActivity;
import com.abandiak.alerta.core.utils.BaseActivity;
import com.abandiak.alerta.core.utils.SystemBars;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MessagesActivity extends BaseActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBars.apply(this);
        setContentView(R.layout.activity_messages);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        viewPager.setAdapter(new MessagesPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, pos) -> {
            if (pos == 0) tab.setText("Direct Messages");
            else tab.setText("Team Chats");
        }).attach();

        setupBottomNav();
    }

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_messages);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home)
                startActivity(new Intent(this, HomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            else if (id == R.id.nav_map)
                startActivity(new Intent(this, MapActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            else if (id == R.id.nav_tasks)
                startActivity(new Intent(this, TasksActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            else if (id == R.id.nav_teams)
                startActivity(new Intent(this, TeamsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            else if (id == R.id.nav_more)
                startActivity(new Intent(this, MoreActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));

            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_messages);
        }
    }
}

