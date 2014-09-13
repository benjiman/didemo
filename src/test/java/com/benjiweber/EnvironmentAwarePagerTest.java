package com.benjiweber;

import org.junit.Test;

import static com.benjiweber.IncidentNotifierTest.ExpectedInvocation;
import static com.benjiweber.Person.person;
import static org.junit.Assert.*;

public class EnvironmentAwarePagerTest {

    @Test(expected = ExpectedInvocation.class)
    public void should_use_production_pager_when_in_production() {
        Person benji = person("benji");
        Pager prod = (person, message) -> ExpectedInvocation.with(() -> {
            assertEquals(benji, person);
            assertEquals("hello", message);
        });
        Pager dev = (person, message) -> fail("Should have used the prod pager");

        class AlwaysOnProductionPager extends EnvironmentAwarePager implements AlwaysInProduction {
            AlwaysOnProductionPager(Pager prodPager, Pager devPager) {
                super(prodPager, devPager);
            }
        }
        new AlwaysOnProductionPager(prod, dev).page(benji , "hello");
    }

    interface AlwaysInProduction extends EnvironmentAware {
        default boolean isProduction() { return true; }
    }
}