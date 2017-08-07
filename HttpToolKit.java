
import android.telecom.Call;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by steven on 17-8-3.
 */

public class HttpToolKit {
    static void PostMe(String url, HashMap<String,String> form, HashMap<String,String> files, Callback callback)throws Exception{
        if (!url.startsWith("http://")){
            url="http://"+url;
        }
        String boundary = "xx--------------------------------------------------------------xx";
        MultipartBody.Builder builder = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM);
        if (files!=null&&files.size()>0){
            Iterator<Map.Entry<String, String>> it = files.entrySet().iterator();
            while (it.hasNext()){
                HashMap.Entry<String,String> entry=it.next();
                String[] strs=entry.getValue().split("/");
                String filename=strs[strs.length-1];
                builder.addFormDataPart(entry.getKey(),filename, RequestBody.create(MediaType.parse("application/octet-stream"),new File(entry.getValue())));
            }
        }
        if (form!=null&&form.size()>0){
            Iterator<HashMap.Entry<String,String>> it=form.entrySet().iterator();
            while (it.hasNext()){
                HashMap.Entry<String,String> entry=it.next();
                builder.addFormDataPart(entry.getKey(),entry.getValue());
            }
        }
        Request request = new Request.Builder().url(url).post(builder.build()).build();
        OkHttpClient client=new OkHttpClient();
        client.newCall(request).enqueue(callback);
    }
    static void GetMe(String url, Callback callback)throws Exception{
        Request request=new Request.Builder().url(url).build();
        OkHttpClient client=new OkHttpClient();
        client.newCall(request).enqueue(callback);
    }
    interface OnDownloadListener{
        void onDownloadFailed(Exception e);
        void onDownloading(int progress);
        void onDownloadSuccess();
    }
    static void DownloadFile(final String url, final String saveDir, final OnDownloadListener listener) {
        Request request = new Request.Builder().url(url).build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                // 下载失败
                listener.onDownloadFailed(e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    String[] strs=url.split("/");
                    String filename=strs[strs.length-1];
                    File file;
                    if (filename.contains("?")) {
                        file = new File(saveDir, filename.substring(0, filename.indexOf("?")));
                    }else {
                        file=new File(saveDir,filename);
                    }
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中
                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    // 下载完成
                    listener.onDownloadSuccess();
                } catch (Exception e) {
                    listener.onDownloadFailed(e);
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
    }
    static WebSocket newWebSocket(String url, WebSocketListener wsl){
        if (url.startsWith("http://"))
            url=url.substring(7);
        if (!url.startsWith("ws://"))
            url="ws://"+url;
        OkHttpClient client=new OkHttpClient();
        WebSocket wsc = client.newWebSocket(new Request.Builder().url(url).build(), wsl);
        return wsc;
    }
}
