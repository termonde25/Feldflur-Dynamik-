package feldflur.gui;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import feldflur.umwelt.SchnappschussKanal;
import feldflur.welt.Schnappschuss;
import feldflur.welt.Welt;

/**
 * libGDX-Fenster. Zeichnet nur den aktuellen Schnappschuss,
 * greift nie direkt auf Welt oder Agenten zu.
 */
public class FeldflurAnzeige extends ApplicationAdapter
{
    private final SchnappschussKanal kanal;
    private OrthographicCamera kamera;
    private ShapeRenderer stift;

    public FeldflurAnzeige(SchnappschussKanal kanal)
    {
        this.kanal = kanal;
    }

    @Override
    public void create()
    {
        // Kamera rechnet in Metern statt Pixeln: 100 x 60 m passen ins Fenster
        kamera = new OrthographicCamera();
        kamera.setToOrtho(false, Welt.SPALTEN * Welt.ZELLE, Welt.ZEILEN * Welt.ZELLE);
        stift = new ShapeRenderer();
    }

    @Override
    public void render()
    {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);
        Schnappschuss s = kanal.hole();
        if (s == null)
            return; // Umwelt hat noch keinen Tick gemacht

        kamera.update();
        stift.setProjectionMatrix(kamera.combined);
        stift.begin(ShapeRenderer.ShapeType.Filled);

        // Raster: Hecke dunkelgrün, sonst Vegetation von braun (0) bis grün (1)
        for (int sx = 0; sx < Welt.SPALTEN; sx++)
        {
            for (int sy = 0; sy < Welt.ZEILEN; sy++)
            {
                if (s.hecke()[sx][sy])
                {
                    stift.setColor(0.05f, 0.25f, 0.08f, 1);
                }
                else
                {
                    float v = s.vegetation()[sx][sy];
                    stift.setColor(0.55f - 0.35f * v, 0.45f + 0.15f * v, 0.25f - 0.1f * v, 1);
                }
                stift.rect(sx * Welt.ZELLE, sy * Welt.ZELLE, Welt.ZELLE, Welt.ZELLE);
            }
        }

        // Hasen: braune Kreise
        stift.setColor(0.6f, 0.4f, 0.2f, 1);
        float[] h = s.hasenXY();
        for (int i = 0; i < h.length; i += 2)
            stift.circle(h[i], h[i + 1], 0.8f, 12);

        // Füchse: orange-rote, etwas größere Kreise
        stift.setColor(0.9f, 0.35f, 0.1f, 1);
        float[] f = s.fuchsXY();
        for (int i = 0; i < f.length; i += 2)
            stift.circle(f[i], f[i + 1], 1.1f, 12);

        stift.end();
    }

    @Override
    public void dispose()
    {
        stift.dispose();
    }
}