package feldflur;

import java.util.List;

import feldflur.welt.Art;
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
            List<Integer> angekommen = welt.tick();
            System.out.printf("Tick %3d: Hase bei (%.1f | %.1f)%n", welt.getTick(), welt.getX(hase), welt.getY(hase));
            if (angekommen.contains(hase))
            {
                System.out.println("Angekommen!");
                break;
            }
        }
    }
}