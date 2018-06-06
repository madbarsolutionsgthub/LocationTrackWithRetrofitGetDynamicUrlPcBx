package org.bitm.pencilbox.retrofitgetdynamicurlpb5;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.bitm.pencilbox.retrofitgetdynamicurlpb5.currentweather.CurrentWeatherResponse;
import org.bitm.pencilbox.retrofitgetdynamicurlpb5.currentweather.Main;
import org.bitm.pencilbox.retrofitgetdynamicurlpb5.currentweather.WeatherService;
import org.bitm.pencilbox.retrofitgetdynamicurlpb5.geocode.GeocodeResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity{
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/";
    private static final String GEO_BASE_URL = "https://maps.googleapis.com/maps/api/";
    private static final String TAG = MainActivity.class.getSimpleName();
    private WeatherService service;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String units = "metric";
    private FusedLocationProviderClient client;
    private TextView tv, latlngtv;
    private LocationCallback callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.tv);
        latlngtv = findViewById(R.id.latlngtv);

        client = LocationServices.getFusedLocationProviderClient(this);
        if(checkLocationPermission()){
            client.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        getData();
                    }
                }
            });
            callback = new LocationCallback(){
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    for(Location location : locationResult.getLocations()){
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        latlngtv.setText(latitude+", "+longitude);
                        getData();//weather data
                        getAddressData();
                    }
                }
            };
            client.requestLocationUpdates(getLocationRequest(),callback,null);
        }
    }

    private void getAddressData() {
        GeocodeService service = RetrofitClient.getClient(GEO_BASE_URL).create(GeocodeService.class);

        String endUrl = String.format("geocode/json?latlng=%f,%f&key=%s",latitude,longitude,getString(R.string.geocode_api));
        service.getAddress(endUrl).enqueue(new Callback<GeocodeResponse>() {
            @Override
            public void onResponse(Call<GeocodeResponse> call, Response<GeocodeResponse> response) {
                if(response.code() == 200){
                    GeocodeResponse geocodeResponse = response.body();
                    String address = geocodeResponse.getResults().get(0).getFormattedAddress();
                    ((TextView)findViewById(R.id.addressTV)).setText(address);
                }
            }

            @Override
            public void onFailure(Call<GeocodeResponse> call, Throwable t) {

            }
        });
    }

    private LocationRequest getLocationRequest(){
        LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(5000);
        request.setFastestInterval(2000);
        return request;
    }

    private void getData() {

        service = RetrofitClient.getClient(BASE_URL).create(WeatherService.class);
        String endUrl = String.format("weather?lat=%f&lon=%f&units=%s&appid=%s",latitude,longitude,units,
                getString(R.string.weather_api));

        Call<CurrentWeatherResponse> call = service.getCurrentWeatherData(endUrl);
        call.enqueue(new Callback<CurrentWeatherResponse>() {
            @Override
            public void onResponse(Call<CurrentWeatherResponse> call, Response<CurrentWeatherResponse> response) {
                if(response.code() == 200){
                    CurrentWeatherResponse currentWeatherResponse = response.body();
                    String city = currentWeatherResponse.getName();
                    double temp = currentWeatherResponse.getMain().getTemp();
                    Log.e(TAG, "onResponse: "+city+", "+temp);
                    tv.setText(city+", "+temp);
                }
            }

            @Override
            public void onFailure(Call<CurrentWeatherResponse> call, Throwable t) {

            }
        });
    }

    private boolean checkLocationPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},111);
            return false;
        }
        return true;
    }
}
