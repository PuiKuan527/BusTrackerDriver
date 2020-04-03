package com.test.bustrackerdriver;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.test.bustrackerdriver.Interface.IOnLoadLocationListener;
import com.test.bustrackerdriver.POJO.Example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GeoQueryEventListener,
        IOnLoadLocationListener{

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentuser;
    private DatabaseReference myLocationRef;
    private GeoFire geoFire;
    private List<LatLng> busStop;
    private List<LatLng> driver;
    private IOnLoadLocationListener listener;
    LatLng origin;
    LatLng dest;
    ArrayList<LatLng> MarkerPoints;
    TextView ShowDistanceDuration;
    Polyline line;
    private Button logout,mylocation;
    final FirebaseUser user1 = FirebaseAuth.getInstance().getCurrentUser();

    private DatabaseReference myBusStop,mydriver;
    private Location lastLocation;
    private GeoQuery geoquery;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        databaseReference = FirebaseDatabase.getInstance().getReference("BusStopArea").child("BusStop");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Student student = dataSnapshot.child("Library").getValue(Student.class);
                int num=student.getStudentNo()+1;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mylocation=findViewById(R.id.mylocation);

        mylocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addUserMarker();
            }
        });

        ShowDistanceDuration = (TextView) findViewById(R.id.show_distance_time);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        // Initializing
        MarkerPoints = new ArrayList<>();

        //show error dialog if Google Play Services not available
        if (!isGooglePlayServicesAvailable()) {
            Log.d("onCreate", "Google Play Services not available. Ending Test case.");
            finish();
        }
        else {
            Log.d("onCreate", "Google Play Services available. Continuing.");
        }

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

                        buildLocationRequest();
                        buildLocationCallback();
                        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(MapsActivity.this);

                        initArea();
                        //initArea1();
                        settingGeoFire();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this,"You must enable permission",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

    }

    private void initArea() {
        myBusStop=FirebaseDatabase.getInstance()
                .getReference("BusStopArea")
                .child("BusStop");
        listener=this;

        myBusStop.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<MyLatLng> latLngList = new ArrayList<>();
                for (DataSnapshot locationSnapShot: dataSnapshot.getChildren()){
                    MyLatLng latLng = locationSnapShot.getValue(MyLatLng.class);
                    latLngList.add(latLng);
                }
                listener.OnLoadLocationSuccess(latLngList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void initArea1() {
        mydriver=FirebaseDatabase.getInstance()
                .getReference("driversAvailable")
                .child(user1.getUid());
        listener=this;

        mydriver.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<MyLatLng1> latLngList1 = new ArrayList<>();
                for (DataSnapshot locationSnapShot: dataSnapshot.getChildren()){
                    MyLatLng1 latLng1 = locationSnapShot.getValue(MyLatLng1.class);
                    latLngList1.add(latLng1);
                }
                listener.OnLoadLocationSuccess1(latLngList1);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void settingGeoFire() {
        myLocationRef= FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire=new GeoFire(myLocationRef);
    }

    private void buildLocationCallback() {
        locationCallback=new LocationCallback(){
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                if (mMap != null)
                {
                    lastLocation=locationResult.getLastLocation();
                    addUserMarker();

                }
            }
        };

    }

    private void addUserMarker() {
        geoFire.setLocation("You", new GeoLocation(lastLocation.getLatitude(),
                lastLocation.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if(currentuser != null) currentuser.remove();
                currentuser = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(lastLocation.getLatitude(),
                                lastLocation.getLongitude()))
                        .title("You"));

                mMap.animateCamera(CameraUpdateFactory
                        .newLatLngZoom(currentuser.getPosition(),17.5f));

            }
        });
    }

    private void buildLocationRequest() {
        locationRequest=new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (fusedLocationProviderClient != null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_DENIED){
                    return;
                }
            }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,Looper.myLooper());

        addCirleArea();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                // clearing map and generating new marker points if user clicks on map more than two times
                if (MarkerPoints.size() > 1) {
                    mMap.clear();
                    MarkerPoints.clear();
                    MarkerPoints = new ArrayList<>();
                    ShowDistanceDuration.setText("");
                }

                // Adding new item to the ArrayList
                MarkerPoints.add(point);

                // Creating MarkerOptions
                MarkerOptions options = new MarkerOptions();

                // Setting the position of the marker
                options.position(point);

                /**
                 * For the start location, the color of marker is GREEN and
                 * for the end location, the color of marker is RED.
                 */
                if (MarkerPoints.size() == 1) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).title("Start");
                } else if (MarkerPoints.size() == 2) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).title("End");
                }


                // Add new marker to the Google Map Android API V2
                mMap.addMarker(options);

                // Checks, whether start and end locations are captured
                if (MarkerPoints.size() >= 2) {
                    origin = MarkerPoints.get(0);
                    dest = MarkerPoints.get(1);
                }

            }
        });

        Button btnDriving = (Button) findViewById(R.id.btnDriving);
        btnDriving.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                build_retrofit_and_get_response("driving");
            }
        });

        Button btnWalk = (Button) findViewById(R.id.btnWalk);
        btnWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                build_retrofit_and_get_response("walking");
            }
        });
    }


    private void build_retrofit_and_get_response(String type) {

        String url = "https://maps.googleapis.com/maps/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitMaps service = retrofit.create(RetrofitMaps.class);

        Call<Example> call = service.getDistanceDuration("metric", origin.latitude + "," + origin.longitude,dest.latitude + "," + dest.longitude, type);

        call.enqueue(new Callback<Example>() {
            @Override
            public void onResponse(Response<Example> response, Retrofit retrofit) {

                try {
                    //Remove previous line from map
                    if (line != null) {
                        line.remove();
                    }
                    // This loop will go through all the results and add marker on each location.
                    for (int i = 0; i < response.body().getRoutes().size(); i++) {
                        String distance = response.body().getRoutes().get(i).getLegs().get(i).getDistance().getText();
                        String time = response.body().getRoutes().get(i).getLegs().get(i).getDuration().getText();
                        ShowDistanceDuration.setText("Distance:" + distance + ", Duration:" + time);
                        String encodedString = response.body().getRoutes().get(0).getOverviewPolyline().getPoints();
                        List<LatLng> list = decodePoly(encodedString);
                        line = mMap.addPolyline(new PolylineOptions()
                                .addAll(list)
                                .width(20)
                                .color(Color.RED)
                                .geodesic(true)
                        );
                    }
                } catch (Exception e) {
                    Log.d("onResponse", "There is an error");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d("onFailure", t.toString());
            }
        });

    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }

    // Checking if Google Play Services Available or not
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        0).show();
            }
            return false;
        }
        return true;
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    private void addCirleArea() {
        if(geoquery!=null) {
            geoquery.removeGeoQueryEventListener(this);
            geoquery.removeAllListeners();
        }
        for (LatLng latLng:busStop){
            mMap.addCircle(new CircleOptions().center(latLng)
                    .radius(15.0f)
                    .strokeColor(Color.BLUE)
                    .fillColor(0x220000FF)
                    .strokeWidth(5.0f)
            );

            geoquery = geoFire.queryAtLocation(new GeoLocation((latLng.latitude),latLng.longitude),0.01f);
            geoquery.addGeoQueryEventListener(MapsActivity.this);

        }
    }

    @Override
    protected void onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }


    @Override
    public void onKeyEntered(String key, GeoLocation location) {

        FirebaseDatabase.getInstance()
                .getReference("BusStopArea")
                .child("BusStop")
                .child("Library")
                .child("StudentNo")
                .setValue(1)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MapsActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
        sendNotification("Jane",String.format("%s entered the bus stop area",key));
    }

    @Override
    public void onKeyExited(String key) {

        FirebaseDatabase.getInstance()
                .getReference("BusStopArea")
                .child("BusStop")
                .child("Library")
                .child("StudentNo")
                .setValue(-1)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MapsActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });

        sendNotification("Wilson",String.format("%s exited the bus stop area",key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("Jane",String.format("%s move within the bus stop area",key));
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, ""+error.getMessage(),Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String title, String content) {

        Toast.makeText(this, ""+content, Toast.LENGTH_SHORT).show();
        String NOTIFICATION_CHANNEL_ID="jane_multiple_location";
        NotificationManager notificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notification",
                    NotificationManager.IMPORTANCE_DEFAULT);

            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long [] {0,1000,500,1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder= new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification=builder.build();
        notificationManager.notify(new Random().nextInt(),notification);
    }

    @Override
    public void OnLoadLocationSuccess(List<MyLatLng> latLngs) {
        busStop=new ArrayList<>();

        for(MyLatLng myLatLng:latLngs)
        {
            LatLng convert=new LatLng(myLatLng.getLatitude(),myLatLng.getLongitude());
            busStop.add(convert);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);

        if (mMap!=null){
            mMap.clear();

            addUserMarker();

            addCirleArea();
        }
    }

    @Override
    public void OnLoadLocationSuccess1(List<MyLatLng1> latLngs) {
        driver=new ArrayList<>();

        for(MyLatLng1 myLatLng1:latLngs)
        {
            LatLng convert=new LatLng(myLatLng1.getLatitude(),myLatLng1.getLongitude());
            driver.add(convert);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);

        if (mMap!=null){
            mMap.clear();

            geoFire.setLocation("Driver", new GeoLocation(lastLocation.getLatitude(),
                    lastLocation.getLongitude()), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    if(currentuser != null) currentuser.remove();
                    currentuser = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(lastLocation.getLatitude(),
                                    lastLocation.getLongitude()))
                            .title("Driver"));

                    mMap.animateCamera(CameraUpdateFactory
                            .newLatLngZoom(currentuser.getPosition(),17.5f));

                }
            });
        }
    }


    @Override
    public void OnLoadLocationFailed(String Message) {
        Toast.makeText(this,""+Message,Toast.LENGTH_SHORT).show();
    }
}
