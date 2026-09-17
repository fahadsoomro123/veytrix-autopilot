package com.veytrix.autopilot;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** Complete native VEYTRIX Android surface. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(3, 9, 18);
    private static final int SURFACE = Color.rgb(7, 18, 32);
    private static final int SURFACE_2 = Color.rgb(9, 24, 41);
    private static final int SURFACE_3 = Color.rgb(11, 29, 48);
    private static final int BLUE = Color.rgb(89, 154, 255);
    private static final int CYAN = Color.rgb(72, 216, 248);
    private static final int VIOLET = Color.rgb(151, 100, 255);
    private static final int TEXT = Color.rgb(243, 247, 255);
    private static final int MUTED = Color.rgb(156, 180, 210);
    private static final int SUBTLE = Color.rgb(106, 131, 160);
    private static final int GREEN = Color.rgb(43, 220, 171);
    private static final int AMBER = Color.rgb(236, 192, 103);
    private static final int RED = Color.rgb(244, 108, 127);
    private static final int BORDER = Color.rgb(46, 80, 120);

    private FrameLayout root;
    private VeytrixView surface;
    private FlagshipCoreView coreView;
    private EditText prompt;
    private TextView contextAction, mic, attachAction, send, modeAction;
    private boolean deepMode = true;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        build();
    }

    private void configureWindow() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(Color.rgb(2, 7, 14));
        window.setNavigationBarColor(Color.rgb(2, 7, 14));
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    private void build() {
        root = new FrameLayout(this);
        surface = new VeytrixView(this);
        surface.setContentDescription("VEYTRIX application");
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));

        coreView = new FlagshipCoreView(this);
        coreView.setContentDescription("VEYTRIX autonomous core");
        coreView.setAlpha(0.78f);
        root.addView(coreView, new FrameLayout.LayoutParams(1, 1));

        prompt = new EditText(this);
        prompt.setTextColor(TEXT);
        prompt.setHintTextColor(Color.rgb(132, 159, 190));
        prompt.setHint("Describe your task in plain language...");
        prompt.setGravity(Gravity.TOP | Gravity.START);
        prompt.setSingleLine(false);
        prompt.setMaxLines(3);
        prompt.setHorizontallyScrolling(false);
        prompt.setVerticalScrollBarEnabled(false);
        prompt.setOverScrollMode(View.OVER_SCROLL_NEVER);
        prompt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        prompt.setBackgroundColor(Color.TRANSPARENT);
        root.addView(prompt, new FrameLayout.LayoutParams(1, 1));

        contextAction = action("Context options");
        contextAction.setOnClickListener(v -> toast("Context tools are not connected in this build"));
        root.addView(contextAction, new FrameLayout.LayoutParams(1, 1));
        mic = action("Voice input");
        mic.setOnClickListener(v -> toast("Voice input is not connected in this build"));
        root.addView(mic, new FrameLayout.LayoutParams(1, 1));
        attachAction = action("Attach files");
        attachAction.setOnClickListener(v -> toast("Attachments are not connected in this build"));
        root.addView(attachAction, new FrameLayout.LayoutParams(1, 1));
        send = action("Launch mission");
        send.setOnClickListener(v -> startMission());
        root.addView(send, new FrameLayout.LayoutParams(1, 1));
        modeAction = action("Mission mode");
        modeAction.setOnClickListener(v -> { deepMode = !deepMode; surface.invalidate(); });
        root.addView(modeAction, new FrameLayout.LayoutParams(1, 1));

        setContentView(root);
        root.post(this::positionHomeControls);
    }

    private TextView action(String description) {
        TextView v = new TextView(this);
        v.setText("");
        v.setTextColor(Color.TRANSPARENT);
        v.setBackgroundColor(Color.TRANSPARENT);
        v.setContentDescription(description);
        return v;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void positionHomeControls() {
        if (surface == null || surface.getWidth() <= 0 || surface.getHeight() <= 0) return;
        float s = scale();
        int ox = Math.round((surface.getWidth() - 390f * s) * .5f);
        place(coreView, ox + round(244 * s), round(93 * s), round(122 * s), round(136 * s));
        place(prompt, ox + round(27 * s), round(310 * s), round(338 * s), round(56 * s));
        place(modeAction, ox + round(286 * s), round(269 * s), round(80 * s), round(34 * s));
        place(contextAction, ox + round(26 * s), round(369 * s), round(76 * s), round(40 * s));
        place(mic, ox + round(108 * s), round(369 * s), round(76 * s), round(40 * s));
        place(attachAction, ox + round(190 * s), round(369 * s), round(76 * s), round(40 * s));
        place(send, ox + round(271 * s), round(369 * s), round(94 * s), round(40 * s));
        prompt.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * s);
        prompt.setPadding(round(12 * s), round(9 * s), round(10 * s), round(6 * s));
    }

    private int round(float n) { return Math.round(n); }

    private void place(View v, int l, int t, int w, int h) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
        lp.width = w; lp.height = h; lp.leftMargin = l; lp.topMargin = t; v.setLayoutParams(lp);
    }

    private void startMission() {
        String value = prompt.getText().toString().trim();
        if (value.isEmpty()) {
            prompt.requestFocus();
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(prompt, InputMethodManager.SHOW_IMPLICIT);
            toast("Write a mission first");
            return;
        }
        surface.mission = value;
        surface.page = VeytrixView.RUNS;
        hideComposer();
        prompt.clearFocus();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(prompt.getWindowToken(), 0);
        surface.invalidate();
    }

    private void showComposer() {
        prompt.setVisibility(View.VISIBLE); contextAction.setVisibility(View.VISIBLE); mic.setVisibility(View.VISIBLE);
        attachAction.setVisibility(View.VISIBLE); send.setVisibility(View.VISIBLE); modeAction.setVisibility(View.VISIBLE);
        coreView.setVisibility(View.VISIBLE); positionHomeControls();
    }

    private void hideComposer() {
        prompt.setVisibility(View.GONE); contextAction.setVisibility(View.GONE); mic.setVisibility(View.GONE);
        attachAction.setVisibility(View.GONE); send.setVisibility(View.GONE); modeAction.setVisibility(View.GONE);
        coreView.setVisibility(View.GONE);
    }

    private float scale() { return Math.min(surface.getWidth() / 390f, surface.getHeight() / 844f); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }

    private final class VeytrixView extends View {
        static final int HOME = 0, RUNS = 1, DETAILS = 2, ARTIFACTS = 3, TOOLS = 4, PROFILE = 5, SETTINGS = 6, VOICE = 7, COMPLETED = 8;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int page = HOME;
        private boolean drawerOpen = false;
        private float drawerProgress = 0f, drawerTarget = 0f;
        private boolean paused = false;
        private boolean localNotifications = true;
        private boolean localReducedMotion = false;
        private String mission = "";
        private long startedAt = System.currentTimeMillis();

        VeytrixView(Context context) {
            super(context);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        private float X(float v) { return (getWidth() - 390f * scale()) * .5f + v * scale(); }
        private float Y(float v) { return v * scale(); }

        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); postInvalidateOnAnimation(); }
        @Override protected void onDetachedFromWindow() { removeCallbacks(frame); super.onDetachedFromWindow(); }
        private final Runnable frame = this::invalidate;

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) { super.onSizeChanged(w, h, ow, oh); post(MainActivity.this::positionHomeControls); }

        @Override protected void onDraw(Canvas c) {
            drawBackground(c);
            switch (page) {
                case HOME: drawHome(c); break;
                case RUNS: drawRuns(c); break;
                case DETAILS: drawDetails(c); break;
                case ARTIFACTS: drawArtifacts(c); break;
                case TOOLS: drawTools(c); break;
                case PROFILE: drawProfile(c); break;
                case SETTINGS: drawSettings(c); break;
                case VOICE: drawVoice(c); break;
                default: drawCompleted(c); break;
            }
            drawBottomNav(c);
            animateDrawer(c);
        }

        private void animateDrawer(Canvas c) {
            drawerProgress += (drawerTarget - drawerProgress) * 0.22f;
            if (Math.abs(drawerTarget - drawerProgress) > .01f) postInvalidateOnAnimation();
            if (drawerProgress > .001f) drawDrawer(c, drawerProgress);
        }

        private void drawBackground(Canvas c) {
            p.setShader(new LinearGradient(0, 0, 0, getHeight(), new int[]{Color.rgb(3,9,18), Color.rgb(4,15,28), Color.rgb(2,8,16)}, null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, getWidth(), getHeight(), p); p.setShader(null);
            p.setShader(new RadialGradient(X(320), Y(120), X(170), new int[]{Color.argb(34,73,133,255), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
            c.drawCircle(X(320), Y(120), X(170), p); p.setShader(null);
            p.setShader(new RadialGradient(X(68), Y(670), X(150), new int[]{Color.argb(18,151,100,255), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
            c.drawCircle(X(68), Y(670), X(150), p); p.setShader(null);
        }

        private void drawHome(Canvas c) {
            round(c,16,16,374,76,SURFACE,BORDER,18); icon(c,38,46,IC_MENU,MUTED);
            drawLogo(c,145,42); txt(c,"VEYTRIX",21,TEXT,205,46,true,true); txt(c,"AUTONOMOUS AI DEVELOPMENT",7.2f,MUTED,205,62,true,false);
            round(c,309,28,343,62,SURFACE_2,BORDER,17); icon(c,326,45,IC_BELL,TEXT); dot(c,337,29,3,RED);
            round(c,349,28,374,62,SURFACE_2,BORDER,17); txt(c,"FH",10.5f,TEXT,361.5f,50,true,true); dot(c,368,55,3.5f,GREEN);

            txt(c,"Build",29,TEXT,24,115,false,true); txt(c,"Without",29,Color.rgb(196,180,255),24,145,false,true); txt(c,"Limits.",29,Color.rgb(211,235,255),24,175,false,true);
            txt(c,"Turn an idea into a real mission with a clear, guided workflow.",8.2f,MUTED,24,198,false,false);
            pill(c,24,215,143,246,"AI CORE  •  ONLINE",GREEN);
            txt(c,"THINK",6.8f,SUBTLE,346,105,true,false); txt(c,"PLAN",6.8f,SUBTLE,346,119,true,false); txt(c,"EXECUTE",6.8f,SUBTLE,346,133,true,false); txt(c,"VERIFY",6.8f,SUBTLE,346,147,true,false);

            round(c,16,258,374,416,SURFACE,BORDER,20); icon(c,35,282,IC_SPARK,VIOLET); txt(c,"MISSION / COMMAND",9.4f,TEXT,51,287,false,true);
            pill(c,287,270,366,302,deepMode?"Deep Mode":"Quick Mode",VIOLET); icon(c,352,286,IC_CHEVRON,MUTED);
            round(c,27,309,363,366,Color.rgb(4,13,25),Color.rgb(44,76,116),14);
            composerLabel(c,39,390,"Context",IC_CONTEXT,TEXT); composerLabel(c,121,390,"Voice",IC_MIC,BLUE); composerLabel(c,205,390,"Attach",IC_ATTACH,TEXT);
            gradientButton(c,271,369,365,409,"Launch",IC_PLAY);

            sectionTitle(c,"Core capabilities",16,438); feature(c,16,456,"Smart Plan","Break down ideas",IC_PLAN,VIOLET); feature(c,108,456,"Multi-Agent","Execute with AI",IC_AGENT,BLUE); feature(c,200,456,"Auto Test","Verify securely",IC_CHECK,CYAN); feature(c,292,456,"Deploy","Ship to production",IC_DEPLOY,GREEN);
            round(c,16,548,374,610,SURFACE_2,BORDER,18); metric(c,28,579,"128","Missions",IC_ACTIVITY); metric(c,119,579,"24","Projects",IC_FOLDER); metric(c,210,579,"98%","Success",IC_CHECK); metric(c,301,579,"2.4x","Faster",IC_CLOCK);
            cLine(c,105,560,105,598,Color.rgb(43,73,104)); cLine(c,195,560,195,598,Color.rgb(43,73,104)); cLine(c,285,560,285,598,Color.rgb(43,73,104));

            sectionTitle(c,"Recent activity",16,634); pill(c,313,620,366,646,"View all",BLUE);
            activity(c,16,655,"Build authentication system","Completed successfully","2h ago",GREEN,IC_CODE);
            activity(c,16,701,"Design modern UI components","24 components generated","5h ago",BLUE,IC_DOC);
            activity(c,16,747,"Optimize database queries","Performance improved by 70%","1d ago",VIOLET,IC_GEAR);
        }

        private void drawRuns(Canvas c) {
            topBar(c,"Activity",false); pill(c,18,84,86,113,"4 active",BLUE); pill(c,93,84,181,113,"12 completed",GREEN); pill(c,188,84,270,113,"1 failed",RED);
            sectionTitle(c,"Current mission",18,145); round(c,18,160,372,254,SURFACE,BORDER,18);
            icon(c,41,187,IC_RUN,BLUE); txt(c, mission.isEmpty()?"Create a research report on renewable energy trends":mission,12.4f,TEXT,61,186,false,true); pill(c,61,199,126,224,paused?"Paused":"Running",paused?AMBER:GREEN); txt(c,"Planning → Executing → Verifying",7.1f,MUTED,61,243,false,false); progress(c,61,230,341,BLUE, paused?.46f:.68f); icon(c,345,186,IC_CHEVRON,MUTED);
            sectionTitle(c,"Recent runs",18,282);
            runRow(c,18,295,"Build authentication system","Completed successfully","2h ago",GREEN,"98%",IC_CODE);
            runRow(c,18,380,"Design modern UI components","Generated 24 components","5h ago",GREEN,"100%",IC_DOC);
            runRow(c,18,465,"Optimize database queries","Performance improved by 70%","1d ago",BLUE,"91%",IC_GEAR);
            runRow(c,18,550,"Security review","Needs attention","2d ago",RED,"42%",IC_SHIELD);
            sectionTitle(c,"Run stages",18,660); stage(c,18,678,"Planning","Scope understood",GREEN,true); stage(c,18,713,"Executing","Agents working",BLUE,true); stage(c,18,748,"Verifying","Awaiting final checks",SUBTLE,false);
        }

        private void drawDetails(Canvas c) {
            topBar(c,"Mission details",true); icon(c,24,91,IC_RUN,BLUE); txt(c,mission.isEmpty()?"Authentication system":"Mission run",15,TEXT,47,96,false,true); pill(c,286,79,366,106,paused?"Paused":"Running",paused?AMBER:GREEN);
            cardTitle(c,18,124,"Mission summary",IC_DOC); drawWrapped(c,mission.isEmpty()?"No mission text submitted yet.":mission,8.2f,MUTED,18,161,352,13);
            round(c,18,210,372,274,SURFACE,BORDER,16); txt(c,"OVERALL PROGRESS",7,SUBTLE,32,234,false,true); txt(c,paused?"46%":"68%",24,TEXT,32,262,false,true); progress(c,32,253,357,paused?AMBER:BLUE,paused?.46f:.68f); txt(c,"Current stage",7,SUBTLE,232,233,false,false); txt(c,paused?"Paused":"Executing",11,TEXT,232,252,false,true);
            cardTitle(c,18,294,"Execution timeline",IC_ACTIVITY); timeline(c,18,332,"Understanding objective","Scope and constraints captured","Completed",GREEN); timeline(c,18,379,"Planning approach","Execution plan assembled","Completed",GREEN); timeline(c,18,426,"Executing tasks","Agents are processing the mission","Active",BLUE); timeline(c,18,473,"Verifying results","Security and correctness checks","Pending",SUBTLE);
            round(c,18,520,372,604,SURFACE_2,BORDER,16); icon(c,37,548,IC_SHIELD,GREEN); txt(c,"Verification state",11,TEXT,58,546,false,true); txt(c,"No verification failure reported",7.6f,MUTED,58,563,false,false); button(c,32,576,174,599,"Open results",IC_FOLDER,BLUE,false); button(c,183,576,357,599,"Complete state",IC_CHECK,GREEN,false);
        }

        private void drawArtifacts(Canvas c) {
            topBar(c,"Results",false); sectionTitle(c,"Generated outputs",18,90); txt(c,"Artifacts created by the current mission",7.6f,MUTED,18,106,false,false);
            artifact(c,18,124,"mission-plan.json","JSON","12 KB",GREEN,IC_CODE); artifact(c,18,198,"verification-report.txt","REPORT","8 KB",GREEN,IC_DOC); artifact(c,18,272,"build-output.apk","ANDROID","4.8 MB",BLUE,IC_PACKAGE); artifact(c,18,346,"execution.log","LOG","96 KB",SUBTLE,IC_LIST);
            round(c,18,430,372,510,SURFACE_2,BORDER,16); icon(c,39,457,IC_SHIELD,GREEN); txt(c,"Verification output",11,TEXT,60,455,false,true); txt(c,"Checks completed for available local state",7.4f,MUTED,60,472,false,false); pill(c,60,484,151,506,"Verified",GREEN);
            sectionTitle(c,"Artifact actions",18,544); button(c,18,565,185,604,"Open selected",IC_OPEN,BLUE,false); button(c,195,565,372,604,"Share / export",IC_SHARE,MUTED,false); txt(c,"Opening/exporting artifacts is not connected in this build.",6.9f,SUBTLE,18,626,false,false);
        }

        private void drawTools(Canvas c) {
            topBar(c,"Control",false); sectionTitle(c,"Mission control",18,90); toolCard(c,18,106,"Pause or resume mission","Controls the current local run state",IC_PAUSE,paused?"Resume":"Pause",AMBER); toolCard(c,18,182,"Mission details","Inspect stages, progress and verification",IC_ACTIVITY,"Open",BLUE); toolCard(c,18,258,"Results","Review generated outputs and logs",IC_FOLDER,"Open",BLUE);
            sectionTitle(c,"System",18,354); infoRow(c,18,371,"AI orchestration","Ready",GREEN,IC_SPARK); infoRow(c,18,427,"Native Android UI","Active",GREEN,IC_PHONE); infoRow(c,18,483,"Backend actions","Preserved",MUTED,IC_SERVER); infoRow(c,18,539,"Voice / attachments","Not connected",SUBTLE,IC_MIC);
            round(c,18,612,372,670,SURFACE_2,BORDER,16); txt(c,"Control surface",11,TEXT,34,638,false,true); txt(c,"Use the existing mission flow for real actions.",7.2f,MUTED,34,654,false,false);
        }

        private void drawProfile(Canvas c) {
            topBar(c,"Profile",false); round(c,18,84,372,183,SURFACE,BORDER,20); round(c,34,99,88,153,SURFACE_3,Color.rgb(60,98,144),18); txt(c,"FH",16,TEXT,61,133,true,true); dot(c,76,146,4,GREEN); txt(c,"Fahad Hussain",15,TEXT,106,115,false,true); txt(c,"VEYTRIX user",8,MUTED,106,132,false,false); pill(c,106,145,171,170,"Pro",VIOLET);
            sectionTitle(c,"Activity summary",18,208); stat(c,18,224,"128","Missions",IC_ACTIVITY); stat(c,145,224,"24","Projects",IC_FOLDER); stat(c,272,224,"98%","Success",IC_CHECK);
            sectionTitle(c,"Account information",18,312); infoRow(c,18,329,"Account","Active",GREEN,IC_USER); infoRow(c,18,385,"Identity","FH",MUTED,IC_USER); infoRow(c,18,441,"Workspace","Veytrix Autopilot",MUTED,IC_FOLDER);
            sectionTitle(c,"Account actions",18,514); button(c,18,532,181,570,"Settings",IC_GEAR,BLUE,false); button(c,191,532,372,570,"Sign out",IC_LOGOUT,RED,false);
        }

        private void drawSettings(Canvas c) {
            topBar(c,"Settings",false); sectionTitle(c,"General",18,90); setting(c,18,107,"Appearance","Dark",IC_SUN); setting(c,18,157,"Mission / AI","Deep Mode",IC_SPARK); setting(c,18,207,"Notifications",localNotifications?"Enabled":"Muted",IC_BELL); sectionTitle(c,"Privacy",18,271); setting(c,18,288,"Data surface","Local UI state only",IC_SHIELD); setting(c,18,338,"Support","Available in-app",IC_HELP); sectionTitle(c,"About",18,402); infoRow(c,18,420,"Version","1.0.0",MUTED,IC_INFO); infoRow(c,18,475,"Platform","Native Android",GREEN,IC_PHONE); infoRow(c,18,530,"Motion","Standard",localReducedMotion?AMBER:MUTED,IC_ACTIVITY); round(c,18,596,372,657,SURFACE_2,BORDER,16); txt(c,"VEYTRIX",12,TEXT,34,620,false,true); txt(c,"Autonomous engineering control",7.4f,MUTED,34,638,false,false);
        }

        private void drawVoice(Canvas c) {
            topBar(c,"Voice",false); round(c,18,84,372,166,SURFACE,BORDER,20); icon(c,40,114,IC_MIC,BLUE); txt(c,"Voice input",13,TEXT,63,117,false,true); pill(c,63,129,129,154,"Idle",SUBTLE);
            round(c,18,182,372,392,SURFACE_2,BORDER,22); drawMicOrb(c,195,271); txt(c,"Ready for voice input",13,TEXT,195,329,true,true); txt(c,"The voice action is intentionally unconnected in this build.",7.6f,MUTED,195,350,true,false); button(c,98,361,292,388,"Voice unavailable",IC_MIC,SUBTLE,false);
            sectionTitle(c,"Voice states",18,430); stateCard(c,18,448,"Idle","Ready for input",SUBTLE); stateCard(c,18,501,"Listening","Awaiting connection",BLUE); stateCard(c,18,554,"Processing","Awaiting connection",VIOLET);
        }

        private void drawCompleted(Canvas c) {
            topBar(c,"Completed",true); round(c,18,84,372,181,SURFACE,BORDER,20); icon(c,43,116,IC_CHECK,GREEN); txt(c,"Mission completed",14,TEXT,64,118,false,true); pill(c,64,131,138,156,"Verified",GREEN); txt(c,mission.isEmpty()?"Build authentication system":mission,8.4f,MUTED,64,169,false,false);
            round(c,18,196,372,297,SURFACE_2,BORDER,18); txt(c,"SUMMARY",7,SUBTLE,34,219,false,true); drawWrapped(c,"The run reached its completed state and the available verification summary is ready for review.",8.2f,MUTED,34,239,314,13); progress(c,34,275,350,GREEN,1f);
            sectionTitle(c,"Verification",18,324); infoRow(c,18,341,"Correctness checks","Passed",GREEN,IC_CHECK); infoRow(c,18,397,"Security checks","Passed",GREEN,IC_SHIELD); infoRow(c,18,453,"Generated results","Available",BLUE,IC_FOLDER);
            sectionTitle(c,"Next actions",18,522); button(c,18,540,183,580,"Open results",IC_FOLDER,BLUE,false); button(c,191,540,372,580,"New mission",IC_PLUS,VIOLET,false);
        }

        private void drawBottomNav(Canvas c) {
            round(c,12,784,378,840,Color.rgb(5,14,26),Color.rgb(41,72,109),20); nav(c,50,"Home",HOME,0); nav(c,124,"Activity",RUNS,1); nav(c,198,"Results",ARTIFACTS,2); nav(c,272,"Control",TOOLS,3); nav(c,346,"More",PROFILE,4);
        }

        private void nav(Canvas c,float x,String label,int target,int kind){ boolean active=page==target; int col=active?TEXT:MUTED; if(active){ p.setShader(new RadialGradient(X(x),Y(801),X(24),new int[]{Color.argb(60,130,95,255),Color.TRANSPARENT},null,Shader.TileMode.CLAMP)); c.drawCircle(X(x),Y(801),X(24),p); p.setShader(null);} icon(c,x,800,new int[]{IC_HOME,IC_ACTIVITY,IC_FOLDER,IC_SLIDERS,IC_MORE}[kind],col); txt(c,label,6.7f,col,x,828,true,active); }

        private void drawDrawer(Canvas c,float prog) {
            p.setColor(Color.argb((int)(135*prog),0,0,0)); c.drawRect(0,0,getWidth(),getHeight(),p);
            float left=X(-310 + 310*prog); float right=left+X(310);
            p.setColor(Color.rgb(5,14,26)); c.drawRoundRect(new RectF(left,0,right,getHeight()),0,0,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(X(.7f)); p.setColor(Color.rgb(53,88,130)); c.drawLine(right,0,right,getHeight(),p); p.setStyle(Paint.Style.FILL);
            float ox= -310+310*prog;
            drawBrandMark(c,ox+49,44); txtAt(c,"VEYTRIX",15,TEXT,ox+75,45,false,true); txtAt(c,"AUTONOMOUS CONTROL",6.8f,MUTED,ox+75,58,false,false);
            drawerItem(c,ox+20,92,"Home",IC_HOME,HOME, page==HOME); drawerItem(c,ox+20,144,"Activity",IC_ACTIVITY,RUNS,page==RUNS); drawerItem(c,ox+20,196,"Results",IC_FOLDER,ARTIFACTS,page==ARTIFACTS); drawerItem(c,ox+20,248,"Control",IC_SLIDERS,TOOLS,page==TOOLS); drawerItem(c,ox+20,300,"Voice",IC_MIC,VOICE,page==VOICE); drawerItem(c,ox+20,352,"Profile",IC_USER,PROFILE,page==PROFILE);
            cLineAt(c,ox+20,414,ox+288,414,Color.rgb(39,67,100)); txtAt(c,"Workspace",7,SUBTLE,ox+24,438,false,true); drawerItem(c,ox+20,450,"Settings",IC_GEAR,SETTINGS,page==SETTINGS); drawerItem(c,ox+20,502,"Support",IC_HELP,SETTINGS,false);
            roundAt(c,ox+20,580,ox+288,646,SURFACE_2,BORDER,16); dotAt(c,ox+38,603,4,GREEN); txtAt(c,"System status",8.2f,TEXT,ox+52,608,false,true); txtAt(c,"Native Android • Ready",6.9f,MUTED,ox+52,625,false,false);
        }

        private void drawerItem(Canvas c,float x,float y,String label,int k,int target,boolean active){ if(active) round(c,x,y,x+268,y+42,Color.rgb(11,30,49),Color.rgb(53,96,148),12); icon(c,x+23,y+21,k,active?TEXT:MUTED); txt(c,label,9.1f,active?TEXT:MUTED,x+46,y+26,false,active); }

        private void topBar(Canvas c,String title,boolean back){ round(c,16,16,374,66,SURFACE,BORDER,18); icon(c,38,41,back?IC_BACK:IC_MENU,MUTED); txt(c,title,15,TEXT,66,47,false,true); txt(c,"VEYTRIX",7,SUBTLE,66,61,false,false); round(c,332,27,366,58,SURFACE_2,BORDER,16); txt(c,"FH",9,TEXT,349,48,true,true); }
        private void sectionTitle(Canvas c,String t,float x,float y){ txt(c,t,11.2f,TEXT,x,y,false,true); }
        private void cardTitle(Canvas c,float x,float y,String t,int k){ round(c,x,y,x+354,y+48,SURFACE,BORDER,15); icon(c,x+18,y+24,k,BLUE); txt(c,t,10.4f,TEXT,x+40,y+29,false,true); }

        private void feature(Canvas c,float x,float y,String title,String sub,int k,int col){ round(c,x,y,x+82,y+77,SURFACE,BORDER,15); icon(c,x+18,y+20,k,col); txt(c,title,7.4f,TEXT,x+10,y+45,false,true); txt(c,sub,5.8f,MUTED,x+10,y+60,false,false); icon(c,x+70,y+19,IC_CHEVRON,SUBTLE); }
        private void activity(Canvas c,float x,float y,String title,String sub,String time,int col,int k){ round(c,x,y,374,y+39,SURFACE,BORDER,12); round(c,x+8,y+7,x+36,y+32,Color.argb(90,Color.red(col),Color.green(col),Color.blue(col)),Color.TRANSPARENT,8); icon(c,x+22,y+20,k,Color.WHITE); txt(c,title,6.8f,TEXT,x+46,y+15,false,true); txt(c,sub,5.8f,MUTED,x+46,y+28,false,false); txt(c,time,5.8f,SUBTLE,339,y+15,true,false); }
        private void runRow(Canvas c,float x,float y,String title,String sub,String time,int col,String pct,int k){ round(c,x,y,372,y+72,SURFACE,BORDER,15); icon(c,x+22,y+24,k,col); txt(c,title,9.1f,TEXT,x+43,y+21,false,true); txt(c,sub,6.6f,MUTED,x+43,y+38,false,false); txt(c,time,6.1f,SUBTLE,340,y+18,true,false); progress(c,43,y+49,288,col,Float.parseFloat(pct.replace("%",""))/100f); txt(c,pct,6.6f,col,334,y+52,true,true); }
        private void stage(Canvas c,float x,float y,String title,String sub,int col,boolean done){ dot(c,x+7,y,5,col); txt(c,title,8.5f,TEXT,x+22,y+4,false,true); txt(c,sub,6.4f,MUTED,x+87,y+4,false,false); if(done) icon(c,346,y,IC_CHECK,col); }
        private void timeline(Canvas c,float x,float y,String title,String sub,String status,int col){ dot(c,x+10,y,5,col); txt(c,title,8.2f,TEXT,x+26,y+4,false,true); txt(c,sub,6.5f,MUTED,x+26,y+18,false,false); pill(c,286,y-10,360,y+12,status,col); }
        private void artifact(Canvas c,float x,float y,String name,String type,String size,int col,int k){ round(c,x,y,372,y+60,SURFACE,BORDER,14); icon(c,x+22,y+30,k,col); txt(c,name,8.5f,TEXT,x+42,y+24,false,true); txt(c,type+"  •  "+size,6.3f,MUTED,x+42,y+40,false,false); icon(c,346,y+30,IC_OPEN,SUBTLE); }
        private void toolCard(Canvas c,float x,float y,String title,String sub,int k,String action,int col){ round(c,x,y,372,y+60,SURFACE,BORDER,14); icon(c,x+22,y+30,k,col); txt(c,title,8.4f,TEXT,x+42,y+23,false,true); txt(c,sub,6.4f,MUTED,x+42,y+40,false,false); button(c,286,y+16,358,y+44,action,action.equals("Pause")?IC_PAUSE:IC_OPEN,col,false); }
        private void infoRow(Canvas c,float x,float y,String key,String value,int col,int k){ round(c,x,y,372,y+46,SURFACE,BORDER,12); icon(c,x+22,y+23,k,col); txt(c,key,7.6f,MUTED,x+42,y+27,false,false); txt(c,value,8.2f,TEXT,348,y+27,true,true); }
        private void stat(Canvas c,float x,float y,String v,String label,int k){ round(c,x,y,x+118,y+65,SURFACE,BORDER,14); icon(c,x+18,y+19,k,BLUE); txt(c,v,14,TEXT,x+18,y+44,false,true); txt(c,label,6.6f,MUTED,x+18,y+57,false,false); }
        private void setting(Canvas c,float x,float y,String key,String value,int k){ round(c,x,y,372,y+42,SURFACE,BORDER,12); icon(c,x+20,y+21,k,MUTED); txt(c,key,8.3f,TEXT,x+40,y+26,false,true); txt(c,value,7.0f,MUTED,348,y+26,true,false); }
        private void stateCard(Canvas c,float x,float y,String a,String b,int col){ round(c,x,y,372,y+44,SURFACE,BORDER,12); dot(c,x+20,y+22,4,col); txt(c,a,7.8f,TEXT,x+34,y+26,false,true); txt(c,b,6.8f,MUTED,348,y+26,true,false); }
        private void metric(Canvas c,float x,float y,String v,String label,int k){ icon(c,x+8,y-10,k,VIOLET); txt(c,v,11.4f,TEXT,x+29,y-4,false,true); txt(c,label,6.3f,MUTED,x+29,y+10,false,false); }
        private void progress(Canvas c,float l,float y,float r,int col,float value){ round(c,l,y,r,y+4,Color.rgb(29,48,70),Color.TRANSPARENT,2); round(c,l,y,l+(r-l)*Math.max(0,Math.min(1,value)),y+4,col,Color.TRANSPARENT,2); }
        private void pill(Canvas c,float l,float t,float r,float b,String s,int col){ round(c,l,t,r,b,Color.argb(28,Color.red(col),Color.green(col),Color.blue(col)),Color.argb(92,Color.red(col),Color.green(col),Color.blue(col)),10); txt(c,s,6.6f,col,(l+r)/2,t+12,true,true); }
        private void composerLabel(Canvas c,float x,float y,String s,int k,int col){ icon(c,x-13,y-1,k,col); txt(c,s,7.5f,TEXT,x+1,y+3,false,false); }
        private void gradientButton(Canvas c,float l,float t,float r,float b,String s,int k){ p.setShader(new LinearGradient(X(l),Y(t),X(r),Y(b),new int[]{Color.rgb(122,80,240),Color.rgb(71,131,255),Color.rgb(54,177,241)},null,Shader.TileMode.CLAMP)); c.drawRoundRect(new RectF(X(l),Y(t),X(r),Y(b)),X(12),X(12),p); p.setShader(null); icon(c,(l+r)/2-18,(t+b)/2,k,Color.WHITE); txt(c,s,8.2f,Color.WHITE,(l+r)/2+8,t+25,true,true); }
        private void button(Canvas c,float l,float t,float r,float b,String s,int k,int col,boolean disabled){ int fill=disabled?Color.rgb(17,28,42):Color.rgb(9,24,40); round(c,l,t,r,b,fill,Color.argb(120,Color.red(col),Color.green(col),Color.blue(col)),11); icon(c,l+18,(t+b)/2,k,disabled?SUBTLE:col); txt(c,s,7.4f,disabled?SUBTLE:TEXT,l+34,t+16,false,true); }
        private void round(Canvas c,float l,float t,float r,float b,int fill,int border,float rad){ p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(fill); RectF q=new RectF(X(l),Y(t),X(r),Y(b)); c.drawRoundRect(q,X(rad),X(rad),p); if(border!=Color.TRANSPARENT){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,X(.7f)));p.setColor(border);c.drawRoundRect(q,X(rad),X(rad),p);p.setStyle(Paint.Style.FILL);} }
        private void roundAt(Canvas c,float l,float t,float r,float b,int fill,int border,float rad){p.setColor(fill);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(X(l),Y(t),X(r),Y(b)),X(rad),X(rad),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(X(.7f));p.setColor(border);c.drawRoundRect(new RectF(X(l),Y(t),X(r),Y(b)),X(rad),X(rad),p);p.setStyle(Paint.Style.FILL);}
        private void txt(Canvas c,String s,float sz,int col,float x,float y,boolean center,boolean bold){p.setShader(null);p.setColor(col);p.setStyle(Paint.Style.FILL);p.setTextSize(X(sz));p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,X(x),Y(y),p);}
        private void txtAt(Canvas c,String s,float sz,int col,float x,float y,boolean center,boolean bold){p.setColor(col);p.setTextSize(X(sz));p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,X(x),Y(y),p);}
        private void drawWrapped(Canvas c,String s,float sz,int col,float x,float y,float max,float line){p.setTextSize(X(sz));String[] w=s.split(" ");StringBuilder b=new StringBuilder();float yy=y;for(String z:w){String n=b.length()==0?z:b+" "+z;if(p.measureText(n)>X(max)&&b.length()>0){txt(c,b.toString(),sz,col,x,yy,false,false);b=new StringBuilder(z);yy+=line;}else b=new StringBuilder(n);}if(b.length()>0)txt(c,b.toString(),sz,col,x,yy,false,false);}
        private void cLine(Canvas c,float x1,float y1,float x2,float y2,int col){stroke.setColor(col);stroke.setStrokeWidth(X(.7f));c.drawLine(X(x1),Y(y1),X(x2),Y(y2),stroke);}
        private void cLineAt(Canvas c,float x1,float y1,float x2,float y2,int col){stroke.setColor(col);stroke.setStrokeWidth(X(.7f));c.drawLine(X(x1),Y(y1),X(x2),Y(y2),stroke);}
        private void dot(Canvas c,float x,float y,float r,int col){p.setStyle(Paint.Style.FILL);p.setColor(col);c.drawCircle(X(x),Y(y),X(r),p);}
        private void dotAt(Canvas c,float x,float y,float r,int col){p.setColor(col);c.drawCircle(X(x),Y(y),X(r),p);}
        private void drawLogo(Canvas c,float x,float y){drawBrandMark(c,x,y);}
        private void drawBrandMark(Canvas c,float x,float y){Path q=new Path();q.moveTo(X(x-13),Y(y-13));q.lineTo(X(x-4),Y(y+3));q.lineTo(X(x),Y(y+15));q.lineTo(X(x+13),Y(y-13));q.lineTo(X(x+5),Y(y-13));q.lineTo(X(x),Y(y+1));q.lineTo(X(x-5),Y(y-13));q.close();p.setShader(new LinearGradient(X(x-13),Y(y-13),X(x+13),Y(y+15),new int[]{CYAN,VIOLET,Color.rgb(210,80,245)},null,Shader.TileMode.CLAMP));c.drawPath(q,p);p.setShader(null);}
        private void drawMicOrb(Canvas c,float x,float y){p.setShader(new RadialGradient(X(x),Y(y),X(58),new int[]{Color.argb(60,72,216,248),Color.argb(18,151,100,255),Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawCircle(X(x),Y(y),X(58),p);p.setShader(null);round(c,x-24,y-30,x+24,y+30,Color.rgb(10,29,49),Color.rgb(66,119,169),20);icon(c,x,y,IC_MIC,BLUE);}

        private static final int IC_MENU=1,IC_BELL=2,IC_HOME=3,IC_ACTIVITY=4,IC_FOLDER=5,IC_SLIDERS=6,IC_MORE=7,IC_RUN=8,IC_DOC=9,IC_GEAR=10,IC_CHECK=11,IC_SHIELD=12,IC_MIC=13,IC_PACKAGE=14,IC_LIST=15,IC_OPEN=16,IC_SHARE=17,IC_PLAN=18,IC_AGENT=19,IC_CLOCK=20,IC_CODE=21,IC_DEPLOY=22,IC_SPARK=23,IC_CONTEXT=24,IC_ATTACH=25,IC_PLAY=26,IC_PAUSE=27,IC_PHONE=28,IC_SERVER=29,IC_USER=30,IC_LOGOUT=31,IC_SUN=32,IC_HELP=33,IC_INFO=34,IC_BACK=35,IC_PLUS=36,IC_CHEVRON=37;

        private void icon(Canvas c,float x,float y,int kind,int col){stroke.setColor(col);stroke.setStrokeWidth(X(1.6f));stroke.setStyle(Paint.Style.STROKE);Path q=new Path();
            switch(kind){
                case IC_MENU:c.drawLine(X(x-9),Y(y-6),X(x+9),Y(y-6),stroke);c.drawLine(X(x-9),Y(y),X(x+9),Y(y),stroke);c.drawLine(X(x-9),Y(y+6),X(x+9),Y(y+6),stroke);break;
                case IC_BELL:q.moveTo(X(x-7),Y(y+4));q.quadTo(X(x-5),Y(y-7),X(x),Y(y-7));q.quadTo(X(x+5),Y(y-7),X(x+7),Y(y+4));q.lineTo(X(x+9),Y(y+5));q.lineTo(X(x-9),Y(y+5));c.drawPath(q,stroke);c.drawLine(X(x-3),Y(y+9),X(x+3),Y(y+9),stroke);break;
                case IC_HOME:q.moveTo(X(x-9),Y(y));q.lineTo(X(x),Y(y-8));q.lineTo(X(x+9),Y(y));q.lineTo(X(x+7),Y(y));q.lineTo(X(x+7),Y(y+9));q.lineTo(X(x-7),Y(y+9));q.lineTo(X(x-7),Y(y));q.close();c.drawPath(q,stroke);break;
                case IC_ACTIVITY:c.drawLine(X(x-9),Y(y+7),X(x-3),Y(y-2),stroke);c.drawLine(X(x-3),Y(y-2),X(x+2),Y(y+2),stroke);c.drawLine(X(x+2),Y(y+2),X(x+9),Y(y-7),stroke);break;
                case IC_FOLDER:q.addRoundRect(new RectF(X(x-9),Y(y-6),X(x+9),Y(y+8)),X(2),X(2),Path.Direction.CW);q.moveTo(X(x-8),Y(y-6));q.lineTo(X(x-3),Y(y-10));q.lineTo(X(x+3),Y(y-10));q.lineTo(X(x+6),Y(y-6));c.drawPath(q,stroke);break;
                case IC_SLIDERS:c.drawLine(X(x-8),Y(y-6),X(x+8),Y(y-6),stroke);c.drawLine(X(x-8),Y(y),X(x+8),Y(y),stroke);c.drawLine(X(x-8),Y(y+6),X(x+8),Y(y+6),stroke);c.drawCircle(X(x-3),Y(y-6),X(2),stroke);c.drawCircle(X(x+3),Y(y),X(2),stroke);c.drawCircle(X(x-2),Y(y+6),X(2),stroke);break;
                case IC_MORE:c.drawCircle(X(x-6),Y(y),X(1.7f),p);c.drawCircle(X(x),Y(y),X(1.7f),p);c.drawCircle(X(x+6),Y(y),X(1.7f),p);break;
                case IC_RUN:c.drawCircle(X(x),Y(y),X(9),stroke);q.moveTo(X(x-2),Y(y-5));q.lineTo(X(x+5),Y(y));q.lineTo(X(x-2),Y(y+5));c.drawPath(q,stroke);break;
                case IC_DOC:q.addRoundRect(new RectF(X(x-7),Y(y-10),X(x+7),Y(y+10)),X(2),X(2),Path.Direction.CW);c.drawLine(X(x-3),Y(y-3),X(x+3),Y(y-3),stroke);c.drawLine(X(x-3),Y(y+2),X(x+4),Y(y+2),stroke);break;
                case IC_GEAR:c.drawCircle(X(x),Y(y),X(6),stroke);c.drawCircle(X(x),Y(y),X(2),stroke);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(X(x+(float)Math.cos(a)*8),Y(y+(float)Math.sin(a)*8),X(x+(float)Math.cos(a)*10),Y(y+(float)Math.sin(a)*10),stroke);}break;
                case IC_CHECK:q.moveTo(X(x-6),Y(y));q.lineTo(X(x-2),Y(y+4));q.lineTo(X(x+7),Y(y-6));c.drawPath(q,stroke);break;
                case IC_SHIELD:q.moveTo(X(x),Y(y-9));q.lineTo(X(x+8),Y(y-5));q.lineTo(X(x+6),Y(y+5));q.lineTo(X(x),Y(y+10));q.lineTo(X(x-6),Y(y+5));q.lineTo(X(x-8),Y(y-5));q.close();c.drawPath(q,stroke);break;
                case IC_MIC:c.drawRoundRect(new RectF(X(x-4),Y(y-9),X(x+4),Y(y+3)),X(4),X(4),stroke);c.drawArc(new RectF(X(x-8),Y(y-3),X(x+8),Y(y+8)),0,180,false,stroke);c.drawLine(X(x),Y(y+8),X(x),Y(y+11),stroke);break;
                case IC_PACKAGE:c.drawRect(new RectF(X(x-8),Y(y-7),X(x+8),Y(y+7)),stroke);c.drawLine(X(x-8),Y(y-3),X(x),Y(y+1),stroke);c.drawLine(X(x+8),Y(y-3),X(x),Y(y+1),stroke);break;
                case IC_LIST:for(int i=-1;i<=1;i++){c.drawLine(X(x-8),Y(y+i*6),X(x-4),Y(y+i*6),stroke);c.drawLine(X(x-1),Y(y+i*6),X(x+8),Y(y+i*6),stroke);}break;
                case IC_OPEN:q.moveTo(X(x-6),Y(y+2));q.lineTo(X(x+1),Y(y-5));c.drawPath(q,stroke);c.drawRect(new RectF(X(x-8),Y(y-2),X(x+4),Y(y+8)),stroke);c.drawLine(X(x+2),Y(y-7),X(x+8),Y(y-7),stroke);c.drawLine(X(x+8),Y(y-7),X(x+8),Y(y-1),stroke);break;
                case IC_SHARE:c.drawCircle(X(x-6),Y(y),X(3),stroke);c.drawCircle(X(x+6),Y(y-6),X(3),stroke);c.drawCircle(X(x+6),Y(y+6),X(3),stroke);c.drawLine(X(x-3),Y(y-1),X(x+3),Y(y-5),stroke);c.drawLine(X(x-3),Y(y+1),X(x+3),Y(y+5),stroke);break;
                case IC_PLAN:q.moveTo(X(x-9),Y(y));q.lineTo(X(x),Y(y-7));q.lineTo(X(x+9),Y(y));q.lineTo(X(x),Y(y+7));q.close();c.drawPath(q,stroke);break;
                case IC_AGENT:c.drawCircle(X(x-5),Y(y-3),X(4),stroke);c.drawCircle(X(x+5),Y(y-3),X(4),stroke);c.drawLine(X(x-7),Y(y+6),X(x+7),Y(y+6),stroke);break;
                case IC_CLOCK:c.drawCircle(X(x),Y(y),X(8),stroke);c.drawLine(X(x),Y(y),X(x),Y(y-5),stroke);c.drawLine(X(x),Y(y),X(x+4),Y(y+3),stroke);break;
                case IC_CODE:c.drawLine(X(x-8),Y(y),X(x-3),Y(y-4),stroke);c.drawLine(X(x-8),Y(y),X(x-3),Y(y+4),stroke);c.drawLine(X(x+8),Y(y),X(x+3),Y(y-4),stroke);c.drawLine(X(x+8),Y(y),X(x+3),Y(y+4),stroke);c.drawLine(X(x-1),Y(y+6),X(x+2),Y(y-6),stroke);break;
                case IC_DEPLOY:q.moveTo(X(x),Y(y-10));q.lineTo(X(x+8),Y(y+5));q.lineTo(X(x),Y(y+2));q.lineTo(X(x-8),Y(y+5));q.close();c.drawPath(q,stroke);c.drawCircle(X(x),Y(y-10),X(2),stroke);break;
                case IC_SPARK:q.moveTo(X(x),Y(y-9));q.lineTo(X(x+3),Y(y-3));q.lineTo(X(x+9),Y(y));q.lineTo(X(x+3),Y(y+3));q.lineTo(X(x),Y(y+9));q.lineTo(X(x-3),Y(y+3));q.lineTo(X(x-9),Y(y));q.lineTo(X(x-3),Y(y-3));q.close();c.drawPath(q,stroke);break;
                case IC_CONTEXT:q.moveTo(X(x-8),Y(y-4));q.lineTo(X(x),Y(y-9));q.lineTo(X(x+8),Y(y-4));q.lineTo(X(x),Y(y+1));q.close();c.drawPath(q,stroke);q.reset();q.moveTo(X(x-8),Y(y+2));q.lineTo(X(x),Y(y+7));q.lineTo(X(x+8),Y(y+2));c.drawPath(q,stroke);break;
                case IC_ATTACH:q.moveTo(X(x+5),Y(y-7));q.cubicTo(X(x+10),Y(y-2),X(x+7),Y(y+4),X(x+2),Y(y+8));q.cubicTo(X(x-4),Y(y+13),X(x-10),Y(y+7),X(x-5),Y(y+2));q.lineTo(X(x+4),Y(y-8));c.drawPath(q,stroke);break;
                case IC_PLAY:q.moveTo(X(x-5),Y(y-7));q.lineTo(X(x+7),Y(y));q.lineTo(X(x-5),Y(y+7));q.close();c.drawPath(q,stroke);break;
                case IC_PAUSE:c.drawRect(new RectF(X(x-6),Y(y-7),X(x-2),Y(y+7)),stroke);c.drawRect(new RectF(X(x+2),Y(y-7),X(x+6),Y(y+7)),stroke);break;
                case IC_PHONE:c.drawRoundRect(new RectF(X(x-7),Y(y-10),X(x+7),Y(y+10)),X(2),X(2),stroke);c.drawLine(X(x-3),Y(y+7),X(x+3),Y(y+7),stroke);break;
                case IC_SERVER:c.drawRoundRect(new RectF(X(x-8),Y(y-8),X(x+8),Y(y-2)),X(2),X(2),stroke);c.drawRoundRect(new RectF(X(x-8),Y(y+2),X(x+8),Y(y+8)),X(2),X(2),stroke);break;
                case IC_USER:c.drawCircle(X(x),Y(y-4),X(4),stroke);q.addArc(new RectF(X(x-8),Y(y+1),X(x+8),Y(y+11)),180,180);c.drawPath(q,stroke);break;
                case IC_LOGOUT:c.drawRect(new RectF(X(x-8),Y(y-8),X(x-3),Y(y+8)),stroke);c.drawLine(X(x-1),Y(y),X(x+8),Y(y),stroke);c.drawLine(X(x+8),Y(y),X(x+4),Y(y-4),stroke);c.drawLine(X(x+8),Y(y),X(x+4),Y(y+4),stroke);break;
                case IC_SUN:c.drawCircle(X(x),Y(y),X(5),stroke);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(X(x+(float)Math.cos(a)*8),Y(y+(float)Math.sin(a)*8),X(x+(float)Math.cos(a)*10),Y(y+(float)Math.sin(a)*10),stroke);}break;
                case IC_HELP:c.drawCircle(X(x),Y(y),X(8),stroke);txt(c,"?",9,col,x,y+4,true,true);break;
                case IC_INFO:c.drawCircle(X(x),Y(y),X(8),stroke);txt(c,"i",9,col,x,y+4,true,true);break;
                case IC_BACK:q.moveTo(X(x+5),Y(y-7));q.lineTo(X(x-3),Y(y));q.lineTo(X(x+5),Y(y+7));c.drawPath(q,stroke);break;
                case IC_PLUS:c.drawLine(X(x-7),Y(y),X(x+7),Y(y),stroke);c.drawLine(X(x),Y(y-7),X(x),Y(y+7),stroke);break;
                case IC_CHEVRON:q.moveTo(X(x-3),Y(y-4));q.lineTo(X(x+2),Y(y));q.lineTo(X(x-3),Y(y+4));c.drawPath(q,stroke);break;
            }
            stroke.setStyle(Paint.Style.STROKE);
        }

        @Override public boolean onTouchEvent(MotionEvent e){ if(e.getAction()!=MotionEvent.ACTION_UP)return true; float s=scale();float x=(e.getX()-(getWidth()-390*s)*.5f)/s;float y=e.getY()/s;
            if(drawerProgress>.55f){ if(x<300){handleDrawer(y);}else{drawerTarget=0;drawerOpen=false;} return true; }
            if(y>778){ if(x<82) select(HOME); else if(x<156) select(RUNS); else if(x<230) select(ARTIFACTS); else if(x<304) select(TOOLS); else select(PROFILE); return true; }
            if(page==HOME){ if(y<78&&x<78){openDrawer();return true;} if(y<78&&x>340){select(PROFILE);return true;} if(y>=650&&y<780){select(RUNS);return true;} if(y>=456&&y<540){if(x<98)select(RUNS);else if(x<190)select(TOOLS);else if(x<282)select(TOOLS);else select(ARTIFACTS);return true;} }
            else { if(y<72&&x<60){select(HOME);return true;} if(y<72&&x>325){select(PROFILE);return true;} }
            if(page==RUNS&&y>=160&&y<260){select(DETAILS);return true;} if(page==RUNS&&y>=295&&y<640){if(y<375)select(DETAILS);else if(y>465)select(COMPLETED);else select(DETAILS);return true;}
            if(page==DETAILS&&y>=575&&y<610){select(ARTIFACTS);return true;} if(page==DETAILS&&y>=520&&y<575){select(COMPLETED);return true;}
            if(page==ARTIFACTS&&y>=565&&y<610){toast("Artifact opening is not connected in this build");return true;} if(page==TOOLS&&y>=106&&y<166){paused=!paused;invalidate();return true;} if(page==TOOLS&&y>=180&&y<330){select(y<250?DETAILS:ARTIFACTS);return true;}
            if(page==PROFILE&&y>=520&&y<585){if(x<185)select(SETTINGS);else toast("Sign out is not connected in this build");return true;} if(page==SETTINGS&&y>=200&&y<255){localNotifications=!localNotifications;invalidate();return true;} if(page==VOICE&&y>=350&&y<405){toast("Voice input is not connected in this build");return true;} if(page==COMPLETED&&y>=535&&y<590){if(x<190)select(ARTIFACTS);else select(HOME);return true;} return true; }

        private void handleDrawer(float y){ if(y>=82&&y<132)select(HOME); else if(y<184)select(RUNS); else if(y<236)select(ARTIFACTS); else if(y<288)select(TOOLS); else if(y<340)select(VOICE); else if(y<392)select(PROFILE); else if(y>=438&&y<500)select(SETTINGS); else {drawerTarget=0;drawerOpen=false;} }
        private void openDrawer(){drawerOpen=true;drawerTarget=1;hideComposer();invalidate();}
        private void select(int target){page=target;drawerOpen=false;drawerTarget=0;if(target==HOME)showComposer();else hideComposer();invalidate();}
    }
}
