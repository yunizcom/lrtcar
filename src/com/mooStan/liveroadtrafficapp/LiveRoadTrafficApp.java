package com.mooStan.liveroadtrafficapp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.opengles.GL10;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.mooStan.liveroadtrafficapp.R;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class LiveRoadTrafficApp extends Activity {

	static final LatLng HAMBURG = new LatLng(53.558, 9.927);
	static final LatLng KIEL = new LatLng(53.551, 9.993);
	private GoogleMap googleMap;
	private Location glolocation;
	private boolean gpsMode = false, fbCaptionFilled = false;
	private String weatherString = "", fbCaption = "";
	
	private ImageView fbShare, normalMap, sateliteMap, lagendaImg, sateliteSwitch, popboxOK_btn, ic_map_label, ic_close;
	private RelativeLayout popbox, mainTop;
	private LinearLayout popboxCenter;
	private TextView popboxMSG, weatherOverlay, weatherOverlayShadow;
	private EditText fbmsg;
	
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
	private boolean pendingPublishReauthorization = false;
	
	public Bitmap fbImg;
	public String fbMsg;
	
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
	    ic_map_label = (ImageView) findViewById(R.id.ic_map_label);
	    ic_close = (ImageView) findViewById(R.id.ic_close);
	    
	    popboxOK_btn = (ImageView) findViewById(R.id.popboxOK_btn);
	    
	    popbox = (RelativeLayout) findViewById(R.id.popbox);
	    mainTop = (RelativeLayout) findViewById(R.id.mainTop);
	    popboxCenter = (LinearLayout) findViewById(R.id.popboxCenter);
	    
	    //setStageBackground_relative(popbox,"backgrounds/opacity_50_bg.png");
		setStageBackground_linear(popboxCenter,"backgrounds/popUp_bg.png");
	    
	    popboxMSG = (TextView) findViewById(R.id.popboxMSG);
	    fbmsg = (EditText) findViewById(R.id.fbmsg);
	    
	    weatherOverlay = (TextView) findViewById(R.id.weatherOverlay);
	    weatherOverlayShadow = (TextView) findViewById(R.id.weatherOverlayShadow);
	    
	    Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/GROBOLD.ttf");
	    popboxMSG.setTypeface(tf);
	    popboxMSG.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
	    popboxMSG.setTextColor(Color.parseColor("#333333"));
	    
	    tf = Typeface.createFromAsset(getAssets(), "fonts/homizio_nova.ttf");

	    weatherOverlayShadow.setTypeface(tf);
	    weatherOverlayShadow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 44);
	    weatherOverlayShadow.setTextColor(Color.parseColor("#333333"));
	    
	    weatherOverlay.setTypeface(tf);
	    weatherOverlay.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 44);
	    weatherOverlay.setTextColor(Color.parseColor("#ffffff"));
	    
	    enableButtons();
	    
	    LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
	    boolean enabledGPS = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
        //boolean enabledWiFi = service.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	    
	    if(!isNetworkAvailable()){
	    	popBox(1,"You need a smooth internet connection to load the map and live traffic feeds.");
		}else{
			if(enabledGPS == false){
				popBox(1,"Please turn on your phone GPS for better location determination.");
			}
		}

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
	    
	    /*
	    PackageInfo info;
		try {
		    info = getPackageManager().getPackageInfo("com.mooStan.liveroadtrafficapp", PackageManager.GET_SIGNATURES);
		    for (Signature signature : info.signatures) {
		        MessageDigest md;
		        md = MessageDigest.getInstance("SHA");
		        md.update(signature.toByteArray());
		        String something = new String(Base64.encode(md.digest(), 0));
		        //String something = new String(Base64.encodeBytes(md.digest()));
		        Log.e("hash key", something);
		    }
		} catch (NameNotFoundException e1) {
		    Log.e("name not found", e1.toString());
		} catch (NoSuchAlgorithmException e) {
		    Log.e("no such an algorithm", e.toString());
		} catch (Exception e) {
		    Log.e("exception", e.toString());
		}
		*/
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
               
				centerMapOnMyLocation();
                /*getWeatherData(5.398448,100.294014);
                
                Date d1 =new Date();
            	SimpleDateFormat date = new SimpleDateFormat("EEEE,\nMMM dd yyyy\nhh:mm aa");
            	
            	popBox(3,weatherString + date.format(d1));*/

                //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HAMBURG, 14));

                //Marker ciu = googleMap.addMarker(new MarkerOptions().position(HAMBURG).title("You at here!"));
            	
            }else{
            	//Log.v("debug","map failed");
            }
        }
    }
    
    private void centerMapOnMyLocation() {

    	LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
    	Criteria criteria = new Criteria();
    	String provider = service.getBestProvider(criteria, false);
    	Location location = service.getLastKnownLocation(provider);
    	LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());

    	         
    	//Toast.makeText(getApplicationContext(), userLocation.toString() , Toast.LENGTH_LONG).show();
    	getWeatherData(location.getLatitude(), location.getLongitude());

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

		            	//publishPhoto(loadBitmapFromView_BITMAP(mainTop),"Look on the traffic!");
		            	//captureMapScreen();
		            	popBox(4,"Insert your snapshot caption at below :");

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
		            		popBox(1,"GPS mode is disabled, map will steel on current location.");
		            	}else{
		            		gpsMode = true;
		            		popBox(1,"GPS mode is enabled, map will update your location automatically.");
		            	}

		                break;
		            }
	            }
	            return true;
	        }
	    });
    	
    	popboxOK_btn.setOnTouchListener(new View.OnTouchListener() {
	        @Override
	        public boolean onTouch(View arg0, MotionEvent arg1) {
	            switch (arg1.getAction()) {
		            case MotionEvent.ACTION_DOWN: {
		            	popboxOK_btn.setAlpha(180);

		                break;
		            }
		            case MotionEvent.ACTION_UP:{
		            	popboxOK_btn.setAlpha(255);

		            	if(fbCaptionFilled == false){
		            		popBox(0,"");
		            	}else{
		            		popBox(0,"");
		            		fbCaptionFilled = false;
		            		fbCaption = fbmsg.getText().toString();
		            		captureMapScreen();
		            	}
		            	
		                break;
		            }
	            }
	            return true;
	        }
	    });
    	
    	ic_close.setOnTouchListener(new View.OnTouchListener() {
	        @Override
	        public boolean onTouch(View arg0, MotionEvent arg1) {
	            switch (arg1.getAction()) {
		            case MotionEvent.ACTION_DOWN: {
		            	ic_close.setAlpha(180);

		                break;
		            }
		            case MotionEvent.ACTION_UP:{
		            	ic_close.setAlpha(255);

	            		fbCaptionFilled = false;
	            		popBox(0,"");
		            	
		                break;
		            }
	            }
	            return true;
	        }
	    });
    	
    }
    
    @SuppressLint("NewApi")
	private void setStageBackground_linear(LinearLayout thisStage, String fileName){

		try 
		{
			InputStream ims = getAssets().open(fileName);
		    Drawable d = Drawable.createFromStream(ims, null);
		    
		    if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
		    	thisStage.setBackgroundDrawable(d);
		    } else {
		    	thisStage.setBackground(d);
		    }
		    
		    ims = null;
		    d = null;

		}
		catch(IOException ex) 
		{
		    return;
		}
		
	}
    
    @SuppressLint("NewApi")
	private void setStageBackground_relative(RelativeLayout thisStage, String fileName){

		try 
		{
			InputStream ims = getAssets().open(fileName);
		    Drawable d = Drawable.createFromStream(ims, null);
		    
		    if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
		    	thisStage.setBackgroundDrawable(d);
		    } else {
		    	thisStage.setBackground(d);
		    }
		    
		    ims = null;
		    d = null;

		}
		catch(IOException ex) 
		{
		    return;
		}
		
	}
    
    private void buttonClickable(boolean allows){
    	if(allows == true){

    		fbShare.setVisibility(View.VISIBLE);
		    normalMap.setVisibility(View.VISIBLE);
		    sateliteMap.setVisibility(View.VISIBLE);
		    sateliteSwitch.setVisibility(View.VISIBLE);
		    
		    fbShare.setEnabled(true);
	    	normalMap.setEnabled(true);
	    	sateliteMap.setEnabled(true);
	    	sateliteSwitch.setEnabled(true);
	    	
    	}else{

		    fbShare.setVisibility(View.INVISIBLE);
		    normalMap.setVisibility(View.INVISIBLE);
		    sateliteMap.setVisibility(View.INVISIBLE);
		    sateliteSwitch.setVisibility(View.INVISIBLE);
		    
		    fbShare.setEnabled(false);
	    	normalMap.setEnabled(false);
	    	sateliteMap.setEnabled(false);
	    	sateliteSwitch.setEnabled(false);
    	}
    }
    
    private void popBox(int type, String pmsg){
    	/*reset 1st*/
    	popbox.setVisibility(View.INVISIBLE);
    	popboxMSG.setVisibility(View.VISIBLE);
    	ic_close.setVisibility(View.INVISIBLE);
		popboxMSG.setText(pmsg);
		buttonClickable(true);
		/*reset 1st*/
    	
		popboxCenter.setVisibility(View.VISIBLE);
    	popbox.setVisibility(View.VISIBLE);
    	popboxOK_btn.setVisibility(View.INVISIBLE);
    	fbmsg.setVisibility(View.INVISIBLE);
    	
    	weatherOverlayShadow.setVisibility(View.INVISIBLE);
    	weatherOverlay.setVisibility(View.INVISIBLE);
    	ic_map_label.setVisibility(View.INVISIBLE);
    	
    	buttonClickable(false);
    	switch(type){
	    	case 0:{
	    		popbox.setVisibility(View.INVISIBLE);
	    		popboxMSG.setText(pmsg);
	    		buttonClickable(true);
	    		break;
	    	}
	    	case 1:{
	    		popboxOK_btn.setVisibility(View.VISIBLE);
	    		popboxMSG.setText(pmsg);
	    		break;
	    	}
	    	case 2:{
	    		popboxMSG.setText(pmsg);
	    		break;
	    	}
	    	case 3:{
	    		popboxCenter.setVisibility(View.INVISIBLE);
	    		
	    		//weatherOverlayShadow.setText(pmsg);
	    		//weatherOverlayShadow.setVisibility(View.VISIBLE);
	    		
	    		ic_map_label.setVisibility(View.VISIBLE);
	    		
	    		weatherOverlay.setText(pmsg);
	    		weatherOverlay.setVisibility(View.VISIBLE);

	    		break;
	    	}
	    	case 4:{
	    		fbCaptionFilled = true;
	    		fbmsg.setVisibility(View.VISIBLE);
	    		ic_close.setVisibility(View.VISIBLE);
	    		popboxOK_btn.setVisibility(View.VISIBLE);
	    		break;
	    	}
    	}
    }
    
    public void getWeatherData(double lat, double lon){
    	String url = "http://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon;
		//-------load JSON
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        //nameValuePairs.add(new BasicNameValuePair("convo_id", "4546db1fd1"));
        //nameValuePairs.add(new BasicNameValuePair("say", words));
		
		JSONObject json = getJSONfromURL(url, nameValuePairs);

		try {
			if(json == null){
				weatherString = "";
			}else{
				String curTempStr = json.getJSONObject("main").getString("temp").toString();
				JSONArray curSkyStatus = json.getJSONArray("weather");
				
				double curTempDeg = Double.parseDouble(curTempStr) / 10;
				
				DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "en"));
				symbols.setDecimalSeparator('.');
				DecimalFormat df = new DecimalFormat("#.#", symbols);
				
				weatherString = df.format(curTempDeg) + " °„C\n" + curSkyStatus.getJSONObject(0).getString("description").toString() + "\n";
				//Log.v("debug",weatherString);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//-------load JSON
    }
    
    public static JSONObject getJSONfromURL(String url,List<NameValuePair> postDatas ){

		//initialize
		InputStream is = null;
		String result = "";
		JSONObject jArray = null;

		//http post
		try{
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(url);
			
	        httppost.setEntity(new UrlEncodedFormEntity(postDatas));
			
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();

		}catch(Exception e){
			Log.e("log_tag", "Error in http connection "+e.toString());
		}

		//convert response to string
		try{
			BufferedReader reader = new BufferedReader(new InputStreamReader(is,"iso-8859-1"),8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			result=sb.toString();
		}catch(Exception e){
			Log.e("log_tag", "Error converting result "+e.toString());
		}

		//try parse the string to a JSON object
		try{
	        	jArray = new JSONObject(result);
		}catch(JSONException e){
			Log.e("log_tag", "Error parsing data "+e.toString());
		}

		return jArray;
	} 
    
    public boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
    
    /* for FB sdk */
    public Bitmap loadBitmapFromView_BITMAP(View v) {

    	Date d1 =new Date();
    	SimpleDateFormat date = new SimpleDateFormat("EEEE,\nMMM dd yyyy\nhh:mm aa");
    	
    	popBox(3,weatherString + date.format(d1));
    	
	    Bitmap bitmap;
		View v1 = v.getRootView();
		v1.setDrawingCacheEnabled(true);
		bitmap = Bitmap.createBitmap(v1.getDrawingCache());

		bitmap=Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * 1), (int)(bitmap.getHeight() * 1), true);
		v1.setDrawingCacheEnabled(false);
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);

		popBox(0,"");
		
		return bitmap;
	}
    
    public void publishPhoto(Bitmap imgData, String message) {
    	fbImg = imgData;
    	fbMsg = message;
		Session session = Session.getActiveSession();
			
	    if (session != null){//Log.v("DEBUG","FB session 2 : " + session + " | " + session.isOpened());

	        // Check for publish permissions    
	        List<String> permissions = session.getPermissions();
	        if (!isSubsetOf(PERMISSIONS, permissions)) {
	            pendingPublishReauthorization = true;
	            Session.NewPermissionsRequest newPermissionsRequest = new Session
	                    .NewPermissionsRequest(this, PERMISSIONS);
	        session.requestNewPublishPermissions(newPermissionsRequest);
	            return;
	        }else{
	        	popBox(0,"");
	        	popBox(2,"\n\nUploading, please hold \non...");
	        }

	        Request request = Request.newUploadPhotoRequest(session, imgData, new Request.Callback()
	        {
	            @Override
	            public void onCompleted(Response response)
	            {
	            	//Log.v("DEBUG","DONE UPLOAD : " + response);
	            	popBox(0,"");
	            	popBox(1,"Live traffic snapshot has been posted on your Facebook wall.");

	            }
	        });
	        Bundle postParams = request.getParameters(); // <-- THIS IS IMPORTANT
	        postParams.putString("name", message + " Get your Live Road Traffic App here http://goo.gl/u5mbYx #LiveRoadTrafficApp");
	        postParams.putString("link","http://goo.gl/zcBrSS");
	        request.setParameters(postParams);
	        request.executeAsync();
	        
	    }else{
	    	Session.openActiveSession(this, true, new Session.StatusCallback() {
	    		
			    // callback when session changes state
			    @SuppressWarnings("deprecation")
				@Override
			    public void call(Session session, SessionState state, Exception exception) {
					//Log.v("DEBUG","FB session : " + session + " | " + session.isOpened());	

			    	if (session.isOpened()) {
			    		publishPhoto(fbImg,fbMsg);
			    		// make request to the /me API
			    		/*Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {

			    		  // callback after Graph API response with user object
			    		  @Override
			    		  public void onCompleted(GraphUser user, Response response) {

			    			  

			    		  }
			    		});*/
			    		
			    	}
			    	
			    }
			  });
	    }

	}

    private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
	    for (String string : subset) {
	        if (!superset.contains(string)) {
	            return false;
	        }
	    }
	    return true;
	}
    /* for FB sdk */
    
    public void captureMapScreen() {
    	
    	popBox(2,"\n\nSnapshotting, please hold \non...");
    	
        SnapshotReadyCallback callback = new SnapshotReadyCallback() {

            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                try {
                	
                	Date d1 =new Date();
                	SimpleDateFormat date = new SimpleDateFormat("EEEE,\nMMM dd yyyy\nhh:mm aa");
                	
                	popBox(3,weatherString + date.format(d1));
                	
                	mainTop.setDrawingCacheEnabled(true);
                    Bitmap backBitmap = mainTop.getDrawingCache();
                    Bitmap bmOverlay = Bitmap.createBitmap(
                            backBitmap.getWidth(), backBitmap.getHeight(),
                            backBitmap.getConfig());
                    Canvas canvas = new Canvas(bmOverlay);
                    canvas.drawBitmap(snapshot, new Matrix(), null);
                    canvas.drawBitmap(backBitmap, 0, 0, null);
                    /*FileOutputStream out = new FileOutputStream(
                            Environment.getExternalStorageDirectory()
                                    + "/MapScreenShot"
                                    + System.currentTimeMillis() + ".png");*/

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmOverlay.compress(Bitmap.CompressFormat.PNG, 90, stream);
                    
                    //cacheMapImage.setImageBitmap(bmOverlay);
                    
                    //publishPhoto(loadBitmapFromView_BITMAP(mainTop),"Look at the traffic!");
                    
                    if(fbCaption == "" || fbCaption.length() == 0 || fbCaption == null){
                    	fbCaption = "My road traffic check!";
                    }
                    
                    publishPhoto(bmOverlay,fbCaption);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        googleMap.snapshot(callback);

    }
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}
    
    @Override
    protected void onResume() {
        super.onResume();

        initilizeMap();
    }

}
