package dia.ismd;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;

@SpringBootTest
@ApplicationModuleTest
class IsmdBackendApplicationTests {

    @Disabled("Temporarily disabled for CI workflow testing")
    @Test
    void contextLoads() {
    }


    @Disabled("Temporarily disabled for CI workflow testing")
    @Test
    void modulesTest(){
        ApplicationModules.of(IsmdBackendApplication.class).verify();
    }

}
