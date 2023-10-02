package com.galeos.smartalert;
import static java.lang.String.format;

import android.Manifest;
import android.annotation.SuppressLint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class EmergenciesActivity extends AppCompatActivity implements LocationListener {
    ListView incidents_listview;
    Button logoutBtn, messageBtn, declineBtn;
    ArrayList<String> arrayList;
    ArrayAdapter<String> adapter;
    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    private static final double r = 6372.8; // In kilometers
    String curEmergency, curTimestamp, curLocation;
    TextView emergency_info_text_view, location_info_text_view;
    private String url = "https://fcm.googleapis.com/fcm/send";
    //Geo
    private static final String TAG = "EmergenciesActivity";
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private float GEOFENCE_RADIUS = 2000;
    private String GEOFENCE_ID = ""+new Random().nextInt()+"";
    LocationManager locationManager;
    private int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;
    private String token = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergencies);

        logoutBtn = findViewById(R.id.logoutBtn);
        messageBtn = findViewById(R.id.messageBtn);
        declineBtn = findViewById(R.id.declineBtn);
        emergency_info_text_view = findViewById(R.id.emergency_info_text_view);
        location_info_text_view = findViewById(R.id.location_info_text_view);

        arrayList = new ArrayList<>();
        //location instantiate
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //Geo
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);

        Intent intent = getIntent();
        curEmergency = intent.getStringExtra("Emergency");

        String[] splitCurEmergency = curEmergency.split("\\,", 0);
        curEmergency = splitCurEmergency[0];
        double lat = Double.parseDouble(splitCurEmergency[1]);
        double lon = Double.parseDouble(splitCurEmergency[2]);
        curLocation = lat +"," + lon;
        emergency_info_text_view.setText(curEmergency);
        location_info_text_view.setText(curLocation);

        //Get current Location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 123);
            finish();
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            finish();
            Toast.makeText(EmergenciesActivity.this, getString(R.string.Turnon_location_message), Toast.LENGTH_SHORT).show();
        }

        messageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addGeofence(lat, lon, GEOFENCE_RADIUS);
            }
        });
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EmergenciesActivity.this, ChooseRoleActivity.class));
                finish();
            }
        });

        declineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                declineIncident();
                startActivity(new Intent(EmergenciesActivity.this, EmployeeMainActivity.class));
                finish();

            }
        });
    }

    private void addGeofence(double lat, double lon, float radius) {
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, lat, lon, radius, Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_ENTER);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Geofence Added...");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure " + errorMessage);
                    }
                });
    }

    private void declineIncident(){
        firestore = FirebaseFirestore.getInstance();
        // Collection Reference to all incidents
        CollectionReference incidentsRef = firestore.collection("incidents");

        // Query to get the incidents in order
        Query query = incidentsRef
                .orderBy("Timestamp", Query.Direction.DESCENDING)
                .orderBy("Emergency", Query.Direction.ASCENDING);

        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (DocumentSnapshot document : queryDocumentSnapshots){
                    if(document.getString("Emergency").equals(curEmergency))
                        if(document.getString("Location") == curLocation)
                            System.out.println(document.getString("Comments"));
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission
                Toast.makeText(this, "You can add geofences...", Toast.LENGTH_SHORT).show();
            } else {
                //We do not have the permission..
                Toast.makeText(this, "Background location access is neccessary for geofences to trigger...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    @Override
    public void onLocationChanged(@NonNull List<Location> locations) {
        LocationListener.super.onLocationChanged(locations);
    }





}