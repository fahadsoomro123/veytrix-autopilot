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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Space;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * VEYTRIX native Android application UI.
 * Pure native Views + Canvas; no WebView/HTML.
 */
public final class MainActivity extends Activity {

    private static final int LEGACY_UNUSED_ACCENT = Color.TRANSPARENT;

    private FrameLayout root;
    private FrameLayout pageHost;
    private LinearLayout bottomNav;
    private FrameLayout drawerShade;
    private LinearLayout drawer;
    private VeytrixAutopilotClient autopilotClient;
    private VeytrixVoiceView activeVoiceView;
    private String pendingVoiceMission = "";
    private int systemBarTopInset;
    private int systemBarBottomInset;


    private int currentPage = 0; // 0 Home, 1 Activity, 2 Results, 3 Control, 4 More

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        autopilotClient = new VeytrixAutopilotClient(this);
        buildShell();
        showHome();
    }

    @Override protected void onDestroy() {
        if (activeVoiceView != null) {
            activeVoiceView.release();
            activeVoiceView = null;
        }
        if (autopilotClient != null) {
            autopilotClient.shutdown();
        }
        super.onDestroy();
    }

    private void configureWindow() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(VeytrixDesignTokens.WHITE);
        window.setNavigationBarColor(VeytrixDesignTokens.WHITE);
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
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
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
        drawerShade.setBackgroundColor(withAlpha(VeytrixDesignTokens.PURPLE, 58));
        drawerShade.setVisibility(View.GONE);
        drawerShade.setOnClickListener(v -> closeDrawer());
        root.addView(drawerShade, full());

        drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setPadding(dp(16), dp(18), dp(16), dp(16));
        drawer.setBackgroundColor(VeytrixDesignTokens.WHITE);
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
        brandCol.addView(text("VEYTRIX", 18, VeytrixDesignTokens.TEXT_PRIMARY, true));
        brandCol.addView(text("AUTONOMOUS AI DEVELOPMENT", 8, VeytrixDesignTokens.TEXT_SECONDARY, false));
        header.addView(brandCol, new LinearLayout.LayoutParams(0, -2, 1));
        IconView close = new IconView(this, "close");
        close.setOnClickListener(v -> closeDrawer());
        header.addView(close, new LinearLayout.LayoutParams(dp(32), dp(32)));
        drawer.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));
        drawer.addView(text("WORKSPACE", 9, VeytrixDesignTokens.TEXT_SECONDARY, true), new LinearLayout.LayoutParams(-1, dp(28)));
        drawerItem("Home", "home", 0); drawerItem("Activity", "activity", 1);
        drawerItem("Results", "results", 2); drawerItem("Control", "control", 3);
        drawerItem("Profile", "profile", 4); drawerItem("Voice", "voice", 5); drawerItem("Settings", "settings", 6);
        Space spacer = new Space(this); drawer.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        drawer.addView(text("SYSTEM", 9, VeytrixDesignTokens.TEXT_SECONDARY, true), new LinearLayout.LayoutParams(-1, dp(26)));        drawerItem("Updates", "download", 9);        drawer.addView(text("SUPPORT", 9, VeytrixDesignTokens.TEXT_SECONDARY, true), new LinearLayout.LayoutParams(-1, dp(26)));
        drawerItem("Help & Support", "support", 7); drawerItem("Completed", "check", 8);
        LinearLayout account = new LinearLayout(this); account.setGravity(Gravity.CENTER_VERTICAL);
        account.setPadding(dp(10), dp(8), dp(10), dp(8)); account.setBackground(roundDrawable(VeytrixDesignTokens.PEARL, 14, VeytrixDesignTokens.SILVER));
        IconView avatar = new IconView(this, "profile"); account.addView(avatar, new LinearLayout.LayoutParams(dp(30), dp(30)));
        LinearLayout acc = new LinearLayout(this); acc.setOrientation(LinearLayout.VERTICAL); acc.setPadding(dp(10),0,0,0);
        acc.addView(text("GitHub connection",10,VeytrixDesignTokens.TEXT_PRIMARY,true)); acc.addView(text("Secure credential state",8,VeytrixDesignTokens.VIOLET,true)); account.addView(acc);
        drawer.addView(account, new LinearLayout.LayoutParams(-1, dp(56)));
    }

    private void drawerItem(String label, String icon, int destination) {
        LinearLayout item = new LinearLayout(this); item.setGravity(Gravity.CENTER_VERTICAL); item.setPadding(dp(8),0,dp(8),0);
        item.addView(new IconView(this, icon), new LinearLayout.LayoutParams(dp(26),dp(26)));
        TextView labelView=text(label,11,VeytrixDesignTokens.TEXT_PRIMARY,true); labelView.setPadding(dp(8),0,0,0);
        item.addView(labelView,new LinearLayout.LayoutParams(0,-1,1));
        item.setOnClickListener(v->{closeDrawer(); switch(destination){case 0:navigate(0);break;case 1:navigate(1);break;case 2:navigate(2);break;case 3:navigate(3);break;case 4:showProfile();break;case 5:showVoice();break;case 6:showSettings();break;case 7:showSupport();break;case 8:showCompleted();break;case 9:showUpdates();break;default:break;}});
        drawer.addView(item,new LinearLayout.LayoutParams(-1,dp(44)));
    }

    private void openDrawer(){rebuildDrawer();drawerShade.setVisibility(View.VISIBLE);drawer.setVisibility(View.VISIBLE);}
    private void closeDrawer(){drawerShade.setVisibility(View.GONE);drawer.setVisibility(View.GONE);}

    private void navigate(int page){currentPage=page;if(page==0)showHome();else if(page==1)showActivity();else if(page==2)showResults();else if(page==3)showControl();else showMore();}
    private void clearPage(){
        closeDrawer();
        if (activeVoiceView != null) {
            activeVoiceView.release();
            activeVoiceView = null;
        }
        pageHost.removeAllViews();
        pageHost.setPadding(
                dp(14),
                systemBarTopInset + dp(6),
                dp(14),
                systemBarBottomInset + dp(82)
        );
    }

    private void showHome() {
        clearPage();
        pageHost.setPadding(
                dp(14),
                systemBarTopInset + dp(6),
                dp(14),
                systemBarBottomInset + dp(82)
        );
        VeytrixHomeView home = new VeytrixHomeView(
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
        );
        pageHost.addView(home, full());
        if (!pendingVoiceMission.isEmpty()) {
            home.setMissionText(pendingVoiceMission);
            pendingVoiceMission = "";
        }
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

    private void showActivity() {
        clearPage();
        pageHost.addView(
                new VeytrixActivityView(
                        this,
                        autopilotClient,
                        new VeytrixActivityView.Host() {
                            @Override public void openConnection() {
                                showConnectionDialog();
                            }

                            @Override public void openRunDetails(
                                    VeytrixAutopilotClient.RunInfo run) {
                                showRunDetails(run);
                            }
                        }
                ),
                full()
        );
    }

    private void showResults() {
        clearPage();
        pageHost.addView(
                new VeytrixResultsView(
                        this,
                        autopilotClient,
                        () -> showConnectionDialog()
                ),
                full()
        );
    }

    private void showControl() {
        clearPage();
        pageHost.addView(
                new VeytrixControlView(
                        this,
                        autopilotClient,
                        () -> showConnectionDialog()
                ),
                full()
        );
    }

    private void showMore() {
        clearPage();
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14), dp(8), dp(14), dp(10));
        col.setBackgroundColor(VeytrixDesignTokens.PEARL);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button menu = button("☰", VeytrixDesignTokens.WHITE, VeytrixDesignTokens.TEXT_PRIMARY);
        menu.setOnClickListener(v -> openDrawer());
        header.addView(menu, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = text("MORE", 22, VeytrixDesignTokens.TEXT_PRIMARY, true);
        title.setPadding(dp(8), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        col.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));

        col.addView(text(
                "Secondary tools, account and support surfaces.",
                10,
                VeytrixDesignTokens.TEXT_SECONDARY,
                false
        ), marginBottom(8));

        col.addView(actionListRow("Profile", "Connection and target repository", v -> showProfile()),
                new LinearLayout.LayoutParams(-1, dp(56)));
        col.addView(actionListRow("Voice Command", "Native speech recognition to mission composer", v -> showVoice()),
                new LinearLayout.LayoutParams(-1, dp(56)));
        col.addView(actionListRow("Completed", "Real successful workflow runs", v -> showCompleted()),
                new LinearLayout.LayoutParams(-1, dp(56)));
        col.addView(actionListRow("Help & Support", "Connection and control guidance", v -> showSupport()),
                new LinearLayout.LayoutParams(-1, dp(56)));
        col.addView(actionListRow("Control", "Engine and verification settings", v -> showControl()),
                new LinearLayout.LayoutParams(-1, dp(56)));

        pageHost.addView(col, full());
    }

    private LinearLayout actionListRow(
            String title,
            String subtitle,
            View.OnClickListener action
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(4), dp(12), dp(4));
        row.setBackground(roundDrawable(
                VeytrixDesignTokens.WHITE, 15, VeytrixDesignTokens.SILVER
        ));
        TextView marker = text("•", 18, VeytrixDesignTokens.VIOLET, true);
        marker.setGravity(Gravity.CENTER);
        row.addView(marker, new LinearLayout.LayoutParams(dp(30), dp(48)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(8), 0, dp(6), 0);
        copy.addView(text(title, 11, VeytrixDesignTokens.TEXT_PRIMARY, true));
        copy.addView(text(subtitle, 9, VeytrixDesignTokens.TEXT_SECONDARY, false));
        row.addView(copy, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView arrow = text("›", 17, VeytrixDesignTokens.VIOLET, true);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(48)));
        row.setOnClickListener(action);
        return row;
    }

    private void showProfile() {
        clearPage();
        pageHost.addView(new VeytrixProfileView(
                this,
                autopilotClient,
                new VeytrixProfileView.Host() {
                    @Override public void openConnection() {
                        showConnectionDialog();
                    }
                    @Override public void openDrawer() {
                        MainActivity.this.openDrawer();
                    }
                    @Override public void closeConnection() {
                        showHome();
                    }
                }
        ), full());
    }

    private void showVoice() {
        clearPage();
        activeVoiceView = new VeytrixVoiceView(
                this,
                new VeytrixVoiceView.Host() {
                    @Override public void openDrawer() {
                        MainActivity.this.openDrawer();
                    }
                    @Override public void sendCommandToMission(String command) {
                        pendingVoiceMission = command;
                        showHome();
                    }
                }
        );
        pageHost.addView(activeVoiceView, full());
    }

    private void showCompleted() {
        clearPage();
        pageHost.addView(new VeytrixCompletedView(
                this,
                autopilotClient,
                new VeytrixCompletedView.Host() {
                    @Override public void openConnection() {
                        showConnectionDialog();
                    }
                    @Override public void openDrawer() {
                        MainActivity.this.openDrawer();
                    }
                    @Override public void openRunDetails(VeytrixAutopilotClient.RunInfo run) {
                        showRunDetails(run);
                    }
                }
        ), full());
    }

    private void showUpdates() {
        clearPage();
        pageHost.addView(
                new VeytrixUpdateView(
                        this,
                        new VeytrixUpdateClient(this),
                        () -> MainActivity.this.openDrawer()
                ),
                full()
        );
    }

    private void showSupport() {
        clearPage();
        pageHost.addView(new VeytrixSupportView(
                this,
                autopilotClient,
                new VeytrixSupportView.Host() {
                    @Override public void openDrawer() {
                        MainActivity.this.openDrawer();
                    }
                    @Override public void openConnection() {
                        showConnectionDialog();
                    }
                    @Override public void openControl() {
                        showControl();
                    }
                }
        ), full());
    }

    private void showRunDetails(VeytrixAutopilotClient.RunInfo run) {
        clearPage();
        pageHost.addView(new VeytrixRunDetailsView(
                this,
                autopilotClient,
                run,
                new VeytrixRunDetailsView.Host() {
                    @Override public void openDrawer() {
                        MainActivity.this.openDrawer();
                    }
                    @Override public void openActivity() {
                        showActivity();
                    }
                }
        ), full());
    }

    private void showSettings() {
        showControl();
    }

    private Button button(String label, int background, int fg){Button b=new Button(this);b.setText(label);b.setTextSize(11);b.setTextColor(fg);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setMinHeight(dp(48));b.setMinWidth(dp(48));b.setBackground(roundDrawable(background,14,background==VeytrixDesignTokens.WHITE?VeytrixDesignTokens.SILVER:background));return b;}
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
    private int withAlpha(int color, int alpha){return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));}
    private FrameLayout.LayoutParams full(){return new FrameLayout.LayoutParams(-1,-1);}

    private final class IconView extends View {
        private final String kind; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        IconView(Context c,String kind){super(c);this.kind=kind;setContentDescription(kind);}
        @Override protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,s=Math.min(getWidth(),getHeight())/32f;int col=VeytrixDesignTokens.TEXT_PRIMARY;if(kind.equals("logo")||kind.equals("spark"))col=VeytrixDesignTokens.PURPLE;else if(kind.equals("test")||kind.equals("done")||kind.equals("check")||kind.equals("bell")||kind.equals("successLarge"))col=VeytrixDesignTokens.STATE_VERIFIED;else if(kind.equals("android")||kind.equals("code")||kind.equals("document")||kind.equals("mission")||kind.equals("activity")||kind.equals("results"))col=VeytrixDesignTokens.VIOLET;p.setColor(col);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,dp(1.6f)));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);draw(c,x,y,s);}
        private void draw(Canvas c,float x,float y,float s){if(kind.equals("hamburger")){line(c,x-7*s,y-5*s,x+7*s,y-5*s);line(c,x-7*s,y,x+7*s,y);line(c,x-7*s,y+5*s,x+7*s,y+5*s);}else if(kind.equals("logo")){Path q=new Path();q.moveTo(x-9*s,y-10*s);q.lineTo(x,y+9*s);q.lineTo(x+9*s,y-10*s);q.close();c.drawPath(q,p);}else if(kind.equals("profile")||kind.equals("profileLarge")){c.drawCircle(x,y-4*s,4*s,p);c.drawRoundRect(new RectF(x-7*s,y+1*s,x+7*s,y+9*s),4*s,4*s,p);}else if(kind.equals("check")||kind.equals("done")){line(c,x-7*s,y,x-1*s,y+6*s);line(c,x-1*s,y+6*s,x+8*s,y-7*s);}else if(kind.equals("database")){c.drawOval(new RectF(x-7*s,y-6*s,x+7*s,y),p);c.drawArc(new RectF(x-7*s,y-2*s,x+7*s,y+9*s),0,180,false,p);}else if(kind.equals("document")||kind.equals("report")){c.drawRect(new RectF(x-6*s,y-8*s,x+6*s,y+8*s),p);line(c,x-3*s,y-2*s,x+3*s,y-2*s);line(c,x-3*s,y+2*s,x+3*s,y+2*s);}else if(kind.equals("code")){line(c,x-4*s,y,x-8*s,y+4*s);line(c,x-8*s,y+4*s,x-4*s,y+8*s);line(c,x+4*s,y,x+8*s,y+4*s);line(c,x+8*s,y+4*s,x+4*s,y+8*s);}else if(kind.equals("android")){c.drawRoundRect(new RectF(x-7*s,y-5*s,x+7*s,y+7*s),2*s,2*s,p);line(c,x-5*s,y-5*s,x-2*s,y-10*s);line(c,x+5*s,y-5*s,x+2*s,y-10*s);}else if(kind.equals("archive")){c.drawRect(new RectF(x-8*s,y-6*s,x+8*s,y+6*s),p);}else if(kind.equals("mission")){c.drawRect(new RectF(x-7*s,y-7*s,x+7*s,y+7*s),p);line(c,x-4*s,y-2*s,x+4*s,y-2*s);}else if(kind.equals("alert")){Path q=new Path();q.moveTo(x,y-8*s);q.lineTo(x+8*s,y+7*s);q.lineTo(x-8*s,y+7*s);q.close();c.drawPath(q,p);}else if(kind.equals("model")){c.drawCircle(x,y,7*s,p);c.drawCircle(x,y,2*s,p);}else if(kind.equals("fallback")){c.drawArc(new RectF(x-7*s,y-7*s,x+7*s,y+7*s),-60,250,false,p);}else if(kind.equals("clock")){c.drawCircle(x,y,7*s,p);line(c,x,y,x+4*s,y+3*s);line(c,x,y,x,y-4*s);}else if(kind.equals("spark")){line(c,x,y-9*s,x,y+9*s);line(c,x-9*s,y,x+9*s,y);}else if(kind.equals("voice")){c.drawRoundRect(new RectF(x-4*s,y-8*s,x+4*s,y+3*s),4*s,4*s,p);c.drawArc(new RectF(x-9*s,y-1*s,x+9*s,y+10*s),0,180,false,p);}else if(kind.equals("attach")){Path q=new Path();q.moveTo(x-2*s,y-7*s);q.cubicTo(x-10*s,y+s,x+s*1,y+10*s,x+6*s,y+5*s);c.drawPath(q,p);}else if(kind.equals("launch")){Path q=new Path();q.moveTo(x-5*s,y-7*s);q.lineTo(x+7*s,y);q.lineTo(x-5*s,y+7*s);q.close();c.drawPath(q,p);}else if(kind.equals("bell")){c.drawRoundRect(new RectF(x-6*s,y-6*s,x+6*s,y+6*s),4*s,4*s,p);line(c,x-8*s,y+8*s,x+8*s,y+8*s);}else if(kind.equals("settings")||kind.equals("gear")){c.drawCircle(x,y,7*s,p);c.drawCircle(x,y,2*s,p);}else if(kind.equals("appearance")){c.drawCircle(x,y,7*s,p);line(c,x,y-7*s,x,y+7*s);}else if(kind.equals("privacy")){c.drawRect(new RectF(x-6*s,y-5*s,x+6*s,y+7*s),p);}else if(kind.equals("info")){c.drawCircle(x,y,7*s,p);line(c,x,y-2*s,x,y+5*s);}else if(kind.equals("support")){c.drawArc(new RectF(x-8*s,y-8*s,x+8*s,y+8*s),20,140,false,p);}else if(kind.equals("successLarge")){p.setStyle(Paint.Style.FILL);p.setColor(VeytrixDesignTokens.STATE_VERIFIED);c.drawCircle(x,y,Math.min(getWidth(),getHeight())*.42f,p);p.setStyle(Paint.Style.STROKE);p.setColor(VeytrixDesignTokens.WHITE);p.setStrokeWidth(dp(3));line(c,x-11,y,x-3,y+9);line(c,x-3,y+9,x+13,y-12);}else {p.setStyle(Paint.Style.FILL);p.setColor(VeytrixDesignTokens.TEXT_SECONDARY);c.drawCircle(x,y,4*s,p);}p.setStyle(Paint.Style.FILL);}
        private void line(Canvas c,float a,float b,float d,float e){c.drawLine(a,b,d,e,p);}
    }

}
