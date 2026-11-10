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

    /** Zu (x, y) laufen. Das Future wird erst bei Ankunft erfüllt (false = abgebrochen/tot). */
    public IFuture<Boolean> bewegeZu(int id, float x, float y);

    /** Fuchs versucht, einen Hasen zu fangen. */
    public IFuture<Boolean> fangen(int fuchsId, int haseId);
}