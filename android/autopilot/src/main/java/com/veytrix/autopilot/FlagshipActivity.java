package com.veytrix.autopilot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Scroll-free mobile command surface. The existing MainActivity remains the full mission console. */
public final class FlagshipActivity extends Activity {
    private static final int BG = 0xFF080B12;
    private static final int PANEL = 0xFF111725;
    private static final int PANEL_2 = 0xFF151C2D;
    private static final int BORDER = 0xFF273149;
    private static final int TEXT = 0xFFF5F7FB;
    private static final int MUTED = 0xFF8F9AB2;
    private static final int VIOLET = 0xFF7C5CFF;
    private static final int CYAN = 0xFF39D7FF;
    private static final int GREEN = 0xFF47E6A0;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(build());
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(12));
        root.setBackgroundColor(BG);

        LinearLayout top = row();
        TextView mark = pill("V", VIOLET, Color.WHITE, 42);
        top.addView(mark, box(42, 42));
        LinearLayout brand = column();
        TextView title = label("VEYTRIX", 19, TEXT, true);
        brand.addView(title, wrap());
        TextView subtitle = label("AUTONOMOUS ENGINEERING CONTROL", 8, CYAN, true);
        brand.addView(subtitle, wrap());
        LinearLayout.LayoutParams bp = weight(); bp.leftMargin = dp(11); top.addView(brand, bp);
        TextView online = label("●  ONLINE", 9, GREEN, true); top.addView(online, wrap());
        root.addView(top, wrap());

        TextView greeting = label("What do you want Veytrix to ship?", 25, TEXT, true);
        LinearLayout.LayoutParams gp = wrap(); gp.topMargin = dp(24); root.addView(greeting, gp);
        TextView desc = label("One command. Inspect → repair → build → verify → deliver.", 11, MUTED, false);
        root.addView(desc, wrap());

        LinearLayout hero = card();
        LinearLayout heroTop = row();
        TextView heroTitle = label("SMART AUTOPILOT", 11, CYAN, true); heroTop.addView(heroTitle, weight());
        heroTop.addView(label("READY", 9, GREEN, true), wrap());
        hero.addView(heroTop, wrap());
        TextView heroMain = label("Deterministic first. AI only when reasoning is actually required.", 15, TEXT, true);
        LinearLayout.LayoutParams hm = wrap(); hm.topMargin = dp(10); hero.addView(heroMain, hm);
        TextView providers = label("Gemini  •  Groq  •  Mistral  •  OpenRouter  •  free-model pool", 9, MUTED, false);
        LinearLayout.LayoutParams pr = wrap(); pr.topMargin = dp(7); hero.addView(providers, pr);
        Button launch = action("RUN NEW MISSION", VIOLET, Color.WHITE);
        launch.setOnClickListener(v -> openConsole());
        LinearLayout.LayoutParams lp = full(50); lp.topMargin = dp(14); hero.addView(launch, lp);
        LinearLayout.LayoutParams hp = full(-2); hp.topMargin = dp(18); root.addView(hero, hp);

        LinearLayout grid = new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout r1 = row(); r1.addView(tile("◈", "LIVE RUN", "Monitor current workflow", CYAN, v -> openConsole()), weight());
        LinearLayout.LayoutParams right = weight(); right.leftMargin = dp(10); r1.addView(tile("↺", "MISSION MEMORY", "Recent autonomous runs", VIOLET, v -> openConsole()), right);
        grid.addView(r1, full(108));
        LinearLayout r2 = row(); LinearLayout.LayoutParams a = weight(); a.topMargin = dp(10); r2.addView(tile("⌁", "AI ROUTER", "Free provider cascade", GREEN, v -> openConsole()), a);
        LinearLayout.LayoutParams b = weight(); b.leftMargin = dp(10); b.topMargin = dp(10); r2.addView(tile("⌾", "SECURITY", "Keystore + encrypted token", VIOLET, v -> openConsole()), b);
        grid.addView(r2, full(108));
        LinearLayout.LayoutParams gridp = full(226); gridp.topMargin = dp(12); root.addView(grid, gridp);

        LinearLayout spacer = new LinearLayout(this); root.addView(spacer, weight());
        LinearLayout nav = card(); nav.setPadding(dp(8), dp(7), dp(8), dp(7));
        nav.addView(navItem("⌂", "CONTROL", true), weight());
        nav.addView(navItem("⚡", "MISSION", false), weight());
        nav.addView(navItem("◌", "RUNS", false), weight());
        nav.addView(navItem("⚙", "SETTINGS", false), weight());
        root.addView(nav, full(68));
        return root;
    }

    private View tile(String icon, String title, String sub, int accent, View.OnClickListener click) {
        LinearLayout c = card(); c.setOnClickListener(click); c.setPadding(dp(13), dp(11), dp(11), dp(10));
        TextView i = label(icon, 19, accent, true); c.addView(i, wrap());
        TextView t = label(title, 10, TEXT, true); LinearLayout.LayoutParams tp = wrap(); tp.topMargin = dp(6); c.addView(t, tp);
        TextView s = label(sub, 8, MUTED, false); LinearLayout.LayoutParams sp = wrap(); sp.topMargin = dp(3); c.addView(s, sp);
        return c;
    }

    private View navItem(String icon, String text, boolean active) {
        LinearLayout c = column(); c.setGravity(Gravity.CENTER);
        c.addView(label(icon, 17, active ? VIOLET : MUTED, true), wrap());
        c.addView(label(text, 7, active ? TEXT : MUTED, true), wrap());
        return c;
    }

    private Button action(String text, int fill, int fg) {
        Button b = new Button(this); b.setAllCaps(false); b.setText(text); b.setTextSize(11); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setTextColor(fg); b.setGravity(Gravity.CENTER); b.setPadding(0,0,0,0); b.setBackground(bg(fill, fill, 14, 0)); return b;
    }

    private TextView pill(String text, int fill, int fg, int size) { TextView v=label(text,20,fg,true); v.setGravity(Gravity.CENTER); v.setBackground(bg(fill,fill,size/2,0)); return v; }
    private TextView label(String text,float size,int color,boolean bold){ TextView v=new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color); v.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL); return v; }
    private LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout column(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout card(){ LinearLayout l=column(); l.setPadding(dp(14),dp(13),dp(14),dp(13)); l.setBackground(bg(PANEL,PANEL_2,18,1)); return l; }
    private GradientDrawable bg(int fill,int stroke,int radius,int strokeWidth){ GradientDrawable d=new GradientDrawable(); d.setColor(fill); if(strokeWidth>0)d.setStroke(dp(strokeWidth),stroke); d.setCornerRadius(dp(radius)); return d; }
    private LinearLayout.LayoutParams wrap(){return new LinearLayout.LayoutParams(-2,-2);}
    private LinearLayout.LayoutParams full(int h){return new LinearLayout.LayoutParams(-1,h<0?-2:dp(h));}
    private LinearLayout.LayoutParams box(int w,int h){return new LinearLayout.LayoutParams(dp(w),dp(h));}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-1,1f);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void openConsole(){startActivity(new Intent(this,MainActivity.class));}
}
