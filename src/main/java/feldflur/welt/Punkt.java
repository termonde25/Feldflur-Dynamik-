package feldflur.welt;

import jadex.core.annotation.NoCopy;

/** Ein Ort in der Welt (in Metern). */
@NoCopy
public record Punkt(float x, float y)
{
    public float abstandZu(float ox, float oy)
    {
        float dx = ox - x, dy = oy - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}