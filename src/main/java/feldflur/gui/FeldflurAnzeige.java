package feldflur.gui;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import feldflur.umwelt.SchnappschussKanal;
import feldflur.welt.Schnappschuss;
import feldflur.welt.Welt;

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

        // Vegetation: braun (0) bis grün (1)
        for (int sx = 0; sx < Welt.SPALTEN; sx++)
        {
            for (int sy = 0; sy < Welt.ZEILEN; sy++)
            {
                float v = s.vegetation()[sx][sy];
                stift.setColor(0.55f - 0.35f * v, 0.45f + 0.15f * v, 0.25f - 0.1f * v, 1);
                stift.rect(sx * Welt.ZELLE, sy * Welt.ZELLE, Welt.ZELLE, Welt.ZELLE);
            }
        }

        // Hasen als braune Kreise
        stift.setColor(0.6f, 0.4f, 0.2f, 1);
        float[] xy = s.hasenXY();
        for (int i = 0; i < xy.length; i += 2)
            stift.circle(xy[i], xy[i + 1], 0.8f, 12);

        stift.end();
    }

    @Override
    public void dispose()
    {
        stift.dispose();
    }
}