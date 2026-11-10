package feldflur.agenten;

import java.util.Random;

import feldflur.umwelt.IUmweltDienst;
import feldflur.welt.Art;
import feldflur.welt.Wahrnehmung;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 * Hase: läuft (noch) zufällig umher.
 * Schritt C: Fuchs sehen -> in den nächsten Busch flüchten.
 */
public class HasenAgent
{
    private static final float STREIF_WEITE = 10f; // so weit läuft er höchstens auf einmal

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

                // 2. Entscheiden: irgendwohin in der Nähe
                float x = w.x() + (zufall.nextFloat() - 0.5f) * 2 * STREIF_WEITE;
                float y = w.y() + (zufall.nextFloat() - 0.5f) * 2 * STREIF_WEITE;

                // 3. Handeln und warten, bis erledigt
                umwelt.bewegeZu(id, x, y).get();

                exe.scheduleStep(this);
            }
        });
    }
}