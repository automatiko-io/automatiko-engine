package $Package$;


import io.automatik.engine.api.Config;
import io.automatik.engine.api.uow.UnitOfWorkManager;

public class Application implements io.automatik.engine.api.Application {

   
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
