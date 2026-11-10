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
 * Fuchs: Hase in Sicht -> verfolgen und fangen, sonst Revier absuchen.
 * Später (Phase 3) wird daraus ein BDI-Agent mit Ziel "SattWerden".
 */
public class FuchsAgent
{
    private static final float JAGD_SCHRITT = 2f;  // kurze Schritte -> oft neu hinschauen
    private static final float SUCH_WEITE = 12f;

    private final Random zufall = new Random();
    private IUmweltDienst umwelt;
    private int id;

    @OnStart
    protected void start(IComponent agent)
    {
        umwelt = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IUmweltDienst.class);
        id = umwelt.anmelden(Art.FUCHS).get();
        System.out.println("Fuchs " + id + " ist da.");

        IExecutionFeature exe = agent.getFeature(IExecutionFeature.class);
        exe.scheduleStep(new Runnable()
        {
            public void run()
            {
                // 1. Wahrnehmen
                Wahrnehmung w = umwelt.wasIstInMeinerNaehe(id).get();
                Sichtung hase = w.naechstes(Art.HASE);

                // 2. Entscheiden + 3. Handeln
                if (hase != null && hase.abstand() <= Welt.FANG_RADIUS)
                {
                    // nah genug: zuschlagen
                    umwelt.fangen(id, hase.id()).get();
                }
                else if (hase != null)
                {
                    // Hase gesehen: ein kurzes Stück hinterher
                    float schritt = Math.min(JAGD_SCHRITT, hase.abstand());
                    float x = w.x() + (hase.x() - w.x()) / hase.abstand() * schritt;
                    float y = w.y() + (hase.y() - w.y()) / hase.abstand() * schritt;
                    umwelt.bewegeZu(id, x, y).get();
                }
                else
                {
                    // nichts zu sehen: Revier absuchen
                    float x = w.x() + (zufall.nextFloat() - 0.5f) * 2 * SUCH_WEITE;
                    float y = w.y() + (zufall.nextFloat() - 0.5f) * 2 * SUCH_WEITE;
                    umwelt.bewegeZu(id, x, y).get();
                }

                exe.scheduleStep(this);
            }
        });
    }
}