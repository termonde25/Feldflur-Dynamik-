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

    // ---------- Büsche ----------
    /** Anzahl großer Büsche (4 x 3 Zellen). */
    public static final int ANZAHL_GROSS = 1;
    /** Anzahl mittlerer Büsche (3 x 2 oder 2 x 3 Zellen). */
    public static final int ANZAHL_MITTEL = 3;
    /** Anzahl kleiner Büsche (2 x 2 Zellen). */
    public static final int ANZAHL_KLEIN = 4;
    /** Mindestabstand zwischen zwei Büschen (in Zellen, 6 Zellen = 12 m). */
    public static final int BUSCH_ABSTAND = 6;

    // ---------- Gras und Hunger ----------
    /** So viel Gras wächst pro Tick in jeder Zelle nach (langsam -> Fraßspuren bleiben sichtbar). */
    public static final float WACHSTUM = 0.0003f;
    /** So viel Energie verliert ein Hase pro Tick. */
    public static final float HASE_HUNGER = 0.04f;
    /** So viel Gras frisst ein Hase pro Fress-Tick. */
    public static final float BISS = 0.005f;
    /** So viel Energie bringt 1.0 Gras. */
    public static final float ENERGIE_PRO_GRAS = 100f;
    /** Unter diesem Grasstand lohnt sich Fressen nicht. */
    public static final float MIN_GRAS = 0.15f;

    /** So nah muss ein Fuchs an einem Hasen sein, um ihn zu fangen (Meter). */
    public static final float FANG_RADIUS = 1.0f;

    /** Radius des Fuchsreviers um den Bau (Meter). */
    public static final float REVIER_RADIUS = 30f;

    /** Mindestabstand eines Fuchsbaus zum Kartenrand (Meter). */
    public static final float BAU_RAND = 10f;

    /** Aus dieser Nähe bemerkt ein Hase einen lauernden (geduckten) Fuchs (Meter). */
    public static final float LAUER_SICHTWEITE = 2.5f;

    /** Ansprung aus der Lauer: so viel schneller ... */
    public static final float SPRINT_FAKTOR = 1.8f;

    /** ... für so viele Ticks (8 x 50 ms = 0.4 s – ein kurzer Satz). */
    public static final int SPRINT_TICKS = 8;

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

    /**
     * Verteilt Büsche über das Feld: erst die großen, dann die mittleren,
     * dann die kleinen (große finden schwerer Platz, kleine passen in Lücken).
     * Zwischen zwei Büschen bleiben mindestens BUSCH_ABSTAND Zellen frei.
     */
    private void legeHeckenAn()
    {
        for (int i = 0; i < ANZAHL_GROSS; i++)
            setzeBusch(4, 3);

        for (int i = 0; i < ANZAHL_MITTEL; i++)
        {
            if (zufall.nextBoolean())
                setzeBusch(3, 2);   // quer
            else
                setzeBusch(2, 3);   // hochkant
        }

        for (int i = 0; i < ANZAHL_KLEIN; i++)
            setzeBusch(2, 2);
    }

    /**
     * Sucht eine freie Stelle für einen Busch der Größe breite x hoehe (in Zellen)
     * und setzt ihn dort. Probiert bis zu 100 zufällige Stellen; findet sich keine,
     * wird der Busch weggelassen (das Feld ist dann einfach zu voll).
     */
    private void setzeBusch(int breite, int hoehe)
    {
        for (int versuch = 0; versuch < 100; versuch++)
        {
            // 1 Zelle Abstand zum Kartenrand
            int x = 1 + zufall.nextInt(SPALTEN - breite - 2);
            int y = 1 + zufall.nextInt(ZEILEN - hoehe - 2);

            if (platzFrei(x, y, breite, hoehe))
            {
                heckenBlock(x, y, breite, hoehe);
                return;
            }
        }
        System.out.println("Kein Platz mehr für einen Busch " + breite + "x" + hoehe);
    }

    /**
     * Ist der Bereich um den geplanten Busch frei?
     * Geprüft wird der Busch selbst plus BUSCH_ABSTAND Zellen rundherum.
     */
    private boolean platzFrei(int x, int y, int breite, int hoehe)
    {
        for (int sx = x - BUSCH_ABSTAND; sx < x + breite + BUSCH_ABSTAND; sx++)
        {
            for (int sy = y - BUSCH_ABSTAND; sy < y + hoehe + BUSCH_ABSTAND; sy++)
            {
                if (sx < 0 || sy < 0 || sx >= SPALTEN || sy >= ZEILEN)
                    continue;   // außerhalb der Karte zählt als frei
                if (hecke[sx][sy])
                    return false;
            }
        }
        return true;
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
        return hecke[zelleX(x)][zelleY(y)];
    }

    /** Hecke an einer Zelle entfernen (später für den Landwirt). */
    public void maeheHecke(int sx, int sy)
    {
        hecke[sx][sy] = false;
    }

    // ------------------------------------------------------------------
    // Tiere
    // ------------------------------------------------------------------

    /**
     * Legt ein neues Tier an und gibt seine ID zurück.
     * Hasen starten zufällig, Füchse an einem gut verteilten Bauplatz.
     */
    public int neuesTier(Art art)
    {
        float x, y;
        if (art == Art.FUCHS)
        {
            float[] platz = waehleBauPlatz();
            x = platz[0];
            y = platz[1];
        }
        else
        {
            x = zufall.nextFloat() * breite();
            y = zufall.nextFloat() * hoehe();
        }

        Tier t = new Tier(naechsteId++, art, x, y);

        if (art == Art.FUCHS)
        {
            // Ein Fuchs startet an seinem Bau
            t.bauX = x;
            t.bauY = y;
        }
        else
        {
            // Hasen starten unterschiedlich satt, damit nicht alle gleichzeitig hungrig werden
            t.energie = 50f + zufall.nextFloat() * 50f;
        }

        tiere.put(t.id, t);
        return t.id;
    }

    /**
     * Sucht einen Bauplatz, der möglichst weit von allen anderen Bauen entfernt ist
     * ("bester von 30 Kandidaten"). Dadurch verteilen sich die Baue über die Karte.
     * Baue liegen nicht im Busch und halten Abstand zum Rand.
     */
    private float[] waehleBauPlatz()
    {
        float[] bester = { breite() / 2, hoehe() / 2 };   // Notlösung: Kartenmitte
        float besterAbstand = -1;

        for (int i = 0; i < 30; i++)
        {
            float x = BAU_RAND + zufall.nextFloat() * (breite() - 2 * BAU_RAND);
            float y = BAU_RAND + zufall.nextFloat() * (hoehe() - 2 * BAU_RAND);
            if (istHecke(x, y))
                continue;

            // Abstand zum nächsten schon vorhandenen Bau
            float naechsterBau = Float.MAX_VALUE;
            for (Tier t : tiere.values())
                if (t.art == Art.FUCHS)
                    naechsterBau = Math.min(naechsterBau, abstand(x, y, t.bauX, t.bauY));

            if (naechsterBau > besterAbstand)
            {
                besterAbstand = naechsterBau;
                bester = new float[] { x, y };
            }
        }
        return bester;
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
        t.lauert = false;   // wer sich bewegt, lauert nicht mehr

        // Merken, ob schon beim Loslaufen etwas Wichtiges in Sicht war
        t.sahWichtigesBeimStart = siehtWichtiges(t);
    }

    /**
     * Ein Fuchs duckt sich und wartet einen Tick (Lauern).
     * Solange er lauert, sehen Hasen ihn nur aus nächster Nähe.
     * Jede Bewegung (setzeZiel) beendet das Lauern wieder.
     *
     * @return false, wenn das Tier kein lebender Fuchs ist
     */
    public boolean lauern(int id)
    {
        Tier t = tiere.get(id);
        if (t == null || t.art != Art.FUCHS)
            return false;
        setzeZiel(id, t.x, t.y);   // "Ziel = hier" -> im nächsten Tick erledigt
        t.lauert = true;           // erst NACH setzeZiel, weil setzeZiel das Lauern beendet
        return true;
    }

    /**
     * Ansprung aus der Lauer: wie setzeZiel, aber für kurze Zeit mit Sprint.
     * Regel der Welt: Ein Ansprung ist NUR aus der Lauer heraus möglich.
     *
     * @return false, wenn das Tier kein lauernder Fuchs ist
     */
    public boolean springeAn(int id, float x, float y)
    {
        Tier t = tiere.get(id);
        if (t == null || t.art != Art.FUCHS || !t.lauert)
            return false;
        setzeZiel(id, x, y);         // beendet das Lauern
        t.sprintTicks = SPRINT_TICKS;
        return true;
    }

    /**
     * Ein Hase frisst einen Tick lang Gras an seiner Position.
     * Das Gras in der Zelle nimmt ab (sichtbare Fraßspur), die Energie steigt.
     *
     * @return false, wenn kein lebender Hase, im Busch oder kein Gras da
     */
    public boolean fressen(int id)
    {
        Tier t = tiere.get(id);
        if (t == null || t.art != Art.HASE || istHecke(t.x, t.y))
            return false;

        int sx = zelleX(t.x), sy = zelleY(t.y);
        float menge = Math.min(BISS, vegetation[sx][sy]);
        if (menge <= 0)
            return false;

        vegetation[sx][sy] -= menge;
        t.energie = Math.min(100f, t.energie + menge * ENERGIE_PRO_GRAS);
        setzeZiel(id, t.x, t.y);   // Fressen dauert einen Tick
        return true;
    }

    // ------------------------------------------------------------------
    // Sehen
    // ------------------------------------------------------------------

    /**
     * Die eine Sichtregel für alles: Kann 'betrachter' das Tier 'o' sehen?
     * - nur im eigenen Sichtradius
     * - Füchse sehen keine Hasen, die im Busch sitzen
     * - Hasen sehen einen lauernden Fuchs nur aus nächster Nähe
     */
    private boolean kannSehen(Tier betrachter, Tier o)
    {
        if (o == betrachter)
            return false;
        float d = abstand(betrachter.x, betrachter.y, o.x, o.y);
        if (d > betrachter.art.sicht)
            return false;
        if (betrachter.art == Art.FUCHS && o.art == Art.HASE && istHecke(o.x, o.y))
            return false;
        if (betrachter.art == Art.HASE && o.art == Art.FUCHS && o.lauert && d > LAUER_SICHTWEITE)
            return false;
        return true;
    }

    /**
     * Sieht das Tier gerade etwas, worauf es sofort reagieren sollte?
     * Fuchs: einen Hasen (Beute). Hase: einen Fuchs (Gefahr).
     */
    private boolean siehtWichtiges(Tier t)
    {
        Art gesucht = t.art == Art.FUCHS ? Art.HASE : Art.FUCHS;
        for (Tier o : tiere.values())
            if (o.art == gesucht && kannSehen(t, o))
                return true;
        return false;
    }

    /**
     * Was kann dieses Tier gerade sehen bzw. spüren?
     * Andere Tiere, Büsche in Sichtweite, der nächste Busch,
     * dazu die eigene Energie und (für Hasen) die beste Grasstelle.
     */
    public Wahrnehmung wahrnehmung(int id)
    {
        Tier t = tiere.get(id);
        if (t == null)
            return Wahrnehmung.tot();

        // Andere Tiere in Sichtweite (gleiche Regel wie bei der Unterbrechung)
        List<Sichtung> gesehen = new ArrayList<>();
        for (Tier o : tiere.values())
        {
            if (!kannSehen(t, o))
                continue;
            gesehen.add(new Sichtung(o.id, o.art, o.x, o.y, abstand(t.x, t.y, o.x, o.y)));
        }

        // Alle Zellen in Sichtweite durchgehen: Büsche sammeln, nächsten Busch
        // und (für Hasen) die beste Grasstelle finden
        List<Punkt> buesche = new ArrayList<>();
        float heckeX = Float.NaN, heckeY = Float.NaN;
        float futterX = Float.NaN, futterY = Float.NaN;
        float naechsterBusch = Float.MAX_VALUE;
        float bestesFutter = -Float.MAX_VALUE;

        for (int sx = 0; sx < SPALTEN; sx++)
        {
            for (int sy = 0; sy < ZEILEN; sy++)
            {
                float mx = (sx + 0.5f) * ZELLE;   // Mitte der Zelle
                float my = (sy + 0.5f) * ZELLE;
                float d = abstand(t.x, t.y, mx, my);
                if (d > t.art.sicht)
                    continue;

                if (hecke[sx][sy])
                {
                    buesche.add(new Punkt(mx, my));
                    if (d < naechsterBusch)
                    {
                        naechsterBusch = d;
                        heckeX = mx;
                        heckeY = my;
                    }
                }
                else if (t.art == Art.HASE && vegetation[sx][sy] >= MIN_GRAS)
                {
                    // Viel Gras ist gut, weit weg ist schlecht
                    float punkte = vegetation[sx][sy] - 0.03f * d;
                    if (punkte > bestesFutter)
                    {
                        bestesFutter = punkte;
                        futterX = mx;
                        futterY = my;
                    }
                }
            }
        }

        boolean imBusch = istHecke(t.x, t.y);
        float grasHier = imBusch ? 0f : vegetation[zelleX(t.x)][zelleY(t.y)];

        return new Wahrnehmung(true, t.x, t.y, t.energie, imBusch, grasHier,
            heckeX, heckeY, futterX, futterY,
            List.copyOf(gesehen), List.copyOf(buesche));
    }

    // ------------------------------------------------------------------
    // Fangen
    // ------------------------------------------------------------------

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

    /** Ein Simulationsschritt. */
    public TickErgebnis tick()
    {
        tick++;

        // 1. Umwelt aktualisieren: Gras wächst langsam nach
        for (int sx = 0; sx < SPALTEN; sx++)
            for (int sy = 0; sy < ZEILEN; sy++)
                vegetation[sx][sy] = Math.min(1f, vegetation[sx][sy] + WACHSTUM);

        // 3a. Tiere ein Stück Richtung Ziel bewegen
        List<Integer> angekommen = new ArrayList<>();
        for (Tier t : tiere.values())
        {
            // Sprint läuft mit der Zeit ab – egal, ob das Tier gerade läuft
            boolean sprintet = t.sprintTicks > 0;
            if (sprintet)
                t.sprintTicks--;

            if (!t.unterwegs)
                continue;

            float tempo = t.art.tempo * (sprintet ? SPRINT_FAKTOR : 1f);
            float dx = t.zielX - t.x;
            float dy = t.zielY - t.y;
            float abstand = (float) Math.sqrt(dx * dx + dy * dy);

            if (abstand <= tempo)
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
                t.x += dx / abstand * tempo;
                t.y += dy / abstand * tempo;
            }
        }

        // 3b. Unterbrechen: Wer beim Loslaufen nichts Wichtiges sah,
        //     es jetzt aber sieht, bleibt stehen und soll neu entscheiden.
        List<Integer> unterbrochen = new ArrayList<>();
        for (Tier t : tiere.values())
        {
            if (t.unterwegs && !t.sahWichtigesBeimStart && siehtWichtiges(t))
            {
                t.unterwegs = false;
                unterbrochen.add(t.id);
            }
        }

        // 3c. Stoffwechsel: Hasen werden hungrig (Verhungern kommt später)
        for (Tier t : tiere.values())
            if (t.art == Art.HASE)
                t.energie = Math.max(0f, t.energie - HASE_HUNGER);

        return new TickErgebnis(angekommen, unterbrochen);
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
        List<Boolean> versteckt = new ArrayList<>();
        List<Float> fuechse = new ArrayList<>();
        List<Boolean> lauernd = new ArrayList<>();
        List<Float> baue = new ArrayList<>();
        for (Tier t : tiere.values())
        {
            if (t.art == Art.HASE)
            {
                hasen.add(t.x);
                hasen.add(t.y);
                versteckt.add(istHecke(t.x, t.y));
            }
            else
            {
                fuechse.add(t.x);
                fuechse.add(t.y);
                lauernd.add(t.lauert);
                baue.add(t.bauX);
                baue.add(t.bauY);
            }
        }

        return new Schnappschuss(tick, veg, hk,
            zuArray(hasen), zuBoolArray(versteckt),
            zuArray(fuechse), zuBoolArray(lauernd),
            zuArray(baue), gefressen);
    }

    private static float[] zuArray(List<Float> liste)
    {
        float[] a = new float[liste.size()];
        for (int i = 0; i < a.length; i++)
            a[i] = liste.get(i);
        return a;
    }

    private static boolean[] zuBoolArray(List<Boolean> liste)
    {
        boolean[] a = new boolean[liste.size()];
        for (int i = 0; i < a.length; i++)
            a[i] = liste.get(i);
        return a;
    }

    /** Spalte der Zelle, in der x (Meter) liegt. */
    private static int zelleX(float x)
    {
        return Math.min(SPALTEN - 1, Math.max(0, (int) (x / ZELLE)));
    }

    /** Zeile der Zelle, in der y (Meter) liegt. */
    private static int zelleY(float y)
    {
        return Math.min(ZEILEN - 1, Math.max(0, (int) (y / ZELLE)));
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