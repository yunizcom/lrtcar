package com.mooStan.liveroadtrafficapp;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mooStan.liveroadtrafficapp.R;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.Activity;

public class LiveRoadTrafficApp extends Activity {

	static final LatLng HAMBURG = new LatLng(53.558, 9.927);
	static final LatLng KIEL = new LatLng(53.551, 9.993);
	private GoogleMap googleMap;
	private Location glolocation;
	private boolean gpsMode = false;
	
	private ImageView fbShare, normalMap, sateliteMap, lagendaImg, sateliteSwitch;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_live_road_traffic_app);

		/*setup system control*/
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    StrictMode.setThreadPolicy(policy);
	    /*setup system control*/
		
	    fbShare = (ImageView) findViewById(R.id.fbShare);
	    normalMap = (ImageView) findViewById(R.id.normalMap);
	    sateliteMap = (ImageView) findViewById(R.id.sateliteMap);
	    lagendaImg = (ImageView) findViewById(R.id.lagendaImg);
	    sateliteSwitch = (ImageView) findViewById(R.id.sateliteSwitch);
	    
	    enableButtons();
	    
		int checkGooglePlayServices = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	    if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
	    	GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices, this, 1122).show();
	    }else{
	    	try {
	            // Loading map
	            initilizeMap();
	 
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	/**
     * function to load map. If map is not created it will create it for you
     * */
    private void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();
 
            // check if map is created successfully or not
            if (googleMap != null) {
            	
            	//googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                googleMap.setMyLocationEnabled(true);
                googleMap.setTrafficEnabled(true);

				//centerMapOnMyLocation();
                
                //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HAMBURG, 14));

                //Marker ciu = googleMap.addMarker(new MarkerOptions().position(HAMBURG).title("You at here!"));
            	
            }else{
            	Log.v("debug","map failed");
            }
        }
    }
    
    private void centerMapOnMyLocation() {

    	LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
    	Criteria criteria = new Criteria();
    	String provider = service.getBestProvider(criteria, false);
    	Location location = service.getLastKnownLocation(provider);
    	LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());

    	         
    	Toast.makeText(getApplicationContext(), userLocation.toString() , Toast.LENGTH_LONG).show();


        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
        
        //googleMap.addMarker(new MarkerOptions().position(userLocation).title("You at here!"));
        
        googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {

            @Override
            public void onMyLocationChange(Location arg0) {
            	googleMap.clear();
            	//googleMap.addMarker(new MarkerOptions().position(new LatLng(arg0.getLatitude(), arg0.getLongitude())).title("You at here!"));
            	if(gpsMode == true){
	            	LatLng userLocation = new LatLng(arg0.getLatitude(),arg0.getLongitude());
	            	googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,googleMap.getCameraPosition().zoom));
            	}
            }
        });

        
    }
    
    private void enableButtons(){
    	fbShare.setOnTouchListener(new View.OnTouchListener() {
	        @Override
	        public boolean onTouch(View arg0, MotionEvent arg1) {
	            switch (arg1.getAction()) {
		            case MotionEvent.ACTION_DOWN: {
		            	fbShare.setAlpha(180);

		                break;
		            }
		            case MotionEvent.ACTION_UP:{
		            	fbShare.setAlpha(255);

		            	
		                break;
		            }
	            }
	            return true;
	        }
	    });
    	
    	normalMap.setOnTouchListener(new View.OnTouchListener() {
	        @Override
	        public boolean onTouch(View arg0, MotionEvent arg1) {
	            switch (arg1.getAction()) {
		            case MotionEvent.ACTION_DOWN: {
		            	normalMap.setAlpha(180);

		                break;
		            }
		            case MotionEvent.ACTION_UP:{
		            	normalMap.setAlpha(255);

		            	googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		            	
		                break;
		            }
	            }
	            return true;
	        }
	    });
    	
    	sateliteMap.setOnTouchListener(new View.OnTouchListener() {
	        @Override
	        public boolean onTouch(View arg0, MotionEvent arg1) {
	            switch (arg1.getAction()) {
		            case MotionEvent.ACTION_DOWN: {
		            	sateliteMap.setAlpha(180);

		                break;
		            }
		            case MotionEvent.ACTION_UP:{
		            	sateliteMap.setAlpha(255);

		            	googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		            	
		                break;
		            }
	            }
	            return true;
	        }
	    });
    	
    	sateliteSwitch.setOnTouchListener(new View.OnTouchListener() {
	        @Override
	        public boolean onTouch(View arg0, MotionEvent arg1) {
	            switch (arg1.getAction()) {
		            case MotionEvent.ACTION_DOWN: {
		            	sateliteSwitch.setAlpha(180);

		                break;
		            }
		            case MotionEvent.ACTION_UP:{
		            	sateliteSwitch.setAlpha(255);

		            	if(gpsMode == true){
		            		gpsMode = false;
		            	}else{
		            		gpsMode = true;
		            	}
		            	
		                break;
		            }
	            }
	            return true;
	        }
	    });
    	
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
    }

}
