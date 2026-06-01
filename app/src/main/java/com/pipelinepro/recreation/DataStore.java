package com.pipelinepro.recreation;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DataStore {
    private static final String PREFS = "pipelinepro_native";
    private static final String SEEDED = "seeded_v2";
    private final SharedPreferences prefs;

    public final List<Sale> sales = new ArrayList<>();
    public final List<Store> stores = new ArrayList<>();
    public final List<String> regions = new ArrayList<>();
    public final List<Barcode> barcodes = new ArrayList<>();
    public final List<Question> questions = new ArrayList<>();
    public final List<Incentive> incentives = new ArrayList<>();

    public DataStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(SEEDED, false)) {
            seed();
            saveAll();
            prefs.edit().putBoolean(SEEDED, true).apply();
        } else {
            loadAll();
        }
    }

    public String selectedStore() {
        return prefs.getString("store", null);
    }

    public void selectStore(String name) {
        prefs.edit().putString("store", name).apply();
    }

    public void clearStore() {
        prefs.edit().remove("store").apply();
    }

    public Store currentStore() {
        for (Store store : stores) if (store.name.equals(selectedStore())) return store;
        return null;
    }

    public String currentRegion() {
        Store store = currentStore();
        return store == null ? "" : store.region;
    }

    public void addStore(String name, String region) {
        if (name.trim().isEmpty() || region.trim().isEmpty()) return;
        for (Store store : stores) {
            if (store.name.equalsIgnoreCase(name.trim())) {
                store.region = region.trim();
                saveStores();
                return;
            }
        }
        stores.add(new Store(id(), name.trim(), region.trim()));
        if (!regions.contains(region.trim())) regions.add(region.trim());
        Collections.sort(regions);
        saveStores();
        saveRegions();
    }

    public List<String> storesInRegion(String region) {
        List<String> names = new ArrayList<>();
        for (Store store : stores) if (region == null || region.length() == 0 || store.region.equals(region)) names.add(store.name);
        Collections.sort(names);
        return names;
    }

    public void addSale(Sale sale) {
        sale.id = id();
        sale.created = System.currentTimeMillis();
        sales.add(0, sale);
        saveSales();
    }

    public void updateSale(Sale sale) {
        for (int i = 0; i < sales.size(); i++) {
            if (sales.get(i).id.equals(sale.id)) {
                sales.set(i, sale);
                saveSales();
                return;
            }
        }
    }

    public void deleteSale(String id) {
        for (int i = sales.size() - 1; i >= 0; i--) if (sales.get(i).id.equals(id)) sales.remove(i);
        saveSales();
    }

    public List<Sale> todaysSales(String store) {
        List<Sale> out = new ArrayList<>();
        for (Sale sale : sales) if (today().equals(sale.date) && sale.employee.equals(store)) out.add(sale);
        Collections.sort(out, (a, b) -> Long.compare(b.created, a.created));
        return out;
    }

    public List<Sale> filtered(String period, String scope) {
        LinkedHashSet<String> regionStores = new LinkedHashSet<>(storesInRegion(currentRegion()));
        List<Sale> out = new ArrayList<>();
        for (Sale sale : sales) {
            if (!inPeriod(sale.date, period)) continue;
            if ("mine".equals(scope) && !sale.employee.equals(selectedStore())) continue;
            if ("region".equals(scope) && !regionStores.contains(sale.employee)) continue;
            out.add(sale);
        }
        return out;
    }

    public Stats stats(List<Sale> source, String scope) {
        Stats stats = new Stats();
        LinkedHashSet<String> regionStores = new LinkedHashSet<>(storesInRegion(currentRegion()));
        for (Sale sale : source) {
            if (sale.soundbar) continue;
            if (!sale.refund) {
                stats.tvUnits++;
                stats.tvValue += sale.price;
            }
            if ("Hisense".equals(sale.brand)) {
                if (!sale.refund) {
                    stats.hisenseUnits++;
                    stats.hisenseValue += sale.price;
                    if (sale.premium()) stats.premiumUnits++;
                } else {
                    stats.hisenseValue += sale.price;
                }
            }
        }
        for (Sale sale : sales) {
            if (!today().equals(sale.date) || !sale.soundbar) continue;
            if ("mine".equals(scope) && !sale.employee.equals(selectedStore())) continue;
            if ("region".equals(scope) && !regionStores.contains(sale.employee)) continue;
            stats.soundbarUnits++;
            stats.soundbarValue += sale.price;
        }
        stats.asp = stats.hisenseUnits == 0 ? 0 : stats.hisenseValue / stats.hisenseUnits;
        stats.sov = stats.tvValue == 0 ? 0 : Math.round((float) (stats.hisenseValue * 100 / stats.tvValue));
        stats.premiumMix = stats.hisenseUnits == 0 ? 0 : Math.round((float) (stats.premiumUnits * 100 / stats.hisenseUnits));
        return stats;
    }

    public List<Row> leaderboard() {
        Map<String, Row> rows = new LinkedHashMap<>();
        for (String store : storesInRegion(currentRegion())) rows.put(store, new Row(store));
        for (Sale sale : sales) {
            if (!inPeriod(sale.date, "week")) continue;
            Row row = rows.get(sale.employee);
            if (row == null) continue;
            if (sale.soundbar) {
                row.soundbars++;
                continue;
            }
            if (sale.refund) continue;
            row.totalValue += sale.price;
            if ("Hisense".equals(sale.brand)) {
                row.hisenseUnits++;
                row.hisenseValue += sale.price;
                if (sale.premium()) row.premiumUnits++;
            }
        }
        List<Row> out = new ArrayList<>(rows.values());
        for (Row row : out) row.finish();
        out.sort((a, b) -> {
            int byUnits = Integer.compare(b.hisenseUnits, a.hisenseUnits);
            return byUnits != 0 ? byUnits : Double.compare(b.hisenseValue, a.hisenseValue);
        });
        return out;
    }

    public Map<String, Double> brandValues(List<Sale> source) {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("Hisense", 0d);
        map.put("LG", 0d);
        map.put("Samsung", 0d);
        map.put("Sony", 0d);
        map.put("Other", 0d);
        for (Sale sale : source) {
            if (sale.soundbar || sale.refund) continue;
            String brand = map.containsKey(sale.brand) ? sale.brand : "Other";
            map.put(brand, map.get(brand) + sale.price);
        }
        return map;
    }

    public Map<String, Integer> weeklySov(String scope) {
        Map<String, Integer> map = new LinkedHashMap<>();
        LocalDate start = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() % 7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            double total = 0;
            double hisense = 0;
            for (Sale sale : filtered("week", scope)) {
                if (!sale.date.equals(day.toString()) || sale.soundbar || sale.refund) continue;
                total += sale.price;
                if ("Hisense".equals(sale.brand)) hisense += sale.price;
            }
            map.put(day.format(DateTimeFormatter.ofPattern("EEE", Locale.UK)), total == 0 ? 0 : Math.round((float) (hisense * 100 / total)));
        }
        return map;
    }

    public double commission(Sale sale) {
        if (sale.refund) return 0;
        if (sale.soundbar) return sale.model.contains("5140") ? 15 : sale.model.contains("5100") ? 10 : 7;
        if (!"Hisense".equals(sale.brand)) return 0;
        if ("RGB MINI LED".equals(sale.tier)) return 50;
        if ("LASER".equals(sale.tier)) return 45;
        if ("PREMIUM".equals(sale.tier)) return 30;
        if ("QLED".equals(sale.tier)) return 15;
        return 8;
    }

    private boolean inPeriod(String isoDate, String period) {
        LocalDate date;
        try { date = LocalDate.parse(isoDate); } catch (Exception ex) { return false; }
        LocalDate now = LocalDate.now();
        if ("today".equals(period)) return date.equals(now);
        if ("week".equals(period)) return !date.isBefore(now.minusDays(now.getDayOfWeek().getValue() % 7));
        if ("month".equals(period)) return date.getYear() == now.getYear() && date.getMonth() == now.getMonth();
        return date.getYear() == now.getYear();
    }

    private void seed() {
        regions.clear();
        Collections.addAll(regions, "North", "South", "Wales", "Scotland");
        stores.clear();
        stores.add(new Store(id(), "Currys Bristol", "South"));
        stores.add(new Store(id(), "AO Birmingham", "South"));
        stores.add(new Store(id(), "Argos Manchester", "North"));
        stores.add(new Store(id(), "Currys Leeds", "North"));
        stores.add(new Store(id(), "John Lewis Cardiff", "Wales"));
        stores.add(new Store(id(), "Costco Glasgow", "Scotland"));
        barcodes.clear();
        barcodes.add(new Barcode("U7S launch bundle", "30/06/2026", "501234567890123"));
        barcodes.add(new Barcode("Soundbar attach offer", "15/07/2026", "501234567890789"));
        questions.clear();
        questions.add(new Question("Can soundbars count toward units?", "Currys Bristol", "Yes, for units incentives. SOV uses TV revenue only."));
        incentives.clear();
        incentives.add(new Incentive("South", "sov", 20, "Hit 20% Hisense share of value today."));
        sales.clear();
        addSeed("Currys Bristol", "Hisense", "U7S", "65", "PREMIUM", 999, false, false, today());
        addSeed("Currys Bristol", "LG", "", "", "", 899, false, false, today());
        addSeed("Currys Bristol", "Other", "AX5140Q", "", "", 249, true, false, today());
        addSeed("AO Birmingham", "Hisense", "A7S", "55", "QLED", 549, false, false, LocalDate.now().minusDays(1).toString());
        addSeed("Argos Manchester", "Samsung", "", "", "", 699, false, false, today());
        addSeed("Currys Leeds", "Hisense", "C2 Ultra", "150", "LASER", 2499, false, false, LocalDate.now().minusDays(2).toString());
    }

    private void addSeed(String employee, String brand, String model, String size, String tier, double price, boolean soundbar, boolean refund, String date) {
        Sale sale = new Sale();
        sale.id = id();
        sale.employee = employee;
        sale.brand = brand;
        sale.model = model;
        sale.size = size;
        sale.tier = tier;
        sale.price = refund ? -Math.abs(price) : price;
        sale.soundbar = soundbar;
        sale.refund = refund;
        sale.date = date;
        sale.created = System.currentTimeMillis();
        sales.add(sale);
    }

    private void loadAll() {
        regions.clear(); stores.clear(); sales.clear(); barcodes.clear(); questions.clear(); incentives.clear();
        JSONArray regionArray = array("regions");
        for (int i = 0; i < regionArray.length(); i++) regions.add(regionArray.optString(i));
        JSONArray storeArray = array("stores");
        for (int i = 0; i < storeArray.length(); i++) stores.add(Store.from(storeArray.optJSONObject(i)));
        JSONArray saleArray = array("sales");
        for (int i = 0; i < saleArray.length(); i++) sales.add(Sale.from(saleArray.optJSONObject(i)));
        JSONArray barcodeArray = array("barcodes");
        for (int i = 0; i < barcodeArray.length(); i++) barcodes.add(Barcode.from(barcodeArray.optJSONObject(i)));
        JSONArray questionArray = array("questions");
        for (int i = 0; i < questionArray.length(); i++) questions.add(Question.from(questionArray.optJSONObject(i)));
        JSONArray incentiveArray = array("incentives");
        for (int i = 0; i < incentiveArray.length(); i++) incentives.add(Incentive.from(incentiveArray.optJSONObject(i)));
    }

    private void saveAll() { saveRegions(); saveStores(); saveSales(); saveBarcodes(); saveQuestions(); saveIncentives(); }
    private void saveRegions() { JSONArray a = new JSONArray(); for (String r : regions) a.put(r); save("regions", a); }
    private void saveStores() { JSONArray a = new JSONArray(); for (Store s : stores) a.put(s.json()); save("stores", a); }
    private void saveSales() { JSONArray a = new JSONArray(); for (Sale s : sales) a.put(s.json()); save("sales", a); }
    private void saveBarcodes() { JSONArray a = new JSONArray(); for (Barcode b : barcodes) a.put(b.json()); save("barcodes", a); }
    private void saveQuestions() { JSONArray a = new JSONArray(); for (Question q : questions) a.put(q.json()); save("questions", a); }
    private void saveIncentives() { JSONArray a = new JSONArray(); for (Incentive i : incentives) a.put(i.json()); save("incentives", a); }
    private JSONArray array(String key) { try { return new JSONArray(prefs.getString(key, "[]")); } catch (JSONException e) { return new JSONArray(); } }
    private void save(String key, JSONArray value) { prefs.edit().putString(key, value.toString()).apply(); }
    private static String id() { return UUID.randomUUID().toString(); }
    public static String today() { return LocalDate.now().toString(); }
    public static String money(double v) { return (v < 0 ? "-GBP " : "GBP ") + String.format(Locale.UK, "%,.0f", Math.abs(v)); }

    public static class Sale {
        public String id = id(), employee = "", brand = "", model = "", size = "", tier = "", date = today();
        public double price = 0;
        public boolean soundbar = false, refund = false;
        public long created = System.currentTimeMillis();
        public boolean premium() { return "PREMIUM".equals(tier) || "LASER".equals(tier) || "RGB MINI LED".equals(tier); }
        public String name() { return soundbar ? "Soundbar " + model : (brand + ("Hisense".equals(brand) ? " " + model + " " + size + "\"" : "")).trim(); }
        JSONObject json() { JSONObject j = new JSONObject(); try { j.put("id", id); j.put("employee", employee); j.put("brand", brand); j.put("model", model); j.put("size", size); j.put("tier", tier); j.put("date", date); j.put("price", price); j.put("soundbar", soundbar); j.put("refund", refund); j.put("created", created); } catch (JSONException ignored) {} return j; }
        static Sale from(JSONObject j) { Sale s = new Sale(); if (j == null) return s; s.id = j.optString("id", id()); s.employee = j.optString("employee"); s.brand = j.optString("brand"); s.model = j.optString("model"); s.size = j.optString("size"); s.tier = j.optString("tier"); s.date = j.optString("date", today()); s.price = j.optDouble("price"); s.soundbar = j.optBoolean("soundbar"); s.refund = j.optBoolean("refund"); s.created = j.optLong("created", System.currentTimeMillis()); return s; }
        Sale copy() { Sale s = new Sale(); s.id=id; s.employee=employee; s.brand=brand; s.model=model; s.size=size; s.tier=tier; s.date=date; s.price=price; s.soundbar=soundbar; s.refund=refund; s.created=created; return s; }
    }

    public static class Store {
        public String id, name, region;
        Store(String id, String name, String region) { this.id = id; this.name = name; this.region = region; }
        JSONObject json() { JSONObject j = new JSONObject(); try { j.put("id", id); j.put("name", name); j.put("region", region); } catch (JSONException ignored) {} return j; }
        static Store from(JSONObject j) { return new Store(j == null ? id() : j.optString("id", id()), j == null ? "" : j.optString("name"), j == null ? "" : j.optString("region")); }
    }

    public static class Barcode {
        public String desc, expiry, code;
        Barcode(String desc, String expiry, String code) { this.desc = desc; this.expiry = expiry; this.code = code; }
        JSONObject json() { JSONObject j = new JSONObject(); try { j.put("desc", desc); j.put("expiry", expiry); j.put("code", code); } catch (JSONException ignored) {} return j; }
        static Barcode from(JSONObject j) { return new Barcode(j == null ? "" : j.optString("desc"), j == null ? "" : j.optString("expiry"), j == null ? "" : j.optString("code")); }
    }

    public static class Question {
        public String q, by, answer;
        Question(String q, String by, String answer) { this.q = q; this.by = by; this.answer = answer; }
        JSONObject json() { JSONObject j = new JSONObject(); try { j.put("q", q); j.put("by", by); j.put("answer", answer); } catch (JSONException ignored) {} return j; }
        static Question from(JSONObject j) { return new Question(j == null ? "" : j.optString("q"), j == null ? "" : j.optString("by"), j == null ? "" : j.optString("answer")); }
    }

    public static class Incentive {
        public String region, metric, desc;
        public double target;
        Incentive(String region, String metric, double target, String desc) { this.region = region; this.metric = metric; this.target = target; this.desc = desc; }
        JSONObject json() { JSONObject j = new JSONObject(); try { j.put("region", region); j.put("metric", metric); j.put("target", target); j.put("desc", desc); } catch (JSONException ignored) {} return j; }
        static Incentive from(JSONObject j) { return new Incentive(j == null ? "" : j.optString("region"), j == null ? "units" : j.optString("metric"), j == null ? 0 : j.optDouble("target"), j == null ? "" : j.optString("desc")); }
    }

    public static class Stats { public int tvUnits, hisenseUnits, premiumUnits, premiumMix, sov, soundbarUnits; public double tvValue, hisenseValue, asp, soundbarValue; }
    public static class Row { public String store; public int hisenseUnits, premiumUnits, premiumPct, sov, soundbars; public double hisenseValue, totalValue; Row(String store) { this.store = store; } void finish() { premiumPct = hisenseUnits == 0 ? 0 : Math.round((float) premiumUnits * 100 / hisenseUnits); sov = totalValue == 0 ? 0 : Math.round((float) (hisenseValue * 100 / totalValue)); } }
}
