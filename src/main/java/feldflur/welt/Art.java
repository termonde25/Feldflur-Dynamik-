package feldflur.welt;

public enum Art
{
    //    tempo  sicht
    HASE (0.5f,  8f),
    FUCHS(0.7f, 10f);

    /** Wie weit sich das Tier pro Tick bewegt (in Metern). */
    public final float tempo;

    /** Wie weit das Tier sehen kann (in Metern). */
    public final float sicht;

    Art(float tempo, float sicht)
    {
        this.tempo = tempo;
        this.sicht = sicht;
    }
}