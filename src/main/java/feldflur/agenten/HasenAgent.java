package feldflur.agenten;

import java.util.Random;

import feldflur.umwelt.IUmweltDienst;
import feldflur.welt.Art;
import feldflur.welt.Welt;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;
import jadex.requiredservice.IRequiredServiceFeature;

public class HasenAgent
{
    private final Random zufall = new Random();
    private IUmweltDienst umwelt;
    private int id;

    @OnStart
    protected void start(IComponent agent)
    {
        // Dienst der Umwelt suchen (wie in Blatt 4)
        umwelt = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IUmweltDienst.class);

        // Körper in der Welt anlegen lassen
        id = umwelt.anmelden(Art.HASE).get();
        System.out.println("Hase " + id + " ist da (Agent " + agent.getId() + ")");

        IExecutionFeature exe = agent.getFeature(IExecutionFeature.class);
        exe.scheduleStep(new Runnable()
        {
            public void run()
            {
                // Entscheiden: irgendwohin laufen (später: fliehen, fressen, Partner suchen)
                float x = zufall.nextFloat() * Welt.SPALTEN * Welt.ZELLE;
                float y = zufall.nextFloat() * Welt.ZEILEN * Welt.ZELLE;

                // Handeln: Auftrag an die Umwelt und warten, bis er erledigt ist
                umwelt.bewegeZu(id, x, y).get();
                System.out.printf("Hase %d ist bei (%.0f | %.0f) angekommen%n", id, x, y);

                exe.scheduleStep(this);      // nächste Entscheidung
            }
        });
    }
}