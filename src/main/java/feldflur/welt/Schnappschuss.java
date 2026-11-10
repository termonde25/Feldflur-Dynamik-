package feldflur.welt;

/** Unveränderliche Kopie der Welt für die Grafik. */
public record Schnappschuss(
    long tick,
    float[][] vegetation,
    boolean[][] hecke,
    float[] hasenXY,
    float[] fuchsXY,
    int gefressen)
{
}