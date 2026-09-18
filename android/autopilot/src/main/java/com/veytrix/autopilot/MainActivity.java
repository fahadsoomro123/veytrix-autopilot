package com.veytrix.autopilot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * VEYTRIX native Android application UI.
 * Pure native Views + Canvas; no WebView/HTML.
 */
public final class MainActivity extends Activity {

    private static final int WHITE = Color.rgb(255, 255, 255);
    private static final int INK = Color.rgb(16, 34, 59);
    private static final int MUTED = Color.rgb(104, 125, 151);
    private static final int BORDER = Color.rgb(211, 224, 240);
    private static final int NAVY = Color.rgb(8, 23, 42);
    private static final int NAVY_2 = Color.rgb(12, 32, 55);
    private static final int BLUE = Color.rgb(55, 132, 255);
    private static final int PURPLE = Color.rgb(126, 84, 239);
    private static final int GREEN = Color.rgb(34, 190, 145);
    private static final int RED = Color.rgb(226, 76, 97);

    private FrameLayout root;
    private FrameLayout pageHost;
    private LinearLayout bottomNav;
    private FrameLayout drawerShade;
    private LinearLayout drawer;
    private String mission = "";
    private boolean deepMode = true;
    private VeytrixAutopilotClient autopilotClient;
    private TextView activeRunStatusView;
    private TextView activeRunMetaView;
    private Button activeRunOpenButton;
    private String activeRunUrl = "";
    private long activeRunId = 0L;
    private boolean voiceListening = false;

    private boolean autoExecute = true;
    private boolean autoTest = true;
    private boolean parallelAgents = true;
    private boolean notifications = true;
    private String primaryModel = "GPT-4o";
    private int fallbackModels = 3;
    private String maxExecutionTime = "30 minutes";

