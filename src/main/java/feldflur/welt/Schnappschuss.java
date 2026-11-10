package feldflur.welt;

/** Unveränderliche Kopie der Welt für die Grafik. */
public record Schnappschuss(
    long tick,
    float[][] vegetation,
    boolean[][] hecke,
    float[] hasenXY,
    boolean[] hasenVersteckt,   // pro Hase: sitzt er gerade im Busch?
    float[] fuchsXY,
    int gefressen)
{
}