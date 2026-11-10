package feldflur.welt;

/**
 * Der Körper eines Tieres in der Welt.
 * Gehört der Welt, nicht dem Agenten: Der Agent entscheidet nur,
 * die Welt rechnet aus, was passiert.
 */
class Tier
{
    final int id;
    final Art art;
    float x, y;
    float zielX, zielY;
    boolean unterwegs = false;

    Tier(int id, Art art, float x, float y)
    {
        this.id = id;
        this.art = art;
        this.x = x;
        this.y = y;
    }
}