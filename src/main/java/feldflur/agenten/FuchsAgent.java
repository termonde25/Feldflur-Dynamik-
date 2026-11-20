package feldflur.agenten;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import feldflur.umwelt.IUmweltDienst;
import feldflur.welt.Art;
import feldflur.welt.Punkt;
import feldflur.welt.Sichtung;
import feldflur.welt.Wahrnehmung;
import feldflur.welt.Welt;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 * Fuchs mit Bau, Revier, Gedächtnis und zwei Jagdstrategien.
 *
 * Wissen (Beliefs):
 *   - wo sein Bau ist
 *   - welche Büsche es in seinem Revier gibt (lernt er beim Herumlaufen)
 *   - wo er zuletzt einen Hasen gesehen hat
 *   - in welchem Busch sich gerade ein Hase versteckt (falls er das gesehen hat)
 *
 * Verhalten:
 *   SUCHEN  – kein Hase in Sicht: Revier in Etappen absuchen
 *   JAGEN   – Hase in Sicht: Fluchtweg abschneiden, dann zuschlagen
 *   LAUERN  – (nur manchmal) Hase ist in einen bekannten Busch geflüchtet:
 *             mit Abstand vor dem Busch ducken und warten. Kommt der Hase raus,
 *             anspringen, sobald der Fuchs ihn erwischt ODER ihm den Weg zur
 *             Deckung abschneiden kann. Ohne Hasen in Sicht nach 7 s aufgeben,
 *             mit Hasen in Sicht bis zu 15 s warten. Danach eine Lauer-Pause.
 *
 * Später (Phase 3) wird daraus ein BDI-Agent mit Ziel "SattWerden".
 */
public class FuchsAgent
{
    /** Schalter zum Vergleichen der Strategien. */
    public static final boolean STRATEGIE_ABSCHNEIDEN = true;
    public static final boolean STRATEGIE_LAUERN = true;

    private static final float JAGD_SCHRITT = 2f;   // kurze Schritte -> oft neu hinschauen
    private static final float SUCH_SCHRITT = 6f;   // Etappen beim Suchen -> unterwegs lernen

    /** Wohin auf der Linie Hase -> Busch der Fuchs sich stellt (0 = beim Hasen, 1 = am Busch). */
    private static final float ABSCHNEIDEN_ANTEIL = 0.6f;

    /** Suchziele halten so viel Abstand zum Kartenrand (Meter). */
    private static final float RAND = 1f;

    // ---------- Lauern ----------

    /** War der Hase zuletzt so nah an einem bekannten Busch, nimmt der Fuchs an: "Der ist da rein!" */
    private static final float VERSTECK_ERKENNEN = 3f;

    /** Mit dieser Wahrscheinlichkeit legt er sich auf die Lauer (sonst zieht er weiter). */
    private static final float LAUER_WAHRSCHEINLICHKEIT = 0.35f;

    /** Abstand des Lauerplatzes vom Busch (Meter) – weit genug, dass der Hase ihn nicht bemerkt. */
    private static final float LAUER_ABSTAND = 7f;

    /** Ohne Hasen in Sicht gibt er nach so vielen Ticks auf (140 x 50 ms = 7 s). */
    private static final int LAUER_MAX_OHNE_HASE = 140;

    /** Mit Hasen in Sicht wartet er bis zu so vielen Ticks auf den richtigen Moment (15 s). */
    private static final int LAUER_MAX_MIT_HASE = 300;

    /** Nach einem Lauern so viele Runden lang nicht wieder lauern. */
    private static final int LAUER_PAUSE_RUNDEN = 60;

    // ---------- Angriffsentscheidung ----------

    /** Tempo beim Ansprung (gilt nur für die kurze Sprint-Phase). */
    private static final float SPRINT_TEMPO = Art.FUCHS.tempo * Welt.SPRINT_FAKTOR;

    /** Der Fuchs muss so viel schneller sein als der Hase (1.5 = 50 % Puffer). */
    private static final float SICHERHEIT = 1.5f;

    /** Ist der Hase näher als das an einer Deckung, greift er aus der Lauer gar nicht erst an (Meter). */
    private static final float MIN_DECKUNGSABSTAND = 5f;

    /**
     * Der Hase ist schon sicher, sobald er die KANTE des Busches erreicht –
     * etwa so viel früher als die Mitte der Buschzelle (Meter).
     */
    private static final float DECKUNGS_RAND = 1f;

