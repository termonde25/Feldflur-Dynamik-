package feldflur;

import feldflur.agenten.HasenAgent;
import feldflur.umwelt.UmweltAgent;
import jadex.core.IComponentManager;

public class Start
{
    public static void main(String[] args)
    {
        // Erst die Umwelt – und warten, bis sie wirklich läuft (.get())
        IComponentManager.get().create(new UmweltAgent()).get();

        // Dann die Hasen
        for (int i = 0; i < 5; i++)
            IComponentManager.get().create(new HasenAgent());

        IComponentManager.get().waitForLastComponentTerminated();
    }
}