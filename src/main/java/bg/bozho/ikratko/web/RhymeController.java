package bg.bozho.ikratko.web;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import bg.bozho.ikratko.other.RhymeService;

@Controller
public class RhymeController {

    @Autowired
    private RhymeService rhymeService;

    @RequestMapping("/rhymes")
    public String rhymeIndex() {
        return "rhymes";
    }

    @RequestMapping("/rhymes/find")
    @ResponseBody
    public Set<String> getRhymes(@RequestParam("ending") String ending, @RequestParam(value="syllables", required=false) int syllables){
        return rhymeService.getRhymes(ending, syllables);
    }

    @RequestMapping("/rhymes/similarEndings")
    @ResponseBody
    public Set<String> getRhumes(@RequestParam("ending") String ending){
        return rhymeService.getSimilarEndings(ending);
    }
}
