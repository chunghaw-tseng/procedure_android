package com.factory.procedure.pictureapp.touch_helpers;

import android.support.v7.widget.RecyclerView;

/**
 * Interface for the dragging
 */

public interface OnStartDragListener {

//  Called on the activity when the viewHolder is dragged
    void onStartDrag(RecyclerView.ViewHolder viewHolder);
}
