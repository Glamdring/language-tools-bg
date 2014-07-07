package bg.bozho.ikratko.web;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import bg.bozho.ikratko.other.ParonymService;

@Controller
public class ParonymController {

    @Autowired
    private ParonymService paronymService;

    @RequestMapping("/paronyms")
    public String rhymeIndex() {
        return "paronyms";
    }

    @RequestMapping("/paronyms/find")
    @ResponseBody
    public Set<String> getParonyms(@RequestParam("word") String word){
        return paronymService.findParonyms(word);
    }
}
