package dev.gemfire.cqworker;

import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.person.Person;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class DataLoader {

    public static void main(String[] args) throws InterruptedException {

        ClientCache clientCache = new ClientCacheFactory()
                .addPoolLocator("localhost", 10334)
                .setPdxSerializer(new ReflectionBasedAutoSerializer("dev.gemfire.*"))
                .setPdxReadSerialized(false)
                .create();

        Region<String, Customer> region = clientCache
                .<String, Customer>createClientRegionFactory(ClientRegionShortcut.PROXY)
                .create("test");
        Fairy fairy = Fairy.create();
        for (int i = 0; i < 5000; i++) {
            Person person = fairy.person();
            Customer customer = Customer.builder()
                    .firstName(person.getFirstName())
                    .middleName(person.getMiddleName())
                    .lastName(person.getLastName())
                    .email(person.getEmail())
                    .username(person.getUsername())
                    .passportNumber(person.getPassportNumber())
                    .password(person.getPassword())
                    .telephoneNumber(person.getTelephoneNumber())
                    .dateOfBirth(person.getDateOfBirth().toString())
                    .age(person.getAge())
                    .companyEmail(person.getCompanyEmail())
                    .nationalIdentificationNumber(person.getNationalIdentificationNumber())
                    .nationalIdentityCardNumber(person.getNationalIdentityCardNumber())
                    .passportNumber(person.getPassportNumber())
                    .guid(UUID.randomUUID().toString())
                    .count(i)
                    .build();
            region.put(customer.getGuid(), customer);
        }
    }
}
