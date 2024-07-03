package dev.gemfire.cqworker;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.query.*;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Command(name = "cqworker", mixinStandardHelpOptions = true, version = "cqworker 1.0",
        description = "Runs a cqworker that is partitioning events over N workers")
public class CqWorker implements Callable<Integer> {

    @Option(names = {"-w", "--worker"}, description = "The worker number", required = true)
    private int workerNumber = 4;
    @Option(names = {"-p", "--partitions"}, description = "The number of workers to partition over", required = true)
    private int partitions = 4;

    @Override
    public Integer call() throws Exception {
        ClientCache clientCache = new ClientCacheFactory()
                .addPoolLocator("localhost", 10334)
                .set("durable-client-id", "myDurableClient" + workerNumber) // Durable client ID
                .set("durable-client-timeout", "300") // Timeout in seconds
                .setPoolSubscriptionEnabled(true)
                .setPdxSerializer(new ReflectionBasedAutoSerializer("dev.gemfire.*"))
                .create();

        Region<String, String> region = clientCache
                .<String, String>createClientRegionFactory(ClientRegionShortcut.PROXY)
                .create("test");
        region.get(1);

        QueryService queryService = clientCache.getQueryService();
        // Create CqAttributes
        CqAttributesFactory cqAttributesFactory = new CqAttributesFactory();
        cqAttributesFactory.addCqListener(new MyCqListener());
        CqAttributes cqAttributes = cqAttributesFactory.create();

        // Create the Continuous Query
        String cqName = "MyDurableCQ " + workerNumber;
        //Hashcode could return negative numbers so we need todo some trickery to keep positive numbers so we don't
        //have to think to hard.
        String queryString = "SELECT * FROM /test where ((guid.hashCode() % " + partitions + ") + " + partitions +") % "+ partitions + " = " + workerNumber;
        CqQuery cqQuery = queryService.newCq(cqName, queryString, cqAttributes, true);
        cqQuery.executeWithInitialResults();
        clientCache.readyForEvents();

        System.out.println("cqQuery = " + queryString);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CqWorker()).execute(args);
        System.exit(exitCode);
    }


    private class MyCqListener implements CqListener {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        @Override
        public void onEvent(CqEvent cqEvent) {
            System.out.println("MyCqListener.onEvent " + atomicInteger.incrementAndGet());
            System.out.println("cqEvent.getNewValue() = " + cqEvent.getNewValue());
        }

        @Override
        public void onError(CqEvent cqEvent) {
            System.out.println("MyCqListener.onError");
            System.out.println("cqEvent = " + cqEvent);
        }

    }
}
