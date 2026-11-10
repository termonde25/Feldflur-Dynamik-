package feldflur.umwelt;

import feldflur.welt.Art;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface IUmweltDienst
{
    /** Neues Tier anlegen. Liefert die ID des Körpers in der Welt. */
    public IFuture<Integer> anmelden(Art art);

    /** Zu (x, y) laufen. Das Future wird erst bei Ankunft erfüllt. */
    public IFuture<Boolean> bewegeZu(int id, float x, float y);
}