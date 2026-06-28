package com.tiredcity.app.data.model.search;

/**
 * Base type for the "Gợi ý cho bạn" discovery list, which mixes promotions,
 * events and products inside a single RecyclerView with multiple view types.
 */
public interface SearchItem {

    int TYPE_PROMOTION = 0;
    int TYPE_EVENT     = 1;
    int TYPE_PRODUCT   = 2;

    int getType();
}
