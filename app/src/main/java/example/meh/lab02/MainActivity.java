package example.meh.lab02;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //sample url: feeds.bbci.co.uk/news/world/rss.xml

    private static final String TAG = "MainActivity";
    public static final int RESULT_CODE_UPDATE = 0;

    private RecyclerView mRecyclerView;
    private EditText filterText;
    private Button mFetchFeedButton;
    private SwipeRefreshLayout mSwipeLayout;
    private TextView mFeedTitleTextView;
    private TextView mFeedLinkTextView;
    private TextView mFeedDescriptionTextView;

    private List<RssFeedModel> mFeedModelList;
    private String mFeedTitle;
    private String mFeedLink;
    private String mFeedDescription;
    private String urlLink;
    private String filter;
    private int maxItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recyclerView);
        filterText = findViewById(R.id.filterRssText);
        mFetchFeedButton = findViewById(R.id.fetchFeedButton);
        mSwipeLayout = findViewById(R.id.swipeRefreshLayout);
        mFeedTitleTextView = findViewById(R.id.feedTitle);
        mFeedDescriptionTextView = findViewById(R.id.feedDescription);
        mFeedLinkTextView = findViewById(R.id.feedLink);
        filter = "";

        urlLink = "";
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        maxItems = 1;

        mFetchFeedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (urlLink != "") {
                    filter = filterText.getText().toString();
                    new FetchFeedTask().execute((Void) null);
                }
            }
        });
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new FetchFeedTask().execute((Void) null);
            }
        });

        Button preferences = findViewById(R.id.prefButton);
        preferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent actionTransfer = new Intent(MainActivity.this, UserActivity.class);
                actionTransfer.putExtra("url", urlLink);
                startActivityForResult(actionTransfer, RESULT_CODE_UPDATE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        if (requestCode != RESULT_CODE_UPDATE) return;

        if (resultCode != Activity.RESULT_OK) return;

        urlLink = data.getStringExtra(UserActivity.PARAM_URL);
        maxItems = Integer.parseInt(data.getStringExtra(UserActivity.PARAM_MAX));
        new FetchFeedTask().execute((Void) null);
    }

    private class FetchFeedTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            mSwipeLayout.setRefreshing(true);
            mFeedTitle = null;
            mFeedLink = null;
            mFeedDescription = null;
            mFeedTitleTextView.setText("Feed Title: " + mFeedTitle);
            mFeedDescriptionTextView.setText("Feed Description: " + mFeedDescription);
            mFeedLinkTextView.setText("Feed Link: " + mFeedLink);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (TextUtils.isEmpty(urlLink))
                return false;

            try {
                if(!urlLink.startsWith("http://") && !urlLink.startsWith("https://"))
                    urlLink = "http://" + urlLink;

                URL url = new URL(urlLink);
                InputStream inputStream = url.openConnection().getInputStream();
                mFeedModelList = parseFeed(inputStream);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error", e);
            } catch (XmlPullParserException e) {
                Log.e(TAG, "Error", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mSwipeLayout.setRefreshing(false);

            if (success) {
                mFeedTitleTextView.setText("Feed Title: " + mFeedTitle);
                mFeedDescriptionTextView.setText("Feed Description: " + mFeedDescription);
                mFeedLinkTextView.setText("Feed Link: " + mFeedLink);
                // Fill RecyclerView
                mRecyclerView.setAdapter(new RssFeedListAdapter(mFeedModelList));
            } else {
                Toast.makeText(MainActivity.this,
                        "Enter a valid Rss feed url",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public List<RssFeedModel> parseFeed(InputStream inputStream) throws XmlPullParserException, IOException {
        String title = null;
        String link = null;
        String description = null;
        boolean isItem = false;
        List<RssFeedModel> items = new ArrayList<>();

        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlPullParser.setInput(inputStream, null);

            xmlPullParser.nextTag();
            while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                int eventType = xmlPullParser.getEventType();

                String name = xmlPullParser.getName();
                if(name == null)
                    continue;

                if(eventType == XmlPullParser.END_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = false;
                    }
                    continue;
                }

                if (eventType == XmlPullParser.START_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = true;
                        continue;
                    }
                }

                Log.d("MainActivity", "Parsing name ==> " + name);
                String result = "";
                if (xmlPullParser.next() == XmlPullParser.TEXT) {
                    result = xmlPullParser.getText();
                    xmlPullParser.nextTag();
                }

                if (name.equalsIgnoreCase("title")) {
                    title = result;
                } else if (name.equalsIgnoreCase("link")) {
                    if (title == null) {
                        continue;
                    }
                    else {
                        link = result;
                    }

                } else if (name.equalsIgnoreCase("description")) {
                    description = result;
                }

                if (title != null && link != null && description != null) {
                    if(isItem) {
                        if(items.size() < maxItems){
                            if (filter != "" && title.matches(".*"+filter+".*")) {
                                RssFeedModel item = new RssFeedModel(title, link, description);
                                items.add(item);
                            }
                            else if (filter == ""){
                                RssFeedModel item = new RssFeedModel(title, link, description);
                                items.add(item);
                            }
                        }
                    }
                    else {
                        mFeedTitle = title;
                        mFeedLink = link;
                        mFeedDescription = description;
                    }

                    title = null;
                    link = null;
                    description = null;
                    isItem = false;
                }
            }

        return items;
        } finally {
            inputStream.close();
        }
    }

    public class RssFeedListAdapter
            extends RecyclerView.Adapter<RssFeedListAdapter.FeedModelViewHolder> {

        private List<RssFeedModel> mRssFeedModels;

        public class FeedModelViewHolder extends RecyclerView.ViewHolder {
            private View rssFeedView;

            public FeedModelViewHolder(View v) {
                super(v);
                rssFeedView = v;
            }
        }

        public RssFeedListAdapter(List<RssFeedModel> rssFeedModels) {
            mRssFeedModels = rssFeedModels;
        }

        @Override
        public FeedModelViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_rss_feed, parent, false);
            FeedModelViewHolder holder = new FeedModelViewHolder(v);
            return holder;
        }

        @Override
        public void onBindViewHolder(FeedModelViewHolder holder, int position) {
            final RssFeedModel rssFeedModel = mRssFeedModels.get(position);
            ((TextView)holder.rssFeedView.findViewById(R.id.titleText)).setText(rssFeedModel.title);
            ((TextView)holder.rssFeedView.findViewById(R.id.descriptionText)).setText(rssFeedModel.description);
            ((TextView)holder.rssFeedView.findViewById(R.id.linkText)).setText(rssFeedModel.link);
            holder.rssFeedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent readAction = new Intent(MainActivity.this, ReaderActivity.class);
                    readAction.putExtra("web", rssFeedModel.link);
                    startActivity(readAction);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mRssFeedModels.size();
        }
    }

    public class RssFeedModel {

        public String title;
        public String link;
        public String description;

        public RssFeedModel(String title, String link, String description) {
            this.title = title;
            this.link = link;
            this.description = description;
        }
    }
}


