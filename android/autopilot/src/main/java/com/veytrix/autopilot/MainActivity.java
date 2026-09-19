package com.veytrix.autopilot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.Window;
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
    private VeytrixAutopilotClient autopilotClient;
    private boolean deepMode = true;
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
        configureWindow();
        autopilotClient = new VeytrixAutopilotClient(this);
        buildShell();
        showHome();
    }

    @Override protected void onDestroy() {
        if (autopilotClient != null) {
            autopilotClient.shutdown();
        }
        super.onDestroy();
    }

    private void configureWindow() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
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
        root.setBackgroundColor(VeytrixDesignTokens.PEARL);

        pageHost = new FrameLayout(this);
        root.addView(pageHost, full());

        bottomNav = buildBottomNav();
        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(
                -1, dp(68), Gravity.BOTTOM
        );
        navLp.leftMargin = dp(10);
        navLp.rightMargin = dp(10);
        navLp.bottomMargin = dp(8);
        root.addView(bottomNav, navLp);

        buildDrawer();
        setContentView(root);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            WindowInsetsCompat.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );
            pageHost.setPadding(
                    dp(14),
                    bars.top + dp(6),
                    dp(14),
                    bars.bottom + dp(82)
            );

            ViewGroup.LayoutParams params = bottomNav.getLayoutParams();
            if (params instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams bottomParams =
                        (FrameLayout.LayoutParams) params;
                bottomParams.bottomMargin = bars.bottom + dp(8);
                bottomNav.setLayoutParams(bottomParams);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(6), dp(6), dp(6), dp(6));
        nav.setBackground(roundDrawable(
                VeytrixDesignTokens.WHITE,
                20,
                VeytrixDesignTokens.SILVER
        ));
        String[] labels = {"Home", "Activity", "Results", "Control"};
        String[] icons = {"home", "activity", "results", "control"};

        for (int i = 0; i < labels.length; i++) {
            final int page = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(2), dp(4), dp(2));

            IconView icon = new IconView(this, icons[i]);
            TextView text = text(
                    labels[i],
                    9,
                    VeytrixDesignTokens.TEXT_SECONDARY,
                    true
            );
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

    private void showHome() {
        clearPage();
        pageHost.setPadding(
                dp(14), dp(6), dp(14), dp(82)
        );
        pageHost.addView(
                new VeytrixHomeView(
                        this,
                        autopilotClient,
                        new VeytrixHomeView.Host() {
                            @Override public void openDrawer() {
                                MainActivity.this.openDrawer();
                            }

                            @Override public void openConnection() {
                                MainActivity.this.showConnectionDialog();
                            }
                        }
                ),
                full()
        );
    }

    private void showConnectionDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), dp(4), dp(8), 0);

        EditText repository = new EditText(this);
        repository.setSingleLine(true);
        repository.setText(autopilotClient.getTargetRepository());
        repository.setHint("owner/repository");
        repository.setTextSize(15);
        repository.setInputType(InputType.TYPE_CLASS_TEXT);
        repository.setPadding(dp(10), 0, dp(10), 0);
        repository.setBackground(roundDrawable(
                VeytrixDesignTokens.PEARL,
                12,
                VeytrixDesignTokens.SILVER
        ));

        EditText token = new EditText(this);
        token.setSingleLine(true);
        token.setHint("GitHub token");
        token.setTextSize(15);
        token.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        token.setPadding(dp(10), 0, dp(10), 0);
        token.setBackground(roundDrawable(
                VeytrixDesignTokens.PEARL,
                12,
                VeytrixDesignTokens.SILVER
        ));

        TextView security = text(
                "Token is stored encrypted with Android Keystore and never shown in logs.",
                11,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        );
        security.setPadding(dp(2), dp(8), dp(2), dp(4));

        form.addView(
                text("TARGET REPOSITORY", 10, VeytrixDesignTokens.TEXT_PRIMARY, true),
                new LinearLayout.LayoutParams(-1, dp(24))
        );
        form.addView(repository, new LinearLayout.LayoutParams(-1, dp(52)));
        form.addView(
                text("GITHUB CREDENTIAL", 10, VeytrixDesignTokens.TEXT_PRIMARY, true),
                margin(0, 9, 0, 0)
        );
        form.addView(token, new LinearLayout.LayoutParams(-1, dp(52)));
        form.addView(security, new LinearLayout.LayoutParams(-1, dp(48)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Secure GitHub Connection")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("VERIFY & SAVE", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setTextColor(VeytrixDesignTokens.VIOLET);
            positive.setOnClickListener(v -> {
                String repo = repository.getText().toString().trim();
                String tokenOverride = token.getText().toString().trim();
                try {
                    VeytrixInputValidator.validateRepository(repo);
                } catch (IllegalArgumentException error) {
                    repository.setError(error.getMessage());
                    return;
                }

                positive.setEnabled(false);
                positive.setText("VERIFYING…");

                autopilotClient.verifyConnection(
                        tokenOverride,
                        repo,
                        new VeytrixAutopilotClient.SimpleCallback<
                                VeytrixAutopilotClient.Verification>() {
                            @Override public void onSuccess(
                                    VeytrixAutopilotClient.Verification value) {
                                dialog.dismiss();
                                toast("Connected to " + value.login);
                                showHome();
                            }

                            @Override public void onError(String message) {
                                positive.setEnabled(true);
                                positive.setText("VERIFY & SAVE");
                                toast(message);
                            }
                        }
                );
            });
        });

        dialog.show();
    }

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

    private void showSettings(){clearPage();LinearLayout col=pageColumn();col.addView(topBar("Settings"),wrap());col.addView(text("Configure your VEYTRIX experience.",10,MUTED,false),marginBottom(8));LinearLayout card=cardColumn();card.addView(actionListRow("General","App behavior and language","settings",v->toast("General settings opened")),rowHeight());card.addView(actionListRow("Appearance","Theme, colors and display","appearance",v->toast("Appearance settings opened")),rowHeight());card.addView(actionListRow("AI & Mission","Model settings and execution","spark",v->showControl()),rowHeight());card.addView(liveRow("Notifications","Updates and alerts","notifications",notifications,"bell"),rowHeight());card.addView(actionListRow("Privacy","Data and security","privacy",v->toast("Privacy settings opened")),rowHeight());card.addView(actionListRow("About","VEYTRIX version 1.0.1","info",v->toast("VEYTRIX Autopilot")),rowHeight());card.addView(actionListRow("Support","Get help and contact us","support",v->toast("Support is ready for connection")),rowHeight());col.addView(card);pageHost.addView(col,full());}

    private void showVoice(){clearPage();LinearLayout col=pageColumn();col.setGravity(Gravity.CENTER_HORIZONTAL);col.addView(topBar("Voice Command"),wrap());col.addView(text("Create and control a mission with your voice.",10,MUTED,false),marginBottom(10));VoiceView voice=new VoiceView(this);voice.setOnClickListener(v->{voiceListening=!voiceListening;voice.setListening(voiceListening);toast(voiceListening?"Listening…":"Voice input stopped");});col.addView(voice,new LinearLayout.LayoutParams(dp(220),dp(220)));col.addView(text(voiceListening?"Listening…":"Tap to speak",21,INK,true),marginTop(8));col.addView(text("Give a voice command to create your mission.",9,MUTED,false),wrap());pageHost.addView(col,full());}

    private void showDetails(String title,String desc,String status){clearPage();LinearLayout col=pageColumn();col.addView(topBar(title),wrap());TextView s=text(status,9,status.equals("Failed")?RED:GREEN,true);s.setPadding(dp(8),dp(5),dp(8),dp(5));s.setBackground(roundDrawable(Color.argb(24,Color.red(status.equals("Failed")?RED:GREEN),Color.green(status.equals("Failed")?RED:GREEN),Color.blue(status.equals("Failed")?RED:GREEN)),9,Color.TRANSPARENT));col.addView(s,marginBottom(8));LinearLayout tl=cardColumn();tl.addView(stageRow("Planning","Analyzing requirements",true));tl.addView(stageRow("Executing","Generating code and files",true));tl.addView(stageRow("Verifying","Running tests and validation",false));tl.addView(stageRow("Finalizing","Preparing mission results",false));col.addView(tl,marginBottom(10));LinearLayout output=cardColumn();output.addView(text("Live output",11,INK,true));output.addView(text("$ Initializing project structure…\n$ Creating API endpoints…\n$ Setting database models…\n$ Generating tests…\n$ Running validation…",9,MUTED,false),marginTop(8));col.addView(output,marginBottom(10));LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button pause=button("Pause",Color.rgb(239,246,255),INK);Button cancel=button("Cancel",Color.rgb(255,239,242),RED);pause.setOnClickListener(v->toast("Mission paused"));cancel.setOnClickListener(v->toast("Mission cancelled"));actions.addView(pause,new LinearLayout.LayoutParams(0,dp(42),1));space(actions,8,0);actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(42),1));col.addView(actions);pageHost.addView(col,full());}

    private View stageRow(String title,String sub,boolean done){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(6),0,dp(6));row.addView(new IconView(this,done?"done":"dot"),new LinearLayout.LayoutParams(dp(28),dp(28)));LinearLayout cp=new LinearLayout(this);cp.setOrientation(LinearLayout.VERTICAL);cp.setPadding(dp(9),0,0,0);cp.addView(text(title,10,INK,true));cp.addView(text(sub,8,MUTED,false));row.addView(cp,new LinearLayout.LayoutParams(0,-2,1));return row;}

    private void showCompleted(){clearPage();LinearLayout col=pageColumn();col.setGravity(Gravity.CENTER_HORIZONTAL);col.addView(topBar("Mission Completed"),wrap());col.addView(new IconView(this,"successLarge"),new LinearLayout.LayoutParams(dp(88),dp(88)));col.addView(text("Mission Completed!",24,INK,true),marginTop(10));col.addView(text("Your AI mission has been successfully completed.",10,MUTED,false),marginBottom(12));LinearLayout stats=cardRow();stats.addView(stat("12","Files"),weightChild());stats.addView(stat("3","Tests"),weightChild());stats.addView(stat("2.4m","Total Time"),weightChild());stats.addView(stat("100%","Verified"),weightChild());col.addView(stats,marginBottom(10));Button view=button("View Results",PURPLE,WHITE);view.setOnClickListener(v->navigate(2));Button start=button("Start New Mission",NAVY,WHITE);start.setOnClickListener(v->navigate(0));col.addView(view,marginBottom(7));col.addView(start);pageHost.addView(col,full());}

    private LinearLayout topBar(String title){LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);IconView menu=new IconView(this,"hamburger");menu.setOnClickListener(v->openDrawer());bar.addView(menu,new LinearLayout.LayoutParams(dp(36),dp(36)));IconView logo=new IconView(this,"logo");bar.addView(logo,new LinearLayout.LayoutParams(dp(34),dp(34)));TextView brand=text("VEYTRIX",15,INK,true);brand.setPadding(dp(8),0,0,0);bar.addView(brand,new LinearLayout.LayoutParams(0,-1,1));bar.addView(new IconView(this,"profile"),new LinearLayout.LayoutParams(dp(36),dp(36)));wrap.addView(bar,new LinearLayout.LayoutParams(-1,dp(40)));if(title!=null&&!title.isEmpty())wrap.addView(text(title,25,INK,true),marginTop(5));return wrap;}

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
