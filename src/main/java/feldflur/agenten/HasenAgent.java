package feldflur.agenten;

import java.util.Random;

import feldflur.umwelt.IUmweltDienst;
import feldflur.welt.Art;
import feldflur.welt.Sichtung;
import feldflur.welt.Wahrnehmung;
import feldflur.welt.Welt;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 * Hase mit Subsumption-Architektur (PP4).
 *
 * Die Schichten werden von oben nach unten geprüft. Die erste, die zuständig
 * ist, handelt – alle darunter werden unterdrückt:
 *
 *   L0 Fliehen   – Fuchs in Sicht: in den Busch, dort bleiben, oder weglaufen
 *   L1 Fressen   – hungrig: Gras fressen bzw. zur besten Grasstelle laufen
 *   L2 Streifen  – sonst: ein Stück umherlaufen (später: Partner suchen)
 *
 * Hunger gegen Angst: Ein hungriger Hase verlässt den Busch zum Fressen –
 * aber nur, solange kein Fuchs in Sicht ist (L0 steht über L1).
 */
public class HasenAgent
{
    private static final float STREIF_SCHRITT = 3f;  // kurz, damit er oft neu hinschaut
    private static final float FLUCHT_SCHRITT = 4f;

    /** Unter dieser Energie fängt er an zu fressen ... */
    private static final float HUNGRIG = 60f;
    /** ... und frisst dann am Stück, bis er so satt ist (Hysterese). */
    private static final float SATT = 95f;

    private final Random zufall = new Random();
    private IUmweltDienst umwelt;
    private int id;

    /** Frisst er gerade? (damit er nicht nach einem Bissen wieder aufhört) */
    private boolean amFressen = false;

    @OnStart
    protected void start(IComponent agent)
    {
        umwelt = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IUmweltDienst.class);
        id = umwelt.anmelden(Art.HASE).get();

        IExecutionFeature exe = agent.getFeature(IExecutionFeature.class);
        exe.scheduleStep(new Runnable()
        {
            public void run()
            {
                // 1. Wahrnehmen
                Wahrnehmung w = umwelt.wasIstInMeinerNaehe(id).get();
                if (!w.lebendig())
                {
                    agent.terminate();   // gefressen -> Agent beendet sich
                    return;
                }

                // 2.+3. Schichten von oben nach unten: die erste zuständige handelt
                if (!schichtFliehen(w))
                    if (!schichtFressen(w))
                        schichtStreifen(w);

                exe.scheduleStep(this);
            }
        });
    }

    /** L0: Überleben geht vor. @return true, wenn diese Schicht gehandelt hat */
    private boolean schichtFliehen(Wahrnehmung w)
    {
        Sichtung fuchs = w.naechstes(Art.FUCHS);
        if (fuchs == null)
            return false;   // nicht zuständig -> nächste Schicht

        amFressen = false;  // Flucht unterbricht das Fressen

        if (w.inHecke())
        {
            umwelt.bewegeZu(id, w.x(), w.y()).get();            // im Busch: still sitzen bleiben
        }
        else if (w.hatHecke())
        {
            umwelt.bewegeZu(id, w.heckeX(), w.heckeY()).get();  // zum nächsten Busch rennen
        }
        else
        {
            // kein Busch in Sicht: vom Fuchs weg
            float dx = w.x() - fuchs.x();
            float dy = w.y() - fuchs.y();
            float d = Math.max(0.01f, (float) Math.sqrt(dx * dx + dy * dy));
            umwelt.bewegeZu(id, w.x() + dx / d * FLUCHT_SCHRITT, w.y() + dy / d * FLUCHT_SCHRITT).get();
        }
        return true;
    }

    /** L1: Hunger stillen. @return true, wenn diese Schicht gehandelt hat */
    private boolean schichtFressen(Wahrnehmung w)
    {
        boolean hungrig = w.energie() < HUNGRIG;
        boolean nochNichtSatt = amFressen && w.energie() < SATT;
        if (!hungrig && !nochNichtSatt)
        {
            amFressen = false;
            return false;   // nicht zuständig -> nächste Schicht
        }
        amFressen = true;

        // Genug Gras unter mir (und nicht im Busch)? -> fressen
        if (!w.inHecke() && w.grasHier() >= Welt.MIN_GRAS)
        {
            umwelt.fressen(id).get();
            return true;
        }

        // Sonst zur besten Grasstelle in Sicht laufen
        if (w.hatFutter())
        {
            umwelt.bewegeZu(id, w.futterX(), w.futterY()).get();
            return true;
        }

        // Kein Futter in Sicht -> L2 soll herumlaufen, um welches zu finden
        return false;
    }

    /** L2: Wenn sonst nichts zu tun ist, ein kurzes Stück herumlaufen. */
    private void schichtStreifen(Wahrnehmung w)
    {
        double winkel = zufall.nextDouble() * 2 * Math.PI;
        umwelt.bewegeZu(id,
            w.x() + (float) Math.cos(winkel) * STREIF_SCHRITT,
            w.y() + (float) Math.sin(winkel) * STREIF_SCHRITT).get();
    }
}