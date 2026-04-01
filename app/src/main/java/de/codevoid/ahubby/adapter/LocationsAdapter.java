package de.codevoid.ahubby.adapter;

import android.graphics.PorterDuff;
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
import de.codevoid.ahubby.model.HubLocation;

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.ViewHolder> {

    public interface ToggleListener {
        void onToggle(HubLocation loc, boolean newValue);
    }

    private final List<HubLocation> items = new ArrayList<>();
    private ToggleListener listener;

    public void setItems(List<HubLocation> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public void setToggleListener(ToggleListener l) {
        listener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        HubLocation loc = items.get(position);
        h.title.setText(loc.title);
        h.category.setText(loc.mainCategory);
        h.country.setText(loc.country.isEmpty() ? "" : "· " + loc.country);
        h.publicBadge.setVisibility(loc.isPublic ? View.VISIBLE : View.GONE);

        int color = categoryColor(h.itemView, loc.mainCategory);
        h.categoryDot.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);

        updateButton(h.toggleButton, loc.showOnMap);

        h.toggleButton.setOnClickListener(v -> {
            boolean next = !loc.showOnMap;
            loc.showOnMap = next;
            updateButton(h.toggleButton, next);
            if (listener != null) listener.onToggle(loc, next);
        });
    }

    private int categoryColor(View v, String category) {
        if (category == null) return v.getContext().getColor(R.color.cat_default);
        String lower = category.toLowerCase();
        if (lower.contains("camp")) return v.getContext().getColor(R.color.cat_camping);
        if (lower.contains("fuel") || lower.contains("gas") || lower.contains("petrol"))
            return v.getContext().getColor(R.color.cat_fuel);
        if (lower.contains("food") || lower.contains("restaurant") || lower.contains("eat"))
            return v.getContext().getColor(R.color.cat_food);
        if (lower.contains("hotel") || lower.contains("accomm") || lower.contains("sleep"))
            return v.getContext().getColor(R.color.cat_accommodation);
        if (lower.contains("view") || lower.contains("scenic") || lower.contains("point"))
            return v.getContext().getColor(R.color.cat_viewpoint);
        return v.getContext().getColor(R.color.cat_default);
    }

    private void updateButton(MaterialButton btn, boolean active) {
        if (active) {
            btn.setText(btn.getContext().getString(R.string.toggle_in_dmd2));
            btn.setStrokeColorResource(R.color.primary);
            btn.setTextColor(btn.getContext().getColor(R.color.primary));
        } else {
            btn.setText(btn.getContext().getString(R.string.toggle_show_in_dmd2));
            btn.setStrokeColorResource(R.color.text_secondary);
            btn.setTextColor(btn.getContext().getColor(R.color.text_secondary));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View categoryDot;
        final TextView title;
        final TextView category;
        final TextView country;
        final TextView publicBadge;
        final MaterialButton toggleButton;

        ViewHolder(View v) {
            super(v);
            categoryDot = v.findViewById(R.id.category_dot);
            title = v.findViewById(R.id.title);
            category = v.findViewById(R.id.category);
            country = v.findViewById(R.id.country);
            publicBadge = v.findViewById(R.id.public_badge);
            toggleButton = v.findViewById(R.id.toggle_button);
        }
    }
}