    private final Random zufall = new Random();
    private IUmweltDienst umwelt;
    private int id;

    // ---------- Wissen (Beliefs) ----------
    private float bauX, bauY;
    private final Set<Punkt> bekannteBuesche = new LinkedHashSet<>();

    /** Wo er in der letzten Runde einen Hasen gesehen hat (NaN = keinen). */
    private float letzterHaseX = Float.NaN, letzterHaseY = Float.NaN;

    /** Busch, vor dem er lauert (null = lauert gerade nicht). */
    private Punkt versteck = null;
    private float lauerX, lauerY;
    private int lauerTicks;

    /** Wie viele Runden er noch nicht wieder lauern darf. */
    private int lauerPause = 0;

    /** Aktuelles Suchziel im Revier (NaN = noch keins). */
    private float suchZielX = Float.NaN, suchZielY = Float.NaN;

    @OnStart
    protected void start(IComponent agent)
    {
        umwelt = agent.getFeature(IRequiredServiceFeature.class).getLocalService(IUmweltDienst.class);
        id = umwelt.anmelden(Art.FUCHS).get();

        // Der Fuchs startet an seinem Bau und merkt sich, wo der ist
        Wahrnehmung erste = umwelt.wasIstInMeinerNaehe(id).get();
        bauX = erste.x();
        bauY = erste.y();
        System.out.printf("Fuchs %d hat seinen Bau bei (%.0f | %.0f)%n", id, bauX, bauY);

        IExecutionFeature exe = agent.getFeature(IExecutionFeature.class);
        exe.scheduleStep(new Runnable()
        {
            public void run()
            {
                if (lauerPause > 0)
                    lauerPause--;

                // 1. Wahrnehmen + Wissen aktualisieren
                Wahrnehmung w = umwelt.wasIstInMeinerNaehe(id).get();
                lerneBuesche(w);
                Sichtung hase = w.naechstes(Art.HASE);

                // 2. Entscheiden + 3. Handeln
                if (hase != null)
                {
                    letzterHaseX = hase.x();
                    letzterHaseY = hase.y();
                    suchZielX = Float.NaN;

                    boolean aufDerLauer = versteck != null && amLauerplatz(w);

                    if (aufDerLauer && lohntAngriff(w, hase, true))
                    {
                        // Gelegenheit: aus der Lauer anspringen (mit kurzem Sprint)
                        beendeLauern("springt aus dem Hinterhalt!");
                        ansprung(w, hase);
                    }
                    else if (aufDerLauer && lauerTicks < LAUER_MAX_MIT_HASE)
                    {
                        // Beute im Blick, aber noch keine Chance: geduckt bleiben und geduldig sein
                        lauerTicks++;
                        umwelt.lauern(id).get();
                    }
                    else
                    {
                        // Normale Jagd (Lauern ggf. abbrechen)
                        if (versteck != null)
                            beendeLauern(aufDerLauer
                                ? "hat lange genug gelauert und gibt auf"
                                : "bricht das Lauern ab und jagt");
                        jagen(w, hase);
                    }
                }
                else
                {
                    // Kein Hase zu sehen. Ist gerade einer in einem Busch verschwunden?
                    if (versteck == null && STRATEGIE_LAUERN && lauerPause == 0)
                    {
                        Punkt busch = erkenneVersteck();
                        if (busch != null && zufall.nextFloat() < LAUER_WAHRSCHEINLICHKEIT)
                            beginneLauern(w, busch);
                    }
                    letzterHaseX = Float.NaN;   // Sichtung ist verarbeitet
                    letzterHaseY = Float.NaN;

                    if (versteck != null)
                        lauern(w);
                    else
                        revierAbsuchen(w);
                }

                exe.scheduleStep(this);
            }
        });
    }

    // ------------------------------------------------------------------
    // Wissen aktualisieren
    // ------------------------------------------------------------------

