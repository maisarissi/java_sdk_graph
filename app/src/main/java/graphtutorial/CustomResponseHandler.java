package graphtutorial;

import java.util.HashMap;

import com.microsoft.kiota.ResponseHandler;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParsableFactory;

public class CustomResponseHandler implements ResponseHandler {

    @Override
    public <NativeResponseType, ModelType> ModelType handleResponse(NativeResponseType response,
            HashMap<String, ParsableFactory<? extends Parsable>> errorMappings) {
        // Do your own implementation of the reponse here
        throw new UnsupportedOperationException("CustomerResponseHandler");
    }
    
}
