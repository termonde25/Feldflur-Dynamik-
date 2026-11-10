package feldflur;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;

public class HelloAgent
{
    @OnStart
    protected void start(IComponent agent)
    {
        System.out.println("Hallo, ich bin der Agent " + agent.getId() + " und wurde gerade gestartet.");
        System.out.println("Ich warte fünf Sekunden, dann beende ich mich.");
        agent.getFeature(IExecutionFeature.class).waitForDelay(5000).get();
        System.out.println("Okay, Zeit ist um, ich beende mich.");
        agent.terminate();
    }

    public static void main(String[] args)
    {
        IComponentManager.get().create(new HelloAgent());
        IComponentManager.get().waitForLastComponentTerminated();
    }
}