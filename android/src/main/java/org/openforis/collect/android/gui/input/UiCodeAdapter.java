package org.openforis.collect.android.gui.input;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.R;
import org.openforis.collect.android.viewmodel.UiCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Daniel Wiell
 */
class UiCodeAdapter extends ArrayAdapter<UiCode> {
    private static final int LAYOUT_RESOURCE_ID = R.layout.wrapping_dropdown_item;
    private final Context context;
    private final List<UiCode> codes;
    private List<UiCode> filteredCodes;

    UiCodeAdapter(Context context, List<UiCode> codes) {
        super(context, LAYOUT_RESOURCE_ID, codes);
        this.context = context;
        this.codes = codes;
        this.filteredCodes = new CopyOnWriteArrayList<UiCode>(codes);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        CodeHolder holder;
        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(LAYOUT_RESOURCE_ID, parent, false);

            holder = new CodeHolder();
            holder.label = (TextView) row.findViewById(R.id.label);
            holder.description = row.findViewById(R.id.description);

            row.setTag(holder);
        } else {
            holder = (CodeHolder) row.getTag();
        }

        UiCode code = filteredCodes.get(position);
        holder.label.setText(code.toString());
        String description = code.getDescription();
        if (StringUtils.isBlank(description)) {
            holder.description.setVisibility(View.GONE);
        } else {
            description = StringUtils.prependIfMissing(description, "(", "[");
            description = StringUtils.appendIfMissing(description, ")", "]");
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(description);
        }
        return row;
    }

    public UiCode getItem(int position) {
        return filteredCodes.get(position);
    }

    public int getCount() {
        return filteredCodes.size();
    }

    public Filter getFilter() {
        return new Filter() {
            protected FilterResults performFiltering(CharSequence constraint) {
                List<UiCode> codes = findCodes(constraint);
                FilterResults results = new FilterResults();
                results.values = codes;
                results.count = codes.size();
                return results;
            }

            private List<UiCode> findCodes(CharSequence constraint) {
                if (constraint == null || constraint.length() == 0)
                    return codes;
                String query = constraint.toString().trim().toLowerCase();
                ArrayList<UiCode> matchingCodes = new ArrayList<UiCode>();
                String[] terms = query.split(" ");
                for (UiCode code : codes) {
                    if (matches(code, terms))
                        matchingCodes.add(code);
                }
                return matchingCodes;
            }

            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredCodes.clear();
                if (results.values != null)
                    filteredCodes.addAll((List<UiCode>) results.values);
                if (results.count > 0)
                    notifyDataSetChanged();
                else
                    notifyDataSetInvalidated();
            }
        };
    }

    private boolean matches(UiCode code, String[] terms) {
        for (String term : terms) {
            if (!matches(term, code.getValue()) && !matches(term, code.getLabel()) && !matches(term, code.toString()))
                return false;
        }
        return true;
    }

    private boolean matches(String term, String s) {
        return s != null && s.trim().toLowerCase().matches(".*\\b" + term + ".*");
    }

    private static class CodeHolder {
        TextView label;
        TextView description;
    }
}
