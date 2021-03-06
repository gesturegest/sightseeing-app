package team8.codepath.sightseeingapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;

import team8.codepath.sightseeingapp.R;
import team8.codepath.sightseeingapp.activities.TripDetailActivity;
import team8.codepath.sightseeingapp.activities.TripListActivity;
import team8.codepath.sightseeingapp.classes.PhotoTask;
import team8.codepath.sightseeingapp.models.TripModel;

/**
 * Created by floko_000 on 8/18/2016.
 */
public class TripsRecyclerAdapter extends FirebaseRecyclerAdapter<TripModel,
        TripsRecyclerAdapter.ViewHolder> implements GoogleApiClient.OnConnectionFailedListener {

    private Context mContext;
    GoogleApiClient mGoogleApiClient;
    DatabaseReference mDatabaseReferenceFavs;
    ArrayList<String> userFavorites = new ArrayList<>();
    HashMap images = new HashMap();


    public TripsRecyclerAdapter(int modelLayout, DatabaseReference ref,  GoogleApiClient googleApiClient, DatabaseReference favsRef) {
        super(TripModel.class, modelLayout, TripsRecyclerAdapter.ViewHolder.class, ref);
        mGoogleApiClient = googleApiClient;
        mDatabaseReferenceFavs = favsRef;
        setUserFavorites(favsRef);
        loadTripsImages(ref);
    }

    private void loadTripsImages(DatabaseReference ref) {

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                images.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {

                    final TripModel trip = child.getValue(TripModel.class);

                    String PhotoPlaceId = trip.getPlaceId();
                    new PhotoTask(400, 400, mGoogleApiClient) {
                        @Override
                        protected void onPreExecute() {}
                        @Override
                        protected void onPostExecute(AttributedPhoto attributedPhoto) {
                            if (attributedPhoto != null) {
                                images.put(trip.getId(), attributedPhoto.bitmap);
                            }
                        }
                    }.execute(PhotoPlaceId);
                }
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void setUserFavorites(DatabaseReference favsRef) {

        favsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                userFavorites.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    TripModel trip = child.getValue(TripModel.class);
                    userFavorites.add(trip.getId());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return mContext;
    }

    @Override
    public TripsRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        mContext = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.item_trip, parent, false);

        // Return a new holder instance
        return new ViewHolder(contactView);
    }

    @Override
    protected void populateViewHolder(ViewHolder viewHolder, TripModel model, int position) {
    }

    @Override
    public void onBindViewHolder(final TripsRecyclerAdapter.ViewHolder viewHolder, final int position) {
        final TripModel trip = getItem(position);

        final boolean[] isFavorite = {(userFavorites.contains(trip.getId()))};

        final ImageView bannerView = viewHolder.banner;

        // Populate subviews
        bannerView.setImageResource(android.R.color.transparent); // clear out old image for recycled view

        if(images.isEmpty()){

            String PhotoPlaceId = trip.getPlaceId();
            new PhotoTask(400, 400, mGoogleApiClient) {
                @Override
                protected void onPreExecute() {
                    // Display a temporary image to show while bitmap is loading.
                    bannerView.setImageResource(R.drawable.places_back);
                }
                @Override
                protected void onPostExecute(AttributedPhoto attributedPhoto) {
                    if (attributedPhoto != null) {
                        // Photo has been loaded, display it.
                        bannerView.setImageBitmap(attributedPhoto.bitmap);

                    }
                }
            }.execute(PhotoPlaceId);
            Log.i("LOADING IMAGE...", "api");

        }
        else{
            Bitmap imageBitMap = (Bitmap) images.get(trip.getId());
            bannerView.setImageBitmap(imageBitMap);
            Log.i("LOADING IMAGE...", "bitmap hashmap");

        }

        viewHolder.name.setText(trip.getName());
        viewHolder.length.setText("Length: " + trip.getHumanReadableTotalLength());

        viewHolder.ivFavorite.setImageBitmap(null);
        if(isFavorite[0])
            favoriteTrip(viewHolder.ivFavorite);
        else
            unfavoriteTrip(viewHolder.ivFavorite);

        viewHolder.ivFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isFavorite[0]){
                    mDatabaseReferenceFavs.child(trip.getId()).removeValue();
                    unfavoriteTrip(viewHolder.ivFavorite);
                    isFavorite[0] = false;
                }
                else{
                    mDatabaseReferenceFavs.child(trip.getId()).setValue(trip);
                    favoriteTrip(viewHolder.ivFavorite);
                    isFavorite[0] = true;
                }
            }
        });

        viewHolder.cvTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Activity activity = (Activity) getContext();

                Intent intent = new Intent(activity, TripDetailActivity.class);
                intent.putExtra("trip", Parcels.wrap(trip));

                Pair<View, String> p1 = Pair.create((View)viewHolder.name, "transitionTripName");
                Pair<View, String> p2 = Pair.create((View)viewHolder.banner, "transitionTripMap");
                Pair<View, String> p3 = Pair.create((View)viewHolder.length, "transitionTripLength");

                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(activity, p1, p2, p3);

                activity.startActivity(intent, options.toBundle());

            }
        });
    }

    private void favoriteTrip(ImageButton ivFavorite) {
        ivFavorite.setImageResource(R.drawable.ic_heart_white_34);
    }

    private void unfavoriteTrip(ImageButton ivFavorite) {
        ivFavorite.setImageResource(R.drawable.ic_heart_outline_34);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView banner;
        public TextView name;
        public TextView distance;
        public TextView length;
        public CardView cvTrip;
        public ImageButton ivFavorite;

        public ViewHolder(View itemView) {
            super(itemView);
            banner = (ImageView) itemView.findViewById(R.id.ivTripBanner);
            name = (TextView) itemView.findViewById(R.id.tvTripName);
            distance = (TextView) itemView.findViewById(R.id.tvTripDistance);
            length = (TextView) itemView.findViewById(R.id.tvTripLength);
            cvTrip = (CardView) itemView.findViewById(R.id.cvTrip);
            ivFavorite = (ImageButton) itemView.findViewById(R.id.ivFavorite);

        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

}
