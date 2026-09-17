package com.veytrix.autopilot;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Native VEYTRIX flagship control surface.
 * This pass intentionally contains presentation state only; production automation APIs
 * remain outside the preview surface until the approved UI is locked.
 */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(11, 11, 10);
    private static final int PANEL = Color.rgb(24, 23, 20);
    private static final int PANEL_2 = Color.rgb(31, 29, 24);
    private static final int INK = Color.rgb(246, 241, 229);
    private static final int MUTED = Color.rgb(170, 164, 149);
    private static final int DIM = Color.rgb(109, 104, 93);
    private static final int LINE = Color.rgb(58, 54, 45);
    private static final int CHAMP = Color.rgb(216, 189, 135);
    private static final int CHAMP_2 = Color.rgb(240, 221, 186);
    private static final int VIOLET = Color.rgb(154, 134, 255);
    private static final int GREEN = Color.rgb(143, 210, 168);
    private static final int AMBER = Color.rgb(226, 182, 108);

    private final Map<String, String> navLabels = new LinkedHashMap<>();
    private FrameLayout root;
    private FrameLayout pageHost;
    private LinearLayout bottomNav;
    private FrameLayout drawerOverlay;
    private EditText missionInput;
    private String missionText = "";
    private String current = "command";
    private TextView contextLabel;
    private FlagshipCoreView coreView;
    private long startedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        navLabels.put("command", "Command");
        navLabels.put("live", "Live Run");
        navLabels.put("timeline", "Timeline");
        navLabels.put("artifacts", "Artifacts");
        navLabels.put("settings", "Settings");
        buildShell();
        show("command");
    }

    private void configureWindow() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    private void buildShell() {
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        root.addView(shell, match());

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(14), dp(10), dp(14), dp(10));
        topBar.setBackgroundColor(Color.rgb(17, 17, 15));
        shell.addView(topBar, new LinearLayout.LayoutParams(-1, dp(68)));

        TextView menu = text("☰", 22, INK);
        menu.setGravity(Gravity.CENTER);
        topBar.addView(menu, new LinearLayout.LayoutParams(dp(44), dp(44)));
        menu.setBackground(round(Color.rgb(22, 21, 18), LINE, 14));
        menu.setOnClickListener(v -> openDrawer());

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(14), 0, 0, 0);
        topBar.addView(brand, new LinearLayout.LayoutParams(0, -1, 1));
        TextView word = text("VEYTRIX", 12, CHAMP_2);
        word.setTypeface(Typeface.DEFAULT_BOLD);
        word.setLetterSpacing(.22f);
        brand.addView(word, wrap());
        contextLabel = text("Autonomous command system · Live", 11, DIM);
        brand.addView(contextLabel, wrap());

        TextView attention = button("Attention", false);
        topBar.addView(attention, new LinearLayout.LayoutParams(dp(102), dp(42)));
        attention.setOnClickListener(v -> show("failure"));

        TextView start = button("Start mission", true);
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(dp(130), dp(42));
        startLp.leftMargin = dp(8);
        topBar.addView(start, startLp);
        start.setOnClickListener(v -> focusPrompt());

        pageHost = new FrameLayout(this);
        pageHost.setPadding(dp(14), dp(12), dp(14), dp(82));
        shell.addView(pageHost, new LinearLayout.LayoutParams(-1, 0, 1));

        bottomNav = new LinearLayout(this);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(6), dp(6), dp(6), dp(6));
        bottomNav.setBackground(round(PANEL, Color.argb(30,255,255,255), 22));
        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(-1, dp(66), Gravity.BOTTOM);
        navLp.leftMargin = dp(8); navLp.rightMargin = dp(8); navLp.bottomMargin = dp(8);
        root.addView(bottomNav, navLp);
        populateBottomNav();

        buildDrawer();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            topBar.setPadding(dp(14), top + dp(5), dp(14), dp(5));
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) bottomNav.getLayoutParams();
            lp.bottomMargin = bottom + dp(8);
            bottomNav.setLayoutParams(lp);
            pageHost.setPadding(dp(14), dp(12), dp(14), bottom + dp(82));
            return insets;
        });
        setContentView(root);
    }

    private void populateBottomNav() {
        bottomNav.removeAllViews();
        addBottomItem("command", "⌂", "Command");
        addBottomItem("live", "◉", "Live");
        addBottomItem("timeline", "⌁", "Trace");
        addBottomItem("artifacts", "◇", "Outputs");
        addBottomItem("settings", "⚙", "Settings");
    }

    private void addBottomItem(String id, String icon, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setClickable(true);
        item.setPadding(dp(3), dp(2), dp(3), dp(2));
        TextView i = text(icon, 18, DIM); i.setGravity(Gravity.CENTER);
        item.addView(i, new LinearLayout.LayoutParams(-1, dp(28)));
        TextView l = text(label, 9, DIM); l.setGravity(Gravity.CENTER);
        item.addView(l, new LinearLayout.LayoutParams(-1, dp(22)));
        item.setTag(id);
        item.setOnClickListener(v -> show((String) v.getTag()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        bottomNav.addView(item, lp);
    }

    private void buildDrawer() {
        drawerOverlay = new FrameLayout(this);
        drawerOverlay.setBackgroundColor(Color.argb(170, 0, 0, 0));
        drawerOverlay.setVisibility(View.GONE);
        drawerOverlay.setOnClickListener(v -> closeDrawer());

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(20), dp(16), dp(16));
        panel.setBackground(round(Color.rgb(22,21,18), LINE, 0));
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(dp(322), -1, Gravity.START);
        drawerOverlay.addView(panel, panelLp);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(head, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView title = text("VEYTRIX", 13, CHAMP_2);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(.22f);
        head.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        TextView close = button("Close", false);
        head.addView(close, new LinearLayout.LayoutParams(dp(82), dp(40)));
        close.setOnClickListener(v -> closeDrawer());

        String[][] items = {
                {"command", "⌂", "Command Center"}, {"live", "◉", "Live Autopilot"},
                {"timeline", "⌁", "Execution Timeline"}, {"artifacts", "◇", "Artifacts"},
                {"verify", "✓", "Verification"}, {"failure", "!", "Recovery"},
                {"ai", "✦", "AI Engine"}, {"security", "◈", "Security"},
                {"settings", "⚙", "Settings"}, {"about", "i", "System / About"}
        };
        for (String[] row : items) {
            TextView b = text(row[1] + "   " + row[2], 13, MUTED);
            b.setGravity(Gravity.CENTER_VERTICAL);
            b.setPadding(dp(14), 0, dp(10), 0);
            b.setBackground(round(Color.TRANSPARENT, Color.TRANSPARENT, 15));
            panel.addView(b, new LinearLayout.LayoutParams(-1, dp(48)));
            b.setOnClickListener(v -> { closeDrawer(); show(row[0]); });
        }
        root.addView(drawerOverlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private void openDrawer() { drawerOverlay.setVisibility(View.VISIBLE); drawerOverlay.bringToFront(); }
    private void closeDrawer() { drawerOverlay.setVisibility(View.GONE); }

    private void show(String id) {
        current = id;
        pageHost.removeAllViews();
        View page;
        switch (id) {
            case "command": page = commandPage(); break;
            case "live": page = livePage(); break;
            case "timeline": page = timelinePage(); break;
            case "artifacts": page = artifactsPage(); break;
            case "verify": page = verifyPage(); break;
            case "failure": page = failurePage(); break;
            case "ai": page = aiPage(); break;
            case "security": page = securityPage(); break;
            case "about": page = aboutPage(); break;
            case "settings": page = settingsPage(); break;
            default: page = commandPage();
        }
        pageHost.addView(page, match());
        contextLabel.setText((navLabels.containsKey(id) ? navLabels.get(id) : titleFor(id)) + " · Live");
        updateBottomState();
    }

    private String titleFor(String id) {
        switch (id) {
            case "verify": return "Verification"; case "failure": return "Recovery";
            case "ai": return "AI Engine"; case "security": return "Security";
            case "about": return "System / About"; default: return "VEYTRIX";
        }
    }

    private void updateBottomState() {
        for (int n = 0; n < bottomNav.getChildCount(); n++) {
            View v = bottomNav.getChildAt(n);
            boolean on = current.equals(v.getTag());
            if (v instanceof LinearLayout) {
                ((TextView)((LinearLayout)v).getChildAt(0)).setTextColor(on ? CHAMP_2 : DIM);
                ((TextView)((LinearLayout)v).getChildAt(1)).setTextColor(on ? INK : DIM);
                v.setBackground(round(on ? Color.rgb(36,34,28) : Color.TRANSPARENT, Color.TRANSPARENT, 18));
            }
        }
    }

    private View commandPage() {
        LinearLayout page = pageColumn();
        addEyebrow(page, "CORE EXPERIENCE");
        TextView h = heading("Command Center", 29); page.addView(h, wrap());
        page.addView(text("One viewport. Full system state. Your intent stays first.", 13, MUTED), wrapWithBottom(12));

        LinearLayout promptCard = cardColumn();
        LinearLayout.LayoutParams pcp = new LinearLayout.LayoutParams(-1, 0, 1.23f);
        page.addView(promptCard, pcp);
        addEyebrow(promptCard, "MISSION PROMPT");
        TextView promptTitle = heading("What should VEYTRIX accomplish?", 22); promptCard.addView(promptTitle, wrapWithBottom(6));
        promptCard.addView(text("Describe the work. VEYTRIX will turn the intent into a traceable mission.", 12, MUTED), wrapWithBottom(10));

        FrameLayout inputWrap = new FrameLayout(this);
        inputWrap.setBackground(round(Color.rgb(12,12,10), LINE, 18));
        promptCard.addView(inputWrap, new LinearLayout.LayoutParams(-1, dp(120)));
        missionInput = new EditText(this);
        missionInput.setText(missionText);
        missionInput.setHint("e.g. Build the Android app, test it, repair failures, and prepare the verified APK.");
        missionInput.setHintTextColor(DIM);
        missionInput.setTextColor(INK);
        missionInput.setTextSize(14);
        missionInput.setGravity(Gravity.TOP | Gravity.START);
        missionInput.setPadding(dp(14), dp(14), dp(48), dp(14));
        missionInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputWrap.addView(missionInput, match());
        TextView mic = text("⌁", 21, CHAMP_2); mic.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams mlp = new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.END | Gravity.TOP);
        mlp.setMargins(0, dp(7), dp(7), 0); inputWrap.addView(mic, mlp);
        mic.setBackground(round(Color.rgb(33,31,26), LINE, 14));
        mic.setOnClickListener(v -> toast("Voice input is reserved for the next capability pass."));

        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL);
        promptCard.addView(actions, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView attach = button("Add context", false); actions.addView(attach, new LinearLayout.LayoutParams(dp(112), dp(42)));
        attach.setOnClickListener(v -> toast("Context picker is a preview interaction."));
        TextView go = button("Start mission", true); LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(0, dp(46), 1); glp.leftMargin = dp(8); actions.addView(go, glp);
        go.setOnClickListener(v -> startMission());

        LinearLayout telemetry = cardColumn();
        page.addView(telemetry, new LinearLayout.LayoutParams(-1, 0, .9f));
        rowTitle(telemetry, "System telemetry", "LIVE");
        LinearLayout grid = new LinearLayout(this); grid.setOrientation(LinearLayout.HORIZONTAL); telemetry.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        metric(grid, "Control health", "99.2%", "Stable"); metric(grid, "Active tasks", "08", "+2 queued");
        metric(grid, "Recovery", "01", "Contained"); metric(grid, "Verified", "24", "All signed");

        TextView recent = text("Last mission  ·  VX-1042  ·  Verified build 12s ago", 11, DIM); page.addView(recent, wrapWithTop(7));
        return page;
    }

    private void startMission() {
        String value = missionInput == null ? missionText : missionInput.getText().toString().trim();
        if (value.isEmpty()) { toast("Write a mission first."); focusPrompt(); return; }
        missionText = value; startedAt = SystemClock.elapsedRealtime(); show("live");
        toast("Mission staged in native preview.");
    }

    private void focusPrompt() {
        show("command");
        if (missionInput != null) { missionInput.requestFocus(); }
    }

    private View livePage() {
        LinearLayout page = pageColumn();
        addEyebrow(page, "MISSION · VX-1042");
        page.addView(heading("Live Autopilot", 27), wrap());
        page.addView(text(missionText.isEmpty() ? "The control surface stays calm while the system works." : missionText, 12, MUTED), wrapWithBottom(10));

        LinearLayout card = cardColumn(); page.addView(card, new LinearLayout.LayoutParams(-1, 0, 1.2f));
        rowTitle(card, "EXECUTION PROGRESS", "ACTIVE");
        LinearLayout center = new LinearLayout(this); center.setGravity(Gravity.CENTER); center.setOrientation(LinearLayout.VERTICAL); card.addView(center, new LinearLayout.LayoutParams(-1, 0, 1));
        coreView = new FlagshipCoreView(this); center.addView(coreView, new LinearLayout.LayoutParams(dp(250), dp(250)));
        TextView state = heading("Autopilot is executing", 21); center.addView(state, wrapWithTop(5));
        TextView phase = text("Observe  →  reason  →  act  →  verify", 12, MUTED); center.addView(phase, wrapWithBottom(10));
        TextView bar = text("72%   ·   Verification gate", 11, CHAMP_2); center.addView(bar, wrapWithBottom(7));

        LinearLayout controls = cardColumn(); page.addView(controls, new LinearLayout.LayoutParams(-1, 0, .78f));
        rowTitle(controls, "AUTOPILOT CONTROLS", "SAFE");
        actionRow(controls, "Pause autonomy", "Freezes new actions without clearing evidence.", "Pause", () -> toast("Autopilot paused in native preview."));
        actionRow(controls, "Recovery", "Inspect the contained warning.", "Inspect", () -> show("failure"));
        return page;
    }

    private View timelinePage() {
        LinearLayout page = pageColumn(); addEyebrow(page, "TRACEABLE AUTONOMOUS WORK"); page.addView(heading("Execution Timeline",27),wrap()); page.addView(text("Every decision has a visible place in the run.",13,MUTED),wrapWithBottom(10));
        LinearLayout card=cardColumn(); page.addView(card,new LinearLayout.LayoutParams(-1,0,1)); rowTitle(card,"VX-1042 timeline","LIVE");
        String[] steps={"01 · Intake","02 · Plan","03 · Prepare","04 · Execute","05 · Verify"}; String[] subs={"Intent parsed and normalized","Deterministic route selected","Workspace and credentials verified","Autonomous actions in progress","Evidence gates queued"};
        for(int i=0;i<steps.length;i++){LinearLayout r=infoRow(steps[i],subs[i]); TextView status=text(i<3?"DONE":i==3?"RUNNING":"QUEUED",10,i<3?GREEN:i==3?AMBER:MUTED); r.addView(status,new LinearLayout.LayoutParams(dp(70),-1)); card.addView(r,new LinearLayout.LayoutParams(-1,dp(58)));}
        return page;
    }

    private View artifactsPage() {
        LinearLayout page=pageColumn(); addEyebrow(page,"VERIFIED BUILD OUTPUTS"); page.addView(heading("Artifacts",27),wrap()); page.addView(text("Outputs are presented as evidence, not decoration.",13,MUTED),wrapWithBottom(10));
        LinearLayout card=cardColumn(); page.addView(card,new LinearLayout.LayoutParams(-1,0,1)); rowTitle(card,"Recent artifacts","24 VERIFIED");
        String[] names={"autopilot-debug.apk","release-report.json","execution-trace.txt","recovery-fingerprint.txt"}; String[] desc={"Android package · phone test","Machine-readable evidence","Mission trace · VX-1042","Failure classification output"};
        for(int i=0;i<names.length;i++){LinearLayout r=infoRow(names[i],desc[i]); TextView s=text(i==0?"VERIFIED":i==1?"SIGNED":i==3?"MATCH":"READY",10,GREEN); r.addView(s,new LinearLayout.LayoutParams(dp(74),-1)); card.addView(r,new LinearLayout.LayoutParams(-1,dp(60)));}
        TextView v=button("Open verification",false); card.addView(v,new LinearLayout.LayoutParams(-1,dp(44))); v.setOnClickListener(x->show("verify"));
        return page;
    }

    private View verifyPage(){return statePage("EVIDENCE BEFORE DELIVERY","Verification","PASS","All mandatory checks are represented as explicit gates.",new String[][]{{"Contract","Android contract satisfied"},{"Artifact","Expected asset present"},{"Identity","Package + launch activity matched"},{"Phone test","Ready for installation"}});}
    private View failurePage(){return statePage("FAILURE ISOLATED · REPAIR ACTIVE","Recovery Detail","ATTENTION","A transient warning is contained and does not block the primary mission.",new String[][]{{"Classification","Transient dependency response"},{"Retry policy","Backoff + verification"},{"Impact","None detected"},{"Next gate","Verification"}});}
    private View aiPage(){return statePage("DETERMINISTIC FIRST · ESCALATION WHEN REQUIRED","AI Engine","READY","Planner, router and escalation surfaces remain visible without exposing secrets.",new String[][]{{"Planner","Deterministic-first"},{"Router","Route selected"},{"Escalation","Available when required"},{"Evidence","Trace preserved"}});}
    private View securityPage(){return statePage("TRUST BOUNDARY & CREDENTIALS","Security","INTACT","Credentials remain outside the presentation layer; only state labels are shown.",new String[][]{{"Credentials","Not rendered"},{"Boundary","Enforced"},{"Package identity","com.veytrix.autopilot"},{"Artifact signing","Separate release path"}});}
    private View aboutPage(){return statePage("SYSTEM / ABOUT","VEYTRIX","NATIVE","Native Android flagship surface · preview-to-production pass.",new String[][]{{"UI layer","Native Views"},{"Core visual","Custom Canvas renderer"},{"WebView","Not used"},{"Mode","Presentation state only"}});}

    private View settingsPage(){
        LinearLayout page=pageColumn(); addEyebrow(page,"CONTROL PLANE PREFERENCES"); page.addView(heading("Settings",27),wrap()); page.addView(text("Premium shell controls, separated from production logic.",13,MUTED),wrapWithBottom(10));
        LinearLayout card=cardColumn(); page.addView(card,new LinearLayout.LayoutParams(-1,0,1));
        actionRow(card,"Reduce motion","Use shorter transitions","Toggle",()->toast("Reduce-motion preview toggled."));
        actionRow(card,"Haptics","Preview tactile feedback","Toggle",()->toast("Haptics preview toggled."));
        actionRow(card,"System detail","Show compact telemetry","Toggle",()->toast("System detail toggled."));
        actionRow(card,"About this build","Review native implementation state","Open",()->show("about"));
        return page;
    }

    private View statePage(String eyebrow,String title,String big,String copy,String[][] rows){
        LinearLayout page=pageColumn(); addEyebrow(page,eyebrow); page.addView(heading(title,27),wrap()); page.addView(text(copy,13,MUTED),wrapWithBottom(10));
        LinearLayout card=cardColumn(); page.addView(card,new LinearLayout.LayoutParams(-1,0,1)); TextView b=heading(big,45); b.setTextColor(big.equals("ATTENTION")?AMBER:CHAMP_2); card.addView(b,wrapWithBottom(8));
        for(String[] r:rows) {LinearLayout line=infoRow(r[0],r[1]); card.addView(line,new LinearLayout.LayoutParams(-1,dp(60)));}
        return page;
    }

    private void actionRow(LinearLayout parent,String title,String sub,String action,Runnable run){
        LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(12),0,dp(8),0); r.setBackground(round(PANEL_2,Color.TRANSPARENT,16));
        LinearLayout copy=new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.addView(text(title,12,INK),wrap()); copy.addView(text(sub,10,MUTED),wrap());
        r.addView(copy,new LinearLayout.LayoutParams(0,-1,1)); TextView b=button(action,false); r.addView(b,new LinearLayout.LayoutParams(dp(88),dp(40))); b.setOnClickListener(v->run.run()); parent.addView(r,new LinearLayout.LayoutParams(-1,dp(62)));
    }

    private LinearLayout infoRow(String title,String sub){
        LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(12),0,dp(10),0); r.setBackground(round(PANEL_2,Color.TRANSPARENT,16));
        LinearLayout copy=new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.addView(text(title,12,INK),wrap()); copy.addView(text(sub,10,MUTED),wrap()); r.addView(copy,new LinearLayout.LayoutParams(0,-1,1)); return r;
    }

    private void metric(LinearLayout parent,String k,String v,String d){
        LinearLayout m=new LinearLayout(this); m.setOrientation(LinearLayout.VERTICAL); m.setPadding(dp(10),dp(10),dp(10),dp(9)); m.setBackground(round(PANEL_2,Color.TRANSPARENT,17));
        m.addView(text(k,10,MUTED),wrap()); TextView val=heading(v,22); m.addView(val,wrap()); m.addView(text(d,10,GREEN),wrap()); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,.5f); lp.setMargins(0,dp(2),dp(5),dp(2)); parent.addView(m,lp);
    }

    private void rowTitle(LinearLayout parent,String title,String right){ LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); TextView t=text(title,13,INK); t.setTypeface(Typeface.DEFAULT_BOLD); r.addView(t,new LinearLayout.LayoutParams(0,dp(28),1)); TextView q=text(right,10,DIM); q.setLetterSpacing(.08f); r.addView(q,wrap()); parent.addView(r,new LinearLayout.LayoutParams(-1,dp(34))); }

    private LinearLayout pageColumn(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout cardColumn(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(14),dp(16),dp(14));l.setBackground(round(PANEL,Color.argb(28,255,255,255),24));return l;}
    private void addEyebrow(LinearLayout p,String s){TextView v=text(s,10,DIM);v.setLetterSpacing(.16f);v.setTypeface(Typeface.DEFAULT_BOLD);p.addView(v,wrapWithBottom(3));}
    private TextView heading(String s,int sp){TextView v=text(s,sp,INK);v.setTypeface(Typeface.DEFAULT_BOLD);return v;}
    private TextView text(String s,int sp,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setFontFeatureSettings("kern");return v;}
    private TextView button(String s,boolean primary){TextView v=text(s,13,primary?Color.rgb(24,20,12):MUTED);v.setTypeface(Typeface.DEFAULT_BOLD);v.setGravity(Gravity.CENTER);v.setBackground(primary?roundGradient(CHAMP,Color.rgb(165,133,84),14):round(Color.TRANSPARENT,LINE,14));return v;}
    private GradientDrawable round(int color,int strokeColor,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);if(strokeColor!=Color.TRANSPARENT)g.setStroke(dp(1),strokeColor);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable roundGradient(int top,int bottom,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{top,bottom});g.setCornerRadius(dp(radius));g.setStroke(dp(1),Color.argb(70,216,189,135));return g;}
    private void toast(String s){android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_SHORT).show();}
    private LinearLayout.LayoutParams match(){return new LinearLayout.LayoutParams(-1,-1);}
    private LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(-2,-2);}
    private LinearLayout.LayoutParams wrapWithBottom(int px){LinearLayout.LayoutParams p=wrap();p.bottomMargin=dp(px);return p;}
    private LinearLayout.LayoutParams wrapWithTop(int px){LinearLayout.LayoutParams p=wrap();p.topMargin=dp(px);return p;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}

    @Override protected void onResume(){super.onResume(); startedAt=startedAt==0?SystemClock.elapsedRealtime():startedAt; animateCore();}
    private void animateCore(){ if(coreView==null)return; coreView.animateCore(SystemClock.elapsedRealtime()-startedAt); coreView.postDelayed(this::animateCore,48); }

    @Override public void onBackPressed(){ if(drawerOverlay!=null&&drawerOverlay.getVisibility()==View.VISIBLE){closeDrawer();return;} if(!current.equals("command")){show("command");return;} super.onBackPressed(); }
}
