
class CustomDeserializer extends io.automatiko.engine.quarkus.functionflow.gcp.PubSubModelDeserializer {

    private static final long serialVersionUID = -3615751636647437846L;
    
    @Override
    protected Class<? extends io.automatiko.engine.api.Model> type() throws Exception {
        return null;
    }

}