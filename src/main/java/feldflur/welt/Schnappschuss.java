package feldflur.welt;

/** Unveränderliche Kopie der Welt für die Grafik. */
public record Schnappschuss(
    long tick,
    float[][] vegetation,
    boolean[][] hecke,
    float[] hasenXY,
    boolean[] hasenVersteckt,   // pro Hase: sitzt er gerade im Busch?
    float[] fuchsXY,
    boolean[] fuchsLauert,      // pro Fuchs: liegt er auf der Lauer?
    float[] bauXY,              // Fuchsbaue, gleiche Reihenfolge wie fuchsXY
    int gefressen)
{
}