package feldflur.welt;

public enum Art
{
    HASE(0.5f),
    FUCHS(0.7f);

    /** Wie weit sich das Tier pro Tick bewegt (in Metern). */
    public final float tempo;

    Art(float tempo)
    {
        this.tempo = tempo;
    }
}