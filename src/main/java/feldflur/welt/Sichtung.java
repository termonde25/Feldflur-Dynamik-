package feldflur.welt;

import jadex.core.annotation.NoCopy;

/** Ein anderes Tier, das ein Agent gerade sehen kann. */
@NoCopy
public record Sichtung(int id, Art art, float x, float y, float abstand)
{
}