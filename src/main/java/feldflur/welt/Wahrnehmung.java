package feldflur.welt;

import java.util.List;

import jadex.core.annotation.NoCopy;

/**
 * Antwort der Umwelt auf "Was ist in meiner Nähe?".
 * Enthält nur, was das Tier selbst sehen kann.
 *
 * @param heckeX, heckeY  Mitte der nächsten Heckenzelle in Sichtweite (NaN = keine in Sicht)
 */
@NoCopy
public record Wahrnehmung(
    boolean lebendig,
    float x, float y,
    boolean inHecke,
    float heckeX, float heckeY,
    List<Sichtung> sichtungen)
{
    /** Wahrnehmung für ein Tier, das nicht mehr lebt. */
    public static Wahrnehmung tot()
    {
        return new Wahrnehmung(false, 0, 0, false, Float.NaN, Float.NaN, List.of());
    }

    /** Ist ein Busch in Sicht? */
    public boolean hatHecke()
    {
        return !Float.isNaN(heckeX);
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