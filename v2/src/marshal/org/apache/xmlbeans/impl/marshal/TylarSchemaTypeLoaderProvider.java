/*   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.xmlbeans.impl.marshal;

import org.apache.xmlbeans.SchemaTypeLoader;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.impl.binding.tylar.Tylar;

import java.io.IOException;

final class TylarSchemaTypeLoaderProvider
    implements SchemaTypeLoaderProvider
{
    private final Tylar tylar;
    private SchemaTypeLoader schemaTypeLoader; //cached result

    TylarSchemaTypeLoaderProvider(Tylar tylar)
    {
        this.tylar = tylar;
    }

    public SchemaTypeLoader getSchemaTypeLoader()
        throws XmlException
    {
        if (schemaTypeLoader == null) {
            try {
                schemaTypeLoader = tylar.getSchemaTypeLoader();
            }
            catch (IOException ioe) {
                throw new XmlException(ioe);
            }
            catch (XmlException xe) {
                throw new XmlException(xe);
            }
        }
        assert schemaTypeLoader != null;
        return schemaTypeLoader;
    }
}
