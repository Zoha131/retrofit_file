package com.trial.retrofitfile

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.net.toFile
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

class MainActivity : AppCompatActivity() {

    private val DEVICE_PATH: Int = 398
    private val BASE_URL = "https://pdftoworder.com/"
    private lateinit var pdfClient: PDFClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val logging = HttpLoggingInterceptor()
        logging.level = (HttpLoggingInterceptor.Level.BODY)

        val okHttpClient = OkHttpClient
                .Builder()
                .addInterceptor(logging)
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

        pdfClient = retrofit.create(PDFClient::class.java)


        getPath.setOnClickListener { getFileUri() }
    }


    fun getFileUri(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        //intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.setType("application/pdf")
        startActivityForResult(intent, DEVICE_PATH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {


        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEVICE_PATH && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                Log.d("PATHURL", "${it.path} ${File(it.path!!).name}")

                lifecycleScope.launch {
                    //val api_key =  RequestBody.create(MediaType.parse("text/plain"), "somevalue")
                    try {
                        val api_key = "DzkpCKjktggtCT1ZE8bFqca7anmmkpOcg975".toRequestBody("text/plain".toMediaTypeOrNull())
                        val tool_uid = "PR5".toRequestBody("text/plain".toMediaTypeOrNull())


                        val path = it.path!!
                        val file: File = it.toFile()

                        val requestFile: RequestBody = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                        val multipartBody: MultipartBody.Part = MultipartBody.Part.createFormData("input", file.name, requestFile)

                        val result = pdfClient.convert(
                                input = multipartBody,
                                api_key = api_key,
                                tool_uid = tool_uid
                        )



                        Log.d("imin", "$path")
                    }catch (ex: Exception){
                        Log.d("fatal", "${ex.message}")
                    }
                }
            }
        }
    }
}


interface PDFClient {

    @POST("api/convert")
    @Multipart
    suspend fun convert(
            @Part input: MultipartBody.Part,
            @Part("api_key") api_key: RequestBody,
            @Part("tool_uid") tool_uid: RequestBody
    ): ResponseBody
}


