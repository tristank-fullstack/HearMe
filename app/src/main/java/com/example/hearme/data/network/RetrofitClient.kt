package com.example.hearme.data.network

import android.util.Log
import com.example.hearme.data.network.api.GoogleApiService
import com.example.hearme.data.network.api.TranscriptionApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.FirebaseAuth // Para obtener el token
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.tasks.await // Para obtener token de forma suspendida
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

object RetrofitClient {

    // --- Configuración para GOOGLE APIS (Geocoding, Places, etc.) ---
    private const val GOOGLE_BASE_URL = "https://maps.googleapis.com/"
    // ¡¡ IMPORTANTE !! Tu API Key de Google Maps Platform
    // Considera moverla a un lugar seguro como local.properties y BuildConfig
    private const val GOOGLE_API_KEY = "AIzaSyAEu4xz1YOueLEzIFfEgNC5dU6EX-HNkUM"

    // Moshi (común para ambos clientes Retrofit)
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Logger de Red (común)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Muestra todo (cambiar a NONE en producción)
    }

    // Cliente OkHttp para GOOGLE APIS (con interceptor para añadir la key)
    private val googleApiOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain -> // Interceptor para añadir API Key a cada llamada a Google
            val original = chain.request()
            val originalHttpUrl = original.url
            val url = originalHttpUrl.newBuilder()
                .addQueryParameter("key", GOOGLE_API_KEY) // Añade ?key=TU_CLAVE
                .build()
            val requestBuilder = original.newBuilder().url(url)
            chain.proceed(requestBuilder.build())
        }
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Instancia de Retrofit para GOOGLE APIS
    private val retrofitGoogle = Retrofit.Builder()
        .baseUrl(GOOGLE_BASE_URL)
        .client(googleApiOkHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // Servicio para GOOGLE APIS
    val googleApiService: GoogleApiService by lazy {
        retrofitGoogle.create(GoogleApiService::class.java)
    }
    // --- Fin Configuración Google APIs ---


    private val myApiOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Base URL para la API
    private const val MY_API_BASE_URL = "https://hearmeapi-f6f9f893c738.herokuapp.com/"

    private val retrofitMyApi = Retrofit.Builder()
        .baseUrl(MY_API_BASE_URL)
        .client(myApiOkHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // Servicio para tu API (en este caso para transcripción)
    val transcriptionApiService: TranscriptionApiService by lazy {
        retrofitMyApi.create(TranscriptionApiService::class.java)
    }




} // Fin object RetrofitClient