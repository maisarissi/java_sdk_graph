package graphtutorial;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.kiota.ResponseHandler;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParsableFactory;

public class CustomResponseHandler implements ResponseHandler {

    @Override
    public <NativeResponseType, ModelType> ModelType handleResponse(NativeResponseType response,
            HashMap<String, ParsableFactory<? extends Parsable>> errorMappings) {
        // Do your own implementation of the reponse here
        try{ 
            okhttp3.Response castResponse = (okhttp3.Response) response; 
            assert castResponse.body() != null; 
            System.out.println(castResponse.body().string()); 
        } 
        catch(ClassCastException ex) { 
            throw new RuntimeException(ex); 
        } catch (IOException e) { 
            throw new RuntimeException(e); 
        }

        return null;
    }
}
