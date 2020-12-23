package $Package$;


import io.automatiko.engine.api.Config;
import io.automatiko.engine.api.uow.UnitOfWorkManager;

public class Application implements io.automatiko.engine.api.Application {

   
    public Config config() {
        return config;
    }
    
    public UnitOfWorkManager unitOfWorkManager() {
        return config().process().unitOfWorkManager();
    }
    
    public void setup() {
        if (config().process() != null) {
            if (eventPublishers != null) {
                eventPublishers.forEach(publisher -> 
                unitOfWorkManager().eventManager().addPublisher(publisher));
                
            }
            unitOfWorkManager().eventManager().setService(service.orElse(""));
            unitOfWorkManager().eventManager().setAddons(config().addons());
        }
    }
}
