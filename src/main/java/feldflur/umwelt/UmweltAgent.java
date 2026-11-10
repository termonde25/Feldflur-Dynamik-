package feldflur.umwelt;

import java.util.HashMap;
import java.util.Map;

import feldflur.welt.Art;
import feldflur.welt.Welt;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.OnStart;

public class UmweltAgent implements IUmweltDienst
{
    private final Welt welt = new Welt();

    /** Tiere, die gerade unterwegs sind: ID -> Future, das bei Ankunft erfüllt wird */
    private final Map<Integer, Future<Boolean>> unterwegs = new HashMap<>();

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
                for (int id : welt.tick())
                {
                    Future<Boolean> f = unterwegs.remove(id);
                    if (f != null)
                        f.setResult(true);   // Hase ist angekommen -> sein .get() kehrt zurück
                }
                exe.waitForDelay(50).get();  // 50 ms warten
                exe.scheduleStep(this);      // nächsten Tick einplanen
            }
        });
    }

    // ---------- Dienstmethoden ----------

    public IFuture<Integer> anmelden(Art art)
    {
        int id = welt.neuesTier(art);
        return new Future<>(id);             // Ergebnis ist sofort da
    }

    public IFuture<Boolean> bewegeZu(int id, float x, float y)
    {
        Future<Boolean> alt = unterwegs.remove(id);
        if (alt != null)
            alt.setResult(false);            // altes Ziel wurde aufgegeben

        welt.setzeZiel(id, x, y);
        Future<Boolean> f = new Future<>();  // Ergebnis kommt später
        unterwegs.put(id, f);
        return f;
    }
}