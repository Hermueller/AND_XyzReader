package com.example.xyzreader.ui;

import android.support.design.widget.AppBarLayout;
import android.support.v4.app.LoaderManager;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.ReaderAdapterClickListener;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, ReaderAdapterClickListener {

    private static final String TAG = ArticleListActivity.class.toString();
    private RecyclerView mRecyclerView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mRecyclerView = findViewById(R.id.recycler_view);
        getSupportLoaderManager().initLoader(0, null, this);

        setToolbarTitleWhenCollapsed();
    }

    /**
     * Code from:  https://stackoverflow.com/a/32724422
     */
    private void setToolbarTitleWhenCollapsed() {
        final CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_layout);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        final ImageView toolbarIv = findViewById(R.id.toolbar_iv);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbarLayout.setTitle(getString(R.string.welcome_xyz_reader));
                    toolbarIv.setVisibility(View.GONE);
                    isShow = true;
                } else if(isShow) {
                    collapsingToolbarLayout.setTitle(" ");
                    toolbarIv.setVisibility(View.VISIBLE);
                    isShow = false;
                }
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor, this);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int spanCount = getResources().getInteger(R.integer.list_column_count);
        GridLayoutManager glm = new GridLayoutManager(this, spanCount);
        mRecyclerView.setLayoutManager(glm);

        /* ITEM-DECORATION NOT USED
         * Reason: It's too much code only to draw one single line.
          * The same result can be achieved by a View. */

        /*DividerItemDecoration divider = new DividerItemDecoration(
                mRecyclerView.getContext(),
                getDrawable(R.drawable.padded_divider),
                DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(divider);*/
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onReadItemClick(long id, ImageView thumbnail) {
        Intent intent = new Intent(ArticleListActivity.this , ArticleDetailActivity.class);
        // Pass data object in the bundle and populate details activity.
        intent.putExtra(ArticleDetailActivity.EXTRA_READER_IMAGE_THUMBNAIL_TRANSITION_NAME,
                ViewCompat.getTransitionName(thumbnail));
        intent.setData(ItemsContract.Items.buildItemUri(id));

        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(
                        this,
                        thumbnail,
                        ViewCompat.getTransitionName(thumbnail));
        startActivity(intent, options.toBundle());
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;
        private boolean isOfCards = false;

        ReaderAdapterClickListener mListener;

        Adapter(Cursor cursor, ReaderAdapterClickListener listener) {
            mCursor = cursor;
            mListener = listener;
            isOfCards = getResources().getBoolean(R.bool.detail_is_card);
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            return new ViewHolder(view);
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

            final String transitionName = holder.titleView.getText().toString().replace(" ", "_") + "_thumbnail";
            ViewCompat.setTransitionName(holder.thumbnailView, transitionName);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onReadItemClick(
                            getItemId(holder.getAdapterPosition()), holder.thumbnailView);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        DynamicHeightNetworkImageView thumbnailView;
        TextView titleView;
        TextView subtitleView;

        ViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }
}
