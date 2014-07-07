package bg.bozho.ikratko;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import bg.bozho.ikratko.other.RhymeService;


@ContextConfiguration(locations="/applicationContext.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class IntegrationTest {

    @Autowired
    private RhymeService rService;

    @Test
    public void syllableLimitTest(){
        int rhymes = rService.getRhymes("ова", 3).size();
        Assert.assertTrue("No rhymes found", rhymes > 0);
    }
}
