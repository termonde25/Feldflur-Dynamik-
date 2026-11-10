package feldflur.agenten;

import java.util.Random;

import feldflur.umwelt.IUmweltDienst;
import feldflur.welt.Art;
import feldflur.welt.Sichtung;
import feldflur.welt.Wahrnehmung;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 * Hase mit Subsumption-Architektur (PP4).
 *
 * Die Schichten werden von oben nach unten geprüft. Die erste, die etwas
 * tun will, gewinnt und unterdrückt alle darunter:
 *
 *   L0 Fliehen   – Fuchs in Sicht: in den Busch, dort bleiben, oder weglaufen
 *   L1 Fressen   – (Schritt F)
 *   L2 Streifen  – sonst: ein Stück umherlaufen (später: Partner suchen)
 */
public class HasenAgent
{
    private static final float STREIF_SCHRITT = 3f;  // kurz, damit er oft neu hinschaut
    private static final float FLUCHT_SCHRITT = 4f;

    private final Random zufall = new Random();
    private IUmweltDienst umwelt;
    private int id;

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

                // 2. Entscheiden: Schichten von oben nach unten
                float[] ziel = schichtFliehen(w);
                if (ziel == null)
                    ziel = schichtStreifen(w);

                // 3. Handeln und warten, bis erledigt
                umwelt.bewegeZu(id, ziel[0], ziel[1]).get();

                exe.scheduleStep(this);
            }
        });
    }

    /** L0: Überleben geht vor. Liefert null, wenn kein Fuchs in Sicht ist. */
    private float[] schichtFliehen(Wahrnehmung w)
    {
        Sichtung fuchs = w.naechstes(Art.FUCHS);
        if (fuchs == null)
            return null;   // Schicht nicht zuständig -> nächste Schicht

        if (w.inHecke())
            return new float[] { w.x(), w.y() };            // im Busch: still sitzen bleiben

        if (w.hatHecke())
            return new float[] { w.heckeX(), w.heckeY() };  // zum nächsten Busch rennen

        // kein Busch in Sicht: vom Fuchs weg
        float dx = w.x() - fuchs.x();
        float dy = w.y() - fuchs.y();
        float d = Math.max(0.01f, (float) Math.sqrt(dx * dx + dy * dy));
        return new float[] { w.x() + dx / d * FLUCHT_SCHRITT, w.y() + dy / d * FLUCHT_SCHRITT };
    }

    /** L2: Wenn sonst nichts zu tun ist, ein kurzes Stück herumlaufen. */
    private float[] schichtStreifen(Wahrnehmung w)
    {
        double winkel = zufall.nextDouble() * 2 * Math.PI;
        return new float[] {
            w.x() + (float) Math.cos(winkel) * STREIF_SCHRITT,
            w.y() + (float) Math.sin(winkel) * STREIF_SCHRITT };
    }
}