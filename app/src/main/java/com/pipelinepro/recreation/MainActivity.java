package com.pipelinepro.recreation;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(15, 17, 23);
    private static final int PANEL = Color.rgb(22, 27, 39);
    private static final int CARD = Color.rgb(30, 37, 53);
    private static final int VIOLET = Color.rgb(124, 58, 237);
    private static final int MUTED = Color.rgb(148, 163, 184);
    private static final int SUBTLE = Color.rgb(71, 85, 105);
    private static final int EMERALD = Color.rgb(16, 185, 129);
    private static final int AMBER = Color.rgb(245, 158, 11);
    private static final int RED = Color.rgb(239, 68, 68);
    private static final String[] BRANDS = {"Hisense", "LG", "Samsung", "Sony", "Other"};
    private static final String[] MODELS_2025 = {"A4Q", "A5Q", "A6Q", "A7Q", "A85Q", "Canvas", "Deco", "E7Q", "E7Q Pro", "U7Q", "U7Q Pro", "U8Q", "PX3", "C2", "C2 Ultra", "Other"};
    private static final String[] MODELS_2026 = {"A4S", "A5S", "A6S", "A7S", "E7S", "E7S Pro", "U7S", "U7S Pro", "U8Q", "UR8S", "UR9S", "Follow Me", "PX3", "C2", "C2 Ultra", "Other"};
    private static final String[] SIZES = {"32", "40", "43", "50", "55", "65", "75", "85", "100", "150", "300", "Other"};
    private static final String[] TIERS = {"UHD", "QLED", "PREMIUM", "RGB MINI LED", "LASER"};
    private static final String[] SOUNDBARS = {"AX5140Q", "AX5100Q", "AX3120Q", "HT Saturn", "Other"};

    private DataStore store;
    private FrameLayout root;
    private String page = "Dashboard", period = "today", scope = "mine", todayTab = "tv";
    private String saleType = "tv", brand = "", model = "", size = "", tier = "", price = "", date = DataStore.today(), year = "2026";
    private boolean refund = false;
    private int qty = 1;

    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        store = new DataStore(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        setContentView(root);
        render();
    }

    private void render() {
        root.removeAllViews();
        if (store.selectedStore() == null) { root.addView(storePicker()); return; }
        LinearLayout app = vbox();
        app.setBackgroundColor(BG);
        root.addView(app, new FrameLayout.LayoutParams(-1, -1));
        app.addView(header());
        app.addView(nav());
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = vbox();
        content.setPadding(dp(16), dp(16), dp(16), dp(30));
        scroll.addView(content);
        app.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if ("Dashboard".equals(page)) dashboard(content);
        else if ("Log Sale".equals(page)) logSale(content);
        else if ("Today's Sales".equals(page)) todaysSales(content);
        else if ("Leaderboards".equals(page)) leaderboards(content);
        else if ("Tools".equals(page)) tools(content);
        else commission(content);
    }

    private View storePicker() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = vbox();
        body.setPadding(dp(22), dp(50), dp(22), dp(28));
        scroll.addView(body);
        body.addView(text("SalesTracker", 32, Color.WHITE, true));
        body.addView(text("Pick your store to start tracking Hisense performance.", 15, MUTED, false));
        body.addView(space(22));
        for (DataStore.Store s : store.stores) {
            LinearLayout c = card();
            c.addView(rowText(s.name, s.region));
            c.addView(text("Dashboard, sales logging, leaderboards, ASM tools", 12, MUTED, false));
            c.setOnClickListener(v -> { store.selectStore(s.name); render(); });
            body.addView(c); body.addView(space(10));
        }
        Button add = primary("Add Store");
        add.setOnClickListener(v -> addStoreDialog());
        body.addView(add);
        return scroll;
    }

    private View header() {
        LinearLayout h = hbox();
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(16), dp(12), dp(16), dp(12));
        h.setBackgroundColor(PANEL);
        LinearLayout title = vbox();
        title.addView(text("SalesTracker", 18, Color.WHITE, true));
        title.addView(text(store.selectedStore() + " - " + store.currentRegion(), 12, MUTED, false));
        h.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button switcher = small("Switch");
        switcher.setOnClickListener(v -> { store.clearStore(); render(); });
        h.addView(switcher);
        return h;
    }

    private View nav() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setBackgroundColor(PANEL);
        LinearLayout row = hbox();
        row.setPadding(dp(10), 0, dp(10), dp(10));
        for (String p : new String[]{"Dashboard", "Log Sale", "Today's Sales", "Leaderboards", "Tools", "Commission"}) {
            Button b = chip(p, p.equals(page));
            b.setOnClickListener(v -> { page = p; render(); });
            row.addView(b);
        }
        scroll.addView(row);
        return scroll;
    }

    private void dashboard(LinearLayout body) {
        List<DataStore.Sale> sales = store.filtered(period, scopeKey());
        DataStore.Stats stats = store.stats(sales, scopeKey());
        body.addView(text("Dashboard", 28, Color.WHITE, true));
        body.addView(text(("mine".equals(scopeKey()) ? store.selectedStore() : store.currentRegion() + " region") + " - Week " + LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear()), 13, MUTED, false));
        body.addView(space(12));
        body.addView(seg(new String[]{"today", "week", "month", "year"}, new String[]{"Today", "Week", "MTD", "YTD"}, period, v -> { period = v; render(); }));
        body.addView(space(8));
        body.addView(seg(new String[]{"mine", "region"}, new String[]{"My Store", "Full Region"}, scopeKey(), v -> { scope = v; render(); }));
        body.addView(space(12));
        LinearLayout sov = panel(stats.sov >= 20 ? EMERALD : stats.sov >= 10 ? AMBER : RED);
        sov.addView(text("Hisense Share of Value", 12, MUTED, true));
        sov.addView(text(stats.sov + "%", 42, stats.sov >= 20 ? EMERALD : stats.sov >= 10 ? AMBER : RED, true));
        sov.addView(text(DataStore.money(stats.hisenseValue) + " of " + DataStore.money(stats.tvValue) + " total TV revenue", 12, MUTED, false));
        sov.addView(progress(stats.sov, stats.sov >= 20 ? EMERALD : stats.sov >= 10 ? AMBER : RED));
        body.addView(sov); body.addView(space(12));
        LinearLayout r1 = hbox();
        r1.addView(kpi("Hisense Units", String.valueOf(stats.hisenseUnits), "of " + stats.tvUnits + " TVs", VIOLET), weight());
        r1.addView(kpi("Hisense Revenue", DataStore.money(stats.hisenseValue), "all Hisense TVs", EMERALD), weight());
        body.addView(r1); body.addView(space(10));
        LinearLayout r2 = hbox();
        r2.addView(kpi("ASP", DataStore.money(stats.asp), "average price", Color.rgb(14, 165, 233)), weight());
        r2.addView(kpi("Premium", stats.premiumMix + "%", "premium mix", AMBER), weight());
        body.addView(r2); body.addView(space(10));
        body.addView(kpi("Soundbar", DataStore.money(stats.soundbarValue), stats.soundbarUnits + " today", VIOLET));
        body.addView(space(12));
        LinearLayout brandCard = card();
        brandCard.addView(text("Share of Value by Brand", 16, Color.WHITE, true));
        brandCard.addView(new BrandChart(store.brandValues(sales)), new LinearLayout.LayoutParams(-1, dp(175)));
        body.addView(brandCard); body.addView(space(12));
        LinearLayout weekCard = card();
        weekCard.addView(text("Hisense SOV % - This Week", 16, Color.WHITE, true));
        weekCard.addView(new WeekChart(store.weeklySov(scopeKey())), new LinearLayout.LayoutParams(-1, dp(175)));
        body.addView(weekCard);
    }

    private void logSale(LinearLayout body) {
        body.addView(text("Log a Sale" + (qty > 1 ? " x" + qty : ""), 28, Color.WHITE, true));
        body.addView(text("Record TV, soundbar, refund, or multi-sale entries.", 13, MUTED, false));
        body.addView(space(12));
        body.addView(seg(new String[]{"tv", "soundbar"}, new String[]{"TV", "Soundbar"}, saleType, v -> { saleType = v; render(); }));
        body.addView(space(10));
        LinearLayout controls = hbox();
        Button ref = outline(refund ? "Refund On" : "Refund"); ref.setTextColor(refund ? RED : MUTED); ref.setOnClickListener(v -> { refund = !refund; render(); });
        Button minus = outline("-"); minus.setOnClickListener(v -> { if (qty > 1) qty--; render(); });
        Button plus = outline("+"); plus.setOnClickListener(v -> { qty++; render(); });
        controls.addView(ref, weight()); controls.addView(minus); controls.addView(outline("Qty " + qty)); controls.addView(plus);
        body.addView(controls); body.addView(space(12));
        LinearLayout form = panel(refund ? RED : VIOLET);
        if ("tv".equals(saleType)) {
            form.addView(section("Brand *"));
            form.addView(grid(Arrays.asList(BRANDS), brand, 2, v -> { brand = v; if (!"Hisense".equals(brand)) { model = size = tier = ""; } render(); }));
            if ("Hisense".equals(brand) && !refund) {
                form.addView(section("Model Year")); form.addView(seg(new String[]{"2025", "2026"}, new String[]{"2025", "2026"}, year, v -> { year = v; model = tier = ""; render(); }));
                form.addView(section("Model *")); form.addView(grid(Arrays.asList("2025".equals(year) ? MODELS_2025 : MODELS_2026), model, 3, v -> { model = v; tier = tierFor(v); if (size.length() > 0) price = suggested(v, size); render(); }));
                form.addView(section("Size *")); form.addView(grid(Arrays.asList(SIZES), size, 4, v -> { size = v; if (model.length() > 0) price = suggested(model, size); render(); }));
                form.addView(section("Tier *")); form.addView(grid(Arrays.asList(TIERS), tier, 2, v -> { tier = v; render(); }));
            }
        } else if (!refund) {
            form.addView(section("Soundbar Model *")); form.addView(grid(Arrays.asList(SOUNDBARS), model, 2, v -> { model = v; render(); }));
        }
        form.addView(section("Sale Price (GBP) *")); EditText p = input("799", price, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); form.addView(p);
        form.addView(section("Sale Date")); EditText d = input("YYYY-MM-DD", date, InputType.TYPE_CLASS_DATETIME); form.addView(d); form.addView(space(12));
        Button save = primary(refund ? "Log Refund" : "Log Sale"); save.setOnClickListener(v -> { price = p.getText().toString(); date = d.getText().toString(); saveSale(); });
        form.addView(save); body.addView(form);
    }

    private void todaysSales(LinearLayout body) {
        body.addView(text("Today's Sales", 28, Color.WHITE, true));
        body.addView(text(store.selectedStore() + " - " + DataStore.today(), 13, MUTED, false)); body.addView(space(12));
        body.addView(seg(new String[]{"tv", "soundbar"}, new String[]{"TV", "Soundbar"}, todayTab, v -> { todayTab = v; render(); })); body.addView(space(12));
        LinearLayout list = card();
        double value = 0; int units = 0;
        for (DataStore.Sale s : store.todaysSales(store.selectedStore())) if (("soundbar".equals(todayTab)) == s.soundbar) { value += s.price; units++; }
        list.addView(text(units + " units - " + DataStore.money(value), 18, Color.WHITE, true));
        for (DataStore.Sale s : store.todaysSales(store.selectedStore())) {
            if (("soundbar".equals(todayTab)) != s.soundbar) continue;
            LinearLayout row = saleRow(s); row.setOnClickListener(v -> editSale(s)); list.addView(row);
        }
        if (units == 0) list.addView(text("No sales logged yet.", 13, MUTED, false));
        body.addView(list);
    }

    private void leaderboards(LinearLayout body) {
        body.addView(text("Leaderboards", 28, Color.WHITE, true)); body.addView(text("This week - " + store.currentRegion(), 13, MUTED, false)); body.addView(space(12));
        int rank = 1;
        for (DataStore.Row row : store.leaderboard()) {
            LinearLayout c = card(); c.addView(text("#" + rank + "  " + row.store, 18, Color.WHITE, true));
            c.addView(text(row.hisenseUnits + " Hisense units - " + DataStore.money(row.hisenseValue), 13, MUTED, false));
            c.addView(text("SOV " + row.sov + "% - Premium " + row.premiumPct + "% - Soundbars " + row.soundbars, 12, SUBTLE, false));
            body.addView(c); body.addView(space(10)); rank++;
        }
    }

    private void tools(LinearLayout body) {
        body.addView(text("Tools", 28, Color.WHITE, true)); body.addView(text("Barcodes, sizing, training, specs, and feedback.", 13, MUTED, false)); body.addView(space(12));
        LinearLayout sku = card(); sku.addView(text("SKU Lookup", 16, Color.WHITE, true)); EditText q = input("U7S, UR9S, C2 Ultra...", "", InputType.TYPE_CLASS_TEXT); TextView result = text("", 13, EMERALD, true); Button search = primary("Search"); search.setOnClickListener(v -> result.setText(spec(q.getText().toString()))); sku.addView(q); sku.addView(space(8)); sku.addView(search); sku.addView(result); body.addView(sku); body.addView(space(12));
        LinearLayout bc = card(); bc.addView(rowButton("Barcodes", "Add", v -> barcodeDialog())); for (DataStore.Barcode b : store.barcodes) { bc.addView(text(b.desc + " - " + b.code, 13, Color.WHITE, false)); bc.addView(text("Expires " + b.expiry, 11, SUBTLE, false)); } body.addView(bc); body.addView(space(12));
        body.addView(info("Sizing Guide", "32-43 in: bedrooms and kitchens\n50-55 in: everyday living rooms\n65-75 in: main living room sweet spot\n85-100 in: cinema and premium demos")); body.addView(space(12));
        body.addView(info("Training Materials", "RGB Mini LED: lead with brightness, contrast, and sports performance.\nLaser Cinema: sell the big-screen experience.\nSoundbar Attach: ask every premium TV customer about audio.")); body.addView(space(12));
        LinearLayout ask = card(); ask.addView(rowButton("Ask an ASM", "Ask", v -> questionDialog())); for (DataStore.Question qu : store.questions) { ask.addView(text(qu.q, 13, Color.WHITE, true)); ask.addView(text(qu.answer.length() == 0 ? "Awaiting answer" : qu.answer, 12, qu.answer.length() == 0 ? MUTED : EMERALD, false)); } body.addView(ask);
    }

    private void commission(LinearLayout body) {
        body.addView(text("Commission", 28, Color.WHITE, true)); body.addView(text("Estimated from local monthly sales for " + store.selectedStore(), 13, MUTED, false)); body.addView(space(12));
        double total = 0; int count = 0;
        for (DataStore.Sale sale : store.sales) if (sale.employee.equals(store.selectedStore()) && sale.date.startsWith(LocalDate.now().toString().substring(0, 7))) { double c = store.commission(sale); if (c > 0) { total += c; count++; } }
        body.addView(kpi("Estimated Commission", DataStore.money(total), count + " qualifying sales this month", EMERALD)); body.addView(space(12));
        body.addView(info("Current Rates", "UHD GBP 8 - QLED GBP 15 - Premium GBP 30\nLaser GBP 45 - RGB Mini LED GBP 50\nSoundbars GBP 7-15"));
    }

    private String scopeKey() { return "region".equals(scope) ? "region" : "mine"; }
    private void saveSale() { double n; try { n = Double.parseDouble(price); } catch (Exception e) { toast("Enter a price"); return; } if (date.trim().isEmpty()) date = DataStore.today(); if ("tv".equals(saleType) && brand.isEmpty()) { toast("Choose a brand"); return; } if ("Hisense".equals(brand) && !refund && (model.isEmpty() || size.isEmpty() || tier.isEmpty())) { toast("Choose model, size, and tier"); return; } if ("soundbar".equals(saleType) && !refund && model.isEmpty()) { toast("Choose a soundbar model"); return; } for (int i = 0; i < qty; i++) { DataStore.Sale s = new DataStore.Sale(); s.employee = store.selectedStore(); s.soundbar = "soundbar".equals(saleType); s.brand = s.soundbar ? "Other" : brand; s.model = s.soundbar || "Hisense".equals(brand) ? model : ""; s.size = "Hisense".equals(brand) ? size : ""; s.tier = "Hisense".equals(brand) ? tier : ""; s.price = refund ? -Math.abs(n) : n; s.refund = refund; s.date = date; store.addSale(s); } brand = model = size = tier = price = ""; refund = false; qty = 1; date = DataStore.today(); toast("Sale logged"); page = "Dashboard"; render(); }
    private void editSale(DataStore.Sale sale) { DataStore.Sale copy = sale.copy(); LinearLayout b = vbox(); b.setPadding(dp(8), dp(8), dp(8), 0); EditText p = input("Price", String.valueOf(Math.abs(copy.price)), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); b.addView(text(copy.name(), 15, Color.WHITE, true)); b.addView(p); new AlertDialog.Builder(this).setTitle("Edit Sale").setView(b).setPositiveButton("Save", (d, w) -> { copy.price = copy.refund ? -Math.abs(num(p.getText().toString())) : num(p.getText().toString()); store.updateSale(copy); render(); }).setNegativeButton("Delete", (d, w) -> { store.deleteSale(copy.id); render(); }).setNeutralButton("Cancel", null).show(); }
    private void addStoreDialog() { LinearLayout b = vbox(); b.setPadding(dp(8), dp(8), dp(8), 0); EditText n = input("Store name", "", InputType.TYPE_CLASS_TEXT); EditText r = input("Region", store.currentRegion().isEmpty() ? "South" : store.currentRegion(), InputType.TYPE_CLASS_TEXT); b.addView(n); b.addView(space(8)); b.addView(r); new AlertDialog.Builder(this).setTitle("Add Store").setView(b).setPositiveButton("Save", (d,w) -> { store.addStore(n.getText().toString(), r.getText().toString()); render(); }).setNegativeButton("Cancel", null).show(); }
    private void barcodeDialog() { LinearLayout b = vbox(); b.setPadding(dp(8), dp(8), dp(8), 0); EditText d = input("Description", "", InputType.TYPE_CLASS_TEXT); EditText e = input("Expiry", "30/06/2026", InputType.TYPE_CLASS_TEXT); EditText c = input("Code", "", InputType.TYPE_CLASS_NUMBER); b.addView(d); b.addView(e); b.addView(c); new AlertDialog.Builder(this).setTitle("Add Barcode").setView(b).setPositiveButton("Save", (x,w) -> { store.barcodes.add(new DataStore.Barcode(d.getText().toString(), e.getText().toString(), c.getText().toString())); toast("Added locally"); render(); }).setNegativeButton("Cancel", null).show(); }
    private void questionDialog() { LinearLayout b = vbox(); b.setPadding(dp(8), dp(8), dp(8), 0); EditText q = input("Question", "", InputType.TYPE_CLASS_TEXT); b.addView(q); new AlertDialog.Builder(this).setTitle("Ask an ASM").setView(b).setPositiveButton("Send", (x,w) -> { store.questions.add(new DataStore.Question(q.getText().toString(), store.selectedStore(), "")); toast("Question saved"); render(); }).setNegativeButton("Cancel", null).show(); }

    private String tierFor(String m) { if (m.startsWith("UR")) return "RGB MINI LED"; if (m.startsWith("C2") || m.startsWith("PX3")) return "LASER"; if (m.contains("Pro") || m.startsWith("U7") || m.startsWith("U8")) return "PREMIUM"; if (m.startsWith("A5") || m.startsWith("A7") || m.startsWith("E7") || m.equals("Canvas") || m.equals("Deco")) return "QLED"; return "UHD"; }
    private String suggested(String m, String s) { int inches = numInt(s, 55); int base = m.startsWith("UR9") ? 2199 : m.startsWith("UR8") ? 1799 : m.startsWith("U8") ? 1299 : m.startsWith("U7") ? 899 : m.startsWith("E7") ? 549 : m.startsWith("A7") ? 449 : m.startsWith("A5") ? 349 : m.startsWith("C2") ? 1799 : m.startsWith("PX3") ? 1499 : 249; return String.valueOf(base + Math.max(0, inches - 55) * 14); }
    private String spec(String q) { String x = q.toUpperCase(Locale.UK); if (x.contains("UR9")) return "UR9S - RGB Mini LED flagship, premium brightness, gaming, high-end demo focus."; if (x.contains("UR8")) return "UR8S - RGB Mini LED step-up premium range."; if (x.contains("U7")) return "U7S / U7S Pro - premium Mini LED value for sports, movies, and gaming."; if (x.contains("E7")) return "E7S / E7S Pro - QLED everyday upsell range."; if (x.contains("C2") || x.contains("PX3")) return "Laser Cinema - big-screen premium home cinema."; if (x.contains("AX")) return "Hisense soundbar - attach to TV sales for audio upgrade."; return "Try U7S, UR9S, E7S, C2 Ultra, or AX5140Q."; }

    private LinearLayout saleRow(DataStore.Sale s) { LinearLayout r = vbox(); r.setPadding(0, dp(12), 0, dp(12)); r.addView(text(s.name(), 15, Color.WHITE, true)); r.addView(text(DataStore.money(s.price) + " - " + s.date + (s.refund ? " - refund" : ""), 12, s.refund ? RED : MUTED, false)); return r; }
    private LinearLayout kpi(String t, String v, String sub, int color) { LinearLayout c = card(); c.addView(text(t, 11, MUTED, true)); c.addView(text(v, v.length() > 9 ? 22 : 28, color, true)); c.addView(text(sub, 11, SUBTLE, false)); return c; }
    private LinearLayout info(String t, String b) { LinearLayout c = card(); c.addView(text(t, 16, Color.WHITE, true)); c.addView(text(b, 13, MUTED, false)); return c; }
    private LinearLayout rowButton(String t, String b, View.OnClickListener l) { LinearLayout r = hbox(); r.setGravity(Gravity.CENTER_VERTICAL); r.addView(text(t, 16, Color.WHITE, true), new LinearLayout.LayoutParams(0, -2, 1)); Button btn = small(b); btn.setOnClickListener(l); r.addView(btn); return r; }
    private LinearLayout rowText(String l, String r) { LinearLayout row = hbox(); row.setGravity(Gravity.CENTER_VERTICAL); row.addView(text(l, 16, Color.WHITE, true), new LinearLayout.LayoutParams(0, -2, 1)); row.addView(text(r, 12, VIOLET, true)); return row; }
    private TextView section(String t) { TextView v = text(t, 11, MUTED, true); v.setPadding(0, dp(14), 0, dp(8)); return v; }
    private View seg(String[] vals, String[] labels, String selected, Setter setter) { LinearLayout r = hbox(); r.setPadding(dp(4), dp(4), dp(4), dp(4)); r.setBackground(bg(PANEL, 12, Color.TRANSPARENT)); for (int i = 0; i < vals.length; i++) { String val = vals[i]; Button b = chip(labels[i], val.equals(selected)); b.setOnClickListener(v -> setter.set(val)); r.addView(b, new LinearLayout.LayoutParams(0, dp(42), 1)); } return r; }
    private LinearLayout grid(List<String> vals, String selected, int cols, Setter setter) { LinearLayout g = vbox(); LinearLayout row = null; for (int i = 0; i < vals.size(); i++) { if (i % cols == 0) { row = hbox(); g.addView(row); } String val = vals.get(i); Button b = chip(val, val.equals(selected)); b.setTextSize(12); b.setOnClickListener(v -> setter.set(val)); row.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1)); } return g; }
    private View progress(int pct, int color) { FrameLayout f = new FrameLayout(this); f.setBackground(bg(PANEL, 12, Color.TRANSPARENT)); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(12)); lp.setMargins(0, dp(10), 0, 0); f.setLayoutParams(lp); View bar = new View(this); bar.setBackground(bg(color, 12, Color.TRANSPARENT)); f.addView(bar, new FrameLayout.LayoutParams(Math.max(dp(5), dp(Math.min(100, Math.max(0, pct)) * 3)), -1)); return f; }
    private EditText input(String h, String v, int type) { EditText e = new EditText(this); e.setHint(h); e.setText(v); e.setInputType(type); e.setSingleLine(true); e.setTextColor(Color.WHITE); e.setHintTextColor(SUBTLE); e.setTextSize(15); e.setPadding(dp(14), 0, dp(14), 0); e.setBackground(bg(PANEL, 12, Color.rgb(45, 55, 72))); e.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(50))); return e; }
    private Button primary(String t) { Button b = button(t, VIOLET, Color.WHITE); b.setTypeface(Typeface.DEFAULT_BOLD); return b; }
    private Button small(String t) { Button b = button(t, Color.rgb(54, 38, 91), Color.WHITE); b.setTextSize(12); b.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(36))); return b; }
    private Button outline(String t) { Button b = button(t, PANEL, MUTED); b.setBackground(bg(PANEL, 12, Color.rgb(48, 56, 76))); return b; }
    private Button chip(String t, boolean active) { Button b = button(t, active ? VIOLET : PANEL, active ? Color.WHITE : MUTED); b.setTextSize(13); b.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(40))); return b; }
    private Button button(String t, int bg, int fg) { Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(fg); b.setMinHeight(0); b.setMinimumHeight(0); b.setPadding(dp(12), 0, dp(12), 0); b.setBackground(bg(bg, 12, Color.TRANSPARENT)); b.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(46))); return b; }
    private LinearLayout card() { LinearLayout c = vbox(); c.setPadding(dp(16), dp(14), dp(16), dp(14)); c.setBackground(bg(CARD, 16, Color.TRANSPARENT)); return c; }
    private LinearLayout panel(int stroke) { LinearLayout c = card(); c.setBackground(bg(CARD, 18, stroke)); return c; }
    private TextView text(String s, int sp, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextColor(color); t.setTextSize(sp); if (bold) t.setTypeface(Typeface.DEFAULT_BOLD); t.setLineSpacing(dp(2), 1f); return t; }
    private LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private GradientDrawable bg(int color, int radius, int stroke) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke); return d; }
    private View space(int h) { Space s = new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); return s; }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private double num(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }
    private int numInt(String s, int fallback) { try { return Integer.parseInt(s); } catch (Exception e) { return fallback; } }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    interface Setter { void set(String v); }

    public class BrandChart extends View { private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); private final Map<String, Double> data; BrandChart(Map<String, Double> data) { super(MainActivity.this); this.data = data; } @Override protected void onDraw(Canvas c) { int w = getWidth(); double max = 1; for (double v : data.values()) max = Math.max(max, v); int y = 28, i = 0; int[] colors = {VIOLET, EMERALD, Color.rgb(59,130,246), RED, AMBER}; for (String k : data.keySet()) { double v = data.get(k); p.setColor(MUTED); p.setTextSize(24); c.drawText(k, 16, y + 18, p); p.setColor(Color.rgb(45,55,72)); c.drawRoundRect(116, y, w - 16, y + 18, 12, 12, p); p.setColor(colors[i++ % colors.length]); c.drawRoundRect(116, y, (float)(116 + (w - 132) * (v / max)), y + 18, 12, 12, p); p.setColor(Color.WHITE); p.setTextSize(21); c.drawText(DataStore.money(v), w - 120, y + 18, p); y += 34; } } }
    public class WeekChart extends View { private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); private final Map<String, Integer> data; WeekChart(Map<String, Integer> data) { super(MainActivity.this); this.data = data; } @Override protected void onDraw(Canvas c) { int w = getWidth(), h = getHeight(), left = 20, bottom = h - 32, top = 18, gap = dp(8), bw = Math.max(10, (w - 44 - gap * 6) / 7), i = 0; p.setStrokeWidth(2); p.setColor(Color.rgb(45,55,72)); c.drawLine(left, bottom - (bottom-top)*.1f, w-12, bottom - (bottom-top)*.1f, p); c.drawLine(left, bottom - (bottom-top)*.2f, w-12, bottom - (bottom-top)*.2f, p); for (String d : data.keySet()) { int pct = data.get(d); float x = left + i * (bw + gap), bh = (bottom-top) * Math.min(100, pct) / 100f; p.setColor(pct >= 20 ? EMERALD : pct >= 10 ? AMBER : VIOLET); c.drawRoundRect(x, bottom - bh, x + bw, bottom, 12, 12, p); p.setColor(MUTED); p.setTextSize(20); c.drawText(d, x, h - 8, p); p.setColor(Color.WHITE); c.drawText(pct + "%", x, Math.max(top + 16, bottom - bh - 5), p); i++; } } }
}
