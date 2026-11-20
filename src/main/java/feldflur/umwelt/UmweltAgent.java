package feldflur.umwelt;

import java.util.HashMap;
import java.util.Map;

import feldflur.welt.Art;
import feldflur.welt.TickErgebnis;
import feldflur.welt.Wahrnehmung;
import feldflur.welt.Welt;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.OnStart;

/**
 * Die Umwelt als eigener Agent: besitzt die Welt, tickt alle 50 ms
 * und bietet den Tieren den Dienst IUmweltDienst an.
 */
public class UmweltAgent implements IUmweltDienst
{
    private final Welt welt = new Welt();
    private final SchnappschussKanal kanal;

    /** Tiere mit laufendem Auftrag: ID -> Future, das bei Erledigung/Abbruch erfüllt wird */
    private final Map<Integer, Future<Boolean>> unterwegs = new HashMap<>();

    public UmweltAgent(SchnappschussKanal kanal)
    {
        this.kanal = kanal;
    }

    @OnStart
    protected void start(IComponent agent)
    {
        System.out.println("Umwelt gestartet: " + welt.breite() + " x " + welt.hoehe() + " m");
        IExecutionFeature exe = agent.getFeature(IExecutionFeature.class);

        // Tick-Schleife als kurzer, wiederholter Step (Muster wie SellerAgent, Blatt 5)
        exe.scheduleStep(new Runnable()
        {
            public void run()
            {
                TickErgebnis ergebnis = welt.tick();

                // Angekommen -> true, unterwegs unterbrochen -> false
                for (int id : ergebnis.angekommen())
                    melde(id, true);
                for (int id : ergebnis.unterbrochen())
                    melde(id, false);

                // Kopie für die Grafik ablegen
                kanal.setze(welt.schnappschuss());

                exe.waitForDelay(50).get();  // 50 ms warten
                exe.scheduleStep(this);      // nächsten Tick einplanen
            }
        });
    }

    /** Das wartende Future eines Tieres erfüllen -> dessen .get() kehrt zurück. */
    private void melde(int id, boolean erfolg)
    {
        Future<Boolean> f = unterwegs.remove(id);
        if (f != null)
            f.setResult(erfolg);
    }

    /** Neues Future für einen Auftrag anlegen, das im Tick erfüllt wird. */
    private IFuture<Boolean> neuerAuftrag(int id)
    {
        Future<Boolean> f = new Future<>();
        unterwegs.put(id, f);
        return f;
    }

    // ---------- Dienstmethoden ----------

    public IFuture<Integer> anmelden(Art art)
    {
        int id = welt.neuesTier(art);
        return new Future<>(id);
    }

    public IFuture<Wahrnehmung> wasIstInMeinerNaehe(int id)
    {
        return new Future<>(welt.wahrnehmung(id));
    }

    public IFuture<Boolean> bewegeZu(int id, float x, float y)
    {
        // Tote Tiere laufen nirgendwo mehr hin
        if (!welt.lebt(id))
            return new Future<>(false);

        melde(id, false);            // alter Auftrag wird abgebrochen
        welt.setzeZiel(id, x, y);
        return neuerAuftrag(id);     // erfüllt bei Ankunft/Unterbrechung
    }

    public IFuture<Boolean> lauern(int id)
    {
        melde(id, false);            // alter Auftrag wird abgebrochen
        if (!welt.lauern(id))
            return new Future<>(false);  // kein lebender Fuchs
        return neuerAuftrag(id);     // erfüllt im nächsten Tick
    }

    public IFuture<Boolean> springeAn(int id, float x, float y)
    {
        melde(id, false);            // alter Auftrag wird abgebrochen
        if (!welt.springeAn(id, x, y))
            return new Future<>(false);  // lauert nicht -> kein Ansprung
        System.out.println("Fuchs " + id + " sprintet los!");
        return neuerAuftrag(id);     // erfüllt bei Ankunft/Unterbrechung
    }

    public IFuture<Boolean> fressen(int id)
    {
        melde(id, false);            // alter Auftrag wird abgebrochen
        if (!welt.fressen(id))
            return new Future<>(false);  // geht gerade nicht
        return neuerAuftrag(id);     // erfüllt im nächsten Tick
    }

    public IFuture<Boolean> fangen(int fuchsId, int haseId)
    {
        boolean erfolg = welt.fangen(fuchsId, haseId);
        if (erfolg)
        {
            // Der Hase wartet evtl. gerade auf seine Ankunft -> aufwecken mit "false"
            melde(haseId, false);
            System.out.println("Fuchs " + fuchsId + " hat Hase " + haseId + " gefangen.");
        }
        return new Future<>(erfolg);
    }
}