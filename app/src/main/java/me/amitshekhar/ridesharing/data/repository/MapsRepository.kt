package me.amitshekhar.ridesharing.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.amitshekhar.ridesharing.data.models.LatLngNew
import me.amitshekhar.ridesharing.data.network.NetworkService
import me.amitshekhar.ridesharing.simulator.WebSocket
import me.amitshekhar.ridesharing.simulator.WebSocketListener
import me.amitshekhar.ridesharing.ui.maps.MapsPresenter
import me.amitshekhar.ridesharing.ui.maps.MapsViewModel
import me.amitshekhar.ridesharing.utils.Constants
import org.json.JSONObject
import javax.inject.Inject

class MapsRepository @Inject constructor(private val networkService: NetworkService) : WebSocketListener {

    companion object {
        private const val TAG = "MapsRepository"
    }

    private var webSocket: WebSocket

    private val _nearbyCabsFlow = MutableSharedFlow<List<LatLngNew>>(replay = 1)
    val nearbyCabsFlow: SharedFlow<List<LatLngNew>> = _nearbyCabsFlow

    private val _cabBookedEventRep = MutableStateFlow<Boolean>(false)
    val cabBookedEventRep: StateFlow<Boolean> = _cabBookedEventRep

    private val _cabArrivedEventRep = MutableStateFlow<Boolean>(false)
    val cabArrivedEventRep: StateFlow<Boolean> = _cabArrivedEventRep

    private val _cabIsArrivingEventRep = MutableStateFlow<Boolean>(false)
    val cabIsArrivingEventRep : StateFlow<Boolean> = _cabIsArrivingEventRep

    private val _pickupPathRep = MutableStateFlow<List<LatLngNew>>((listOf(LatLngNew(0.0, 0.0))))
    val pickupPathRep: StateFlow<List<LatLngNew>> = _pickupPathRep

    private val _updatedCabsFlowRep = MutableStateFlow<LatLngNew>(LatLngNew(0.0,0.0))
    val updatedCabsFlowRep: StateFlow<LatLngNew> = _updatedCabsFlowRep

    private val _informTripStartEventRep = MutableStateFlow<Boolean>(false)
    val informTripStartEventRep: StateFlow<Boolean> = _informTripStartEventRep

    private val _informTripEndEventRep = MutableStateFlow<Boolean>(false)
    val informTripEndEventRep: StateFlow<Boolean> = _informTripEndEventRep


    init {
        webSocket = networkService.createWebSocket(this)
        webSocket.connect()
    }

    fun onDetach() {
        webSocket.disconnect()
    }

    fun requestNearbyCabsRepo(latLngNew: LatLngNew) {
        val latLng = LatLng(latLngNew.latitude,latLngNew.longitude)
        val jsonObject = JSONObject()
        jsonObject.put(Constants.TYPE, Constants.NEAR_BY_CABS)
        jsonObject.put(Constants.LAT, latLng.latitude)
        jsonObject.put(Constants.LNG, latLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }

    fun requestCabRepo(pickUpLatLng: LatLngNew, dropLatLng: LatLngNew) {
        val jsonObject = JSONObject()
        jsonObject.put("type", "requestCab")
        jsonObject.put("pickUpLat", pickUpLatLng.latitude)
        jsonObject.put("pickUpLng", pickUpLatLng.longitude)
        jsonObject.put("dropLat", dropLatLng.latitude)
        jsonObject.put("dropLng", dropLatLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }

    fun handleOnMessageNearbyCabs(jsonObject: JSONObject)  {
        val nearbyCabLocations = arrayListOf<LatLngNew>()
        val jsonArray = jsonObject.getJSONArray(Constants.LOCATIONS)
        for (i in 0 until jsonArray.length()) {
            val lat = (jsonArray.get(i) as JSONObject).getDouble(Constants.LAT)
            val lng = (jsonArray.get(i) as JSONObject).getDouble(Constants.LNG)
            val latLng = LatLngNew(lat, lng)
            nearbyCabLocations.add(latLng)
        }
        Log.d(TAG,nearbyCabLocations.toString())
            _nearbyCabsFlow.tryEmit(nearbyCabLocations)
    }

    fun handleCabBooked(state: Boolean){
        _cabBookedEventRep.tryEmit(state)
    }

    fun showPath(pickupPathVal: List<LatLngNew>){
        _pickupPathRep.tryEmit(pickupPathVal)
    }

    fun updateCabLocation(cabLatLng: LatLngNew) {
        _updatedCabsFlowRep.tryEmit(cabLatLng)
    }


    fun informCabArrived(state: Boolean){
        _cabArrivedEventRep.tryEmit(state)
    }

    fun cabIsArriving(state: Boolean){
        _cabIsArrivingEventRep.tryEmit(state)
    }


    fun informTripStart(){
        _informTripStartEventRep.tryEmit(true)
    }

    fun informTripEnd(){
        _informTripEndEventRep.tryEmit(true)
    }

    override fun onConnect() {
        Log.d(TAG, "onConnect")
    }

    override fun onMessage(data: String) {
        Log.d(TAG, "onMessage data : $data")
        val jsonObject = JSONObject(data)
        when (jsonObject.getString(Constants.TYPE)) {
            Constants.NEAR_BY_CABS -> {
                handleOnMessageNearbyCabs(jsonObject)
            }
            Constants.CAB_BOOKED -> {
                handleCabBooked(true)
//                view?.informCabBooked()

            }
            Constants.PICKUP_PATH, Constants.TRIP_PATH -> {
                val jsonArray = jsonObject.getJSONArray("path")
                val pickUpPath = arrayListOf<LatLngNew>()
                for (i in 0 until jsonArray.length()) {
                    val lat = (jsonArray.get(i) as JSONObject).getDouble("lat")
                    val lng = (jsonArray.get(i) as JSONObject).getDouble("lng")
                    val latLng = LatLngNew(lat, lng)
                    pickUpPath.add(latLng)
                }
                showPath(pickUpPath)
            }
            Constants.LOCATION -> {
                val latCurrent = jsonObject.getDouble("lat")
                val lngCurrent = jsonObject.getDouble("lng")
                val latLng = LatLngNew(latCurrent,lngCurrent)
                updateCabLocation(latLng)
            }
            Constants.CAB_IS_ARRIVING -> {
                cabIsArriving(true)
            }
            Constants.CAB_ARRIVED -> {
                informCabArrived(true)
            }
            Constants.TRIP_START -> {
                informTripStart()
            }
            Constants.TRIP_END -> {
                informTripEnd()
            }
        }
    }

    override fun onDisconnect() {
        Log.d(TAG, "onDisconnect")
    }

    override fun onError(error: String) {
        Log.d(TAG, "onError : $error")
//        val jsonObject = JSONObject(error)
//        when (jsonObject.getString(Constants.TYPE)) {
//            Constants.ROUTES_NOT_AVAILABLE -> {
//                view?.showRoutesNotAvailableError()
//            }
//            Constants.DIRECTION_API_FAILED -> {
//                view?.showDirectionApiFailedError(
//                    "Direction API Failed : " + jsonObject.getString(
//                        Constants.ERROR
//                    )
//                )
//            }
//        }
    }



}

