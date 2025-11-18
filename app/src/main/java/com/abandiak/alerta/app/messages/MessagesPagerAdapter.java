package com.abandiak.alerta.app.messages;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MessagesPagerAdapter extends FragmentStateAdapter {

    public MessagesPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new MessagesDMFragment();
        else return new MessagesTeamsFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
