package com.benjiweber;

import org.joda.time.DateTime;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.benjiweber.Incident.incident;
import static com.benjiweber.Team.team;
import static com.benjiweber.Notifier.notifier;
import static com.benjiweber.MatchesAny._;

public class Example {
    public static void main(String... args) {
        Incident incident = incident(team("team name"), "incident name");

        Notifier notifier = notifier(Partially.apply(
            FunctionalIncidentNotifier::notifyOf,
            new ConfigFileRota(new File("/etc/my.rota")),
            new EmailPager("smtp.example.com"),
            DateTime::new,
            _
        ));

        notifier.notifyOf(incident);
    }

}
interface Notifier {
    void notifyOf(Incident incident);
    static Notifier notifier(Consumer<Incident> notifier) {
        return incident -> notifier.accept(incident);
    }
}

class FunctionalIncidentNotifier {
    public static void notifyOf(Rota rota, Pager pager, Supplier<DateTime> clock, Incident incident) {
        Person onCall = rota.onCallFor(incident.team());
        pager.page(onCall, "Oh noes, " + incident  + " happened at " + clock.get());
    }
}

class Partially {
    static <T,U,V,W> Consumer<W> apply(QuadConsumer<T,U,V,W> f, T t, U u, V v, MatchesAny _) {
        return w -> f.apply(t,u,v,w);
    }
}
interface QuadConsumer<T,U,V,W> {
    void apply(T t, U u, V v, W w);
}
class MatchesAny {
    public static MatchesAny _;
}

interface MonitoringApp extends
        Application,
        IncidentNotifierProvider {
    default void main() {
        String teamName = args().get(0);
        String incidentName = args().get(1);
        notifier().notifyOf(incident(team(teamName), incidentName));
    }
}

interface ProductionApp extends
        MonitoringApp,
        DefaultIncidentNotifierProvider,
        EmailPagerProvider,
        ConfigFileRotaProvider,
        DateTimeProvider {}

interface WorkstationApp extends
        MonitoringApp,
        DefaultIncidentNotifierProvider,
        ConsolePagerProvider,
        ConfigFileRotaProvider,
        DateTimeProvider  {}

interface IncidentNotifierProvider {
    IncidentNotifier notifier();
}

interface DefaultIncidentNotifierProvider extends IncidentNotifierProvider, PagerProvider, RotaProvider, ClockProvider {
    default IncidentNotifier notifier() { return new IncidentNotifier(pager(), rota(), clock()); }
}

interface PagerProvider {
    Pager pager();
}

interface RotaProvider {
    Rota rota();
}

interface ClockProvider {
    Supplier<DateTime> clock();
}

interface EmailPagerProvider extends PagerProvider {
    default Pager pager() { return new EmailPager("smtp.example.com"); }
}

interface ConsolePagerProvider extends PagerProvider {
    default Pager pager() { return (Person onCall, String message) -> System.out.println("Stub pager says " + onCall + " " + message); }
}

interface ConfigFileRotaProvider extends RotaProvider {
    default ConfigFileRota rota() { return new ConfigFileRota(new File("/etc/my.rota")); }
}

interface DateTimeProvider extends ClockProvider {
    default Supplier<DateTime> clock() { return DateTime::new; }
}


interface Application {
    List<String> args();
}

class IncidentNotifier {

    final Rota rota;
    final Pager pager;
    final Supplier<DateTime> clock;

    IncidentNotifier(Pager pager, Rota rota, Supplier<DateTime> clock) {
        this.pager = pager;
        this.rota = rota;
        this.clock = clock;
    }

    void notifyOf(Incident incident) {
        Person onCall = rota.onCallFor(incident.team());
        pager.page(onCall, "Oh noes, " + incident  + " happened at " + clock.get());
    }

}

interface Rota {
    Person onCallFor(Team team);
}
interface Pager {
    void page(Person onCall, String message);
}
interface Incident {
    Team team();
    String name();
    static Incident incident(Team team, String name) {
        return new Incident() {
            public Team team() { return team; }
            public String name() { return name; }
            public String toString() { return name(); }
        };
    }
}
interface Person {
    String name();
    static Person person(String name) { return () -> name; }
}
interface Team {
    String name();
    static Team team(String name) { return () -> name; }
}

class EmailPager implements Pager {
    public EmailPager(String smtpServer) {}
    public void page(Person onCall, String message) {
        System.out.println(message);
    }
}

interface ConsolePager extends Pager {}

interface EnvironmentAware {
    default boolean isProduction() {
        // We could check for a machine manifest here
        return false;
    }
}

class EnvironmentAwarePager implements Pager, EnvironmentAware {
    final Pager prodPager;
    final Pager devPager;

    EnvironmentAwarePager(Pager prodPager, Pager devPager) {
        this.prodPager = prodPager;
        this.devPager = devPager;
    }

    public void page(Person onCall, String message) {
        if (isProduction()) prodPager.page(onCall, message);
        else devPager.page(onCall, message);
    }
}

class ConfigFileRota implements Rota {
    public ConfigFileRota(File file) {

    }
    public Person onCallFor(Team team) {
        return null;
    }
}