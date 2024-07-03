# Partitioned GemFire CQ Data Worker

In distributed data management and real-time processing, GemFire offers powerful capabilities for managing large volumes of data with low latency. One of its key features is Continuous Query (CQ), which allows real-time monitoring of data changes and triggers actions based on those changes.

Introducing the Partitioned GemFire CQ Data Worker, a specialized design to handle and process data events efficiently by leveraging the power of partitioning. This worker partitions events across multiple workers, ensuring optimal distribution and processing load balancing.

## Key Features
* Event Partitioning: The GemFire CQ Data Worker partitions incoming events based on the specified number of workers. Each worker is assigned a subset of events, enabling parallel processing and improving throughput.
* Scalability: By distributing events across multiple workers, the system can easily scale to handle increased data volumes and processing demands.
* Real-Time Processing: With Continuous Query, the data worker can immediately react to data changes, providing real-time insights and actions.
* Fault Tolerance: The partitioning strategy ensures that even if a worker fails, other workers can continue processing, maintaining system reliability.   When the worker restarts it will pick up where it left off using durable client subscriptions.

# How It Works
* Continuous Query Subscription: The data worker subscribes to specific data events using GemFire's Continuous Query mechanism. This allows it to receive notifications whenever relevant data changes occur.
* Event Partitioning: Upon receiving an event, the data worker determines the appropriate partition based on a hashing algorithm where we take the hashcode of some property and modulus it by the number of workers. The event is then routed to the corresponding worker.
* Parallel Processing: Each worker processes its assigned events independently and in parallel, allowing for efficient and fast data handling.
* Durable Event Subscription:  Each worker has its own durable subscription queue to ensure each worker can resume when it needs to be taken down for upgrades.

# Use Cases
* Real-Time Analytics: Ideal for applications requiring real-time data analysis and decision-making, such as financial trading platforms, fraud detection systems, and IoT monitoring solutions.
* Distributed Event Processing: Suitable for environments where high throughput and low latency are critical, such as e-commerce transaction processing, supply chain management, and logistics.
* Scalable Data Processing: Perfect for systems that need to scale horizontally to handle growing data volumes, ensuring consistent performance as demand increases.
* By incorporating the GemFire CQ Data Worker into your architecture, you can harness the full potential of Apache Geode's real-time data processing capabilities while ensuring efficient and scalable event handling. Whether you're dealing with high-velocity data streams or complex event processing requirements, this worker provides a robust solution to meet your needs.

# Alternative design considerations

In system architecture, there are often multiple approaches to achieve the same goal. One simpler pattern for implementing a dynamic partitioning, event-driven design is to allow the GemFire server itself to process events. This approach leverages the built-in capabilities of the GemFire server, simplifying the overall design.   This is achieved by utilizing GemFire Async Event Listener handling events while maintaining the benefits of server-side processing.

The primary drawback of this method is the absence of Continuous Query (CQ) support on the server, which can further refine the data set which needs processing.

# How to run

To see this in action there are a couple of items that need run.

1) The GemFire cluster - this would have the responsibility of storing the data and keeping the data safe as the workers come and go with durable CQ subscriptions.
2) Two CQ workers so we can see the partitioning in action
3) A Data Loader - the data loader will inject 5000 customer records to simulate some "reasonable" data to work with

## Run all the things

I am currently running on windows so those are the most polished of scripts.    If you have issues with any of the scripts let me know and I will attempt to fix it up - or submit a pull request to help out.
```bash
cd <project dir>
cd scripts
startGemFire.bat
```
I like to wait for GemFire to be full initialized and running.   So just wait for the script to finish.
```
cd <project dir>
start gradlew runCqWorker1
start gradlew runCqWorker2
```
To ensure that the CqWorkers are fully up and running wait for a line of output that looks like this:  `cqQuery = SELECT * FROM /test where ((guid.hashCode() % 2) + 2) % 2 = 0`

Its ok if you don't want to wait - it just makes it harder to see if the workers started before or after the data loader.   This only matters on the first time before the durable queues are established.   After they are establish GemFire will be maintaining the durable queue while the client isn't running.
```
start gradlew runDataLoader
```
The data loader will inject 5000 customer records that you can watch on the 2 cq workers.   If the output scrolls to fast because of how awesomely fast GemFire is put a sleep for 10 ms in the data loader loop.

Enjoy

Charlie

