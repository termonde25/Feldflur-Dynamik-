package feldflur.welt;

/**
 * Der Körper eines Tieres in der Welt.
 * Gehört der Welt, nicht dem Agenten: Der Agent entscheidet nur,
 * die Welt rechnet aus, was passiert.
 */
class Tier
{
    final int id;
    final Art art;
    float x, y;
    float zielX, zielY;
    boolean unterwegs = false;

    /**
     * Hat das Tier beim Loslaufen schon etwas Wichtiges gesehen
     * (Fuchs: einen Hasen, Hase: einen Fuchs)? Wenn nein, wird die
     * Bewegung unterbrochen, sobald etwas Wichtiges auftaucht.
     */
    boolean sahWichtigesBeimStart = false;

    /** Lage des Baus (nur bei Füchsen gesetzt, sonst NaN). */
    float bauX = Float.NaN, bauY = Float.NaN;

    /** Fuchs liegt geduckt auf der Lauer (Hasen sehen ihn dann schlechter). */
    boolean lauert = false;

    /** Wie viele Ticks der Sprint noch anhält (0 = normales Tempo). */
    int sprintTicks = 0;

    /** Energie 0..100 (vorerst nur bei Hasen genutzt). */
    float energie = 100f;

    Tier(int id, Art art, float x, float y)
    {
        this.id = id;
        this.art = art;
        this.x = x;
        this.y = y;
    }
}