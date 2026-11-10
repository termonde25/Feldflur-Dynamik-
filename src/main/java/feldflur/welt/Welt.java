package feldflur.welt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Das Weltmodell: Vegetation und Hecken auf einem Raster, dazu die Körper aller Tiere.
 * Reines Java – kein Jadex, keine Grafik.
 */
public class Welt
{
    public static final int SPALTEN = 50;
    public static final int ZEILEN = 30;
    public static final float ZELLE = 2f; // eine Zelle ist 2 x 2 Meter

    /** Wie viele kleine Büsche zufällig verteilt werden. */
    public static final int ANZAHL_BUESCHE = 8;

    /** So nah muss ein Fuchs an einem Hasen sein, um ihn zu fangen (Meter). */
    public static final float FANG_RADIUS = 1.0f;

    private final float[][] vegetation = new float[SPALTEN][ZEILEN];
    private final boolean[][] hecke = new boolean[SPALTEN][ZEILEN];
    private final Map<Integer, Tier> tiere = new LinkedHashMap<>();
    private final Random zufall = new Random(42);
    private int naechsteId = 1;
    private long tick = 0;
    private int gefressen = 0;

    public Welt()
    {
        // Am Anfang ist überall etwas Gras (zwischen 0.5 und 1.0)
        for (int sx = 0; sx < SPALTEN; sx++)
            for (int sy = 0; sy < ZEILEN; sy++)
                vegetation[sx][sy] = 0.5f + zufall.nextFloat() * 0.5f;

        legeHeckenAn();
    }

    // ------------------------------------------------------------------
    // Hecken und Büsche
    // ------------------------------------------------------------------

    /** Ein langer Feldrain und mehrere kleine Büsche. */
    private void legeHeckenAn()
    {
        // Ein langer Feldrain (eine Zelle breit, 20 Zellen = 40 m lang)
        heckenBlock(10, 18, 20, 1);

        // Kleine Büsche: 2x2 oder 3x2 Zellen, zufällig verteilt
        for (int i = 0; i < ANZAHL_BUESCHE; i++)
        {
            int breite = 2 + zufall.nextInt(2);   // 2 oder 3
            int hoehe = 2;
            int x = 1 + zufall.nextInt(SPALTEN - breite - 2);
            int y = 1 + zufall.nextInt(ZEILEN - hoehe - 2);
            heckenBlock(x, y, breite, hoehe);
        }
    }

    /** Setzt einen rechteckigen Block (in Zellen) auf Hecke. */
    private void heckenBlock(int x, int y, int breite, int hoehe)
    {
        for (int sx = x; sx < x + breite && sx < SPALTEN; sx++)
            for (int sy = y; sy < y + hoehe && sy < ZEILEN; sy++)
                hecke[sx][sy] = true;
    }

    /** Steht an dieser Position (in Metern) eine Hecke? */
    public boolean istHecke(float x, float y)
    {
        int sx = Math.min(SPALTEN - 1, Math.max(0, (int) (x / ZELLE)));
        int sy = Math.min(ZEILEN - 1, Math.max(0, (int) (y / ZELLE)));
        return hecke[sx][sy];
    }

    /** Hecke an einer Zelle entfernen (später für den Landwirt). */
    public void maeheHecke(int sx, int sy)
    {
        hecke[sx][sy] = false;
    }

    // ------------------------------------------------------------------
    // Tiere
    // ------------------------------------------------------------------

    /** Legt ein neues Tier an einer zufälligen Stelle an und gibt seine ID zurück. */
    public int neuesTier(Art art)
    {
        float x = zufall.nextFloat() * breite();
        float y = zufall.nextFloat() * hoehe();
        Tier t = new Tier(naechsteId++, art, x, y);
        tiere.put(t.id, t);
        return t.id;
    }

    /** Lebt das Tier mit dieser ID noch? */
    public boolean lebt(int id)
    {
        return tiere.containsKey(id);
    }

    /** Ein Tier möchte zu (x, y). Die Bewegung passiert erst in tick(). */
    public void setzeZiel(int id, float x, float y)
    {
        Tier t = tiere.get(id);
        if (t == null)
            return;
        t.zielX = Math.max(0, Math.min(breite(), x)); // nicht aus der Welt laufen
        t.zielY = Math.max(0, Math.min(hoehe(), y));
        t.unterwegs = true;
    }

