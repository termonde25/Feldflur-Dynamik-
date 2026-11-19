package feldflur;

import feldflur.welt.Art;
import feldflur.welt.TickErgebnis;
import feldflur.welt.Welt;

public class WeltTest
{
    public static void main(String[] args)
    {
        Welt welt = new Welt();
        int hase = welt.neuesTier(Art.HASE);
        System.out.printf("Hase %d startet bei (%.1f | %.1f)%n", hase, welt.getX(hase), welt.getY(hase));

        welt.setzeZiel(hase, 10, 10);
        System.out.println("Ziel: (10 | 10)");

        while (true)
        {
            TickErgebnis e = welt.tick();
            System.out.printf("Tick %3d: Hase bei (%.1f | %.1f)%n", welt.getTick(), welt.getX(hase), welt.getY(hase));
            if (e.angekommen().contains(hase))
            {
                System.out.println("Angekommen!");
                break;
            }
            if (e.unterbrochen().contains(hase))
            {
                System.out.println("Unterbrochen!");
                break;
            }
        }
    }
}