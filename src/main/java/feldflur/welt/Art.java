package feldflur.welt;

public enum Art
{
    //    tempo  sicht
    HASE (0.4f, 12f),   // sieht fast rundum, bemerkt den Fuchs zuerst
    FUCHS(0.5f,  9f);   // etwas schneller, sieht aber weniger weit

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