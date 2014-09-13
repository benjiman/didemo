package com.benjiweber;

import org.joda.time.DateTime;
import org.junit.Test;

import static com.benjiweber.Person.person;
import static com.benjiweber.Incident.incident;
import static com.benjiweber.Team.team;
import static org.junit.Assert.assertEquals;

public class IncidentNotifierTest {


    @Test(expected=ExpectedInvocation.class)
    public void should_notify_me_when_I_am_on_call() {
        DateTime now = new DateTime();
        Person benji = person("benji");
        Rota rota = regardlessOfTeamItIs -> benji;
        Incident incident = incident(team("a team"), "some incident");
        Pager pager = (person, message) -> ExpectedInvocation.with(() ->
            assertEquals("Oh noes, some incident happened at " + now, message)
        );

        new IncidentNotifier(pager, rota, () -> now).notifyOf(incident);
    }
    static class ExpectedInvocation extends RuntimeException{
        static void with(Runnable action) {
            action.run();
            throw new ExpectedInvocation();
        }
    }

}