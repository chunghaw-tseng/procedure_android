package com.factory.procedure.pictureapp.touch_helpers;


/**
 * Interface for ItemTouch, to detect the touch on the viewHolder
 */
public interface ItemTouchHelperAdapter {

//    Function called on item move
    boolean onItemMove(int fromPosition, int toPosition);

//    Function called on item dismiss (Swipe)
    void onItemDismiss(int position);

}
