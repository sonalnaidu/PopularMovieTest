package com.example.android.popularmoviestest;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageAdapter movieAdapter;
    private String[] thumbIds = new String[20];
    private String [][] movieDetails = new String[6][20];

    protected boolean isOnline() {

        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnectedOrConnecting());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isOnline())
        {
            Log.i("ONLINE", "Fetching Posters");
            setContentView(R.layout.activity_main);
            GridView gridView = (GridView) findViewById(R.id.gridview);
            movieAdapter = new ImageAdapter(this, thumbIds);
            gridView.setAdapter(movieAdapter);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String movieId = thumbIds[position];
                    Log.i("Movie Name", movieDetails[1][position]);
                    Intent intent = new Intent(getApplicationContext(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, movieId);
                    intent.putExtra("title", movieDetails[1][position]);
                    intent.putExtra("overview", movieDetails[2][position]);
                    intent.putExtra("release_date", movieDetails[3][position]);
                    intent.putExtra("vote_average", movieDetails[4][position]);
                    intent.putExtra("movie_id", movieDetails[5][position]);
                    startActivity(intent);
                }
            });
        }
        else{
            Log.v("OFFLINE", "Need to display something here");
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextChange(String newText){
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query){
                final SharedPreferences favPrefs = getApplication().getSharedPreferences("searchHistory", Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = favPrefs.edit();
                edit.putBoolean(query, Boolean.TRUE).apply();
                Log.i("Query", query);
                updateMovies(query);
                return true;
            }
        });

        return true;
    }

    public void updateMovies(String fetch_type){
        if (isOnline()) {
            FetchMovieTask movieTask = new FetchMovieTask();
            // top_rated? or popular switch here
            movieTask.execute(fetch_type);
        }
        else{
            Toast.makeText(getApplicationContext(), "No Internet = No Movies.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void  onStart(){
        super.onStart();
        updateMovies("history");
    }

    public class ImageAdapter extends BaseAdapter{
        private Context mContext;
        public int getCount() {
            return mThumbIds.length;
        }
        public Object getItem(int position) {
            return mThumbIds[position];
        }
        public long getItemId(int position) {
            return 0;
        }
        private String[] mThumbIds;

        public ImageAdapter(Context c, String[] mThumbIds) {
            mContext = c;
            this.mThumbIds = mThumbIds;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null){
                imageView = new ImageView(mContext);
            }
            else{
                imageView = (ImageView) convertView;
            }
            Picasso.with(getBaseContext())
                    .load(this.mThumbIds[position])
                    .into(imageView);
            return imageView;
        }

    }

    public class FetchMovieTask extends AsyncTask<String, Void, String[][]> {

        private final String LOG_TAG = FetchMovieTask.class.getSimpleName();

        private String getResponseString(String searchString)
        {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String responseString = null;
            try
            {
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("https")
                        .authority("www.omdbapi.com")
                        .appendQueryParameter("t", searchString)
                        .appendQueryParameter("y",null)
                        .appendQueryParameter("plot","short")
                        .appendQueryParameter("r","json");

                String searchQuery = builder.build().toString();
                Log.v(LOG_TAG, "BUILT URI = " + searchQuery);

                URL url = new URL(searchQuery);
                // Create the request and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                Log.v(LOG_TAG, "Called url");

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                responseString = buffer.toString();
                Log.v(LOG_TAG, "RESPONSE JSON STRING = " + responseString);
                return responseString;

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error fetching ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
        }

        private String[][] setFromHistory(String requestType)
                throws JSONException
        {
            final SharedPreferences favPrefs = getApplicationContext().getSharedPreferences("searchHistory", Context.MODE_PRIVATE);
            Map<String,?> keys = favPrefs.getAll();
            int favourites_length = keys.size();
            String [] favouriteIds = new String[favourites_length];
            String[][] posterUrls = new String[6][favourites_length];

            int i = 0;
            for(Map.Entry<String,?> entry : keys.entrySet()){
                Log.d("map values",entry.getKey() + ": " +
                        entry.getValue().toString());
                favouriteIds[i++] = entry.getKey();
            }

            for(i=0 ; i<favourites_length; i++) {
                JSONObject movieDetail = new JSONObject(this.getResponseString(favouriteIds[i]));
                posterUrls[0][i] = movieDetail.getString("Poster");
                posterUrls[1][i] = movieDetail.getString("Title");
                posterUrls[2][i] = movieDetail.getString("Plot");
                posterUrls[3][i] = movieDetail.getString("Released");
                posterUrls[4][i] = movieDetail.getString("imdbRating");
                posterUrls[5][i] = movieDetail.getString("imdbID");
            }
            return posterUrls;
        }

        private String[][] getPostersUrlsFromJson(String responseJsonStr)
            throws JSONException
        {
            JSONObject movieDetail = new JSONObject(responseJsonStr);
            String[][] posterUrls = new String[6][20];
            for (int i=0; i < 1; i++)
            {
                posterUrls[0][i] = movieDetail.getString("Poster");
                posterUrls[1][i] = movieDetail.getString("Title");
                posterUrls[2][i] = movieDetail.getString("Plot");
                posterUrls[3][i] = movieDetail.getString("Released");
                posterUrls[4][i] = movieDetail.getString("imdbRating");
                posterUrls[5][i] = movieDetail.getString("imdbID");
            }
            return posterUrls;
        }

        @Override
        protected String[][] doInBackground(String... params) {
            try {
                Log.v("PARAMS ", params[0]);
                if (params[0].equals("history"))
                    return setFromHistory(params[0]);
                else
                    return getPostersUrlsFromJson(this.getResponseString(params[0]));
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[][] result) {
            if (result!=null){
                int i=0;
                for(int j=result[1].length; i<j; i++){
                    thumbIds[i] = result[0][i];
                    movieDetails[0][i] = result[0][i];
                    movieDetails[1][i] = result[1][i];
                    movieDetails[2][i] = result[2][i];
                    movieDetails[3][i] = result[3][i];
                    movieDetails[4][i] = result[4][i];
                    movieDetails[5][i] = result[5][i];
                }
                for(i=result[1].length; i<20; i++){
                    thumbIds[i] = null;
                    movieDetails[0][i] = null;
                    movieDetails[1][i] = null;
                    movieDetails[2][i] = null;
                    movieDetails[3][i] = null;
                    movieDetails[4][i] = null;
                    movieDetails[5][i] = null;
                }
                movieAdapter.notifyDataSetChanged();
                Log.v(LOG_TAG, "Updated the movie Adapter: "+thumbIds[0]);
                Log.v(LOG_TAG, "Updated the movie details: "+movieDetails[1][0]);
            }
            else{
                Log.v(LOG_TAG,"Unable to update adapter as result was null");
            }
        }

    }

}
