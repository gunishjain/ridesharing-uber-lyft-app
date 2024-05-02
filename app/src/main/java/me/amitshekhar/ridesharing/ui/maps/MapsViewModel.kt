package me.amitshekhar.ridesharing.ui.maps

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.amitshekhar.ridesharing.data.models.LatLngNew
import me.amitshekhar.ridesharing.data.models.Marker
import me.amitshekhar.ridesharing.data.repository.MapsRepository
import me.amitshekhar.ridesharing.simulator.WebSocketListener
import me.amitshekhar.ridesharing.utils.Constants
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class MapsViewModel @Inject constructor(private val mapsRepository: MapsRepository) : ViewModel() {

    companion object {
        private const val TAG = "MapsVM"

    }

    private val _latLngNewState = MutableStateFlow((listOf(LatLngNew(0.0, 0.0))))
    val latLngNewState: StateFlow<List<LatLngNew>> = _latLngNewState

    private val _cabBookedEvent = MutableStateFlow<Boolean>(false)
    val cabBookedEvent: StateFlow<Boolean> = _cabBookedEvent

    private val _cabIsArrivingEvent = MutableStateFlow<Boolean>(false)
    val cabIsArrivingEvent : StateFlow<Boolean> = _cabIsArrivingEvent

    private val _pickupPathState = MutableStateFlow<List<LatLngNew>>((listOf(LatLngNew(0.0, 0.0))))
    val pickupPathState: StateFlow<List<LatLngNew>> = _pickupPathState

    private val _updatedCabsFlow = MutableStateFlow<LatLngNew>(LatLngNew(0.0,0.0))
    val updatedCabsFlow: StateFlow<LatLngNew> = _updatedCabsFlow

    private val _cabArrivedEvent = MutableStateFlow<Boolean>(false)
    val cabArrivedEvent: StateFlow<Boolean> = _cabArrivedEvent

    private val _informTripStartEvent = MutableStateFlow<Boolean>(false)
    val informTripStartEventRep: StateFlow<Boolean> = _informTripStartEvent

    private val _informTripEndEvent = MutableStateFlow<Boolean>(false)
    val informTripEndEventRep: StateFlow<Boolean> = _informTripEndEvent


    fun requestNearbyCabs(latLngNew: LatLngNew) {
        viewModelScope.launch {
            mapsRepository.requestNearbyCabsRepo(latLngNew)
            mapsRepository.nearbyCabsFlow.collect {
                Log.d(TAG, it.toString())
                _latLngNewState.value = it
            }
        }
    }


    private suspend fun checkCabBook(){
        mapsRepository.cabBookedEventRep.collect {
            Log.d(TAG, it.toString())
            _cabBookedEvent.value=it
        }
    }

    private suspend fun pickupPath() {

            mapsRepository.pickupPathRep.collect {
                Log.d(TAG, it.toString())
                _pickupPathState.value = it
            }

    }

    private suspend fun updateCabLocation() {

            mapsRepository.updatedCabsFlowRep.collect {
                Log.d(TAG, it.toString())
                _updatedCabsFlow.value=it

        }
    }

    private suspend fun cabIsArriving(){
        mapsRepository.cabIsArrivingEventRep.collect {
            Log.d(TAG, it.toString())
            _cabIsArrivingEvent.value=it
        }
    }

    private suspend fun cabArrived(){
        mapsRepository.cabArrivedEventRep.collect {
            Log.d(TAG, it.toString())
            _cabArrivedEvent.value = it
        }
    }

    private suspend fun informTripStart(){
        mapsRepository.informTripStartEventRep.collect {
            _informTripStartEvent.value=it
        }
    }

    private suspend fun informTripEnd(){
        mapsRepository.informTripEndEventRep.collect {
            _informTripEndEvent.value=it
        }
    }

    
    fun requestCab(pickUpLatLng: LatLngNew, dropLatLng: LatLngNew){

        viewModelScope.launch {

            mapsRepository.requestCabRepo(pickUpLatLng,dropLatLng)

            viewModelScope.launch {
                mapsRepository.cabBookedEventRep.collect {
                    Log.d(TAG, it.toString())
                    _cabBookedEvent.value=it
                }
            }


            viewModelScope.launch {
                mapsRepository.pickupPathRep.collect {
                    Log.d(TAG, it.toString())
                    _pickupPathState.value = it
                }
            }

            viewModelScope.launch {
                mapsRepository.updatedCabsFlowRep.collect {
                    Log.d(TAG, it.toString())
                    _updatedCabsFlow.value = it
                }
                }

            viewModelScope.launch {
                mapsRepository.cabIsArrivingEventRep.collect {
                    Log.d(TAG, it.toString())
                    _cabIsArrivingEvent.value=it
                }
            }


            viewModelScope.launch {
                mapsRepository.cabArrivedEventRep.collect {
                    Log.d(TAG, it.toString())
                    _cabArrivedEvent.value = it
                }
            }


            viewModelScope.launch {
                mapsRepository.informTripStartEventRep.collect {
                    Log.d(TAG,it.toString())
                    _informTripStartEvent.value=it
                }
            }

            viewModelScope.launch {
                mapsRepository.informTripEndEventRep.collect {
                    _informTripEndEvent.value=it
                }
            }

//            checkCabBook()
//            pickupPath()
//            updateCabLocation()
//            cabIsArriving()
//            cabArrived()
//            informTripStart()
//            informTripEnd()
        }

    }

}