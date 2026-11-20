package feldflur.gui;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import feldflur.umwelt.SchnappschussKanal;
import feldflur.welt.Schnappschuss;
import feldflur.welt.Welt;

/**
 * libGDX-Fenster. Zeichnet nur den aktuellen Schnappschuss,
 * greift nie direkt auf Welt oder Agenten zu.
 *
 * Aufteilung des Fensters:
 *   oben  – das Feld (100 x 60 m)
 *   unten – schmale Statusleiste mit den Zahlen
 */
public class FeldflurAnzeige extends ApplicationAdapter
{
    /** Höhe der Statusleiste in Pixeln (Start.java macht das Fenster entsprechend höher). */
    public static final int LEISTE_HOEHE = 30;

    /** Dauer eines Ticks in Millisekunden (wie im UmweltAgent) – für die Zeitanzeige. */
    private static final int TICK_MS = 50;

    private final SchnappschussKanal kanal;

    private OrthographicCamera kamera;        // Feld: rechnet in Metern
    private OrthographicCamera leistenKamera; // Statusleiste: rechnet in Pixeln
    private ShapeRenderer stift;
    private SpriteBatch textStift;
    private BitmapFont schrift;

    public FeldflurAnzeige(SchnappschussKanal kanal)
    {
        this.kanal = kanal;
    }

    @Override
    public void create()
    {
        // Feld: 100 x 60 m
        kamera = new OrthographicCamera();
        kamera.setToOrtho(false, Welt.SPALTEN * Welt.ZELLE, Welt.ZEILEN * Welt.ZELLE);

        // Statusleiste: volle Fensterbreite x LEISTE_HOEHE Pixel
        leistenKamera = new OrthographicCamera();
        leistenKamera.setToOrtho(false, Gdx.graphics.getWidth(), LEISTE_HOEHE);

        stift = new ShapeRenderer();
        textStift = new SpriteBatch();
        schrift = new BitmapFont();                    // eingebaute Standardschrift
        schrift.setColor(0.93f, 0.89f, 0.75f, 1);      // helles Sandbeige
    }

    @Override
    public void resize(int breite, int hoehe)
    {
        leistenKamera.setToOrtho(false, breite, LEISTE_HOEHE);
    }

    @Override
    public void render()
    {
        // Hintergrund = Farbe der Leiste (dunkles Olivgrün, passend zu den Hecken)
        ScreenUtils.clear(0.16f, 0.20f, 0.12f, 1);

        Schnappschuss s = kanal.hole();
        if (s == null)
            return; // Umwelt hat noch keinen Tick gemacht

        int breite = Gdx.graphics.getWidth();
        int hoehe = Gdx.graphics.getHeight();

        // Oberer Bereich: das Feld
        HdpiUtils.glViewport(0, LEISTE_HOEHE, breite, hoehe - LEISTE_HOEHE);
        zeichneFeld(s);

        // Unterer Bereich: die Statusleiste
        HdpiUtils.glViewport(0, 0, breite, LEISTE_HOEHE);
        zeichneLeiste(s);
    }

    /** Raster, Baue, Hasen und Füchse – in Metern. */
    private void zeichneFeld(Schnappschuss s)
    {
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

        // Fuchsbaue: dunkelbraune Quadrate
        stift.setColor(0.3f, 0.15f, 0.05f, 1);
        float[] b = s.bauXY();
        for (int i = 0; i < b.length; i += 2)
            stift.rect(b[i] - 0.9f, b[i + 1] - 0.9f, 1.8f, 1.8f);

        // Hasen: braun auf dem Feld, hell sandfarben wenn im Busch versteckt
        float[] h = s.hasenXY();
        boolean[] versteckt = s.hasenVersteckt();
        for (int i = 0; i < versteckt.length; i++)
        {
            if (versteckt[i])
                stift.setColor(0.95f, 0.85f, 0.6f, 1);   // versteckt
            else
                stift.setColor(0.55f, 0.35f, 0.15f, 1);  // offen sichtbar
            stift.circle(h[i * 2], h[i * 2 + 1], 0.8f, 12);
        }

        // Füchse: rot; auf der Lauer dunkelrot und kleiner (geduckt)
        float[] f = s.fuchsXY();
        boolean[] lauert = s.fuchsLauert();
        for (int i = 0; i < lauert.length; i++)
        {
            if (lauert[i])
            {
                stift.setColor(0.45f, 0.05f, 0.05f, 1);
                stift.circle(f[i * 2], f[i * 2 + 1], 0.9f, 16);
            }
            else
            {
                stift.setColor(0.85f, 0.1f, 0.1f, 1);
                stift.circle(f[i * 2], f[i * 2 + 1], 1.2f, 16);
            }
        }

        stift.end();
    }

    /** Schmale Statusleiste unter dem Feld – in Pixeln. */
    private void zeichneLeiste(Schnappschuss s)
    {
        long sekunden = s.tick() * TICK_MS / 1000;
        String text = String.format("Hasen %d      Fuechse %d      Gefressen %d      Zeit %02d:%02d",
            s.hasenXY().length / 2,
            s.fuchsXY().length / 2,
            s.gefressen(),
            sekunden / 60, sekunden % 60);

        leistenKamera.update();
        textStift.setProjectionMatrix(leistenKamera.combined);
        textStift.begin();
        // y = Oberkante des Textes -> so wird er senkrecht mittig
        schrift.draw(textStift, text, 12, LEISTE_HOEHE / 2f + schrift.getCapHeight() / 2f);
        textStift.end();
    }

    @Override
    public void dispose()
    {
        stift.dispose();
        textStift.dispose();
        schrift.dispose();
    }
}