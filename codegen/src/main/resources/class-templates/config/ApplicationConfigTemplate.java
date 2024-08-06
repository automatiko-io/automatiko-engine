import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;

import io.automatiko.engine.api.decision.DecisionConfig;
import io.automatiko.engine.api.workflow.ProcessConfig;


public class ApplicationConfig implements io.automatiko.engine.api.Config {

    protected ProcessConfig processConfig;
    protected DecisionConfig decisionConfig;

    @Override
    public ProcessConfig process() {
        return processConfig;
    }

    @Override
    public DecisionConfig decision() {
        return decisionConfig;
    }

    private static <C, L> List<L> merge(Collection<C> configs, Function<C, Collection<L>> configToListeners, Collection<L> listeners) {
        return Stream.concat(
                configs.stream().flatMap(c -> configToListeners.apply(c).stream()),
                listeners.stream()
        ).collect(Collectors.toList());
    }

    protected boolean isPersistenceDisabled() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.automatiko.persistence.disabled", Boolean.class).orElse(false);
    }
}
