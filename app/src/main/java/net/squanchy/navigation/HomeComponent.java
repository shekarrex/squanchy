package net.squanchy.navigation;

import net.squanchy.analytics.Analytics;
import net.squanchy.injection.ActivityLifecycle;
import net.squanchy.injection.ApplicationComponent;
import net.squanchy.remoteconfig.RemoteConfig;

import dagger.Component;

@ActivityLifecycle
@Component(dependencies = {ApplicationComponent.class})
public interface HomeComponent {

    Analytics analytics();

    RemoteConfig remoteConfig();
}
