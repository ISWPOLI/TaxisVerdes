package co.com.taxisverdes.taxisverdes.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import co.com.taxisverdes.taxisverdes.R;
import co.com.taxisverdes.taxisverdes.service.location.FetchAddressIntentService;

import static co.com.taxisverdes.taxisverdes.service.location.FetchAddressIntentService.LOCATION_DATA_EXTRA;
import static co.com.taxisverdes.taxisverdes.service.location.FetchAddressIntentService.RECEIVER;
import static co.com.taxisverdes.taxisverdes.service.location.FetchAddressIntentService.RESULT_DATA_KEY;
import static co.com.taxisverdes.taxisverdes.utils.NotificationUtils.showGeneralError;

public class HomeFragment extends Fragment implements OnMapReadyCallback, LocationListener, GoogleMap.OnMapClickListener {

    public static final String ADDRESS_TYPE = "address-type";

    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1;

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationManager mLocationManager;
    private Marker mOriginLocationMarker;
    private Marker mDestinationLocationMarker;
    private boolean mChoosingOrigin;
    private boolean mChoosingDestination;
    private View mInflatedView;
    private TextView mOriginTextView;
    private TextView mDestinationTextView;
    private ImageButton mGoToMyLocationImageButton;
    private ConstraintLayout mRequestTaxiButton;
    private AddressResultReceiver mResultReceiver;
    private ProgressBar mProgressBar;

    public enum AddressSearchType {
        ORIGIN, DESTINATION
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInflatedView = inflater.inflate(R.layout.fragment_home, container, false);
        return mInflatedView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        findViews();
        addListenersToViews();
        configureGeocoderReceiver();

        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void configureGeocoderReceiver() {
        mResultReceiver = new AddressResultReceiver(new Handler());
    }

    private void addListenersToViews() {
        mOriginTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                mChoosingOrigin = hasFocus;
                if (mOriginLocationMarker != null) {
                    animateCameraToLocation(mOriginLocationMarker.getPosition());
                }
            }
        });
        mDestinationTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                mChoosingDestination = hasFocus;
                if (mDestinationLocationMarker != null) {
                    animateCameraToLocation(mDestinationLocationMarker.getPosition());
                }
            }
        });
        mGoToMyLocationImageButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        animateCameraToLocation(location);
                    }
                });
            }
        });
        mRequestTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestTaxi();
            }
        });
    }

    private void requestTaxi() {
        mProgressBar.setVisibility(View.VISIBLE);
        // Start long running operation in a background thread
        new Thread(new Runnable() {
            private int progressStatus = 0;
            private Handler handler = new Handler();

            public void run() {
                while (progressStatus < 100) {
                    progressStatus += 1;
                    handler.post(new Runnable() {
                        public void run() {
                            mProgressBar.setProgress(progressStatus);
                        }
                    });
                    try {
                        // Sleep for 200 milliseconds.
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        }).start();
    }

    private void updateOriginAddress() {
        if (mOriginLocationMarker != null) {
            requestAddress(mOriginLocationMarker.getPosition(), AddressSearchType.ORIGIN);
        }
    }

    private void updateDestinationAddress() {
        if (mDestinationLocationMarker != null) {
            requestAddress(mDestinationLocationMarker.getPosition(), AddressSearchType.DESTINATION);
        }
    }

    private void requestAddress(LatLng latLng, AddressSearchType type) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        requestAddress(location, type);
    }

    private void findViews() {
        mOriginTextView = mInflatedView.findViewById(R.id.homeOrigin);
        mDestinationTextView = mInflatedView.findViewById(R.id.homeDestination);
        mGoToMyLocationImageButton = mInflatedView.findViewById(R.id.homeGoToMyLocationButton);
        mRequestTaxiButton = mInflatedView.findViewById(R.id.homeRequestTaxiButton);
        mProgressBar = mInflatedView.findViewById(R.id.progressBar);
    }

    @Override
    public void onLocationChanged(Location location) {
        animateCameraToLocation(location);
    }

    private void moveCameraToLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        moveCameraToLocation(latLng);
    }

    private void moveCameraToLocation(LatLng latLng) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
        mMap.moveCamera(cameraUpdate);
    }

    private void animateCameraToLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        animateCameraToLocation(latLng);
    }

    private void animateCameraToLocation(LatLng latLng) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            mMap = googleMap;
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style));
            mMap.setOnMapClickListener(this);
            if (ActivityCompat
                    .checkSelfPermission(getContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(getContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                    LatLng cameraPosition = mMap.getCameraPosition().target;
                    if (mChoosingOrigin) {
                        createOrUpdateOriginMarker(cameraPosition);
                    } else if (mChoosingDestination) {
                        createOrUpdateDestinationMarker(cameraPosition);
                    }
                }
            });
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        moveCameraToLocation(location);
                    }
                }
            });
        } catch (Exception e) {
            showGeneralError(e);
        }
    }

    @Override
    public void onMapClick(LatLng selectedPosition) {
        if (mChoosingOrigin) {
            createOrUpdateOriginMarker(selectedPosition);
        } else if (mChoosingDestination) {
            createOrUpdateDestinationMarker(selectedPosition);
        }
    }

    private void createOrUpdateOriginMarker(LatLng selectedPosition) {
        if (mOriginLocationMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(selectedPosition);
            markerOptions.title(getString(R.string.homeOriginMarkerLabel));
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(76));
            mOriginLocationMarker = mMap.addMarker(markerOptions);
        } else {
            mOriginLocationMarker.setPosition(selectedPosition);
        }
        mOriginLocationMarker.showInfoWindow();
        updateOriginAddress();
    }

    private void createOrUpdateDestinationMarker(LatLng selectedPosition) {
        if (mDestinationLocationMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(selectedPosition);
            markerOptions.title(getString(R.string.homeDestinationMarkerLabel));
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(76));
            mDestinationLocationMarker = mMap.addMarker(markerOptions);
        } else {
            mDestinationLocationMarker.setPosition(selectedPosition);
        }
        mDestinationLocationMarker.showInfoWindow();
        updateDestinationAddress();
    }

    private void requestAddress(Location location, AddressSearchType type) {
        Intent intent = new Intent(getActivity(), FetchAddressIntentService.class);

        intent.putExtra(RECEIVER, mResultReceiver);
        intent.putExtra(LOCATION_DATA_EXTRA, location);
        intent.putExtra(ADDRESS_TYPE, type);

        getActivity().startService(intent);
    }

    private class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String addressOutput = resultData.getString(RESULT_DATA_KEY);
            AddressSearchType searchType = AddressSearchType.valueOf(resultData.getString(ADDRESS_TYPE));

            switch (searchType) {
                case ORIGIN:
                    updateOriginAddress(addressOutput);
                    break;
                case DESTINATION:
                    updateDestinationAddress(addressOutput);
                    break;
                default:
                    break;
            }
        }
    }

    private void updateOriginAddress(String addressOutput) {
        mOriginTextView.setText(addressOutput);
    }

    private void updateDestinationAddress(String addressOutput) {
        mDestinationTextView.setText(addressOutput);
    }
}
