package com.abandiak.alerta.app.teams;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.app.map.MapActivity;
import com.abandiak.alerta.app.messages.MessagesActivity;
import com.abandiak.alerta.app.more.MoreActivity;
import com.abandiak.alerta.app.tasks.TasksActivity;
import com.abandiak.alerta.core.utils.BaseActivity;
import com.abandiak.alerta.core.utils.SystemBars;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.abandiak.alerta.data.model.Team;
import com.abandiak.alerta.data.repository.TeamRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.ListenerRegistration;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TeamsActivity extends BaseActivity {

    private BottomNavigationView bottomNav;
    private View emptyState;
    private RecyclerView recycler;
    private TeamListAdapter adapter;
    private TeamRepository repo;
    private ListenerRegistration reg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBars.apply(this);

        setContentView(R.layout.activity_teams);

        View root = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(
                    v.getPaddingLeft(),
                    topInset,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return insets;
        });

        repo = new TeamRepository();
        emptyState = findViewById(R.id.emptyState);
        recycler = findViewById(R.id.recyclerTeams);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeamListAdapter();
        recycler.setAdapter(adapter);

        adapter.setOnTeamClick(this::showTeamDetailsDialog);

        findViewById(R.id.btnCreateTeam).setOnClickListener(v -> showCreateDialog());
        findViewById(R.id.btnJoinTeam).setOnClickListener(v -> showJoinDialog());

        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_teams);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(this, MapActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_tasks) {
                    startActivity(new Intent(this, TasksActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_messages) {
                    startActivity(new Intent(this, MessagesActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } else if (id == R.id.nav_more) {
                    startActivity(new Intent(this, MoreActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                }
                return id == R.id.nav_teams;
            });
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        reg = repo.listenMyTeams(new TeamRepository.TeamsListener() {
            @Override
            public void onSuccess(List<Team> list) {
                adapter.submit(list);
                boolean empty = list == null || list.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(Exception e) {
                ToastUtils.show(TeamsActivity.this, "Failed to load teams.");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent().getBooleanExtra("open_join_team", false)) {
            getIntent().removeExtra("open_join_team");
            showJoinDialog();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }

    private void showCreateDialog() {

        View view = getLayoutInflater().inflate(R.layout.dialog_create_team, null);

        EditText inputName = view.findViewById(R.id.inputTeamName);
        EditText inputDesc = view.findViewById(R.id.inputTeamDesc);
        EditText inputRegion = view.findViewById(R.id.inputTeamRegion);
        View colorPreview = view.findViewById(R.id.viewColorPreview);
        Button btnPickColor = view.findViewById(R.id.btnPickColor);

        final int[] selectedColor = { Color.RED };

        btnPickColor.setOnClickListener(v -> {
            View pickerView = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
            ColorPickerView colorPickerView = pickerView.findViewById(R.id.colorPickerView);
            View preview = pickerView.findViewById(R.id.colorPreview);
            EditText inputR = pickerView.findViewById(R.id.inputR);
            EditText inputG = pickerView.findViewById(R.id.inputG);
            EditText inputB = pickerView.findViewById(R.id.inputB);
            Button btnCancel = pickerView.findViewById(R.id.btnCancel);
            Button btnOk = pickerView.findViewById(R.id.btnOk);

            final int[] dialogColor = { selectedColor[0] };
            preview.setBackgroundColor(dialogColor[0]);

            inputR.setText(String.valueOf(Color.red(dialogColor[0])));
            inputG.setText(String.valueOf(Color.green(dialogColor[0])));
            inputB.setText(String.valueOf(Color.blue(dialogColor[0])));

            colorPickerView.setColorListener((ColorEnvelopeListener) (envelope, fromUser) -> {
                int c = envelope.getColor();
                dialogColor[0] = c;
                preview.setBackgroundColor(c);
                inputR.setText(String.valueOf(Color.red(c)));
                inputG.setText(String.valueOf(Color.green(c)));
                inputB.setText(String.valueOf(Color.blue(c)));
            });

            AlertDialog pickerDialog = new AlertDialog.Builder(this)
                    .setView(pickerView)
                    .create();

            btnCancel.setOnClickListener(x -> pickerDialog.dismiss());
            btnOk.setOnClickListener(x -> {
                selectedColor[0] = dialogColor[0];
                ((GradientDrawable) colorPreview.getBackground().mutate())
                        .setColor(selectedColor[0]);
                pickerDialog.dismiss();
            });

            pickerDialog.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btnCreate).setOnClickListener(v -> {

            String name = inputName.getText().toString().trim();
            String desc = inputDesc.getText().toString().trim();
            String region = inputRegion.getText().toString().trim();

            if (name.isEmpty()) {
                inputName.setError("Name required");
                return;
            }

            repo.createTeam(name, desc, selectedColor[0], region, (ok, msg) -> {
                ToastUtils.show(this, ok ? "Team created." : msg);
                if (ok) dialog.dismiss();
            });
        });

        dialog.show();
    }


    private void showTeamDetailsDialog(Team team) {
        View view = getLayoutInflater().inflate(R.layout.dialog_team_details, null);

        TextView textName = view.findViewById(R.id.textTeamName);
        TextView textDesc = view.findViewById(R.id.textTeamDescription);
        TextView textCode = view.findViewById(R.id.textTeamCode);
        TextView textCreatedBy = view.findViewById(R.id.textCreatedBy);
        TextView textCreatedAt = view.findViewById(R.id.textCreatedAt);

        textName.setText(team.getName());
        textDesc.setText(team.getDescription() == null || team.getDescription().isEmpty()
                ? "No description provided."
                : team.getDescription());
        textCode.setText("Code: " + (team.getCode() == null ? "—" : team.getCode()));
        String createdBy = (team.getCreatedByName() != null && !team.getCreatedByName().isEmpty())
                ? team.getCreatedByName()
                : "Unknown";
        textCreatedBy.setText("Created by: " + createdBy);


        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String date = team.getCreatedAt() > 0 ? sdf.format(new java.util.Date(team.getCreatedAt())) : "—";
        textCreatedAt.setText("Created at: " + date);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnEdit).setOnClickListener(v -> {
            dialog.dismiss();
            showEditTeamDialog(team);
        });

        view.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            View confirmView = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);
            AlertDialog confirmDialog = new AlertDialog.Builder(this)
                    .setView(confirmView)
                    .create();

            confirmView.findViewById(R.id.btnCancel).setOnClickListener(v2 -> confirmDialog.dismiss());
            confirmView.findViewById(R.id.btnConfirm).setOnClickListener(v2 -> {
                repo.deleteTeam(team.getId(), success -> {
                    if (success) {
                        ToastUtils.show(this, "Team deleted.");
                        confirmDialog.dismiss();
                        dialog.dismiss();
                    } else {
                        ToastUtils.show(this, "Failed to delete team.");
                    }
                });
            });

            confirmDialog.show();
        });

        dialog.show();
    }

    private void showEditTeamDialog(Team team) {

        View view = getLayoutInflater().inflate(R.layout.dialog_create_team, null);

        TextView dialogTitle = view.findViewById(R.id.dialogTitle);
        EditText inputName = view.findViewById(R.id.inputTeamName);
        EditText inputDesc = view.findViewById(R.id.inputTeamDesc);
        EditText inputRegion = view.findViewById(R.id.inputTeamRegion);
        View colorPreview = view.findViewById(R.id.viewColorPreview);
        Button btnPickColor = view.findViewById(R.id.btnPickColor);
        Button btnSave = view.findViewById(R.id.btnCreate);

        dialogTitle.setText("Edit Team");
        btnSave.setText("Save");

        inputName.setText(team.getName());
        inputDesc.setText(team.getDescription());
        inputRegion.setText(team.getRegion());
        final int[] selectedColor = { team.getColor() };

        ((GradientDrawable) colorPreview.getBackground().mutate())
                .setColor(selectedColor[0]);

        btnPickColor.setOnClickListener(v -> {
            View pickerView = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
            ColorPickerView colorPickerView = pickerView.findViewById(R.id.colorPickerView);
            View preview = pickerView.findViewById(R.id.colorPreview);
            EditText inputR = pickerView.findViewById(R.id.inputR);
            EditText inputG = pickerView.findViewById(R.id.inputG);
            EditText inputB = pickerView.findViewById(R.id.inputB);
            Button btnCancel = pickerView.findViewById(R.id.btnCancel);
            Button btnOk = pickerView.findViewById(R.id.btnOk);

            final int[] dialogColor = { selectedColor[0] };
            preview.setBackgroundColor(dialogColor[0]);

            inputR.setText(String.valueOf(Color.red(dialogColor[0])));
            inputG.setText(String.valueOf(Color.green(dialogColor[0])));
            inputB.setText(String.valueOf(Color.blue(dialogColor[0])));

            colorPickerView.setColorListener((ColorEnvelopeListener) (ColorEnvelope envelope, boolean fromUser) -> {
                int c = envelope.getColor();
                dialogColor[0] = c;
                preview.setBackgroundColor(c);
                inputR.setText(String.valueOf(Color.red(c)));
                inputG.setText(String.valueOf(Color.green(c)));
                inputB.setText(String.valueOf(Color.blue(c)));
            });

            AlertDialog pickerDialog = new AlertDialog.Builder(this)
                    .setView(pickerView)
                    .create();

            btnCancel.setOnClickListener(x -> pickerDialog.dismiss());
            btnOk.setOnClickListener(x -> {
                selectedColor[0] = dialogColor[0];
                ((GradientDrawable) colorPreview.getBackground().mutate())
                        .setColor(selectedColor[0]);
                pickerDialog.dismiss();
            });

            pickerDialog.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        view.findViewById(R.id.btnCancel)
                .setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {

            String newName = inputName.getText().toString().trim();
            String newDesc = inputDesc.getText().toString().trim();
            String newRegion = inputRegion.getText().toString().trim();

            if (newName.isEmpty()) {
                inputName.setError("Name required");
                return;
            }

            team.setName(newName);
            team.setDescription(newDesc);
            team.setRegion(newRegion);
            team.setColor(selectedColor[0]);

            repo.updateTeam(team, (ok, msg) -> {
                ToastUtils.show(this, ok ? "Team updated." : msg);
                if (ok) dialog.dismiss();
            });
        });

        dialog.show();
    }


    private int parseColorValue(String text) {
        if (text == null || text.isEmpty()) return 0;
        try {
            int v = Integer.parseInt(text);
            return Math.max(0, Math.min(255, v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showJoinDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_join_team, null);
        EditText input = view.findViewById(R.id.inputCode);

        AlertDialog d = new AlertDialog.Builder(this).setView(view).create();
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> d.dismiss());
        view.findViewById(R.id.btnJoin).setOnClickListener(v -> {
            String code = input.getText() == null ? "" : input.getText().toString();
            if (code.trim().length() != 6) {
                input.setError("Enter 6-char code");
                return;
            }
            repo.joinByCode(code, (ok, msg) -> {
                ToastUtils.show(this, ok ? "Joined team." : msg);
                if (ok) d.dismiss();
            });
        });
        d.show();
    }
}
