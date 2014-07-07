package bg.bozho.ikratko.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import bg.bozho.ikratko.Checker;
import bg.bozho.ikratko.Checker.Mistake;
import bg.bozho.ikratko.Checker.Result;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.base.Joiner;

@Controller
public class HomeController {

    @Autowired
    private DatastoreService datastoreService;

    @Autowired
    private Checker checker;

    @RequestMapping("/")
    public String home() {
        return "index";
    }

    @RequestMapping("/submit")
    public String submit(@RequestParam String text, Model model) {
        boolean singleWord = !text.trim().contains(" ");
        if (singleWord) {
            model.addAttribute("error", "singleWord");
        }

        Result result = checker.process(text, true);
        List<String> mistakenWords = new ArrayList<String>(result.getMistakes().size());
        List<String> otherMisspelledWords = new ArrayList<String>(result.getOtherSpellingMistakes().size());
        for (Mistake mistake : result.getMistakes()) {
            mistakenWords.add(mistake.getWord());
        }
        for (Mistake mistake : result.getOtherSpellingMistakes()) {
            otherMisspelledWords.add(mistake.getWord());
        }

        // don't store single-word attempts
        if (!singleWord) {
            Entity check = new Entity("check");
            check.setUnindexedProperty("text", text);
            check.setUnindexedProperty("results", Joiner.on(", ").join(mistakenWords));
            check.setUnindexedProperty("incorrect", Boolean.FALSE);
            check.setProperty("date", System.currentTimeMillis());
            Key key = datastoreService.put(check);
            model.addAttribute("resultId", key.getId());
        }

        model.addAttribute("mistakes", result.getMistakes());
        model.addAttribute("otherMistakes", Joiner.on(", ").join(otherMisspelledWords));
        model.addAttribute("properNames", Joiner.on(", ").join(result.getProperNames()));
        model.addAttribute("isCorrect", result.getMistakes().isEmpty());
        model.addAttribute("input", text);

        return "results";
    }

    @RequestMapping("/reportIncorrectResult")
    @ResponseBody
    public void reportIncorrectResult(@RequestParam long resultId) throws EntityNotFoundException {
        Entity check = datastoreService.get(KeyFactory.createKey("check", resultId));
        check.setUnindexedProperty("incorrect", Boolean.TRUE);
        datastoreService.put(check);
    }

}
