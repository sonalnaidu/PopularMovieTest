package com.example.android.popularmoviestest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Intent intent = getActivity().getIntent();
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        final SharedPreferences favPrefs = this.getActivity().getSharedPreferences("favourites", Context.MODE_PRIVATE);
        final String movie_id = intent.getStringExtra("movie_id");
        final Boolean markedAsFav = favPrefs.getBoolean(movie_id, false);
        Log.v("marked as fav", markedAsFav.toString());

        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
            String imagePath = intent.getStringExtra(Intent.EXTRA_TEXT);
            ImageView imageView = ((ImageView) rootView.findViewById(R.id.imageView));
            Picasso.with(getContext())
                    .load(imagePath)
                    .into(imageView);
            String title = intent.getStringExtra("title");
            String overview= intent.getStringExtra("overview");
            String release_date = intent.getStringExtra("release_date");
            String vote_average = intent.getStringExtra("vote_average");
            String rating;
            try {
                rating = vote_average.substring(0, 3);
            } catch (Exception e){
                rating = vote_average;
            }
            String release_year = release_date.substring(release_date.length() - 4);
            title = title+" ("+release_year+")";
            TextView movie_title_tv = ((TextView) rootView.findViewById(R.id.movie_title));
            movie_title_tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            movie_title_tv.setText(title);
            ((TextView) rootView.findViewById(R.id.overview)).setText(overview);
            ((TextView) rootView.findViewById(R.id.rating)).setText(rating);
        }
        return rootView;
    }
}
