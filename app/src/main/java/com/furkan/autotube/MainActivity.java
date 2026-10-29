package com.furkan.autotube;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int BG=0xFF080D18, PANEL=0xFF111A2B, MUTED=0xFFA5B1C8;
    private final Handler mainHandler=new Handler(Looper.getMainLooper());
    private final ExecutorService iconWorker=Executors.newFixedThreadPool(2);
    private LinearLayout shortcutStrip;
    private java.io.File pendingUpdateFile;
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}

    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);ShortcutStore.removeBuiltinShortcuts(this);buildScreen();}

    private void buildScreen(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(12),dp(16),dp(12),dp(24));scroll.addView(page);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);page.addView(top,lp(-1,-2));
        TextView logo=label("▶",20,0xFFFFFFFF,true);logo.setGravity(Gravity.CENTER);logo.setBackground(gradient(0xFF765BFF,0xFF22C7D6,18));top.addView(logo,lp(dp(50),dp(50)));
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setPadding(dp(12),0,0,0);brand.addView(label("AutoTube",21,0xFFF8FAFF,true));brand.addView(label("VİDEO • WEB • KEŞFET",10,0xFF9EAAC1,true));top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=label("BETA",10,0xFF8CEDE8,true);badge.setPadding(dp(11),dp(7),dp(11),dp(7));badge.setBackground(card(0xFF102C35,0xFF23616B,14));top.addView(badge);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(20),dp(22),dp(20),dp(20));hero.setBackground(gradient(0xFF172443,0xFF132E3C,24));LinearLayout.LayoutParams heroLp=lp(-1,-2);heroLp.topMargin=dp(28);page.addView(hero,heroLp);
        TextView eyebrow=label("YOLCULUĞUNUN EĞLENCE MERKEZİ",10,0xFF92E8E4,true);eyebrow.setLetterSpacing(.12f);hero.addView(eyebrow);
        TextView title=label("İzle. Keşfet.\nDilediğin yerde.",30,0xFFF8FAFF,true);title.setLineSpacing(dp(2),1f);LinearLayout.LayoutParams titleLp=lp(-1,-2);titleLp.topMargin=dp(12);hero.addView(title,titleLp);
        TextView desc=label("YouTube, web ve kendi favorilerin.",14,0xFFBAC7DC,false);LinearLayout.LayoutParams descLp=lp(-1,-2);descLp.topMargin=dp(9);hero.addView(desc,descLp);
        addSectionTitle(page,"Sabit kısayollar","YouTube ve web tarayıcı her zaman burada.");
        LinearLayout pinned=new LinearLayout(this);pinned.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams pinnedLp=lp(-1,dp(134));pinnedLp.topMargin=dp(14);page.addView(pinned,pinnedLp);
        LinearLayout.LayoutParams youtubeLp=new LinearLayout.LayoutParams(0,-1,1);youtubeLp.rightMargin=dp(10);pinned.addView(tile("▶","YouTube","Videoları aç",0xFF5C3152,0xFFB73155,v->openBrowser("https://m.youtube.com")),youtubeLp);
        pinned.addView(tile("◎","Web tarayıcı","Web'de gezin",0xFF173F52,0xFF168AAB,v->openBrowser("https://www.google.com")),new LinearLayout.LayoutParams(0,-1,1));

        addSectionTitle(page,"Favorilerim","Favori uygulama ve web sitelerini ekle.");
        HorizontalScrollView horizontal=new HorizontalScrollView(this);horizontal.setHorizontalScrollBarEnabled(false);horizontal.setClipToPadding(false);LinearLayout.LayoutParams hLp=lp(-1,dp(156));hLp.topMargin=dp(14);page.addView(horizontal,hLp);
        shortcutStrip=new LinearLayout(this);shortcutStrip.setOrientation(LinearLayout.HORIZONTAL);horizontal.addView(shortcutStrip,new HorizontalScrollView.LayoutParams(-2,-1));renderCustomShortcuts();

        LinearLayout tip=new LinearLayout(this);tip.setGravity(Gravity.CENTER_VERTICAL);tip.setPadding(dp(15),dp(14),dp(15),dp(14));tip.setBackground(card(PANEL,0xFF26344B,17));LinearLayout.LayoutParams tipLp=lp(-1,-2);tipLp.topMargin=dp(20);page.addView(tip,tipLp);
        TextView info=label("✦",19,0xFF9C8AFF,true);info.setGravity(Gravity.CENTER);info.setBackground(card(0xFF282241,0xFF40356A,14));tip.addView(info,lp(dp(38),dp(38)));
        LinearLayout tipCopy=new LinearLayout(this);tipCopy.setOrientation(LinearLayout.VERTICAL);tipCopy.setPadding(dp(12),0,0,0);tipCopy.addView(label("Kısayollarını düzenle",14,0xFFF4F6FC,true));TextView note=label("＋ ile ekle, ⋮ menüsünden sil veya düzenle, basılı tutup sürükleyerek sırala.",12,MUTED,false);note.setMaxLines(2);LinearLayout.LayoutParams noteLp=lp(-1,-2);noteLp.topMargin=dp(4);tipCopy.addView(note,noteLp);tip.addView(tipCopy,new LinearLayout.LayoutParams(0,-2,1));
        TextView foot=label("AUTOTUBE  ·  SENİN EKRANIN, SENİN SEÇİMİN",9,0xFF65728A,true);foot.setGravity(Gravity.CENTER);LinearLayout.LayoutParams footLp=lp(-1,-2);footLp.topMargin=dp(25);page.addView(foot,footLp);
        TextView update=actionButton("↻  Güncellemeleri kontrol et",0xFF151F32,0xFF33445E);LinearLayout.LayoutParams updateLp=lp(-1,dp(46));updateLp.topMargin=dp(16);page.addView(update,updateLp);update.setOnClickListener(v->checkForUpdates());setContentView(scroll);
    }

    private void addSectionTitle(LinearLayout page,String title,String desc){TextView t=label(title,18,0xFFF2F5FC,true);LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(24);page.addView(t,p);TextView s=label(desc,13,MUTED,false);LinearLayout.LayoutParams q=lp(-1,-2);q.topMargin=dp(5);page.addView(s,q);}
    private View tile(String icon,String title,String subtitle,int start,int accent,View.OnClickListener click){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(dp(16),dp(14),dp(12),dp(13));box.setBackground(gradient(start,0xFF111A2B,20));box.setClickable(true);box.setFocusable(true);TextView symbol=label(icon,20,0xFFFFFFFF,true);symbol.setGravity(Gravity.CENTER);symbol.setBackground(card(accent,accent,15));box.addView(symbol,lp(dp(42),dp(42)));TextView name=label(title,16,0xFFF8FAFF,true);LinearLayout.LayoutParams n=lp(-1,-2);n.topMargin=dp(12);box.addView(name,n);TextView sub=label(subtitle,12,0xFFB0BCD0,false);LinearLayout.LayoutParams s=lp(-1,-2);s.topMargin=dp(4);box.addView(sub,s);box.setOnClickListener(click);return box;}
    private TextView actionButton(String value,int bg,int stroke){TextView t=label(value,13,0xFFF4F7FF,true);t.setGravity(Gravity.CENTER);t.setBackground(card(bg,stroke,14));return t;}
    private void openBrowser(String url){Intent i=new Intent(this,BrowserActivity.class);i.putExtra(BrowserActivity.EXTRA_INITIAL_URL,url);startActivity(i);}
    private void openApp(String packageName){Intent launch=getPackageManager().getLaunchIntentForPackage(packageName);if(launch==null){Toast.makeText(this,"Bu uygulama açılamıyor.",Toast.LENGTH_SHORT).show();return;}try{startActivity(launch);}catch(Exception e){Toast.makeText(this,"Uygulama başlatılamadı.",Toast.LENGTH_SHORT).show();}}

    private void showAddWebsite(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(20),dp(8),dp(20),dp(2));
        EditText name=new EditText(this);name.setSingleLine(true);name.setTextColor(0xFFF5F8FF);name.setHintTextColor(0xFF8D9AB0);name.setHint("Kısayol adı (ör. Haberler)");name.setBackground(card(0xFF101D30,0xFF2A4562,12));name.setPadding(dp(13),dp(10),dp(13),dp(10));form.addView(name,lp(-1,dp(48)));
        EditText url=new EditText(this);url.setSingleLine(true);url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);url.setTextColor(0xFFF5F8FF);url.setHintTextColor(0xFF8D9AB0);url.setHint("https://site.com");url.setBackground(card(0xFF101D30,0xFF2A4562,12));url.setPadding(dp(13),dp(10),dp(13),dp(10));LinearLayout.LayoutParams ulp=lp(-1,dp(48));ulp.topMargin=dp(10);form.addView(url,ulp);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Web sitesi ekle").setView(form).setNegativeButton("Vazgeç",null).setPositiveButton("Ekle",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String normalized=ShortcutStore.normalizeUrl(url.getText().toString());String n=name.getText().toString().trim();if(n.isEmpty()&& !normalized.isEmpty()){String host=Uri.parse(normalized).getHost();n=host==null?"Web sitesi":host;}if(normalized.isEmpty()){url.setError("Geçerli bir web adresi gir.");return;}ShortcutStore.add(this,n,normalized,false);dialog.dismiss();renderCustomShortcuts();}));dialog.show();
    }

    private void showAddShortcutOptions(){
        new AlertDialog.Builder(this).setTitle("Favori ekle").setItems(new String[]{"Telefondan uygulama seç","Web sitesi ekle"},(d,which)->{
            if(which==0){showInstalledApps();return;}
            showAddWebsite();
        }).setNegativeButton("Kapat",null).show();
    }

    private void showInstalledApps(){
        Intent query=new Intent(Intent.ACTION_MAIN);query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> matches=getPackageManager().queryIntentActivities(query,0);
        LinkedHashMap<String,ResolveInfo> unique=new LinkedHashMap<>();
        for(ResolveInfo info:matches){if(info.activityInfo==null)continue;String packageName=info.activityInfo.packageName;if(!getPackageName().equals(packageName))unique.putIfAbsent(packageName,info);}
        ArrayList<ResolveInfo> apps=new ArrayList<>(unique.values());
        Collections.sort(apps,(a,b)->String.CASE_INSENSITIVE_ORDER.compare(String.valueOf(a.loadLabel(getPackageManager())),String.valueOf(b.loadLabel(getPackageManager()))));
        if(apps.isEmpty()){new AlertDialog.Builder(this).setMessage("Seçilebilecek bir uygulama bulunamadı.").setPositiveButton("Tamam",null).show();return;}
        String[] labels=new String[apps.size()];for(int i=0;i<apps.size();i++)labels[i]=String.valueOf(apps.get(i).loadLabel(getPackageManager()));
        new AlertDialog.Builder(this).setTitle("Favori uygulama seç").setItems(labels,(dialog,which)->{ResolveInfo selected=apps.get(which);String packageName=selected.activityInfo.packageName;ShortcutStore.addApp(this,packageName,labels[which]);renderCustomShortcuts();}).setNegativeButton("Vazgeç",null).show();
    }

    private void checkForUpdates(){Toast.makeText(this,"GitHub kontrol ediliyor…",Toast.LENGTH_SHORT).show();GithubUpdater.check(BuildConfig.VERSION_NAME,(release,error)->{if(isFinishing())return;if(error!=null){new AlertDialog.Builder(this).setMessage(error).setPositiveButton("Tamam",null).show();return;}if(release==null){new AlertDialog.Builder(this).setMessage("AutoTube güncel. ("+BuildConfig.VERSION_NAME+")").setPositiveButton("Tamam",null).show();return;}
        new AlertDialog.Builder(this).setTitle("Güncelleme var: "+release.version).setMessage(release.notes.isEmpty()?"Yeni sürümü indirip kurmak ister misin?":release.notes+"\n\nYeni sürümü indirip kurmak ister misin?").setNegativeButton("Daha sonra",null).setPositiveButton("İndir",(dialog,which)->downloadUpdate(release)).show();});}

    private void downloadUpdate(GithubUpdater.Release release){
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(22),dp(10),dp(22),dp(8));
        TextView details=label("0%  •  0.0 MB / 0.0 MB",14,0xFFE8EDF7,true);details.setGravity(Gravity.CENTER_HORIZONTAL);content.addView(details,lp(-1,-2));
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setIndeterminate(true);LinearLayout.LayoutParams barLp=lp(-1,dp(8));barLp.topMargin=dp(14);content.addView(bar,barLp);
        AlertDialog progress=new AlertDialog.Builder(this).setTitle("Güncelleme").setView(content).create();progress.setCancelable(false);progress.show();
        GithubUpdater.download(this,release,new GithubUpdater.DownloadCallback(){
            @Override public void onProgress(long downloadedBytes,long totalBytes){
                if(!progress.isShowing())return;
                if(totalBytes>0){int percent=(int)Math.min(100,(downloadedBytes*100L)/totalBytes);bar.setIndeterminate(false);bar.setProgress(percent);details.setText(String.format(Locale.getDefault(),"%d%%  •  %s / %s",percent,megabytes(downloadedBytes),megabytes(totalBytes)));}
                else{bar.setIndeterminate(true);details.setText(megabytes(downloadedBytes));}
            }
            @Override public void onResult(java.io.File file,String error){
                if(isFinishing())return;progress.dismiss();if(error!=null){new AlertDialog.Builder(MainActivity.this).setMessage(error).setPositiveButton("Tamam",null).show();return;}pendingUpdateFile=file;continueInstall();
            }
        });
    }

    private String megabytes(long bytes){return String.format(Locale.getDefault(),"%.1f MB",bytes/1_000_000.0);}

    private void continueInstall(){if(pendingUpdateFile==null||!pendingUpdateFile.exists())return;if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));startActivity(settings);Toast.makeText(this,"Kuruluma devam etmek için AutoTube'a izin ver.",Toast.LENGTH_LONG).show();return;}installApk(pendingUpdateFile);}

    private void installApk(java.io.File file){try{android.content.pm.PackageManager pm=getPackageManager();android.content.pm.PackageInfo info=pm.getPackageArchiveInfo(file.getAbsolutePath(),0);if(info==null||!getPackageName().equals(info.packageName))throw new Exception("İndirilen dosya AutoTube güncellemesi değil.");android.net.Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);Intent install=new Intent(Intent.ACTION_VIEW);install.setDataAndType(uri,"application/vnd.android.package-archive");install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(install);pendingUpdateFile=null;}catch(Exception e){new AlertDialog.Builder(this).setMessage(e.getMessage()==null?"Kurulum başlatılamadı.":e.getMessage()).setPositiveButton("Tamam",null).show();}}

    @Override protected void onResume(){super.onResume();if(pendingUpdateFile!=null&&pendingUpdateFile.exists()&&(Build.VERSION.SDK_INT<26||getPackageManager().canRequestPackageInstalls()))installApk(pendingUpdateFile);}

    private void showEditWebsite(ShortcutStore.Item item){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(20),dp(8),dp(20),dp(2));
        EditText name=new EditText(this);name.setSingleLine(true);name.setTextColor(0xFFF5F8FF);name.setHintTextColor(0xFF8D9AB0);name.setHint("Kısayol adı");name.setText(item.name);name.setBackground(card(0xFF101D30,0xFF2A4562,12));name.setPadding(dp(13),dp(10),dp(13),dp(10));form.addView(name,lp(-1,dp(48)));
        EditText url=new EditText(this);url.setSingleLine(true);url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);url.setTextColor(0xFFF5F8FF);url.setHintTextColor(0xFF8D9AB0);url.setHint("https://site.com");url.setText(item.value);url.setBackground(card(0xFF101D30,0xFF2A4562,12));url.setPadding(dp(13),dp(10),dp(13),dp(10));LinearLayout.LayoutParams ulp=lp(-1,dp(48));ulp.topMargin=dp(10);form.addView(url,ulp);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Kısayolu düzenle").setView(form).setNegativeButton("Vazgeç",null).setPositiveButton("Kaydet",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String normalized=ShortcutStore.normalizeUrl(url.getText().toString());String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Bir ad gir.");return;}if(normalized.isEmpty()){url.setError("Geçerli bir web adresi gir.");return;}ShortcutStore.update(this,item.id,n,normalized);dialog.dismiss();renderCustomShortcuts();}));dialog.show();
    }

    private void showEditAppName(ShortcutStore.Item item){
        EditText name=new EditText(this);name.setSingleLine(true);name.setText(item.name);name.setSelectAllOnFocus(true);name.setTextColor(0xFFF5F8FF);name.setHintTextColor(0xFF8D9AB0);name.setPadding(dp(14),dp(10),dp(14),dp(10));name.setBackground(card(0xFF101D30,0xFF2A4562,12));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Uygulama adını düzenle").setView(name).setNegativeButton("Vazgeç",null).setPositiveButton("Kaydet",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Bir ad gir.");return;}ShortcutStore.update(this,item.id,n,item.value);dialog.dismiss();renderCustomShortcuts();}));dialog.show();
    }

    private void showShortcutMenu(View anchor,ShortcutStore.Item item){
        PopupMenu menu=new PopupMenu(this,anchor);menu.getMenu().add("Düzenle");menu.getMenu().add("Sil");
        menu.setOnMenuItemClickListener(option->{if("Düzenle".contentEquals(option.getTitle())){if(item.app)showEditAppName(item);else showEditWebsite(item);return true;}
            new AlertDialog.Builder(this).setTitle("Kısayolu sil?").setMessage(item.name+" ana ekrandan kaldırılacak.").setNegativeButton("Vazgeç",null).setPositiveButton("Sil",(d,w)->{ShortcutStore.remove(this,item.id);renderCustomShortcuts();}).show();return true;});menu.show();
    }

    private void renderCustomShortcuts(){if(shortcutStrip==null)return;shortcutStrip.removeAllViews();List<ShortcutStore.Item> items=ShortcutStore.read(this);for(ShortcutStore.Item item:items)addShortcutTile(item,items);addAddShortcutTile();}

    private void addShortcutTile(ShortcutStore.Item item,List<ShortcutStore.Item> items){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(11),dp(12),dp(10));box.setBackground(card(PANEL,0xFF26344B,17));LinearLayout.LayoutParams boxLp=lp(dp(132),-1);boxLp.rightMargin=dp(10);shortcutStrip.addView(box,boxLp);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);box.addView(header,lp(-1,dp(42)));ImageView icon=new ImageView(this);icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);icon.setPadding(dp(5),dp(5),dp(5),dp(5));icon.setBackground(card(0xFF17323A,0xFF28616A,13));header.addView(icon,lp(dp(42),dp(42)));
        header.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));TextView more=label("⋮",24,0xFFABB7CA,true);more.setGravity(Gravity.CENTER);header.addView(more,lp(dp(28),dp(36)));more.setContentDescription("Kısayol seçenekleri");more.setOnClickListener(v->showShortcutMenu(more,item));
        TextView title=label(item.name,13,0xFFF5F7FC,true);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams tlp=lp(-1,-2);tlp.topMargin=dp(10);box.addView(title,tlp);
        TextView hint=label("Açmak için dokun",10,0xFF8998AE,false);LinearLayout.LayoutParams hlp=lp(-1,-2);hlp.topMargin=dp(4);box.addView(hint,hlp);
        box.setClickable(true);box.setOnClickListener(v->{if(item.app)openApp(item.value);else openBrowser(item.value);});
        box.setOnLongClickListener(v->{ClipData data=ClipData.newPlainText("shortcut",item.id);v.startDragAndDrop(data,new View.DragShadowBuilder(v),null,0);return true;});
        box.setOnDragListener((v,event)->{if(event.getAction()==android.view.DragEvent.ACTION_DROP){String dragged=event.getClipData().getItemAt(0).getText().toString();int target=0;for(int i=0;i<items.size();i++)if(items.get(i).id.equals(item.id)){target=i;break;}ShortcutStore.move(this,dragged,target);renderCustomShortcuts();return true;}return true;});
        if(item.app){try{icon.setImageDrawable(getPackageManager().getApplicationIcon(item.value));}catch(Exception ignored){icon.setImageResource(android.R.drawable.ic_menu_view);}}
        else{icon.setImageResource(android.R.drawable.ic_menu_view);iconWorker.execute(()->{Bitmap bitmap=fetchIcon(item.value);if(bitmap!=null)mainHandler.post(()->{if(!isFinishing()&&icon.getTag()==null){icon.setImageBitmap(bitmap);icon.setTag(item.value);}});});}
    }

    private void addAddShortcutTile(){LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(10),dp(10),dp(10),dp(10));tile.setBackground(card(0xFF121B2D,0xFF394461,17));LinearLayout.LayoutParams p=lp(dp(112),-1);p.rightMargin=dp(4);shortcutStrip.addView(tile,p);TextView plus=label("＋",29,0xFF9D9BFF,true);plus.setGravity(Gravity.CENTER);plus.setBackground(card(0xFF252744,0xFF514B83,18));tile.addView(plus,lp(dp(46),dp(46)));TextView text=label("Kısayol ekle",11,0xFFDCE2F1,true);text.setGravity(Gravity.CENTER);LinearLayout.LayoutParams tp=lp(-1,-2);tp.topMargin=dp(11);tile.addView(text,tp);tile.setOnClickListener(v->showAddShortcutOptions());}

    private Bitmap fetchIcon(String site){HttpURLConnection connection=null;try{String favicon="https://www.google.com/s2/favicons?domain_url="+Uri.encode(site)+"&sz=128";connection=(HttpURLConnection)new URL(favicon).openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(5000);connection.setInstanceFollowRedirects(true);connection.setRequestProperty("User-Agent","AutoTube Android");connection.connect();if(connection.getResponseCode()!=200)return null;try(java.io.InputStream in=connection.getInputStream()){return BitmapFactory.decodeStream(in);}}catch(Exception ignored){return null;}finally{if(connection!=null)connection.disconnect();}}

    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private GradientDrawable card(int color,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;}
    private GradientDrawable gradient(int a,int b,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));return d;}
    @Override protected void onDestroy(){super.onDestroy();if(isFinishing())iconWorker.shutdownNow();}
}