    private int currentPage = 0; // 0 Home, 1 Activity, 2 Results, 3 Control, 4 More

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        autopilotClient = new VeytrixAutopilotClient(this);
        configureWindow();
        buildShell();
        showHome();
    }

    private void configureWindow() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(WHITE);
        window.setNavigationBarColor(WHITE);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void buildShell() {
        root = new FrameLayout(this);
        root.setBackgroundColor(WHITE);

        pageHost = new FrameLayout(this);
        pageHost.setPadding(dp(14), dp(6), dp(14), dp(76));
        root.addView(pageHost, full());

        bottomNav = buildBottomNav();
        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(-1, dp(64), Gravity.BOTTOM);
        navLp.leftMargin = dp(8); navLp.rightMargin = dp(8); navLp.bottomMargin = dp(8);
        root.addView(bottomNav, navLp);

        buildDrawer();
        setContentView(root);
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(7), dp(5), dp(7), dp(5));
        nav.setBackground(roundDrawable(NAVY, 20, BORDER));
        String[] labels = {"Home", "Activity", "Results", "Control"};
        String[] icons = {"home", "activity", "results", "control"};
        for (int i = 0; i < labels.length; i++) {
            final int page = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            IconView icon = new IconView(this, icons[i]);
            TextView text = text(labels[i], 9, WHITE, true);
            text.setGravity(Gravity.CENTER);
            item.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
            item.addView(text, new LinearLayout.LayoutParams(-1, dp(17)));
            item.setOnClickListener(v -> navigate(page));
            nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
        return nav;
    }

    private void buildDrawer() {
        drawerShade = new FrameLayout(this);
        drawerShade.setBackgroundColor(Color.argb(105, 5, 16, 28));
        drawerShade.setVisibility(View.GONE);
        drawerShade.setOnClickListener(v -> closeDrawer());
        root.addView(drawerShade, full());

        drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setPadding(dp(16), dp(18), dp(16), dp(16));
        drawer.setBackgroundColor(WHITE);
        drawer.setElevation(dp(12));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(310), -1, Gravity.LEFT);
        lp.topMargin = dp(12); lp.bottomMargin = dp(12);
        root.addView(drawer, lp);
        drawer.setVisibility(View.GONE);
        rebuildDrawer();
    }

    private void rebuildDrawer() {
        drawer.removeAllViews();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        IconView mark = new IconView(this, "logo");
        header.addView(mark, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout brandCol = new LinearLayout(this);
        brandCol.setOrientation(LinearLayout.VERTICAL);
        brandCol.setPadding(dp(10), 0, 0, 0);
        brandCol.addView(text("VEYTRIX", 18, INK, true));
        brandCol.addView(text("AUTONOMOUS AI DEVELOPMENT", 8, MUTED, false));
        header.addView(brandCol, new LinearLayout.LayoutParams(0, -2, 1));
        IconView close = new IconView(this, "close");
        close.setOnClickListener(v -> closeDrawer());
        header.addView(close, new LinearLayout.LayoutParams(dp(32), dp(32)));
        drawer.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));
        drawer.addView(text("WORKSPACE", 9, MUTED, true), new LinearLayout.LayoutParams(-1, dp(28)));
        drawerItem("Home", "home", 0); drawerItem("Activity", "activity", 1);
        drawerItem("Results", "results", 2); drawerItem("Control", "control", 3);
        drawerItem("Profile", "profile", 4); drawerItem("Voice", "voice", 5); drawerItem("Settings", "settings", 6);
        Space spacer = new Space(this); drawer.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        drawer.addView(text("SUPPORT", 9, MUTED, true), new LinearLayout.LayoutParams(-1, dp(26)));
        drawerItem("Help & Support", "support", 7); drawerItem("Completed", "check", 8);
        LinearLayout account = new LinearLayout(this); account.setGravity(Gravity.CENTER_VERTICAL);
        account.setPadding(dp(10), dp(8), dp(10), dp(8)); account.setBackground(roundDrawable(Color.rgb(244,248,253), 14, BORDER));
        IconView avatar = new IconView(this, "profile"); account.addView(avatar, new LinearLayout.LayoutParams(dp(30), dp(30)));
        LinearLayout acc = new LinearLayout(this); acc.setOrientation(LinearLayout.VERTICAL); acc.setPadding(dp(10),0,0,0);
        acc.addView(text("Fahad Hussain",10,INK,true)); acc.addView(text("Pro User",8,PURPLE,true)); account.addView(acc);
        drawer.addView(account, new LinearLayout.LayoutParams(-1, dp(56)));
    }

    private void drawerItem(String label, String icon, int destination) {
        LinearLayout item = new LinearLayout(this); item.setGravity(Gravity.CENTER_VERTICAL); item.setPadding(dp(8),0,dp(8),0);
        item.addView(new IconView(this, icon), new LinearLayout.LayoutParams(dp(26),dp(26)));
        TextView labelView=text(label,11,INK,true); labelView.setPadding(dp(8),0,0,0);
        item.addView(labelView,new LinearLayout.LayoutParams(0,-1,1));
        item.setOnClickListener(v->{closeDrawer(); switch(destination){case 0:navigate(0);break;case 1:navigate(1);break;case 2:navigate(2);break;case 3:navigate(3);break;case 4:showProfile();break;case 5:showVoice();break;case 6:showSettings();break;case 8:showCompleted();break;default:toast("Support is ready for connection");}});
        drawer.addView(item,new LinearLayout.LayoutParams(-1,dp(44)));
    }

    private void openDrawer(){rebuildDrawer();drawerShade.setVisibility(View.VISIBLE);drawer.setVisibility(View.VISIBLE);}
    private void closeDrawer(){drawerShade.setVisibility(View.GONE);drawer.setVisibility(View.GONE);}

    private void navigate(int page){currentPage=page;if(page==0)showHome();else if(page==1)showActivity();else if(page==2)showResults();else if(page==3)showControl();else showMore();}
    private void clearPage(){closeDrawer();pageHost.removeAllViews();pageHost.setPadding(dp(14),dp(6),dp(14),dp(76));}

    private void showHome(){clearPage();pageHost.addView(new HomeView(this),full());}

    private void showActivity(){
        clearPage(); LinearLayout col=pageColumn(); col.addView(topBar("Activity"),wrap());
        col.addView(text("Track, monitor and manage your AI missions.",11,MUTED,false),marginBottom(9));
        col.addView(filterRow(),wrap());
        addMissionCard(col,"E-commerce API","Building scalable backend services","67%","Running",GREEN);
        addMissionCard(col,"Modern UI Components","Generating 24 components","100%","Completed",GREEN);
        addMissionCard(col,"Database Optimization","Analyzing and optimizing","0%","Queued",MUTED);
        addMissionCard(col,"Mobile App Setup","Setting up native Android project","0%","Failed",RED);
        addMissionCard(col,"AI Chat Integration","Integrating AI capabilities","100%","Completed",GREEN);
        pageHost.addView(scrollWrap(col),full());
    }

    private HorizontalScrollView filterRow(){
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        String[] fs={"All","Running","Completed","Failed"};for(String f:fs){TextView chip=text(f,9,f.equals("All")?WHITE:MUTED,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(13),0,dp(13),0);chip.setBackground(roundDrawable(f.equals("All")?BLUE:Color.rgb(242,246,252),12,f.equals("All")?BLUE:BORDER));chip.setOnClickListener(v->toast(f+" filter selected"));row.addView(chip,new LinearLayout.LayoutParams(-2,dp(32)));space(row,6,1);}hs.addView(row);return hs;
    }

    private void addMissionCard(LinearLayout col,String title,String desc,String progress,String status,int color){
        LinearLayout card=cardColumn();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(new IconView(this,status.equals("Failed")?"alert":"mission"),new LinearLayout.LayoutParams(dp(32),dp(32)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(10),0,dp(8),0);info.addView(text(title,11,INK,true));info.addView(text(desc,8,MUTED,false));row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        TextView st=text(status,8,color,true);st.setGravity(Gravity.CENTER);st.setPadding(dp(7),dp(5),dp(7),dp(5));st.setBackground(roundDrawable(Color.argb(24,Color.red(color),Color.green(color),Color.blue(color)),9,Color.TRANSPARENT));row.addView(st,new LinearLayout.LayoutParams(-2,dp(27)));card.addView(row);
        ProgressView bar=new ProgressView(this,progress,BLUE);LinearLayout.LayoutParams barLp=new LinearLayout.LayoutParams(-1,dp(5));barLp.topMargin=dp(8);card.addView(bar,barLp);card.setOnClickListener(v->showDetails(title,desc,status));col.addView(card,marginBottom(7));
    }

    private void showResults(){
        clearPage();LinearLayout col=pageColumn();col.addView(topBar("Results"),wrap());col.addView(text("Generated files, builds and verified artifacts.",11,MUTED,false),marginBottom(9));
        col.addView(filterRowResults(),wrap());
        addResult(col,"api-server.js","Build Artifact · 12.4 MB","code");addResult(col,"database.sql","SQL File · 2.1 MB","database");addResult(col,"README.md","Documentation · 8.2 KB","document");addResult(col,"app-release.apk","Android Build · 24.8 MB","android");addResult(col,"test-report.html","Test Report · 1.4 MB","report");addResult(col,"components.zip","UI Components · 5.6 MB","archive");
        pageHost.addView(scrollWrap(col),full());
    }

    private HorizontalScrollView filterRowResults(){
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);String[] fs={"All","Code","Builds","Docs","Other"};
        for(String f:fs){TextView chip=text(f,9,f.equals("All")?WHITE:MUTED,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(13),0,dp(13),0);chip.setBackground(roundDrawable(f.equals("All")?BLUE:Color.rgb(242,246,252),12,f.equals("All")?BLUE:BORDER));chip.setOnClickListener(v->toast(f+" filter selected"));row.addView(chip,new LinearLayout.LayoutParams(-2,dp(32)));space(row,6,1);}hs.addView(row);return hs;
    }

    private void addResult(LinearLayout col,String name,String meta,String type){
        LinearLayout row=cardRow();row.addView(new IconView(this,type),new LinearLayout.LayoutParams(dp(38),dp(38)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(10),0,dp(8),0);info.addView(text(name,11,INK,true));info.addView(text(meta,8,MUTED,false));row.addView(info,new LinearLayout.LayoutParams(0,-2,1));TextView act=text("↓",13,BLUE,true);act.setGravity(Gravity.CENTER);act.setBackground(roundDrawable(Color.rgb(239,246,255),10,BORDER));act.setOnClickListener(v->toast("Action ready: "+name));row.addView(act,new LinearLayout.LayoutParams(dp(32),dp(32)));col.addView(row,marginBottom(7));
    }

    private void showControl(){
        clearPage();LinearLayout col=pageColumn();col.addView(topBar("Control Center"),wrap());col.addView(text("Configure and control your AI environment.",10,MUTED,false),marginBottom(4));
        col.addView(text("Mission Control",9,INK,true),margin(0,4,0,3));LinearLayout missionCard=cardColumn();missionCard.addView(liveRow("Deep Mode","Reasoning and planning","deep",deepMode,"spark"),rowHeight());missionCard.addView(liveRow("Auto Execute","Automatic action execution","auto",autoExecute,"execute"),rowHeight());missionCard.addView(liveRow("Auto Test","Verify generated changes","test",autoTest,"test"),rowHeight());col.addView(missionCard,marginBottom(6));
        col.addView(text("AI Models",9,INK,true),margin(0,2,0,3));LinearLayout ai=cardColumn();ai.addView(clickRow("Primary Model",primaryModel,"model","primary"),rowHeight());ai.addView(clickRow("Fallback Models",fallbackModels+" configured","fallback","fallback"),rowHeight());col.addView(ai,marginBottom(6));
        col.addView(text("Execution",9,INK,true),margin(0,2,0,3));LinearLayout exec=cardColumn();exec.addView(clickRow("Max Execution Time",maxExecutionTime,"clock","time"),rowHeight());exec.addView(liveRow("Parallel Agents","Run compatible tasks together","parallel",parallelAgents,"agents"),rowHeight());col.addView(exec,marginBottom(6));
        col.addView(text("Alerts",9,INK,true),margin(0,2,0,3));LinearLayout alerts=cardColumn();alerts.addView(liveRow("Notifications","Updates and mission alerts","notifications",notifications,"bell"),rowHeight());col.addView(alerts);
        pageHost.addView(col,full());
    }

    private ViewGroup.LayoutParams rowHeight(){return new LinearLayout.LayoutParams(-1,dp(44));}

    private View liveRow(String title,String subtitle,String key,boolean on,String icon){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(4),dp(10),dp(4));row.addView(new IconView(this,icon),new LinearLayout.LayoutParams(dp(28),dp(28)));
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(dp(9),0,dp(8),0);copy.addView(text(title,9,INK,true));copy.addView(text(subtitle,7,MUTED,false));row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        ToggleView toggle=new ToggleView(this,on);toggle.setOnClickListener(v->setToggle(key,!toggle.isOn()));row.addView(toggle,new LinearLayout.LayoutParams(dp(34),dp(20)));row.setOnClickListener(v->setToggle(key,!toggle.isOn()));return row;
    }

    private void setToggle(String key,boolean value){switch(key){case "deep":deepMode=value;break;case "auto":autoExecute=value;break;case "test":autoTest=value;break;case "parallel":parallelAgents=value;break;case "notifications":notifications=value;break;}showControl();toast(key+" "+(value?"enabled":"disabled"));}

    private View clickRow(String title,String subtitle,String icon,String action){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(4),dp(10),dp(4));row.addView(new IconView(this,icon),new LinearLayout.LayoutParams(dp(28),dp(28)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(dp(9),0,dp(8),0);copy.addView(text(title,9,INK,true));copy.addView(text(subtitle,7,MUTED,false));row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=text("›",15,MUTED,true);row.addView(arrow,new LinearLayout.LayoutParams(dp(18),-1));View.OnClickListener listener=v->{if("primary".equals(action))choosePrimary();else if("fallback".equals(action))chooseFallback();else chooseTime();};row.setOnClickListener(listener);arrow.setOnClickListener(listener);return row;
    }

    private void choosePrimary(){String[] options={"GPT-4o","Claude 3.5 Sonnet","Gemini 2.5 Pro","Qwen 2.5 Coder"};new AlertDialog.Builder(this).setTitle("Primary Model").setSingleChoiceItems(options,indexOf(options,primaryModel),(d,which)->{primaryModel=options[which];d.dismiss();showControl();toast("Primary model updated");}).show();}
    private void chooseFallback(){String[] options={"1 fallback","2 fallbacks","3 fallbacks","4 fallbacks"};new AlertDialog.Builder(this).setTitle("Fallback Models").setSingleChoiceItems(options,fallbackModels-1,(d,which)->{fallbackModels=which+1;d.dismiss();showControl();toast("Fallback depth updated");}).show();}
    private void chooseTime(){String[] options={"10 minutes","30 minutes","60 minutes"};new AlertDialog.Builder(this).setTitle("Maximum Execution Time").setSingleChoiceItems(options,indexOf(options,maxExecutionTime),(d,which)->{maxExecutionTime=options[which];d.dismiss();showControl();toast("Execution time updated");}).show();}
    private int indexOf(String[] arr,String value){for(int i=0;i<arr.length;i++)if(arr[i].equals(value))return i;return 0;}

    private void showMore(){
        clearPage();LinearLayout col=pageColumn();col.addView(topBar("More"),wrap());col.addView(text("Account, preferences and system options.",10,MUTED,false),marginBottom(7));
        LinearLayout profile=cardColumn();profile.setGravity(Gravity.CENTER_HORIZONTAL);profile.setPadding(dp(16),dp(14),dp(16),dp(14));profile.addView(new IconView(this,"profileLarge"),new LinearLayout.LayoutParams(dp(72),dp(72)));profile.addView(text("Fahad Hussain",18,INK,true),wrap());profile.addView(text("@fahadsoomro123",9,MUTED,false),wrap());TextView tag=text("Pro User",8,PURPLE,true);tag.setPadding(dp(8),dp(5),dp(8),dp(5));tag.setBackground(roundDrawable(Color.rgb(244,239,255),10,Color.rgb(221,207,251)));profile.addView(tag,marginTop(6));col.addView(profile,marginBottom(8));
        LinearLayout menu=cardColumn();menu.addView(actionListRow("Profile","Account and mission stats","profile",v->showProfile()),rowHeight());menu.addView(actionListRow("Settings","Theme, AI and notifications","settings",v->showSettings()),rowHeight());menu.addView(actionListRow("Voice Command","Speak a mission","voice",v->showVoice()),rowHeight());menu.addView(actionListRow("Completed","Recent completed missions","check",v->showCompleted()),rowHeight());col.addView(menu);pageHost.addView(col,full());
    }

    private View actionListRow(String title,String sub,String icon,View.OnClickListener action){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(4),dp(10),dp(4));row.addView(new IconView(this,icon),new LinearLayout.LayoutParams(dp(30),dp(30)));LinearLayout cp=new LinearLayout(this);cp.setOrientation(LinearLayout.VERTICAL);cp.setPadding(dp(9),0,dp(8),0);cp.addView(text(title,9,INK,true));cp.addView(text(sub,7,MUTED,false));row.addView(cp,new LinearLayout.LayoutParams(0,-2,1));row.addView(text("›",15,MUTED,true),new LinearLayout.LayoutParams(dp(18),-1));row.setOnClickListener(action);return row;}

    private void showProfile(){clearPage();LinearLayout col=pageColumn();col.addView(topBar("Profile"),wrap());LinearLayout card=cardColumn();card.setGravity(Gravity.CENTER_HORIZONTAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.addView(new IconView(this,"profileLarge"),new LinearLayout.LayoutParams(dp(80),dp(80)));card.addView(text("Fahad Hussain",19,INK,true),marginTop(8));card.addView(text("@fahadsoomro123",9,MUTED,false),wrap());col.addView(card,marginBottom(9));LinearLayout stats=cardRow();stats.addView(stat("128","Missions"),weightChild());stats.addView(stat("24","Projects"),weightChild());stats.addView(stat("98%","Success"),weightChild());stats.addView(stat("2.4x","Faster"),weightChild());col.addView(stats,marginBottom(9));LinearLayout account=cardColumn();account.addView(actionListRow("Account Information","Profile and account details","profile",v->toast("Account information opened")),rowHeight());account.addView(actionListRow("Mission History","View previous missions","activity",v->navigate(1)),rowHeight());account.addView(actionListRow("Usage Statistics","Workspace activity","results",v->navigate(2)),rowHeight());col.addView(account);pageHost.addView(col,full());}

    private void showSettings(){
        clearPage();
        LinearLayout col=pageColumn();
        col.addView(topBar("Settings"),wrap());
        col.addView(text("Configure your VEYTRIX experience.",10,MUTED,false),marginBottom(8));
        LinearLayout card=cardColumn();
        String connectionState=autopilotClient.hasToken()?"GitHub connected":"GitHub setup required";
        card.addView(actionListRow("GitHub Connection",connectionState,"privacy",v->showConnectionDialog()),rowHeight());
        card.addView(actionListRow("General","App behavior and language","settings",v->toast("General settings opened")),rowHeight());
        card.addView(actionListRow("Appearance","Theme, colors and display","appearance",v->toast("Appearance settings opened")),rowHeight());
        card.addView(actionListRow("AI & Mission","Model settings and execution","spark",v->showControl()),rowHeight());
        card.addView(liveRow("Notifications","Updates and alerts","notifications",notifications,"bell"),rowHeight());
        card.addView(actionListRow("Privacy","Data and security","privacy",v->toast("Privacy settings opened")),rowHeight());
        card.addView(actionListRow("About","VEYTRIX version 1.0.1","info",v->toast("VEYTRIX Autopilot")),rowHeight());
        card.addView(actionListRow("Support","Get help and contact us","support",v->toast("Support is ready for connection")),rowHeight());
        pageHost.addView(card,full());
    }

    private void showVoice(){clearPage();LinearLayout col=pageColumn();col.setGravity(Gravity.CENTER_HORIZONTAL);col.addView(topBar("Voice Command"),wrap());col.addView(text("Create and control a mission with your voice.",10,MUTED,false),marginBottom(10));VoiceView voice=new VoiceView(this);voice.setOnClickListener(v->{voiceListening=!voiceListening;voice.setListening(voiceListening);toast(voiceListening?"Listening…":"Voice input stopped");});col.addView(voice,new LinearLayout.LayoutParams(dp(220),dp(220)));col.addView(text(voiceListening?"Listening…":"Tap to speak",21,INK,true),marginTop(8));col.addView(text("Give a voice command to create your mission.",9,MUTED,false),wrap());pageHost.addView(col,full());}

    private void showDetails(String title,String desc,String status){clearPage();LinearLayout col=pageColumn();col.addView(topBar(title),wrap());TextView s=text(status,9,status.equals("Failed")?RED:GREEN,true);s.setPadding(dp(8),dp(5),dp(8),dp(5));s.setBackground(roundDrawable(Color.argb(24,Color.red(status.equals("Failed")?RED:GREEN),Color.green(status.equals("Failed")?RED:GREEN),Color.blue(status.equals("Failed")?RED:GREEN)),9,Color.TRANSPARENT));col.addView(s,marginBottom(8));LinearLayout tl=cardColumn();tl.addView(stageRow("Planning","Analyzing requirements",true));tl.addView(stageRow("Executing","Generating code and files",true));tl.addView(stageRow("Verifying","Running tests and validation",false));tl.addView(stageRow("Finalizing","Preparing mission results",false));col.addView(tl,marginBottom(10));LinearLayout output=cardColumn();output.addView(text("Live output",11,INK,true));output.addView(text("$ Initializing project structure…\n$ Creating API endpoints…\n$ Setting database models…\n$ Generating tests…\n$ Running validation…",9,MUTED,false),marginTop(8));col.addView(output,marginBottom(10));LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button pause=button("Pause",Color.rgb(239,246,255),INK);Button cancel=button("Cancel",Color.rgb(255,239,242),RED);pause.setOnClickListener(v->toast("Mission paused"));cancel.setOnClickListener(v->toast("Mission cancelled"));actions.addView(pause,new LinearLayout.LayoutParams(0,dp(42),1));space(actions,8,0);actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(42),1));col.addView(actions);pageHost.addView(col,full());}

    private View stageRow(String title,String sub,boolean done){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(6),0,dp(6));row.addView(new IconView(this,done?"done":"dot"),new LinearLayout.LayoutParams(dp(28),dp(28)));LinearLayout cp=new LinearLayout(this);cp.setOrientation(LinearLayout.VERTICAL);cp.setPadding(dp(9),0,0,0);cp.addView(text(title,10,INK,true));cp.addView(text(sub,8,MUTED,false));row.addView(cp,new LinearLayout.LayoutParams(0,-2,1));return row;}

    private void showCompleted(){clearPage();LinearLayout col=pageColumn();col.setGravity(Gravity.CENTER_HORIZONTAL);col.addView(topBar("Mission Completed"),wrap());col.addView(new IconView(this,"successLarge"),new LinearLayout.LayoutParams(dp(88),dp(88)));col.addView(text("Mission Completed!",24,INK,true),marginTop(10));col.addView(text("Your AI mission has been successfully completed.",10,MUTED,false),marginBottom(12));LinearLayout stats=cardRow();stats.addView(stat("12","Files"),weightChild());stats.addView(stat("3","Tests"),weightChild());stats.addView(stat("2.4m","Total Time"),weightChild());stats.addView(stat("100%","Verified"),weightChild());col.addView(stats,marginBottom(10));Button view=button("View Results",PURPLE,WHITE);view.setOnClickListener(v->navigate(2));Button start=button("Start New Mission",NAVY,WHITE);start.setOnClickListener(v->navigate(0));col.addView(view,marginBottom(7));col.addView(start);pageHost.addView(col,full());}

    private LinearLayout topBar(String title){LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);IconView menu=new IconView(this,"hamburger");menu.setOnClickListener(v->openDrawer());bar.addView(menu,new LinearLayout.LayoutParams(dp(36),dp(36)));IconView logo=new IconView(this,"logo");bar.addView(logo,new LinearLayout.LayoutParams(dp(34),dp(34)));TextView brand=text("VEYTRIX",15,INK,true);brand.setPadding(dp(8),0,0,0);bar.addView(brand,new LinearLayout.LayoutParams(0,-1,1));bar.addView(new IconView(this,"profile"),new LinearLayout.LayoutParams(dp(36),dp(36)));wrap.addView(bar,new LinearLayout.LayoutParams(-1,dp(40)));if(title!=null&&!title.isEmpty())wrap.addView(text(title,25,INK,true),marginTop(5));return wrap;}

    private final class HomeView extends FrameLayout {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final EditText input;
        HomeView(Context c){super(c);setWillNotDraw(false);setBackgroundColor(WHITE);input=new EditText(c);input.setSingleLine(false);input.setMaxLines(3);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE);input.setHint("Describe your task in plain language...");input.setHintTextColor(Color.rgb(135,156,181));input.setTextColor(WHITE);input.setTextSize(13);input.setPadding(dp(12),dp(8),dp(10),dp(6));input.setBackground(roundDrawable(NAVY_2,12,Color.rgb(44,78,121)));addView(input,new FrameLayout.LayoutParams(dp(338),dp(56)));addOverlay("menu",8,10,36,36,v->openDrawer());addOverlay("avatar",346,10,36,36,v->showProfile());addOverlay("mode",286,238,80,30,v->{deepMode=!deepMode;invalidate();});addOverlay("context",20,298,78,34,v->toast("Context tools are ready for connection"));addOverlay("voice",104,298,78,34,v->showVoice());addOverlay("attach",188,298,78,34,v->toast("Attachment picker is ready for connection"));addOverlay("launch",270,298,96,34,v->launchMission());}
        private void addOverlay(String desc,int x,int y,int w,int h,View.OnClickListener listener){TextView v=new TextView(getContext());v.setBackgroundColor(Color.TRANSPARENT);v.setContentDescription(desc);v.setOnClickListener(listener);LayoutParams lp=new LayoutParams(dp(w),dp(h));lp.leftMargin=dp(x);lp.topMargin=dp(y);addView(v,lp);}
        @Override protected void onLayout(boolean ch,int l,int t,int r,int b){int w=r-l,h=b-t;float s=Math.min(w/390f,h/760f);float ox=(w-390f*s)*.5f;for(int i=0;i<getChildCount();i++){View child=getChildAt(i);LayoutParams lp=(LayoutParams)child.getLayoutParams();int x=Math.round(ox+lp.leftMargin/getResources().getDisplayMetrics().density*s);int y=Math.round(lp.topMargin/getResources().getDisplayMetrics().density*s);child.layout(x,y,x+child.getMeasuredWidth(),y+child.getMeasuredHeight());}}
        @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);float s=Math.min(w/390f,h/760f);setMeasuredDimension(w,h);for(int i=0;i<getChildCount();i++){View child=getChildAt(i);LayoutParams lp=(LayoutParams)child.getLayoutParams();int cw=Math.max(1,Math.round(lp.width/getResources().getDisplayMetrics().density*s));int ch=Math.max(1,Math.round(lp.height/getResources().getDisplayMetrics().density*s));child.measure(MeasureSpec.makeMeasureSpec(cw,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(ch,MeasureSpec.EXACTLY));}}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float s=Math.min(getWidth()/390f,getHeight()/760f),ox=(getWidth()-390f*s)*.5f;drawBackground(c,ox,s);drawHeader(c,ox,s);drawHero(c,ox,s);drawCommand(c,ox,s);drawFeatures(c,ox,s);drawMetrics(c,ox,s);drawRecent(c,ox,s);}
        private void drawBackground(Canvas c,float ox,float s){p.setStyle(Paint.Style.FILL);p.setColor(WHITE);c.drawRect(0,0,getWidth(),getHeight(),p);p.setColor(Color.rgb(246,249,253));c.drawCircle(322*s+ox,130*s,125*s,p);p.setColor(Color.argb(18,126,84,239));c.drawCircle(74*s+ox,560*s,120*s,p);}
        private void drawHeader(Canvas c,float ox,float s){round(c,ox+8*s,10*s,ox+44*s,46*s,Color.rgb(239,244,250),11*s,BORDER);icon(c,"hamburger",ox+26*s,28*s,INK,1.7f,s);icon(c,"logo",ox+60*s,28*s,BLUE,1.8f,s);paint(c,"VEYTRIX",ox+83*s,27*s,16*s,INK,true,false);paint(c,"AUTONOMOUS AI DEVELOPMENT",ox+84*s,40*s,6*s,MUTED,false,false);round(c,ox+344*s,10*s,ox+382*s,46*s,NAVY,11*s,BORDER);paint(c,"FH",ox+363*s,33*s,10*s,WHITE,true,true);}
        private void drawHero(Canvas c,float ox,float s){paint(c,"Build",ox+18*s,86*s,36*s,INK,true,false);paint(c,"Without",ox+18*s,118*s,36*s,PURPLE,true,false);paint(c,"Limits.",ox+18*s,150*s,36*s,BLUE,true,false);paint(c,"Turn ideas into real applications",ox+18*s,171*s,9*s,MUTED,false,false);paint(c,"with autonomous AI agents.",ox+18*s,184*s,9*s,MUTED,false,false);round(c,ox+18*s,194*s,ox+140*s,222*s,NAVY,12*s,BORDER);icon(c,"online",ox+31*s,208*s,GREEN,1.7f,s);paint(c,"AI CORE  •  ONLINE",ox+45*s,211*s,7*s,WHITE,true,false);p.setShader(new android.graphics.RadialGradient(312*s+ox,126*s,55*s,new int[]{Color.rgb(126,135,245),Color.rgb(64,69,153),Color.TRANSPARENT},null,android.graphics.Shader.TileMode.CLAMP));c.drawCircle(312*s+ox,126*s,55*s,p);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(67,72,164));c.drawCircle(312*s+ox,126*s,29*s,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.1f*s);p.setColor(Color.rgb(122,97,240));c.drawOval(new RectF(260*s+ox,103*s,364*s+ox,149*s),p);c.drawOval(new RectF(273*s+ox,83*s,350*s+ox,171*s),p);p.setStyle(Paint.Style.FILL);String[] steps={"THINK","PLAN","EXECUTE","VERIFY"};for(int i=0;i<steps.length;i++)paint(c,steps[i],ox+345*s,(104+i*13)*s,8*s,MUTED,false,true);}
        private void drawCommand(Canvas c,float ox,float s){round(c,ox+16*s,229*s,ox+374*s,343*s,NAVY,17*s,Color.rgb(66,101,148));icon(c,"spark",ox+31*s,247*s,PURPLE,1.8f,s);paint(c,"MISSION / COMMAND",ox+46*s,250*s,10*s,WHITE,true,false);round(c,ox+287*s,238*s,ox+365*s,263*s,Color.rgb(17,34,57),10*s,Color.rgb(52,77,116));paint(c,deepMode?"Deep Mode":"Quick Mode",ox+326*s,255*s,7*s,WHITE,true,true);round(c,ox+25*s,268*s,ox+365*s,328*s,NAVY_2,12*s,Color.rgb(44,78,121));drawBtn(c,ox+20*s,298*s,ox+98*s,332*s,"Context","context",Color.rgb(13,30,50),s);drawBtn(c,ox+104*s,298*s,ox+182*s,332*s,"Voice","voice",Color.rgb(13,30,50),s);drawBtn(c,ox+188*s,298*s,ox+266*s,332*s,"Attach","attach",Color.rgb(13,30,50),s);drawBtn(c,ox+270*s,298*s,ox+366*s,332*s,"Launch","launch",Color.rgb(86,88,255),s);}
        private void drawFeatures(Canvas c,float ox,float s){paint(c,"Core capabilities",ox+16*s,367*s,10*s,INK,true,false);float[] xs={16,111,206};String[] titles={"Smart Plan","Multi-Agent","Auto Test"};String[] subs={"Break down a plan","Coordinate agents","Verify & secure"};String[] icons={"plan","agents","test"};int[] cols={PURPLE,BLUE,GREEN};for(int i=0;i<3;i++){round(c,ox+xs[i]*s,374*s,ox+(xs[i]+84)*s,449*s,WHITE,14*s,BORDER);round(c,ox+(xs[i]+9)*s,383*s,ox+(xs[i]+35)*s,409*s,Color.rgb(238,244,251),8*s,BORDER);icon(c,icons[i],ox+(xs[i]+22)*s,396*s,cols[i],1.6f,s);paint(c,titles[i],ox+(xs[i]+9)*s,422*s,7*s,INK,true,false);paint(c,subs[i],ox+(xs[i]+9)*s,435*s,5.5f*s,MUTED,false,false);}}
        private void drawMetrics(Canvas c,float ox,float s){paint(c,"Overview",ox+16*s,465*s,10*s,INK,true,false);round(c,ox+16*s,472*s,ox+374*s,526*s,NAVY,16*s,BORDER);metricCanvas(c,ox+59*s,494*s,"128","Missions",s);metricCanvas(c,ox+149*s,494*s,"24","Projects",s);metricCanvas(c,ox+239*s,494*s,"98%","Success",s);metricCanvas(c,ox+329*s,494*s,"2.4x","Faster",s);}
        private void metricCanvas(Canvas c,float x,float y,String v,String l,float s){paint(c,v,x,y,15*s,WHITE,true,true);paint(c,l,x,y+14*s,6*s,MUTED,false,true);}
        private void drawRecent(Canvas c,float ox,float s){paint(c,"Recent activity",ox+16*s,546*s,10*s,INK,true,false);paint(c,"View all",ox+370*s,546*s,7*s,BLUE,true,true);recent(c,ox,556*s,"Build authentication system","Completed successfully","2h ago",GREEN,"check",s);recent(c,ox,599*s,"Design modern UI components","24 components generated","5h ago",BLUE,"document",s);recent(c,ox,642*s,"Optimize database queries","Performance improved by 70%","1d ago",PURPLE,"gear",s);}
        private void recent(Canvas c,float ox,float y,String title,String sub,String time,int accent,String ic,float s){round(c,ox+16*s,y,ox+374*s,y+38*s,WHITE,13*s,BORDER);round(c,ox+23*s,y+7*s,ox+49*s,y+31*s,Color.rgb(241,245,251),8*s,BORDER);icon(c,ic,ox+36*s,y+19*s,accent,1.6f,s);paint(c,title,ox+58*s,y+16*s,7*s,INK,true,false);paint(c,sub,ox+58*s,y+28*s,6.2f*s,MUTED,false,false);paint(c,time,ox+362*s,y+13*s,6*s,MUTED,false,true);}
        private void drawBtn(Canvas c,float l,float t,float r,float b,String label,String ic,int color,float s){round(c,l,t,r,b,color,9*s,color);icon(c,ic,l+12*s,t+17*s,WHITE,1.4f,s);paint(c,label,l+30*s,t+22*s,6.2f*s,WHITE,true,false);}
        private void round(Canvas c,float l,float t,float r,float b,int color,float radius,int border){p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(color);c.drawRoundRect(l,t,r,b,radius,radius,p);if(border!=Color.TRANSPARENT){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1f,1f));p.setColor(border);c.drawRoundRect(l,t,r,b,radius,radius,p);p.setStyle(Paint.Style.FILL);}}
        private void icon(Canvas c,String k,float cx,float cy,int color,float sw,float s){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(sw*s);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(color);if(k.equals("hamburger")){c.drawLine(cx-7*s,cy-5*s,cx+7*s,cy-5*s,p);c.drawLine(cx-7*s,cy,cx+7*s,cy,p);c.drawLine(cx-7*s,cy+5*s,cx+7*s,cy+5*s,p);}else if(k.equals("logo")){Path q=new Path();q.moveTo(cx-9*s,cy-10*s);q.lineTo(cx,cy+9*s);q.lineTo(cx+9*s,cy-10*s);q.lineTo(cx+3*s,cy-7*s);q.lineTo(cx,cy-1*s);q.lineTo(cx-3*s,cy-7*s);q.close();c.drawPath(q,p);}else if(k.equals("online")){p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy,4*s,p);}else if(k.equals("profile")){c.drawCircle(cx,cy-4*s,4*s,p);c.drawRoundRect(new RectF(cx-7*s,cy+1*s,cx+7*s,cy+9*s),4*s,4*s,p);}else if(k.equals("spark")){c.drawLine(cx,cy-9*s,cx,cy+9*s,p);c.drawLine(cx-9*s,cy,cx+9*s,cy,p);}else if(k.equals("context")){c.drawRect(new RectF(cx-7*s,cy-7*s,cx+7*s,cy+7*s),p);}else if(k.equals("voice")){c.drawRoundRect(new RectF(cx-4*s,cy-8*s,cx+4*s,cy+3*s),4*s,4*s,p);c.drawArc(new RectF(cx-9*s,cy-1*s,cx+9*s,cy+10*s),0,180,false,p);c.drawLine(cx,cy+10*s,cx,cy+14*s,p);}else if(k.equals("attach")){Path q=new Path();q.moveTo(cx-2*s,cy-7*s);q.cubicTo(cx-10*s,cy+1*s,cx+s*1,cy+10*s,cx+6*s,cy+5*s);q.cubicTo(cx+12*s,cy-1*s,cx+6*s,cy-8*s,cx+2*s,cy-4*s);c.drawPath(q,p);}else if(k.equals("launch")){Path q=new Path();q.moveTo(cx-5*s,cy-7*s);q.lineTo(cx+7*s,cy);q.lineTo(cx-5*s,cy+7*s);q.close();c.drawPath(q,p);}else if(k.equals("plan")){c.drawLine(cx-6*s,cy-7*s,cx+7*s,cy-7*s,p);c.drawLine(cx-6*s,cy,cx+7*s,cy,p);c.drawLine(cx-6*s,cy+7*s,cx+5*s,cy+7*s,p);}else if(k.equals("agents")){c.drawCircle(cx,cy-6*s,3*s,p);c.drawCircle(cx-7*s,cy+5*s,3*s,p);c.drawCircle(cx+7*s,cy+5*s,3*s,p);c.drawLine(cx-2*s,cy-3*s,cx-5*s,cy+2*s,p);c.drawLine(cx+2*s,cy-3*s,cx+5*s,cy+2*s,p);}else if(k.equals("test")||k.equals("done")||k.equals("check")){c.drawLine(cx-7*s,cy,cx-1*s,cy+6*s,p);c.drawLine(cx-1*s,cy+6*s,cx+8*s,cy-7*s,p);}else if(k.equals("mission")){c.drawRect(new RectF(cx-7*s,cy-7*s,cx+7*s,cy+7*s),p);c.drawLine(cx-4*s,cy-2*s,cx+4*s,cy-2*s,p);c.drawLine(cx-4*s,cy+2*s,cx+2*s,cy+2*s,p);}else if(k.equals("alert")){Path q=new Path();q.moveTo(cx,cy-8*s);q.lineTo(cx+8*s,cy+7*s);q.lineTo(cx-8*s,cy+7*s);q.close();c.drawPath(q,p);}else if(k.equals("document")||k.equals("report")){c.drawRect(new RectF(cx-6*s,cy-8*s,cx+6*s,cy+8*s),p);c.drawLine(cx-3*s,cy-2*s,cx+3*s,cy-2*s,p);c.drawLine(cx-3*s,cy+2*s,cx+3*s,cy+2*s,p);}else if(k.equals("code")){c.drawLine(cx-4*s,cy,cx-8*s,cy+4*s,p);c.drawLine(cx-8*s,cy+4*s,cx-4*s,cy+8*s,p);c.drawLine(cx+4*s,cy,cx+8*s,cy+4*s,p);c.drawLine(cx+8*s,cy+4*s,cx+4*s,cy+8*s,p);}else if(k.equals("database")){c.drawOval(new RectF(cx-7*s,cy-6*s,cx+7*s,cy),p);c.drawArc(new RectF(cx-7*s,cy-2*s,cx+7*s,cy+9*s),0,180,false,p);c.drawLine(cx-7*s,cy-2*s,cx-7*s,cy+4*s,p);c.drawLine(cx+7*s,cy-2*s,cx+7*s,cy+4*s,p);}else if(k.equals("android")){c.drawRoundRect(new RectF(cx-7*s,cy-5*s,cx+7*s,cy+7*s),2*s,2*s,p);c.drawLine(cx-5*s,cy-5*s,cx-2*s,cy-10*s,p);c.drawLine(cx+5*s,cy-5*s,cx+2*s,cy-10*s,p);}else if(k.equals("archive")){c.drawRect(new RectF(cx-8*s,cy-6*s,cx+8*s,cy+6*s),p);c.drawLine(cx-4*s,cy-10*s,cx+4*s,cy-10*s,p);}else if(k.equals("model")){c.drawCircle(cx,cy,7*s,p);c.drawCircle(cx,cy,2*s,p);}else if(k.equals("fallback")){c.drawArc(new RectF(cx-7*s,cy-7*s,cx+7*s,cy+7*s),-60,250,false,p);}else if(k.equals("clock")){c.drawCircle(cx,cy,7*s,p);c.drawLine(cx,cy,cx+4*s,cy+3*s,p);c.drawLine(cx,cy,cx,cy-4*s,p);}else if(k.equals("parallel")){c.drawCircle(cx-5*s,cy,3*s,p);c.drawCircle(cx+5*s,cy,3*s,p);c.drawLine(cx-2*s,cy,cx+2*s,cy,p);}else if(k.equals("bell")){c.drawRoundRect(new RectF(cx-6*s,cy-6*s,cx+6*s,cy+6*s),4*s,4*s,p);c.drawLine(cx-8*s,cy+8*s,cx+8*s,cy+8*s,p);}else if(k.equals("settings")||k.equals("gear")){c.drawCircle(cx,cy,7*s,p);c.drawCircle(cx,cy,2*s,p);}else if(k.equals("appearance")){c.drawCircle(cx,cy,7*s,p);c.drawLine(cx,cy-7*s,cx,cy+7*s,p);}else if(k.equals("privacy")){c.drawRect(new RectF(cx-6*s,cy-5*s,cx+6*s,cy+7*s),p);c.drawCircle(cx,cy-5*s,3*s,p);}else if(k.equals("info")){c.drawCircle(cx,cy,7*s,p);c.drawLine(cx,cy-2*s,cx,cy+5*s,p);}else if(k.equals("support")){c.drawArc(new RectF(cx-8*s,cy-8*s,cx+8*s,cy+8*s),20,140,false,p);}else if(k.equals("profileLarge")){p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(241,246,252));c.drawCircle(cx,cy,38*s,p);p.setStyle(Paint.Style.STROKE);p.setColor(BLUE);c.drawCircle(cx,cy,38*s,p);c.drawCircle(cx,cy-9*s,7*s,p);}else if(k.equals("successLarge")){p.setStyle(Paint.Style.FILL);p.setColor(GREEN);c.drawCircle(cx,cy,Math.min(getWidth(),getHeight())*.42f,p);p.setStyle(Paint.Style.STROKE);p.setColor(WHITE);p.setStrokeWidth(dp(3));c.drawLine(cx-11,cy,cx-3,cy+9,p);c.drawLine(cx-3,cy+9,cx+13,cy-12,p);}else if(k.equals("dot")){p.setStyle(Paint.Style.FILL);p.setColor(MUTED);c.drawCircle(cx,cy,5*s,p);}else if(k.equals("activity")||k.equals("results")||k.equals("control")||k.equals("home")){c.drawRect(new RectF(cx-6*s,cy-6*s,cx+6*s,cy+6*s),p);}p.setStyle(Paint.Style.FILL);}
        private void paint(Canvas c,String t,float x,float y,float size,int color,boolean bold,boolean center){p.setTypeface(bold?Typeface.create(Typeface.DEFAULT,Typeface.BOLD):Typeface.DEFAULT);p.setTextSize(size);p.setColor(color);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(t,x,y,p);p.setTextAlign(Paint.Align.LEFT);}
    }

    private void launchMission(){
        HomeView hv=null;
        if(pageHost.getChildCount()>0 && pageHost.getChildAt(0) instanceof HomeView) hv=(HomeView)pageHost.getChildAt(0);
        if(hv!=null){
            mission=hv.input.getText().toString().trim();
            if(mission.isEmpty()){
                hv.input.requestFocus();
                ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(hv.input,InputMethodManager.SHOW_IMPLICIT);
                toast("Write a mission first");
                return;
            }
        }
        if(!autopilotClient.hasToken()){
            showConnectionDialog();
            return;
        }
        showLiveMission(mission);
        final String target=autopilotClient.getTargetRepository();
        autopilotClient.startMission(mission,target,"main","auto","2",new VeytrixAutopilotClient.Callback(){
            @Override public void onStarted(){ updateLiveMessage("Connecting to the VEYTRIX control plane…"); }
            @Override public void onRunLocated(VeytrixAutopilotClient.RunInfo run){
                activeRunId=run.id; activeRunUrl=run.htmlUrl; updateLiveRun(run);
            }
            @Override public void onRunUpdated(VeytrixAutopilotClient.RunInfo run){ updateLiveRun(run); }
            @Override public void onCompleted(VeytrixAutopilotClient.RunInfo run){
                updateLiveRun(run);
                toast(run.isSuccessful()?"Mission verified successfully":"Mission finished with a blocker");
            }
            @Override public void onError(String message){ updateLiveMessage("Mission error: "+message); if(activeRunOpenButton!=null)activeRunOpenButton.setVisibility(View.GONE); }
        });
    }

    private void showConnectionDialog(){
        LinearLayout form=new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18),dp(4),dp(18),dp(4));

        EditText target= new EditText(this);
        target.setSingleLine(true);
        target.setText(autopilotClient.getTargetRepository());
        target.setHint("owner/repository");
        target.setTextSize(13);
        target.setPadding(dp(12),0,dp(12),0);
        target.setBackground(roundDrawable(Color.rgb(244,248,253),10,BORDER));
        form.addView(target,new LinearLayout.LayoutParams(-1,dp(48)));

        Space gap=new Space(this);
        form.addView(gap,new LinearLayout.LayoutParams(1,dp(10)));

        EditText token=new EditText(this);
        token.setSingleLine(true);
        token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        token.setHint(autopilotClient.hasToken()?"Saved token • leave blank to reuse":"GitHub fine-grained token");
        token.setTextSize(13);
        token.setPadding(dp(12),0,dp(12),0);
        token.setBackground(roundDrawable(Color.rgb(244,248,253),10,BORDER));
        form.addView(token,new LinearLayout.LayoutParams(-1,dp(48)));

        TextView note=text("The token is encrypted locally with Android Keystore. It is never committed to the repository.",9,MUTED,false);
        note.setPadding(0,dp(10),0,0);
        form.addView(note,wrap());

        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle("Connect VEYTRIX")
                .setView(form)
                .setNegativeButton("CANCEL",null)
                .setPositiveButton("VERIFY & SAVE",null)
                .create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            String repo=target.getText().toString().trim();
            String typed=token.getText().toString().trim();
            if(repo.isEmpty() || !repo.matches("[^/\\\\s]+/[^/\\\\s]+")){
                target.setError("Use owner/repository");
                return;
            }
            try{
                autopilotClient.saveTargetRepository(repo);
            }catch(Exception e){
                target.setError(e.getMessage());
                return;
            }
            autopilotClient.verifyConnection(typed,repo,new VeytrixAutopilotClient.SimpleCallback<VeytrixAutopilotClient.Verification>(){
                @Override public void onSuccess(VeytrixAutopilotClient.Verification value){
                    dialog.dismiss();
                    toast("Connected as @"+value.login);
                    showSettings();
                }
                @Override public void onError(String message){ token.setError(message); }
            });
        }));
        dialog.show();
    }

    private void showLiveMission(String missionText){
        clearPage();
        LinearLayout col=pageColumn();
        col.addView(topBar("Live Mission"),wrap());
        col.addView(text("Autonomous control-plane execution",10,MUTED,false),marginBottom(9));

        LinearLayout card=cardColumn();
        card.addView(text("MISSION",9,PURPLE,true),marginBottom(4));
        card.addView(text(missionText,14,INK,true),marginBottom(9));
        activeRunStatusView=text("Dispatching…",11,GREEN,true);
        card.addView(activeRunStatusView,marginBottom(4));
        activeRunMetaView=text("Preparing secure GitHub workflow dispatch",9,MUTED,false);
        card.addView(activeRunMetaView,marginBottom(8));

        activeRunOpenButton=button("OPEN LIVE RUN",BLUE,WHITE);
        activeRunOpenButton.setVisibility(View.GONE);
        activeRunOpenButton.setOnClickListener(v->{
            if(!activeRunUrl.isEmpty()) startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(activeRunUrl)));
        });
        card.addView(activeRunOpenButton,marginBottom(8));
        col.addView(card,marginBottom(9));

        LinearLayout stages=cardColumn();
        stages.addView(text("EXECUTION PIPELINE",9,INK,true),marginBottom(4));
        stages.addView(stageRow("Understanding","Mission received by control plane",false),rowHeight());
        stages.addView(stageRow("Planning","Deterministic inspection and engine selection",false),rowHeight());
        stages.addView(stageRow("Executing","AI or deterministic mission execution",false),rowHeight());
        stages.addView(stageRow("Verifying","Tests, artifact checks and evidence",false),rowHeight());
        stages.addView(stageRow("Completed","Final result returned to device",false));
        col.addView(stages,marginBottom(9));

        LinearLayout actions=new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button home=button("Back Home",Color.rgb(239,246,255),INK);
        home.setOnClickListener(v->navigate(0));
        Button settings=button("Connection",Color.rgb(239,246,255),INK);
        settings.setOnClickListener(v->showSettings());
        actions.addView(home,new LinearLayout.LayoutParams(0,dp(44),1));
        space(actions,8,0);
        actions.addView(settings,new LinearLayout.LayoutParams(0,dp(44),1));
        col.addView(actions);
        pageHost.addView(scrollWrap(col),full());
    }

    private void updateLiveMessage(String message){
        if(activeRunStatusView!=null) activeRunStatusView.setText(message);
        if(activeRunMetaView!=null) activeRunMetaView.setText("VEYTRIX control plane");
    }

    private void updateLiveRun(VeytrixAutopilotClient.RunInfo run){
        if(activeRunStatusView==null || activeRunMetaView==null) return;
        String status=run.isFinished() ? ("COMPLETED • "+(run.conclusion.isEmpty()?"UNKNOWN":run.conclusion.toUpperCase())) : run.status.toUpperCase();
        activeRunStatusView.setText(status);
        activeRunMetaView.setText("Run #"+run.number+" • updated "+run.updatedAt);
        activeRunUrl=run.htmlUrl;
        if(activeRunOpenButton!=null) activeRunOpenButton.setVisibility(run.htmlUrl.isEmpty()?View.GONE:View.VISIBLE);
    }

    @Override protected void onDestroy(){
        if(autopilotClient!=null) autopilotClient.shutdown();
        super.onDestroy();
    }

    private LinearLayout pageColumn(){LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);return col;}
    private ScrollView scrollWrap(View child){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setVerticalScrollBarEnabled(false);s.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);if(child.getParent()!=null)((ViewGroup)child.getParent()).removeView(child);s.addView(child);return s;}
    private LinearLayout cardColumn(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(10),dp(8),dp(10),dp(8));c.setBackground(roundDrawable(WHITE,16,BORDER));c.setElevation(dp(2));return c;}
    private LinearLayout cardRow(){LinearLayout c=new LinearLayout(this);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(10),dp(8),dp(10),dp(8));c.setBackground(roundDrawable(WHITE,16,BORDER));c.setElevation(dp(2));return c;}
    private LinearLayout stat(String value,String label){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);c.addView(text(value,15,INK,true),new LinearLayout.LayoutParams(-1,dp(20)));c.addView(text(label,7,MUTED,false),new LinearLayout.LayoutParams(-1,dp(16)));return c;}
    private LinearLayout.LayoutParams weightChild(){return new LinearLayout.LayoutParams(0,dp(52),1);}
    private Button button(String label,int background,int fg){Button b=new Button(this);b.setText(label);b.setTextSize(10);b.setTextColor(fg);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(roundDrawable(background,12,background==WHITE?BORDER:background));b.setPadding(0,0,0,0);return b;}
    private TextView text(String s,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);return t;}
    private GradientDrawable roundDrawable(int fill,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(Math.max(1,dp(1)),stroke);return d;}
    private ViewGroup.LayoutParams marginBottom(int px){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(px);return p;}
    private ViewGroup.LayoutParams marginTop(int px){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(px);return p;}
    private ViewGroup.LayoutParams margin(int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.leftMargin=dp(l);p.topMargin=dp(t);p.rightMargin=dp(r);p.bottomMargin=dp(b);return p;}
    private ViewGroup.LayoutParams wrap(){return new LinearLayout.LayoutParams(-1,-2);}
    private void space(LinearLayout parent,int width,int height){Space s=new Space(parent.getContext());parent.addView(s,new LinearLayout.LayoutParams(dp(width),dp(height)));}
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private FrameLayout.LayoutParams full(){return new FrameLayout.LayoutParams(-1,-1);}

    private final class IconView extends View {
        private final String kind; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        IconView(Context c,String kind){super(c);this.kind=kind;setContentDescription(kind);}
        @Override protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,s=Math.min(getWidth(),getHeight())/32f;int col=INK;if(kind.equals("logo")||kind.equals("spark"))col=PURPLE;else if(kind.equals("test")||kind.equals("done")||kind.equals("check")||kind.equals("bell")||kind.equals("successLarge"))col=GREEN;else if(kind.equals("android")||kind.equals("code")||kind.equals("document")||kind.equals("mission")||kind.equals("activity")||kind.equals("results"))col=BLUE;p.setColor(col);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,dp(1.6f)));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);draw(c,x,y,s);}
        private void draw(Canvas c,float x,float y,float s){if(kind.equals("hamburger")){line(c,x-7*s,y-5*s,x+7*s,y-5*s);line(c,x-7*s,y,x+7*s,y);line(c,x-7*s,y+5*s,x+7*s,y+5*s);}else if(kind.equals("logo")){Path q=new Path();q.moveTo(x-9*s,y-10*s);q.lineTo(x,y+9*s);q.lineTo(x+9*s,y-10*s);q.close();c.drawPath(q,p);}else if(kind.equals("profile")||kind.equals("profileLarge")){c.drawCircle(x,y-4*s,4*s,p);c.drawRoundRect(new RectF(x-7*s,y+1*s,x+7*s,y+9*s),4*s,4*s,p);}else if(kind.equals("check")||kind.equals("done")){line(c,x-7*s,y,x-1*s,y+6*s);line(c,x-1*s,y+6*s,x+8*s,y-7*s);}else if(kind.equals("database")){c.drawOval(new RectF(x-7*s,y-6*s,x+7*s,y),p);c.drawArc(new RectF(x-7*s,y-2*s,x+7*s,y+9*s),0,180,false,p);}else if(kind.equals("document")||kind.equals("report")){c.drawRect(new RectF(x-6*s,y-8*s,x+6*s,y+8*s),p);line(c,x-3*s,y-2*s,x+3*s,y-2*s);line(c,x-3*s,y+2*s,x+3*s,y+2*s);}else if(kind.equals("code")){line(c,x-4*s,y,x-8*s,y+4*s);line(c,x-8*s,y+4*s,x-4*s,y+8*s);line(c,x+4*s,y,x+8*s,y+4*s);line(c,x+8*s,y+4*s,x+4*s,y+8*s);}else if(kind.equals("android")){c.drawRoundRect(new RectF(x-7*s,y-5*s,x+7*s,y+7*s),2*s,2*s,p);line(c,x-5*s,y-5*s,x-2*s,y-10*s);line(c,x+5*s,y-5*s,x+2*s,y-10*s);}else if(kind.equals("archive")){c.drawRect(new RectF(x-8*s,y-6*s,x+8*s,y+6*s),p);}else if(kind.equals("mission")){c.drawRect(new RectF(x-7*s,y-7*s,x+7*s,y+7*s),p);line(c,x-4*s,y-2*s,x+4*s,y-2*s);}else if(kind.equals("alert")){Path q=new Path();q.moveTo(x,y-8*s);q.lineTo(x+8*s,y+7*s);q.lineTo(x-8*s,y+7*s);q.close();c.drawPath(q,p);}else if(kind.equals("model")){c.drawCircle(x,y,7*s,p);c.drawCircle(x,y,2*s,p);}else if(kind.equals("fallback")){c.drawArc(new RectF(x-7*s,y-7*s,x+7*s,y+7*s),-60,250,false,p);}else if(kind.equals("clock")){c.drawCircle(x,y,7*s,p);line(c,x,y,x+4*s,y+3*s);line(c,x,y,x,y-4*s);}else if(kind.equals("spark")){line(c,x,y-9*s,x,y+9*s);line(c,x-9*s,y,x+9*s,y);}else if(kind.equals("voice")){c.drawRoundRect(new RectF(x-4*s,y-8*s,x+4*s,y+3*s),4*s,4*s,p);c.drawArc(new RectF(x-9*s,y-1*s,x+9*s,y+10*s),0,180,false,p);}else if(kind.equals("attach")){Path q=new Path();q.moveTo(x-2*s,y-7*s);q.cubicTo(x-10*s,y+s,x+s*1,y+10*s,x+6*s,y+5*s);c.drawPath(q,p);}else if(kind.equals("launch")){Path q=new Path();q.moveTo(x-5*s,y-7*s);q.lineTo(x+7*s,y);q.lineTo(x-5*s,y+7*s);q.close();c.drawPath(q,p);}else if(kind.equals("bell")){c.drawRoundRect(new RectF(x-6*s,y-6*s,x+6*s,y+6*s),4*s,4*s,p);line(c,x-8*s,y+8*s,x+8*s,y+8*s);}else if(kind.equals("settings")||kind.equals("gear")){c.drawCircle(x,y,7*s,p);c.drawCircle(x,y,2*s,p);}else if(kind.equals("appearance")){c.drawCircle(x,y,7*s,p);line(c,x,y-7*s,x,y+7*s);}else if(kind.equals("privacy")){c.drawRect(new RectF(x-6*s,y-5*s,x+6*s,y+7*s),p);}else if(kind.equals("info")){c.drawCircle(x,y,7*s,p);line(c,x,y-2*s,x,y+5*s);}else if(kind.equals("support")){c.drawArc(new RectF(x-8*s,y-8*s,x+8*s,y+8*s),20,140,false,p);}else if(kind.equals("successLarge")){p.setStyle(Paint.Style.FILL);p.setColor(GREEN);c.drawCircle(x,y,Math.min(getWidth(),getHeight())*.42f,p);p.setStyle(Paint.Style.STROKE);p.setColor(WHITE);p.setStrokeWidth(dp(3));line(c,x-11,y,x-3,y+9);line(c,x-3,y+9,x+13,y-12);}else {p.setStyle(Paint.Style.FILL);p.setColor(MUTED);c.drawCircle(x,y,4*s,p);}p.setStyle(Paint.Style.FILL);}
        private void line(Canvas c,float a,float b,float d,float e){c.drawLine(a,b,d,e,p);}
    }

    private static final class ProgressView extends View {private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final String progress;private final int color;ProgressView(Context c,String progress,int color){super(c);this.progress=progress;this.color=color;}@Override protected void onDraw(Canvas c){float pct=0;try{pct=Float.parseFloat(progress.replace("%",""))/100f;}catch(Exception ignored){}p.setColor(Color.rgb(229,236,246));c.drawRoundRect(new RectF(0,0,getWidth(),getHeight()),getHeight(),getHeight(),p);p.setColor(color);c.drawRoundRect(new RectF(0,0,getWidth()*pct,getHeight()),getHeight(),getHeight(),p);}}
    private static final class ToggleView extends View {private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private boolean on;ToggleView(Context c,boolean on){super(c);this.on=on;}boolean isOn(){return on;}@Override protected void onDraw(Canvas c){p.setColor(on?BLUE:Color.rgb(220,228,239));c.drawRoundRect(new RectF(0,0,getWidth(),getHeight()),getHeight()/2f,getHeight()/2f,p);p.setColor(WHITE);float d=getHeight()-4;float cx=on?getWidth()-2-d/2f:2+d/2f;c.drawCircle(cx,getHeight()/2f,d/2f,p);}}
    private static final class VoiceView extends View {private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private boolean listening;VoiceView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}void setListening(boolean v){listening=v;invalidate();}@Override protected void onDraw(Canvas c){float cx=getWidth()/2f,cy=getHeight()/2f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(BLUE);c.drawCircle(cx,cy,82,p);p.setColor(PURPLE);c.drawCircle(cx,cy,62,p);p.setStyle(Paint.Style.FILL);p.setColor(listening?GREEN:PURPLE);c.drawCircle(cx,cy,44,p);p.setColor(WHITE);c.drawRoundRect(new RectF(cx-8,cy-21,cx+8,cy+15),8,8,p);p.setStyle(Paint.Style.STROKE);p.setColor(WHITE);c.drawArc(new RectF(cx-19,cy-4,cx+19,cy+25),0,180,false,p);c.drawLine(cx,cy+25,cx,cy+34,p);p.setStyle(Paint.Style.FILL);}}
}
