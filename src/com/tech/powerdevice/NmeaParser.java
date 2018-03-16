package com.tech.powerdevice;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils.SimpleStringSplitter;

/**
 * This class contains methods to parse NMEA sentences, and can compute its checksum.
 * 
 * @author Vitor Ribeiro
 * @author Herbert von Broeuschmeul
 *
 */
public class NmeaParser {

	/** This prevents the class from being instantiated. 
	 */
	private NmeaParser() {
	};

	/** Parse a NMEA Sentence. 
	 * TODO - Unfinished method.
	 * @param nmea
	 * @return
	 * @throws Exception
	 */
	public static void parseNmeaSentence(String nmea) throws Exception {
		String nmeaSentence = null;
		String sentence = null;
		String checksum = null;
		
		Pattern patern = Pattern.compile("\\$([^*$]*)(?:\\*([0-9A-F][0-9A-F]))?\r\n");
		Matcher matcher = patern.matcher(nmea);

		if (matcher.matches()){
			nmeaSentence = matcher.group(0);
			sentence = matcher.group(1);
			checksum = matcher.group(2);

			SimpleStringSplitter splitter = new SimpleStringSplitter(',');
			splitter.setString(sentence);
			String command = splitter.next();

			if (command.equalsIgnoreCase("GPGGA")) {
				// $GPGGA,123456,1234.123,N,1234.123,E,1,99,0.1,123.4,M,12.3,M,1,1*1A
				// Where:
				//  GGA          Global Positioning System Fix Data
				//  123456       Fix taken at 12:34:56 UTC
				//  1234.123,N   Latitude 12 deg 34.123' N
				//  1234.123,E   Longitude 12 deg 34.123' E
				//  1            Fix quality
				//  99           Number of satellites being tracked
				//  0.1          Horizontal dilution of position
				//  123.4,M      Altitude in Meters, above mean sea level
				//  12.3,M       Height of mean sea level above WGS84 ellipsoid
				//  1            Time in seconds since last DGPS update
				//  1            DGPS station ID number
				//  *1A          The checksum data, always begins with *

				// UTC time of fix.
				String time = splitter.next();
				// Latitude.
				String lat = splitter.next();
				// Direction (N/S).
				String latDir = splitter.next();
				// Longitude.
				String lon = splitter.next();
				// Direction (E/W).
				String lonDir = splitter.next();
				// Fix quality: 
				// 0 = invalid
				// 1 = GPS fix (SPS)
				// 2 = DGPS fix
				// 3 = PPS fix
				// 4 = Real Time Kinematic
				// 5 = Float RTK
				// 6 = estimated (dead reckoning) (2.3 feature)
				// 7 = Manual input mode
				// 8 = Simulation mode
				String quality = splitter.next();
				// Number of satellites being tracked.
				String nbSat = splitter.next();
				// Horizontal dilution of position.
				String hdop = splitter.next();
				// Altitude in Meters, above mean sea level.
				String alt = splitter.next();
				// Height of mean sea level above WGS84 ellipsoid.
				String geoAlt = splitter.next();
				// Time in seconds since last DGPS update.
				String timeDGPS = splitter.next();
				// DGPS station ID number.
				String idDGPS = splitter.next();

			} else if (command.equalsIgnoreCase("GPRMC")){
				// $GPRMC,123456,A,1234.123,N,1234.123,E,123.4,123.4,121212,123.4,E*1A
				// Where:
				//	RMC          Recommended Minimum sentence C
				//	123456       Fix taken at 12:34:56 UTC
				//	A            Status.
				//	1234.123,N   Latitude 12 deg 34.123' N
				//	1234.123,E   Longitude 12 deg 34.123' E
				//	123.4        Speed over the ground in knots
				//	123.4        Track angle in degrees True
				//	121212       Date - 12/12/12
				//	123.4,E      Magnetic Variation
				//	*1A          The checksum data, always begins with *

				// UTC time of fix.
				String time = splitter.next();
				// Fix status: A=active, V=void, D=differential, E=estimated, N=not valid, S=simulator. 
				String status = splitter.next();
				// Latitude.
				String lat = splitter.next();
				// Direction (N/S).
				String latDir = splitter.next();
				// Longitude.
				String lon = splitter.next();
				// Direction (E/W).
				String lonDir = splitter.next();
				// Speed over the ground in knots.		 
				String speed = splitter.next();
				// Track angle in degrees True.
				String bearing = splitter.next();
				// UTC date of fix.
				String date = splitter.next();
				// Magnetic Variation.
				String magn = splitter.next();
				// Magnetic variation direction (E/W).
				String magnDir = splitter.next();

			} else if (command.equalsIgnoreCase("GPGSA")){
				// $GPGSA,A,3,01,02,03,04,05,06,07,08,09,10,11,12,2.5,1.5,2.0*1A
				// Where:
				//  GSA      Satellite status
				//  A        Auto selection of 2D or 3D fix (M = manual) 
				//  3        3D fix
				//  01 to 12 PRNs of satellites used for fix (space for 12) 
				//  2.5      PDOP (Position dilution of precision) 
				//  1.5      Horizontal dilution of precision (HDOP) 
				//  2.0      Vertical dilution of precision (VDOP)
				//  *1A      The checksum data, always begins with *

				// Mode: A = Auto selection of 2D or 3D fix; M = manual.
				String mode = splitter.next();
				// Fix type: 1 - no fix; 2 - 2D; 3 - 3D.
				String fixType = splitter.next();
				// Discard PRNs of satellites used for fix (space for 12) 
				for (int i=0 ; ((i<12)&&(!"1".equals(fixType))); i++){
					splitter.next();
				}
				// Position dilution of precision (float).
				String pdop = splitter.next();
				// Horizontal dilution of precision (float).
				String hdop = splitter.next();
				// Vertical dilution of precision (float).
				String vdop = splitter.next();		

			} else if (command.equalsIgnoreCase("GPVTG")){
				// $GPVTG,123.4,T,123.4,M,123.4,N,123.4,K*1A
				// Where:
				//	VTG          Track made good and ground speed
				//	123.4,T      True track made good (degrees)
				//	123.4,M      Magnetic track made good
				//	123.4,N      Ground speed, knots
				//	123.4,K      Ground speed, Kilometers per hour
				//  *1A          The checksum data, always begins with *

				// Track angle in degrees True.
				String bearing = splitter.next();
				// T.
				splitter.next();
				// Magnetic track made good.
				String magn = splitter.next();
				// M.
				splitter.next();
				// Speed over the ground in knots.		 
				String speedKnots = splitter.next();
				// N.
				splitter.next();
				// Speed over the ground in Kilometers per hour.		 
				String speedKm = splitter.next();
				// K.
				splitter.next();

			} else if (command.equalsIgnoreCase("GPGLL")){
				// $GPGLL,1234.12,N,1234.12,E,123456,A,*1A
				// Where:
				//	GLL          Geographic position, Latitude and Longitude
				//	1234.12,N    Latitude 12 deg. 34.12 min. North
				//	1234.12,W    Longitude 12 deg. 34.12 min. West
				//	123412       Fix taken at 12:34:56 UTC
				//	A            Status
				//  *1A          The checksum data, always begins with *

				// Latitude.
				String lat = splitter.next();
				// Direction (N/S).
				String latDir = splitter.next();
				// Longitude.
				String lon = splitter.next();
				// Direction (E/W).
				String lonDir = splitter.next();
				// UTC time of fix.
				String time = splitter.next();
				// Fix status: A=active, V=void, D=differential, E=estimated, N=not valid, S=simulator.
				String status = splitter.next();
			}
		}
	}

