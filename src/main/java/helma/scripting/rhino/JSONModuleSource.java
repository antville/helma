package helma.scripting.rhino;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.commonjs.module.provider.ModuleSource;

public class JSONModuleSource extends ModuleSource {

    private static final long serialVersionUID = 4446798833357540398L;

    public JSONModuleSource(Object securityDomain, URI uri, URI base, Object validator) {
        super(null, securityDomain, uri, base, validator);
    }

    @Override
    public Reader getReader() {
        StringBuffer content = new StringBuffer();
        content.append("module.exports = ");

        try {
            content.append(IOUtils.toString(this.getUri().toURL().openStream(), "UTF-8"));
        } catch (IOException e) {
            content.append("null");
        }

        content.append(";");

        return new StringReader(content.toString());
    }

}
