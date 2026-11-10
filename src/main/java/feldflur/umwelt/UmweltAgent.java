package feldflur.umwelt;

import java.util.HashMap;
import java.util.Map;

import feldflur.welt.Art;
import feldflur.welt.Wahrnehmung;
import feldflur.welt.Welt;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.OnStart;

/**
 * Die Umwelt als eigener Agent: besitzt die Welt, tickt alle 50 ms
 * und bietet den Tieren den Dienst IUmweltDienst an.
 */
public class UmweltAgent implements IUmweltDienst
{
    private final Welt welt = new Welt();
    private final SchnappschussKanal kanal;

    /** Tiere, die gerade unterwegs sind: ID -> Future, das bei Ankunft erfüllt wird */
    private final Map<Integer, Future<Boolean>> unterwegs = new HashMap<>();

    public UmweltAgent(SchnappschussKanal kanal)
    {
        this.kanal = kanal;
    }

    @OnStart
    protected void start(IComponent agent)
    {
        System.out.println("Umwelt gestartet: " + welt.breite() + " x " + welt.hoehe() + " m");
        IExecutionFeature exe = agent.getFeature(IExecutionFeature.class);

        // Tick-Schleife als kurzer, wiederholter Step (Muster wie SellerAgent, Blatt 5)
        exe.scheduleStep(new Runnable()
        {
            public void run()
            {
                // Welt einen Schritt weiterrechnen, angekommene Tiere benachrichtigen
                for (int id : welt.tick())
                {
                    Future<Boolean> f = unterwegs.remove(id);
                    if (f != null)
                        f.setResult(true);
                }

                // Kopie für die Grafik ablegen
                kanal.setze(welt.schnappschuss());

                exe.waitForDelay(50).get();  // 50 ms warten
                exe.scheduleStep(this);      // nächsten Tick einplanen
            }
        });
    }

    // ---------- Dienstmethoden ----------

    public IFuture<Integer> anmelden(Art art)
    {
        int id = welt.neuesTier(art);
        return new Future<>(id);
    }

    public IFuture<Wahrnehmung> wasIstInMeinerNaehe(int id)
    {
        return new Future<>(welt.wahrnehmung(id));
    }

    public IFuture<Boolean> bewegeZu(int id, float x, float y)
    {
        // Tote Tiere laufen nirgendwo mehr hin
        if (!welt.lebt(id))
            return new Future<>(false);

        Future<Boolean> alt = unterwegs.remove(id);
        if (alt != null)
            alt.setResult(false);            // altes Ziel wurde aufgegeben

        welt.setzeZiel(id, x, y);
        Future<Boolean> f = new Future<>();  // Ergebnis kommt später
        unterwegs.put(id, f);
        return f;
    }

    public IFuture<Boolean> fangen(int fuchsId, int haseId)
    {
        boolean erfolg = welt.fangen(fuchsId, haseId);
        if (erfolg)
        {
            // Der Hase wartet evtl. gerade auf seine Ankunft -> aufwecken mit "false"
            Future<Boolean> f = unterwegs.remove(haseId);
            if (f != null)
                f.setResult(false);
            System.out.println("Fuchs " + fuchsId + " hat Hase " + haseId + " gefangen.");
        }
        return new Future<>(erfolg);
    }
}