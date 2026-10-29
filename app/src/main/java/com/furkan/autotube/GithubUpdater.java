package com.furkan.autotube;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class GithubUpdater {
    private static final String RELEASE_URL="https://api.github.com/repos/fidanci/AUTOTUBE/releases/latest";
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private GithubUpdater() {}

    interface CheckCallback { void onResult(Release release, String error); }
    interface DownloadCallback {
        void onProgress(long downloadedBytes,long totalBytes);
        void onResult(File file, String error);
    }

    static final class Release {
        final String version, downloadUrl, notes;
        Release(String version,String downloadUrl,String notes){this.version=version;this.downloadUrl=downloadUrl;this.notes=notes;}
    }

    static void check(String installedVersion,CheckCallback callback){
        WORKER.execute(()->{Release release=null;String error=null;HttpURLConnection c=null;
            try{c=(HttpURLConnection)new URL(RELEASE_URL).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","AutoTube-Android");int status=c.getResponseCode();if(status==404)throw new Exception("Henüz GitHub'da bir güncelleme yayımlanmamış.");if(status<200||status>=300)throw new Exception("GitHub'a ulaşılamadı ("+status+").");
                JSONObject json=new JSONObject(new String(readAll(c.getInputStream()),java.nio.charset.StandardCharsets.UTF_8));String tag=json.optString("tag_name","").replaceFirst("^[vV]","");String url=null;JSONArray assets=json.optJSONArray("assets");if(assets!=null)for(int i=0;i<assets.length();i++){JSONObject a=assets.optJSONObject(i);if(a!=null&&"AutoTube.apk".equals(a.optString("name"))){url=a.optString("browser_download_url",null);break;}}
                if(tag.isEmpty()||url==null)throw new Exception("GitHub sürümünde AutoTube.apk dosyası bulunamadı.");if(compareVersions(tag,installedVersion)<=0)release=null;else release=new Release(tag,url,json.optString("body",""));
            }catch(Exception e){error=e.getMessage()==null?"Güncelleme kontrol edilemedi.":e.getMessage();}finally{if(c!=null)c.disconnect();}Release result=release;String message=error;MAIN.post(()->callback.onResult(result,message));
        });
    }

    static void download(Context context,Release release,DownloadCallback callback){
        WORKER.execute(()->{File result=null;String error=null;HttpURLConnection c=null;
            try{File directory=new File(context.getCacheDir(),"updates");if(!directory.exists()&&!directory.mkdirs())throw new Exception("Güncelleme dosyası için klasör açılamadı.");File target=new File(directory,"AutoTube.apk");c=(HttpURLConnection)new URL(release.downloadUrl).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","AutoTube-Android");int status=c.getResponseCode();if(status<200||status>=300)throw new Exception("APK indirilemedi ("+status+"), tekrar dene.");
                long totalBytes=c.getContentLengthLong();
                long downloadedBytes=0,lastReportedAt=0;
                MAIN.post(()->callback.onProgress(0,totalBytes));
                try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(target)){
                    byte[] buffer=new byte[16384];int count;
                    while((count=in.read(buffer))!=-1){
                        out.write(buffer,0,count);downloadedBytes+=count;
                        long now=SystemClock.elapsedRealtime();
                        if(now-lastReportedAt>=200||(totalBytes>0&&downloadedBytes>=totalBytes)){
                            long current=downloadedBytes,size=totalBytes;
                            MAIN.post(()->callback.onProgress(current,size));lastReportedAt=now;
                        }
                    }
                    out.getFD().sync();
                }
                long completedBytes=downloadedBytes,size=totalBytes;MAIN.post(()->callback.onProgress(completedBytes,size));
                if(target.length()<100000)throw new Exception("İndirilen APK dosyası eksik görünüyor.");result=target;
            }catch(Exception e){error=e.getMessage()==null?"İndirme başarısız oldu.":e.getMessage();}finally{if(c!=null)c.disconnect();}File file=result;String message=error;MAIN.post(()->callback.onResult(file,message));
        });
    }

    private static byte[] readAll(InputStream input)throws Exception{try(InputStream in=input;java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private static int compareVersions(String a,String b){String[] aa=a.split("\\."),bb=b.split("\\.");for(int i=0;i<Math.max(aa.length,bb.length);i++){long x=i<aa.length?number(aa[i]):0,y=i<bb.length?number(bb[i]):0;if(x!=y)return Long.compare(x,y);}return 0;}
    private static long number(String value){try{return Long.parseLong(value.replaceAll("[^0-9].*$",""));}catch(Exception ignored){return 0;}}
}
