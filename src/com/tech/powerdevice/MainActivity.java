package com.tech.powerdevice;


import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.support.v8.util.Mathematical;
import android.support.v8.util.Utils;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

/**
 * 
 * @author Vitor Ribeiro
 *
 */
public class MainActivity extends ActionBarActivity implements
SensorEventListener,
GoogleApiClient.ConnectionCallbacks,
GoogleApiClient.OnConnectionFailedListener,
com.google.android.gms.location.LocationListener {

	/** Log Tag. */
	private static final String TAG = MainActivity.class.getSimpleName();

	/** Interval for location updates. */
	private static final int UPDATE_INTERVAL = 1000;

	/** Tag for error flag. */
	private static final String STATE_RESOLVING_ERROR = "resolving_error";

	/** Sensor manager. */
	private SensorManager mSensorManager = null;

	/** Accelerometer sensor. */
	private Sensor mAccelerometer = null;
	/** Gravity sensor. */
	private Sensor mGravity = null;
	/** Linear Acceleration sensor. */
	private Sensor mLinearAcceleration = null;

	/** Magnetometer sensor. */
	private Sensor mMagnetometer = null;
	/** Uncalibrated Magnetometer sensor. */
	private Sensor mMagnetometerUncalibrated = null;
	/** Orientation sensor. */
	private Sensor mOrientation = null;
	/** Proximity sensor. */
	private Sensor mProximity = null;

	/** Gyroscope sensor. */
	private Sensor mGyroscope = null;
	/** Uncalibrated Gyroscope sensor. */
	private Sensor mGyroscopeUncalibrated = null;
	/** Game Rotation Vector sensor. */
	private Sensor mGameRotation = null;
	/** Geo Rotation Vector sensor. */
	private Sensor mGeoRotation = null;
	/** Rotation Vector sensor. */
	private Sensor mRotation = null;

	/** Light sensor. */
	private Sensor mLight = null;
	/** Pressure sensor. */
	private Sensor mPressure = null;
	/** Relative Humidity sensor. */
	private Sensor mHumidity = null;
	/** Ambient Temperature sensor. */
	private Sensor mAmbTemperature = null;
	/** Temperature sensor. */
	private Sensor mTemperature = null;

	/** Step Counter sensor. */
	private Sensor mStepCounter = null;

	/** Array of 3 floats containing the gravity vector expressed in the device's coordinate. */ 
	private float[] mFilteredGravity = null;
	/** Array of 3 floats containing the geomagnetic vector expressed in the device's coordinate. */
	private float[] mFilteredGeomagnetic = null;
	/** Array of 3 floats containing the device's orientation based on the rotation matrix.<br>
	 * values[0]: azimuth, rotation around the Z axis.<br>
	 * values[1]: pitch, rotation around the X axis.<br>
	 * values[2]: roll, rotation around the Y axis. */
	private float[] mFilteredOrientation = null;
	/** The geomagnetic inclination angle in radians. */
	private float mFilteredInclination = 0f;

	/** Telephony manager. */
	private TelephonyManager mTelephonyManager = null;
	/** Listener for telephony updates. */
	private PhoneStateListener mPhoneStateListener = null;

	/** Location manager. */
	private LocationManager mLocationManager = null;
	/** The main entry point for Google Play Services. */
	private GoogleApiClient mGoogleApiClient = null;
	/** Flag to track whether the application is resolving an error. */
	private boolean mResolvingError = false;
	/** Location object that holds accuracy and frequency parameters. */
	private LocationRequest mLocationRequest = null;
	/** Fallback listener if Google Services are not available. */
	private LocationListener mLocationListener = null;
	/** SkyView listener. */
	private Listener mGPSStatusListener = null;
	/** Listener to receive NMEA 0183 compliant sentences from the GPS. */
	private NmeaListener mGPSStatusNmeaListener = null;

	/** Map object. */
	private GoogleMap mMap = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState!=null) {
			mResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
		}

		// Setup the UI and sensors.
		setContentView(R.layout.activity_main);
		setUpUI();
		setUpSensors();
		setUpTelephony();
		setUpLocation();
		setUpSkyView();
		setUpNMEA();
		//setUpMapIfNeeded();
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Remove test providers to prevent fake locations.
		clearTestProviders();

		// Register the Telephony and Location.
		registerTelephony();
		registerLocation();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Register Sensors.
		registerSensors();

		// Setup map.
		setUpMapIfNeeded();
	}

	@Override
	protected void onPause() {
		// Unregister sensors.
		unregisterSensors();

		super.onPause();
	}

	@Override
	protected void onStop() {
		// Unregister the Telephony and Location.
		unregisterLocation();
		unregisterTelephony();

		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (outState!=null) {
			outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
		}

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 999) {
			mResolvingError = false;
			// Make sure the application is not already connected or attempting to connect
			if (resultCode == RESULT_OK && mGoogleApiClient != null && 
					!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
				mGoogleApiClient.connect();
			}
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if (sensor==null) {
			return;
		}

		if (BuildConfig.DEBUG) {
			Log.d(TAG, sensor.getName() + " accuracy: " + accuracy);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event==null) {
			return;	
		}

		// Due to some external source a considerable amount of noise is added to these signals. 
		// We apply a low pass filter as these high frequency signals (noise) 
		// cause the readings to hop between considerable high and low values. 
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			// Get the acceleration minus G on all axis.
			// Conceptually, this sensor measures the acceleration applied to the sensor itself. 
			// This means that the force of gravity is always influencing the measured acceleration.
			if (mFilteredGravity == null) {
				mFilteredGravity = event.values.clone();
			} else {
				mFilteredGravity = Mathematical.lowPassFilter(event.values.clone(), mFilteredGravity, 0.2f);
			}
			
			Log.i(TAG, "TYPE_ACCELEROMETER");
		} else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
			Log.i(TAG, "TYPE_GRAVITY");
		} else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			Log.i(TAG, "TYPE_LINEAR_ACCELERATION");
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			// Get the ambient magnetic field on all axis.
			if (mFilteredGeomagnetic == null) {
				mFilteredGeomagnetic = event.values.clone();
			} else {
				mFilteredGeomagnetic = Mathematical.lowPassFilter(event.values.clone(), mFilteredGeomagnetic, 0.2f);
			}
			
			Log.i(TAG, "TYPE_MAGNETIC_FIELD");
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) {
			Log.i(TAG, "TYPE_MAGNETIC_FIELD_UNCALIBRATED");
		} else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			Log.i(TAG, "TYPE_ORIENTATION");
		} else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
			Log.i(TAG, "TYPE_PROXIMITY");
		} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			Log.i(TAG, "TYPE_GYROSCOPE");
		} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
			Log.i(TAG, "TYPE_GYROSCOPE_UNCALIBRATED");
		} else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
			Log.i(TAG, "TYPE_GAME_ROTATION_VECTOR");
		} else if (event.sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
			Log.i(TAG, "TYPE_GEOMAGNETIC_ROTATION_VECTOR");
		} else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			Log.i(TAG, "TYPE_ROTATION_VECTOR");
		} else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
			Log.i(TAG, "TYPE_LIGHT");
		} else if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			Log.i(TAG, "TYPE_PRESSURE");
		} else if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
			Log.i(TAG, "TYPE_RELATIVE_HUMIDITY");
		} else if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
			Log.i(TAG, "TYPE_AMBIENT_TEMPERATURE");
		} else if (event.sensor.getType() == Sensor.TYPE_TEMPERATURE) {
			Log.i(TAG, "TYPE_TEMPERATURE");
		} else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
			Log.i(TAG, "TYPE_STEP_COUNTER");
		}
		

		try {
			Log.i(TAG, "0: " + event.values[0]);
			Log.i(TAG, "1: " + event.values[1]);
			Log.i(TAG, "2: " + event.values[2]);
			Log.i(TAG, "3: " + event.values[3]);
			Log.i(TAG, "4: " + event.values[4]);
			Log.i(TAG, "5: " + event.values[5]);	
		} catch (Exception unimportantException) {}

		if (mFilteredGravity != null && mFilteredGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			// We compute the inclination matrix I as well as the rotation matrix R 
			// transforming a vector from the device coordinate system to the world's coordinate system
			// which is defined as a direct orthonormal basis.

			if (SensorManager.getRotationMatrix(R, I, mFilteredGravity, mFilteredGeomagnetic)) {
				// X is tangential to the ground and roughly points East.
				// Y is tangential to the ground and points towards the magnetic North Pole.
				// Z points towards the sky and is perpendicular to the ground.
				// We can remap the coordinates if they do not suit our purpose.
				//SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, R);

				// Get the device's orientation: azimuth, pitch and roll.
				mFilteredOrientation = new float[3];
				SensorManager.getOrientation(R, mFilteredOrientation);
				mFilteredOrientation[0] = (float) (Math.toDegrees(mFilteredOrientation[0]));
				mFilteredOrientation[1] = (float) (Math.toDegrees(mFilteredOrientation[1]));
				mFilteredOrientation[2] = (float) (Math.toDegrees(mFilteredOrientation[2]));

				// Get the geomagnetic inclination angle.
				mFilteredInclination = (float) Math.toDegrees(SensorManager.getInclination(I));

				// Show values.
				String textViewSensor = (
						"Azimuth: " + Float.toString(mFilteredOrientation[0]) + "\n" +
								"Pitch: " + Float.toString(mFilteredOrientation[1]) + "\n" +
								"Roll: " + Float.toString(mFilteredOrientation[2]) + "\n" +
								"Inclination: " + Float.toString(mFilteredInclination));
			}
		}
	}

	@Override
	public void onConnected(Bundle dataBundle) {
		if (mGoogleApiClient!=null) {
			// OPTION: If you only want actions within a certain area, you do not need to register location updates. 
			// So save battery by using the Geofence APIs to set alerts when the device enters or exits one an area.
			//LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, geofence, pendingIntent);

			if (mLocationRequest!=null) {
				LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
			}

			// Get last known location.
			// Warning: passive locations can be inaccurate
			Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
			onLocationChanged(location);

			//fixOnLocation(location);
		}
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Google Play Services connection suspended");
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		// Google Play services can resolve some errors it detects.
		// If the error has a resolution, we try sending an Intent to
		// start a Google Play services activity that can resolve the error.
		if (mResolvingError) {
			// Already attempting to resolve an error.
			return;
		} else if (connectionResult.hasResolution()) {
			try {
				mResolvingError = true;
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this, 999);
			} catch (Exception e) {
				// There was an error with the resolution intent. Try again.
				if (BuildConfig.DEBUG) {
					Log.e(TAG, e!=null ? e.getMessage() : "Unidentified Error");
				}
				mGoogleApiClient.connect();
			}
		} else {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Google Play Services: " + connectionResult.getErrorCode());
			}
			mResolvingError = true;
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1) 
	@Override	
	public void onLocationChanged(Location location) {
		if (location == null) {
			return;
		}

		// Show the new location.
		String locationDetails = "Provider: " + location.getProvider().toUpperCase(Locale.getDefault()) + "\n";
		if (Utils.hasJellyBeanMR1()) {
			locationDetails += "Elapsed Time: " + Long.toString(location.getElapsedRealtimeNanos()) + "\n";
		}
		locationDetails += "Time: " + Long.toString(location.getTime()) + "\n" +
				"Accuracy: " + Float.toString(location.getAccuracy()) + "\n" +
				"Latitude: " + Double.toString(location.getLatitude()) + "\n" +
				"Longitude: " + Double.toString(location.getLongitude()) + "\n" +
				"Altitude: " + Double.toString(location.getAltitude()) + "\n" +
				"Bearing: " + Float.toString(location.getBearing()) + "\n" +
				"Speed: " + Float.toString(location.getSpeed());
	}

	/** Handle button clicks.
	 * 
	 * @param view button that was pressed.
	 */
	public void onClick(View view) {
		if (view==null) {
			return;
		}

		// Hide or show views according to the button.
		switch(view.getId()) {
		default:
			return;
		}
	}

	/**
	 * Setup UI.
	 */
	private void setUpUI() {

	}

	/**
	 * Setup sensors.
	 */
	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.KITKAT) 
	private void setUpSensors() {
		// Get the sensor manager.
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		if (mSensorManager == null) {
			return;
		}

		// Motion
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (Utils.hasGingerbread()) {
			mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
			mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		}

		// Position
		mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (Utils.hasJellyBeanMR2()) {
			mMagnetometerUncalibrated = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
		}
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		// Rotation
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		if (Utils.hasJellyBeanMR2()) {
			mGyroscopeUncalibrated = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
			mGameRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
		}
		if (Utils.hasKitKat()) {
			mGeoRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
		}
		if (Utils.hasGingerbread()) {
			mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR); 
		}

		// Environmental
		mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		if (Utils.hasIceCreamSandwich()) {
			mHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY); 
			mAmbTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE); 
		}
		mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE);

		// Step
		if (Utils.hasKitKat()) {
			mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
		}
	}

	/**
	 * Register the sensors.
	 */
	private void registerSensors() {
		if (mSensorManager == null) {
			return;
		}

		// Motion
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);

		// Position
		mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mMagnetometerUncalibrated, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);

		// Rotation
		mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mGyroscopeUncalibrated, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mGameRotation, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mGeoRotation, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_NORMAL);

		// Environmental
		mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mHumidity, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mAmbTemperature, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);

		// Step
		mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
	}

	/**
	 * Unregister the sensors.
	 */
	private void unregisterSensors() {
		if (mSensorManager == null) {
			return;
		}

		mSensorManager.unregisterListener(this);
	}

	/** 
	 * Setup telephony.
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2) 
	private void setUpTelephony() {
		// Get the telephony manager.
		mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if (mTelephonyManager == null) {
			return;
		}

		// Setup the telephony listener.
		mPhoneStateListener = new PhoneStateListener() {
			@Override
			public void onCellLocationChanged(CellLocation location) {
				if (location==null) {
					return;
				}

				String textViewCell;
				// The two major radio systems used in cell phones are 
				// GSM (Global System for Mobiles) and CDMA (Code Division Multiple Access).
				// The U.S. is mostly a CDMA country which is not part of the norm because most of the world is GSM. 				
				// The GSM vs CDMA gap will eventually close with the new 4G standard LTE (Long Term Evolution).
				if (location instanceof GsmCellLocation) {
					GsmCellLocation gsmCellLocation = (GsmCellLocation) location;

					textViewCell=("Cell ID: " + Integer.toString(gsmCellLocation.getCid()) + "\n" +
							"Location Area Code: " + Integer.toString(gsmCellLocation.getLac()) + "\n" +
							"Primary Scrambling Code : " + Integer.toString(gsmCellLocation.getPsc()));
				} else if (location instanceof CdmaCellLocation) {
					CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) location;

					// Convert coordinates to decimal degrees.
					double latitude = convertQuartSecToDecDegrees(cdmaCellLocation.getBaseStationLatitude());
					double longitude = convertQuartSecToDecDegrees(cdmaCellLocation.getBaseStationLongitude());

					textViewCell=("Base Station Id: " + Integer.toString(cdmaCellLocation.getBaseStationId()) + "\n" +
							"Base Station Location: " +	String.valueOf(latitude) + " : " + String.valueOf(longitude) + "\n" +
							"Network Id: " + Integer.toString(cdmaCellLocation.getNetworkId()) + "\n" + 
							"System Id: " + Integer.toString(cdmaCellLocation.getSystemId()));
				}
			}

			@SuppressWarnings("deprecation")
			@Override
			public void onCellInfoChanged(List<CellInfo> cellInfo) {
				if (cellInfo==null || !Utils.hasJellyBeanMR1()) {
					return;
				}

				// Show the cells information.
				// We iterate through all cells available.
				for (CellInfo cell : cellInfo) {
					// There are many types of cells with different properties.
					// Every cell has a cell identity and signal properties.
					// For an identity property, we can gather either 
					// base station/cell id and location either with coordinates or area codes.  
					// Signal properties can be represented in various ways.
					// We also have a timestamp and a register flag.
					// Note that a device can be registered to more than one cell.
					String cellDetails = "Timestamp: " + Long.toString(cell.getTimeStamp()) + "\n" +
							"Registered: " + cell.isRegistered() + "\n";

					if (cell instanceof CellInfoGsm && Utils.hasJellyBeanMR1()) {
						// Cell for 2G GSM digital cellular networks.
						CellInfoGsm cellInfoGsm = (CellInfoGsm) cell;
						CellIdentityGsm cellIdentity = cellInfoGsm.getCellIdentity();
						CellSignalStrengthGsm cellSignalStrength = cellInfoGsm.getCellSignalStrength();

						cellDetails += "Cell Type: GSM" + "\n" +
								"Cell Identity: " + Integer.toString(cellIdentity.getCid()) + "\n" +
								"Primary Scrambling Code: " + Integer.toString(cellIdentity.getPsc()) + "\n" +
								"Location Area Code: " + cellIdentity.getLac() + "\n" +
								"Mobile Country Code: " + Integer.toString(cellIdentity.getMcc()) + "\n" + 
								"Mobile Network Code: " + Integer.toString(cellIdentity.getMnc()) + "\n" +
								"Asu: " + Integer.toString(cellSignalStrength.getAsuLevel()) + "\n" +
								"Dbm: " + Integer.toString(cellSignalStrength.getDbm()) + "\n" +
								"Level: " + Integer.toString(cellSignalStrength.getLevel());
					} else if (cell instanceof CellInfoWcdma && Utils.hasJellyBeanMR2()) {
						// Cell for 3G GSM digital cellular networks.
						CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cell;
						CellIdentityWcdma cellIdentity = cellInfoWcdma.getCellIdentity();
						CellSignalStrengthWcdma cellSignalStrength = cellInfoWcdma.getCellSignalStrength();

						cellDetails += "Cell Type: WCDMA" + "\n" +
								"Cell Identity: " + Integer.toString(cellIdentity.getCid()) + "\n" +
								"Primary Scrambling Code: " + Integer.toString(cellIdentity.getPsc()) + "\n" +
								"Location Area Code: " + cellIdentity.getLac() + "\n" +
								"Mobile Country Code: " + Integer.toString(cellIdentity.getMcc()) + "\n" + 
								"Mobile Network Code: " + Integer.toString(cellIdentity.getMnc()) + "\n" +
								"Asu: " + Integer.toString(cellSignalStrength.getAsuLevel()) + "\n" +
								"Dbm: " + Integer.toString(cellSignalStrength.getDbm()) + "\n" +
								"Level: " + Integer.toString(cellSignalStrength.getLevel());
					} else if (cell instanceof CellInfoCdma && Utils.hasJellyBeanMR1()) {
						// Cell for 2G/3G CDMA digital cellular networks.
						CellInfoCdma cellInfoCdma = (CellInfoCdma) cell;
						CellIdentityCdma cellIdentity = cellInfoCdma.getCellIdentity();
						CellSignalStrengthCdma cellSignalStrength = cellInfoCdma.getCellSignalStrength();

						// Convert coordinates to decimal degrees.
						double latitude = convertQuartSecToDecDegrees(cellIdentity.getLatitude());
						double longitude = convertQuartSecToDecDegrees(cellIdentity.getLongitude());

						cellDetails += "Cell Type: CDMA" + "\n" +	
								"Base Station ID: " + Integer.toString(cellIdentity.getBasestationId()) + "\n" +
								"Base Station Location: " + String.valueOf(latitude) + " : " + String.valueOf(longitude)  + "\n" +
								"System Id: " + Integer.toString(cellIdentity.getSystemId()) + "\n" + 
								"Network Id: " + Integer.toString(cellIdentity.getNetworkId()) + "\n" +
								"Asu: " + Integer.toString(cellSignalStrength.getAsuLevel()) + "\n" +
								"Dbm: " + Integer.toString(cellSignalStrength.getDbm()) + "\n" +
								"Level: " + Integer.toString(cellSignalStrength.getLevel()) + "\n" +
								"Cdma Dbm: " + Integer.toString(cellSignalStrength.getCdmaDbm()) + "\n" +
								"Cdma Ecio: " + Integer.toString(cellSignalStrength.getCdmaEcio()) + "\n" +
								"Cdma Level: " + Integer.toString(cellSignalStrength.getCdmaLevel()) + "\n" +
								"Evdo Dbm: " + Integer.toString(cellSignalStrength.getEvdoDbm()) + "\n" +
								"Evdo Ecio: " + Integer.toString(cellSignalStrength.getEvdoEcio()) + "\n" +
								"Evdo Level: " + Integer.toString(cellSignalStrength.getEvdoLevel()) + "\n" +
								"Evdo Snr: " + Integer.toString(cellSignalStrength.getEvdoSnr());
					} else 	if (cell instanceof CellInfoLte && Utils.hasJellyBeanMR1()) {
						// Cell for 4G LTE digital cellular networks.
						CellInfoLte cellInfoLte = (CellInfoLte) cell;
						CellIdentityLte cellIdentity = cellInfoLte.getCellIdentity();
						CellSignalStrengthLte cellSignalStrength = cellInfoLte.getCellSignalStrength();

						// LTE cell details.
						cellDetails += "Cell Type: LTE" + "\n" +
								"Cell Identity: " + Integer.toString(cellIdentity.getCi()) + "\n" +
								"Physical Cell Id: " + Integer.toString(cellIdentity.getPci()) + "\n" +
								"Tracking Area Code: " + cellIdentity.getTac() + "\n" +
								"Mobile Country Code: " + Integer.toString(cellIdentity.getMcc()) + "\n" + 
								"Mobile Network Code: " + Integer.toString(cellIdentity.getMnc()) + "\n" +
								"Asu: " + Integer.toString(cellSignalStrength.getAsuLevel()) + "\n" +
								"Dbm: " + Integer.toString(cellSignalStrength.getDbm()) + "\n" +
								"Level: " + Integer.toString(cellSignalStrength.getLevel()) + "\n" +
								"Timing Advance: " + Integer.toString(cellSignalStrength.getTimingAdvance());
					}

					TextView cellView = new TextView(MainActivity.this);
					cellView.setText(cellDetails + "\n");
				}
			}

			@Override
			public void onSignalStrengthsChanged(SignalStrength signalStrength) {
				if (signalStrength==null) {
					return;
				}

				String textViewSignal = ("Gsm Bit Error Rate: " + Integer.toString(signalStrength.getGsmBitErrorRate()) + "\n" +
						"Gsm Signal Strength: " + Integer.toString(signalStrength.getGsmSignalStrength()) + "\n" +
						"Cdma Dbm: " + Integer.toString(signalStrength.getCdmaDbm()) + "\n" + 
						"Cdma Ecio: " + Integer.toString(signalStrength.getCdmaEcio()) + "\n" +
						"Evdo Dbm: " + Integer.toString(signalStrength.getEvdoDbm()) + "\n" +
						"Evdo Ecio: " + Integer.toString(signalStrength.getEvdoEcio()) + "\n" +
						"Evdo Snr: " + Integer.toString(signalStrength.getEvdoSnr()));
			}
		};

		// Get the device's telephony details. 
		String telephonyDetails = "Device Id: " + mTelephonyManager.getDeviceId() + "\n" +
				"Subscriber Id: " + mTelephonyManager.getSubscriberId() + "\n" +
				"Phone Type: " + getPhoneType(mTelephonyManager.getPhoneType()) + "\n" +
				"Network Type: " + getNetworkType(mTelephonyManager.getNetworkType()) + "\n" +
				"ISO Country Code: " + mTelephonyManager.getNetworkCountryIso() + "\n" +
				"Registered Operator: " + mTelephonyManager.getNetworkOperatorName() + "\n" +
				"Roaming: " + mTelephonyManager.isNetworkRoaming();

		// Get all observed cell detailed information from all radios on the device. 
		// These include the primary and neighboring cells.
		// It is only available since API 17+ and on some devices this may return null 
		// in which case we end up it the shorter cell information obtained from 
		// getCellLocation() plus getNeighboringCellInfo().
		if (Utils.hasJellyBeanMR1()) {
			List<CellInfo> cellInfo = mTelephonyManager.getAllCellInfo();
			mPhoneStateListener.onCellInfoChanged(cellInfo);
		}

		// Get the abridged cell information which the device is currently using.
		// As GSM/CDMA will eventually migrate to LTE, this method will be eventually be deprecated (maybe in 2020).
		// So if we are dealing with an LTE connection this method will return null, 
		// as there is still no complete Android implementation for the new 4G standard.
		CellLocation location = mTelephonyManager.getCellLocation();
		mPhoneStateListener.onCellLocationChanged(location);

		// Get the neighboring cell information of the device. 
		// Some of the identity codes might return invalid values as in some countries it is not possible 
		// to obtain cell identity without connecting to it. We can, however, use the valid signal values
		// to compare with the ones from the list returned in getAllCellInfo() and add some missing data on either lists.
		List<NeighboringCellInfo> neighboringCellInfo = mTelephonyManager.getNeighboringCellInfo();
		if (neighboringCellInfo!=null) {
			for (NeighboringCellInfo cell : neighboringCellInfo) {
				String neighboringCellDetails = "Cell Id: " + cell.getCid() + "\n" +
						"Network Type: " + getNetworkType(cell.getNetworkType()) + "\n" +
						"Location Area Code: " + cell.getLac() + "\n" +
						"Primary Scrambling Code: " + cell.getPsc() + "\n" +
						"Signal Strength: " + cell.getRssi();

				TextView cellView = new TextView(MainActivity.this);
				cellView.setText(neighboringCellDetails + "\n");
			}
		}

	}

	/**
	 * Register telephony listeners.
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1) 
	private void registerTelephony() {
		if (mTelephonyManager == null) {
			return;
		}

		// Listen for changes to the device's cell location and changes to the network signal strengths (cellular).
		int events = PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		if (Utils.hasJellyBeanMR1()) {
			// Add the observed cells information listener that is only available for API 17+.
			events |= PhoneStateListener.LISTEN_CELL_INFO;
		}
		mTelephonyManager.listen(mPhoneStateListener, events);
	}

	/**
	 * Unregister telephony listeners.
	 */
	private void unregisterTelephony() {
		if (mTelephonyManager == null) {
			return;
		}

		mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
	}

	/**
	 * Setup the location listeners. 
	 * First we try the Google Services Fused Location Provider, 
	 * if the last is not available then it falls back to the default provider. 
	 */
	private void setUpLocation() {
		// Location is usually obtained through the use of GPS or Wi-Fi APs plus Internet for lookup, 
		// although some devices possess other hardware or software capabilities to improve or speed up this process. 
		// The Location APIs from Google Services make it easy to build location-aware applications 
		// without needing to focus on the details of the underlying location technology. 
		// They minimize power consumption by using all of the capabilities of the device hardware.
		// The Fused Location Provider uses a combination of internal motion and position sensors, 
		// transceivers and location sensors.

		// Check if Google Play Services is available.
		int resultCode = ConnectionResult.INTERNAL_ERROR;
		try {
			resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);	
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, e!=null ? e.getMessage() : "Unidentified Error");
			}
		}

		// Acquire a reference to the system Location Manager.
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// If Google Play Services is available then create a new location client.
		if (ConnectionResult.SUCCESS == resultCode) {
			// Setup the LocationClient.
			mGoogleApiClient = new GoogleApiClient.Builder(this)
			.addApi(LocationServices.API)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.build();
			// Create the LocationRequest object.
			mLocationRequest = LocationRequest.create();
			// Set the priority and balance power with precision. Higher accuracy requires more battery.
			// The priority of the request is a strong hint to the LocationClient for which location sources to use. 
			// PRIORITY_HIGH_ACCURACY is more likely to use GPS; PRIORITY_BALANCED_POWER_ACCURACY is more likely 
			// to use WIFI & Cell tower positioning with about 100 meter accuracy, 
			// but it also depends on many other factors (such as which sources are available);
			// PRIORITY_LOW_POWER is considered to be about 10km accuracy;
			// PRIORITY_NO_POWER is equivalent to a passive listener, so unless a different client 
			// has requested location updates, this will return nothing.
			mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			// Set the update interval.
			mLocationRequest.setInterval(UPDATE_INTERVAL);
			// Set the fastest update interval. This means that if a different client 
			// has requested location updates in a smaller window than the one defined above, 
			// with higher or equivalent precision, we use this interval. 
			mLocationRequest.setFastestInterval(1000);
			// Set the smallest displacement in meters the user must move between location updates.
			mLocationRequest.setSmallestDisplacement(0);
		} else {
			// If Google Play Services was not available for some reason, 
			// show the error and use the default listeners.
			String error = "Google Play Services unavailable";
			try {
				error = GooglePlayServicesUtil.getErrorString(resultCode);
			} catch (Exception e) {
				if (BuildConfig.DEBUG) {
					Log.e(TAG, e!=null ? e.getMessage() : "Unidentified Error");
				}
			}
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Google Play Services: " + error);
			}

			// Setup the classic location listener if we do not have Google Play Services available.
			mLocationListener = new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					// Send the update to the same overridden method as the Google Play Services.
					MainActivity.this.onLocationChanged(location);
				}

				@Override
				public void onProviderDisabled(String provider) {
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "Provider disabled: " + provider);
					}
				}

				@Override
				public void onProviderEnabled(String provider) {
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "Provider enabled: " + provider);
					}
				}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "Status changed: " + provider + ":" + status);
					}
				}
			};
		}
	}

	/**
	 * Setup SkyView listener.
	 */
	private void setUpSkyView() {
		if (mLocationManager == null) {
			return;	
		}

		mGPSStatusListener = new GpsStatus.Listener() {
			@Override
			public void onGpsStatusChanged(int event) {
				GpsStatus status = null;

				switch (event) {
				case GpsStatus.GPS_EVENT_FIRST_FIX:
					// Get the time in milliseconds required to receive the first fix.
					status = mLocationManager.getGpsStatus(null);
					if (status!=null) {
						int firstFix = status.getTimeToFirstFix();
					}
					break;

				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					if (mLocationManager!=null) {
						status = mLocationManager.getGpsStatus(null);
						if (status!=null) {
							// Get the maximum number of satellites that can be listed.
							int maxSatellites = status.getMaxSatellites();

							// Show the SkyView information.
							// We iterate through all satellites available on the GPS engine.
							Iterable<GpsSatellite> skyview = status.getSatellites();
							for (GpsSatellite gpsSatellite : skyview) {
								String satelliteDetails = "Satellite ID: " + Integer.toString(gpsSatellite.getPrn()) + "\n" +
										"SNR: " + Float.toString(gpsSatellite.getSnr()) + "\n" +
										"Used In Fix: " + gpsSatellite.usedInFix() + "\n" +
										"Almanac: " + gpsSatellite.hasAlmanac() + "\n" +
										"Ephemeris: " + gpsSatellite.hasEphemeris() + "\n" +
										"Azimuth: " + Float.toString(gpsSatellite.getAzimuth()) + "\n" +
										"Elevation: " + Float.toString(gpsSatellite.getElevation());

								TextView satelliteView = new TextView(MainActivity.this);
								satelliteView.setText(satelliteDetails  + "\n");
							}
						}
					}
					break;
				case GpsStatus.GPS_EVENT_STARTED:
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "GPS system has started");
					}
					break;
				case GpsStatus.GPS_EVENT_STOPPED:
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "GPS system has stopped");
					}
					break;

				default:
					break;
				}
			}
		};
	}

	/**
	 * Setup NMEA listener.
	 */
	private void setUpNMEA() {
		if (mLocationManager == null) {
			return;
		}

		mGPSStatusNmeaListener = new GpsStatus.NmeaListener() {
			@Override
			public void onNmeaReceived(long timestamp, String nmea) {
				String textViewNMEA = ("NMEA Timestamp: " + Long.toString(timestamp) + "\n" +
						"NMEA Sentence: " + nmea);

				// TODO - incomplete method.
				try {
					NmeaParser.parseNmeaSentence(nmea);
				} catch (Exception e) {
					if (BuildConfig.DEBUG) {
						Log.e(TAG, e!=null ? e.getMessage() : "Unidentified Error");
					}
				}
			}
		}; 
	}

	/**
	 * Register location listeners.
	 */
	private void registerLocation() {
		// If the GoogleApiClient is not null then we have the Google Services available.
		if (mGoogleApiClient!=null && !mResolvingError) {
			mGoogleApiClient.connect();
		} 

		if (mLocationManager!=null) {
			// OPTION: If you only want actions within a certain area, you do not need to register location updates. 
			// So save battery by using the Geofence APIs to set alerts when the device enters or exits one an area.
			//mLocationManager.addProximityAlert(latitude, longitude, radius, expiration, pendingIntent);

			// If the LocationListener is not null then we request a location the classic way.
			if (mLocationListener!=null) {
				mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, 0, mLocationListener);
				mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL, 0, mLocationListener);
			}

			// Register SkyView listener.
			if (mGPSStatusListener!=null) {
				mLocationManager.addGpsStatusListener(mGPSStatusListener);
			}

			// Register NMEA listener.
			if (mGPSStatusNmeaListener!=null) {
				mLocationManager.addNmeaListener(mGPSStatusNmeaListener);
			}

			// Get last known location.
			// Warning: passive locations can be inaccurate
			Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			onLocationChanged(location);

			//fixOnLocation(location);
		}
	}

	/**
	 * Unregister location listeners.
	 */
	private void unregisterLocation() {
		// Remove and disconnect from the Google Play Services location updates.
		if (mGoogleApiClient!=null) {
			//LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofence);

			if (mLocationRequest!=null) {
				LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
			}
			mGoogleApiClient.disconnect();
		}

		// Remove all the listeners.
		if (mLocationManager!=null) {
			//mLocationManager.removeProximityAlert(pendingIntent);

			if (mLocationListener!=null) {
				mLocationManager.removeUpdates(mLocationListener);
			}
			if (mGPSStatusListener!=null) {
				mLocationManager.removeGpsStatusListener(mGPSStatusListener);
			}
			if (mGPSStatusNmeaListener!=null) {
				mLocationManager.removeNmeaListener(mGPSStatusNmeaListener);
			}
		}
	}

	/**
	 * Remove test providers.
	 */
	private void clearTestProviders() {
		if (mLocationManager == null) {
			return;
		}

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Mock Location enabled: " + isMockLocationEnabled());
		}

		// We iterate through the list of all known location providers and try to remove mock values.
		List<String> allProviders = mLocationManager.getAllProviders();
		for (String provider : allProviders) {
			try {
				mLocationManager.clearTestProviderEnabled(provider);
				mLocationManager.clearTestProviderLocation(provider);
				mLocationManager.clearTestProviderStatus(provider);
				mLocationManager.removeTestProvider(provider);	
			} catch (Exception e) {
				if (BuildConfig.DEBUG) {
					Log.e(TAG, e!=null ? e.getMessage() : "Unidentified Error");
				}
			}
		}
	}

	/**
	 * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
	 * installed) and the map has not already been instantiated. This will ensure that we only ever
	 * call {@link #setUpMap()} once when {@link #mMap} is not null.
	 * <p>
	 * If it isn't installed {@link SupportMapFragment} (and
	 * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
	 * install/update the Google Play services APK on their device.
	 * <p>
	 * A user can return to this FragmentActivity after following the prompt and correctly
	 * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
	 * have been completely destroyed during this process (it is likely that it would only be
	 * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
	 * method in {@link #onResume()} to guarantee that it will be called.
	 */
	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null) {

			// Try to obtain the map from the SupportMapFragment.
			try {
				//mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();	
			} catch (Exception e) {
				if (BuildConfig.DEBUG) {
					Log.e(TAG, e!=null ? e.getMessage() : "Unidentified Error");
				}
			}

			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				setUpMap();
			}
		}
	}

	/**
	 * This is where we can add markers or lines, add listeners or move the camera.
	 * This should only be called once and when we are sure that {@link #mMap} is not null.
	 */
	private void setUpMap() {
		mMap.getUiSettings().setAllGesturesEnabled(true);
		mMap.getUiSettings().setCompassEnabled(false);
		mMap.getUiSettings().setIndoorLevelPickerEnabled(false);
		mMap.getUiSettings().setMyLocationButtonEnabled(true);
		mMap.getUiSettings().setZoomControlsEnabled(false);

		mMap.setBuildingsEnabled(false);
		mMap.setIndoorEnabled(false);
		mMap.setMyLocationEnabled(true);
		mMap.setTrafficEnabled(false);
	}

	/** Check if the Mock Location setting is enabled.
	 * @return true if enabled, false otherwise.
	 */
	private boolean isMockLocationEnabled() {
		try {
			if (!Settings.Secure.getString(getContentResolver(), 
					Settings.Secure.ALLOW_MOCK_LOCATION).equalsIgnoreCase("0")) {
				return true;
			}
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, e!=null ? e.getMessage() : "Unidentified Error");
			}
		}

		return false;
	}

	/**
	 * Animate camera to a given location.
	 * @param location object.
	 */
	private void fixOnLocation(Location location) {
		if (location!=null) {
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
					new LatLng(location.getLatitude(), location.getLongitude()), 13));
		}
	}

	/**
	 * Converts latitude or longitude from 0.25 seconds 
	 * (as defined in the 3GPP2 C.S0005-A v6.0 standard) to decimal degrees.
	 * 
	 * @param quartSec latitude or longitude in 0.25 seconds units
	 * @return latitude or longitude in decimal degrees units
	 */
	private double convertQuartSecToDecDegrees(int quartSec) {
		if(Double.isNaN(quartSec) || quartSec < -2592000 || quartSec > 2592000){
			// Invalid value
			return Double.NaN;
		}
		return ((double)quartSec) / (3600 * 4);
	}


	/** Get the type of radio used to transmit voice calls. 
	 * Usually GSM or CDMA.
	 * @param type constant of the {@link #TelephonyManager} object.
	 * @return String representation of the phone type.
	 */
	private String getPhoneType(int type) {
		String phoneType = null;
		switch (type) {
		case TelephonyManager.PHONE_TYPE_CDMA:
			phoneType = "CDMA";
			break;
		case TelephonyManager.PHONE_TYPE_GSM:
			phoneType = "GSM";
			break;
		case TelephonyManager.PHONE_TYPE_SIP:
			phoneType = "SIP";
			break;
		default:
			phoneType = "NONE";
			break;
		}
		return phoneType;
	}

	/** Get the network type.
	 * @param type constant of the {@link #TelephonyManager} object.
	 * @return String representation of the network type.
	 */
	private String getNetworkType(int type) {
		String networkType = null;
		switch (type) {
		case TelephonyManager.NETWORK_TYPE_1xRTT:
			networkType = "1xRTT";
			break;
		case TelephonyManager.NETWORK_TYPE_CDMA:
			networkType = "CDMA";
			break;
		case TelephonyManager.NETWORK_TYPE_EDGE:
			networkType = "EDGE";
			break;
		case TelephonyManager.NETWORK_TYPE_EHRPD:
			networkType = "EHRPD";
			break;
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
			networkType = "EDVO 0";
			break;
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
			networkType = "EDVO A";
			break;
		case TelephonyManager.NETWORK_TYPE_EVDO_B:
			networkType = "EDVO B";
			break;
		case TelephonyManager.NETWORK_TYPE_GPRS:
			networkType = "GPRS";
			break;
		case TelephonyManager.NETWORK_TYPE_HSDPA:
			networkType = "HSDPA";
			break;
		case TelephonyManager.NETWORK_TYPE_HSPA:
			networkType = "HSPA";
			break;
		case TelephonyManager.NETWORK_TYPE_HSPAP:
			networkType = "HSPAP";
			break;
		case TelephonyManager.NETWORK_TYPE_HSUPA:
			networkType = "HSUPA";
			break;
		case TelephonyManager.NETWORK_TYPE_IDEN:
			networkType = "IDEN";
			break;
		case TelephonyManager.NETWORK_TYPE_LTE:
			networkType = "LTE";
			break;
		case TelephonyManager.NETWORK_TYPE_UMTS:
			networkType = "UMTS";
			break;
		default:
			networkType = "UNKNOWN";
			break;
		}
		return networkType;
	}
}
