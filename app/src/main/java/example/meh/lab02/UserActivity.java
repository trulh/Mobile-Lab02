package example.meh.lab02;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = "UserActivity";
    public static final String PARAM_URL = "url";
    public static final String PARAM_MAX = "max";

    private RecyclerView mRecyclerView;
    private EditText mEditText;
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
    private int maxItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        mRecyclerView = findViewById(R.id.recyclerView);
        mEditText = findViewById(R.id.rssFeedEditText);
        mFetchFeedButton = findViewById(R.id.fetchFeedButton);
        mSwipeLayout = findViewById(R.id.swipeRefreshLayout);
        mFeedTitleTextView = findViewById(R.id.feedTitle);
        mFeedDescriptionTextView = findViewById(R.id.feedDescription);
        mFeedLinkTextView = findViewById(R.id.feedLink);
        mEditText.setText(getIntent().getStringExtra("url"));
        urlLink = mEditText.getText().toString();
        mFetchFeedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FetchFeedTask().execute((Void) null);
            }
        });

        Button update = findViewById(R.id.updateBtn);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent updatePreferences = new Intent(UserActivity.this, MainActivity.class);
                if(mEditText.getText().toString() != "") {
                    updatePreferences.putExtra(PARAM_URL, urlLink);
                }
                updatePreferences.putExtra(PARAM_MAX, Integer.toString(maxItems));
                setResult(Activity.RESULT_OK, updatePreferences);
                finish();
            }
        });
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.r10:
                if (checked)
                    maxItems = 10;
                    break;
            case R.id.r20:
                if (checked)
                    maxItems = 20;
                    break;
            case R.id.r50:
                if (checked)
                    maxItems = 50;
                    break;
            case R.id.r100:
                if (checked)
                    maxItems = 100;
                    break;
            default: maxItems = 10;
        }
    }

    private class FetchFeedTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            mSwipeLayout.setRefreshing(true);
            mFeedTitleTextView.setText("Feed Title: " + mFeedTitle);
            mFeedDescriptionTextView.setText("Feed Description: " + mFeedDescription);
            mFeedLinkTextView.setText("Feed Link: " + mFeedLink);
            urlLink = mEditText.getText().toString();
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
                Toast.makeText(UserActivity.this,
                        "Rss feed url is valid",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(UserActivity.this,
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

                Log.d("UserActivity", "Parsing name ==> " + name);
                String result = "";
                if (xmlPullParser.next() == XmlPullParser.TEXT) {
                    result = xmlPullParser.getText();
                    xmlPullParser.nextTag();
                }

                if (name.equalsIgnoreCase("title")) {
                    title = result;
                } else if (name.equalsIgnoreCase("link")) {
                    link = result;
                } else if (name.equalsIgnoreCase("description")) {
                    description = result;
                }

                if (title != null && link != null && description != null) {
                    if(isItem) {
                        if(items.size()<maxItems){
                            RssFeedModel item = new RssFeedModel(title, link, description);
                            items.add(item);}
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

