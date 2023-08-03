package bifromq.plugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class AuthPlugin extends Plugin {
    /**
     * Constructor to be used by plugin manager for plugin instantiation. Your plugins have to provide constructor with
     * this exact signature to be successfully loaded by manager.
     *
     * @param wrapper
     */
    public AuthPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

}
