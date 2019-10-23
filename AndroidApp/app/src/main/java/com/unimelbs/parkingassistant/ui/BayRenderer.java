package com.unimelbs.parkingassistant.ui;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.model.DataFeed;
import com.unimelbs.parkingassistant.util.DistanceUtil;
import com.unimelbs.parkingassistant.util.Timer;

/**
 * Custom cluster renderer, used to implement the logic of displaying markers
 * representing bays, and to control model update in an efficient way.
 */
public class BayRenderer extends DefaultClusterRenderer<Bay> implements GoogleMap.OnCameraIdleListener
{
    private final float AVAILABLE_BAY_COLOR = BitmapDescriptorFactory.HUE_GREEN;
    private final float OCCUPIED_BAY_COLOR = BitmapDescriptorFactory.HUE_RED;
    private final double STATE_API_CIRCLE_RADIUS = 1000;
    private final double STREET_VIEW_RADIUS = 250;
    private final int STATUS_FRESHNESS_INTERVAL=120;

    private Context context;
    private GoogleMap mMap;
    private ClusterManager<Bay> clusterManager;
    private static final String TAG="BayRenderer";
    private DataFeed dataFeed;
    private LatLng circleCentre;
    private long lastBayStatusUpdateTime;


    /**
     * Constructors for BayRenderer.
     * @param context
     * @param mMap
     * @param clusterManager
     */
    public BayRenderer(Context context, GoogleMap mMap, ClusterManager<Bay> clusterManager)
    {
        super(context,mMap,clusterManager);
        this.context = context;
        this.mMap = mMap;
        this.clusterManager = clusterManager;
    }
    public BayRenderer(Context context,
                       GoogleMap mMap, 
                       ClusterManager<Bay> clusterManager,
                       DataFeed dataFeed)
    {
        this(context,mMap,clusterManager);
        this.dataFeed = dataFeed;
        
    }

    /**
     * Checks bay information before rendering it on the map, changes marker properties
     * Accordingly.
     * @param item
     * @param markerOptions
     */
    @Override
    protected void onBeforeClusterItemRendered(Bay item, MarkerOptions markerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions);
        if (item.isAvailable())
        {

            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(AVAILABLE_BAY_COLOR));
        }
        else
        {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(OCCUPIED_BAY_COLOR));
        }
    }

    @Override
    protected void onClusterItemRendered(Bay clusterItem, Marker marker) {
        super.onClusterItemRendered(clusterItem, marker);
    }


    /**
     * This is executed when the user has finished interacting with the map.
     * i.e. zoom or move.
     */
    @Override
    public void onCameraIdle()
    {
        LatLng cameraFocus = mMap.getCameraPosition().target;
        //Calculating the radius of the circle including the Visible rectangle of the map.
        double radius = DistanceUtil.getRadius(mMap);
        Log.d(TAG, "onCameraIdle: current view radius:"+radius+" zoom:"+
                mMap.getCameraPosition().zoom);

        //Checks if radius (in meters) of the shown part of the map is < the defined street view
        //radius. This is the point when Bay status API is called to show it on the map.
        if (radius<STREET_VIEW_RADIUS)
        {
            //If this is the first use of the app, set a centre for a circle that bounds
            //an area for which bay status is updated.
            if (circleCentre==null)
            {
                circleCentre = mMap.getCameraPosition().target;

                lastBayStatusUpdateTime = System.currentTimeMillis();
                Log.d(TAG, "onCameraIdle: initial circle set. Position:"+
                        circleCentre.toString()+" updating bays + refreshing map at "+
                        Timer.convertToTimestamp(lastBayStatusUpdateTime));
                dataFeed.fetchBaysStates(circleCentre);
            }
            //This is the case that bay status has been updated in the same session.
            else
            {
                //Calculates bay status information validity (freshness).
                long dataLifeInSeconds = (System.currentTimeMillis()-lastBayStatusUpdateTime)/1000;
                Log.d(TAG, "onCameraIdle: data life: "+dataLifeInSeconds+" seconds.");

                //If the data is old (sensor data is updated every 2 minutes). Call back-end
                //API to get current status information.
                if (dataLifeInSeconds>STATUS_FRESHNESS_INTERVAL)
                {
                    Log.d(TAG, "onCameraIdle: current data timestamp: "+
                            Timer.convertToTimestamp(lastBayStatusUpdateTime)+
                            " system time: "+Timer.convertToTimestamp(System.currentTimeMillis())+
                            ". State data is old, refreshing it.");
                    dataFeed.fetchBaysStates(circleCentre);
                    lastBayStatusUpdateTime = System.currentTimeMillis();
                }
                //The case that the data is fresh.
                else
                {
                    //Calculates the physical distance corresponding to user navigation
                    //on the map from the centre of the last updated circle area.
                    double cameraMoveDistance =
                            Math.round(DistanceUtil.getDistanceS(circleCentre,cameraFocus));
                    double boundary = cameraMoveDistance+radius;

                    //Checks if the user has moved on the map within the area of the last updated circle
                    //If the user move outside the boundary.
                    if (boundary>STATE_API_CIRCLE_RADIUS)
                    {
                        Log.d(TAG, "onCameraIdle: Moved out of the boundary Need to call the state API again");
                        //Setting a new circle.
                        circleCentre=cameraFocus;
                        lastBayStatusUpdateTime=System.currentTimeMillis();
                        dataFeed.fetchBaysStates(circleCentre);
                    }
                    else
                    {
                        Log.d(TAG, "onCameraIdle: moving within boundaries."+
                                "State data timestamp:"+Timer.convertToTimestamp(lastBayStatusUpdateTime));
                    }
                }
            }
          }
        }
    }
