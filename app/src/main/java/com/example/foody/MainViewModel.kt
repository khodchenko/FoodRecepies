package com.example.foody

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.foody.data.Repository
import com.example.foody.models.FoodRecipe
import com.example.foody.util.NetworkResult
import kotlinx.coroutines.launch
import retrofit2.Response

class MainViewModel @ViewModelInject constructor(
    private val repository: Repository,
    application: Application
) : AndroidViewModel(application) {

    var recipesResponse: MutableLiveData<NetworkResult<FoodRecipe>> = MutableLiveData()

    fun getRecipes(queries: Map<String,String>) = viewModelScope.launch {
        getRecipesSafeCall(queries)
    }

    private suspend fun getRecipesSafeCall(queries: Map<String, String>) {
        recipesResponse.value = NetworkResult.Loading()
        if (hasInternetConnection()){
            try {
                val response =repository.remote.getRecipes(queries)
                recipesResponse.value = handleFoodRecipesResponse(response)
            }catch (e:Exception){
                recipesResponse.value = NetworkResult.Error("Recipes not found.")
            }
        }else{
            recipesResponse.value = NetworkResult.Error("No Internet Connection.")
        }

    }

    private fun handleFoodRecipesResponse(response: Response<FoodRecipe>): NetworkResult<FoodRecipe>? {
        when {
            response.message().toString().contains("timeout") -> {
                return NetworkResult.Error("Timeout")
            }
            response.code() == 402 -> {
                return NetworkResult.Error("API Key Limited.    ")
            }
            response.body()!!.results.isNullOrEmpty() -> {
                return NetworkResult.Error("Recipes not found.")
            }
            response.isSuccessful -> {
                val foodRecipes = response.body()
                return NetworkResult.Success(foodRecipes!!)
            }
            else -> {
                return NetworkResult.Error(response.message())
            }
        }
    }


    private fun hasInternetConnection():Boolean{
        val connectivityManger = getApplication<Application>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val activeNetwork = connectivityManger.activeNetwork ?: return false
        val cappabilites = connectivityManger.getNetworkCapabilities(activeNetwork) ?: return false
        return when{
            cappabilites.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            cappabilites.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            cappabilites.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}