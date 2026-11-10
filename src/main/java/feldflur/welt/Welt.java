package feldflur.welt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Das Weltmodell: Vegetation auf einem Raster und die Körper aller Tiere.
 * Reines Java – kein Jadex, keine Grafik.
 */
public class Welt
{
    public static final int SPALTEN = 50;
    public static final int ZEILEN = 30;
    public static final float ZELLE = 2f; // eine Zelle ist 2 x 2 Meter

    private final float[][] vegetation = new float[SPALTEN][ZEILEN];
    private final Map<Integer, Tier> tiere = new LinkedHashMap<>();
    private final Random zufall = new Random(42);
    private int naechsteId = 1;
    private long tick = 0;

    public Welt()
    {
        // Am Anfang ist überall etwas Gras (zwischen 0.5 und 1.0)
        for (int sx = 0; sx < SPALTEN; sx++)
            for (int sy = 0; sy < ZEILEN; sy++)
                vegetation[sx][sy] = 0.5f + zufall.nextFloat() * 0.5f;
    }

    /** Legt ein neues Tier an einer zufälligen Stelle an und gibt seine ID zurück. */
    public int neuesTier(Art art)
    {
        float x = zufall.nextFloat() * breite();
        float y = zufall.nextFloat() * hoehe();
        Tier t = new Tier(naechsteId++, art, x, y);
        tiere.put(t.id, t);
        return t.id;
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

    public float breite() { return SPALTEN * ZELLE; }
    public float hoehe()  { return ZEILEN * ZELLE; }
    public long getTick() { return tick; }
    public float getX(int id) { return tiere.get(id).x; }
    public float getY(int id) { return tiere.get(id).y; }
}