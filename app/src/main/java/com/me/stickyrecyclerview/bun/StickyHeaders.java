package com.me.stickyrecyclerview.bun;

import android.view.View;

/**
 * Created by hvngoc on 11/16/20
 */
public interface StickyHeaders {
    boolean isStickyHeader(int position);

    interface ViewSetup {
        /**
         * Adjusts any necessary properties of the {@code holder} that is being used as a sticky header.
         * <p>
         * {@link #teardownStickyHeaderView(View)} will be called sometime after this method
         * and before any other calls to this method go through.
         */
        void setupStickyHeaderView(View stickyHeader);

        /**
         * Reverts any properties changed in {@link #setupStickyHeaderView(View)}.
         * <p>
         * Called after {@link #setupStickyHeaderView(View)}.
         */
        void teardownStickyHeaderView(View stickyHeader);
    }
}
