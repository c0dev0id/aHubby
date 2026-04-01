package de.codevoid.ahubby.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import de.codevoid.ahubby.R;
import de.codevoid.ahubby.model.GpxFile;

public class GpxAdapter extends RecyclerView.Adapter<GpxAdapter.ViewHolder> {

    public interface ToggleListener {
        void onToggle(GpxFile file, boolean newValue);
    }

    private final List<GpxFile> all = new ArrayList<>();
    private final List<GpxFile> filtered = new ArrayList<>();
    private String countryFilter = null;
    private ToggleListener listener;

    public void setItems(List<GpxFile> items) {
        all.clear();
        all.addAll(items);
        applyFilter();
    }

    public void setCountryFilter(String country) {
        countryFilter = country;
        applyFilter();
    }

    public void setToggleListener(ToggleListener l) {
        listener = l;
    }

    private void applyFilter() {
        filtered.clear();
        for (GpxFile f : all) {
            if (countryFilter == null || countryFilter.equals(f.country)) {
                filtered.add(f);
            }
        }
        notifyDataSetChanged();
    }

    public List<String> getCountries() {
        List<String> countries = new ArrayList<>();
        for (GpxFile f : all) {
            if (!f.country.isEmpty() && !countries.contains(f.country)) {
                countries.add(f.country);
            }
        }
        java.util.Collections.sort(countries);
        return countries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gpx, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        GpxFile f = filtered.get(position);
        h.title.setText(f.title);
        h.country.setText(f.country);
        h.publicBadge.setVisibility(f.isPublic ? View.VISIBLE : View.GONE);
        updateButton(h.toggleButton, f.showOnMap, h.itemView.getContext());

        h.toggleButton.setOnClickListener(v -> {
            boolean next = !f.showOnMap;
            f.showOnMap = next;
            updateButton(h.toggleButton, next, v.getContext());
            if (listener != null) listener.onToggle(f, next);
        });
    }

    private void updateButton(MaterialButton btn, boolean active, android.content.Context ctx) {
        if (active) {
            btn.setText(ctx.getString(R.string.toggle_in_dmd2));
            btn.setStrokeColorResource(R.color.primary);
            btn.setTextColor(btn.getContext().getColor(R.color.primary));
        } else {
            btn.setText(ctx.getString(R.string.toggle_show_in_dmd2));
            btn.setStrokeColorResource(R.color.text_secondary);
            btn.setTextColor(btn.getContext().getColor(R.color.text_secondary));
        }
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView country;
        final TextView publicBadge;
        final MaterialButton toggleButton;

        ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.title);
            country = v.findViewById(R.id.country);
            publicBadge = v.findViewById(R.id.public_badge);
            toggleButton = v.findViewById(R.id.toggle_button);
        }
    }
}