    /**
     * Gedächtnis pflegen:
     * - Vergessen: gemerkte Büsche, die gerade in Sicht sein müssten,
     *   aber nicht mehr da sind (z. B. später vom Landwirt gemäht)
     * - Lernen: sichtbare Büsche im eigenen Revier merken
     */
    private void lerneBuesche(Wahrnehmung w)
    {
        int vorher = bekannteBuesche.size();
        float sicherSichtbar = Art.FUCHS.sicht - 0.5f;   // kleiner Puffer am Sichtrand

        bekannteBuesche.removeIf(b ->
            b.abstandZu(w.x(), w.y()) <= sicherSichtbar && !w.buesche().contains(b));

        for (Punkt b : w.buesche())
            if (b.abstandZu(bauX, bauY) <= Welt.REVIER_RADIUS)
                bekannteBuesche.add(b);

        if (bekannteBuesche.size() != vorher)
            System.out.printf("Fuchs %d kennt jetzt %d Busch-Zellen in seinem Revier%n",
                id, bekannteBuesche.size());
    }

    /**
     * Ist der Hase, den ich eben noch gesehen habe, in einen bekannten Busch gelaufen?
     * (Bekannte Büsche liegen immer im eigenen Revier.)
     *
     * @return der Busch, in dem der Hase verschwunden ist, oder null
     */
    private Punkt erkenneVersteck()
    {
        if (Float.isNaN(letzterHaseX))
            return null;   // in der letzten Runde war kein Hase zu sehen

        Punkt busch = naechsterBusch(letzterHaseX, letzterHaseY, bekannteBuesche);

        // Hase war nicht an einem bekannten Busch -> wohl nur aus der Sicht gelaufen
        if (busch == null || busch.abstandZu(letzterHaseX, letzterHaseY) > VERSTECK_ERKENNEN)
            return null;
        return busch;
    }

    // ------------------------------------------------------------------
    // Handlungen
    // ------------------------------------------------------------------

    /** JAGEN: zuschlagen, wenn nah genug, sonst (strategisch) hinterher. */
    private void jagen(Wahrnehmung w, Sichtung hase)
    {
        if (hase.abstand() <= Welt.FANG_RADIUS)
        {
            umwelt.fangen(id, hase.id()).get();
            return;
        }
        float[] ziel = STRATEGIE_ABSCHNEIDEN
            ? planAbschneiden(w, hase)
            : planDirektHinterher(w, hase);
        umwelt.bewegeZu(id, ziel[0], ziel[1]).get();
    }

    /**
     * ANSPRUNG aus der Lauer: mit kurzem Sprint zum Abfangpunkt zwischen
     * Hase und Deckung (bzw. direkt auf den Hasen, wenn der Weg schon zu ist).
     * Nicht mehr einfach hinterher – von hinten holt er den Hasen nach dem
     * Sprint kaum noch ein.
     */
    private void ansprung(Wahrnehmung w, Sichtung hase)
    {
        if (hase.abstand() <= Welt.FANG_RADIUS)
        {
            umwelt.fangen(id, hase.id()).get();
            return;
        }
        float[] ziel = planAbschneiden(w, hase);
        umwelt.springeAn(id, ziel[0], ziel[1]).get();
        // Danach jagt er normal weiter (ebenfalls mit Abschneiden).
    }

    /** Lauerplatz festlegen: LAUER_ABSTAND vor dem Busch, auf der Seite des Fuchses. */
    private void beginneLauern(Wahrnehmung w, Punkt busch)
    {
        float dx = w.x() - busch.x(), dy = w.y() - busch.y();
        float d = Math.max(0.01f, (float) Math.sqrt(dx * dx + dy * dy));
        lauerX = klemme(busch.x() + dx / d * LAUER_ABSTAND, RAND, Welt.SPALTEN * Welt.ZELLE - RAND);
        lauerY = klemme(busch.y() + dy / d * LAUER_ABSTAND, RAND, Welt.ZEILEN * Welt.ZELLE - RAND);
        lauerTicks = 0;
        versteck = busch;
        System.out.printf("Fuchs %d hat gesehen, wie ein Hase im Busch verschwand – er legt sich auf die Lauer%n", id);
    }

    /** Lauern beenden und eine Pause einlegen (damit er nicht vor dem Busch "festklebt"). */
    private void beendeLauern(String grund)
    {
        System.out.printf("Fuchs %d %s%n", id, grund);
        versteck = null;
        lauerPause = LAUER_PAUSE_RUNDEN;
    }

