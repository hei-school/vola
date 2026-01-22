package school.hei.vola.conf;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ZavatraConf extends FacadeIT{
    @Value("${orange.api.url}")
    private String apiUrl;

    @Test
    public void is_api_url_exist(){

        assertNotNull(apiUrl);
    }

}

