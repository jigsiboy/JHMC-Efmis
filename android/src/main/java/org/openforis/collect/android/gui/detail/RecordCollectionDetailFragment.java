package org.openforis.collect.android.gui.detail;

import android.view.Menu;
import android.view.MenuInflater;

import org.openforis.collect.android.viewmodel.UiInternalNode;
import org.openforis.collect.android.viewmodel.UiNode;
import org.openforis.collect.android.viewmodel.UiRecordCollection;

/**
 * @author Daniel Wiell
 */
public class RecordCollectionDetailFragment extends AbstractNodeCollectionDetailFragment<UiRecordCollection> {

    protected UiInternalNode addNode() {
         return surveyService().addRecord(node().getName());
    }

    protected UiInternalNode getSelectedNode(int position, UiRecordCollection recordCollection) {
        UiNode recordPlaceholder = recordCollection.getChildAt(position);
        return surveyService().selectRecord(recordPlaceholder.getId());
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //hide action menu
    }

    public void onPrepareOptionsMenu(Menu menu) {
        //action menu is hidden
    }

}
