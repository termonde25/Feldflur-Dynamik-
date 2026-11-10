package feldflur;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import feldflur.agenten.HasenAgent;
import feldflur.gui.FeldflurAnzeige;
import feldflur.umwelt.SchnappschussKanal;
import feldflur.umwelt.UmweltAgent;
import jadex.core.IComponentManager;

public class Start
{
    public static void main(String[] args)
    {
        SchnappschussKanal kanal = new SchnappschussKanal();

        IComponentManager.get().create(new UmweltAgent(kanal)).get();
        for (int i = 0; i < 20; i++)
            IComponentManager.get().create(new HasenAgent());

        // libGDX läuft im main-Thread und blockiert, bis das Fenster geschlossen wird
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Feldflur-Dynamik");
        config.setWindowedMode(1000, 600);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new FeldflurAnzeige(kanal), config);

        System.exit(0); // Fenster zu -> alle Agenten beenden
    }
}