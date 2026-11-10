package feldflur.umwelt;

import feldflur.welt.Schnappschuss;

/** Übergabestelle: Umweltagent schreibt, libGDX liest. */
public class SchnappschussKanal
{
    private volatile Schnappschuss aktuell;

    public void setze(Schnappschuss s) { aktuell = s; }
    public Schnappschuss hole()        { return aktuell; }
}