    /** LAUERN ohne Hasen in Sicht: erst zum Lauerplatz, dort geduckt warten, nach 7 s aufgeben. */
    private void lauern(Wahrnehmung w)
    {
        if (!amLauerplatz(w))
        {
            umwelt.bewegeZu(id, lauerX, lauerY).get();   // hinlaufen (noch nicht geduckt)
            return;
        }

        if (lauerTicks >= LAUER_MAX_OHNE_HASE)
        {
            beendeLauern("hat lange genug gelauert und gibt auf");
            revierAbsuchen(w);
            return;
        }

        lauerTicks++;
        umwelt.lauern(id).get();   // ducken und einen Tick warten
    }

    /** SUCHEN: in Etappen zu einem Suchziel im Revier. */
    private void revierAbsuchen(Wahrnehmung w)
    {
        float[] ziel = planRevierAbsuchen(w);
        umwelt.bewegeZu(id, ziel[0], ziel[1]).get();
    }

    // ------------------------------------------------------------------
    // Entscheidungshilfen
    // ------------------------------------------------------------------

    private boolean amLauerplatz(Wahrnehmung w)
    {
        return Math.hypot(lauerX - w.x(), lauerY - w.y()) < 0.5;
    }

    /**
     * Lohnt sich der Angriff? Ja, wenn EINER der beiden Wege mit Puffer klappt:
     *
     *   1. Direkt:      Fuchs erreicht den Hasen, bevor der Hase am Buschrand ist.
     *   2. Abschneiden: Fuchs ist vor dem Hasen am Abfangpunkt zwischen Hase und Busch
     *                   (dann ist der Fluchtweg versperrt).
     *
     * Nie, wenn der Hase ohnehin direkt an einer Deckung sitzt.
     *
     * @param mitSprint true beim Ansprung aus der Lauer (kurze Sprint-Phase)
     */
    private boolean lohntAngriff(Wahrnehmung w, Sichtung hase, boolean mitSprint)
    {
        Punkt deckung = naechsterBusch(hase.x(), hase.y(), alleBuesche(w));
        if (deckung == null)
            return true;   // keine Deckung bekannt -> Hase hat keine Fluchtmöglichkeit

        float haseZurDeckung = deckung.abstandZu(hase.x(), hase.y());
        if (haseZurDeckung < MIN_DECKUNGSABSTAND)
            return false;  // Hase ist mit zwei Sprüngen wieder drin -> sinnlos

        // Weg 1: direkt erwischen, bevor er am Buschrand (nicht der Mitte!) ist
        float zeitHaseZurDeckung = (haseZurDeckung - DECKUNGS_RAND) / Art.HASE.tempo;
        float zeitDirekt = zeitBis(hase.abstand() - Welt.FANG_RADIUS, mitSprint);
        if (zeitDirekt * SICHERHEIT < zeitHaseZurDeckung)
            return true;

        // Weg 2: Fluchtweg abschneiden – vor dem Hasen am Abfangpunkt sein
        float abfangX = hase.x() + (deckung.x() - hase.x()) * ABSCHNEIDEN_ANTEIL;
        float abfangY = hase.y() + (deckung.y() - hase.y()) * ABSCHNEIDEN_ANTEIL;
        float fuchsZumAbfang = (float) Math.hypot(abfangX - w.x(), abfangY - w.y());
        float haseZumAbfang = (float) Math.hypot(abfangX - hase.x(), abfangY - hase.y());

        float zeitFuchs = zeitBis(fuchsZumAbfang, mitSprint);
        float zeitHase = haseZumAbfang / Art.HASE.tempo;
        return zeitFuchs * SICHERHEIT < zeitHase;
    }

    /**
     * Wie viele Ticks braucht der Fuchs für diese Strecke?
     * Mit Sprint: die ersten SPRINT_TICKS schnell, danach normales Tempo.
     */
    private float zeitBis(float strecke, boolean mitSprint)
    {
        if (strecke <= 0)
            return 0;
        if (!mitSprint)
            return strecke / Art.FUCHS.tempo;

        float sprintStrecke = Welt.SPRINT_TICKS * SPRINT_TEMPO;
        if (strecke <= sprintStrecke)
            return strecke / SPRINT_TEMPO;
        return Welt.SPRINT_TICKS + (strecke - sprintStrecke) / Art.FUCHS.tempo;
    }

    // ------------------------------------------------------------------
    // Pläne (berechnen nur Ziele)
    // ------------------------------------------------------------------

