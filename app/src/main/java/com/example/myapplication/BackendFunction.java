package com.example.myapplication;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.database.Cursor;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

public class BackendFunction  extends AsyncTask<Bitmap,Void,String>{
    private static  Context context;
    private String Challan_no = "default" ;
    private String Action = "default";
    private String Role = "default" ;
    private String MEDIA = null ;

   static int serverResponseCode;


    public BackendFunction(Context context){
        this.context=context;
    }


    public void storelocation(String  longitude , String lattitude , String challan_no ){

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = Constant.ROOT_URL+"storelocation?longitude="+longitude+"&lattitude="+lattitude +"&challanid="+challan_no;

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                     ScaffChallanstatus.getInstance().alert("Stored"+longitude+":"+longitude);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);

    }


    public  void uploadMedia(Bitmap media, String challanid ,String action , String role) {
          Challan_no = challanid ; Action = action ; Role = role ;
          execute(media);   // call to start the background thread execution
    }


    @Override
    protected String doInBackground(Bitmap ... bitmaps) {
        // Background thread

        // this will convert the bitmap to string so we can send it through URL
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmaps[0].compress(Bitmap.CompressFormat.JPEG,70,byteArrayOutputStream);
        byte[] imagebytearray = byteArrayOutputStream.toByteArray();
        String encodedimageforupload = Base64.encodeToString(imagebytearray, Base64.DEFAULT);
        return encodedimageforupload;
    }

    @Override
    protected void onPreExecute() {

    }
    @Override
    protected void onPostExecute(String result) {
        // DO what after doInBackground process complete
        MEDIA  = result ;

            RequestQueue queue = Volley.newRequestQueue(context);
            String url = Constant.ROOT_URL + "challanverify";

            StringRequest sr = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.e("HttpClient", "success! response: " + response.toString());
                         //   Toast.makeText(context,""+response.toString()+" UPLOADED",Toast.LENGTH_LONG).show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("HttpClient", "error: " + error.toString());
                            Toast.makeText(context,"Server_Error",Toast.LENGTH_LONG).show();
                        }

                    })
            {  // pass request body
                @Override
                protected Map<String,String> getParams(){
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("media",MEDIA);
                    params.put("challanid",Challan_no);
                    params.put("action",Action);
                    params.put("Role",Role);
                    return params;
                }
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("Content-Type","application/x-www-form-urlencoded");
                    return params;
                }
            };  // you are sending a imagestring so it takes too much time So you need to set it
            sr.setRetryPolicy(new DefaultRetryPolicy(120000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(sr);

    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);  // eq.. SHow Downloading percentage   .. process update
    }



    public static void Video_Upload(String sourceFileUri , String challan_no, String action, String role ) {
        String upLoadServerUri =  Constant.ROOT_URL + "challanverify";
        String fileName = sourceFileUri;
        String[] data = {action,challan_no,role};

        // RUN UPLOAD CODE IN BACKGROUND SO IT DO NOT INTERUPT THE MAIN THREAD PROCESS OR HOLD THE UI
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    //Your code goes here
                    String response =   upload_to_server(sourceFileUri ,data);
                 System.out.println("[RESPONSE]  :"+response);


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    } // end upLoad2Server

    public static String upload_to_server(String file , String[] Data) {

        String fileName = file;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        File sourceFile = new File(file);
        if (!sourceFile.isFile()) {
            Log.e("Huzza", "Source File Does not exist");
            return null;
        }

        try {
            FileInputStream fileInputStream = new FileInputStream(sourceFile);
            URL url = new URL(Constant.ROOT_URL + "challanverify");
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("myFile", fileName);
            conn.setRequestProperty("data",String.valueOf(Data));
            dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"myFile\";filename=\"" + fileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);
         /*   {"myFile":{"name":"VID_20210116_150549782.mp4","type":"","tmp_name":"\/tmp\/php7Gjtsn","error":0,"size":1857196}}   ON SERVER YOU FINE THIS FORMAT (return $_FILES)
            move_uploaded_file($_FILES["myFile"]["tmp_name"], $_FILES["myFile"]["name"]); */
            bytesAvailable = fileInputStream.available();
            Log.i("Huzza", "Initial .available : " + bytesAvailable);

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            serverResponseCode = conn.getResponseCode();

            fileInputStream.close();
            dos.flush();
            dos.close();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (serverResponseCode == 200) {
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn
                        .getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                rd.close();
            } catch (IOException ioex) {
            }
            return sb.toString();
        }else {
            return "Could not upload";
        }
    }

    private String getAppDir() { // get the path of storage directory
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
    public  void CompressVideo(String videoPath) throws FFmpegCommandAlreadyRunningException {
        // this method is used to compress the video via "fast-forward mpeg"
      //  implementation 'nl.bravobit:android-ffmpeg:1.1.1' // video compress  Dependency add it in gradle

        String inputVideoPath = videoPath ;                 // video path
        String outputPath = getAppDir() + "/"+String.valueOf(323) +"video_compress.mp4"; // output file path
        String[] commandArray = new String[]{"-y", "-i", inputVideoPath, "-s", "720x480", "-r", "25",                  // command to execute
                "-vcodec", "mpeg4", "-b:v", "300k", "-b:a", "48000", "-ac", "2", "-ar", "22050", outputPath};

        if (FFmpeg.getInstance(context).isSupported()) {
            // ffmpeg is supported
            System.out.println("supported");

            FFmpeg ffmpeg = FFmpeg.getInstance(context);
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(commandArray, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Log.e("FFmpeg", "onStart");
                }

                @Override
                public void onProgress(String message) {
                    Log.e("FFmpeg onProgress? ", message);
                }

                @Override
                public void onFailure(String message) {
                    Log.e("FFmpeg onFailure? ", message);
                }

                @Override
                public void onSuccess(String message) {
                    Log.e("FFmpeg onSuccess? ", message);

                }

                @Override
                public void onFinish() {
                    System.out.println("converted output file path is"+outputPath);
                    Video_Upload(outputPath,Challan_no,Action,Constant.ROLE);        // Now upload the compressed file to server
                }

            });

        } else {
            // ffmpeg is not supported
            System.out.println("Not Supported");
        }

    }
}

/*---------------------------------------- AsyncTask -------------------------------*/
/* AsyncTask is used to do heavy processing (HTTP request who took more time,DB operation ,  image processing ) in another thread (doInBackground) because if main thread do such thing its hold or stop the App UI execution ....
So better approach is do such things in another thread ,, many other thing also for doing Async is one of them
 */


/*

 */