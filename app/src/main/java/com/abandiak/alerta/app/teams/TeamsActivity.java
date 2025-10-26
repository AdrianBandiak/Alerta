package com.abandiak.alerta.app.teams;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.app.map.MapActivity;
import com.abandiak.alerta.app.more.MoreActivity;
import com.abandiak.alerta.app.tasks.TasksActivity;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.abandiak.alerta.data.model.Team;
import com.abandiak.alerta.data.repository.TeamRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;

public class TeamsActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private View emptyState;
    private RecyclerView recycler;
    private TeamListAdapter adapter;
    private TeamRepository repo;
    private ListenerRegistration reg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_gray));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.status_bar_gray));
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_teams);

        repo = new TeamRepository();
        emptyState = findViewById(R.id.emptyState);
        recycler = findViewById(R.id.recyclerTeams);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeamListAdapter();
        recycler.setAdapter(adapter);

        adapter.setOnTeamClick(team -> {
            ToastUtils.show(this, "Open team: " + team.getName());
        });

        findViewById(R.id.btnCreateTeam).setOnClickListener(v -> showCreateDialog());
        findViewById(R.id.btnJoinTeam).setOnClickListener(v -> showJoinDialog());

        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_teams);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(this, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_tasks) {
                    startActivity(new Intent(this, TasksActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_more) {
                    startActivity(new Intent(this, MoreActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                }
                overridePendingTransition(0, 0);
                return id == R.id.nav_teams;
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        reg = repo.listenMyTeams(new TeamRepository.TeamsListener() {
            @Override public void onSuccess(List<Team> list) {
                adapter.submit(list);
                boolean empty = list == null || list.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
            @Override public void onError(Exception e) {
                ToastUtils.show(TeamsActivity.this, "Failed to load teams.");
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    private void showCreateDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_create_team, null);
        TextInputEditText inputName = view.findViewById(R.id.inputTeamName);
        TextInputEditText inputDesc = view.findViewById(R.id.inputTeamDesc);

        AlertDialog d = new AlertDialog.Builder(this).setView(view).create();
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> d.dismiss());
        view.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String name = text(inputName);
            String desc = text(inputDesc);
            if (name.isEmpty()) { inputName.setError("Name required"); return; }
            repo.createTeam(name, desc, (ok, msg) -> {
                ToastUtils.show(this, ok ? "Team created." : msg);
                if (ok) d.dismiss();
            });
        });
        d.show();
    }

    private void showJoinDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_join_team, null);
        EditText input = view.findViewById(R.id.inputCode);

        AlertDialog d = new AlertDialog.Builder(this).setView(view).create();
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> d.dismiss());
        view.findViewById(R.id.btnJoin).setOnClickListener(v -> {
            String code = input.getText()==null ? "" : input.getText().toString();
            if (code.trim().length() != 6) { input.setError("Enter 6-char code"); return; }
            repo.joinByCode(code, (ok, msg) -> {
                ToastUtils.show(this, ok ? "Joined team." : msg);
                if (ok) d.dismiss();
            });
        });
        d.show();
    }

    private static String text(EditText e){ return e.getText()==null? "": e.getText().toString().trim(); }
}
