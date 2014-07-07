package bg.bozho.ikratko.web;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import bg.bozho.ikratko.other.Anagram;

import com.google.common.collect.Sets;

@Controller
public class AnagramController {

    @Autowired
    private Anagram anagramService;

    @RequestMapping("/anagrams")
    public String anagramIndex() {
        return "anagrams";
    }

    @RequestMapping("/anagrams/get")
    @ResponseBody
    public Set<String> getAnagrams(@RequestParam("word") String word){
        if (word.length() > 15 || word.contains(" ")) {
            return Sets.newHashSet("Думата трябва да е по-къса от 15 символа и да не съдържа интервали");
        }
        if (StringUtils.isBlank(word)) {
            return Sets.newHashSet("Моля въведете дума");
        }
        Set<String> anagrams = anagramService.getAnagrams(word);
        if (anagrams.isEmpty()) {
            return Sets.newHashSet("(няма)");
        }
        return anagrams;
    }
}