    /**
     * Revier absuchen: ein zufälliges Ziel im Revier wählen und in
     * Etappen dorthin laufen. Nach jeder Etappe schaut er neu hin
     * (und lernt dabei Büsche kennen).
     */
    private float[] planRevierAbsuchen(Wahrnehmung w)
    {
        boolean keinZiel = Float.isNaN(suchZielX);
        boolean zielErreicht = !keinZiel
            && Math.hypot(suchZielX - w.x(), suchZielY - w.y()) < 1f;

        if (keinZiel || zielErreicht)
        {
            // Zufälliger Punkt im Kreis um den Bau (sqrt -> gleichmäßig verteilt).
            // Ziel muss auf der Karte liegen, sonst hängt der Fuchs am Rand fest.
            double winkel = zufall.nextDouble() * 2 * Math.PI;
            double r = Math.sqrt(zufall.nextDouble()) * Welt.REVIER_RADIUS;
            suchZielX = klemme(bauX + (float) (Math.cos(winkel) * r), RAND, Welt.SPALTEN * Welt.ZELLE - RAND);
            suchZielY = klemme(bauY + (float) (Math.sin(winkel) * r), RAND, Welt.ZEILEN * Welt.ZELLE - RAND);
        }
        return schrittRichtung(w.x(), w.y(), suchZielX, suchZielY, SUCH_SCHRITT);
    }

    /** Einfach direkt auf den Hasen zu. */
    private float[] planDirektHinterher(Wahrnehmung w, Sichtung hase)
    {
        return schrittRichtung(w.x(), w.y(), hase.x(), hase.y(), JAGD_SCHRITT);
    }

    /**
     * Fluchtweg abschneiden (mit Gedächtnis):
     * 1. Welcher Busch ist für den Hasen der nächste? (bekannte + sichtbare Büsche)
     * 2. Ist der Fuchs näher an diesem Busch als der Hase?
     *    nein -> zuerst auf die Linie Hase -> Busch, Richtung Busch
     *    ja   -> Weg ist versperrt, direkt angreifen
     */
    private float[] planAbschneiden(Wahrnehmung w, Sichtung hase)
    {
        Punkt busch = naechsterBusch(hase.x(), hase.y(), alleBuesche(w));
        if (busch == null)
            return planDirektHinterher(w, hase);

        float fuchsZumBusch = busch.abstandZu(w.x(), w.y());
        float haseZumBusch = busch.abstandZu(hase.x(), hase.y());
        if (fuchsZumBusch < haseZumBusch)
            return planDirektHinterher(w, hase);   // Fluchtweg ist versperrt: angreifen

        float zielX = hase.x() + (busch.x() - hase.x()) * ABSCHNEIDEN_ANTEIL;
        float zielY = hase.y() + (busch.y() - hase.y()) * ABSCHNEIDEN_ANTEIL;
        return schrittRichtung(w.x(), w.y(), zielX, zielY, JAGD_SCHRITT);
    }

    // ------------------------------------------------------------------
    // Hilfsfunktionen
    // ------------------------------------------------------------------

    /** Alle Büsche, die der Fuchs kennt oder gerade sieht. */
    private Set<Punkt> alleBuesche(Wahrnehmung w)
    {
        Set<Punkt> alle = new HashSet<>(bekannteBuesche);
        alle.addAll(w.buesche());
        return alle;
    }

    /** Der Busch, der dem Punkt (x, y) am nächsten ist – oder null. */
    private Punkt naechsterBusch(float x, float y, Set<Punkt> buesche)
    {
        Punkt bester = null;
        float besterAbstand = Float.MAX_VALUE;
        for (Punkt b : buesche)
        {
            float d = b.abstandZu(x, y);
            if (d < besterAbstand)
            {
                besterAbstand = d;
                bester = b;
            }
        }
        return bester;
    }

    /** Ein Schritt von (vonX, vonY) Richtung (nachX, nachY), höchstens maxSchritt lang. */
    private float[] schrittRichtung(float vonX, float vonY, float nachX, float nachY, float maxSchritt)
    {
        float dx = nachX - vonX, dy = nachY - vonY;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 0.001f)
            return new float[] { vonX, vonY };
        float s = Math.min(maxSchritt, d);
        return new float[] { vonX + dx / d * s, vonY + dy / d * s };
    }

    /** Wert auf den Bereich [min, max] begrenzen. */
    private static float klemme(float wert, float min, float max)
    {
        return Math.max(min, Math.min(max, wert));
    }
}