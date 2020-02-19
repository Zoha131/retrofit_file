package com.trial.retrofitfile

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.IOException
import okio.Okio
import okio.source
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File


/*
* MY GOAL
* =============
* Get file from android
* Upload that file to server using retrofit
* Download File from server
* Save that file to the local storage
* */


/*
* Facing two main problems
* 1. Problem getting file using intent
*    => Uri lacks 'file' scheme:
*
* 2. Uploading that file using retrofit
*
* Also I need to download and save file using retrofit.
* I am not sure if there would be a problem too.
*
* It would be very grateful if you can help me with these problems
* */



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
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.type = "application/pdf"
        startActivityForResult(intent, DEVICE_PATH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {


        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEVICE_PATH && resultCode == Activity.RESULT_OK) {
            data?.data?.let {uri ->
                Log.d("PATHURL", "${uri.path} ${File(uri.path!!).name}")

                lifecycleScope.launch {
                    //val api_key =  RequestBody.create(MediaType.parse("text/plain"), "somevalue")
                    try {
                        val api_key = "DzkpCKjktggtCT1ZE8bFqca7anmmkpOcg975".toRequestBody("text/plain".toMediaTypeOrNull())
                        val tool_uid = "PR5".toRequestBody("text/plain".toMediaTypeOrNull())
                        val inputRequestBody = InputStreamRequestBody("application/pdf".toMediaTypeOrNull()!!, contentResolver, uri)

//
//                        val a = getRealPathFromURI(uri)
//                        val file: File = File(uri.path!!)
//
//                        val requestFile: RequestBody = file.asRequestBody("application/pdf".toMediaTypeOrNull())
//                        val multipartBody: MultipartBody.Part = MultipartBody.Part.createFormData("input", file.name, requestFile)

                        val result = pdfClient.convert(
                                input = inputRequestBody,
                                api_key = api_key,
                                tool_uid = tool_uid
                        )




                        Log.d("PATHURL", "${uri.path}")
                    }catch (ex: Exception){
                        Log.d("PATHURL", "fatal ${ex.message}")
                    }
                }
            }
        }
    }

    fun getPath(context: Context, uri: Uri): String? {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            System.out.println("getPath() uri: " + uri.toString())
            System.out.println("getPath() uri authority: " + uri.getAuthority())
            System.out.println("getPath() uri path: " + uri.getPath())
            // ExternalStorageProvider
            if ("com.android.externalstorage.documents" == uri.getAuthority()) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                println("getPath() docId: " + docId + ", split: " + split.size + ", type: " + type)
                // This is for checking Main Memory
                return if ("primary".equals(type, ignoreCase = true)) {
                    if (split.size > 1) {
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1] + "/"
                    } else {
                        Environment.getExternalStorageDirectory().toString() + "/"
                    }
                    // This is for checking SD Card
                } else {
                    "storage" + "/" + docId.replace(":", "/")
                }
            }
        }
        return null
    }

    fun getRealPathFromURI(contentUri: Uri?): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(contentUri!!, proj, null, null, null) ?: return null
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }
}


interface PDFClient {

    @POST("api/convert")
    @Multipart
    suspend fun convert(
            @Part("input") input: InputStreamRequestBody,
            @Part("api_key") api_key: RequestBody,
            @Part("tool_uid") tool_uid: RequestBody
    ): ResponseBody
}

class InputStreamRequestBody(
    private val contentType: MediaType,
    private val contentResolver: ContentResolver,
    private val uri: Uri
) :
    RequestBody() {
    override fun contentType(): MediaType {
        return contentType
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return -1
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        sink.writeAll((contentResolver.openInputStream(uri)!!.source()))
    }

}