    /**
     * Was kann dieses Tier gerade sehen?
     * Nur Tiere im eigenen Sichtradius. Füchse sehen keine Hasen in Hecken.
     */
    public Wahrnehmung wahrnehmung(int id)
    {
        Tier t = tiere.get(id);
        if (t == null)
            return Wahrnehmung.tot();

        List<Sichtung> gesehen = new ArrayList<>();
        for (Tier o : tiere.values())
        {
            if (o == t)
                continue;
            float d = abstand(t.x, t.y, o.x, o.y);
            if (d > t.art.sicht)
                continue;
            // Deckung: Ein Hase im Busch ist für Füchse unsichtbar
            if (t.art == Art.FUCHS && o.art == Art.HASE && istHecke(o.x, o.y))
                continue;
            gesehen.add(new Sichtung(o.id, o.art, o.x, o.y, d));
        }
        return new Wahrnehmung(true, t.x, t.y, istHecke(t.x, t.y), List.copyOf(gesehen));
    }

    /**
     * Ein Fuchs versucht, einen Hasen zu fangen.
     * Die Welt entscheidet eindeutig: Ein Hase kann nur einmal gefressen werden.
     */
    public boolean fangen(int fuchsId, int haseId)
    {
        Tier fuchs = tiere.get(fuchsId);
        Tier hase = tiere.get(haseId);
        if (fuchs == null || hase == null || fuchs.art != Art.FUCHS || hase.art != Art.HASE)
            return false;
        if (abstand(fuchs.x, fuchs.y, hase.x, hase.y) > FANG_RADIUS)
            return false;
        if (istHecke(hase.x, hase.y))
            return false; // im Busch sicher

        tiere.remove(haseId);
        gefressen++;
        return true;
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    /**
     * Ein Simulationsschritt.
     * @return IDs der Tiere, die in diesem Tick an ihrem Ziel angekommen sind
     */
    public List<Integer> tick()
    {
        tick++;

        // 1. Umwelt aktualisieren: Gras wächst langsam nach
        for (int sx = 0; sx < SPALTEN; sx++)
            for (int sy = 0; sy < ZEILEN; sy++)
                vegetation[sx][sy] = Math.min(1f, vegetation[sx][sy] + 0.001f);

        // 3. Tiere ein Stück Richtung Ziel bewegen
        List<Integer> angekommen = new ArrayList<>();
        for (Tier t : tiere.values())
        {
            if (!t.unterwegs)
                continue;

            float dx = t.zielX - t.x;
            float dy = t.zielY - t.y;
            float abstand = (float) Math.sqrt(dx * dx + dy * dy);

            if (abstand <= t.art.tempo)
            {
                // letzter Schritt: genau aufs Ziel
                t.x = t.zielX;
                t.y = t.zielY;
                t.unterwegs = false;
                angekommen.add(t.id);
            }
            else
            {
                // ein Schritt der Länge "tempo" in Zielrichtung
                t.x += dx / abstand * t.art.tempo;
                t.y += dy / abstand * t.art.tempo;
            }
        }
        return angekommen;
    }

    // ------------------------------------------------------------------
    // Anzeige und Abfragen
    // ------------------------------------------------------------------

    /** Erstellt eine Kopie des aktuellen Zustands für die Anzeige. */
    public Schnappschuss schnappschuss()
    {
        float[][] veg = new float[SPALTEN][];
        boolean[][] hk = new boolean[SPALTEN][];
        for (int sx = 0; sx < SPALTEN; sx++)
        {
            veg[sx] = vegetation[sx].clone();
            hk[sx] = hecke[sx].clone();
        }

        // Positionen nach Art getrennt sammeln
        List<Float> hasen = new ArrayList<>();
        List<Float> fuechse = new ArrayList<>();
        for (Tier t : tiere.values())
        {
            List<Float> liste = t.art == Art.HASE ? hasen : fuechse;
            liste.add(t.x);
            liste.add(t.y);
        }
        return new Schnappschuss(tick, veg, hk, zuArray(hasen), zuArray(fuechse), gefressen);
    }

    private static float[] zuArray(List<Float> liste)
    {
        float[] a = new float[liste.size()];
        for (int i = 0; i < a.length; i++)
            a[i] = liste.get(i);
        return a;
    }

    private static float abstand(float x1, float y1, float x2, float y2)
    {
        float dx = x2 - x1, dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public float breite() { return SPALTEN * ZELLE; }
    public float hoehe()  { return ZEILEN * ZELLE; }
    public long getTick() { return tick; }
    public float getX(int id) { return tiere.get(id).x; }
    public float getY(int id) { return tiere.get(id).y; }
}