	/** Convert latitude from degrees to double representation.
	 * 
	 * @param lat in degrees.
	 * @param orientation either N or S.
	 * @return latitude as a double.
	 */
	public static double parseNmeaLatitude(String lat, String orientation) {
		double latitude = 0.0;
		if (lat != null && orientation != null && !lat.isEmpty() && !orientation.isEmpty()) {
			double temp1 = Double.parseDouble(lat);
			double temp2 = Math.floor(temp1/100d); 
			double temp3 = (temp1/100 - temp2)/0.6d;
			
			if (orientation.equals("S")) {
				latitude = -(temp2+temp3);
			} else if (orientation.equals("N")) {
				latitude = (temp2+temp3);
			}
		}
		
		return latitude;
	}

	/** Convert longitude from degrees to double representation.
	 * 
	 * @param lon in degrees.
	 * @param orientation either W or E.
	 * @return longitude as a double.
	 */
	public static double parseNmeaLongitude(String lon, String orientation) {
		double longitude = 0.0;
		if (lon != null && orientation != null && !lon.isEmpty() && !orientation.isEmpty()) {
			double temp1 = Double.parseDouble(lon);
			double temp2 = Math.floor(temp1/100d); 
			double temp3 = (temp1/100d - temp2)/0.6d;
			
			if (orientation.equals("W")) {
				longitude = -(temp2+temp3);
			} else if (orientation.equals("E")) {
				longitude = (temp2+temp3);
			}
		}
		
		return longitude;
	}

	/** Convert speed to meters per second.
	 * @param speed unit.
	 * @param metric unit representation.
	 * @return speed in meters per second.
	 */
	public static float parseNmeaSpeed(String speed, String metric) {
		float meterSpeed = 0.0f;
		if (speed != null && metric != null && !speed.isEmpty() && !metric.isEmpty()) {
			float temp1 = Float.parseFloat(speed)/3.6f;
			if (metric.equals("K")) {
				meterSpeed = temp1;
			} else if (metric.equals("N")) {
				meterSpeed = temp1*1.852f;
			}
		}
		
		return meterSpeed;
	}

	/** Convert time to the Unix timestamp.
	 * @param time
	 * @return timestamp.
	 * @throws Exception
	 */
	public static long parseNmeaTime(String time) throws Exception {
		long timestamp = 0;
		SimpleDateFormat fmt = new SimpleDateFormat("HHmmss.SSS", Locale.getDefault());
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));

		if (time != null && time != null) {
			long now = System.currentTimeMillis();
			long today = now - (now %86400000L);
			
			// Sometime we don't have millisecond in the time string, so we have to reformat it. 
			long temp1 = fmt.parse(String.format((Locale)null,"%010.3f", Double.parseDouble(time))).getTime();
			long temp2 = today+temp1;
			
			// If we're around midnight we could have a problem.
			if (temp2 - now > 43200000l) {
				timestamp  = temp2 - 86400000l;
			} else if (now - temp2 > 43200000l){
				timestamp  = temp2 + 86400000l;
			} else {
				timestamp  = temp2;
			}
		}

		return timestamp;
	}

	/** Compute checksum.
	 * @param string
	 * @return checksum.
	 */
	public static byte computeNmeaChecksum(String string) {
		byte checksum = 0;
		for (char c : string.toCharArray()){
			checksum ^= (byte)c;			
		}
		
		return checksum;
	}
}