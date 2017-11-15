package co.com.taxisverdes.taxisverdes.home;

import android.Manifest;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private AddressResultReceiver mResultReceiver;

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
            }
        });
        mDestinationTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                mChoosingDestination = hasFocus;
            }
        });
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
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
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
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
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
