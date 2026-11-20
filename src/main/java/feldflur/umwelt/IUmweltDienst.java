package feldflur.umwelt;

import feldflur.welt.Art;
import feldflur.welt.Wahrnehmung;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

/**
 * Dienst des Umweltagenten. Bewusst grob: wenige Aufrufe, die viel erledigen.
 */
@Service
public interface IUmweltDienst
{
    /** Neues Tier anlegen. Liefert die ID des Körpers in der Welt. */
    public IFuture<Integer> anmelden(Art art);

    /** Wahrnehmung: Was kann ich gerade sehen? */
    public IFuture<Wahrnehmung> wasIstInMeinerNaehe(int id);

    /**
     * Zu (x, y) laufen. Erfüllt bei Ankunft (true) oder wenn die Bewegung
     * abgebrochen wurde (false: unterbrochen, neues Ziel oder tot).
     */
    public IFuture<Boolean> bewegeZu(int id, float x, float y);

    /** Fuchs versucht, einen Hasen zu fangen. */
    public IFuture<Boolean> fangen(int fuchsId, int haseId);

    /**
     * Fuchs duckt sich und wartet einen Tick (Lauern).
     * Hasen sehen ihn dann nur aus nächster Nähe. Jede Bewegung beendet das Lauern.
     */
    public IFuture<Boolean> lauern(int id);

    /**
     * Ansprung aus der Lauer: wie bewegeZu, aber für kurze Zeit mit Sprint.
     * Nur möglich, wenn der Fuchs gerade lauert (sonst sofort false).
     */
    public IFuture<Boolean> springeAn(int id, float x, float y);

    /**
     * Hase frisst einen Tick lang Gras an seiner Position.
     * false, wenn das nicht geht (im Busch, kein Gras, kein Hase).
     */
    public IFuture<Boolean> fressen(int id);
}