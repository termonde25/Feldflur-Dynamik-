package feldflur;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import feldflur.agenten.FuchsAgent;
import feldflur.agenten.HasenAgent;
import feldflur.gui.FeldflurAnzeige;
import feldflur.umwelt.SchnappschussKanal;
import feldflur.umwelt.UmweltAgent;
import jadex.core.IComponentManager;

public class Start
{
    public static final int START_HASEN = 20;
    public static final int START_FUECHSE = 3;

    public static void main(String[] args)
    {
        SchnappschussKanal kanal = new SchnappschussKanal();

        // Erst die Umwelt – und warten, bis sie wirklich läuft (.get())
        IComponentManager.get().create(new UmweltAgent(kanal)).get();

        for (int i = 0; i < START_HASEN; i++)
            IComponentManager.get().create(new HasenAgent());
        for (int i = 0; i < START_FUECHSE; i++)
            IComponentManager.get().create(new FuchsAgent());

        // libGDX läuft im main-Thread und blockiert, bis das Fenster geschlossen wird
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Feldflur-Dynamik");
        config.setWindowedMode(1000, 600);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new FeldflurAnzeige(kanal), config);

        System.exit(0); // Fenster zu -> alle Agenten beenden
    }
}