package blatt6.aufgabe3;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.Inject;

public class FuchsAgent
{
    private static final long TICK_MS = 200;          // wie oft der Fuchs wahrnimmt
    private static final double SICHT = 0.30;
    private static final double FANG_ABSTAND = 0.04;   // gleicher Wert wie im UmweltAgent
    private static final double STREIF_WEITE = 0.20;
    private static final long RUHE_NACH_FANG_MS = 2000;

    private enum Modus { STREIFEN, JAGEN }

    @Inject
    private IComponent agent;

    @Inject
    private IExecutionFeature exec;

    private final String name;
    private IUmweltDienst umwelt;

    private Modus modus = Modus.STREIFEN;
    private boolean unterwegs = false;   // läuft gerade ein Lauf?
    private int laufNummer = 0;          // Nummer des aktuellsten Laufs
    private int beute = 0;

    public FuchsAgent(String name)
    {
        this.name = name;
    }

    @Inject
    protected void umweltGefunden(IUmweltDienst umwelt)
    {
        this.umwelt = umwelt;
        exec.scheduleStep(new Runnable()
        {
            boolean angemeldet = false;

            public void run()
            {
                try
                {
                    if (!angemeldet)
                    {
                        umwelt.anmelden(name, TierArt.FUCHS).get();
                        angemeldet = true;
                    }
                    tick();
                    exec.waitForDelay(TICK_MS).get();   // kurze Pause, dann nächster Tick
                    exec.scheduleStep(this);
                }
                catch (RuntimeException e)
                {
                    // z. B. "Simulation beendet" aus wasIstInMeinerNaehe()
                    System.out.println("[" + name + "] Ende (" + beute + " Hasen gefangen): " + e.getMessage());
                    agent.terminate();
                }
            }
        });
    }

    /** Ein Tick: wahrnehmen -> entscheiden -> (Lauf starten oder ändern). Blockiert NICHT bis zur Ankunft. */
    private void tick()
    {
        Wahrnehmung w = umwelt.wasIstInMeinerNaehe(name, SICHT).get();
        Sichtung hase = w.naechste(TierArt.HASE);

        if (hase != null)
        {
            if (modus == Modus.STREIFEN)
                System.out.println("[" + name + "] Hase " + hase.getName() + " gesichtet – breche Streifen ab, jage!");
            modus = Modus.JAGEN;

            // Nah genug? Dann zuschnappen.
            if (hase.getAbstand() <= FANG_ABSTAND)
            {
                boolean gefangen = umwelt.fangen(name, hase.getName()).get();
                if (gefangen)
                {
                    beute++;
                    System.out.println("[" + name + "] Lecker! (" + beute + ")");
                    starteLauf(w.getX(), w.getY());          // stehen bleiben (alter Lauf wird ersetzt)
                    modus = Modus.STREIFEN;
                    exec.waitForDelay(RUHE_NACH_FANG_MS).get(); // satt, kurz ausruhen
                    return;
                }
            }

            // Ziel in JEDEM Tick auf die aktuelle Hasenposition setzen – der Hase bewegt sich ja.
            starteLauf(hase.getX(), hase.getY());
            return;
        }

        // Kein Hase in Sicht
        if (modus == Modus.JAGEN)
        {
            System.out.println("[" + name + "] Hase aus den Augen verloren.");
            modus = Modus.STREIFEN;
            unterwegs = false;   // sofort neues Streifziel wählen statt zur letzten Hasenposition zu laufen
        }

        if (!unterwegs)
        {
            double zielX = w.getX() + (Math.random() - 0.5) * STREIF_WEITE;
            double zielY = w.getY() + (Math.random() - 0.5) * STREIF_WEITE;
            starteLauf(zielX, zielY);
        }
    }

    /**
     * Startet einen Lauf, OHNE auf die Ankunft zu warten.
     * Ein neuer Aufruf ersetzt den alten Lauf in der Umwelt ("Neues Ziel").
     */
    private void starteLauf(double x, double y)
    {
        unterwegs = true;
        final int meinLauf = ++laufNummer;

        umwelt.bewegeZu(name, x, y).then(angekommen ->
        {
            if (meinLauf == laufNummer)      // nur der aktuellste Lauf zählt
                unterwegs = false;
        }).catchEx(fehler ->
        {
            // "Neues Ziel" = wir haben den Lauf selbst ersetzt -> völlig normal, ignorieren.
            // Andere Fehler (z. B. "Simulation beendet") merkt der nächste Tick über wasIstInMeinerNaehe().
            if (meinLauf == laufNummer)
                unterwegs = false;
        });
    }
}