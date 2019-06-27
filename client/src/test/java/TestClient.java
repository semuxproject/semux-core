import org.semux.api.v2.ApiException;
import org.semux.api.v2.client.SemuxApi;
import org.semux.api.v2.model.GetInfoResponse;

public class TestClient {

    /**
     * Example to show usage of setting up client
     *
     * @param args
     * @throws ApiException
     */
    public static void main(String[] args) throws ApiException {

        SemuxApi api = new SemuxApi();
        api.getApiClient().setBasePath("http://localhost:5171/v2.2.0");
        api.getApiClient().setUsername("user");
        api.getApiClient().setPassword("pass");
        GetInfoResponse info = api.getInfo();
        System.out.println(info);

    }
}
