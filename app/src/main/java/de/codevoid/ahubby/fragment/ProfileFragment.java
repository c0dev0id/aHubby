package de.codevoid.ahubby.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import de.codevoid.ahubby.LoginActivity;
import de.codevoid.ahubby.R;
import de.codevoid.ahubby.auth.AuthStore;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle saved) {
        AuthStore store = new AuthStore(requireContext());

        setRow(view, R.id.row_display_name, getString(R.string.label_display_name),
                emptyOr(store.getDisplayName()));
        setRow(view, R.id.row_email, getString(R.string.label_email),
                emptyOr(store.getEmail()));
        setRow(view, R.id.row_device, getString(R.string.label_device),
                emptyOr(store.getDeviceName()));
        setRow(view, R.id.row_map_license, getString(R.string.label_map_license),
                store.getMapLicense() ? getString(R.string.label_yes) : getString(R.string.label_no));
        setRow(view, R.id.row_community_points, getString(R.string.label_community_points),
                String.valueOf(store.getCommunityPoints()));

        view.findViewById(R.id.logout_button).setOnClickListener(v -> {
            store.clear();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void setRow(View parent, int rowId, String label, String value) {
        View row = parent.findViewById(rowId);
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        ((TextView) row.findViewById(R.id.row_value)).setText(value);
    }

    private String emptyOr(String s) {
        return (s == null || s.isEmpty()) ? getString(R.string.label_none) : s;
    }
}
