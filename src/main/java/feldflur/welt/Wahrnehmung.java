package feldflur.welt;

import java.util.List;

import jadex.core.annotation.NoCopy;

/**
 * Antwort der Umwelt auf "Was ist in meiner Nähe?".
 * Enthält nur, was das Tier selbst sehen bzw. spüren kann.
 *
 * @param energie          eigene Energie (0..100)
 * @param grasHier         Gras in der eigenen Zelle (0..1, im Busch 0)
 * @param heckeX, heckeY   Mitte der nächsten Heckenzelle in Sichtweite (NaN = keine)
 * @param futterX, futterY beste Grasstelle in Sichtweite (NaN = keine) – nur für Hasen
 * @param sichtungen       andere Tiere in Sichtweite
 * @param buesche          Mitten aller Heckenzellen in Sichtweite
 */
@NoCopy
public record Wahrnehmung(
    boolean lebendig,
    float x, float y,
    float energie,
    boolean inHecke,
    float grasHier,
    float heckeX, float heckeY,
    float futterX, float futterY,
    List<Sichtung> sichtungen,
    List<Punkt> buesche)
{
    /** Wahrnehmung für ein Tier, das nicht mehr lebt. */
    public static Wahrnehmung tot()
    {
        return new Wahrnehmung(false, 0, 0, 0, false, 0,
            Float.NaN, Float.NaN, Float.NaN, Float.NaN, List.of(), List.of());
    }

    /** Ist ein Busch in Sicht? */
    public boolean hatHecke()
    {
        return !Float.isNaN(heckeX);
    }

    /** Ist eine lohnende Grasstelle in Sicht? */
    public boolean hatFutter()
    {
        return !Float.isNaN(futterX);
    }

    /** Das nächste gesehene Tier einer Art, oder null. */
    public Sichtung naechstes(Art art)
    {
        Sichtung beste = null;
        for (Sichtung s : sichtungen)
            if (s.art() == art && (beste == null || s.abstand() < beste.abstand()))
                beste = s;
        return beste;
    }
}