package org.openforis.collect.android.Network;

import org.openforis.collect.android.sqlite.DataModel.RecordHolder;

public interface ResponseListener {
    void onResponse(RecordHolder recordHolder);
}
