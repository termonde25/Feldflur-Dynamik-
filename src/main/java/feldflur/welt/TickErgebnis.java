package feldflur.welt;

import java.util.List;

/**
 * Was in einem Tick mit den Bewegungen passiert ist.
 *
 * @param angekommen   Tiere, die ihr Ziel erreicht haben
 * @param unterbrochen Tiere, die stehen geblieben sind, weil unterwegs
 *                     etwas Wichtiges aufgetaucht ist
 */
public record TickErgebnis(List<Integer> angekommen, List<Integer> unterbrochen)
{
}